package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudDataSeeder {

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public CloudDataSeeder(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public void ensureBootstrapManager(@NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }

            long now = System.currentTimeMillis();
            Map<String, Object> values = new HashMap<>();
            values.put("user_id", "manager_default");
            values.put("username", "admin@cfplus.app");
            values.put("email", "admin@cfplus.app");
            values.put("password_hash", DataHelper.sha256("01020304"));
            values.put("full_name", "Quản lý");
            values.put("phone", "");
            values.put("role", "manager");
            values.put("status", true);
            values.put("created_at", now);
            values.put("updated_at", FieldValue.serverTimestamp());

            firestore.collection("users")
                    .document("manager_default")
                    .set(values, SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void seedBaseData(@NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }

            long now = System.currentTimeMillis();
            WriteBatch batch = firestore.batch();

            seedManager(batch, now);
            seedStaff(batch, now);
            seedCategories(batch, now);
            seedProducts(batch, now);
            seedTables(batch, now);
            seedIngredients(batch, now);
            seedPromotions(batch, now);

            batch.commit()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void backfillProductSizes(@NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("products")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        WriteBatch batch = firestore.batch();
                        int updated = 0;
                        for (QueryDocumentSnapshot document : snapshot) {
                            Object rawSizes = document.get("available_sizes");
                            if (rawSizes instanceof List && !((List<?>) rawSizes).isEmpty()) {
                                continue;
                            }
                            String categoryId = document.getString("category_id");
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("available_sizes", defaultSizesByCategory(categoryId));
                            updates.put("updated_at", FieldValue.serverTimestamp());
                            batch.set(document.getReference(), updates, SetOptions.merge());
                            updated++;
                        }
                        if (updated == 0) {
                            callback.onComplete(true, "Không có sản phẩm nào cần cập nhật size.");
                            return;
                        }
                        final int updatedCount = updated;
                        batch.commit()
                                .addOnSuccessListener(unused -> callback.onComplete(true, "Đã cập nhật size cho " + updatedCount + " sản phẩm."))
                                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private void seedManager(WriteBatch batch, long now) {
        Map<String, Object> values = new HashMap<>();
        values.put("user_id", "manager_default");
        values.put("username", "admin@cfplus.app");
        values.put("email", "admin@cfplus.app");
        values.put("password_hash", DataHelper.sha256("01020304"));
        values.put("full_name", "Quản lý");
        values.put("phone", "");
        values.put("role", "manager");
        values.put("status", true);
        values.put("created_at", now);
        values.put("updated_at", now);
        batch.set(firestore.collection("users").document("manager_default"), values, SetOptions.merge());
    }

    private void seedStaff(WriteBatch batch, long now) {
        Map<String, Object> values = new HashMap<>();
        values.put("user_id", "staff_default");
        values.put("username", "nhanvientest@gmail.com");
        values.put("email", "nhanvientest@gmail.com");
        values.put("password_hash", DataHelper.sha256("01020304"));
        values.put("full_name", "Nhân viên");
        values.put("phone", "0301040204");
        values.put("role", "staff");
        values.put("status", true);
        values.put("created_at", now);
        values.put("updated_at", now);
        batch.set(firestore.collection("users").document("staff_default"), values, SetOptions.merge());
    }

    private void seedCategories(WriteBatch batch, long now) {
        writeCategory(batch, "cafe", "Cà phê", "https://images.pexels.com/photos/20205947/pexels-photo-20205947.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeCategory(batch, "tra_sua", "Trà sữa", "https://images.pexels.com/photos/27126830/pexels-photo-27126830.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeCategory(batch, "matcha", "Matcha", "https://images.pexels.com/photos/12201274/pexels-photo-12201274.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeCategory(batch, "tra_trai_cay", "Trà trái cây", "https://images.pexels.com/photos/5798/tea-cup-tea-time-table-cloth.jpg?auto=compress&cs=tinysrgb&w=900", now);
        writeCategory(batch, "an_vat", "Ăn vặt", "https://images.pexels.com/photos/1583884/pexels-photo-1583884.jpeg?auto=compress&cs=tinysrgb&w=900", now);
    }

    private void seedProducts(WriteBatch batch, long now) {
        writeProduct(batch, "p_cafe_sua_da", "cafe", "Cà phê sữa đá", "Cà phê rang xay đậm vị, pha cùng sữa đặc và đá lạnh dễ uống.", 29000, "https://images.pexels.com/photos/20205947/pexels-photo-20205947.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_bac_xiu", "cafe", "Bạc xỉu", "Thơm mùi cà phê nhẹ, vị sữa béo ngọt phù hợp uống buổi sáng.", 32000, "https://images.pexels.com/photos/302899/pexels-photo-302899.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_tra_sua_tran_chau", "tra_sua", "Trà sữa trân châu", "Trà sữa thơm béo với topping trân châu dai mềm.", 35000, "https://images.pexels.com/photos/27126830/pexels-photo-27126830.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_tra_sua_caramel", "tra_sua", "Trà sữa caramel", "Vị caramel dịu ngọt, lớp trà sữa mượt và thơm.", 39000, "https://images.pexels.com/photos/11160122/pexels-photo-11160122.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_matcha_latte", "matcha", "Matcha latte", "Matcha thanh mát hòa cùng sữa tươi, hợp cho khách thích vị trà xanh.", 38000, "https://images.pexels.com/photos/12201274/pexels-photo-12201274.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_matcha_sua_dua", "matcha", "Matcha sữa dừa", "Matcha mát dịu kết hợp sữa dừa thơm béo, hậu vị êm.", 42000, "https://images.pexels.com/photos/5947024/pexels-photo-5947024.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_tra_dao_cam_sa", "tra_trai_cay", "Trà đào cam sả", "Trà trái cây thanh mát với đào ngâm, cam lát và mùi sả nhẹ.", 39000, "https://images.pexels.com/photos/5946972/pexels-photo-5946972.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_chanh_day_da", "tra_trai_cay", "Chanh dây đá", "Đồ uống chua ngọt giải nhiệt, hợp gọi kèm đồ ăn vặt.", 33000, "https://images.pexels.com/photos/5798/tea-cup-tea-time-table-cloth.jpg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_khoai_tay_chien", "an_vat", "Khoai tây chiên", "Khoai tây vàng giòn, dễ dùng chung cho nhóm bạn.", 45000, "https://images.pexels.com/photos/1583884/pexels-photo-1583884.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_ga_vien_chien", "an_vat", "Gà viên chiên", "Viên gà chiên giòn, chấm tương ớt hoặc sốt mayo đều hợp.", 52000, "https://images.pexels.com/photos/7428284/pexels-photo-7428284.jpeg?auto=compress&cs=tinysrgb&w=900", now);
        writeProduct(batch, "p_pho_mai_que", "an_vat", "Phô mai que", "Phô mai que kéo sợi, món ăn vặt dễ bán trong quán.", 48000, "https://images.pexels.com/photos/9650081/pexels-photo-9650081.jpeg?auto=compress&cs=tinysrgb&w=900", now);
    }

    private void seedTables(WriteBatch batch, long now) {
        writeTable(batch, TableCloudRepository.TAKEAWAY_TABLE_ID, TableCloudRepository.TAKEAWAY_TABLE_NAME, TableCloudRepository.TAKEAWAY_TABLE_CODE, "Quầy", "free", true, now);
        writeTable(batch, "table_001", "Bàn 1", "BAN01", "Tầng 1", "free", true, now);
        writeTable(batch, "table_002", "Bàn 2", "BAN02", "Tầng 1", "free", true, now);
    }

    private void seedIngredients(WriteBatch batch, long now) {
        writeIngredient(batch, "sua_tuoi", "Sữa tươi", "ml", 5000.0, 1000.0, now);
        writeIngredient(batch, "bot_matcha", "Bột matcha", "g", 1000.0, 200.0, now);
        writeIngredient(batch, "tran_chau_den", "Trân châu đen", "g", 2500.0, 400.0, now);
        writeIngredient(batch, "khoai_tay_cat", "Khoai tây cắt sẵn", "g", 5000.0, 800.0, now);
    }

    private void seedPromotions(WriteBatch batch, long now) {
        Map<String, Object> values = new HashMap<>();
        values.put("promotion_id", "promo_sale10");
        values.put("code", "SALE10");
        values.put("type", "percent");
        values.put("value", 10.0);
        values.put("min_order", 50000);
        values.put("max_discount", 20000);
        values.put("start_date", null);
        values.put("end_date", null);
        values.put("is_active", true);
        values.put("created_at", now);
        values.put("updated_at", now);
        batch.set(firestore.collection("promotions").document("promo_sale10"), values, SetOptions.merge());
    }

    private void writeCategory(WriteBatch batch, String id, String name, @Nullable String imageUrl, long now) {
        Map<String, Object> values = new HashMap<>();
        values.put("category_id", id);
        values.put("name", name);
        values.put("image_url", imageUrl);
        values.put("is_active", true);
        values.put("created_at", now);
        values.put("updated_at", now);
        batch.set(firestore.collection("categories").document(id), values, SetOptions.merge());
    }

    private void writeProduct(WriteBatch batch,
                              String id,
                              String categoryId,
                              String name,
                              @Nullable String description,
                              int basePrice,
                              @Nullable String imageUrl,
                              long now) {
        Map<String, Object> values = new HashMap<>();
        values.put("product_id", id);
        values.put("category_id", categoryId);
        values.put("name", name);
        values.put("description", description);
        values.put("base_price", basePrice);
        values.put("image_url", imageUrl);
        values.put("available_sizes", defaultSizesByCategory(categoryId));
        values.put("is_active", true);
        values.put("created_at", now);
        values.put("updated_at", now);
        batch.set(firestore.collection("products").document(id), values, SetOptions.merge());
    }

    private void writeTable(WriteBatch batch, String id, String name, String code, @Nullable String area, String status, boolean active, long now) {
        Map<String, Object> values = new HashMap<>();
        values.put("tableId", id);
        values.put("name", name);
        values.put("code", code);
        values.put("area", area);
        values.put("status", status);
        values.put("active", active);
        values.put("updatedAt", now);
        batch.set(firestore.collection("tables").document(id), values, SetOptions.merge());
    }

    private void writeIngredient(WriteBatch batch, String id, String name, String unit, double currentQty, double minStock, long now) {
        Map<String, Object> values = new HashMap<>();
        values.put("ingredient_id", id);
        values.put("name", name);
        values.put("unit", unit);
        values.put("current_qty", currentQty);
        values.put("min_stock", minStock);
        values.put("is_active", true);
        values.put("created_at", now);
        values.put("updated_at", now);
        batch.set(firestore.collection("ingredients").document(id), values, SetOptions.merge());
    }

    private String fallbackMessage(@Nullable String value) {
        return value == null ? "Firebase chưa sẵn sàng" : value;
    }

    @NonNull
    private List<String> defaultSizesByCategory(@Nullable String categoryId) {
        if ("an_vat".equals(categoryId)) {
            return Arrays.asList("M");
        }
        return Arrays.asList("S", "M", "L");
    }
}
