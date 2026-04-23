package com.example.do_an_hk1_androidstudio;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalCategory;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class QuanLyMonActivity extends AppCompatActivity {

    private final List<LocalProduct> allProducts = new ArrayList<>();
    private final List<LocalProduct> products = new ArrayList<>();
    private final List<LocalCategory> categoryDocs = new ArrayList<>();
    private final List<String> categoryLabels = new ArrayList<>();
    private ProductAdapter productAdapter;
    private CatalogCloudRepository catalogRepository;
    private ActivityResultLauncher<String[]> pickImageLauncher;
    private Uri pendingImageUri = null;
    private ImageView pendingPickImageView = null;
    private TextView tvEmpty;
    private ListenerRegistration productsListener;
    private ListenerRegistration categoriesListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_mon);
        InsetsHelper.applyActivityRootPadding(this);

        if (!"manager".equals(new LocalSessionManager(this).getCurrentUserRole())) {
            Toast.makeText(this, "Chỉ quản lý mới truy cập được chức năng này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        catalogRepository = new CatalogCloudRepository(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        TextView btnAdd = findViewById(R.id.btnAdd);
        EditText edtSearch = findViewById(R.id.edtSearchProducts);
        RecyclerView rv = findViewById(R.id.rvManageMenu);
        tvEmpty = findViewById(R.id.tvEmptyProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(products);
        rv.setAdapter(productAdapter);
        btnAdd.setOnClickListener(v -> showAddProductDialog());
        edtSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                filterProducts(s.toString());
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Some pickers only grant temporary access. Saving immediately still can upload the image.
            }
            pendingImageUri = uri;
            if (pendingPickImageView != null) {
                pendingPickImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                pendingPickImageView.setImageURI(uri);
            }
        });

        listenCloudData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null) productsListener.remove();
        if (categoriesListener != null) categoriesListener.remove();
    }

    private void listenCloudData() {
        categoriesListener = catalogRepository.listenCategories(categories -> {
            categoryDocs.clear();
            categoryLabels.clear();
            categoryDocs.addAll(categories);
            for (LocalCategory category : categories) {
                categoryLabels.add(category.getName());
            }
            filterProducts(null);
        });

        productsListener = catalogRepository.listenProducts(fetchedProducts -> {
            allProducts.clear();
            allProducts.addAll(fetchedProducts);
            filterProducts(null);
        });
    }

    private void filterProducts(@Nullable String keyword) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        products.clear();
        for (LocalProduct product : allProducts) {
            String categoryLabel = findCategoryLabel(product.getCategoryId());
            String searchable = (product.getName() + " " + categoryLabel).toLowerCase();
            if (query.isEmpty() || searchable.contains(query)) {
                products.add(product);
            }
        }
        productAdapter.notifyDataSetChanged();
        tvEmpty.setText(query.isEmpty() ? "Chưa có món nào trong menu." : "Không tìm thấy món phù hợp với từ khóa bạn vừa nhập.");
        tvEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadCategoriesForSpinner(@NonNull Spinner spinner, @Nullable String preselectCategoryId) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (preselectCategoryId != null) {
            int idx = 0;
            for (int i = 0; i < categoryDocs.size(); i++) {
                if (preselectCategoryId.equals(categoryDocs.get(i).getCategoryId())) {
                    idx = i;
                    break;
                }
            }
            spinner.setSelection(idx);
        }
    }

    private void showAddProductDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_product, null);
        EditText edtName = view.findViewById(R.id.edtProductName);
        EditText edtPrice = view.findViewById(R.id.edtProductBasePrice);
        Spinner spCategory = view.findViewById(R.id.spProductCategory);
        ImageView imgPick = view.findViewById(R.id.imgPickProduct);
        androidx.appcompat.widget.SwitchCompat swActive = view.findViewById(R.id.swProductActive);
        swActive.setChecked(true);

        pendingImageUri = null;
        pendingPickImageView = imgPick;
        imgPick.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));
        loadCategoriesForSpinner(spCategory, null);

        new AlertDialog.Builder(this)
                .setTitle("Thêm món")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> saveProduct(null, edtName, edtPrice, spCategory, swActive))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditProductDialog(LocalProduct product) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_product, null);
        EditText edtName = view.findViewById(R.id.edtProductName);
        EditText edtPrice = view.findViewById(R.id.edtProductBasePrice);
        Spinner spCategory = view.findViewById(R.id.spProductCategory);
        ImageView imgPick = view.findViewById(R.id.imgPickProduct);
        androidx.appcompat.widget.SwitchCompat swActive = view.findViewById(R.id.swProductActive);

        edtName.setText(product.getName());
        edtPrice.setText(String.valueOf(product.getBasePrice()));
        swActive.setChecked(product.isActive());
        pendingImageUri = null;
        pendingPickImageView = imgPick;
        imgPick.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));
        if (!TextUtils.isEmpty(product.getImageUrl())) {
            Glide.with(this).load(product.getImageUrl()).into(imgPick);
            imgPick.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        loadCategoriesForSpinner(spCategory, product.getCategoryId());

        new AlertDialog.Builder(this)
                .setTitle("Sửa món")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> saveProduct(product, edtName, edtPrice, spCategory, swActive))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveProduct(@Nullable LocalProduct existing,
                             EditText edtName,
                             EditText edtPrice,
                             Spinner spCategory,
                             androidx.appcompat.widget.SwitchCompat swActive) {
        String name = edtName.getText().toString().trim();
        String basePriceStr = edtPrice.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(basePriceStr)) {
            Toast.makeText(this, "Vui lòng nhập tên và giá", Toast.LENGTH_SHORT).show();
            return;
        }

        int basePrice;
        try {
            basePrice = Integer.parseInt(basePriceStr);
        } catch (Exception e) {
            Toast.makeText(this, "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categoryDocs.isEmpty()) {
            Toast.makeText(this, "Chưa có danh mục. Hãy tạo danh mục trước.", Toast.LENGTH_LONG).show();
            return;
        }

        int selectedIdx = spCategory.getSelectedItemPosition();
        if (selectedIdx < 0 || selectedIdx >= categoryDocs.size()) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        String categoryId = categoryDocs.get(selectedIdx).getCategoryId();
        String existingImageUrl = existing != null ? existing.getImageUrl() : null;
        Uri uploadUri = pendingImageUri != null ? pendingImageUri : parseLocalImageUri(existingImageUrl);

        if (uploadUri != null && pendingImageUri == null) {
            Toast.makeText(this, "Ảnh cũ chỉ nằm trong máy ảo. Hãy chọn lại ảnh rồi lưu để đưa lên web.", Toast.LENGTH_LONG).show();
            return;
        }

        if (uploadUri != null) {
            Toast.makeText(this, "Đang tải ảnh món lên web...", Toast.LENGTH_SHORT).show();
            catalogRepository.uploadCatalogImage(uploadUri, "products", (success, uploadedUrl, message) -> {
                if (!success || TextUtils.isEmpty(uploadedUrl)) {
                    Toast.makeText(this, message == null ? "Không tải được ảnh. Hãy chọn lại ảnh hoặc kiểm tra mạng." : message, Toast.LENGTH_LONG).show();
                    return;
                }
                persistProduct(existing, name, basePrice, uploadedUrl, categoryId, swActive.isChecked());
            });
            return;
        }

        String imageUrl = existingImageUrl;
        if (existing == null) {
            catalogRepository.addProduct(name, basePrice, imageUrl, categoryId, swActive.isChecked(), (success, message) -> Toast.makeText(
                    this,
                    success ? "Thêm món thành công!" : (message == null ? "Không thêm được món." : message),
                    Toast.LENGTH_SHORT
            ).show());
        } else {
            catalogRepository.updateProduct(existing.getProductId(), name, basePrice, imageUrl, categoryId, swActive.isChecked(), (success, message) -> Toast.makeText(
                    this,
                    success ? "Cập nhật món thành công!" : (message == null ? "Không cập nhật được món." : message),
                    Toast.LENGTH_SHORT
            ).show());
        }
    }

    private void persistProduct(@Nullable LocalProduct existing,
                                String name,
                                int basePrice,
                                @Nullable String imageUrl,
                                String categoryId,
                                boolean active) {
        if (existing == null) {
            catalogRepository.addProduct(name, basePrice, imageUrl, categoryId, active, (success, message) -> Toast.makeText(
                    this,
                    success ? "Thêm món thành công!" : (message == null ? "Không thêm được món." : message),
                    Toast.LENGTH_SHORT
            ).show());
        } else {
            catalogRepository.updateProduct(existing.getProductId(), name, basePrice, imageUrl, categoryId, active, (success, message) -> Toast.makeText(
                    this,
                    success ? "Cập nhật món thành công!" : (message == null ? "Không cập nhật được món." : message),
                    Toast.LENGTH_SHORT
            ).show());
        }
    }

    @Nullable
    private Uri parseLocalImageUri(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        Uri uri = Uri.parse(value);
        return "content".equalsIgnoreCase(uri.getScheme()) ? uri : null;
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductVH> {
        private final List<LocalProduct> data;

        ProductAdapter(List<LocalProduct> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ProductVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            return new ProductVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductVH holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class ProductVH extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvPrice;
        private final TextView tvCategory;
        private final TextView tvActive;
        private final TextView btnEdit;
        private final TextView btnDelete;
        private final ImageView imgThumb;

        ProductVH(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgProductThumb);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvActive = itemView.findViewById(R.id.tvProductActive);
            btnEdit = itemView.findViewById(R.id.btnProductEdit);
            btnDelete = itemView.findViewById(R.id.btnProductDelete);
        }

        void bind(LocalProduct product) {
            tvName.setText(product.getName());
            tvPrice.setText("Giá: " + MoneyFormatter.format(product.getBasePrice()));
            tvCategory.setText("Danh mục: " + findCategoryLabel(product.getCategoryId()));
            tvActive.setText(product.isActive() ? "Đang kinh doanh" : "Tạm ẩn");

            if (!TextUtils.isEmpty(product.getImageUrl())) {
                Glide.with(itemView.getContext())
                        .load(product.getImageUrl())
                        .placeholder(R.mipmap.ic_launcher_round)
                        .into(imgThumb);
            } else {
                imgThumb.setImageResource(R.mipmap.ic_launcher_round);
            }

            btnEdit.setOnClickListener(v -> showEditProductDialog(product));
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(QuanLyMonActivity.this)
                    .setTitle("Xóa món")
                    .setMessage("Bạn có chắc muốn xóa món \"" + product.getName() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> catalogRepository.deleteProduct(product.getProductId(), (success, message) -> {
                        if (!success) {
                            Toast.makeText(QuanLyMonActivity.this, message == null ? "Không xóa được món." : message, Toast.LENGTH_SHORT).show();
                        }
                    }))
                    .setNegativeButton("Hủy", null)
                    .show());
        }
    }

    private String findCategoryLabel(String categoryId) {
        for (LocalCategory category : categoryDocs) {
            if (category.getCategoryId().equals(categoryId)) {
                return category.getName();
            }
        }
        return categoryId;
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
