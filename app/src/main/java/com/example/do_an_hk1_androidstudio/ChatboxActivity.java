package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.config.ChatBackendConfig;
import com.example.do_an_hk1_androidstudio.local.model.LocalCategory;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatboxActivity extends AppCompatActivity {

    private static final String THINKING_TEXT = "\u0110ang suy ngh\u0129...";
    private static final int HISTORY_LIMIT = 10;

    private ScrollView scrollChatMessages;
    private LinearLayout layoutChatMessages;
    private EditText edtMessage;
    private TextView btnSend;
    private TextView thinkingBubble;
    private View latestSuggestionView;
    private CatalogCloudRepository catalogRepository;
    private ListenerRegistration productsListener;
    private ListenerRegistration categoriesListener;
    private final ExecutorService chatExecutor = Executors.newSingleThreadExecutor();
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final List<LocalProduct> menuProducts = new ArrayList<>();
    private final Map<String, String> categoryNameById = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);
        InsetsHelper.applyActivityRootPadding(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        scrollChatMessages = findViewById(R.id.scrollChatMessages);
        layoutChatMessages = findViewById(R.id.layoutChatMessages);
        edtMessage = findViewById(R.id.edtChatMessage);
        btnSend = findViewById(R.id.btnChatSend);
        catalogRepository = new CatalogCloudRepository(this);

        listenMenuData();

        appendBot("Xin ch\u00e0o! T\u00f4i l\u00e0 tr\u1ee3 l\u00fd CFPLUS. B\u1ea1n mu\u1ed1n h\u1ecfi g\u00ec v\u1ec1 qu\u00e1n?");

        btnSend.setOnClickListener(v -> {
            String msg = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(msg)) {
                return;
            }
            appendUser(msg);
            edtMessage.setText("");
            hideProductSuggestions();
            if (wantsHumanSupport(msg)) {
                appendBot("Mình chuyển bạn sang chat trực tiếp với nhân viên để hỗ trợ nhanh hơn.");
                android.content.Intent intent = new android.content.Intent(this, SupportChatActivity.class);
                intent.putExtra(SupportChatActivity.EXTRA_SEED_MESSAGE, msg);
                startActivity(intent);
                return;
            }
            if (ChatBackendConfig.isConfigured()) {
                askBackend(msg);
            } else {
                appendBot("Backend AI ch\u01b0a \u0111\u01b0\u1ee3c c\u1ea5u h\u00ecnh. M\u00ecnh \u0111ang tr\u1ea3 l\u1eddi t\u1ea1m b\u1eb1ng d\u1eef li\u1ec7u n\u1ed9i b\u1ed9: " + buildReply(msg));
                renderProductSuggestions(findRelevantProducts(msg, buildReply(msg)));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null) {
            productsListener.remove();
        }
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
        chatExecutor.shutdownNow();
    }

    private void appendUser(String msg) {
        addMessageBubble(msg, true);
        recordMessage("user", msg);
    }

    private void appendBot(String msg) {
        appendBot(msg, true);
    }

    private void appendBot(String msg, boolean shouldRecord) {
        TextView bubble = addMessageBubble(msg, false);
        if (THINKING_TEXT.equals(msg)) {
            thinkingBubble = bubble;
        }
        if (shouldRecord) {
            recordMessage("assistant", msg);
        }
    }

    private void askBackend(String msg) {
        setSendingState(true);
        appendBot(THINKING_TEXT, false);
        chatExecutor.execute(() -> {
            try {
                String reply = requestBackend(msg);
                runOnUiThread(() -> {
                    removeThinkingLine();
                    appendBot(reply);
                    renderProductSuggestions(findRelevantProducts(msg, reply));
                    setSendingState(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    removeThinkingLine();
                    appendBot("Ch\u01b0a k\u1ebft n\u1ed1i \u0111\u01b0\u1ee3c OpenRouter qua backend. Ki\u1ec3m tra server `cfplus-backend` \u0111ang ch\u1ea1y v\u00e0 key OpenRouter trong `.env`. T\u1ea1m th\u1eddi m\u00ecnh tr\u1ea3 l\u1eddi nhanh: " + buildReply(msg));
                    renderProductSuggestions(findRelevantProducts(msg, buildReply(msg)));
                    Toast.makeText(this, "Backend AI ch\u01b0a k\u1ebft n\u1ed1i. Ki\u1ec3m tra cfplus-backend.", Toast.LENGTH_SHORT).show();
                    setSendingState(false);
                });
            }
        });
    }

    private String requestBackend(String msg) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(ChatBackendConfig.CHAT_API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        JSONObject body = new JSONObject();
        body.put("message", msg);
        body.put("source", "android_customer_app");
        body.put("history", buildHistoryJson());
        body.put("menu_context", buildMenuContextJson());

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readStream(stream);
        connection.disconnect();

        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException(parseBackendError(response));
        }

        JSONObject json = new JSONObject(response);
        String reply = json.optString("reply", "");
        if (TextUtils.isEmpty(reply)) {
            return "M\u00ecnh ch\u01b0a c\u00f3 c\u00e2u tr\u1ea3 l\u1eddi r\u00f5 r\u00e0ng. B\u1ea1n h\u1ecfi l\u1ea1i ng\u1eafn h\u01a1n gi\u00fap m\u00ecnh nh\u00e9.";
        }
        return reply.trim();
    }

    private String parseBackendError(String response) {
        try {
            JSONObject json = new JSONObject(response);
            return json.optString("error", "Backend request failed");
        } catch (Exception ignored) {
            return "Backend request failed";
        }
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private JSONArray buildHistoryJson() throws Exception {
        JSONArray history = new JSONArray();
        int start = Math.max(0, chatHistory.size() - HISTORY_LIMIT);
        for (int i = start; i < chatHistory.size(); i++) {
            ChatMessage message = chatHistory.get(i);
            JSONObject json = new JSONObject();
            json.put("role", message.role);
            json.put("content", message.content);
            history.put(json);
        }
        return history;
    }

    private JSONArray buildMenuContextJson() throws Exception {
        JSONArray menu = new JSONArray();
        List<LocalProduct> products = new ArrayList<>(menuProducts);
        products.sort(Comparator.comparing(LocalProduct::getName, String.CASE_INSENSITIVE_ORDER));
        int count = 0;
        for (LocalProduct product : products) {
            if (!product.isActive() || TextUtils.isEmpty(product.getName())) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("id", product.getProductId());
            item.put("name", product.getName());
            item.put("price", product.getBasePrice());
            item.put("category", getCategoryName(product.getCategoryId()));
            menu.put(item);
            count++;
            if (count >= 30) {
                break;
            }
        }
        return menu;
    }

    private void listenMenuData() {
        categoriesListener = catalogRepository.listenCategories(categories -> {
            categoryNameById.clear();
            for (LocalCategory category : categories) {
                if (category.isActive()) {
                    categoryNameById.put(category.getCategoryId(), category.getName());
                }
            }
        });

        productsListener = catalogRepository.listenProducts(products -> {
            menuProducts.clear();
            for (LocalProduct product : products) {
                if (product.isActive()) {
                    menuProducts.add(product);
                }
            }
        });
    }

    private void renderProductSuggestions(List<LocalProduct> products) {
        hideProductSuggestions();
        if (products.isEmpty()) {
            return;
        }

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_card));
        card.setCardElevation(dp(4));
        card.setRadius(dp(18));
        card.setStrokeWidth(0);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(4), 0, dp(12));
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("Gợi ý từ menu");
        title.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        content.addView(title);

        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollParams.setMargins(0, dp(10), 0, 0);
        scrollView.setLayoutParams(scrollParams);

        LinearLayout suggestionRow = new LinearLayout(this);
        suggestionRow.setOrientation(LinearLayout.HORIZONTAL);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (LocalProduct product : products) {
            View itemView = inflater.inflate(R.layout.item_chat_product_suggestion, suggestionRow, false);
            ImageView image = itemView.findViewById(R.id.imgChatProduct);
            TextView name = itemView.findViewById(R.id.tvChatProductName);
            TextView price = itemView.findViewById(R.id.tvChatProductPrice);
            TextView add = itemView.findViewById(R.id.btnAddChatProduct);

            name.setText(product.getName());
            price.setText(MoneyFormatter.format(product.getBasePrice()));
            Glide.with(this)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.cfplus4)
                    .error(R.drawable.cfplus4)
                    .into(image);

            add.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, chitiet_sanpham.class);
                intent.putExtra("Ten", product.getName());
                intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
                intent.putExtra("hinhAnh", product.getImageUrl());
                intent.putExtra("productId", product.getProductId());
                startActivity(intent);
            });

            suggestionRow.addView(itemView);
        }

        scrollView.addView(suggestionRow);
        content.addView(scrollView);
        card.addView(content);
        latestSuggestionView = card;
        layoutChatMessages.addView(card);
        scrollToBottom();
    }

    private void hideProductSuggestions() {
        if (latestSuggestionView != null && layoutChatMessages != null) {
            layoutChatMessages.removeView(latestSuggestionView);
            latestSuggestionView = null;
        }
    }

    private List<LocalProduct> findRelevantProducts(String userMessage, String botReply) {
        List<ProductScore> scoredProducts = new ArrayList<>();
        String combinedText = normalizeSearchText(userMessage + " " + botReply);
        boolean asksForMenu = containsAny(combinedText, "mon", "menu", "ngon", "goi y", "uong", "cafe", "ca phe", "tra", "matcha", "latte", "capuchino", "capuccino");

        for (LocalProduct product : menuProducts) {
            if (!product.isActive()) {
                continue;
            }
            int score = scoreProduct(product, combinedText);
            if (score > 0 || asksForMenu) {
                scoredProducts.add(new ProductScore(product, score));
            }
        }

        scoredProducts.sort((first, second) -> {
            int compareScore = Integer.compare(second.score, first.score);
            if (compareScore != 0) {
                return compareScore;
            }
            int comparePrice = Integer.compare(first.product.getBasePrice(), second.product.getBasePrice());
            if (comparePrice != 0) {
                return comparePrice;
            }
            return first.product.getName().compareToIgnoreCase(second.product.getName());
        });

        List<LocalProduct> result = new ArrayList<>();
        for (ProductScore scoredProduct : scoredProducts) {
            if (result.size() >= 5) {
                break;
            }
            if (scoredProduct.score > 0 || asksForMenu) {
                result.add(scoredProduct.product);
            }
        }
        return result;
    }

    private int scoreProduct(LocalProduct product, String normalizedText) {
        String productName = normalizeSearchText(product.getName());
        String categoryName = normalizeSearchText(getCategoryName(product.getCategoryId()));
        int score = 0;
        if (!productName.isEmpty() && normalizedText.contains(productName)) {
            score += 100;
        }
        for (String token : productName.split("\\s+")) {
            if (token.length() >= 3 && normalizedText.contains(token)) {
                score += 10;
            }
        }
        if (!categoryName.isEmpty() && normalizedText.contains(categoryName)) {
            score += 18;
        }
        if (product.getBasePrice() <= 30000 && containsAny(normalizedText, "re", "gia tot", "tiet kiem")) {
            score += 8;
        }
        if (containsAny(productName, "sua", "latte", "capuchino", "capuccino") && containsAny(normalizedText, "ngot", "sua", "beo")) {
            score += 8;
        }
        if (containsAny(productName, "den", "espresso") && containsAny(normalizedText, "dam", "dang", "it ngot", "khong sua")) {
            score += 8;
        }
        if (containsAny(productName, "tra", "matcha") && containsAny(normalizedText, "nhe", "it cafein", "khong cafe", "tra", "matcha")) {
            score += 8;
        }
        return score;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean wantsHumanSupport(String rawMessage) {
        String normalized = normalizeSearchText(rawMessage);
        return containsAny(
                normalized,
                "nhan vien", "lien he nhan vien", "ho tro", "gap nhan vien", "chat nhan vien", "tu van"
        );
    }

    private String getCategoryName(String categoryId) {
        String categoryName = categoryNameById.get(categoryId);
        return TextUtils.isEmpty(categoryName) ? "Kh\u00e1c" : categoryName;
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.US)
                .replace('\u0111', 'd');
        return normalized.trim();
    }

    private void recordMessage(String role, String content) {
        if (TextUtils.isEmpty(content)) {
            return;
        }
        chatHistory.add(new ChatMessage(role, content.trim()));
        while (chatHistory.size() > HISTORY_LIMIT) {
            chatHistory.remove(0);
        }
    }

    private void setSendingState(boolean sending) {
        btnSend.setEnabled(!sending);
        btnSend.setAlpha(sending ? 0.65f : 1f);
    }

    private void removeThinkingLine() {
        if (thinkingBubble != null && layoutChatMessages != null) {
            layoutChatMessages.removeView(thinkingBubble);
            thinkingBubble = null;
        }
    }

    private TextView addMessageBubble(String message, boolean fromUser) {
        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        bubble.setLineSpacing(dp(2), 1f);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setMaxWidth(getResources().getDisplayMetrics().widthPixels - dp(96));
        bubble.setTextColor(ContextCompat.getColor(
                this,
                fromUser ? android.R.color.white : R.color.dashboard_text_primary
        ));

        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(ContextCompat.getColor(
                this,
                fromUser ? R.color.dashboard_primary : R.color.dashboard_card
        ));
        background.setCornerRadius(dp(18));
        bubble.setBackground(background);
        bubble.setElevation(dp(2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.setMargins(
                fromUser ? dp(44) : 0,
                dp(4),
                fromUser ? 0 : dp(44),
                dp(8)
        );
        bubble.setLayoutParams(params);

        layoutChatMessages.addView(bubble);
        scrollToBottom();
        return bubble;
    }

    private void scrollToBottom() {
        if (scrollChatMessages == null) {
            return;
        }
        scrollChatMessages.post(() -> scrollChatMessages.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private String buildReply(String msg) {
        String q = msg.toLowerCase(Locale.getDefault());
        if (q.contains("gi\u1edd") || q.contains("mo") || q.contains("m\u1edf")) {
            return "Qu\u00e1n m\u1edf c\u1eeda t\u1eeb 7:00 \u0111\u1ebfn 22:00 m\u1ed7i ng\u00e0y.";
        }
        if (q.contains("khuy\u1ebfn m\u00e3i") || q.contains("giam") || q.contains("gi\u1ea3m")) {
            return "B\u1ea1n c\u00f3 th\u1ec3 xem v\u00e0 \u00e1p d\u1ee5ng m\u00e3 gi\u1ea3m gi\u00e1 \u1edf b\u01b0\u1edbc thanh to\u00e1n n\u1ebfu \u0111\u01a1n h\u00e0ng \u0111\u1ee7 \u0111i\u1ec1u ki\u1ec7n.";
        }
        if (q.contains("best") || q.contains("ngon") || q.contains("g\u1ee3i \u00fd")) {
            return "G\u1ee3i \u00fd h\u00f4m nay: c\u00e0 ph\u00ea s\u1eefa \u0111\u00e1 size M \u00edt \u0111\u00e1, ho\u1eb7c m\u1ed9t ly matcha n\u1ebfu b\u1ea1n th\u00edch v\u1ecb nh\u1eb9 h\u01a1n.";
        }
        if (q.contains("\u0111\u1ecba ch\u1ec9") || q.contains("\u1edf \u0111\u00e2u")) {
            return "B\u1ea1n c\u00f3 th\u1ec3 m\u1edf m\u1ee5c Th\u00f4ng tin c\u1eeda h\u00e0ng \u0111\u1ec3 xem \u0111\u1ecba ch\u1ec9, b\u1ea3n \u0111\u1ed3 v\u00e0 ch\u1ec9 \u0111\u01b0\u1eddng.";
        }
        return "M\u00ecnh \u0111\u00e3 ghi nh\u1eadn. B\u1ea1n c\u00f3 th\u1ec3 h\u1ecfi v\u1ec1 gi\u1edd m\u1edf c\u1eeda, khuy\u1ebfn m\u00e3i, g\u1ee3i \u00fd m\u00f3n ho\u1eb7c \u0111\u1ecba ch\u1ec9 qu\u00e1n.";
    }

    private static final class ChatMessage {
        private final String role;
        private final String content;

        private ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static final class ProductScore {
        private final LocalProduct product;
        private final int score;

        private ProductScore(LocalProduct product, int score) {
            this.product = product;
            this.score = score;
        }
    }
}
