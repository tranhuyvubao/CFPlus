package com.example.do_an_hk1_androidstudio.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingSyncActionDao {

    @Query("SELECT * FROM pending_sync_actions ORDER BY created_at ASC")
    List<PendingSyncActionEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PendingSyncActionEntity entity);

    @Query("DELETE FROM pending_sync_actions WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE pending_sync_actions SET retry_count = retry_count + 1 WHERE id = :id")
    void increaseRetryCount(String id);
}
