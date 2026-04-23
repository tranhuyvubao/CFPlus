package com.example.do_an_hk1_androidstudio.local.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                NotificationInboxEntity.class,
                PendingSyncActionEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class CfPlusLocalDatabase extends RoomDatabase {

    private static volatile CfPlusLocalDatabase instance;

    public abstract NotificationInboxDao notificationInboxDao();

    public abstract PendingSyncActionDao pendingSyncActionDao();

    @NonNull
    public static CfPlusLocalDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (CfPlusLocalDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    CfPlusLocalDatabase.class,
                                    "cfplus_local.db"
                            )
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
