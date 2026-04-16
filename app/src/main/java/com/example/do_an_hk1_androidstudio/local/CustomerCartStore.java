package com.example.do_an_hk1_androidstudio.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.model.CustomerCartItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerCartStore {
    private static final String PREF_NAME = "customer_cart";
    private static final String KEY_ITEMS = "items";

    private final SharedPreferences preferences;

    public CustomerCartStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void addItem(@NonNull String productId,
                        @NonNull String productName,
                        int unitPrice,
                        int quantity,
                        @Nullable String size,
                        @Nullable String iceLevel,
                        @Nullable String note,
                        @Nullable String imageUrl) {
        List<CustomerCartItem> items = getItems();
        String normalizedSize = normalize(size);
        String normalizedIce = normalize(iceLevel);
        String normalizedNote = normalize(note);
        for (int i = 0; i < items.size(); i++) {
            CustomerCartItem item = items.get(i);
            if (productId.equals(item.getProductId())
                    && equalsNullable(normalizedSize, item.getSize())
                    && equalsNullable(normalizedIce, item.getIceLevel())
                    && equalsNullable(normalizedNote, item.getNote())) {
                items.set(i, new CustomerCartItem(
                        item.getCartItemId(),
                        item.getProductId(),
                        item.getProductName(),
                        unitPrice,
                        item.getQuantity() + quantity,
                        normalizedSize,
                        normalizedIce,
                        normalizedNote,
                        normalize(imageUrl)
                ));
                save(items);
                return;
            }
        }

        items.add(new CustomerCartItem(
                UUID.randomUUID().toString(),
                productId,
                productName,
                unitPrice,
                quantity,
                normalizedSize,
                normalizedIce,
                normalizedNote,
                normalize(imageUrl)
        ));
        save(items);
    }

    @NonNull
    public List<CustomerCartItem> getItems() {
        List<CustomerCartItem> items = new ArrayList<>();
        String raw = preferences.getString(KEY_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                items.add(new CustomerCartItem(
                        object.optString("cartItemId"),
                        object.optString("productId"),
                        object.optString("productName"),
                        object.optInt("unitPrice", 0),
                        object.optInt("quantity", 1),
                        normalize(object.optString("size", null)),
                        normalize(object.optString("iceLevel", null)),
                        normalize(object.optString("note", null)),
                        normalize(object.optString("imageUrl", null))
                ));
            }
        } catch (JSONException ignored) {
        }
        return items;
    }

    public int getItemCount() {
        int count = 0;
        for (CustomerCartItem item : getItems()) {
            count += item.getQuantity();
        }
        return count;
    }

    public int getSubtotal() {
        int total = 0;
        for (CustomerCartItem item : getItems()) {
            total += item.getLineTotal();
        }
        return total;
    }

    public void updateQuantity(@NonNull String cartItemId, int newQuantity) {
        List<CustomerCartItem> items = getItems();
        for (int i = 0; i < items.size(); i++) {
            CustomerCartItem item = items.get(i);
            if (cartItemId.equals(item.getCartItemId())) {
                if (newQuantity <= 0) {
                    items.remove(i);
                } else {
                    items.set(i, new CustomerCartItem(
                            item.getCartItemId(),
                            item.getProductId(),
                            item.getProductName(),
                            item.getUnitPrice(),
                            newQuantity,
                            item.getSize(),
                            item.getIceLevel(),
                            item.getNote(),
                            item.getImageUrl()
                    ));
                }
                save(items);
                return;
            }
        }
    }

    public void removeItem(@NonNull String cartItemId) {
        List<CustomerCartItem> items = getItems();
        for (int i = 0; i < items.size(); i++) {
            if (cartItemId.equals(items.get(i).getCartItemId())) {
                items.remove(i);
                save(items);
                return;
            }
        }
    }

    public void clear() {
        preferences.edit().remove(KEY_ITEMS).apply();
    }

    private void save(List<CustomerCartItem> items) {
        JSONArray array = new JSONArray();
        for (CustomerCartItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("cartItemId", item.getCartItemId());
                object.put("productId", item.getProductId());
                object.put("productName", item.getProductName());
                object.put("unitPrice", item.getUnitPrice());
                object.put("quantity", item.getQuantity());
                object.put("size", item.getSize());
                object.put("iceLevel", item.getIceLevel());
                object.put("note", item.getNote());
                object.put("imageUrl", item.getImageUrl());
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    @Nullable
    private String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return TextUtils.isEmpty(trimmed) ? null : trimmed;
    }

    private boolean equalsNullable(@Nullable String a, @Nullable String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
