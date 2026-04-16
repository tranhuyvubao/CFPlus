package com.example.do_an_hk1_androidstudio;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
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

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
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
    private TextView tvTopProducts;
    private TextView tvMetricTotalOrders;
    private TextView tvMetricPaidOrders;
    private TextView tvMetricRevenue;
    private TextView tvMetricAverage;
    private LinearLayout chartContainerRange;
    private LinearLayout orderDetailContainer;
    private final List<LocalOrder> allOrders = new ArrayList<>();
    private ListenerRegistration ordersListener;

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
        tvSummary = findViewById(R.id.tvSummary);
        tvTopProducts = findViewById(R.id.tvTopProducts);
        tvMetricTotalOrders = findViewById(R.id.tvMetricTotalOrders);
        tvMetricPaidOrders = findViewById(R.id.tvMetricPaidOrders);
        tvMetricRevenue = findViewById(R.id.tvMetricRevenue);
        tvMetricAverage = findViewById(R.id.tvMetricAverage);
        chartContainerRange = findViewById(R.id.chartContainerRange);
        orderDetailContainer = findViewById(R.id.orderDetailContainer);

        bindDatePicker(edtStart);
        bindDatePicker(edtEnd);
        setupFilterSpinner();
        btnLoad.setOnClickListener(v -> loadStats());

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
        int tongDon = 0;
        int tongDonPaid = 0;
        int tongTien = 0;
        int onlineCount = 0;
        int dineInCount = 0;
        int takeawayCount = 0;
        Map<String, Integer> productCount = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> revenueByDay = new LinkedHashMap<>();

        for (LocalOrder order : allOrders) {
            long createdAt = order.getCreatedAtMillis();
            if (createdAt < startMillis || createdAt > endMillis || "cancelled".equals(order.getStatus())) {
                continue;
            }

            if ("online".equals(order.getOrderType()) || "customer_app".equals(order.getOrderChannel())) {
                onlineCount++;
            } else if ("takeaway".equals(order.getOrderType())) {
                takeawayCount++;
            } else {
                dineInCount++;
            }

            if (!matchesFilter(order, spOrderFilter.getSelectedItemPosition())) {
                continue;
            }

            filteredOrders.add(order);
            tongDon++;
            if ("paid".equals(order.getStatus())) {
                tongDonPaid++;
            }
            tongTien += order.getTotal();

            String dayKey = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(createdAt));
            revenueByDay.put(dayKey, revenueByDay.getOrDefault(dayKey, 0) + order.getTotal());

            for (LocalOrderItem item : order.getItems()) {
                String name = item.getProductName() == null ? "-" : item.getProductName();
                productCount.put(name, productCount.getOrDefault(name, 0) + item.getQty());
            }
        }

        tvMetricTotalOrders.setText(String.valueOf(tongDon));
        tvMetricPaidOrders.setText(String.valueOf(tongDonPaid));
        tvMetricRevenue.setText(formatMoney(tongTien));
        tvMetricAverage.setText(formatMoney(tongTien / Math.max(1, tongDon)));
        tvSummary.setText("Online: " + onlineCount
                + " | Tại chỗ: " + dineInCount
                + " | Mang về: " + takeawayCount
                + " | Tỷ lệ thanh toán: " + (tongDon == 0 ? 0 : (tongDonPaid * 100 / tongDon)) + "%");

        renderTopProducts(productCount);
        renderBarChart(chartContainerRange, revenueByDay);
        renderOrderDetails(filteredOrders);
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
            emptyView.setTextColor(getColor(R.color.coffee_muted));
            emptyView.setTextSize(15f);
            orderDetailContainer.addView(emptyView);
            return;
        }

        int limit = Math.min(20, orders.size());
        for (int i = 0; i < limit; i++) {
            LocalOrder order = orders.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.app_card_background);
            card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardParams.topMargin = dpToPx(i == 0 ? 0 : 12);
            card.setLayoutParams(cardParams);

            TextView tvCode = new TextView(this);
            tvCode.setText("Mã đơn: " + order.getDisplayOrderCode());
            tvCode.setTextColor(getColor(R.color.coffee_dark));
            tvCode.setTextSize(17f);
            tvCode.setTypeface(tvCode.getTypeface(), Typeface.BOLD);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("Loại: " + mapOrderType(order.getOrderType())
                    + " | Kênh: " + mapOrderChannel(order.getOrderChannel())
                    + " | Trạng thái: " + mapOrderStatus(order.getStatus()));
            tvMeta.setTextColor(getColor(R.color.coffee_muted));
            tvMeta.setTextSize(14f);

            TextView tvTime = new TextView(this);
            tvTime.setText("Thời gian: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())));
            tvTime.setTextColor(getColor(R.color.coffee_muted));
            tvTime.setTextSize(14f);

            TextView tvTotal = new TextView(this);
            tvTotal.setText("Tổng tiền: " + formatMoney(order.getTotal()));
            tvTotal.setTextColor(getColor(R.color.coffee_primary_dark));
            tvTotal.setTextSize(16f);
            tvTotal.setTypeface(tvTotal.getTypeface(), Typeface.BOLD);

            TextView tvItems = new TextView(this);
            tvItems.setText(buildOrderItemsText(order));
            tvItems.setTextColor(getColor(R.color.coffee_text));
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
                tvAddress.setTextColor(getColor(R.color.coffee_muted));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(dp);
        view.setLayoutParams(params);
    }

    private String buildOrderItemsText(LocalOrder order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "Chưa có chi tiết món.";
        }
        StringBuilder builder = new StringBuilder("Chi tiết món:\n");
        for (LocalOrderItem item : order.getItems()) {
            builder.append("- ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQty())
                    .append(" | ")
                    .append(formatMoney(item.getLineTotal()));
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

    private void renderBarChart(LinearLayout container, LinkedHashMap<String, Integer> values) {
        container.removeAllViews();
        int max = 0;
        for (int value : values.values()) {
            max = Math.max(max, value);
        }
        int chartHeight = dpToPx(150);

        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            columnParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            column.setLayoutParams(columnParams);

            TextView valueView = new TextView(this);
            valueView.setText(entry.getValue() == 0 ? "0" : shortMoney(entry.getValue()));
            valueView.setTextColor(getColor(R.color.coffee_muted));
            valueView.setTextSize(12f);

            LinearLayout track = new LinearLayout(this);
            track.setBackgroundResource(R.drawable.app_chart_bar_track);
            track.setGravity(Gravity.BOTTOM);
            LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(dpToPx(30), chartHeight);
            trackParams.topMargin = dpToPx(8);
            track.setLayoutParams(trackParams);
            track.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            View bar = new View(this);
            bar.setBackgroundResource(R.drawable.app_chart_bar_fill);
            int barHeight = max == 0 ? dpToPx(8) : Math.max(dpToPx(8), (int) ((entry.getValue() / (float) max) * (chartHeight - dpToPx(8))));
            bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barHeight));
            track.addView(bar);

            TextView labelView = new TextView(this);
            labelView.setText(entry.getKey());
            labelView.setTextColor(getColor(R.color.coffee_dark));
            labelView.setTextSize(12f);
            labelView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dpToPx(8);
            labelView.setLayoutParams(labelParams);

            column.addView(valueView);
            column.addView(track);
            column.addView(labelView);
            container.addView(column);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String formatMoney(int amount) {
        return MoneyFormatter.format(amount);
    }

    private String shortMoney(int amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1ftr", amount / 1_000_000f);
        }
        if (amount >= 1_000) {
            return String.format(Locale.getDefault(), "%.0fk", amount / 1_000f);
        }
        return String.valueOf(amount);
    }
}
