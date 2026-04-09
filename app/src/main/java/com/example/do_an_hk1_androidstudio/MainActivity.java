package com.example.do_an_hk1_androidstudio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;

public class MainActivity extends AppCompatActivity {

    private TextView tvDK;
    private TextView tvDN;
    private TextView tvQuenmk;
    private EditText editemail;
    private EditText edtmk;
    private TextView testMkDn;
    private android.widget.CheckBox autofill;
    private int failedAttempts = 0;
    private boolean isLocked = false;
    private final long lockTime = 60000;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private UserCloudRepository userRepository;
    private LocalSessionManager sessionManager;

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

        userRepository = new UserCloudRepository(this);
        sessionManager = new LocalSessionManager(this);

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        tvDK = findViewById(R.id.tvDK);
        editemail = findViewById(R.id.editemail);
        edtmk = findViewById(R.id.edtmk);
        tvDN = findViewById(R.id.tvDN);
        tvQuenmk = findViewById(R.id.tvQuenmk);
        testMkDn = findViewById(R.id.testMkDn);
        autofill = findViewById(R.id.autofill);
        ImageView logo = findViewById(R.id.logo);

        if (logo != null) {
            logo.setOnLongClickListener(v -> {
                startActivity(new Intent(MainActivity.this, BootstrapManagerActivity.class));
                return true;
            });
        }

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
                resetLock();
            }
        }

        loadSavedCredentials();

        if (sessionManager.isLoggedIn()) {
            openHomeAndFinish();
            return;
        }

        tvDK.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, trang_dangky.class)));
        tvDN.setOnClickListener(view -> login());
        tvQuenmk.setOnClickListener(view -> onClickForgotPassword());
    }

    private void login() {
        String email = editemail.getText().toString().trim();
        String pass = edtmk.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui long nhap email!", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email khong dung dinh dang!", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Vui long nhap mat khau!", Toast.LENGTH_LONG).show();
            return;
        }
        if (isLocked) {
            Toast.makeText(this, "Tai khoan bi khoa tam thoi. Vui long doi...", Toast.LENGTH_SHORT).show();
            return;
        }

        tvDN.setEnabled(false);
        userRepository.authenticate(email, pass, (user, message) -> {
            tvDN.setEnabled(true);
            if (user != null) {
                failedAttempts = 0;
                editor.putInt("failedAttempts", failedAttempts).apply();
                sessionManager.saveUser(user);

                if (autofill.isChecked()) {
                    saveCredentials(email, pass);
                } else {
                    clearSavedCredentials();
                }

                Toast.makeText(this, "Dang nhap thanh cong! Vai tro: " + user.getRole(), Toast.LENGTH_LONG).show();
                openHomeAndFinish();
                return;
            }

            failedAttempts++;
            editor.putInt("failedAttempts", failedAttempts).apply();
            Toast.makeText(this, message == null ? ("Dang nhap that bai! Lan thu " + failedAttempts) : message, Toast.LENGTH_LONG).show();
            if (failedAttempts >= 3) {
                lockLogin();
            }
        });
    }

    private void lockLogin() {
        isLocked = true;
        tvDN.setEnabled(false);
        testMkDn.setVisibility(View.VISIBLE);
        testMkDn.setText("Ban da dang nhap sai 3 lan. Vui long doi 1 phut...");

        long lockStartTime = System.currentTimeMillis();
        editor.putLong("lockStartTime", lockStartTime).apply();
        startCountdown(lockTime);
    }

    private void startCountdown(long timeInMillis) {
        new android.os.CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                testMkDn.setText("Ban da dang nhap sai 3 lan. Vui long doi " + seconds + " giay...");
            }

            @Override
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
        testMkDn.setVisibility(View.GONE);
    }

    private void saveCredentials(String email, String password) {
        editor.putString("saved_email", email);
        editor.putString("saved_password", password);
        editor.putBoolean("remember_password", true);
        editor.apply();
    }

    private void clearSavedCredentials() {
        editor.remove("saved_email");
        editor.remove("saved_password");
        editor.putBoolean("remember_password", false);
        editor.apply();
    }

    private void loadSavedCredentials() {
        boolean rememberPassword = sharedPreferences.getBoolean("remember_password", false);
        if (!rememberPassword) {
            return;
        }

        String savedEmail = sharedPreferences.getString("saved_email", "");
        String savedPassword = sharedPreferences.getString("saved_password", "");
        if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
            editemail.setText(savedEmail);
            edtmk.setText(savedPassword);
            autofill.setChecked(true);
        }
    }

    private void onClickForgotPassword() {
        String emailAddress = editemail.getText().toString().trim();
        if (TextUtils.isEmpty(emailAddress)) {
            Toast.makeText(this, "Vui long nhap email!", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            Toast.makeText(this, "Email khong dung dinh dang!", Toast.LENGTH_LONG).show();
            return;
        }
        userRepository.emailExists(emailAddress, (exists, message) -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return;
            }
            if (!exists) {
                Toast.makeText(this, "Email khong ton tai trong he thong.", Toast.LENGTH_LONG).show();
                return;
            }

            EditText edtNewPassword = new EditText(this);
            edtNewPassword.setHint("Nhap mat khau moi");
            edtNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Dat lai mat khau")
                    .setView(edtNewPassword)
                    .setPositiveButton("Luu", null)
                    .setNegativeButton("Huy", null)
                    .create();
            dialog.setOnShowListener(listener -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newPassword = edtNewPassword.getText().toString().trim();
                if (newPassword.length() < 6 || !newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
                    Toast.makeText(this, "Mat khau phai tu 6 ky tu, gom chu va so.", Toast.LENGTH_LONG).show();
                    return;
                }
                userRepository.updatePasswordByEmail(emailAddress, newPassword, (success, updateMessage) -> {
                    Toast.makeText(this,
                            success ? "Da cap nhat mat khau tren he thong." : (updateMessage == null ? "Khong cap nhat duoc mat khau." : updateMessage),
                            Toast.LENGTH_LONG).show();
                    if (success) {
                        dialog.dismiss();
                    }
                });
            }));
            dialog.show();
        });
    }

    private void openHomeAndFinish() {
        Intent intent = new Intent(MainActivity.this, trangchu.class);
        startActivity(intent);
        finish();
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
                Toast.makeText(MainActivity.this, "Tim kiem: " + query, Toast.LENGTH_SHORT).show();
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
