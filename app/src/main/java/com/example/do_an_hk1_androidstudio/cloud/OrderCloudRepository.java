package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.CustomerCartItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalCustomerAddress;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.room.PendingSyncRepository;
import com.example.do_an_hk1_androidstudio.ui.NotificationCenter;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderCloudRepository {
    private static final String TAG = "OrderCloudRepository";
    private static final long ORDER_WRITE_TIMEOUT_MS = 15000L;

    public interface OrdersCallback {
        void onChanged(List<LocalOrder> orders);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    public interface OrderCreateCallback {
        void onComplete(boolean success, @Nullable String orderId, @Nullable String message);
    }

    private final FirebaseFirestore firestore;
    private final Context appContext;
    private final PendingSyncRepository pendingSyncRepository;
    private final LocalSessionManager sessionManager;
    private final OrderActionLogRepository actionLogRepository;
    private final StaffNotificationCloudRepository staffNotificationRepository;
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public OrderCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
        pendingSyncRepository = new PendingSyncRepository(appContext);
        sessionManager = new LocalSessionManager(appContext);
        actionLogRepository = new OrderActionLogRepository(appContext);
        staffNotificationRepository = new StaffNotificationCloudRepository(appContext);
    }

    public void createCustomerOrder(String orderType,
                                    String orderChannel,
                                    String customerId,
                                    @Nullable String tableId,
                                    @Nullable String tableName,
                                    @Nullable String productId,
                                    String productName,
                                    int unitPrice,
                                    int qty,
                                    @Nullable String variantName,
                                    @Nullable String note,
                                    @Nullable String imageUrl,
                                    @Nullable LocalCustomerAddress deliveryAddress,
                                    @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }
            long createdAt = System.currentTimeMillis();
            String orderId = DataHelper.newId("cloud_order");
            String orderCode = DataHelper.newOrderCode(createdAt);
            int lineTotal = unitPrice * qty;
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", orderId);
            data.put("orderCode", orderCode);
            data.put("orderType", orderType);
            data.put("orderChannel", orderChannel);
            data.put("tableId", nullableTrim(tableId));
            data.put("tableName", nullableTrim(tableName));
            data.put("customerId", customerId);
            data.put("staffId", null);
            data.put("status", "created");
            data.put("subtotal", lineTotal);
            data.put("discountAmount", 0);
            data.put("total", lineTotal);
            data.put("createdAt", createdAt);
            data.put("updatedAt", createdAt);
            data.put("productId", nullableTrim(productId));
            data.put("productName", productName);
            data.put("unitPrice", unitPrice);
            data.put("qty", qty);
            data.put("variantName", nullableTrim(variantName));
            data.put("note", nullableTrim(note));
            data.put("imageUrl", nullableTrim(imageUrl));
            if (deliveryAddress != null) {
                data.put("deliveryAddressId", deliveryAddress.getAddressId());
                data.put("deliveryRecipientName", nullableTrim(deliveryAddress.getRecipientName()));
                data.put("deliveryPhone", nullableTrim(deliveryAddress.getPhone()));
                data.put("deliveryCountry", nullableTrim(deliveryAddress.getCountry()));
                data.put("deliveryProvince", nullableTrim(deliveryAddress.getProvince()));
                data.put("deliveryDistrict", nullableTrim(deliveryAddress.getDistrict()));
                data.put("deliveryWard", nullableTrim(deliveryAddress.getWard()));
                data.put("deliveryDetailAddress", nullableTrim(deliveryAddress.getDetailAddress()));
            }

            firestore.collection("orders")
                    .document(orderId)
                    .set(data)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void createOnlineAppOrder(String customerId,
                                     @Nullable String productId,
                                     String productName,
                                     int unitPrice,
                                     int qty,
                                     @Nullable String variantName,
                                     @Nullable String note,
                                     @Nullable String imageUrl,
                                     @Nullable LocalCustomerAddress deliveryAddress,
                                     @NonNull CompletionCallback callback) {
        createCustomerOrder("online", "customer_app", customerId, null, null, productId, productName, unitPrice, qty, variantName, note, imageUrl, deliveryAddress, callback);
    }

    public void createOnlineCartOrder(String customerId,
                                      @NonNull List<CustomerCartItem> items,
                                      @NonNull LocalCustomerAddress deliveryAddress,
                                      @Nullable String note,
                                      @Nullable String paymentPreference,
                                      @NonNull OrderCreateCallback callback) {
        if (items.isEmpty()) {
            callback.onComplete(false, null, "Giỏ hàng đang trống.");
            return;
        }

        Log.d(TAG, "createOnlineCartOrder start customerId=" + customerId
                + ", items=" + items.size()
                + ", payment=" + paymentPreference);
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                Log.e(TAG, "createOnlineCartOrder auth failed: " + authMessage);
                callback.onComplete(false, null, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }

            long createdAt = System.currentTimeMillis();
            String orderId = DataHelper.newId("online_order");
            String orderCode = DataHelper.newOrderCode(createdAt);
            int subtotal = 0;
            List<Map<String, Object>> orderItems = new ArrayList<>();
            for (CustomerCartItem item : items) {
                subtotal += item.getLineTotal();
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("productId", item.getProductId());
                itemData.put("productName", item.getProductName());
                itemData.put("variantName", item.buildVariantLabel());
                itemData.put("qty", item.getQuantity());
                itemData.put("unitPrice", item.getUnitPrice());
                itemData.put("note", nullableTrim(item.getNote()));
                itemData.put("lineTotal", item.getLineTotal());
                itemData.put("imageUrl", nullableTrim(item.getImageUrl()));
                orderItems.add(itemData);
            }

            CustomerCartItem firstItem = items.get(0);
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", orderId);
            data.put("orderCode", orderCode);
            data.put("orderType", "online");
            data.put("orderChannel", "customer_app");
            data.put("tableId", null);
            data.put("tableName", null);
            data.put("customerId", customerId);
            data.put("staffId", null);
            data.put("status", "created");
            data.put("subtotal", subtotal);
            data.put("discountAmount", 0);
            data.put("total", subtotal);
            data.put("createdAt", createdAt);
            data.put("updatedAt", createdAt);
            data.put("productId", firstItem.getProductId());
            data.put("productName", firstItem.getProductName());
            data.put("unitPrice", firstItem.getUnitPrice());
            data.put("qty", firstItem.getQuantity());
            data.put("variantName", firstItem.buildVariantLabel());
            data.put("note", nullableTrim(note));
            data.put("imageUrl", nullableTrim(firstItem.getImageUrl()));
            data.put("paymentPreference", nullableTrim(paymentPreference));
            data.put("deliveryAddressId", deliveryAddress.getAddressId());
            data.put("deliveryRecipientName", nullableTrim(deliveryAddress.getRecipientName()));
            data.put("deliveryPhone", nullableTrim(deliveryAddress.getPhone()));
            data.put("deliveryCountry", nullableTrim(deliveryAddress.getCountry()));
            data.put("deliveryProvince", nullableTrim(deliveryAddress.getProvince()));
            data.put("deliveryDistrict", nullableTrim(deliveryAddress.getDistrict()));
            data.put("deliveryWard", nullableTrim(deliveryAddress.getWard()));
            data.put("deliveryDetailAddress", nullableTrim(deliveryAddress.getDetailAddress()));
            data.put("items", orderItems);

            final boolean[] handled = {false};
            final Runnable[] timeoutRunnable = new Runnable[1];
            timeoutRunnable[0] = () -> {
                if (handled[0]) {
                    return;
                }
                handled[0] = true;
                Log.w(TAG, "createOnlineCartOrder timed out waiting for Firestore ACK orderId=" + orderId);
                callback.onComplete(
                        false,
                        null,
                        "Không kết nối được Firestore (firestore.googleapis.com). Kiểm tra DNS/mạng của emulator rồi thử lại."
                );
            };
            mainHandler.postDelayed(timeoutRunnable[0], ORDER_WRITE_TIMEOUT_MS);

            firestore.collection("orders")
                    .document(orderId)
                    .set(data)
                    .addOnSuccessListener(unused -> {
                        if (handled[0]) {
                            return;
                        }
                        handled[0] = true;
                        mainHandler.removeCallbacks(timeoutRunnable[0]);
                        Log.d(TAG, "createOnlineCartOrder success orderId=" + orderId);
                        emitStaffNotification(
                                "new_online_order",
                                "new_online_order_" + orderId,
                                "Đơn online mới",
                                "Đơn " + orderCode + " vừa được tạo từ khách đặt qua app.",
                                orderId,
                                "customer_app",
                                null,
                                null,
                                "created"
                        );
                        callback.onComplete(true, orderId, null);
                    })
                    .addOnFailureListener(e -> {
                        if (handled[0]) {
                            return;
                        }
                        handled[0] = true;
                        mainHandler.removeCallbacks(timeoutRunnable[0]);
                        Log.e(TAG, "createOnlineCartOrder failed orderId=" + orderId, e);
                        callback.onComplete(false, null, buildOrderCreateFailureMessage(e));
                    });
        });
    }

    private String buildOrderCreateFailureMessage(@Nullable Exception e) {
        String message = e == null ? null : e.getMessage();
        if (TextUtils.isEmpty(message)) {
            return "Không thể tạo đơn online. Kiểm tra kết nối Firestore rồi thử lại.";
        }
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("unavailable")
                || lowerMessage.contains("unable to resolve host")
                || lowerMessage.contains("firestore.googleapis.com")
                || lowerMessage.contains("unknownhost")) {
            return "Không kết nối được Firestore (firestore.googleapis.com). Kiểm tra DNS/mạng của emulator rồi thử lại.";
        }
        return message;
    }

    private void emitStaffNotification(@NonNull String type,
                                       @NonNull String eventKey,
                                       @NonNull String title,
                                       @NonNull String body,
                                       @Nullable String orderId,
                                       @Nullable String orderChannel,
                                       @Nullable String tableId,
                                       @Nullable String tableName,
                                       @Nullable String status) {
        staffNotificationRepository.createNotification(
                eventKey,
                eventKey,
                type,
                title,
                body,
                orderId,
                orderChannel,
                tableId,
                tableName,
                status,
                (success, message) -> {
                    if (!success) {
                        Log.w(TAG, "Could not emit staff notification: " + message);
                    }
                }
        );
    }

    public void createTableQrOrder(String customerId,
                                   @Nullable String tableId,
                                   @Nullable String tableName,
                                   @Nullable String productId,
                                   String productName,
                                   int unitPrice,
                                   int qty,
                                   @Nullable String variantName,
                                   @Nullable String note,
                                   @Nullable String imageUrl,
                                   @NonNull CompletionCallback callback) {
        String orderType = TableCloudRepository.TAKEAWAY_TABLE_ID.equals(tableId) ? "takeaway" : "dine_in";
        createCustomerOrder(orderType, "customer_qr", customerId, tableId, tableName, productId, productName, unitPrice, qty, variantName, note, imageUrl, null, callback);
    }

    public ListenerRegistration listenOnlineOrders(@NonNull OrdersCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("orders")
                    .addSnapshotListener((value, error) -> callback.onChanged(filterAndMap(value, null, true))));
        });
        return holder;
    }

    public ListenerRegistration listenAllOrders(@NonNull OrdersCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("orders")
                    .addSnapshotListener((value, error) -> callback.onChanged(filterAndMap(value, null, false))));
        });
        return holder;
    }

    public ListenerRegistration listenOrdersByCustomer(String customerId, @NonNull OrdersCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("orders")
                    .whereEqualTo("customerId", customerId)
                    .addSnapshotListener((value, error) -> callback.onChanged(filterAndMap(value, customerId, false))));
        });
        return holder;
    }

    public ListenerRegistration listenOrdersByStaff(String staffId, @NonNull OrdersCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("orders")
                    .whereEqualTo("staffId", staffId)
                    .addSnapshotListener((value, error) -> callback.onChanged(filterAndMap(value, null, false))));
        });
        return holder;
    }

    public void updateOrderStatus(String orderId, String nextStatus, @Nullable String staffId, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", nextStatus);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            updates.put("needsStaffAttention", false);
            updates.put("lastCustomerAction", null);
            updates.put("supportRequestNote", null);
            updates.put("lastCustomerItemAddedQty", 0);
            updates.put("lastCustomerItemAddedAt", 0L);
            updates.put("lastCustomerAddedItems", new ArrayList<>());
            if (!TextUtils.isEmpty(staffId)) {
                updates.put("staffId", staffId);
            }
            firestore.collection("orders")
                    .document(orderId)
                    .get()
                    .continueWithTask(getTask -> {
                        DocumentSnapshot orderSnapshot = getTask.getResult();
                        return firestore.collection("orders")
                                .document(orderId)
                                .update(updates)
                                .continueWithTask(updateTask -> releaseTableIfNeeded(orderSnapshot, nextStatus));
                    })
                    .addOnSuccessListener(unused -> {
                        actionLogRepository.log(orderId, mapStatusAction(nextStatus), staffId, buildActorName(), null);
                        NotificationCenter.storeAndShow(
                                appContext,
                                "Đơn hàng đã cập nhật",
                                "Đơn " + orderId + " vừa chuyển sang trạng thái " + nextStatus + ".",
                                "order_status",
                                orderId,
                                nextStatus
                        );
                        callback.onComplete(true, null);
                    })
                    .addOnFailureListener(e -> {
                        if (pendingSyncRepository.isLikelyNetworkIssue(e.getMessage())) {
                            enqueuePendingStatusUpdate(orderId, nextStatus, staffId);
                            callback.onComplete(true, "Đã lưu offline, sẽ tự đồng bộ khi có mạng.");
                            return;
                        }
                        callback.onComplete(false, e.getMessage());
                    });
        });
    }

    public void updateOrderItems(@NonNull String orderId,
                                 @NonNull List<LocalOrderItem> items,
                                 int discountAmount,
                                 @Nullable String staffId,
                                 @NonNull CompletionCallback callback) {
        if (items.isEmpty()) {
            callback.onComplete(false, "Đơn hiện không còn món nào để lưu.");
            return;
        }

        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }

            List<Map<String, Object>> orderItems = new ArrayList<>();
            int subtotal = 0;
            for (LocalOrderItem item : items) {
                int qty = Math.max(1, item.getQty());
                int unitPrice = Math.max(0, item.getUnitPrice());
                int lineTotal = qty * unitPrice;
                subtotal += lineTotal;

                Map<String, Object> itemData = new HashMap<>();
                itemData.put("itemId", nullableTrim(item.getItemId()));
                itemData.put("productId", nullableTrim(item.getProductId()));
                itemData.put("productName", nullableTrim(item.getProductName()));
                itemData.put("variantName", nullableTrim(item.getVariantName()));
                itemData.put("qty", qty);
                itemData.put("unitPrice", unitPrice);
                itemData.put("note", nullableTrim(item.getNote()));
                itemData.put("lineTotal", lineTotal);
                itemData.put("imageUrl", nullableTrim(item.getImageUrl()));
                orderItems.add(itemData);
            }

            LocalOrderItem firstItem = items.get(0);
            Map<String, Object> updates = new HashMap<>();
            updates.put("items", orderItems);
            updates.put("productId", nullableTrim(firstItem.getProductId()));
            updates.put("productName", nullableTrim(firstItem.getProductName()));
            updates.put("unitPrice", Math.max(0, firstItem.getUnitPrice()));
            updates.put("qty", Math.max(1, firstItem.getQty()));
            updates.put("variantName", nullableTrim(firstItem.getVariantName()));
            updates.put("note", nullableTrim(firstItem.getNote()));
            updates.put("imageUrl", nullableTrim(firstItem.getImageUrl()));
            updates.put("subtotal", subtotal);
            updates.put("discountAmount", Math.max(0, discountAmount));
            updates.put("total", Math.max(0, subtotal - Math.max(0, discountAmount)));
            updates.put("updatedAt", FieldValue.serverTimestamp());
            updates.put("needsStaffAttention", false);
            updates.put("lastCustomerAction", null);
            updates.put("supportRequestNote", null);
            updates.put("lastCustomerItemAddedQty", 0);
            updates.put("lastCustomerItemAddedAt", 0L);
            updates.put("lastCustomerAddedItems", new ArrayList<>());
            if (!TextUtils.isEmpty(staffId)) {
                updates.put("staffId", staffId);
            }

            firestore.collection("orders")
                    .document(orderId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        actionLogRepository.log(orderId, "Sửa món trong đơn", staffId, buildActorName(), null);
                        NotificationCenter.storeAndShow(
                                appContext,
                                "Đơn hàng đã được chỉnh sửa",
                                "Nhân viên vừa cập nhật lại món trong đơn " + orderId + ".",
                                "order_update",
                                orderId,
                                null
                        );
                        callback.onComplete(true, null);
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void updateOrderTable(String orderId,
                                 @Nullable String tableName,
                                 @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("tableName", nullableTrim(tableName));
            updates.put("updatedAt", FieldValue.serverTimestamp());
            firestore.collection("orders")
                    .document(orderId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        emitStaffNotification(
                                "customer_support_request",
                                "customer_support_request_" + orderId,
                                "Khách cần hỗ trợ",
                                "Đơn " + orderId + " vừa gửi yêu cầu nhân viên kiểm tra và hỗ trợ sửa món.",
                                orderId,
                                null,
                                null,
                                null,
                                "created"
                        );
                        callback.onComplete(true, null);
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void reassignOrderTable(String orderId,
                                   @Nullable String oldTableId,
                                   @Nullable String newTableId,
                                   @Nullable String newTableName,
                                   @Nullable String staffId,
                                   @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("tableId", nullableTrim(newTableId));
            updates.put("tableName", nullableTrim(newTableName));
            updates.put("updatedAt", FieldValue.serverTimestamp());
            if (!TextUtils.isEmpty(staffId)) {
                updates.put("staffId", staffId);
            }

            firestore.collection("orders")
                    .document(orderId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        if (!TextUtils.isEmpty(oldTableId)
                                && !TableCloudRepository.TAKEAWAY_TABLE_ID.equals(oldTableId)
                                && !oldTableId.equals(newTableId)) {
                            Map<String, Object> sourceTable = new HashMap<>();
                            sourceTable.put("status", "free");
                            sourceTable.put("updatedAt", System.currentTimeMillis());
                            firestore.collection("tables").document(oldTableId).update(sourceTable);
                        }
                        if (!TextUtils.isEmpty(newTableId)
                                && !TableCloudRepository.TAKEAWAY_TABLE_ID.equals(newTableId)) {
                            Map<String, Object> targetTable = new HashMap<>();
                            targetTable.put("status", "occupied");
                            targetTable.put("updatedAt", System.currentTimeMillis());
                            firestore.collection("tables").document(newTableId).update(targetTable);
                        }
                        actionLogRepository.log(orderId, "Đổi bàn cho khách", staffId, buildActorName(), nullableTrim(newTableName));
                        emitStaffNotification(
                                "table_changed",
                                "table_changed_" + orderId + "_" + String.valueOf(newTableId),
                                "Đã đổi bàn cho khách",
                                "Don " + orderId + " vua duoc chuyen sang " + String.valueOf(newTableName) + ".",
                                orderId,
                                null,
                                newTableId,
                                newTableName,
                                "created"
                        );
                        callback.onComplete(true, null);
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void cancelOrder(String orderId, @NonNull CompletionCallback callback) {
        updateOrderStatus(orderId, "cancelled", null, callback);
    }

    public void requestStaffSupport(String orderId, @Nullable String requestNote, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("needsStaffAttention", true);
            updates.put("lastCustomerAction", "support_request");
            updates.put("lastCustomerItemAddedAt", System.currentTimeMillis());
            updates.put("lastCustomerItemAddedQty", 0);
            updates.put("lastCustomerAddedItems", new ArrayList<>());
            updates.put("updatedAt", FieldValue.serverTimestamp());
            updates.put("supportRequestNote", TextUtils.isEmpty(requestNote) ? null : requestNote.trim());
            firestore.collection("orders")
                    .document(orderId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        String orderLabel = orderId;
                        String supportText = TextUtils.isEmpty(requestNote) ? "Khách cần hỗ trợ sửa món." : requestNote.trim();
                        emitStaffNotification(
                                "customer_support_request",
                                "customer_support_request_" + orderId + "_" + System.currentTimeMillis(),
                                "Khách yêu cầu sửa món",
                                "Đơn " + orderLabel + ": " + supportText,
                                orderId,
                                "customer_app",
                                null,
                                null,
                                "created"
                        );
                        callback.onComplete(true, null);
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void setOrderPaymentStatus(String orderId,
                                      int amount,
                                      boolean paid,
                                      @NonNull CompletionCallback callback) {
        if (paid) {
            payOrder(orderId, amount, "cash", null, 0, null, callback);
            return;
        }
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "created");
            updates.put("discountAmount", 0);
            updates.put("total", amount);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            firestore.collection("orders")
                    .document(orderId)
                    .update(updates)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void payOrder(String orderId,
                         int originalAmount,
                         String method,
                         @Nullable String bankRef,
                         int discountAmount,
                         @Nullable String promotionCode,
                         @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (authSuccess, authMessage) -> {
            if (!authSuccess) {
                callback.onComplete(false, authMessage == null ? "Firebase auth chưa sẵn sàng" : authMessage);
                return;
            }
            int finalAmount = Math.max(0, originalAmount - discountAmount);
            Map<String, Object> payment = new HashMap<>();
            payment.put("orderId", orderId);
            payment.put("method", method);
            payment.put("bankRef", nullableTrim(bankRef));
            payment.put("amount", finalAmount);
            payment.put("discountAmount", discountAmount);
            payment.put("promotionCode", nullableTrim(promotionCode));
            payment.put("status", "success");
            payment.put("createdAt", System.currentTimeMillis());

            Map<String, Object> orderUpdate = new HashMap<>();
            orderUpdate.put("status", "paid");
            orderUpdate.put("total", finalAmount);
            orderUpdate.put("discountAmount", discountAmount);
            orderUpdate.put("updatedAt", FieldValue.serverTimestamp());

            firestore.collection("payments")
                    .document(orderId)
                    .set(payment)
                    .continueWithTask(task -> firestore.collection("orders").document(orderId).get())
                    .continueWithTask(getOrderTask -> {
                        DocumentSnapshot orderSnapshot = getOrderTask.getResult();
                        return firestore.collection("orders")
                                .document(orderId)
                                .update(orderUpdate)
                                .continueWithTask(updateTask -> releaseTableIfNeeded(orderSnapshot, "paid"));
                    })
                    .addOnSuccessListener(unused -> {
                        actionLogRepository.log(orderId, "Thanh toán đơn", sessionManager.getCurrentUserId(), buildActorName(), method);
                        NotificationCenter.storeAndShow(
                                appContext,
                                "Thanh toán thành công",
                                "Đơn hàng đã được ghi nhận thanh toán.",
                                "payment",
                                orderId,
                                "paid"
                        );
                        callback.onComplete(true, null);
                    })
                    .addOnFailureListener(e -> {
                        if (pendingSyncRepository.isLikelyNetworkIssue(e.getMessage())) {
                            enqueuePendingPayment(orderId, finalAmount, method, bankRef, discountAmount, promotionCode);
                            callback.onComplete(true, "Đã lưu thanh toán offline, sẽ tự đồng bộ khi có mạng.");
                            return;
                        }
                        callback.onComplete(false, e.getMessage());
                    });
        });
    }

    private void enqueuePendingStatusUpdate(@NonNull String orderId,
                                            @NonNull String nextStatus,
                                            @Nullable String staffId) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("orderId", orderId);
            payload.put("nextStatus", nextStatus);
            payload.put("staffId", nullableTrim(staffId));
            pendingSyncRepository.enqueue(PendingSyncRepository.ACTION_UPDATE_STATUS, payload.toString());
        } catch (JSONException ignored) {
        }
    }

    private void enqueuePendingPayment(@NonNull String orderId,
                                       int amount,
                                       @NonNull String method,
                                       @Nullable String bankRef,
                                       int discountAmount,
                                       @Nullable String promotionCode) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("orderId", orderId);
            payload.put("amount", amount);
            payload.put("method", method);
            payload.put("bankRef", nullableTrim(bankRef));
            payload.put("discountAmount", discountAmount);
            payload.put("promotionCode", nullableTrim(promotionCode));
            pendingSyncRepository.enqueue(PendingSyncRepository.ACTION_PAY_ORDER, payload.toString());
        } catch (JSONException ignored) {
        }
    }

    @NonNull
    private String buildActorName() {
        String fullName = sessionManager.getCurrentUserFullName();
        if (!TextUtils.isEmpty(fullName)) {
            return fullName;
        }
        String email = sessionManager.getCurrentUserEmail();
        return email == null ? "Nhân viên quán" : email;
    }

    @NonNull
    private String mapStatusAction(@NonNull String nextStatus) {
        switch (nextStatus) {
            case "confirmed":
                return "Xác nhận đơn";
            case "paid":
                return "Hoàn tất / thanh toán đơn";
            case "cancelled":
                return "Hủy đơn";
            default:
                return "Cập nhật trạng thái đơn";
        }
    }

    private List<LocalOrder> filterAndMap(@Nullable com.google.firebase.firestore.QuerySnapshot snapshot,
                                          @Nullable String customerId,
                                          boolean onlineOnly) {
        List<LocalOrder> result = new ArrayList<>();
        if (snapshot == null) {
            return result;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String orderType = valueOf(doc.getString("orderType"));
            String orderChannel = normalizedOrderChannel(doc, orderType);
            String status = valueOf(doc.getString("status"));
            if (onlineOnly && !("customer_app".equals(orderChannel) || "customer_qr".equals(orderChannel))) {
                continue;
            }
            if (customerId != null && !customerId.equals(doc.getString("customerId"))) {
                continue;
            }
            result.add(mapOrder(doc));
        }
        result.sort(Comparator.comparingLong(LocalOrder::getCreatedAtMillis).reversed());
        return result;
    }

    private LocalOrder mapOrder(DocumentSnapshot doc) {
        return new LocalOrder(
                valueOf(doc.getString("orderId")),
                valueOf(doc.getString("orderCode")),
                normalizedOrderType(valueOf(doc.getString("orderType"))),
                normalizedOrderChannel(doc, valueOf(doc.getString("orderType"))),
                doc.getString("tableId"),
                doc.getString("tableName"),
                doc.getString("customerId"),
                doc.getString("staffId"),
                valueOf(doc.getString("status")),
                intValue(doc.getLong("subtotal")),
                intValue(doc.getLong("discountAmount")),
                intValue(doc.getLong("total")),
                buildDeliveryAddress(doc),
                longValue(doc.get("createdAt")),
                Boolean.TRUE.equals(doc.getBoolean("needsStaffAttention")),
                intValue(doc.getLong("lastCustomerItemAddedQty")),
                longValueOrZero(doc.get("lastCustomerItemAddedAt")),
                mapSnapshotItems(doc.get("lastCustomerAddedItems"), valueOf(doc.getString("orderId"))),
                doc.getString("supportRequestNote"),
                mapOrderItems(doc)
        );
    }

    private String buildDeliveryAddress(DocumentSnapshot doc) {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, doc.getString("deliveryDetailAddress"));
        appendAddressPart(builder, doc.getString("deliveryWard"));
        appendAddressPart(builder, doc.getString("deliveryDistrict"));
        appendAddressPart(builder, doc.getString("deliveryProvince"));
        appendAddressPart(builder, doc.getString("deliveryCountry"));
        return builder.toString();
    }

    private void appendAddressPart(StringBuilder builder, @Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    private List<LocalOrderItem> mapOrderItems(DocumentSnapshot doc) {
        List<LocalOrderItem> items = new ArrayList<>();
        Object rawItems = doc.get("items");
        if (!(rawItems instanceof List<?>)) {
            String orderId = valueOf(doc.getString("orderId"));
            String productName = valueOf(doc.getString("productName"));
            if (!productName.isEmpty()) {
                items.add(new LocalOrderItem(
                        orderId + "_item",
                        orderId,
                        valueOf(doc.getString("productId")),
                        productName,
                        valueOf(doc.getString("variantName")),
                        intValue(doc.getLong("qty")),
                        intValue(doc.getLong("unitPrice")),
                        valueOf(doc.getString("note")),
                        intValue(doc.getLong("total")),
                        valueOf(doc.getString("imageUrl"))
                ));
            }
            return items;
        }

        String orderId = valueOf(doc.getString("orderId"));
        int index = 0;
        for (Object rawItem : (List<?>) rawItems) {
            if (!(rawItem instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> itemMap = (Map<?, ?>) rawItem;
            int unitPrice = intValue(numberToLong(itemMap.get("unitPrice")));
            int qty = intValue(numberToLong(itemMap.get("qty")));
            int lineTotal = intValue(numberToLong(itemMap.get("lineTotal")));
            if (lineTotal <= 0) {
                lineTotal = unitPrice * qty;
            }
            items.add(new LocalOrderItem(
                    orderId + "_item_" + index++,
                    orderId,
                    stringValue(itemMap.get("productId")),
                    stringValue(itemMap.get("productName")),
                    stringValue(itemMap.get("variantName")),
                    qty,
                    unitPrice,
                    stringValue(itemMap.get("note")),
                    lineTotal,
                    stringValue(itemMap.get("imageUrl"))
            ));
        }
        return items;
    }

    private List<LocalOrderItem> mapSnapshotItems(@Nullable Object rawItems, @NonNull String orderId) {
        List<LocalOrderItem> items = new ArrayList<>();
        if (!(rawItems instanceof List<?>)) {
            return items;
        }
        int index = 0;
        for (Object rawItem : (List<?>) rawItems) {
            if (!(rawItem instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> itemMap = (Map<?, ?>) rawItem;
            int unitPrice = intValue(numberToLong(itemMap.get("unitPrice")));
            int qty = intValue(numberToLong(itemMap.get("qty")));
            int lineTotal = intValue(numberToLong(itemMap.get("lineTotal")));
            if (lineTotal <= 0) {
                lineTotal = unitPrice * qty;
            }
            items.add(new LocalOrderItem(
                    orderId + "_added_" + index++,
                    orderId,
                    stringValue(itemMap.get("productId")),
                    stringValue(itemMap.get("productName")),
                    stringValue(itemMap.get("variantName")),
                    qty,
                    unitPrice,
                    stringValue(itemMap.get("note")),
                    lineTotal,
                    stringValue(itemMap.get("imageUrl"))
            ));
        }
        return items;
    }

    private int intValue(@Nullable Long value) {
        return value == null ? 0 : value.intValue();
    }

    @Nullable
    private Long numberToLong(@Nullable Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        return null;
    }

    private long longValue(@Nullable Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        return System.currentTimeMillis();
    }

    private long longValueOrZero(@Nullable Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    @Nullable
    private String nullableTrim(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String valueOf(@Nullable String value) {
        return value == null ? "" : value;
    }

    private String stringValue(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizedOrderType(String rawOrderType) {
        if ("online_app".equals(rawOrderType) || "online".equals(rawOrderType)) {
            return "online";
        }
        if ("table_qr".equals(rawOrderType) || "dine_in".equals(rawOrderType)) {
            return "dine_in";
        }
        return rawOrderType;
    }

    private String normalizedOrderChannel(DocumentSnapshot doc, String rawOrderType) {
        String channel = valueOf(doc.getString("orderChannel"));
        if (!TextUtils.isEmpty(channel)) {
            return channel;
        }
        if ("online_app".equals(rawOrderType) || "online".equals(rawOrderType)) {
            return "customer_app";
        }
        if ("table_qr".equals(rawOrderType) || "dine_in".equals(rawOrderType)) {
            return "customer_qr";
        }
        return "staff_pos";
    }

    private com.google.android.gms.tasks.Task<Void> releaseTableIfNeeded(@Nullable DocumentSnapshot orderSnapshot,
                                                                         @NonNull String nextStatus) {
        if (orderSnapshot == null) {
            return Tasks.forResult(null);
        }
        boolean shouldRelease = "paid".equals(nextStatus) || "cancelled".equals(nextStatus) || "completed".equals(nextStatus);
        if (!shouldRelease) {
            return Tasks.forResult(null);
        }
        String tableId = orderSnapshot.getString("tableId");
        if (TextUtils.isEmpty(tableId) || TableCloudRepository.TAKEAWAY_TABLE_ID.equals(tableId)) {
            return Tasks.forResult(null);
        }
        Map<String, Object> tableValues = new HashMap<>();
        tableValues.put("status", "free");
        tableValues.put("updatedAt", System.currentTimeMillis());
        return firestore.collection("tables").document(tableId).update(tableValues);
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
