package com.example.do_an_hk1_androidstudio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.StoreCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class trangchu extends AppCompatActivity implements FragmentHistory.OnThemHangClickListener {

    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;
    private LocalSessionManager sessionManager;
    private LinearLayout chatBubble;
    private FrameLayout sideMenuOverlay;
    private LinearLayout sideMenuPanel;
    private StoreCloudRepository storeRepository;
    private ListenerRegistration storeProfileListener;
    private boolean customerCanUseChat;
    private int activeNavItemId = View.NO_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.trangchu);

        sessionManager = new LocalSessionManager(this);
        storeRepository = new StoreCloudRepository(this);
        LinearLayout headerContainer = findViewById(R.id.headerContainer);
        ImageView headerLogo = findViewById(R.id.hinh1);
        bottomNavigationView = findViewById(R.id.bottomNav);
        frameLayout = findViewById(R.id.frameLayout);
        sideMenuOverlay = findViewById(R.id.sideMenuOverlay);
        sideMenuPanel = findViewById(R.id.sideMenuPanel);
        InsetsHelper.applyStatusBarPadding(headerContainer);
        InsetsHelper.applyNavigationBarPadding(bottomNavigationView);

        String role = normalizeRole(sessionManager.getCurrentUserRole());
        applyBottomNavForRole(role);
        setupCustomerChatBubble(role);
        setupSidebar(role);
        setupStoreBranding(headerLogo);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                return showSelectedTab(item.getItemId(), false);
            }
        });

        String fragmentToShow = getIntent().getStringExtra("fragmentToShow");
        int initialTabId = "account".equals(fragmentToShow) ? R.id.bottom_account : R.id.bottom_home;
        bottomNavigationView.setSelectedItemId(initialTabId);
        if (activeNavItemId != initialTabId) {
            showSelectedTab(initialTabId, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storeProfileListener != null) {
            storeProfileListener.remove();
        }
    }

    @Override
    public void onThemHangClick() {
        bottomNavigationView.setSelectedItemId(R.id.bottom_home);
    }

    @Override
    public void onBackPressed() {
        if (sideMenuOverlay != null && sideMenuOverlay.getVisibility() == View.VISIBLE) {
            closeSidebar();
            return;
        }
        super.onBackPressed();
    }

    private void applyBottomNavForRole(String role) {
        role = normalizeRole(role);
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

    private void setupCustomerChatBubble(String role) {
        role = normalizeRole(role);
        chatBubble = findViewById(R.id.chatBubble);
        if (chatBubble == null) {
            return;
        }
        customerCanUseChat = "customer".equals(role);
        updateChatBubbleVisibility(false);
        chatBubble.setOnClickListener(v -> startActivity(new Intent(this, ChatboxActivity.class)));
    }

    private void setupSidebar(String role) {
        role = normalizeRole(role);
        View btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
        TextView title = findViewById(R.id.tvSidebarTitle);
        TextView subtitle = findViewById(R.id.tvSidebarSubtitle);

        boolean manager = "manager".equals(role);
        boolean staff = "staff".equals(role);
        if (title != null) {
            title.setText(manager ? "Quản lý CFPLUS" : staff ? "Nhân viên CFPLUS" : "CFPLUS");
        }
        if (subtitle != null) {
            subtitle.setText(manager ? "Quyền quản trị cửa hàng" : staff ? "Ca làm việc hôm nay" : "Lối tắt mua hàng");
        }

        setSidebarRowVisible(R.id.rowSidebarCart, !manager && !staff);
        setSidebarRowVisible(R.id.rowSidebarOrders, !manager && !staff);
        setSidebarRowVisible(R.id.rowSidebarChat, !manager && !staff);
        setSidebarRowVisible(R.id.rowSidebarAnalytics, manager);
        setSidebarRowVisible(R.id.rowSidebarProducts, manager);
        setSidebarRowVisible(R.id.rowSidebarBanners, manager);
        setSidebarRowVisible(R.id.rowSidebarStock, manager || staff);
        setSidebarRowVisible(R.id.rowSidebarStaff, manager);
        setSidebarRowVisible(R.id.rowSidebarStoreSettings, manager);

        if (btnOpenSidebar != null) {
            btnOpenSidebar.setOnClickListener(v -> openSidebar());
        }
        if (sideMenuOverlay != null) {
            sideMenuOverlay.setOnClickListener(v -> closeSidebar());
        }
        if (sideMenuPanel != null) {
            sideMenuPanel.setOnClickListener(v -> {
            });
            sideMenuPanel.post(() -> sideMenuPanel.setTranslationX(-sideMenuPanel.getWidth()));
        }

        bindSidebarRow(R.id.rowSidebarHome, () -> {
            bottomNavigationView.setSelectedItemId(R.id.bottom_home);
            closeSidebar();
        });
        bindSidebarRow(R.id.rowSidebarCart, () -> {
            closeSidebar();
            startActivity(new Intent(this, DatMonOnlineActivity.class));
        });
        bindSidebarRow(R.id.rowSidebarOrders, () -> {
            closeSidebar();
            startActivity(new Intent(this, DonHangCuaToiActivity.class));
        });
        bindSidebarRow(R.id.rowSidebarChat, () -> {
            closeSidebar();
            startActivity(new Intent(this, ChatboxActivity.class));
        });
        bindSidebarRow(R.id.rowSidebarAnalytics, () -> openManagerShortcut(ThongKeTongHopActivity.class));
        bindSidebarRow(R.id.rowSidebarProducts, () -> openManagerShortcut(QuanLyMonActivity.class));
        bindSidebarRow(R.id.rowSidebarBanners, () -> openManagerShortcut(ManagerBannersActivity.class));
        bindSidebarRow(R.id.rowSidebarStock, () -> openManagerShortcut(QuanLyKhoActivity.class));
        bindSidebarRow(R.id.rowSidebarStaff, () -> openManagerShortcut(QuanLyNhanVienActivity.class));
        bindSidebarRow(R.id.rowSidebarStoreSettings, () -> openManagerShortcut(ManagerStoreSettingsActivity.class));
    }

    private void setupStoreBranding(ImageView headerLogo) {
        if (headerLogo == null) {
            return;
        }
        storeProfileListener = storeRepository.listenStoreProfile(profile -> Glide.with(this)
                .load(profile.getLogoUrl() == null || profile.getLogoUrl().trim().isEmpty() ? R.drawable.cfplus2 : profile.getLogoUrl())
                .placeholder(R.drawable.cfplus2)
                .error(R.drawable.cfplus2)
                .into(headerLogo));
    }

    private void openManagerShortcut(Class<?> activityClass) {
        closeSidebar();
        startActivity(new Intent(this, activityClass));
    }

    private void bindSidebarRow(int viewId, Runnable action) {
        View row = findViewById(viewId);
        if (row != null) {
            row.setOnClickListener(v -> action.run());
        }
    }

    private void setSidebarRowVisible(int viewId, boolean visible) {
        View row = findViewById(viewId);
        if (row != null) {
            row.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void openSidebar() {
        if (sideMenuOverlay == null || sideMenuPanel == null) {
            return;
        }
        sideMenuOverlay.setVisibility(View.VISIBLE);
        sideMenuPanel.animate().translationX(0f).setDuration(220).start();
    }

    private void closeSidebar() {
        if (sideMenuOverlay == null || sideMenuPanel == null) {
            return;
        }
        sideMenuPanel.animate()
                .translationX(-sideMenuPanel.getWidth())
                .setDuration(180)
                .withEndAction(() -> sideMenuOverlay.setVisibility(View.GONE))
                .start();
    }

    private void updateChatBubbleVisibility(boolean onHomeTab) {
        if (chatBubble == null) {
            return;
        }
        chatBubble.setVisibility(customerCanUseChat && onHomeTab ? LinearLayout.VISIBLE : LinearLayout.GONE);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.frameLayout, fragment)
                .commit();
    }

    private boolean showSelectedTab(int itemId, boolean force) {
        if (!force && activeNavItemId == itemId) {
            return true;
        }
        String role = normalizeRole(sessionManager.getCurrentUserRole());
        if (itemId == R.id.bottom_home) {
            activeNavItemId = itemId;
            updateChatBubbleVisibility(true);
            showFragment(createHomeFragment(role));
            return true;
        }
        if (itemId == R.id.bottom_history) {
            updateChatBubbleVisibility(false);
            if ("staff".equals(role)) {
                activeNavItemId = itemId;
                showFragment(new FragmentOnlineOrders());
                return true;
            }
            return false;
        }
        if (itemId == R.id.bottom_account) {
            activeNavItemId = itemId;
            updateChatBubbleVisibility(false);
            showFragment(createAccountFragment(role));
            return true;
        }
        if (itemId == R.id.bottom_setting) {
            activeNavItemId = itemId;
            updateChatBubbleVisibility(false);
            showFragment(new Fragment_Setting());
            return true;
        }
        return false;
    }

    private Fragment createHomeFragment(String role) {
        role = normalizeRole(role);
        if ("manager".equals(role)) {
            return new FragmentManagerDashboard();
        }
        if ("staff".equals(role)) {
            return new FragmentStaffTables();
        }
        return new FragmentHome();
    }

    private Fragment createAccountFragment(String role) {
        role = normalizeRole(role);
        if ("manager".equals(role)) {
            return new FragmentManagerAccount();
        }
        if ("staff".equals(role)) {
            return new FragmentStaffAccount();
        }
        return new FragmentCustomerAccount();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "customer";
        }
        String normalized = role.trim().toLowerCase(java.util.Locale.US);
        if ("manager".equals(normalized) || "staff".equals(normalized)) {
            return normalized;
        }
        return "customer";
    }
}
