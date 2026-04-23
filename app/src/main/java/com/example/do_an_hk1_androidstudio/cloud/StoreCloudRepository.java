package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.config.CafeStoreConfig;
import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.model.StoreBranch;
import com.example.do_an_hk1_androidstudio.local.model.StoreProfile;
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

public class StoreCloudRepository {

    public interface StoreProfileCallback {
        void onChanged(@NonNull StoreProfile profile);
    }

    public interface BranchesCallback {
        void onChanged(@NonNull List<StoreBranch> branches);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private static final String STORE_PROFILE_COLLECTION = "store_profiles";
    private static final String STORE_PROFILE_DOCUMENT = "main";
    private static final String BRANCH_COLLECTION = "store_branches";

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public StoreCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public ListenerRegistration listenStoreProfile(@NonNull StoreProfileCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(defaultProfile());
                return;
            }
            holder.setDelegate(firestore.collection(STORE_PROFILE_COLLECTION)
                    .document(STORE_PROFILE_DOCUMENT)
                    .addSnapshotListener((snapshot, error) -> {
                        if (snapshot != null && snapshot.exists()) {
                            callback.onChanged(mapProfile(snapshot));
                        } else {
                            callback.onChanged(defaultProfile());
                        }
                    }));
        });
        return holder;
    }

    public ListenerRegistration listenBranches(@NonNull BranchesCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(defaultBranches());
                return;
            }
            holder.setDelegate(firestore.collection(BRANCH_COLLECTION)
                    .addSnapshotListener((value, error) -> {
                        List<StoreBranch> branches = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                branches.add(mapBranch(snapshot));
                            }
                        }
                        if (branches.isEmpty()) {
                            branches.addAll(defaultBranches());
                        }
                        branches.sort(Comparator.comparing(StoreBranch::getName, String.CASE_INSENSITIVE_ORDER));
                        callback.onChanged(branches);
                    }));
        });
        return holder;
    }

    public void saveStoreProfile(@NonNull StoreProfile profile, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            Map<String, Object> values = new HashMap<>();
            values.put("store_name", safe(profile.getStoreName()));
            values.put("tagline", safe(profile.getTagline()));
            values.put("logo_url", normalizeNullable(profile.getLogoUrl()));
            values.put("email", safe(profile.getEmail()));
            values.put("default_branch_id", safe(profile.getDefaultBranchId()));
            values.put("updated_at", FieldValue.serverTimestamp());
            firestore.collection(STORE_PROFILE_COLLECTION)
                    .document(STORE_PROFILE_DOCUMENT)
                    .set(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void saveBranch(@Nullable String branchId,
                           @NonNull String name,
                           @NonNull String address,
                           @NonNull String phone,
                           @NonNull String hours,
                           double latitude,
                           double longitude,
                           boolean active,
                           @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            String nextBranchId = TextUtils.isEmpty(branchId) ? DataHelper.newId("branch") : branchId;
            Map<String, Object> values = new HashMap<>();
            values.put("branch_id", nextBranchId);
            values.put("name", safe(name));
            values.put("address", safe(address));
            values.put("phone", safe(phone));
            values.put("hours", safe(hours));
            values.put("latitude", latitude);
            values.put("longitude", longitude);
            values.put("is_active", active);
            values.put("updated_at", FieldValue.serverTimestamp());
            firestore.collection(BRANCH_COLLECTION)
                    .document(nextBranchId)
                    .set(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void deleteBranch(@NonNull String branchId, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection(BRANCH_COLLECTION)
                    .document(branchId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    @NonNull
    public StoreProfile defaultProfile() {
        return new StoreProfile(
                CafeStoreConfig.STORE_LABEL,
                "Coffee ordering and cafe management",
                "",
                CafeStoreConfig.STORE_EMAIL,
                "main"
        );
    }

    @NonNull
    public List<StoreBranch> defaultBranches() {
        List<StoreBranch> branches = new ArrayList<>();
        branches.add(new StoreBranch(
                "main",
                CafeStoreConfig.STORE_LABEL,
                CafeStoreConfig.STORE_ADDRESS,
                CafeStoreConfig.STORE_PHONE,
                CafeStoreConfig.STORE_HOURS,
                CafeStoreConfig.STORE_LAT,
                CafeStoreConfig.STORE_LNG,
                true
        ));
        return branches;
    }

    private StoreProfile mapProfile(DocumentSnapshot snapshot) {
        return new StoreProfile(
                stringValue(snapshot.getString("store_name"), CafeStoreConfig.STORE_LABEL),
                stringValue(snapshot.getString("tagline"), "Coffee ordering and cafe management"),
                stringValue(snapshot.getString("logo_url"), ""),
                stringValue(snapshot.getString("email"), CafeStoreConfig.STORE_EMAIL),
                stringValue(snapshot.getString("default_branch_id"), "main")
        );
    }

    private StoreBranch mapBranch(DocumentSnapshot snapshot) {
        return new StoreBranch(
                stringValue(snapshot.getString("branch_id"), snapshot.getId()),
                stringValue(snapshot.getString("name"), CafeStoreConfig.STORE_LABEL),
                stringValue(snapshot.getString("address"), CafeStoreConfig.STORE_ADDRESS),
                stringValue(snapshot.getString("phone"), CafeStoreConfig.STORE_PHONE),
                stringValue(snapshot.getString("hours"), CafeStoreConfig.STORE_HOURS),
                doubleValue(snapshot.get("latitude"), CafeStoreConfig.STORE_LAT),
                doubleValue(snapshot.get("longitude"), CafeStoreConfig.STORE_LNG),
                boolValue(snapshot.get("is_active"), true)
        );
    }

    private String safe(@Nullable String value) {
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

    private double doubleValue(@Nullable Object value, double fallback) {
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        return fallback;
    }

    private String fallbackMessage(@Nullable String value) {
        return value == null ? "Firebase auth chua san sang" : value;
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
