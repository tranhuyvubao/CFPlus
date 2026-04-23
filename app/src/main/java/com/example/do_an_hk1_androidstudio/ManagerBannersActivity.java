package com.example.do_an_hk1_androidstudio;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.BannerCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalCategory;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.local.model.PromoBanner;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerBannersActivity extends AppCompatActivity {

    private BannerCloudRepository bannerRepository;
    private CatalogCloudRepository catalogRepository;
    private ListenerRegistration bannersListener;
    private ListenerRegistration productsListener;
    private ListenerRegistration categoriesListener;
    private LinearLayout layoutManagerBanners;
    private final List<PromoBanner> banners = new ArrayList<>();
    private final List<LocalProduct> products = new ArrayList<>();
    private final Map<String, String> categoryNames = new HashMap<>();
    private AlertDialog editingDialog;
    private ImageView editingPreview;
    private String editingImageUrl = "";

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadBannerImage(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager_banners);
        InsetsHelper.applyActivityRootPadding(this);

        bannerRepository = new BannerCloudRepository(this);
        catalogRepository = new CatalogCloudRepository(this);
        layoutManagerBanners = findViewById(R.id.layoutManagerBanners);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddBanner).setOnClickListener(v -> showBannerDialog(null));
        listenCategories();
        listenProducts();
        listenBanners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannersListener != null) {
            bannersListener.remove();
        }
        if (productsListener != null) {
            productsListener.remove();
        }
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
    }

    private void listenCategories() {
        categoriesListener = catalogRepository.listenCategories(nextCategories -> {
            categoryNames.clear();
            for (LocalCategory category : nextCategories) {
                categoryNames.put(category.getCategoryId(), category.getName());
            }
        });
    }

    private void listenProducts() {
        productsListener = catalogRepository.listenProducts(nextProducts -> {
            products.clear();
            products.addAll(nextProducts);
        });
    }

    private void listenBanners() {
        bannersListener = bannerRepository.listenBanners(nextBanners -> {
            banners.clear();
            banners.addAll(nextBanners);
            renderBanners();
        });
    }

    private void renderBanners() {
        layoutManagerBanners.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (banners.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setBackgroundResource(R.drawable.manager_search_background);
            empty.setPadding(dp(18), dp(18), dp(18), dp(18));
            empty.setText("Chưa có banner nào. Bấm Thêm banner để bắt đầu.");
            empty.setTextColor(getColor(R.color.dashboard_text_secondary));
            layoutManagerBanners.addView(empty);
            return;
        }
        for (PromoBanner banner : banners) {
            View itemView = inflater.inflate(R.layout.item_manager_banner, layoutManagerBanners, false);
            ImageView image = itemView.findViewById(R.id.imgManagerBanner);
            TextView title = itemView.findViewById(R.id.tvManagerBannerTitle);
            TextView subtitle = itemView.findViewById(R.id.tvManagerBannerSubtitle);
            TextView meta = itemView.findViewById(R.id.tvManagerBannerMeta);
            TextView edit = itemView.findViewById(R.id.btnEditBanner);
            TextView delete = itemView.findViewById(R.id.btnDeleteBanner);

            title.setText(banner.getTitle());
            subtitle.setText(banner.getSubtitle());
            String linkedName = getProductSummary(banner.getProductIds());
            String linkedText = TextUtils.isEmpty(linkedName) ? "" : " - " + linkedName;
            meta.setText("Thứ tự " + banner.getSortOrder()
                    + " - " + (banner.isActive() ? "Đang hiển thị" : "Đang ẩn")
                    + linkedText);
            Glide.with(this)
                    .load(banner.getImageUrl())
                    .placeholder(R.drawable.cfplus4)
                    .error(R.drawable.cfplus4)
                    .into(image);
            edit.setOnClickListener(v -> showBannerDialog(banner));
            delete.setOnClickListener(v -> confirmDelete(banner));

            layoutManagerBanners.addView(itemView);
        }
    }

    private void showBannerDialog(@Nullable PromoBanner banner) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_promo_banner, null, false);
        TextView title = dialogView.findViewById(R.id.tvBannerDialogTitle);
        editingPreview = dialogView.findViewById(R.id.imgBannerPreview);
        TextView chooseImage = dialogView.findViewById(R.id.btnChooseBannerImage);
        EditText edtTitle = dialogView.findViewById(R.id.edtBannerTitle);
        EditText edtSubtitle = dialogView.findViewById(R.id.edtBannerSubtitle);
        EditText edtAction = dialogView.findViewById(R.id.edtBannerAction);
        TextView tvSelectedProduct = dialogView.findViewById(R.id.tvSelectedBannerProduct);
        TextView btnClearProduct = dialogView.findViewById(R.id.btnClearBannerProduct);
        EditText edtSortOrder = dialogView.findViewById(R.id.edtBannerSortOrder);
        Switch switchActive = dialogView.findViewById(R.id.switchBannerActive);
        ArrayList<String> selectedProductIds = new ArrayList<>(banner == null ? new ArrayList<>() : banner.getProductIds());

        editingImageUrl = banner == null ? "" : banner.getImageUrl();
        title.setText(banner == null ? "Thêm banner" : "Sửa banner");
        edtTitle.setText(banner == null ? "" : banner.getTitle());
        edtSubtitle.setText(banner == null ? "" : banner.getSubtitle());
        edtAction.setText(banner == null ? "Xem ngay" : banner.getActionText());
        bindSelectedProductLabel(tvSelectedProduct, selectedProductIds);
        edtSortOrder.setText(String.valueOf(banner == null ? banners.size() + 1 : banner.getSortOrder()));
        switchActive.setChecked(banner == null || banner.isActive());
        bindPreview();
        chooseImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        tvSelectedProduct.setOnClickListener(v -> showProductPickerDialog(selectedProductIds, nextSelectedIds -> {
            selectedProductIds.clear();
            selectedProductIds.addAll(nextSelectedIds);
            bindSelectedProductLabel(tvSelectedProduct, selectedProductIds);
        }));
        btnClearProduct.setOnClickListener(v -> {
            selectedProductIds.clear();
            bindSelectedProductLabel(tvSelectedProduct, selectedProductIds);
        });

        editingDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        editingDialog.setOnShowListener(unused -> editingDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nextTitle = edtTitle.getText().toString().trim();
            if (TextUtils.isEmpty(nextTitle)) {
                edtTitle.setError("Nhập tiêu đề banner");
                return;
            }
            bannerRepository.saveBanner(
                    banner == null ? null : banner.getBannerId(),
                    nextTitle,
                    edtSubtitle.getText().toString().trim(),
                    editingImageUrl,
                    edtAction.getText().toString().trim(),
                    selectedProductIds.isEmpty() ? "" : selectedProductIds.get(0),
                    selectedProductIds,
                    parseInt(edtSortOrder.getText().toString(), banners.size() + 1),
                    switchActive.isChecked(),
                    (success, message) -> {
                        Toast.makeText(this, success ? "Đã lưu banner" : valueOrDefault(message, "Không lưu được banner"), Toast.LENGTH_SHORT).show();
                        if (success) {
                            editingDialog.dismiss();
                        }
                    }
            );
        }));
        editingDialog.show();
    }

    private void showProductPickerDialog(List<String> currentSelectedIds, ProductSelectionCallback callback) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(10));
        scrollView.addView(container);
        ArrayList<String> selectedIds = new ArrayList<>(currentSelectedIds);

        TextView title = new TextView(this);
        title.setText("Chọn món cho banner");
        title.setTextColor(getColor(R.color.dashboard_text_primary));
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        container.addView(title);

        TextView clear = new TextView(this);
        clear.setText("Bỏ chọn tất cả món");
        clear.setTextColor(getColor(R.color.dashboard_primary));
        clear.setTextSize(14);
        clear.setTypeface(clear.getTypeface(), android.graphics.Typeface.BOLD);
        clear.setPadding(0, dp(12), 0, dp(12));
        container.addView(clear);

        AlertDialog pickerDialog = new AlertDialog.Builder(this)
                .setView(scrollView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xong", null)
                .create();
        clear.setOnClickListener(v -> {
            selectedIds.clear();
            refreshProductPickerCards(container, selectedIds);
        });

        renderProductPickerCards(container, selectedIds);
        pickerDialog.setOnShowListener(unused ->
                pickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    callback.onSelected(new ArrayList<>(selectedIds));
                    pickerDialog.dismiss();
                }));
        pickerDialog.show();
    }

    private void renderProductPickerCards(LinearLayout container, ArrayList<String> selectedIds) {
        LayoutInflater inflater = LayoutInflater.from(this);
        int cardCount = 0;
        for (LocalProduct product : products) {
            if (!product.isActive()) {
                continue;
            }
            View itemView = inflater.inflate(R.layout.item_banner_product_picker, container, false);
            bindProductPickerCard(itemView, product, selectedIds.contains(product.getProductId()));
            itemView.setTag(product);
            itemView.setOnClickListener(v -> {
                if (selectedIds.contains(product.getProductId())) {
                    selectedIds.remove(product.getProductId());
                } else {
                    selectedIds.add(product.getProductId());
                }
                refreshProductPickerCards(container, selectedIds);
            });
            container.addView(itemView);
            cardCount++;
        }
        if (cardCount == 0) {
            TextView empty = new TextView(this);
            empty.setBackgroundResource(R.drawable.manager_search_background);
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            empty.setText("Chưa có món đang kinh doanh để chọn.");
            empty.setTextColor(getColor(R.color.dashboard_text_secondary));
            container.addView(empty);
        }
    }

    private void refreshProductPickerCards(LinearLayout container, ArrayList<String> selectedIds) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof LocalProduct) {
                LocalProduct product = (LocalProduct) tag;
                bindProductPickerCard(child, product, selectedIds.contains(product.getProductId()));
            }
        }
    }

    private void bindProductPickerCard(View itemView, LocalProduct product, boolean selected) {
        ImageView image = itemView.findViewById(R.id.imgPickerProduct);
        TextView name = itemView.findViewById(R.id.tvPickerProductName);
        TextView meta = itemView.findViewById(R.id.tvPickerProductMeta);
        TextView price = itemView.findViewById(R.id.tvPickerProductPrice);
        TextView action = itemView.findViewById(R.id.tvPickerProductAction);

        name.setText(product.getName());
        String categoryName = categoryNames.get(product.getCategoryId());
        meta.setText(TextUtils.isEmpty(categoryName) ? "Món đang kinh doanh" : "Danh mục: " + categoryName);
        price.setText(MoneyFormatter.format(product.getBasePrice()));
        action.setText(selected ? "Đã chọn" : "Chọn");
        Glide.with(this)
                .load(product.getImageUrl())
                .placeholder(R.drawable.cfplus4)
                .error(R.drawable.cfplus4)
                .into(image);
    }

    private void bindSelectedProductLabel(TextView label, List<String> productIds) {
        String productSummary = getProductSummary(productIds);
        if (TextUtils.isEmpty(productSummary)) {
            label.setText("Chọn các món liên kết");
            label.setTextColor(getColor(R.color.dashboard_text_secondary));
            return;
        }
        label.setText("Đã chọn: " + productSummary);
        label.setTextColor(getColor(R.color.dashboard_text_primary));
    }

    private String getProductSummary(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (String productId : productIds) {
            LocalProduct product = findProduct(productId);
            if (product != null) {
                names.add(product.getName());
            }
        }
        if (names.isEmpty()) {
            return productIds.size() + " món đã chọn";
        }
        if (names.size() <= 2) {
            return TextUtils.join(", ", names);
        }
        return names.get(0) + ", " + names.get(1) + " +" + (names.size() - 2) + " món";
    }

    @Nullable
    private LocalProduct findProduct(String productId) {
        if (TextUtils.isEmpty(productId)) {
            return null;
        }
        for (LocalProduct product : products) {
            if (productId.equals(product.getProductId())) {
                return product;
            }
        }
        return null;
    }

    private void uploadBannerImage(Uri uri) {
        Toast.makeText(this, "Đang xử lý ảnh banner...", Toast.LENGTH_SHORT).show();
        catalogRepository.uploadCatalogImage(uri, "banners", (success, imageUrl, message) -> {
            if (!success || TextUtils.isEmpty(imageUrl)) {
                Toast.makeText(this, valueOrDefault(message, "Không đọc được ảnh banner"), Toast.LENGTH_SHORT).show();
                return;
            }
            editingImageUrl = imageUrl;
            bindPreview();
        });
    }

    private void bindPreview() {
        if (editingPreview == null) {
            return;
        }
        Glide.with(this)
                .load(TextUtils.isEmpty(editingImageUrl) ? R.drawable.cfplus4 : editingImageUrl)
                .placeholder(R.drawable.cfplus4)
                .error(R.drawable.cfplus4)
                .into(editingPreview);
    }

    private void confirmDelete(PromoBanner banner) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa banner?")
                .setMessage("Banner \"" + banner.getTitle() + "\" sẽ bị xóa khỏi slide.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> bannerRepository.deleteBanner(banner.getBannerId(), (success, message) ->
                        Toast.makeText(this, success ? "Đã xóa banner" : valueOrDefault(message, "Không xóa được banner"), Toast.LENGTH_SHORT).show()))
                .show();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface ProductSelectionCallback {
        void onSelected(List<String> productIds);
    }
}
