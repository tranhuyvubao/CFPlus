package com.example.do_an_hk1_androidstudio.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashSet;
import java.util.Set;

public class WishlistStore {
    private static final String PREF_NAME = "customer_wishlist";
    private static final String KEY_PRODUCT_IDS = "favorite_product_ids";

    private final SharedPreferences preferences;

    public WishlistStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFavorite(@NonNull String productId) {
        return getFavoriteIds().contains(productId);
    }

    public void toggle(@NonNull String productId) {
        Set<String> ids = getFavoriteIds();
        if (ids.contains(productId)) {
            ids.remove(productId);
        } else {
            ids.add(productId);
        }
        save(ids);
    }

    public void remove(@NonNull String productId) {
        Set<String> ids = getFavoriteIds();
        ids.remove(productId);
        save(ids);
    }

    public void setFavorite(@NonNull String productId, boolean favorite) {
        Set<String> ids = getFavoriteIds();
        if (favorite) {
            ids.add(productId);
        } else {
            ids.remove(productId);
        }
        save(ids);
    }

    public void replaceAll(@NonNull Set<String> ids) {
        save(ids);
    }

    @NonNull
    public Set<String> getFavoriteIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        String raw = preferences.getString(KEY_PRODUCT_IDS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.trim().isEmpty()) {
                    ids.add(value.trim());
                }
            }
        } catch (JSONException ignored) {
        }
        return ids;
    }

    private void save(Set<String> ids) {
        JSONArray array = new JSONArray();
        for (String id : ids) {
            array.put(id);
        }
        preferences.edit().putString(KEY_PRODUCT_IDS, array.toString()).apply();
    }
}
