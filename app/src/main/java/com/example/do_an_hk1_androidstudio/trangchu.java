package com.example.do_an_hk1_androidstudio;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class trangchu extends AppCompatActivity implements FragmentHistory.OnThemHangClickListener {

    private ViewPager2 viewPager2;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.trangchu);

        LinearLayout headerContainer = findViewById(R.id.headerContainer);
        viewPager2 = findViewById(R.id.viewPager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);
        viewPager2.setVisibility(ViewPager2.VISIBLE);
        viewPager2.setCurrentItem(0, false);

        bottomNavigationView = findViewById(R.id.bottomNav);
        frameLayout = findViewById(R.id.frameLayout);
        InsetsHelper.applyStatusBarPadding(headerContainer);
        InsetsHelper.applyNavigationBarPadding(bottomNavigationView);

        String role = new LocalSessionManager(this).getCurrentUserRole();
        applyBottomNavForRole(role);

        frameLayout.setVisibility(FrameLayout.VISIBLE);
        viewPager2.setVisibility(ViewPager2.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, createHomeFragment(role))
                .commit();

        bottomNavigationView.setSelectedItemId(R.id.bottom_home);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                viewPager2.setVisibility(ViewPager2.GONE);
                frameLayout.setVisibility(FrameLayout.VISIBLE);

                int id = item.getItemId();
                if (id == R.id.bottom_home) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frameLayout, createHomeFragment(new LocalSessionManager(trangchu.this).getCurrentUserRole()))
                            .commit();
                    return true;
                } else if (id == R.id.bottom_history) {
                    String currentRole = new LocalSessionManager(trangchu.this).getCurrentUserRole();
                    if ("staff".equals(currentRole)) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frameLayout, new FragmentOnlineOrders())
                                .commit();
                        return true;
                    }
                    return false;
                } else if (id == R.id.bottom_account) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frameLayout, new Fragment_Account())
                            .commit();
                    return true;
                } else if (id == R.id.bottom_setting) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frameLayout, new Fragment_Setting())
                            .commit();
                    return true;
                }
                return false;
            }
        });

        String fragmentToShow = getIntent().getStringExtra("fragmentToShow");
        if ("account".equals(fragmentToShow)) {
            viewPager2.setCurrentItem(3);
        }

        bindGreeting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindGreeting();
    }

    private void bindGreeting() {
        TextView greetingView = findViewById(R.id.textView2);
        String userId = new LocalSessionManager(this).getCurrentUserId();
        if (userId == null) {
            greetingView.setText("Xin chào, CFPLUS");
            return;
        }

        new UserCloudRepository(this).getUserById(userId, (user, message) -> runOnUiThread(() -> {
            if (user == null) {
                greetingView.setText("Xin chào, CFPLUS");
                return;
            }

            if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
                greetingView.setText("Xin chào, " + user.getFullName());
                return;
            }

            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                greetingView.setText("Xin chào, " + user.getEmail());
                return;
            }

            greetingView.setText("Xin chào!");
        }));
    }

    @Override
    public void onThemHangClick() {
        frameLayout.setVisibility(FrameLayout.GONE);
        viewPager2.setVisibility(ViewPager2.VISIBLE);
        viewPager2.setCurrentItem(0, true);
        bottomNavigationView.setSelectedItemId(R.id.bottom_home);
    }

    private void applyBottomNavForRole(String role) {
        if (bottomNavigationView == null) {
            return;
        }
        android.view.Menu menu = bottomNavigationView.getMenu();
        if (menu == null) {
            return;
        }
        android.view.MenuItem historyItem = menu.findItem(R.id.bottom_history);
        if (historyItem == null) {
            return;
        }
        boolean showHistory = "staff".equals(role);
        historyItem.setVisible(showHistory);
        if (showHistory) {
            historyItem.setTitle("Đơn online");
        }
    }

    private androidx.fragment.app.Fragment createHomeFragment(String role) {
        if ("manager".equals(role)) {
            return new FragmentManagerDashboard();
        }
        if ("staff".equals(role)) {
            return new FragmentStaffTables();
        }
        return new FragmentHome();
    }
}
