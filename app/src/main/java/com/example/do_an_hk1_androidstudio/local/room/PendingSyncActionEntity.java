package com.example.do_an_hk1_androidstudio.local.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_sync_actions")
public class PendingSyncActionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "action_type")
    public String actionType;

    @ColumnInfo(name = "payload_json")
    public String payloadJson;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "retry_count")
    public int retryCount;
}
