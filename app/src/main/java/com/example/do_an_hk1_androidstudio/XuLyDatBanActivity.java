package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalTableReservation;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class XuLyDatBanActivity extends AppCompatActivity {

    private final List<LocalTableReservation> reservations = new ArrayList<>();

    private TableCloudRepository repository;
    private ListenerRegistration reservationListener;
    private ReservationAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_xu_ly_dat_ban);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerReservations));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootReservations));

        String role = new LocalSessionManager(this).getCurrentUserRole();
        if (!"manager".equals(role) && !"staff".equals(role)) {
            finish();
            return;
        }

        repository = new TableCloudRepository(this);
        tvEmpty = findViewById(R.id.tvReservationsEmpty);
        RecyclerView rvReservations = findViewById(R.id.rvReservations);
        rvReservations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReservationAdapter();
        rvReservations.setAdapter(adapter);

        findViewById(R.id.btnBackReservations).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reservationListener != null) {
            reservationListener.remove();
        }
        reservationListener = repository.listenReservations(fetched -> runOnUiThread(() -> {
            reservations.clear();
            for (LocalTableReservation reservation : fetched) {
                if (!"cancelled".equals(reservation.getStatus()) && !"completed".equals(reservation.getStatus())) {
                    reservations.add(reservation);
                }
            }
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(reservations.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reservationListener != null) {
            reservationListener.remove();
            reservationListener = null;
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0L) {
            return "--";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    private String mapStatus(String status) {
        if ("seated".equals(status)) {
            return "Đã vào bàn";
        }
        if ("reserved".equals(status)) {
            return "Đang chờ khách";
        }
        return "Không xác định";
    }

    private void handleResult(boolean success, @Nullable String message, String successText) {
        Toast.makeText(this, success ? successText : (message == null ? "Thao tác thất bại." : message), Toast.LENGTH_SHORT).show();
    }

    private class ReservationAdapter extends RecyclerView.Adapter<ReservationViewHolder> {
        @NonNull
        @Override
        public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff_reservation, parent, false);
            return new ReservationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
            holder.bind(reservations.get(position));
        }

        @Override
        public int getItemCount() {
            return reservations.size();
        }
    }

    private class ReservationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCustomer;
        private final TextView tvTable;
        private final TextView tvTime;
        private final TextView tvGuests;
        private final TextView tvPhone;
        private final TextView tvNote;
        private final TextView tvStatus;
        private final TextView btnConfirm;
        private final TextView btnCancel;
        private final TextView btnComplete;

        ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomer = itemView.findViewById(R.id.tvReservationCustomer);
            tvTable = itemView.findViewById(R.id.tvReservationTable);
            tvTime = itemView.findViewById(R.id.tvReservationTime);
            tvGuests = itemView.findViewById(R.id.tvReservationGuests);
            tvPhone = itemView.findViewById(R.id.tvReservationPhone);
            tvNote = itemView.findViewById(R.id.tvReservationNote);
            tvStatus = itemView.findViewById(R.id.tvReservationStatus);
            btnConfirm = itemView.findViewById(R.id.btnReservationConfirm);
            btnCancel = itemView.findViewById(R.id.btnReservationCancel);
            btnComplete = itemView.findViewById(R.id.btnReservationComplete);
        }

        void bind(LocalTableReservation reservation) {
            tvCustomer.setText(TextUtils.isEmpty(reservation.getCustomerName()) ? "Khách chưa đặt tên" : reservation.getCustomerName());
            tvTable.setText("Bàn: " + (TextUtils.isEmpty(reservation.getTableName()) ? "Chưa xếp bàn" : reservation.getTableName()));
            tvTime.setText("Giờ đến: " + formatTime(reservation.getReservationTimeMillis()));
            tvGuests.setText("Số khách: " + reservation.getGuestCount());
            tvPhone.setText("Điện thoại: " + (TextUtils.isEmpty(reservation.getCustomerPhone()) ? "--" : reservation.getCustomerPhone()));
            tvNote.setText("Ghi chú: " + (TextUtils.isEmpty(reservation.getNote()) ? "Không có" : reservation.getNote()));
            tvStatus.setText(mapStatus(reservation.getStatus()));

            boolean seated = "seated".equals(reservation.getStatus());
            btnConfirm.setVisibility(seated ? View.GONE : View.VISIBLE);
            btnCancel.setVisibility(seated ? View.VISIBLE : View.VISIBLE);
            btnComplete.setVisibility(seated ? View.VISIBLE : View.GONE);
            btnCancel.setText(seated ? "Trả bàn" : "Hủy");

            btnConfirm.setOnClickListener(v -> repository.confirmReservation(
                    reservation.getReservationId(),
                    reservation.getTableId(),
                    (success, message) -> runOnUiThread(() ->
                            handleResult(success, message, "Đã xác nhận khách vào bàn."))));

            btnCancel.setOnClickListener(v -> {
                if (seated) {
                    repository.completeReservation(
                            reservation.getReservationId(),
                            reservation.getTableId(),
                            (success, message) -> runOnUiThread(() ->
                                    handleResult(success, message, "Đã trả bàn và kết thúc đặt chỗ.")));
                } else {
                    repository.cancelReservation(
                            reservation.getReservationId(),
                            reservation.getTableId(),
                            (success, message) -> runOnUiThread(() ->
                                    handleResult(success, message, "Đã hủy đặt bàn.")));
                }
            });

            btnComplete.setOnClickListener(v -> repository.completeReservation(
                    reservation.getReservationId(),
                    reservation.getTableId(),
                    (success, message) -> runOnUiThread(() ->
                            handleResult(success, message, "Đã hoàn tất đặt bàn."))));
        }
    }
}
