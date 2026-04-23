package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ThongKeActivity extends AppCompatActivity {

    private TextView tvTongDon;
    private TextView tvTongTien;
    private TextView tvTongSanPham;
    private TextView tvGiaTriTrungBinh;
    private TextView tvChannelSummary;
    private TextView tvStatusSummary;
    private DonutChartView donutChannelsToday;
    private DonutChartView donutStatusToday;
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
        tvStatusSummary = findViewById(R.id.tvStatusSummary);
        donutChannelsToday = findViewById(R.id.donutChannelsToday);
        donutStatusToday = findViewById(R.id.donutStatusToday);

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
        int paid = 0;
        int confirmed = 0;
        int created = 0;

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
            } else if ("online".equals(order.getOrderType()) || "customer_app".equals(order.getOrderChannel())) {
                quaApp++;
            } else {
                taiCho++;
            }

            if ("paid".equals(order.getStatus())) {
                paid++;
            } else if ("confirmed".equals(order.getStatus())) {
                confirmed++;
            } else {
                created++;
            }
        }

        tvTongDon.setText(String.valueOf(tongDon));
        tvTongTien.setText(MoneyFormatter.format(tongTien));
        tvTongSanPham.setText(String.valueOf(tongSanPham));
        tvGiaTriTrungBinh.setText(MoneyFormatter.format(tongDon == 0 ? 0 : tongTien / tongDon));

        tvChannelSummary.setText("Tại chỗ: " + taiCho
                + "\nMang về: " + takeAway
                + "\nQua app: " + quaApp);
        tvStatusSummary.setText("Đã thanh toán: " + paid
                + "\nĐang làm: " + confirmed
                + "\nChờ xác nhận: " + created);

        donutChannelsToday.setCenterText("Hôm nay", String.valueOf(tongDon));
        donutChannelsToday.setSegments(buildSegments(
                new DonutChartView.Segment("Tại chỗ", taiCho, ContextCompat.getColor(this, R.color.dashboard_primary)),
                new DonutChartView.Segment("Mang về", takeAway, ContextCompat.getColor(this, R.color.dashboard_accent)),
                new DonutChartView.Segment("Qua app", quaApp, ContextCompat.getColor(this, R.color.dashboard_success))
        ));

        donutStatusToday.setCenterText("Tỷ lệ", DonutChartView.formatPercent(paid, Math.max(1, tongDon)));
        donutStatusToday.setSegments(buildSegments(
                new DonutChartView.Segment("Đã thanh toán", paid, ContextCompat.getColor(this, R.color.dashboard_success)),
                new DonutChartView.Segment("Đang làm", confirmed, ContextCompat.getColor(this, R.color.dashboard_warning)),
                new DonutChartView.Segment("Chờ xác nhận", created, ContextCompat.getColor(this, R.color.dashboard_primary))
        ));
    }

    private List<DonutChartView.Segment> buildSegments(DonutChartView.Segment... segments) {
        List<DonutChartView.Segment> list = new ArrayList<>();
        for (DonutChartView.Segment segment : segments) {
            list.add(segment);
        }
        return list;
    }
}
