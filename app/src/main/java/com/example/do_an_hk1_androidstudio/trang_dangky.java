package com.example.do_an_hk1_androidstudio;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.mail.MessagingException;

public class trang_dangky extends MainActivity {
    EditText edtemail;//TextInputEditText
    TextInputEditText edtmk, edtmklai;
    TextView testEmail, testMk, testMklai;
    TextView tvDK, tvDN;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dangky);

        mAuth = FirebaseAuth.getInstance();

        edtemail = findViewById(R.id.edtemail);
        edtmk = findViewById(R.id.edtmk);
        edtmklai = findViewById(R.id.edtmklai);

        testEmail = findViewById(R.id.testEmail);
        testMk = findViewById(R.id.testMk);
        testMklai = findViewById(R.id.testMklai);

        tvDK = findViewById(R.id.tvDK);
        tvDN = findViewById(R.id.tvDN);

        // Ẩn các thông báo lỗi ban đầu
        testEmail.setVisibility(View.GONE);
        testMk.setVisibility(View.GONE);
        testMklai.setVisibility(View.GONE);
        
        // Xử lý khi nhấn nút "Đăng nhập" để quay về màn hình đăng nhập
        tvDN.setOnClickListener(view -> {
            Intent intent = new Intent(trang_dangky.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Xử lý khi nhấn nút Đăng ký
        tvDK.setOnClickListener(view -> {
            String email = edtemail.getText().toString().trim();
            String password = edtmk.getText().toString().trim();
            String passwordAgain = edtmklai.getText().toString().trim();

            boolean isValid = true;

            // Kiểm tra email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                testEmail.setText("Email không đúng định dạng!");
                testEmail.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                testEmail.setVisibility(View.GONE);
            }

            // Kiểm tra mật khẩu
            if (password.length() < 6 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
                testMk.setText("Mật khẩu phải từ 6 ký tự, bao gồm chữ và số!");
                testMk.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                testMk.setVisibility(View.GONE);
            }

            // Kiểm tra nhập lại mật khẩu
            if (!password.equals(passwordAgain)) {
                testMklai.setText("Mật khẩu nhập lại không khớp!");
                testMklai.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                testMklai.setVisibility(View.GONE);
            }

            // Nếu hợp lệ thì hiển thị Dialog xác thực OTP
            if (isValid) {
                showOTPDialog(email);
            }
        });
    }

    private void showOTPDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.xacthuc_otp_dangnhap, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Ánh xạ
        EditText edtotp = dialogView.findViewById(R.id.edtotp);
        TextView tvXacNhan = dialogView.findViewById(R.id.tvXacnhan);
        TextView tvDemGiay = dialogView.findViewById(R.id.testdemgiay);

        // Tạo OTP ngẫu nhiên 6 chữ số
        String otp = String.format("%06d", new Random().nextInt(1000000));

        // Gửi OTP qua email (nếu bạn dùng Firebase hoặc SMTP thì thêm tại đây)
        sendOtpEmail(email, otp); // Bạn cần triển khai hàm này hoặc tích hợp SMTP / server riêng

        // Bắt đầu đếm ngược 2 phút
        new CountDownTimer(2 * 60 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvDemGiay.setText("Mã OTP sẽ hết hạn sau: " + seconds + " giây");
            }

            public void onFinish() {
                tvDemGiay.setText("Mã OTP đã hết hạn. Vui lòng nhận lại mã mới.");
                tvXacNhan.setEnabled(false);
            }
        }.start();

        // Hiển thị dialog
        dialog.show();

        // Xử lý xác nhận OTP
        tvXacNhan.setOnClickListener(v -> {
            String userOtp = edtotp.getText().toString().trim();
            if (userOtp.equals(otp)) {
                Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // Đăng ký tài khoản Firebase
                registerWithFirebase(email, edtmk.getText().toString().trim());
            } else {
                edtotp.setError("Mã OTP không chính xác!");
            }
        });
    }

    private void sendOtpEmail(String email, String otp) {
        new Thread(() -> {
            try {
                // Thay đổi bằng email và AppPassword của bạn
                // Lưu ý: App Password phải được tạo từ Google Account Settings
                // Loại bỏ khoảng trắng trong App Password nếu có
                String appPassword = "rqbo wmyi bkso qctq".replaceAll("\\s+", "");
                GmailSender sender = new GmailSender("dangkhoasny@gmail.com", appPassword);

                String subject = "Xác thực OTP cho CFPLUScafe";
                String message = "Xin chào,\n\nMã OTP của bạn là: " + otp + "\nMã này sẽ hết hạn sau 2 phút.";

                sender.sendEmail(email, subject, message);

                Log.d("OTP", "Gửi email thành công");
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã gửi mã OTP đến email của bạn!", Toast.LENGTH_SHORT).show();
                });
            } catch (MessagingException e) {
                Log.e("OTP", "Gửi email thất bại: " + e.getMessage());
                runOnUiThread(() -> {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("535")) {
                        Toast.makeText(this, "Lỗi xác thực email. Vui lòng kiểm tra App Password!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Không thể gửi email: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("OTP", "Gửi email thất bại: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void registerWithFirebase(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng ký thành công
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Lưu thông tin user vào Firestore
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("uid", user.getUid());
                            userData.put("createdAt", com.google.firebase.Timestamp.now());
                            
                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Register", "User data saved to Firestore");
                                        
                                        // Đăng xuất để user có thể đăng nhập lại
                                        mAuth.signOut();
                                        
                                        // Hiển thị thông báo và chuyển về màn hình đăng nhập
                                        Toast.makeText(trang_dangky.this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                                        
                                        Intent intent = new Intent(trang_dangky.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish(); // Đóng trang đăng ký
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Register", "Error saving user data", e);
                                        // Vẫn đăng xuất và chuyển về màn hình đăng nhập
                                        mAuth.signOut();
                                        Toast.makeText(trang_dangky.this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(trang_dangky.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            // Nếu không lấy được user, vẫn chuyển về màn hình đăng nhập
                            Toast.makeText(trang_dangky.this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(trang_dangky.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // Nếu đăng ký thất bại
                        Toast.makeText(trang_dangky.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
