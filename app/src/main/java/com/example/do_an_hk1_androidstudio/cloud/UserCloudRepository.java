package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.model.LocalCustomerAddress;
import com.example.do_an_hk1_androidstudio.local.model.LocalCustomerProfile;
import com.example.do_an_hk1_androidstudio.local.model.LocalUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserCloudRepository {

    public interface UserCallback {
        void onResult(@Nullable LocalUser user, @Nullable String message);
    }

    public interface CustomerProfileCallback {
        void onResult(@Nullable LocalCustomerProfile profile, @Nullable String message);
    }

    public interface AddressesCallback {
        void onResult(@NonNull List<LocalCustomerAddress> addresses, @Nullable String message);
    }

    public interface UsersCallback {
        void onChanged(@NonNull List<LocalUser> users);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    public interface ExistsCallback {
        void onResult(boolean exists, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public UserCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public void authenticate(String email, String plainPassword, @NonNull UserCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(null, fallbackMessage(message));
                return;
            }
            firestore.collection("users")
                    .whereEqualTo("email", safe(email))
                    .whereEqualTo("password_hash", DataHelper.sha256(plainPassword))
                    .whereEqualTo("status", true)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            callback.onResult(null, "Sai email hoặc mật khẩu");
                            return;
                        }
                        callback.onResult(mapUser(snapshot.getDocuments().get(0)), null);
                    })
                    .addOnFailureListener(e -> callback.onResult(null, e.getMessage()));
        });
    }

    public void emailExists(String email, @NonNull ExistsCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(false, fallbackMessage(message));
                return;
            }
            firestore.collection("users")
                    .whereEqualTo("email", safe(email))
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> callback.onResult(!snapshot.isEmpty(), null))
                    .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
        });
    }

    public void updatePasswordByEmail(String email, String plainPassword, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("users")
                    .whereEqualTo("email", safe(email))
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            callback.onComplete(false, "Không tìm thấy email");
                            return;
                        }
                        snapshot.getDocuments().get(0).getReference()
                                .update(
                                        "password_hash", DataHelper.sha256(plainPassword),
                                        "updated_at", FieldValue.serverTimestamp()
                                )
                                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void registerCustomer(String email, String plainPassword, @NonNull UserCallback callback) {
        emailExists(email, (exists, message) -> {
            if (message != null) {
                callback.onResult(null, message);
                return;
            }
            if (exists) {
                callback.onResult(null, "Email đã tồn tại");
                return;
            }

            long now = System.currentTimeMillis();
            String userId = DataHelper.newId("user");
            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", userId);
            userData.put("username", safe(email));
            userData.put("email", safe(email));
            userData.put("password_hash", DataHelper.sha256(plainPassword));
            userData.put("full_name", "");
            userData.put("phone", "");
            userData.put("role", "customer");
            userData.put("gender", null);
            userData.put("birthday", null);
            userData.put("status", true);
            userData.put("created_at", now);
            userData.put("updated_at", now);

            Map<String, Object> profile = new HashMap<>();
            profile.put("customer_id", userId);
            profile.put("loyalty_point", 0);
            profile.put("gender", null);
            profile.put("birthday", null);
            profile.put("created_at", now);
            profile.put("updated_at", now);

            firestore.collection("users")
                    .document(userId)
                    .set(userData)
                    .continueWithTask(task -> firestore.collection("customer_profiles").document(userId).set(profile))
                    .addOnSuccessListener(unused -> callback.onResult(
                            new LocalUser(userId, safe(email), safe(email), "", "", "customer", null, true, null, now, now),
                            null
                    ))
                    .addOnFailureListener(e -> callback.onResult(null, e.getMessage()));
        });
    }

    public void createStaff(String email, String plainPassword, String fullName, String phone, @NonNull UserCallback callback) {
        emailExists(email, (exists, message) -> {
            if (message != null) {
                callback.onResult(null, message);
                return;
            }
            if (exists) {
                callback.onResult(null, "Email đã tồn tại");
                return;
            }

            long now = System.currentTimeMillis();
            String userId = DataHelper.newId("staff");
            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", userId);
            userData.put("username", safe(email));
            userData.put("email", safe(email));
            userData.put("password_hash", DataHelper.sha256(plainPassword));
            userData.put("full_name", safe(fullName));
            userData.put("phone", safe(phone));
            userData.put("role", "staff");
            userData.put("gender", null);
            userData.put("birthday", null);
            userData.put("status", true);
            userData.put("created_at", now);
            userData.put("updated_at", now);

            firestore.collection("users")
                    .document(userId)
                    .set(userData)
                    .addOnSuccessListener(unused -> callback.onResult(
                            new LocalUser(userId, safe(email), safe(email), safe(fullName), safe(phone), "staff", null, true, null, now, now),
                            null
                    ))
                    .addOnFailureListener(e -> callback.onResult(null, e.getMessage()));
        });
    }

    public void updateUserProfile(String userId,
                                  String fullName,
                                  String gender,
                                  String email,
                                  String phone,
                                  @Nullable Long birthdayMillis,
                                  @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            Map<String, Object> values = new HashMap<>();
            values.put("full_name", safe(fullName));
            values.put("gender", normalizeNullable(gender));
            values.put("email", safe(email));
            values.put("username", safe(email));
            values.put("phone", safe(phone));
            values.put("birthday", birthdayMillis);
            values.put("updated_at", FieldValue.serverTimestamp());

            firestore.collection("users")
                    .document(userId)
                    .update(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void saveCustomerProfile(String customerId,
                                    @Nullable Long birthdayMillis,
                                    @Nullable String gender,
                                    @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("customer_profiles")
                    .document(customerId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        long now = System.currentTimeMillis();
                        Map<String, Object> values = new HashMap<>();
                        values.put("customer_id", customerId);
                        values.put("birthday", birthdayMillis);
                        values.put("gender", normalizeNullable(gender));
                        values.put("updated_at", FieldValue.serverTimestamp());
                        if (!snapshot.exists()) {
                            values.put("loyalty_point", 0);
                            values.put("created_at", now);
                        }
                        firestore.collection("customer_profiles")
                                .document(customerId)
                                .set(values, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void updateUserStatus(String userId, boolean active, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("users")
                    .document(userId)
                    .update("status", active, "updated_at", FieldValue.serverTimestamp())
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public ListenerRegistration listenUsersByRole(String role, @NonNull UsersCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("users")
                    .whereEqualTo("role", role)
                    .addSnapshotListener((value, error) -> {
                        List<LocalUser> users = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                users.add(mapUser(snapshot));
                            }
                        }
                        users.sort((first, second) -> Long.compare(second.getCreatedAtMillis(), first.getCreatedAtMillis()));
                        callback.onChanged(users);
                    }));
        });
        return holder;
    }

    public void getUserById(String userId, @NonNull UserCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(null, fallbackMessage(message));
                return;
            }
            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists() ? mapUser(snapshot) : null, null))
                    .addOnFailureListener(e -> callback.onResult(null, e.getMessage()));
        });
    }

    public void getCustomerProfile(String customerId, @NonNull CustomerProfileCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(null, fallbackMessage(message));
                return;
            }
            firestore.collection("customer_profiles")
                    .document(customerId)
                    .get()
                    .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists() ? mapProfile(snapshot) : null, null))
                    .addOnFailureListener(e -> callback.onResult(null, e.getMessage()));
        });
    }

    public void getCustomerAddresses(String customerId, @NonNull AddressesCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(new ArrayList<>(), fallbackMessage(message));
                return;
            }
            firestore.collection("customer_addresses")
                    .whereEqualTo("customer_id", customerId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<LocalCustomerAddress> addresses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : snapshot) {
                            addresses.add(mapAddress(document));
                        }
                        addresses.sort((first, second) -> {
                            if (first.isDefault() == second.isDefault()) {
                                return Long.compare(second.getCreatedAtMillis(), first.getCreatedAtMillis());
                            }
                            return first.isDefault() ? -1 : 1;
                        });
                        callback.onResult(addresses, null);
                    })
                    .addOnFailureListener(e -> callback.onResult(new ArrayList<>(), e.getMessage()));
        });
    }

    public void getDefaultCustomerAddress(String customerId, @NonNull UserAddressCallback callback) {
        getCustomerAddresses(customerId, (addresses, message) -> callback.onResult(addresses.isEmpty() ? null : addresses.get(0), message));
    }

    public interface UserAddressCallback {
        void onResult(@Nullable LocalCustomerAddress address, @Nullable String message);
    }

    public void saveCustomerAddress(String customerId,
                                    @Nullable String addressId,
                                    String label,
                                    String recipientName,
                                    String phone,
                                    String country,
                                    String province,
                                    String district,
                                    String ward,
                                    String detailAddress,
                                    boolean isDefault,
                                    @NonNull CompletionCallback callback) {
        getCustomerAddresses(customerId, (addresses, message) -> {
            if (message != null) {
                callback.onComplete(false, message);
                return;
            }
            if (TextUtils.isEmpty(addressId) && addresses.size() >= 3) {
                callback.onComplete(false, "Chỉ được lưu tối đa 3 địa chỉ");
                return;
            }

            FirebaseProvider.ensureAuthenticated(appContext, (success, authMessage) -> {
                if (!success) {
                    callback.onComplete(false, fallbackMessage(authMessage));
                    return;
                }
                long now = System.currentTimeMillis();
                String finalAddressId = TextUtils.isEmpty(addressId) ? DataHelper.newId("addr") : addressId;

                if (isDefault) {
                    for (LocalCustomerAddress address : addresses) {
                        firestore.collection("customer_addresses")
                                .document(address.getAddressId())
                                .update("is_default", false, "updated_at", FieldValue.serverTimestamp());
                    }
                }

                Map<String, Object> values = new HashMap<>();
                values.put("address_id", finalAddressId);
                values.put("customer_id", customerId);
                values.put("label", safe(label));
                values.put("recipient_name", safe(recipientName));
                values.put("phone", safe(phone));
                values.put("country", safe(country));
                values.put("province", safe(province));
                values.put("district", safe(district));
                values.put("ward", safe(ward));
                values.put("detail_address", safe(detailAddress));
                values.put("is_default", isDefault || addresses.isEmpty());
                values.put("updated_at", FieldValue.serverTimestamp());
                if (TextUtils.isEmpty(addressId)) {
                    values.put("created_at", now);
                }

                firestore.collection("customer_addresses")
                        .document(finalAddressId)
                        .set(values, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(unused -> callback.onComplete(true, null))
                        .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
            });
        });
    }

    public void deleteCustomerAddress(String customerId, String addressId, @NonNull CompletionCallback callback) {
        getCustomerAddresses(customerId, (addresses, message) -> {
            if (message != null) {
                callback.onComplete(false, message);
                return;
            }
            boolean wasDefault = false;
            String fallbackAddressId = null;
            for (LocalCustomerAddress address : addresses) {
                if (address.getAddressId().equals(addressId)) {
                    wasDefault = address.isDefault();
                } else if (fallbackAddressId == null) {
                    fallbackAddressId = address.getAddressId();
                }
            }
            final boolean finalWasDefault = wasDefault;
            final String finalFallbackAddressId = fallbackAddressId;

            firestore.collection("customer_addresses")
                    .document(addressId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        if (finalWasDefault && finalFallbackAddressId != null) {
                            firestore.collection("customer_addresses")
                                    .document(finalFallbackAddressId)
                                    .update("is_default", true, "updated_at", FieldValue.serverTimestamp())
                                    .addOnSuccessListener(v -> callback.onComplete(true, null))
                                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                            return;
                        }
                        callback.onComplete(true, null);
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private LocalUser mapUser(DocumentSnapshot snapshot) {
        return new LocalUser(
                stringValue(snapshot.getString("user_id"), snapshot.getId()),
                stringValue(snapshot.getString("username"), snapshot.getString("email")),
                stringValue(snapshot.getString("email"), ""),
                stringValue(snapshot.getString("full_name"), ""),
                stringValue(snapshot.getString("phone"), ""),
                stringValue(snapshot.getString("role"), "customer"),
                snapshot.getString("gender"),
                boolValue(snapshot.get("status"), true),
                longObject(snapshot.get("birthday")),
                longValue(snapshot.get("created_at"), System.currentTimeMillis()),
                longObject(snapshot.get("updated_at"))
        );
    }

    private LocalCustomerProfile mapProfile(DocumentSnapshot snapshot) {
        return new LocalCustomerProfile(
                stringValue(snapshot.getString("customer_id"), snapshot.getId()),
                intValue(snapshot.get("loyalty_point")),
                longObject(snapshot.get("birthday")),
                snapshot.getString("gender"),
                longValue(snapshot.get("created_at"), System.currentTimeMillis()),
                longObject(snapshot.get("updated_at"))
        );
    }

    private LocalCustomerAddress mapAddress(DocumentSnapshot snapshot) {
        return new LocalCustomerAddress(
                stringValue(snapshot.getString("address_id"), snapshot.getId()),
                stringValue(snapshot.getString("customer_id"), ""),
                stringValue(snapshot.getString("label"), ""),
                stringValue(snapshot.getString("recipient_name"), ""),
                stringValue(snapshot.getString("phone"), ""),
                stringValue(snapshot.getString("country"), ""),
                stringValue(snapshot.getString("province"), ""),
                stringValue(snapshot.getString("district"), ""),
                stringValue(snapshot.getString("ward"), ""),
                stringValue(snapshot.getString("detail_address"), ""),
                boolValue(snapshot.get("is_default"), false),
                longValue(snapshot.get("created_at"), System.currentTimeMillis()),
                longObject(snapshot.get("updated_at"))
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

    private String fallbackMessage(@Nullable String value) {
        return value == null ? "Firebase auth chưa sẵn sàng" : value;
    }

    private String stringValue(@Nullable String value, String fallback) {
        return value == null ? fallback : value;
    }

    private boolean boolValue(@Nullable Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private long longValue(@Nullable Object value, long fallback) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        return fallback;
    }

    @Nullable
    private Long longObject(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        return null;
    }

    private int intValue(@Nullable Object value) {
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return 0;
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
