package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.FirebaseProvider;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.WishlistCloudRepository;
import com.example.do_an_hk1_androidstudio.local.CustomerCartStore;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalCategory;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.example.do_an_hk1_androidstudio.ui.UiMotion;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class tabOne_TatCa extends Fragment {

    private static final String CATEGORY_ALL = "all";
    private static final String QUICK_NONE = "none";
    private static final String QUICK_POPULAR = "popular";
    private static final String QUICK_PROMO = "promo";
    private static final String QUICK_VALUE = "value";
    private static final String SORT_NAME = "name";
    private static final String SORT_PRICE_ASC = "price_asc";
    private static final String SORT_PRICE_DESC = "price_desc";

    private final List<LocalCategory> categories = new ArrayList<>();
    private final List<LocalProduct> allProducts = new ArrayList<>();
    private final List<LocalOrder> customerOrders = new ArrayList<>();
    private final Map<String, Float> ratingAverageByProductName = new HashMap<>();

    private CatalogCloudRepository catalogRepository;
    private OrderCloudRepository orderRepository;
    private WishlistCloudRepository wishlistCloudRepository;
    private CustomerCartStore cartStore;
    private LocalSessionManager sessionManager;
    private ListenerRegistration categoriesListener;
    private ListenerRegistration productsListener;
    private ListenerRegistration ordersListener;
    private ListenerRegistration reviewsListener;
    private ListenerRegistration favoritesListener;
    private final Set<String> favoriteIds = new LinkedHashSet<>();

    private LinearLayout linearCategoryFilters;
    private LinearLayout linearQuickFilters;
    private View layoutFeaturedSection;
    private LinearLayout linearFeaturedProducts;
    private LinearLayout linearDynamicSections;
    private TextView tvResultCount;
    private TextView tvEmptyState;

    private String selectedCategoryId = CATEGORY_ALL;
    private String selectedQuickFilter = QUICK_NONE;
    private String selectedSortMode = SORT_NAME;
    private int selectedMaxPrice = 0;
    private boolean filterMinFourStars = false;
    private boolean filterPromotionOnly = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_one__tatca, container, false);
        catalogRepository = new CatalogCloudRepository(requireContext());
        orderRepository = new OrderCloudRepository(requireContext());
        wishlistCloudRepository = new WishlistCloudRepository(requireContext());
        cartStore = new CustomerCartStore(requireContext());
        sessionManager = new LocalSessionManager(requireContext());

        linearCategoryFilters = view.findViewById(R.id.linearCategoryFilters);
        linearQuickFilters = view.findViewById(R.id.linearQuickFilters);
        layoutFeaturedSection = view.findViewById(R.id.layoutFeaturedSection);
        linearFeaturedProducts = view.findViewById(R.id.linearFeaturedProducts);
        linearDynamicSections = view.findViewById(R.id.linearDynamicSections);
        tvResultCount = view.findViewById(R.id.tvResultCount);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        listenCategories();
        listenProducts();
        listenOrders();
        listenReviews();
        listenFavorites();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
        if (productsListener != null) {
            productsListener.remove();
        }
        if (ordersListener != null) {
            ordersListener.remove();
        }
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
        if (favoritesListener != null) {
            favoritesListener.remove();
        }
    }

    private void listenFavorites() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            favoriteIds.clear();
            if (isAdded()) {
                renderContent();
            }
            return;
        }
        favoritesListener = wishlistCloudRepository.listenFavoriteIds(userId, ids -> {
            favoriteIds.clear();
            favoriteIds.addAll(ids);
            if (isAdded()) {
                renderContent();
            }
        });
    }

    private void listenCategories() {
        categoriesListener = catalogRepository.listenCategories(fetchedCategories -> {
            categories.clear();
            for (LocalCategory category : fetchedCategories) {
                if (category.isActive()) {
                    categories.add(category);
                }
            }
            categories.sort(Comparator.comparing(LocalCategory::getName, String.CASE_INSENSITIVE_ORDER));
            if (isAdded()) {
                renderContent();
            }
        });
    }

    private void listenProducts() {
        productsListener = catalogRepository.listenProducts(products -> {
            allProducts.clear();
            for (LocalProduct product : products) {
                if (product.isActive()) {
                    allProducts.add(product);
                }
            }
            if (isAdded()) {
                renderContent();
            }
        });
    }

    private void listenOrders() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            return;
        }
        ordersListener = orderRepository.listenOrdersByCustomer(userId, orders -> {
            customerOrders.clear();
            customerOrders.addAll(orders);
            if (isAdded()) {
                renderContent();
            }
        });
    }

    private void listenReviews() {
        FirebaseProvider.ensureAuthenticated(requireContext(), (success, message) -> {
            if (!success) {
                return;
            }
            reviewsListener = FirebaseProvider.getFirestore(requireContext())
                    .collection("reviews")
                    .addSnapshotListener((value, error) -> {
                        ratingAverageByProductName.clear();
                        if (value != null) {
                            Map<String, Integer> countMap = new HashMap<>();
                            Map<String, Integer> totalMap = new HashMap<>();
                            value.getDocuments().forEach(document -> {
                                long ratingLong = 0L;
                                Object ratingValue = document.get("rating");
                                if (ratingValue instanceof Long) {
                                    ratingLong = (Long) ratingValue;
                                } else if (ratingValue instanceof Integer) {
                                    ratingLong = (Integer) ratingValue;
                                }
                                Object productsValue = document.get("product_names");
                                if (productsValue instanceof List<?>) {
                                    for (Object productName : (List<?>) productsValue) {
                                        String normalized = normalizeProductKey(String.valueOf(productName));
                                        if (normalized.isEmpty()) {
                                            continue;
                                        }
                                        totalMap.put(normalized, totalMap.getOrDefault(normalized, 0) + (int) ratingLong);
                                        countMap.put(normalized, countMap.getOrDefault(normalized, 0) + 1);
                                    }
                                }
                            });
                            for (Map.Entry<String, Integer> entry : totalMap.entrySet()) {
                                String key = entry.getKey();
                                int total = entry.getValue();
                                int count = countMap.getOrDefault(key, 1);
                                ratingAverageByProductName.put(key, total / (float) count);
                            }
                        }
                        if (isAdded()) {
                            renderContent();
                        }
                    });
        });
    }

    private void renderContent() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        renderCategoryFilters();
        renderQuickFilters();
        renderFeaturedSection();
        renderSections();
    }

    private void renderCategoryFilters() {
        linearCategoryFilters.removeAllViews();
        linearCategoryFilters.addView(createFilterChip("Tất cả", CATEGORY_ALL.equals(selectedCategoryId), () -> {
            selectedCategoryId = CATEGORY_ALL;
            renderContent();
        }));

        for (LocalCategory category : categories) {
            linearCategoryFilters.addView(createFilterChip(category.getName(),
                    category.getCategoryId().equals(selectedCategoryId),
                    () -> {
                        selectedCategoryId = category.getCategoryId();
                        renderContent();
                    }));
        }
    }

    private void renderQuickFilters() {
        linearQuickFilters.removeAllViews();
        linearQuickFilters.addView(createQuickChip("Phổ biến", QUICK_POPULAR.equals(selectedQuickFilter), () -> toggleQuickFilter(QUICK_POPULAR)));
        linearQuickFilters.addView(createQuickChip("Ưu đãi", QUICK_PROMO.equals(selectedQuickFilter), () -> toggleQuickFilter(QUICK_PROMO)));
        linearQuickFilters.addView(createQuickChip("Giá tốt", QUICK_VALUE.equals(selectedQuickFilter), () -> toggleQuickFilter(QUICK_VALUE)));
        linearQuickFilters.addView(createQuickChip("Bộ lọc", hasAdvancedFilters(), this::showFilterSheet));
    }

    private void toggleQuickFilter(String filter) {
        selectedQuickFilter = filter.equals(selectedQuickFilter) ? QUICK_NONE : filter;
        renderContent();
    }

    private void renderFeaturedSection() {
        linearFeaturedProducts.removeAllViews();
        boolean shouldShow = CATEGORY_ALL.equals(selectedCategoryId)
                && QUICK_NONE.equals(selectedQuickFilter)
                && !hasAdvancedFilters();
        if (!shouldShow) {
            layoutFeaturedSection.setVisibility(View.GONE);
            return;
        }

        List<LocalProduct> products = pickPersonalizedProducts();
        if (products.isEmpty()) {
            layoutFeaturedSection.setVisibility(View.GONE);
            return;
        }

        layoutFeaturedSection.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (LocalProduct product : products) {
            linearFeaturedProducts.addView(createProductCard(inflater, product));
        }
    }

    private void renderSections() {
        linearDynamicSections.removeAllViews();

        int totalProducts = 0;
        for (LocalCategory category : getVisibleCategories()) {
            List<LocalProduct> categoryProducts = getCategoryProducts(category.getCategoryId());
            totalProducts += categoryProducts.size();
            if (categoryProducts.isEmpty()) {
                continue;
            }

            TextView heading = new TextView(requireContext());
            heading.setText(category.getName());
            heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            heading.setTypeface(Typeface.DEFAULT_BOLD);
            heading.setTextColor(ContextCompat.getColor(requireContext(), R.color.dashboard_text_primary));
            heading.setPadding(dp(4), dp(18), dp(4), dp(10));
            linearDynamicSections.addView(heading);

            HorizontalScrollView scrollView = new HorizontalScrollView(requireContext());
            scrollView.setHorizontalScrollBarEnabled(false);
            scrollView.setClipToPadding(false);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (LocalProduct product : categoryProducts) {
                row.addView(createProductCard(inflater, product));
            }

            scrollView.addView(row);
            linearDynamicSections.addView(scrollView);
        }

        tvResultCount.setText(totalProducts + " món phù hợp");
        tvEmptyState.setVisibility(totalProducts == 0 ? View.VISIBLE : View.GONE);
    }

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_catalog_filters, null, false);
        RadioGroup priceGroup = view.findViewById(R.id.radioPriceGroup);
        RadioGroup sortGroup = view.findViewById(R.id.radioSortGroup);
        CheckBox checkRating = view.findViewById(R.id.checkRating);
        CheckBox checkPromotion = view.findViewById(R.id.checkPromotion);
        Button btnReset = view.findViewById(R.id.btnResetFilters);
        Button btnApply = view.findViewById(R.id.btnApplyFilters);

        if (selectedMaxPrice == 30000) {
            priceGroup.check(R.id.radioPrice30);
        } else if (selectedMaxPrice == 50000) {
            priceGroup.check(R.id.radioPrice50);
        } else {
            priceGroup.check(R.id.radioPriceAll);
        }

        if (SORT_PRICE_ASC.equals(selectedSortMode)) {
            sortGroup.check(R.id.radioSortPriceAsc);
        } else if (SORT_PRICE_DESC.equals(selectedSortMode)) {
            sortGroup.check(R.id.radioSortPriceDesc);
        } else {
            sortGroup.check(R.id.radioSortName);
        }

        checkRating.setChecked(filterMinFourStars);
        checkPromotion.setChecked(filterPromotionOnly);

        btnReset.setOnClickListener(v -> {
            selectedSortMode = SORT_NAME;
            selectedMaxPrice = 0;
            filterMinFourStars = false;
            filterPromotionOnly = false;
            dialog.dismiss();
            renderContent();
        });
        btnApply.setOnClickListener(v -> {
            int checkedPrice = priceGroup.getCheckedRadioButtonId();
            if (checkedPrice == R.id.radioPrice30) {
                selectedMaxPrice = 30000;
            } else if (checkedPrice == R.id.radioPrice50) {
                selectedMaxPrice = 50000;
            } else {
                selectedMaxPrice = 0;
            }

            int checkedSort = sortGroup.getCheckedRadioButtonId();
            if (checkedSort == R.id.radioSortPriceAsc) {
                selectedSortMode = SORT_PRICE_ASC;
            } else if (checkedSort == R.id.radioSortPriceDesc) {
                selectedSortMode = SORT_PRICE_DESC;
            } else {
                selectedSortMode = SORT_NAME;
            }

            filterMinFourStars = checkRating.isChecked();
            filterPromotionOnly = checkPromotion.isChecked();
            dialog.dismiss();
            renderContent();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private List<LocalCategory> getVisibleCategories() {
        List<LocalCategory> visibleCategories = new ArrayList<>();
        for (LocalCategory category : categories) {
            if (CATEGORY_ALL.equals(selectedCategoryId) || selectedCategoryId.equals(category.getCategoryId())) {
                visibleCategories.add(category);
            }
        }
        return visibleCategories;
    }

    private List<LocalProduct> getCategoryProducts(String categoryId) {
        List<LocalProduct> products = new ArrayList<>();
        for (LocalProduct product : allProducts) {
            if (categoryId.equals(product.getCategoryId()) && matchesFilters(product)) {
                products.add(product);
            }
        }

        switch (selectedSortMode) {
            case SORT_PRICE_ASC:
                products.sort(Comparator.comparingInt(LocalProduct::getBasePrice)
                        .thenComparing(LocalProduct::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case SORT_PRICE_DESC:
                products.sort((first, second) -> {
                    int comparePrice = Integer.compare(second.getBasePrice(), first.getBasePrice());
                    if (comparePrice != 0) {
                        return comparePrice;
                    }
                    return first.getName().compareToIgnoreCase(second.getName());
                });
                break;
            case SORT_NAME:
            default:
                products.sort(Comparator.comparing(LocalProduct::getName, String.CASE_INSENSITIVE_ORDER));
                break;
        }
        return products;
    }

    private boolean matchesFilters(@NonNull LocalProduct product) {
        if (selectedMaxPrice > 0 && product.getBasePrice() > selectedMaxPrice) {
            return false;
        }
        if (filterMinFourStars && getProductRating(product) < 4f) {
            return false;
        }
        if (filterPromotionOnly && !isPromotionProduct(product)) {
            return false;
        }
        if (QUICK_PROMO.equals(selectedQuickFilter)) {
            return isPromotionProduct(product);
        }
        if (QUICK_VALUE.equals(selectedQuickFilter)) {
            return product.getBasePrice() <= 30000 || isPromotionProduct(product);
        }
        if (QUICK_POPULAR.equals(selectedQuickFilter)) {
            boolean hasSignals = !ratingAverageByProductName.isEmpty() || !customerOrders.isEmpty();
            return !hasSignals || getProductRating(product) >= 4f || getOrderedCount(product.getProductId()) > 0;
        }
        return true;
    }

    private List<LocalProduct> pickPersonalizedProducts() {
        List<LocalProduct> picks = new ArrayList<>();
        for (String favoriteId : favoriteIds) {
            LocalProduct product = findProductById(favoriteId);
            addUniqueProduct(picks, product);
            if (picks.size() >= 4) {
                return picks;
            }
        }
        for (LocalOrder order : customerOrders) {
            for (LocalOrderItem item : order.getItems()) {
                addUniqueProduct(picks, findProductById(item.getProductId()));
                if (picks.size() >= 4) {
                    return picks;
                }
            }
        }
        List<LocalProduct> fallback = new ArrayList<>(allProducts);
        fallback.sort(Comparator.comparingInt(LocalProduct::getBasePrice)
                .thenComparing(LocalProduct::getName, String.CASE_INSENSITIVE_ORDER));
        for (LocalProduct product : fallback) {
            addUniqueProduct(picks, product);
            if (picks.size() >= 4) {
                break;
            }
        }
        return picks;
    }

    private TextView createFilterChip(String label, boolean selected, Runnable action) {
        TextView chip = createBaseChip(label, selected);
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        chip.setOnClickListener(v -> action.run());
        return chip;
    }

    private TextView createQuickChip(String label, boolean selected, Runnable action) {
        TextView chip = createBaseChip(label, selected);
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        chip.setOnClickListener(v -> action.run());
        return chip;
    }

    private TextView createBaseChip(String label, boolean selected) {
        TextView chip = new TextView(requireContext());
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);

        chip.setBackgroundResource(selected ? R.drawable.app_filter_chip_selected : R.drawable.app_filter_chip_outline);
        chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.dashboard_primary : R.color.dashboard_text_secondary));
        return chip;
    }

    private View createProductCard(LayoutInflater inflater, LocalProduct product) {
        View itemView = inflater.inflate(R.layout.listview_layout, linearDynamicSections, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(168), ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(12), 0);
        itemView.setLayoutParams(params);

        ImageButton image = itemView.findViewById(R.id.imageButton1);
        ImageButton addButton = itemView.findViewById(R.id.imageButton2);
        ImageButton btnFavorite = itemView.findViewById(R.id.btnFavorite);
        TextView tvTen = itemView.findViewById(R.id.tvTen);
        TextView tvGia = itemView.findViewById(R.id.tvGia);
        TextView tvBadge = itemView.findViewById(R.id.tvBadge);

        tvTen.setText(product.getName());
        tvGia.setText(MoneyFormatter.format(product.getBasePrice()));
        tvBadge.setText(buildBadge(product));
        ViewCompat.setTransitionName(image, "product_image_" + product.getProductId());
        UiMotion.applyPressFeedback(itemView);

        Glide.with(requireContext())
                .load(product.getImageUrl())
                .placeholder(R.drawable.cfplus4)
                .error(R.drawable.cfplus4)
                .into(image);

        bindFavoriteIcon(btnFavorite, product.getProductId());
        btnFavorite.setOnClickListener(v -> {
            boolean nextFavorite = !favoriteIds.contains(product.getProductId());
            favoriteIds.remove(product.getProductId());
            if (nextFavorite) {
                favoriteIds.add(product.getProductId());
            }
            bindFavoriteIcon(btnFavorite, product.getProductId());
            tvBadge.setText(buildBadge(product));
            UiMotion.pulse(btnFavorite);
            Toast.makeText(requireContext(),
                    nextFavorite ? "Đã lưu món yêu thích" : "Đã bỏ khỏi yêu thích",
                    Toast.LENGTH_SHORT).show();
            String userId = sessionManager.getCurrentUserId();
            if (userId != null) {
                wishlistCloudRepository.setFavorite(userId, product.getProductId(), nextFavorite, (success, message) -> {
                    if (!success && isAdded()) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                                message == null ? "Chưa đồng bộ được món yêu thích lên Firebase." : message,
                                Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });

        addButton.setOnClickListener(v -> {
            UiMotion.bounce(addButton);
            openProductDetail(product, image);
        });

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), chitiet_sanpham.class);
            intent.putExtra("Ten", product.getName());
            intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
            intent.putExtra("hinhAnh", product.getImageUrl());
            intent.putExtra("productId", product.getProductId());
            intent.putExtra("image_transition_name", "product_image_" + product.getProductId());
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    image,
                    "product_image_" + product.getProductId()
            );
            startActivity(intent, options.toBundle());
        });
        return itemView;
    }

    private void openProductDetail(@NonNull LocalProduct product, @NonNull View image) {
        Intent intent = new Intent(getActivity(), chitiet_sanpham.class);
        intent.putExtra("Ten", product.getName());
        intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
        intent.putExtra("hinhAnh", product.getImageUrl());
        intent.putExtra("productId", product.getProductId());
        intent.putExtra("image_transition_name", "product_image_" + product.getProductId());
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                image,
                "product_image_" + product.getProductId()
        );
        startActivity(intent, options.toBundle());
    }

    private void bindFavoriteIcon(ImageButton button, String productId) {
        button.setImageResource(favoriteIds.contains(productId)
                ? R.drawable.ic_heart_filled
                : R.drawable.ic_heart_outline);
    }

    private String buildBadge(LocalProduct product) {
        float rating = getProductRating(product);
        if (rating >= 4f) {
            return String.format(Locale.getDefault(), "%.1f★", rating);
        }
        if (favoriteIds.contains(product.getProductId())) {
            return "Yêu thích";
        }
        if (isPromotionProduct(product)) {
            return "Ưu đãi";
        }
        if (product.getBasePrice() <= 30000) {
            return "Giá tốt";
        }
        return "Gợi ý";
    }

    private boolean hasAdvancedFilters() {
        return selectedMaxPrice > 0 || filterMinFourStars || filterPromotionOnly || !SORT_NAME.equals(selectedSortMode);
    }

    private float getProductRating(@NonNull LocalProduct product) {
        return ratingAverageByProductName.getOrDefault(normalizeProductKey(product.getName()), 0f);
    }

    private int getOrderedCount(String productId) {
        int count = 0;
        for (LocalOrder order : customerOrders) {
            for (LocalOrderItem item : order.getItems()) {
                if (productId.equals(item.getProductId())) {
                    count += Math.max(1, item.getQty());
                }
            }
        }
        return count;
    }

    private boolean isPromotionProduct(@NonNull LocalProduct product) {
        if (allProducts.isEmpty()) {
            return false;
        }
        List<LocalProduct> sorted = new ArrayList<>(allProducts);
        sorted.sort(Comparator.comparingInt(LocalProduct::getBasePrice));
        int thresholdIndex = Math.max(0, (int) Math.floor(sorted.size() * 0.35f) - 1);
        int thresholdPrice = sorted.get(thresholdIndex).getBasePrice();
        return product.getBasePrice() <= thresholdPrice;
    }

    @Nullable
    private LocalProduct findProductById(String productId) {
        for (LocalProduct product : allProducts) {
            if (product.getProductId().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    private void addUniqueProduct(List<LocalProduct> products, @Nullable LocalProduct product) {
        if (product == null) {
            return;
        }
        for (LocalProduct current : products) {
            if (current.getProductId().equals(product.getProductId())) {
                return;
            }
        }
        products.add(product);
    }

    @NonNull
    private String normalizeProductKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.getDefault());
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
