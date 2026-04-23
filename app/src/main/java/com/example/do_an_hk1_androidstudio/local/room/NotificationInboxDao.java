package com.example.do_an_hk1_androidstudio.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationInboxDao {

    @Query("SELECT * FROM notification_inbox ORDER BY created_at DESC")
    List<NotificationInboxEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NotificationInboxEntity entity);

    @Query("UPDATE notification_inbox SET read = 1 WHERE id = :id")
    void markRead(String id);

    @Query("SELECT COUNT(*) FROM notification_inbox WHERE read = 0")
    int countUnread();
}
