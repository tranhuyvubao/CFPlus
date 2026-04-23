package com.example.do_an_hk1_androidstudio;

import android.app.Application;

import com.example.do_an_hk1_androidstudio.local.room.PendingSyncRepository;
import com.example.do_an_hk1_androidstudio.ui.NotificationCenter;
import com.google.android.material.color.DynamicColors;

public class CfPlusApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        NotificationCenter.ensureChannel(this);
        new PendingSyncRepository(this).flushPendingActions();
    }
}
