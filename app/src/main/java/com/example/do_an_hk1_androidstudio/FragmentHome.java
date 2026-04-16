package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.CustomerCartStore;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class FragmentHome extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private SearchView searchView;
    private ListView lvSearch;
    private View tvSearchEmpty;
    private TextView tvCartCount;
    private TimKiemAdapter searchAdapter;

    private final List<SanPham> searchList = new ArrayList<>();
    private final List<LocalProduct> allProducts = new ArrayList<>();

    private CatalogCloudRepository catalogRepository;
    private ListenerRegistration productsListener;
    private CustomerCartStore cartStore;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        catalogRepository = new CatalogCloudRepository(requireContext());
        cartStore = new CustomerCartStore(requireContext());
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager2 = view.findViewById(R.id.viewPager2);
        searchView = view.findViewById(R.id.action_search);
        lvSearch = view.findViewById(R.id.lvSearch);
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);
        tvCartCount = view.findViewById(R.id.tvHomeCartCount);
        View btnOpenCart = view.findViewById(R.id.btnOpenCart);

        if (btnOpenCart != null) {
            btnOpenCart.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), DatMonOnlineActivity.class)));
        }

        tabLayout.setVisibility(View.VISIBLE);
        viewPager2.setVisibility(View.VISIBLE);
        lvSearch.setVisibility(View.GONE);
        tvSearchEmpty.setVisibility(View.GONE);

        ViewPagerAdapter vpAdapter = new ViewPagerAdapter(requireActivity());
        viewPager2.setAdapter(vpAdapter);
        viewPager2.setCurrentItem(0, false);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, pos) -> {
            if (pos == 0) {
                tab.setText("Tất cả");
            } else if (pos == 1) {
                tab.setText("Best Seller");
            } else {
                tab.setText("Món ngon phải thử");
            }
        }).attach();

        searchAdapter = new TimKiemAdapter(requireContext(), searchList);
        lvSearch.setAdapter(searchAdapter);

        searchView.setIconifiedByDefault(false);
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
                    tabLayout.setVisibility(View.VISIBLE);
                    viewPager2.setVisibility(View.VISIBLE);
                } else {
                    doSearch(newText);
                }
                return true;
            }
        });

        listenProducts();
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

    private void doSearch(String keyword) {
        if (!isValidSearchKeyword(keyword)) {
            searchList.clear();
            searchAdapter.notifyDataSetChanged();
            tvSearchEmpty.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "Từ khóa không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        tabLayout.setVisibility(View.GONE);
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
