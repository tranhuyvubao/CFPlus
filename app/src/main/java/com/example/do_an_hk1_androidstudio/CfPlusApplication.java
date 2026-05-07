package com.example.do_an_hk1_androidstudio;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.FirebaseProvider;
import com.example.do_an_hk1_androidstudio.cloud.StaffPushTokenRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.room.PendingSyncRepository;
import com.example.do_an_hk1_androidstudio.ui.NotificationCenter;
import com.example.do_an_hk1_androidstudio.ui.ShiftAttendanceStateStore;
import com.example.do_an_hk1_androidstudio.ui.StaffNotificationSyncManager;
import com.google.android.material.color.DynamicColors;

import java.util.Map;
import java.util.WeakHashMap;

public class CfPlusApplication extends Application {
    private static final long PAGE_LOADING_DURATION_MS = 850L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Activity, LoadingDialogHelper> loadingHelpers = new WeakHashMap<>();
    private final Map<Activity, Runnable> hideTasks = new WeakHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseProvider.ensureInitialized(this);
        DynamicColors.applyToActivitiesIfAvailable(this);
        NotificationCenter.ensureChannel(this);
        new PendingSyncRepository(this).flushPendingActions();
        if ("staff".equals(new LocalSessionManager(this).getCurrentUserRole())
                && ShiftAttendanceStateStore.isCheckedIn(this)) {
            StaffNotificationSyncManager.getInstance(this).startIfNeeded();
            new StaffPushTokenRepository(this).syncForCurrentSession();
        }
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    private final ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
            if (!(activity instanceof AppCompatActivity) || activity instanceof SplashActivity) {
                return;
            }
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            LoadingDialogHelper helper = loadingHelpers.get(activity);
            if (helper == null) {
                helper = new LoadingDialogHelper(appCompatActivity);
                loadingHelpers.put(activity, helper);
            }
            helper.show();
            scheduleHide(activity, helper);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            // no-op
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            // no-op
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            // no-op
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            Runnable oldTask = hideTasks.remove(activity);
            if (oldTask != null) {
                mainHandler.removeCallbacks(oldTask);
            }
            LoadingDialogHelper helper = loadingHelpers.get(activity);
            if (helper != null) {
                helper.hide();
            }
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            // no-op
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Runnable oldTask = hideTasks.remove(activity);
            if (oldTask != null) {
                mainHandler.removeCallbacks(oldTask);
            }
            LoadingDialogHelper helper = loadingHelpers.remove(activity);
            if (helper != null) {
                helper.hide();
            }
        }
    };

    private void scheduleHide(@NonNull Activity activity, @NonNull LoadingDialogHelper helper) {
        Runnable oldTask = hideTasks.remove(activity);
        if (oldTask != null) {
            mainHandler.removeCallbacks(oldTask);
        }

        Runnable hideTask = helper::hide;
        hideTasks.put(activity, hideTask);
        mainHandler.postDelayed(hideTask, PAGE_LOADING_DURATION_MS);
    }
}
