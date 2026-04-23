package com.example.do_an_hk1_androidstudio.local.room;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.do_an_hk1_androidstudio.cloud.FirebaseProvider;
import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingSyncRepository {

    public static final String ACTION_UPDATE_STATUS = "update_status";
    public static final String ACTION_PAY_ORDER = "pay_order";

    private final FirebaseFirestore firestore;
    private final PendingSyncActionDao pendingSyncActionDao;

    public PendingSyncRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
        pendingSyncActionDao = CfPlusLocalDatabase.getInstance(appContext).pendingSyncActionDao();
    }

    public void enqueue(@NonNull String actionType, @NonNull String payloadJson) {
        PendingSyncActionEntity entity = new PendingSyncActionEntity();
        entity.id = DataHelper.newId("sync");
        entity.actionType = actionType;
        entity.payloadJson = payloadJson;
        entity.createdAt = System.currentTimeMillis();
        entity.retryCount = 0;
        pendingSyncActionDao.insert(entity);
    }

    public void flushPendingActions() {
        List<PendingSyncActionEntity> actions = pendingSyncActionDao.getAll();
        for (PendingSyncActionEntity action : actions) {
            try {
                if (ACTION_UPDATE_STATUS.equals(action.actionType)) {
                    flushUpdateStatus(action);
                } else if (ACTION_PAY_ORDER.equals(action.actionType)) {
                    flushPayOrder(action);
                } else {
                    pendingSyncActionDao.deleteById(action.id);
                }
            } catch (JSONException e) {
                pendingSyncActionDao.deleteById(action.id);
            }
        }
    }

    private void flushUpdateStatus(@NonNull PendingSyncActionEntity action) throws JSONException {
        JSONObject payload = new JSONObject(action.payloadJson);
        String orderId = payload.optString("orderId");
        String nextStatus = payload.optString("nextStatus");
        String staffId = payload.optString("staffId");
        if (TextUtils.isEmpty(orderId) || TextUtils.isEmpty(nextStatus)) {
            pendingSyncActionDao.deleteById(action.id);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", nextStatus);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        if (!TextUtils.isEmpty(staffId)) {
            updates.put("staffId", staffId);
        }
        firestore.collection("orders")
                .document(orderId)
                .update(updates)
                .addOnSuccessListener(unused -> pendingSyncActionDao.deleteById(action.id))
                .addOnFailureListener(e -> pendingSyncActionDao.increaseRetryCount(action.id));
    }

    private void flushPayOrder(@NonNull PendingSyncActionEntity action) throws JSONException {
        JSONObject payload = new JSONObject(action.payloadJson);
        String orderId = payload.optString("orderId");
        String method = payload.optString("method");
        String bankRef = payload.optString("bankRef");
        String promotionCode = payload.optString("promotionCode");
        int amount = payload.optInt("amount");
        int discountAmount = payload.optInt("discountAmount");
        if (TextUtils.isEmpty(orderId)) {
            pendingSyncActionDao.deleteById(action.id);
            return;
        }

        Map<String, Object> payment = new HashMap<>();
        payment.put("orderId", orderId);
        payment.put("method", method);
        payment.put("bankRef", TextUtils.isEmpty(bankRef) ? null : bankRef);
        payment.put("amount", amount);
        payment.put("discountAmount", discountAmount);
        payment.put("promotionCode", TextUtils.isEmpty(promotionCode) ? null : promotionCode);
        payment.put("status", "success");
        payment.put("createdAt", System.currentTimeMillis());

        Map<String, Object> orderUpdate = new HashMap<>();
        orderUpdate.put("status", "paid");
        orderUpdate.put("total", amount);
        orderUpdate.put("discountAmount", discountAmount);
        orderUpdate.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("payments")
                .document(orderId)
                .set(payment)
                .continueWithTask(task -> firestore.collection("orders").document(orderId).update(orderUpdate))
                .addOnSuccessListener(unused -> pendingSyncActionDao.deleteById(action.id))
                .addOnFailureListener(e -> pendingSyncActionDao.increaseRetryCount(action.id));
    }

    public boolean isLikelyNetworkIssue(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("unavailable")
                || normalized.contains("unable to resolve host")
                || normalized.contains("failed to resolve")
                || normalized.contains("network")
                || normalized.contains("offline");
    }
}
