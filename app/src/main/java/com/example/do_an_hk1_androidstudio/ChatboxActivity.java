package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

import java.util.Locale;

public class ChatboxActivity extends AppCompatActivity {

    private TextView tvChatLog;
    private EditText edtMessage;
    private TextView btnSend;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);
        InsetsHelper.applyActivityRootPadding(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        tvChatLog = findViewById(R.id.tvChatLog);
        edtMessage = findViewById(R.id.edtChatMessage);
        btnSend = findViewById(R.id.btnChatSend);

        appendBot("Xin chào! Tôi là Chatbox AI (demo). Bạn muốn hỏi gì về quán?");

        btnSend.setOnClickListener(v -> {
            String msg = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(msg)) return;
            appendUser(msg);
            edtMessage.setText("");
            appendBot(fakeAiReply(msg));
        });
    }

    private void appendUser(String msg) {
        tvChatLog.append("\n\nBạn: " + msg);
    }

    private void appendBot(String msg) {
        tvChatLog.append("\n\nAI: " + msg);
    }

    private String fakeAiReply(String msg) {
        String q = msg.toLowerCase(Locale.getDefault());
        if (q.contains("giờ") || q.contains("mo") || q.contains("mở")) {
            return "Quán mở cửa từ 7:00 đến 22:00 (demo).";
        }
        if (q.contains("khuyến mãi") || q.contains("giam")) {
            return "Bạn có thể xem mã giảm giá ở mục Khuyến mãi (demo).";
        }
        if (q.contains("best") || q.contains("ngon") || q.contains("gợi ý")) {
            return "Gợi ý: Matcha Latte size M, ít đá. Hoặc Cafe sữa đá (demo).";
        }
        if (q.contains("địa chỉ") || q.contains("ở đâu")) {
            return "Địa chỉ quán: (bạn điền theo thông tin cửa hàng của bạn) (demo).";
        }
        return "Mình đã ghi nhận. Bạn có thể hỏi về giờ mở cửa, khuyến mãi, gợi ý món, địa chỉ (demo).";
    }
}

