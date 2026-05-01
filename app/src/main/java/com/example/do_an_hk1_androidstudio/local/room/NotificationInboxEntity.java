package com.example.do_an_hk1_androidstudio.local.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notification_inbox")
public class NotificationInboxEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "body")
    public String body;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "order_id")
    public String orderId;

    @ColumnInfo(name = "event_key")
    public String eventKey;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "read")
    public boolean read;
}
