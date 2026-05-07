package com.example.do_an_hk1_androidstudio.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.do_an_hk1_androidstudio.cloud.StaffNotificationCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

public final class StaffNotificationSyncManager {
    private static StaffNotificationSyncManager instance;

    private final Context appContext;
    private final StaffNotificationCloudRepository repository;
    private final Set<String> seenNotificationIds = new HashSet<>();
    private ListenerRegistration registration;
    private boolean initialSyncDone;

    private StaffNotificationSyncManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        repository = new StaffNotificationCloudRepository(appContext);
    }

    @NonNull
    public static synchronized StaffNotificationSyncManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new StaffNotificationSyncManager(context);
        }
        return instance;
    }

    public void startIfNeeded() {
        LocalSessionManager sessionManager = new LocalSessionManager(appContext);
        if (!"staff".equals(sessionManager.getCurrentUserRole())) {
            stop();
            return;
        }
        if (registration != null) {
            return;
        }
        final String userId = sessionManager.getCurrentUserId();
        registration = repository.listenStaffNotifications(notifications -> {
            for (StaffNotificationCloudRepository.StaffNotificationRecord item : notifications) {
                boolean alreadySeenInSession = seenNotificationIds.contains(item.id);
                boolean shouldShowSystemNotification = !alreadySeenInSession
                        && (initialSyncDone || item.pushDispatchedAt <= 0L);
                seenNotificationIds.add(item.id);
                NotificationCenter.storeAndShow(
                        appContext,
                        item.id,
                        item.eventKey,
                        userId,
                        item.title,
                        item.body,
                        item.type,
                        item.orderId,
                        item.status,
                        shouldShowSystemNotification
                );
                if (shouldShowSystemNotification) {
                    repository.markPushDispatched(item.id, null);
                }
            }
            initialSyncDone = true;
        });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        seenNotificationIds.clear();
        initialSyncDone = false;
    }
}
