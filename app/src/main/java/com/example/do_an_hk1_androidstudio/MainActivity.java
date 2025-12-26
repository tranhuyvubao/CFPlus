package com.example.do_an_hk1_androidstudio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    TextView tvDK, tvDN, tvQuenmk;
    private FirebaseAuth mAuth;
    private EditText editemail, edtmk;
    private TextView testMkDn;
    private android.widget.CheckBox autofill;
    private int failedAttempts = 0;
    private boolean isLocked = false;
    private long lockTime = 60000; // 1 phút = 60000 ms
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        tvDK = findViewById(R.id.tvDK);
        editemail = findViewById(R.id.editemail);
        edtmk = findViewById(R.id.edtmk);
        tvDN = findViewById(R.id.tvDN);
        tvQuenmk = findViewById(R.id.tvQuenmk);
        testMkDn = findViewById(R.id.testMkDn); // Khởi tạo testMkDn
        autofill = findViewById(R.id.autofill);

        // Khôi phục trạng thái từ SharedPreferences
        failedAttempts = sharedPreferences.getInt("failedAttempts", 0);
        long lockStartTime = sharedPreferences.getLong("lockStartTime", 0);
        if (lockStartTime > 0) {
            long elapsedTime = System.currentTimeMillis() - lockStartTime;
            if (elapsedTime < lockTime) {
                isLocked = true;
                tvDN.setEnabled(false);
                testMkDn.setVisibility(View.VISIBLE);
                startCountdown(lockTime - elapsedTime);
            } else {
                // Đã hết thời gian khóa, reset
                resetLock();
            }
        }
        
        // Khôi phục email và mật khẩu đã lưu nếu có
        loadSavedCredentials();

        tvDK.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, trang_dangky.class);
            startActivity(intent);
        });

        tvDN.setOnClickListener(view -> login());

        tvQuenmk.setOnClickListener(view -> onClickForgotPassword());
    }

    private void login() {
        String email = editemail.getText().toString().trim();
        String pass = edtmk.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu!", Toast.LENGTH_LONG).show();
            return;
        }

        if (isLocked) {
            Toast.makeText(this, "Tài khoản bị khóa tạm thời! Vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Đăng nhập thành công!", Toast.LENGTH_LONG).show();
                        failedAttempts = 0;
                        editor.putInt("failedAttempts", failedAttempts);
                        
                        // Lưu email và mật khẩu nếu checkbox "Nhớ mật khẩu" được chọn
                        if (autofill.isChecked()) {
                            saveCredentials(email, pass);
                        } else {
                            // Xóa thông tin đã lưu nếu không chọn
                            clearSavedCredentials();
                        }
                        editor.apply();
                        
                        Intent intent = new Intent(MainActivity.this, trangchu.class);
                        startActivity(intent);
                        finish();
                    } else {
                        failedAttempts++;
                        editor.putInt("failedAttempts", failedAttempts);
                        editor.apply();
                        Toast.makeText(MainActivity.this, "Sai thông tin! Lần thứ " + failedAttempts, Toast.LENGTH_SHORT).show();

                        if (failedAttempts >= 3) {
                            lockLogin();
                        }
                    }
                });
    }

    private void lockLogin() {
        isLocked = true;
        tvDN.setEnabled(false);
        testMkDn.setVisibility(View.VISIBLE);
        testMkDn.setText("Bạn đã đăng nhập sai 3 lần. Vui lòng đợi 1 phút...");

        // Lưu thời gian bắt đầu khóa
        long lockStartTime = System.currentTimeMillis();
        editor.putLong("lockStartTime", lockStartTime);
        editor.apply();

        startCountdown(lockTime);
    }

    private void startCountdown(long timeInMillis) {
        new android.os.CountDownTimer(timeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                testMkDn.setText("Bạn đã đăng nhập sai 3 lần. Vui lòng đợi " + seconds + " giây...");
            }

            public void onFinish() {
                resetLock();
            }
        }.start();
    }

    private void resetLock() {
        isLocked = false;
        failedAttempts = 0;
        editor.putInt("failedAttempts", 0);
        editor.putLong("lockStartTime", 0);
        editor.apply();
        tvDN.setEnabled(true);
        testMkDn.setText("Bạn có thể thử đăng nhập lại.");
        testMkDn.setVisibility(View.GONE);
    }
    
    // Lưu email và mật khẩu vào SharedPreferences
    private void saveCredentials(String email, String password) {
        editor.putString("saved_email", email);
        editor.putString("saved_password", password);
        editor.putBoolean("remember_password", true);
        editor.apply();
    }
    
    // Xóa thông tin đã lưu
    private void clearSavedCredentials() {
        editor.remove("saved_email");
        editor.remove("saved_password");
        editor.putBoolean("remember_password", false);
        editor.apply();
    }
    
    // Khôi phục email và mật khẩu đã lưu
    private void loadSavedCredentials() {
        boolean rememberPassword = sharedPreferences.getBoolean("remember_password", false);
        if (rememberPassword) {
            String savedEmail = sharedPreferences.getString("saved_email", "");
            String savedPassword = sharedPreferences.getString("saved_password", "");
            
            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                editemail.setText(savedEmail);
                edtmk.setText(savedPassword);
                autofill.setChecked(true);
            }
        }
    }

    private void onClickForgotPassword() {
        String emailAddress = editemail.getText().toString().trim();
        if (TextUtils.isEmpty(emailAddress)) {
            Toast.makeText(MainActivity.this, "Vui lòng nhập email!", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Đã gửi email khôi phục mật khẩu.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Không gửi được email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(MainActivity.this, "Tìm kiếm: " + query, Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        return true;
    }
}