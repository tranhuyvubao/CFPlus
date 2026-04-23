package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.BannerCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.CustomerCartStore;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.local.model.PromoBanner;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class FragmentHome extends Fragment {

    private ViewPager2 viewPager2;
    private SearchView searchView;
    private ListView lvSearch;
    private View tvSearchEmpty;
    private TextView tvCartCount;
    private HorizontalScrollView bannerScrollView;
    private LinearLayout layoutPromoBanners;
    private TimKiemAdapter searchAdapter;

    private final List<SanPham> searchList = new ArrayList<>();
    private final List<LocalProduct> allProducts = new ArrayList<>();

    private CatalogCloudRepository catalogRepository;
    private BannerCloudRepository bannerRepository;
    private ListenerRegistration productsListener;
    private ListenerRegistration bannersListener;
    private CustomerCartStore cartStore;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        catalogRepository = new CatalogCloudRepository(requireContext());
        bannerRepository = new BannerCloudRepository(requireContext());
        cartStore = new CustomerCartStore(requireContext());
        viewPager2 = view.findViewById(R.id.viewPager2);
        searchView = view.findViewById(R.id.action_search);
        lvSearch = view.findViewById(R.id.lvSearch);
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);
        tvCartCount = view.findViewById(R.id.tvHomeCartCount);
        bannerScrollView = view.findViewById(R.id.bannerScrollView);
        layoutPromoBanners = view.findViewById(R.id.layoutPromoBanners);
        View btnOpenCart = view.findViewById(R.id.btnOpenCart);

        if (btnOpenCart != null) {
            btnOpenCart.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), DatMonOnlineActivity.class)));
        }

        viewPager2.setVisibility(View.VISIBLE);
        lvSearch.setVisibility(View.GONE);
        tvSearchEmpty.setVisibility(View.GONE);

        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return new tabOne_TatCa();
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });
        viewPager2.setCurrentItem(0, false);
        viewPager2.setUserInputEnabled(false);

        searchAdapter = new TimKiemAdapter(requireContext(), searchList);
        lvSearch.setAdapter(searchAdapter);

        searchView.setIconifiedByDefault(false);
        searchView.post(() -> {
            View searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
            if (searchPlate != null) {
                searchPlate.setBackground(null);
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                doSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText.trim())) {
                    lvSearch.setVisibility(View.GONE);
                    tvSearchEmpty.setVisibility(View.GONE);
                    viewPager2.setVisibility(View.VISIBLE);
                } else {
                    doSearch(newText);
                }
                return true;
            }
        });

        listenProducts();
        listenBanners();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindCartCount();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productsListener != null) {
            productsListener.remove();
        }
        if (bannersListener != null) {
            bannersListener.remove();
        }
        if (viewPager2 != null) {
            viewPager2.setAdapter(null);
        }
    }

    public void pulseCartBadge() {
        if (tvCartCount != null && tvCartCount.getVisibility() == View.VISIBLE) {
            com.example.do_an_hk1_androidstudio.ui.UiMotion.pulse(tvCartCount);
        }
    }

    private void bindCartCount() {
        if (tvCartCount == null) {
            return;
        }
        int count = cartStore.getItemCount();
        tvCartCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        tvCartCount.setText(String.valueOf(count));
    }

    private void listenProducts() {
        if (productsListener != null) {
            productsListener.remove();
        }
        productsListener = catalogRepository.listenProducts(products -> {
            allProducts.clear();
            for (LocalProduct product : products) {
                if (product.isActive()) {
                    allProducts.add(product);
                }
            }
        });
    }

    private void listenBanners() {
        if (bannersListener != null) {
            bannersListener.remove();
        }
        bannersListener = bannerRepository.listenBanners(banners -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            renderPromoBanners(banners);
        });
    }

    private void renderPromoBanners(List<PromoBanner> banners) {
        layoutPromoBanners.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int visibleCount = 0;
        for (PromoBanner banner : banners) {
            if (!banner.isActive()) {
                continue;
            }
            View itemView = inflater.inflate(R.layout.item_home_promo_banner, layoutPromoBanners, false);
            ImageView image = itemView.findViewById(R.id.imgBanner);
            TextView title = itemView.findViewById(R.id.tvBannerTitle);
            TextView subtitle = itemView.findViewById(R.id.tvBannerSubtitle);
            TextView action = itemView.findViewById(R.id.tvBannerAction);

            title.setText(banner.getTitle());
            subtitle.setText(banner.getSubtitle());
            action.setText(TextUtils.isEmpty(banner.getActionText()) ? "Xem ngay" : banner.getActionText());
            Glide.with(requireContext())
                    .load(banner.getImageUrl())
                    .placeholder(R.drawable.cfplus4)
                    .error(R.drawable.cfplus4)
                    .into(image);

            itemView.setOnClickListener(v -> openBannerTarget(banner));
            layoutPromoBanners.addView(itemView);
            visibleCount++;
        }
        bannerScrollView.setVisibility(visibleCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void openBannerTarget(PromoBanner banner) {
        List<LocalProduct> linkedProducts = findProductsByIds(banner.getProductIds());
        if (linkedProducts.isEmpty()) {
            Toast.makeText(requireContext(), banner.getTitle(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (linkedProducts.size() == 1) {
            openProductDetail(linkedProducts.get(0));
            return;
        }
        showBannerProductsDialog(banner, linkedProducts);
    }

    private void showBannerProductsDialog(PromoBanner banner, List<LocalProduct> linkedProducts) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(10));
        scrollView.addView(container);

        TextView title = new TextView(requireContext());
        title.setText(banner.getTitle());
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.dashboard_text_primary));
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        container.addView(title);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (LocalProduct product : linkedProducts) {
            View itemView = inflater.inflate(R.layout.item_banner_product_picker, container, false);
            bindBannerProductCard(itemView, product);
            itemView.setOnClickListener(v -> openProductDetail(product));
            container.addView(itemView);
        }

        new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setNegativeButton("Dong", null)
                .show();
    }

    private void bindBannerProductCard(View itemView, LocalProduct product) {
        ImageView image = itemView.findViewById(R.id.imgPickerProduct);
        TextView name = itemView.findViewById(R.id.tvPickerProductName);
        TextView meta = itemView.findViewById(R.id.tvPickerProductMeta);
        TextView price = itemView.findViewById(R.id.tvPickerProductPrice);
        TextView action = itemView.findViewById(R.id.tvPickerProductAction);

        name.setText(product.getName());
        meta.setText("Mon trong banner");
        price.setText(MoneyFormatter.format(product.getBasePrice()));
        action.setText("Xem");
        Glide.with(requireContext())
                .load(product.getImageUrl())
                .placeholder(R.drawable.cfplus4)
                .error(R.drawable.cfplus4)
                .into(image);
    }

    private void openProductDetail(LocalProduct product) {
        Intent intent = new Intent(getActivity(), chitiet_sanpham.class);
        intent.putExtra("Ten", product.getName());
        intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
        intent.putExtra("hinhAnh", product.getImageUrl());
        intent.putExtra("productId", product.getProductId());
        startActivity(intent);
    }

    private List<LocalProduct> findProductsByIds(List<String> productIds) {
        List<LocalProduct> products = new ArrayList<>();
        for (String productId : productIds) {
            LocalProduct product = findProductById(productId);
            if (product != null) {
                products.add(product);
            }
        }
        return products;
    }

    private LocalProduct findProductById(String productId) {
        for (LocalProduct product : allProducts) {
            if (product.getProductId().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void doSearch(String keyword) {
        if (!isValidSearchKeyword(keyword)) {
            searchList.clear();
            searchAdapter.notifyDataSetChanged();
            tvSearchEmpty.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "Tu khoa khong hop le.", Toast.LENGTH_SHORT).show();
            return;
        }

        viewPager2.setVisibility(View.GONE);
        lvSearch.setVisibility(View.VISIBLE);
        tvSearchEmpty.setVisibility(View.GONE);

        searchList.clear();
        searchAdapter.notifyDataSetChanged();

        String keywordLower = removeAccent(keyword.toLowerCase().trim());
        if (keywordLower.isEmpty()) {
            tvSearchEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<SanPhamWithScore> productsWithScore = new ArrayList<>();
        for (LocalProduct product : allProducts) {
            String normalizedName = normalizeProductName(product.getName());
            if (normalizedName.contains(keywordLower)) {
                productsWithScore.add(new SanPhamWithScore(
                        new SanPham(
                                product.getProductId(),
                                product.getName(),
                                String.valueOf(product.getBasePrice()),
                                product.getImageUrl()
                        ),
                        calculateSimilarityScore(normalizedName, keywordLower)
                ));
            }
        }

        productsWithScore.sort((a, b) -> Integer.compare(b.score, a.score));
        for (SanPhamWithScore item : productsWithScore) {
            searchList.add(item.sanPham);
        }
        searchAdapter.notifyDataSetChanged();
        tvSearchEmpty.setVisibility(searchList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isValidSearchKeyword(String keyword) {
        return keyword != null && keyword.matches("[\\p{L}\\p{N} ]{1,50}");
    }

    private String removeAccent(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase().trim(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.replace("đ", "d").replace("Đ", "D");
    }

    private String normalizeProductName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String normalized = removeAccent(name.trim().toLowerCase());
        normalized = normalized.replaceAll("\\s+", "");
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private int calculateSimilarityScore(String productName, String keyword) {
        if (productName.startsWith(keyword)) {
            return 100 + keyword.length();
        }
        int index = productName.indexOf(keyword);
        if (index >= 0 && index < 5) {
            return 80 + keyword.length();
        }
        if (productName.contains(keyword)) {
            return 50 + keyword.length();
        }
        return 0;
    }

    private static class SanPhamWithScore {
        private final SanPham sanPham;
        private final int score;

        SanPhamWithScore(SanPham sanPham, int score) {
            this.sanPham = sanPham;
            this.score = score;
        }
    }
}
