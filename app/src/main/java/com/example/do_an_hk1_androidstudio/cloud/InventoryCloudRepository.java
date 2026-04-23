package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.model.LocalIngredient;
import com.example.do_an_hk1_androidstudio.local.model.LocalStockTransaction;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryCloudRepository {

    public interface IngredientsCallback {
        void onChanged(@NonNull List<LocalIngredient> ingredients);
    }

    public interface TransactionsCallback {
        void onChanged(@NonNull List<LocalStockTransaction> transactions);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public InventoryCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public ListenerRegistration listenIngredients(@NonNull IngredientsCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("ingredients")
                    .addSnapshotListener((value, error) -> {
                        List<LocalIngredient> ingredients = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                ingredients.add(mapIngredient(snapshot));
                            }
                        }
                        ingredients.sort(Comparator.comparing(LocalIngredient::getName, String::compareToIgnoreCase));
                        callback.onChanged(ingredients);
                    }));
        });
        return holder;
    }

    public ListenerRegistration listenRecentTransactions(int limit, @NonNull TransactionsCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("stock_transactions")
                    .limit(limit > 0 ? limit : 50)
                    .addSnapshotListener((value, error) -> {
                        List<LocalStockTransaction> transactions = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                transactions.add(mapTransaction(snapshot));
                            }
                        }
                        transactions.sort((first, second) -> Long.compare(second.getCreatedAtMillis(), first.getCreatedAtMillis()));
                        callback.onChanged(transactions);
                    }));
        });
        return holder;
    }

    public void saveIngredient(@Nullable String ingredientId,
                               String name,
                               String unit,
                               double currentQty,
                               double minStock,
                               boolean active,
                               @NonNull CompletionCallback callback) {
        String finalId = TextUtils.isEmpty(ingredientId) ? DataHelper.newId("ing") : ingredientId;
        long now = System.currentTimeMillis();
        Map<String, Object> values = new HashMap<>();
        values.put("ingredient_id", finalId);
        values.put("name", safe(name));
        values.put("unit", safe(unit));
        values.put("current_qty", currentQty);
        values.put("min_stock", minStock);
        values.put("is_active", active);
        values.put("updated_at", FieldValue.serverTimestamp());
        if (TextUtils.isEmpty(ingredientId)) {
            values.put("created_at", now);
        }

        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("ingredients")
                    .document(finalId)
                    .set(values, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void deleteIngredient(String ingredientId, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("ingredients")
                    .document(ingredientId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void createTransaction(String ingredientId,
                                  String type,
                                  double qty,
                                  @Nullable String note,
                                  @Nullable String staffId,
                                  @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("ingredients")
                    .document(ingredientId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.exists()) {
                            callback.onComplete(false, "Không tìm thấy nguyên liệu");
                            return;
                        }

                        double currentQty = doubleValue(snapshot.get("current_qty"));
                        double nextQty = currentQty;
                        if ("in".equals(type)) {
                            nextQty = currentQty + qty;
                        } else if ("out".equals(type) || "shift_usage".equals(type)) {
                            nextQty = Math.max(0, currentQty - qty);
                        } else if ("adjust".equals(type)) {
                            nextQty = qty;
                        }
                        final double finalNextQty = nextQty;

                        String transactionId = DataHelper.newId("txn");
                        long now = System.currentTimeMillis();
                        Map<String, Object> transaction = new HashMap<>();
                        transaction.put("transaction_id", transactionId);
                        transaction.put("ingredient_id", ingredientId);
                        transaction.put("type", safe(type));
                        transaction.put("qty", qty);
                        transaction.put("note", normalizeNullable(note));
                        transaction.put("staff_id", normalizeNullable(staffId));
                        transaction.put("created_at", now);

                        firestore.collection("stock_transactions")
                                .document(transactionId)
                                .set(transaction)
                                .continueWithTask(task -> firestore.collection("ingredients")
                                        .document(ingredientId)
                                        .update("current_qty", finalNextQty, "updated_at", FieldValue.serverTimestamp()))
                                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private LocalIngredient mapIngredient(DocumentSnapshot snapshot) {
        return new LocalIngredient(
                stringValue(snapshot.getString("ingredient_id"), snapshot.getId()),
                stringValue(snapshot.getString("name"), ""),
                stringValue(snapshot.getString("unit"), ""),
                doubleValue(snapshot.get("current_qty")),
                doubleValue(snapshot.get("min_stock")),
                boolValue(snapshot.get("is_active"), true)
        );
    }

    private LocalStockTransaction mapTransaction(DocumentSnapshot snapshot) {
        return new LocalStockTransaction(
                stringValue(snapshot.getString("transaction_id"), snapshot.getId()),
                stringValue(snapshot.getString("ingredient_id"), ""),
                stringValue(snapshot.getString("type"), ""),
                doubleValue(snapshot.get("qty")),
                snapshot.getString("note"),
                snapshot.getString("staff_id"),
                longValue(snapshot.get("created_at"), System.currentTimeMillis())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Nullable
    private String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stringValue(@Nullable String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private boolean boolValue(@Nullable Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private double doubleValue(@Nullable Object value) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        return 0d;
    }

    private long longValue(@Nullable Object value, long fallback) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        return fallback;
    }

    private String fallbackMessage(@Nullable String value) {
        return value == null ? "Firebase auth chưa sẵn sàng" : value;
    }

    private static class ListenerRegistrationHolder implements ListenerRegistration {
        private ListenerRegistration delegate;

        void setDelegate(ListenerRegistration delegate) {
            this.delegate = delegate;
        }

        @Override
        public void remove() {
            if (delegate != null) {
                delegate.remove();
            }
        }
    }
}
