package com.example.do_an_hk1_androidstudio;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class trangchu extends AppCompatActivity implements FragmentHistory.OnThemHangClickListener {

    TabLayout tabLayout;
    ViewPager2 viewPager2;
    ViewPagerAdapter viewPagerAdapter;
    BottomNavigationView bottomNavigationView;
    FrameLayout frameLayout;
    TextView textView2;
    FirebaseFirestore db;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trangchu);

        viewPager2 = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);
        
        // Đảm bảo ViewPager2 hiển thị ngay từ đầu
        viewPager2.setVisibility(ViewPager2.VISIBLE);
        viewPager2.setCurrentItem(0, false); // Load fragment đầu tiên ngay

        bottomNavigationView = findViewById(R.id.bottomNav);
        frameLayout = findViewById(R.id.frameLayout);

        // Load FragmentHome ngay từ đầu vào frameLayout
        frameLayout.setVisibility(FrameLayout.VISIBLE);
        viewPager2.setVisibility(ViewPager2.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, new FragmentHome())
                .commit();
        
        // Đặt tab Home được chọn mặc định
        bottomNavigationView.setSelectedItemId(R.id.bottom_home);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                viewPager2.setVisibility(ViewPager2.GONE);
                frameLayout.setVisibility(FrameLayout.VISIBLE);

                int id = item.getItemId();
                if (id == R.id.bottom_home) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new FragmentHome()).commit();
                    return true;
                } else if (id == R.id.bottom_history) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new FragmentHistory()).commit();
                    return true;
                } else if (id == R.id.bottom_account) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new Fragment_Account()).commit();
                    return true;
                } else if (id == R.id.bottom_setting) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new Fragment_Setting()).commit();
                    return true;
                }

                return false;
            }
        });
        //Xử lý trang accout
        String fragmentToShow = getIntent().getStringExtra("fragmentToShow");
        if ("account".equals(fragmentToShow)) {
            viewPager2.setCurrentItem(3); // Đặt ViewPager2 hiện tại thành Fragment_Account nằm ở vị trí 3
        }

        //Xử lý textView2 thay Hello,CFPUS
        textView2 = findViewById(R.id.textView2);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userUid = currentUser.getUid();
            
            // Load dữ liệu theo UID của user hiện tại
            db.collection("Người dùng").document(userUid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String hoTen = documentSnapshot.getString("Họ tên NV");
                            
                            if (hoTen != null && !hoTen.isEmpty()) {
                                textView2.setText("Xin chào, " + hoTen);
                            } else {
                                // Nếu chưa có tên, hiển thị email
                                String userEmail = currentUser.getEmail();
                                if (userEmail != null) {
                                    textView2.setText("Xin chào, " + userEmail);
                                } else {
                                    textView2.setText("Xin chào!");
                                }
                            }
                        } else {
                            // Nếu chưa có dữ liệu, hiển thị email
                            String userEmail = currentUser.getEmail();
                            if (userEmail != null) {
                                textView2.setText("Xin chào, " + userEmail);
                            } else {
                                textView2.setText("Xin chào!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Nếu có lỗi, vẫn hiển thị email
                        String userEmail = currentUser.getEmail();
                        if (userEmail != null) {
                            textView2.setText("Xin chào, " + userEmail);
                        } else {
                            textView2.setText("Xin chào!");
                        }
                    });
        } else {
            // Nếu chưa đăng nhập
            textView2.setText("Hello, CFPLUS");
        }
    }
    // Gọi khi nút "Thêm hàng" được bấm trong FragmentHistory
    @Override
    public void onThemHangClick() {
        frameLayout.setVisibility(FrameLayout.GONE);
        viewPager2.setVisibility(ViewPager2.VISIBLE);
        viewPager2.setCurrentItem(0, true); // Quay về FragmentTrangChu
        bottomNavigationView.setSelectedItemId(R.id.bottom_home); // Cập nhật icon nav
    }
}
