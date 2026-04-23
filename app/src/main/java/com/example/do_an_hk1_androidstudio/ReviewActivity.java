package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.ReviewCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_ITEMS_PREVIEW = "items_preview";

    private final List<TextView> stars = new ArrayList<>();
    private int selectedRating = 5;
    private String orderId;
    private ReviewCloudRepository reviewCloudRepository;
    private LocalSessionManager sessionManager;
    private EditText edtComment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        InsetsHelper.applyActivityRootPadding(this);

        reviewCloudRepository = new ReviewCloudRepository(this);
        sessionManager = new LocalSessionManager(this);
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvReviewItems)).setText(getIntent().getStringExtra(EXTRA_ITEMS_PREVIEW));
        edtComment = findViewById(R.id.edtReviewComment);

        stars.add(findViewById(R.id.star1));
        stars.add(findViewById(R.id.star2));
        stars.add(findViewById(R.id.star3));
        stars.add(findViewById(R.id.star4));
        stars.add(findViewById(R.id.star5));

        for (int i = 0; i < stars.size(); i++) {
            final int rating = i + 1;
            stars.get(i).setOnClickListener(v -> {
                selectedRating = rating;
                bindStars();
            });
        }
        bindStars();

        findViewById(R.id.cardReviewImagePlaceholder).setOnClickListener(v ->
                Toast.makeText(this, "Khung upload ảnh review đã sẵn UI, phần upload storage sẽ nối tiếp ở bước sau.", Toast.LENGTH_LONG).show());

        findViewById(R.id.btnSubmitReview).setOnClickListener(v -> submitReview());
    }

    private void bindStars() {
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setAlpha(i < selectedRating ? 1f : 0.35f);
        }
    }

    private void submitReview() {
        String customerId = sessionManager.getCurrentUserId();
        if (TextUtils.isEmpty(customerId) || TextUtils.isEmpty(orderId)) {
            Toast.makeText(this, "Thiếu thông tin để gửi đánh giá.", Toast.LENGTH_SHORT).show();
            return;
        }

        String preview = getIntent().getStringExtra(EXTRA_ITEMS_PREVIEW);
        List<String> items = preview == null || preview.trim().isEmpty()
                ? new ArrayList<>()
                : Arrays.asList(preview.split("\\|"));

        reviewCloudRepository.submitOrderReview(
                orderId,
                customerId,
                selectedRating,
                edtComment.getText().toString().trim(),
                items,
                (success, message) -> runOnUiThread(() -> {
                    Toast.makeText(this,
                            success ? "Đã gửi đánh giá. Cảm ơn bạn!" : (message == null ? "Không thể gửi đánh giá." : message),
                            Toast.LENGTH_SHORT).show();
                    if (success) {
                        finish();
                    }
                })
        );
    }
}
