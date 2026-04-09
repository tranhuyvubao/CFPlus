package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.model.LocalCafeTable;
import com.example.do_an_hk1_androidstudio.local.model.LocalTableReservation;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableCloudRepository {

    public static final String TAKEAWAY_TABLE_ID = "table_takeaway";
    public static final String TAKEAWAY_TABLE_CODE = "TAKEAWAY";
    public static final String TAKEAWAY_TABLE_NAME = "Take away";

    public interface TablesCallback {
        void onChanged(List<LocalCafeTable> tables);
    }

    public interface TableLookupCallback {
        void onResult(@Nullable LocalCafeTable table);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    public interface TablesResultCallback {
        void onResult(@NonNull List<LocalCafeTable> tables, @Nullable String message);
    }

    private final FirebaseFirestore firestore;
    private final Context appContext;

    public TableCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public ListenerRegistration listenTables(@NonNull TablesCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            ensureDefaultTakeawayTable();
            holder.setDelegate(firestore.collection("tables")
                    .addSnapshotListener((value, error) -> {
                        List<LocalCafeTable> tables = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                tables.add(mapTable(doc));
                            }
                        }
                        tables.sort((first, second) -> {
                            boolean firstTakeaway = TAKEAWAY_TABLE_ID.equals(first.getTableId());
                            boolean secondTakeaway = TAKEAWAY_TABLE_ID.equals(second.getTableId());
                            if (firstTakeaway && !secondTakeaway) {
                                return -1;
                            }
                            if (!firstTakeaway && secondTakeaway) {
                                return 1;
                            }
                            return first.getName().compareToIgnoreCase(second.getName());
                        });
                        callback.onChanged(tables);
                    }));
        });
        return holder;
    }

    public void getActiveTables(@NonNull TablesResultCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(new ArrayList<>(), message);
                return;
            }
            ensureDefaultTakeawayTable();
            firestore.collection("tables")
                    .whereEqualTo("active", true)
                    .get()
                    .addOnSuccessListener(query -> {
                        List<LocalCafeTable> tables = new ArrayList<>();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            tables.add(mapTable(doc));
                        }
                        tables.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
                        callback.onResult(tables, null);
                    })
                    .addOnFailureListener(e -> callback.onResult(new ArrayList<>(), e.getMessage()));
        });
    }

    public void saveTable(@Nullable String tableId,
                          String name,
                          String code,
                          @Nullable String area,
                          String status,
                          boolean isActive,
                          @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chua san sang" : authMessage);
                return;
            }
            saveTableInternal(tableId, name, code, area, status, isActive, callback);
        });
    }

    private void saveTableInternal(@Nullable String tableId,
                                   String name,
                                   String code,
                                   @Nullable String area,
                                   String status,
                                   boolean isActive,
                                   @NonNull CompletionCallback callback) {
        if (TextUtils.isEmpty(tableId)) {
            firestore.collection("tables")
                    .get()
                    .addOnSuccessListener(query -> {
                        int nextIndex = computeNextTableIndex(query.getDocuments());
                        String finalId = String.format("table_%03d", nextIndex);
                        String finalCode = String.format("BAN%02d", nextIndex);
                        firestore.collection("tables")
                                .document(finalId)
                                .set(new TablePayload(finalId, name, finalCode, area, status, isActive, System.currentTimeMillis()))
                                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
            return;
        }

        if (TAKEAWAY_TABLE_ID.equals(tableId)) {
            callback.onComplete(false, "Ban Take away khong duoc sua");
            return;
        }

        firestore.collection("tables")
                .document(tableId)
                .set(new TablePayload(tableId, name, normalizeCode(code, name), area, status, isActive, System.currentTimeMillis()))
                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public void deleteTable(String tableId, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chua san sang" : authMessage);
                return;
            }
            if (TAKEAWAY_TABLE_ID.equals(tableId)) {
                callback.onComplete(false, "Ban Take away khong duoc xoa");
                return;
            }
            firestore.collection("tables")
                    .document(tableId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void findTableByCode(String code, @NonNull TableLookupCallback callback) {
        if (TextUtils.isEmpty(code)) {
            callback.onResult(null);
            return;
        }
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onResult(null);
                return;
            }
            firestore.collection("tables")
                    .whereEqualTo("code", code.trim().toUpperCase())
                    .whereEqualTo("active", true)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            callback.onResult(null);
                            return;
                        }
                        callback.onResult(mapTable(query.getDocuments().get(0)));
                    })
                    .addOnFailureListener(e -> callback.onResult(null));
        });
    }

    public void createReservation(String customerId,
                                  @Nullable String tableId,
                                  String tableName,
                                  long reservationTimeMillis,
                                  int guestCount,
                                  @Nullable String note,
                                  @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chua san sang" : authMessage);
                return;
            }

            firestore.collection("users")
                    .document(customerId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String fullName = valueOf(userDoc.getString("full_name"));
                        String phone = valueOf(userDoc.getString("phone"));
                        String reservationId = com.example.do_an_hk1_androidstudio.local.DataHelper.newId("reservation");

                        Map<String, Object> values = new HashMap<>();
                        values.put("reservation_id", reservationId);
                        values.put("table_id", TextUtils.isEmpty(tableId) ? null : tableId);
                        values.put("table_name", tableName);
                        values.put("customer_id", customerId);
                        values.put("customer_name", fullName);
                        values.put("customer_phone", phone);
                        values.put("reservation_time", reservationTimeMillis);
                        values.put("guest_count", guestCount);
                        values.put("note", TextUtils.isEmpty(note) ? null : note.trim());
                        values.put("status", "reserved");
                        values.put("created_at", System.currentTimeMillis());
                        values.put("updated_at", FieldValue.serverTimestamp());

                        firestore.collection("table_reservations")
                                .document(reservationId)
                                .set(values)
                                .continueWithTask(task -> {
                                    if (TextUtils.isEmpty(tableId)) {
                                        return com.google.android.gms.tasks.Tasks.forResult(null);
                                    }
                                    return firestore.collection("tables")
                                            .document(tableId)
                                            .update("status", "reserved", "updatedAt", System.currentTimeMillis());
                                })
                                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private LocalCafeTable mapTable(DocumentSnapshot doc) {
        return new LocalCafeTable(
                valueOf(doc.getString("tableId")),
                valueOf(doc.getString("name")),
                valueOf(doc.getString("code")),
                doc.getString("area"),
                valueOf(doc.getString("status")),
                Boolean.TRUE.equals(doc.getBoolean("active"))
        );
    }

    private String normalizeCode(@Nullable String value, String name) {
        String raw = value == null || value.trim().isEmpty() ? name : value;
        String normalized = raw.toUpperCase().replaceAll("[^A-Z0-9]+", "");
        return normalized.isEmpty() ? ("BAN" + (System.currentTimeMillis() % 100000)) : normalized;
    }

    private int computeNextTableIndex(List<DocumentSnapshot> documents) {
        int maxIndex = 0;
        for (DocumentSnapshot doc : documents) {
            String tableId = valueOf(doc.getString("tableId"));
            if (TAKEAWAY_TABLE_ID.equals(tableId)) {
                continue;
            }
            if (tableId.startsWith("table_")) {
                try {
                    maxIndex = Math.max(maxIndex, Integer.parseInt(tableId.substring("table_".length())));
                } catch (Exception ignored) {
                }
            }
        }
        return maxIndex + 1;
    }

    private void ensureDefaultTakeawayTable() {
        firestore.collection("tables")
                .document(TAKEAWAY_TABLE_ID)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        return;
                    }
                    firestore.collection("tables")
                            .document(TAKEAWAY_TABLE_ID)
                            .set(new TablePayload(
                                    TAKEAWAY_TABLE_ID,
                                    TAKEAWAY_TABLE_NAME,
                                    TAKEAWAY_TABLE_CODE,
                                    "Quay",
                                    "free",
                                    true,
                                    System.currentTimeMillis()
                            ));
                });
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

    private String valueOf(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static class TablePayload {
        public String tableId;
        public String name;
        public String code;
        public String area;
        public String status;
        public boolean active;
        public long updatedAt;

        TablePayload(String tableId, String name, String code, String area, String status, boolean active, long updatedAt) {
            this.tableId = tableId;
            this.name = name;
            this.code = code;
            this.area = area;
            this.status = status;
            this.active = active;
            this.updatedAt = updatedAt;
        }
    }
}
