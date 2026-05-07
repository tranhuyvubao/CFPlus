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

    @Query("SELECT * FROM notification_inbox WHERE user_id = :userId ORDER BY created_at DESC")
    List<NotificationInboxEntity> getAllForUser(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NotificationInboxEntity entity);

    @Query("SELECT COUNT(*) > 0 FROM notification_inbox WHERE id = :id")
    boolean exists(String id);

    @Query("UPDATE notification_inbox SET read = 1 WHERE id = :id")
    void markRead(String id);

    @Query("UPDATE notification_inbox SET read = 1")
    void markAllRead();

    @Query("UPDATE notification_inbox SET read = 1 WHERE user_id = :userId")
    void markAllReadForUser(String userId);

    @Query("SELECT COUNT(*) FROM notification_inbox WHERE read = 0")
    int countUnread();

    @Query("SELECT COUNT(*) FROM notification_inbox WHERE user_id = :userId AND read = 0")
    int countUnreadForUser(String userId);
}
