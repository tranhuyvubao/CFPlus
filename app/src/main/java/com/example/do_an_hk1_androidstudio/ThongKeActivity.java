package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThongKeActivity extends AppCompatActivity {

    private TextView tvTongDon;
    private TextView tvTongTien;
    private TextView tvTongSanPham;
    private TextView tvGiaTriTrungBinh;
    private TextView tvChannelSummary;
    private LinearLayout chartContainerToday;
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_thong_ke);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerThongKe));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootThongKe));

        View tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        tvTongDon = findViewById(R.id.tvTongDon);
        tvTongTien = findViewById(R.id.tvTongTien);
        tvTongSanPham = findViewById(R.id.tvTongSanPham);
        tvGiaTriTrungBinh = findViewById(R.id.tvGiaTriTrungBinh);
        tvChannelSummary = findViewById(R.id.tvChannelSummary);
        chartContainerToday = findViewById(R.id.chartContainerToday);

        listenTodayStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }

    private void listenTodayStats() {
        if (ordersListener != null) {
            ordersListener.remove();
        }
        ordersListener = new OrderCloudRepository(this).listenAllOrders(this::bindStats);
    }

    private void bindStats(List<LocalOrder> orders) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endOfDay = cal.getTimeInMillis();

        int tongDon = 0;
        int tongTien = 0;
        int tongSanPham = 0;
        int takeAway = 0;
        int taiCho = 0;
        int quaApp = 0;
        LinkedHashMap<String, Integer> doanhThuTheoKhungGio = new LinkedHashMap<>();
        doanhThuTheoKhungGio.put("06-09", 0);
        doanhThuTheoKhungGio.put("09-12", 0);
        doanhThuTheoKhungGio.put("12-15", 0);
        doanhThuTheoKhungGio.put("15-18", 0);
        doanhThuTheoKhungGio.put("18-21", 0);
        doanhThuTheoKhungGio.put("21-24", 0);

        for (LocalOrder order : orders) {
            long createdAt = order.getCreatedAtMillis();
            if (createdAt < startOfDay || createdAt > endOfDay || "cancelled".equals(order.getStatus())) {
                continue;
            }

            tongDon++;
            tongTien += order.getTotal();
            for (LocalOrderItem item : order.getItems()) {
                tongSanPham += item.getQty();
            }

            if ("takeaway".equals(order.getOrderType())) {
                takeAway++;
            } else if ("online".equals(order.getOrderType())) {
                quaApp++;
            } else {
                taiCho++;
            }

            Calendar orderTime = Calendar.getInstance();
            orderTime.setTimeInMillis(createdAt);
            String slot = mapHourSlot(orderTime.get(Calendar.HOUR_OF_DAY));
            doanhThuTheoKhungGio.put(slot, doanhThuTheoKhungGio.get(slot) + order.getTotal());
        }

        tvTongDon.setText(String.valueOf(tongDon));
        tvTongTien.setText(formatMoney(tongTien));
        tvTongSanPham.setText(String.valueOf(tongSanPham));
        tvGiaTriTrungBinh.setText(formatMoney(tongDon == 0 ? 0 : tongTien / tongDon));
        tvChannelSummary.setText("Tại chỗ: " + taiCho + " đơn\nMang về: " + takeAway + " đơn\nQua app: " + quaApp + " đơn");
        renderBarChart(chartContainerToday, doanhThuTheoKhungGio);
    }

    private String mapHourSlot(int hour) {
        if (hour < 9) return "06-09";
        if (hour < 12) return "09-12";
        if (hour < 15) return "12-15";
        if (hour < 18) return "15-18";
        if (hour < 21) return "18-21";
        return "21-24";
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
