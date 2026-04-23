package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.example.do_an_hk1_androidstudio.cloud.WishlistCloudRepository;
import com.example.do_an_hk1_androidstudio.local.CustomerCartStore;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.WishlistStore;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.example.do_an_hk1_androidstudio.ui.UiMotion;
import com.google.firebase.firestore.ListenerRegistration;

public class chitiet_sanpham extends AppCompatActivity {

    private TextView tvGia;
    private TextView tvSo;
    private TextView tvOptionPreview;
    private TextView tvCartCount;
    private CheckBox checkSizeS;
    private CheckBox checkSizeM;
    private CheckBox checkSizeL;
    private CheckBox checkDaNhieu;
    private CheckBox checkItDa;
    private CheckBox checkDaRieng;
    private CheckBox checkKhongDa;

    private int soLuong = 1;
    private int giaSanPham = 0;
    private String hinhAnh;
    private String productId;
    private String tenSanPham;
    private CustomerCartStore cartStore;
    private WishlistStore wishlistStore;
    private WishlistCloudRepository wishlistCloudRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration favoritesListener;
    private ImageButton btnFavoriteTop;
    private ImageView imageButton1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportPostponeEnterTransition();
        setContentView(R.layout.chitiet_sanpham);
        InsetsHelper.applyActivityRootPadding(this);

        cartStore = new CustomerCartStore(this);
        wishlistStore = new WishlistStore(this);
        wishlistCloudRepository = new WishlistCloudRepository(this);
        sessionManager = new LocalSessionManager(this);

        imageButton1 = findViewById(R.id.imageButton1);
        TextView tvTen = findViewById(R.id.tvTen);
        tvGia = findViewById(R.id.tvGia);
        tvSo = findViewById(R.id.tvso);
        tvOptionPreview = findViewById(R.id.tvOptionPreview);
        tvCartCount = findViewById(R.id.tvCartCount);
        TextView tvThemGioHang = findViewById(R.id.tvthemgiohang);
        TextView tvQuayLai = findViewById(R.id.tvquaylai);
        TextView btnBackTop = findViewById(R.id.btnBackTop);
        TextView btnOpenCart = findViewById(R.id.btnOpenCart);
        btnFavoriteTop = findViewById(R.id.btnFavoriteTop);
        ImageButton iBTang = findViewById(R.id.iB_tang);
        ImageButton iBGiam = findViewById(R.id.iB_giam);

        checkSizeS = findViewById(R.id.checkSizeS);
        checkSizeM = findViewById(R.id.checkSizeM);
        checkSizeL = findViewById(R.id.checkSizeL);
        checkDaNhieu = findViewById(R.id.check_Da_nhieu);
        checkItDa = findViewById(R.id.check_it_da);
        checkDaRieng = findViewById(R.id.check_da_rieng);
        checkKhongDa = findViewById(R.id.check_khong_da);

        Intent intent = getIntent();
        tenSanPham = intent.getStringExtra("Ten");
        String gia = intent.getStringExtra("Gia");
        hinhAnh = intent.getStringExtra("hinhAnh");
        productId = intent.getStringExtra("productId");

        String transitionName = intent.getStringExtra("image_transition_name");
        if (!TextUtils.isEmpty(transitionName)) {
            ViewCompat.setTransitionName(imageButton1, transitionName);
        }

        tvTen.setText(tenSanPham);
        tvSo.setText(String.valueOf(soLuong));
        try {
            giaSanPham = Integer.parseInt(gia == null ? "0" : gia.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            giaSanPham = 0;
        }
        capNhatSoLuong();

        Glide.with(this)
                .load(hinhAnh)
                .placeholder(R.drawable.cfplus)
                .error(R.drawable.cfplus)
                .fitCenter()
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        startPostponedEnterTransition();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        startPostponedEnterTransition();
                        return false;
                    }
                })
                .into(imageButton1);

        iBTang.setOnClickListener(v -> {
            soLuong++;
            capNhatSoLuong();
            UiMotion.bounce(iBTang);
        });
        iBGiam.setOnClickListener(v -> {
            if (soLuong > 1) {
                soLuong--;
                capNhatSoLuong();
                UiMotion.bounce(iBGiam);
            }
        });

        checkSizeS.setOnCheckedChangeListener((b, c) -> {
            if (c) {
                checkSizeM.setChecked(false);
                checkSizeL.setChecked(false);
            }
            updateOptionPreview();
        });
        checkSizeM.setOnCheckedChangeListener((b, c) -> {
            if (c) {
                checkSizeS.setChecked(false);
                checkSizeL.setChecked(false);
            }
            updateOptionPreview();
        });
        checkSizeL.setOnCheckedChangeListener((b, c) -> {
            if (c) {
                checkSizeS.setChecked(false);
                checkSizeM.setChecked(false);
            }
            updateOptionPreview();
        });

        checkDaNhieu.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkDaNhieu);
            updateOptionPreview();
        });
        checkItDa.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkItDa);
            updateOptionPreview();
        });
        checkDaRieng.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkDaRieng);
            updateOptionPreview();
        });
        checkKhongDa.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkKhongDa);
            updateOptionPreview();
        });

        UiMotion.applyPressFeedback(tvThemGioHang);
        UiMotion.applyPressFeedback(tvQuayLai);
        tvThemGioHang.setOnClickListener(v -> addToCart());
        tvQuayLai.setOnClickListener(v -> finish());
        btnBackTop.setOnClickListener(v -> finish());
        btnOpenCart.setOnClickListener(v -> openCart());
        btnFavoriteTop.setOnClickListener(v -> toggleFavorite());
        updateOptionPreview();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenFavorites();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindCartCount();
        bindFavoriteState();
    }

    private void addToCart() {
        if (TextUtils.isEmpty(productId)) {
            Toast.makeText(this, "Không tìm thấy sản phẩm để thêm vào giỏ.", Toast.LENGTH_SHORT).show();
            return;
        }
        String size = getSelectedSize();
        String da = getSelectedIce();
        if (size == null || da == null) {
            Toast.makeText(this, "Vui lòng chọn size và mức đá.", Toast.LENGTH_SHORT).show();
            return;
        }
        cartStore.addItem(productId, tenSanPham, giaSanPham, soLuong, size, da, null, hinhAnh);
        bindCartCount();
        UiMotion.bounce(findViewById(R.id.tvthemgiohang));
        if (tvCartCount != null && tvCartCount.getVisibility() == android.view.View.VISIBLE) {
            UiMotion.pulse(tvCartCount);
        }
        Toast.makeText(this, "Đã thêm vào giỏ hàng online.", Toast.LENGTH_SHORT).show();
    }

    private void openCart() {
        startActivity(new Intent(this, DatMonOnlineActivity.class));
    }

    private void bindCartCount() {
        int count = cartStore.getItemCount();
        tvCartCount.setVisibility(count > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        tvCartCount.setText(String.valueOf(count));
    }

    private void bindFavoriteState() {
        if (btnFavoriteTop == null || TextUtils.isEmpty(productId)) {
            return;
        }
        btnFavoriteTop.setImageResource(wishlistStore.isFavorite(productId)
                ? R.drawable.ic_heart_filled
                : R.drawable.ic_heart_outline);
    }

    private void toggleFavorite() {
        if (TextUtils.isEmpty(productId)) {
            return;
        }
        boolean nextFavorite = !wishlistStore.isFavorite(productId);
        wishlistStore.setFavorite(productId, nextFavorite);
        bindFavoriteState();
        UiMotion.pulse(btnFavoriteTop);
        Toast.makeText(this,
                wishlistStore.isFavorite(productId) ? "Đã lưu vào yêu thích." : "Đã xóa khỏi yêu thích.",
                Toast.LENGTH_SHORT).show();

        String customerId = sessionManager.getCurrentUserId();
        if (TextUtils.isEmpty(customerId)) {
            return;
        }
        wishlistCloudRepository.setFavorite(customerId, productId, nextFavorite, (success, message) -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, message == null ? "Chưa đồng bộ được món yêu thích lên Firebase." : message, Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void listenFavorites() {
        String customerId = sessionManager.getCurrentUserId();
        if (TextUtils.isEmpty(customerId)) {
            bindFavoriteState();
            return;
        }
        if (favoritesListener != null) {
            favoritesListener.remove();
        }
        favoritesListener = wishlistCloudRepository.listenFavoriteIds(customerId, ids -> runOnUiThread(() -> {
            wishlistStore.replaceAll(ids);
            bindFavoriteState();
        }));
    }

    private void capNhatSoLuong() {
        tvSo.setText(String.valueOf(soLuong));
        int tongTien = giaSanPham * soLuong;
        tvGia.setText("Tổng tiền: " + MoneyFormatter.format(tongTien));
        updateOptionPreview();
    }

    private void updateOptionPreview() {
        if (tvOptionPreview == null) {
            return;
        }
        String size = getSelectedSize();
        String da = getSelectedIce();
        String preview = "Lựa chọn: "
                + (size == null ? "chưa chọn size" : "size " + size)
                + " | "
                + (da == null ? "chưa chọn mức đá" : da.toLowerCase())
                + " | SL " + soLuong;
        tvOptionPreview.setText(preview);
    }

    private String getSelectedSize() {
        if (checkSizeS.isChecked()) return "S";
        if (checkSizeM.isChecked()) return "M";
        if (checkSizeL.isChecked()) return "L";
        return null;
    }

    private String getSelectedIce() {
        if (checkDaNhieu.isChecked()) return "Đá bình thường";
        if (checkItDa.isChecked()) return "Ít đá";
        if (checkDaRieng.isChecked()) return "Đá riêng";
        if (checkKhongDa.isChecked()) return "Không đá";
        return null;
    }

    private void uncheckAllIceExcept(CheckBox keep) {
        for (CheckBox cb : new CheckBox[]{checkDaNhieu, checkItDa, checkDaRieng, checkKhongDa}) {
            if (cb != keep) {
                cb.setChecked(false);
            }
        }
    }
}
