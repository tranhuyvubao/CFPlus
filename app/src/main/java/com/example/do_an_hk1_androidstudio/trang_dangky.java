package com.example.do_an_hk1_androidstudio;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Random;

import javax.mail.MessagingException;

public class trang_dangky extends AppCompatActivity {
    private EditText edtemail;
    private EditText edtHoTen;
    private EditText edtSoDienThoai;
    private EditText edtDiaChi;
    private TextInputEditText edtmk;
    private TextInputEditText edtmklai;
    private TextView testEmail;
    private TextView testThongTin;
    private TextView testMk;
    private TextView testMklai;
    private TextView tvDK;
    private TextView tvDN;
    private UserCloudRepository userCloudRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dangky);
        InsetsHelper.applyActivityRootPadding(this);

        userCloudRepository = new UserCloudRepository(this);

        edtemail = findViewById(R.id.edtemail);
        edtHoTen = findViewById(R.id.edtHoTen);
        edtSoDienThoai = findViewById(R.id.edtSoDienThoai);
        edtDiaChi = findViewById(R.id.edtDiaChi);
        edtmk = findViewById(R.id.edtmk);
        edtmklai = findViewById(R.id.edtmklai);
        testEmail = findViewById(R.id.testEmail);
        testThongTin = findViewById(R.id.testThongTin);
        testMk = findViewById(R.id.testMk);
        testMklai = findViewById(R.id.testMklai);
        tvDK = findViewById(R.id.tvDK);
        tvDN = findViewById(R.id.tvDN);

        testEmail.setVisibility(View.GONE);
        testThongTin.setVisibility(View.GONE);
        testMk.setVisibility(View.GONE);
        testMklai.setVisibility(View.GONE);

        tvDN.setOnClickListener(view -> {
            Intent intent = new Intent(trang_dangky.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        tvDK.setOnClickListener(view -> {
            String email = valueOf(edtemail);
            String fullName = valueOf(edtHoTen);
            String phone = valueOf(edtSoDienThoai);
            String address = valueOf(edtDiaChi);
            String password = valueOf(edtmk);
            String passwordAgain = valueOf(edtmklai);

            boolean isValid = true;

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                testEmail.setText("Email không đúng định dạng!");
                testEmail.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                testEmail.setVisibility(View.GONE);
            }

            if (fullName.length() < 2 || !phone.matches("^0\\d{9,10}$") || address.length() < 6) {
                testThongTin.setText("Vui lòng nhập đủ họ tên, số điện thoại hợp lệ và địa chỉ mặc định.");
                testThongTin.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                testThongTin.setVisibility(View.GONE);
            }

            if (password.length() < 6 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
                testMk.setText("Mật khẩu phải từ 6 ký tự, bao gồm chữ và số!");
                testMk.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                testMk.setVisibility(View.GONE);
            }

            if (!password.equals(passwordAgain)) {
                testMklai.setText("Mật khẩu nhập lại không khớp!");
                testMklai.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                testMklai.setVisibility(View.GONE);
            }

            if (!isValid) {
                return;
            }
            userCloudRepository.emailExists(email, (exists, message) -> {
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (exists) {
                    testEmail.setText("Email đã tồn tại!");
                    testEmail.setVisibility(View.VISIBLE);
                    return;
                }
                showOTPDialog(email, password, fullName, phone, address);
            });
        });
    }

    private void showOTPDialog(String email, String password, String fullName, String phone, String address) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.xacthuc_otp_dangnhap, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText edtotp = dialogView.findViewById(R.id.edtotp);
        TextView tvXacNhan = dialogView.findViewById(R.id.tvXacnhan);
        TextView tvDemGiay = dialogView.findViewById(R.id.testdemgiay);

        String otp = String.format("%06d", new Random().nextInt(1000000));
        sendOtpEmail(email, otp);

        new CountDownTimer(2 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvDemGiay.setText("Mã OTP sẽ hết hạn sau: " + seconds + " giây");
            }

            @Override
            public void onFinish() {
                tvDemGiay.setText("Mã OTP đã hết hạn. Vui lòng nhận lại mã mới.");
                tvXacNhan.setEnabled(false);
            }
        }.start();

        dialog.show();

        tvXacNhan.setOnClickListener(v -> {
            String userOtp = edtotp.getText().toString().trim();
            if (userOtp.equals(otp)) {
                Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                registerLocalAccount(email, password, fullName, phone, address);
            } else {
                edtotp.setError("Mã OTP không chính xác!");
            }
        });
    }

    private void sendOtpEmail(String email, String otp) {
        new Thread(() -> {
            try {
                String appPassword = "rqbo wmyi bkso qctq".replaceAll("\\s+", "");
                GmailSender sender = new GmailSender("dangkhoasny@gmail.com", appPassword);
                String subject = "Xác thực OTP cho CFPLUS cafe";
                String message = "Xin chào,\n\nMã OTP của bạn là: " + otp + "\nMã này sẽ hết hạn sau 2 phút.";
                sender.sendEmail(email, subject, message);

                Log.d("OTP", "Gửi email thành công");
                runOnUiThread(() -> Toast.makeText(this, "Đã gửi mã OTP đến email của bạn!", Toast.LENGTH_SHORT).show());
            } catch (MessagingException e) {
                Log.e("OTP", "Gửi email thất bại: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Không thể gửi email: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("OTP", "Gửi email thất bại: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void registerLocalAccount(String email, String password, String fullName, String phone, String address) {
        userCloudRepository.registerCustomer(email, password, fullName, phone, address, (user, message) -> {
            if (user != null) {
                Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(trang_dangky.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            Toast.makeText(this, message == null ? "Đăng ký thất bại." : message, Toast.LENGTH_SHORT).show();
        });
    }

    private String valueOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }
}
