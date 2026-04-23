package com.example.do_an_hk1_androidstudio;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class QuanLyDanhMucActivity extends AppCompatActivity {

    private final List<LocalCategory> allCategories = new ArrayList<>();
    private final List<LocalCategory> categories = new ArrayList<>();
    private CategoryAdapter adapter;
    private CatalogCloudRepository catalogRepository;
    private TextView tvEmpty;
    private ListenerRegistration categoriesListener;
    private ActivityResultLauncher<String[]> pickImageLauncher;
    private Uri pendingImageUri;
    private ImageView pendingPickImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_danh_muc);
        InsetsHelper.applyActivityRootPadding(this);

        if (!"manager".equals(new LocalSessionManager(this).getCurrentUserRole())) {
            Toast.makeText(this, "Chỉ quản lý mới truy cập được chức năng này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        catalogRepository = new CatalogCloudRepository(this);
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) {
                return;
            }
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

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        RecyclerView rv = findViewById(R.id.rvCategories);
        TextView btnAdd = findViewById(R.id.btnAddCategory);
        EditText edtSearch = findViewById(R.id.edtSearchCategories);
        tvEmpty = findViewById(R.id.tvEmptyCategories);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(categories);
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddCategoryDialog());
        edtSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                filterCategories(s.toString());
            }
        });
        listenCategories();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
    }

    private void listenCategories() {
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
        categoriesListener = catalogRepository.listenCategories(fetched -> {
            allCategories.clear();
            allCategories.addAll(fetched);
            filterCategories(null);
        });
    }

    private void filterCategories(@Nullable String keyword) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        categories.clear();
        for (LocalCategory category : allCategories) {
            if (query.isEmpty() || category.getName().toLowerCase().contains(query)) {
                categories.add(category);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setText(query.isEmpty() ? "Chưa có danh mục nào." : "Không tìm thấy danh mục phù hợp.");
        tvEmpty.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddCategoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText edtName = view.findViewById(R.id.edtCategoryName);
        ImageView imgPick = view.findViewById(R.id.imgPickCategory);
        androidx.appcompat.widget.SwitchCompat swActive = view.findViewById(R.id.swCategoryActive);
        swActive.setChecked(true);

        pendingImageUri = null;
        pendingPickImageView = imgPick;
        imgPick.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));

        new AlertDialog.Builder(this)
                .setTitle("Thêm danh mục")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> {
                    String name = edtName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Tên danh mục không được trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pendingImageUri != null) {
                        saveCategory(null, name, swActive.isChecked());
                        return;
                    }
                    String imageUrl = null;
                    catalogRepository.addCategory(name, imageUrl, swActive.isChecked(), (success, message) -> Toast.makeText(
                            this,
                            success ? "Đã thêm danh mục." : (message == null ? "Không thêm được danh mục." : message),
                            Toast.LENGTH_SHORT
                    ).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditCategoryDialog(LocalCategory category) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText edtName = view.findViewById(R.id.edtCategoryName);
        ImageView imgPick = view.findViewById(R.id.imgPickCategory);
        androidx.appcompat.widget.SwitchCompat swActive = view.findViewById(R.id.swCategoryActive);

        edtName.setText(category.getName());
        swActive.setChecked(category.isActive());
        pendingImageUri = null;
        pendingPickImageView = imgPick;
        imgPick.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));
        if (!TextUtils.isEmpty(category.getImageUrl())) {
            imgPick.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(this).load(category.getImageUrl()).into(imgPick);
        }

        new AlertDialog.Builder(this)
                .setTitle("Sửa danh mục")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> {
                    String name = edtName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Tên danh mục không được trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pendingImageUri != null || parseLocalImageUri(category.getImageUrl()) != null) {
                        saveCategory(category, name, swActive.isChecked());
                        return;
                    }
                    String imageUrl = category.getImageUrl();
                    catalogRepository.updateCategory(category.getCategoryId(), name, imageUrl, swActive.isChecked(), (success, message) -> Toast.makeText(
                            this,
                            success ? "Đã cập nhật danh mục." : (message == null ? "Không cập nhật được danh mục." : message),
                            Toast.LENGTH_SHORT
                    ).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveCategory(@Nullable LocalCategory existing, String name, boolean active) {
        String existingImageUrl = existing != null ? existing.getImageUrl() : null;
        Uri uploadUri = pendingImageUri != null ? pendingImageUri : parseLocalImageUri(existingImageUrl);
        if (uploadUri != null && pendingImageUri == null) {
            Toast.makeText(this, "Ảnh cũ chỉ nằm trong máy ảo. Hãy chọn lại ảnh rồi lưu để đưa lên web.", Toast.LENGTH_LONG).show();
            return;
        }
        if (uploadUri != null) {
            Toast.makeText(this, "Đang tải ảnh danh mục lên web...", Toast.LENGTH_SHORT).show();
            catalogRepository.uploadCatalogImage(uploadUri, "categories", (success, uploadedUrl, message) -> {
                if (!success || TextUtils.isEmpty(uploadedUrl)) {
                    Toast.makeText(this, message == null ? "Không tải được ảnh. Hãy chọn lại ảnh hoặc kiểm tra mạng." : message, Toast.LENGTH_LONG).show();
                    return;
                }
                persistCategory(existing, name, uploadedUrl, active);
            });
            return;
        }

        persistCategory(existing, name, existingImageUrl, active);
    }

    private void persistCategory(@Nullable LocalCategory existing,
                                 String name,
                                 @Nullable String imageUrl,
                                 boolean active) {
        if (existing == null) {
            catalogRepository.addCategory(name, imageUrl, active, (success, message) -> Toast.makeText(
                    this,
                    success ? "Đã thêm danh mục." : (message == null ? "Không thêm được danh mục." : message),
                    Toast.LENGTH_SHORT
            ).show());
        } else {
            catalogRepository.updateCategory(existing.getCategoryId(), name, imageUrl, active, (success, message) -> Toast.makeText(
                    this,
                    success ? "Đã cập nhật danh mục." : (message == null ? "Không cập nhật được danh mục." : message),
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

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryVH> {
        private final List<LocalCategory> data;

        CategoryAdapter(List<LocalCategory> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public CategoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new CategoryVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryVH holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class CategoryVH extends RecyclerView.ViewHolder {
        private final ImageView imgThumb;
        private final TextView tvName;
        private final TextView tvActive;
        private final TextView btnEdit;
        private final TextView btnDelete;

        CategoryVH(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgCategoryThumb);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvActive = itemView.findViewById(R.id.tvCategoryActive);
            btnEdit = itemView.findViewById(R.id.btnCategoryEdit);
            btnDelete = itemView.findViewById(R.id.btnCategoryDelete);
        }

        void bind(LocalCategory category) {
            tvName.setText(category.getName());
            tvActive.setText(category.isActive() ? "Đang hiển thị" : "Tạm ẩn");
            if (!TextUtils.isEmpty(category.getImageUrl())) {
                Glide.with(itemView.getContext())
                        .load(category.getImageUrl())
                        .placeholder(R.mipmap.ic_launcher_round)
                        .into(imgThumb);
            } else {
                imgThumb.setImageResource(R.mipmap.ic_launcher_round);
            }

            btnEdit.setOnClickListener(v -> showEditCategoryDialog(category));
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(QuanLyDanhMucActivity.this)
                    .setTitle("Xóa danh mục")
                    .setMessage("Bạn có chắc muốn xóa danh mục \"" + category.getName() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> catalogRepository.deleteCategory(category.getCategoryId(), (success, message) -> {
                        if (!success) {
                            Toast.makeText(QuanLyDanhMucActivity.this, message == null ? "Không xóa được danh mục." : message, Toast.LENGTH_SHORT).show();
                        }
                    }))
                    .setNegativeButton("Hủy", null)
                    .show());
        }
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
