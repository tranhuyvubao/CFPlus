package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public final class FirebaseProvider {

    private static final String APP_NAME = "cfplus-manual";
    private static final String STORAGE_BUCKET = "cafeplus-1fd32.firebasestorage.app";

    public interface AuthCallback {
        void onResult(boolean success, String message);
    }

    private FirebaseProvider() {
    }

    public static FirebaseFirestore getFirestore(Context context) {
        FirebaseApp app = getOrInitApp(context.getApplicationContext());
        return FirebaseFirestore.getInstance(app);
    }

    public static FirebaseStorage getStorage(Context context) {
        FirebaseApp app = getOrInitApp(context.getApplicationContext());
        return FirebaseStorage.getInstance(app, "gs://" + STORAGE_BUCKET);
    }

    public static void ensureAuthenticated(Context context, AuthCallback callback) {
        FirebaseApp app = getOrInitApp(context.getApplicationContext());
        FirebaseAuth auth = FirebaseAuth.getInstance(app);
        if (auth.getCurrentUser() != null) {
            callback.onResult(true, null);
            return;
        }
        auth.signInAnonymously()
                .addOnSuccessListener(result -> callback.onResult(true, null))
                .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
    }

    private static FirebaseApp getOrInitApp(Context context) {
        for (FirebaseApp app : FirebaseApp.getApps(context)) {
            if (APP_NAME.equals(app.getName())) {
                return app;
            }
        }

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey("AIzaSyAkS5x8aqlgEi0EAfCJQxG2iQISA1IxRso")
                .setApplicationId("1:105381932857:android:4c76bed4adbd5ebb4002e8")
                .setProjectId("cafeplus-1fd32")
                .setStorageBucket(STORAGE_BUCKET)
                .setGcmSenderId("105381932857")
                .build();
        return FirebaseApp.initializeApp(context, options, APP_NAME);
    }

}
