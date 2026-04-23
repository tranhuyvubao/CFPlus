package com.example.do_an_hk1_androidstudio;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.ui.DonutChartView;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.example.do_an_hk1_androidstudio.ui.ReportExportHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThongKeTongHopActivity extends AppCompatActivity {

    private EditText edtStart;
    private EditText edtEnd;
    private Spinner spOrderFilter;
    private TextView tvSummary;
    private TextView tvPaymentSummary;
    private TextView tvTopProducts;
    private TextView tvForecast;
    private TextView tvMetricTotalOrders;
    private TextView tvMetricPaidOrders;
    private TextView tvMetricRevenue;
    private TextView tvMetricAverage;
    private DonutChartView donutOrderTypeRange;
    private DonutChartView donutPaymentRange;
    private LinearLayout orderDetailContainer;
    private final List<LocalOrder> allOrders = new ArrayList<>();
    private ListenerRegistration ordersListener;
    private String currentReportText = "";
    private ReportExportHelper.ExportResult latestExportedReport;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_thong_ke_tong_hop);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerThongKeTongHop));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootThongKeTongHop));

        View tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        edtStart = findViewById(R.id.edtStartDate);
        edtEnd = findViewById(R.id.edtEndDate);
        spOrderFilter = findViewById(R.id.spOrderFilter);
        TextView btnLoad = findViewById(R.id.btnLoadStats);
        TextView btnExportPdf = findViewById(R.id.btnExportPdf);
        TextView btnShareReport = findViewById(R.id.btnShareReport);
        tvSummary = findViewById(R.id.tvSummary);
        tvPaymentSummary = findViewById(R.id.tvPaymentSummary);
        tvTopProducts = findViewById(R.id.tvTopProducts);
        tvForecast = findViewById(R.id.tvForecast);
        tvMetricTotalOrders = findViewById(R.id.tvMetricTotalOrders);
        tvMetricPaidOrders = findViewById(R.id.tvMetricPaidOrders);
        tvMetricRevenue = findViewById(R.id.tvMetricRevenue);
        tvMetricAverage = findViewById(R.id.tvMetricAverage);
        donutOrderTypeRange = findViewById(R.id.donutOrderTypeRange);
        donutPaymentRange = findViewById(R.id.donutPaymentRange);
        orderDetailContainer = findViewById(R.id.orderDetailContainer);

        bindDatePicker(edtStart);
        bindDatePicker(edtEnd);
        setupFilterSpinner();
        btnLoad.setOnClickListener(v -> loadStats());
        btnExportPdf.setOnClickListener(v -> exportPdf());
        btnShareReport.setOnClickListener(v -> shareReport());

        listenOrders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }

    private void listenOrders() {
        if (ordersListener != null) {
            ordersListener.remove();
        }
        ordersListener = new OrderCloudRepository(this).listenAllOrders(orders -> runOnUiThread(() -> {
            allOrders.clear();
            allOrders.addAll(orders);
            if (TextUtils.isEmpty(edtStart.getText()) || TextUtils.isEmpty(edtEnd.getText())) {
                prefillDateRange();
            }
            loadStats();
        }));
    }

    private void prefillDateRange() {
        Calendar now = Calendar.getInstance();
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.getTime());
        edtStart.setText(today);
        edtEnd.setText(today);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Tất cả đơn", "Đơn online", "Đơn tại chỗ", "Đơn mang về"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrderFilter.setAdapter(adapter);
    }

    private void bindDatePicker(EditText target) {
        target.setFocusable(false);
        target.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String current = target.getText().toString().trim();
            if (!current.isEmpty()) {
                try {
                    Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(current);
                    if (date != null) {
                        calendar.setTime(date);
                    }
                } catch (Exception ignored) {
                }
            }

            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, dayOfMonth);
                        target.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(picked.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void loadStats() {
        String startStr = edtStart.getText().toString().trim();
        String endStr = edtEnd.getText().toString().trim();
        if (TextUtils.isEmpty(startStr) || TextUtils.isEmpty(endStr)) {
            return;
        }

        Long startMillis = parseDate(startStr, false);
        Long endMillis = parseDate(endStr, true);
        if (startMillis == null || endMillis == null) {
            Toast.makeText(this, "Ngày phải đúng định dạng dd/MM/yyyy.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startMillis > endMillis) {
            Toast.makeText(this, "Khoảng thời gian không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<LocalOrder> filteredOrders = new ArrayList<>();
        int totalOrders = 0;
        int paidOrders = 0;
        int revenue = 0;
        int onlineCount = 0;
        int dineInCount = 0;
        int takeawayCount = 0;
        int confirmedCount = 0;
        int createdCount = 0;
        Map<String, Integer> productCount = new LinkedHashMap<>();

        for (LocalOrder order : allOrders) {
            long createdAt = order.getCreatedAtMillis();
            if (createdAt < startMillis || createdAt > endMillis || "cancelled".equals(order.getStatus())) {
                continue;
            }
            if (!matchesFilter(order, spOrderFilter.getSelectedItemPosition())) {
                continue;
            }

            filteredOrders.add(order);
            totalOrders++;
            revenue += order.getTotal();

            if ("online".equals(order.getOrderType()) || "customer_app".equals(order.getOrderChannel())) {
                onlineCount++;
            } else if ("takeaway".equals(order.getOrderType())) {
                takeawayCount++;
            } else {
                dineInCount++;
            }

            if ("paid".equals(order.getStatus())) {
                paidOrders++;
            } else if ("confirmed".equals(order.getStatus())) {
                confirmedCount++;
            } else {
                createdCount++;
            }

            for (LocalOrderItem item : order.getItems()) {
                String name = item.getProductName() == null ? "-" : item.getProductName();
                productCount.put(name, productCount.getOrDefault(name, 0) + item.getQty());
            }
        }

        tvMetricTotalOrders.setText(String.valueOf(totalOrders));
        tvMetricPaidOrders.setText(String.valueOf(paidOrders));
        tvMetricRevenue.setText(MoneyFormatter.format(revenue));
        tvMetricAverage.setText(MoneyFormatter.format(revenue / Math.max(1, totalOrders)));
        tvSummary.setText("Online: " + onlineCount + "\nTại chỗ: " + dineInCount + "\nMang về: " + takeawayCount);
        tvPaymentSummary.setText("Đã thanh toán: " + paidOrders + "\nĐang làm: " + confirmedCount + "\nChờ xác nhận: " + createdCount);

        donutOrderTypeRange.setCenterText("Đơn lọc", String.valueOf(totalOrders));
        donutOrderTypeRange.setSegments(buildSegments(
                new DonutChartView.Segment("Online", onlineCount, ContextCompat.getColor(this, R.color.dashboard_success)),
                new DonutChartView.Segment("Tại chỗ", dineInCount, ContextCompat.getColor(this, R.color.dashboard_primary)),
                new DonutChartView.Segment("Mang về", takeawayCount, ContextCompat.getColor(this, R.color.dashboard_accent))
        ));

        donutPaymentRange.setCenterText("Đã thanh toán", DonutChartView.formatPercent(paidOrders, Math.max(1, totalOrders)));
        donutPaymentRange.setSegments(buildSegments(
                new DonutChartView.Segment("Đã thanh toán", paidOrders, ContextCompat.getColor(this, R.color.dashboard_success)),
                new DonutChartView.Segment("Đang làm", confirmedCount, ContextCompat.getColor(this, R.color.dashboard_warning)),
                new DonutChartView.Segment("Chờ xác nhận", createdCount, ContextCompat.getColor(this, R.color.dashboard_primary))
        ));

        renderTopProducts(productCount);
        tvForecast.setText(buildForecast(productCount, startMillis, endMillis));
        renderOrderDetails(filteredOrders);
        currentReportText = buildReportText(startMillis, endMillis, totalOrders, paidOrders, revenue, onlineCount, dineInCount, takeawayCount, productCount);
    }

    private void exportPdf() {
        try {
            latestExportedReport = ReportExportHelper.exportSimplePdf(
                    this,
                    "cfplus-report-" + System.currentTimeMillis() + ".pdf",
                    "CFPLUS Report",
                    currentReportText.isEmpty() ? "Chưa có dữ liệu để xuất." : currentReportText
            );
            Toast.makeText(this, "Đã lưu PDF vào " + latestExportedReport.displayPath, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Không thể xuất PDF lúc này: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareReport() {
        if (latestExportedReport == null) {
            exportPdf();
        }
        if (latestExportedReport != null) {
            ReportExportHelper.shareUri(this, latestExportedReport.uri, "application/pdf");
        }
    }

    private String buildReportText(long startMillis,
                                   long endMillis,
                                   int totalOrders,
                                   int paidOrders,
                                   int revenue,
                                   int onlineCount,
                                   int dineInCount,
                                   int takeawayCount,
                                   Map<String, Integer> productCount) {
        StringBuilder builder = new StringBuilder();
        builder.append("Khoảng thời gian: ").append(formatDate(startMillis)).append(" - ").append(formatDate(endMillis)).append("\n");
        builder.append("Tổng đơn: ").append(totalOrders).append("\n");
        builder.append("Đơn đã thanh toán: ").append(paidOrders).append("\n");
        builder.append("Doanh thu: ").append(MoneyFormatter.format(revenue)).append("\n");
        builder.append("Online: ").append(onlineCount).append("\n");
        builder.append("Tại chỗ: ").append(dineInCount).append("\n");
        builder.append("Mang về: ").append(takeawayCount).append("\n\nTop món:\n");

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(productCount.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int top = Math.min(5, entries.size());
        for (int i = 0; i < top; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            builder.append(i + 1).append(". ").append(entry.getKey()).append(" - ").append(entry.getValue()).append(" lượt\n");
        }
        builder.append("\n").append(buildForecast(productCount, startMillis, endMillis));
        return builder.toString();
    }

    private String buildForecast(Map<String, Integer> productCount, long startMillis, long endMillis) {
        int days = Math.max(1, (int) ((endMillis - startMillis) / (24L * 60L * 60L * 1000L)) + 1);
        int coffeeBase = 0;
        int milkBase = 0;
        int teaBase = 0;
        int iceBase = 0;
        for (Map.Entry<String, Integer> entry : productCount.entrySet()) {
            String lower = entry.getKey().toLowerCase(Locale.getDefault());
            int qty = entry.getValue();
            if (lower.contains("cà phê") || lower.contains("coffee") || lower.contains("latte") || lower.contains("cap")) {
                coffeeBase += qty;
            }
            if (lower.contains("sữa") || lower.contains("latte") || lower.contains("cap")) {
                milkBase += qty;
            }
            if (lower.contains("trà") || lower.contains("tea")) {
                teaBase += qty;
            }
            if (lower.contains("đá") || lower.contains("ice") || lower.contains("trà")) {
                iceBase += qty;
            }
        }

        int nextWeekCoffee = Math.round((coffeeBase / (float) days) * 7f * 1.1f);
        int nextWeekMilk = Math.round((milkBase / (float) days) * 7f * 1.1f);
        int nextWeekTea = Math.round((teaBase / (float) days) * 7f * 1.1f);
        int nextWeekIce = Math.round((iceBase / (float) days) * 7f * 1.1f);

        return "Gợi ý nhập thêm tuần tới:\n"
                + "- Cà phê nền: " + nextWeekCoffee + " ly dự kiến\n"
                + "- Sữa / kem sữa: " + nextWeekMilk + " phần dự kiến\n"
                + "- Trà nền: " + nextWeekTea + " ly dự kiến\n"
                + "- Đá lạnh / đồ uống lạnh: " + nextWeekIce + " ly dự kiến\n"
                + "Cách tính: moving average theo số bán trong khoảng lọc, cộng đệm 10%.";
    }

    private boolean matchesFilter(LocalOrder order, int filterPosition) {
        if (filterPosition == 1) {
            return "online".equals(order.getOrderType()) || "customer_app".equals(order.getOrderChannel());
        }
        if (filterPosition == 2) {
            return "dine_in".equals(order.getOrderType());
        }
        if (filterPosition == 3) {
            return "takeaway".equals(order.getOrderType());
        }
        return true;
    }

    private void renderTopProducts(Map<String, Integer> productCount) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(productCount.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        StringBuilder sb = new StringBuilder();
        int top = Math.min(5, list.size());
        for (int i = 0; i < top; i++) {
            Map.Entry<String, Integer> entry = list.get(i);
            sb.append(i + 1).append(". ").append(entry.getKey()).append(" - ").append(entry.getValue()).append(" lượt\n");
        }
        tvTopProducts.setText(sb.length() == 0 ? "-" : sb.toString().trim());
    }

    private void renderOrderDetails(List<LocalOrder> orders) {
        orderDetailContainer.removeAllViews();
        if (orders.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Không có đơn hàng phù hợp với bộ lọc hiện tại.");
            emptyView.setTextColor(getColor(R.color.dashboard_text_secondary));
            emptyView.setTextSize(15f);
            orderDetailContainer.addView(emptyView);
            return;
        }

        int limit = Math.min(20, orders.size());
        for (int i = 0; i < limit; i++) {
            LocalOrder order = orders.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.manager_search_background);
            card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dpToPx(i == 0 ? 0 : 12);
            card.setLayoutParams(cardParams);

            TextView tvCode = new TextView(this);
            tvCode.setText("Mã đơn: " + order.getDisplayOrderCode());
            tvCode.setTextColor(getColor(R.color.dashboard_text_primary));
            tvCode.setTextSize(17f);
            tvCode.setTypeface(tvCode.getTypeface(), Typeface.BOLD);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("Loại: " + mapOrderType(order.getOrderType()) + " | Kênh: " + mapOrderChannel(order.getOrderChannel()) + " | Trạng thái: " + mapOrderStatus(order.getStatus()));
            tvMeta.setTextColor(getColor(R.color.dashboard_text_secondary));
            tvMeta.setTextSize(14f);

            TextView tvTime = new TextView(this);
            tvTime.setText("Thời gian: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())));
            tvTime.setTextColor(getColor(R.color.dashboard_text_secondary));
            tvTime.setTextSize(14f);

            TextView tvTotal = new TextView(this);
            tvTotal.setText("Tổng tiền: " + MoneyFormatter.format(order.getTotal()));
            tvTotal.setTextColor(getColor(R.color.dashboard_primary));
            tvTotal.setTextSize(16f);
            tvTotal.setTypeface(tvTotal.getTypeface(), Typeface.BOLD);

            TextView tvItems = new TextView(this);
            tvItems.setText(buildOrderItemsText(order));
            tvItems.setTextColor(getColor(R.color.dashboard_text_primary));
            tvItems.setTextSize(14f);

            card.addView(tvCode);
            addMarginTop(tvMeta, 6);
            addMarginTop(tvTime, 4);
            addMarginTop(tvTotal, 8);
            addMarginTop(tvItems, 10);
            card.addView(tvMeta);
            card.addView(tvTime);
            if (!TextUtils.isEmpty(order.getDeliveryAddressText())) {
                TextView tvAddress = new TextView(this);
                tvAddress.setText("Địa chỉ giao: " + order.getDeliveryAddressText());
                tvAddress.setTextColor(getColor(R.color.dashboard_text_secondary));
                tvAddress.setTextSize(14f);
                addMarginTop(tvAddress, 4);
                card.addView(tvAddress);
            }
            card.addView(tvTotal);
            card.addView(tvItems);
            orderDetailContainer.addView(card);
        }
    }

    private void addMarginTop(View view, int dp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dpToPx(dp);
        view.setLayoutParams(params);
    }

    private String buildOrderItemsText(LocalOrder order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "Chưa có chi tiết món.";
        }
        StringBuilder builder = new StringBuilder("Chi tiết món:\n");
        for (LocalOrderItem item : order.getItems()) {
            builder.append("- ").append(item.getProductName()).append(" x").append(item.getQty()).append(" | ").append(MoneyFormatter.format(item.getLineTotal()));
            if (!TextUtils.isEmpty(item.getVariantName())) {
                builder.append(" (").append(item.getVariantName()).append(")");
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    @Nullable
    private Long parseDate(String value, boolean endOfDay) {
        try {
            Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(value);
            if (date == null) {
                return null;
            }
            long millis = date.getTime();
            return endOfDay ? millis + 24L * 60L * 60L * 1000L - 1L : millis;
        } catch (Exception e) {
            return null;
        }
    }

    private String mapOrderType(String orderType) {
        if ("online".equals(orderType)) return "Online";
        if ("takeaway".equals(orderType)) return "Mang về";
        if ("dine_in".equals(orderType)) return "Tại chỗ";
        return "Khác";
    }

    private String mapOrderChannel(String channel) {
        if ("customer_app".equals(channel)) return "Khách app";
        if ("customer_qr".equals(channel)) return "Khách QR";
        if ("staff_pos".equals(channel)) return "Nhân viên";
        return "Khác";
    }

    private String mapOrderStatus(String status) {
        if ("created".equals(status)) return "Chờ xác nhận";
        if ("confirmed".equals(status)) return "Đang làm";
        if ("paid".equals(status)) return "Đã thanh toán";
        if ("cancelled".equals(status)) return "Đã hủy";
        return status;
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }

    private List<DonutChartView.Segment> buildSegments(DonutChartView.Segment... segments) {
        List<DonutChartView.Segment> list = new ArrayList<>();
        for (DonutChartView.Segment segment : segments) {
            list.add(segment);
        }
        return list;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

