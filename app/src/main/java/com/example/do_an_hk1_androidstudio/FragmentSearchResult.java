package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentSearchResult extends Fragment {

    private final List<SanPham> sanPhamList = new ArrayList<>();
    private final List<LocalProduct> allProducts = new ArrayList<>();
    private TimKiemAdapter adapter;
    private ListenerRegistration productsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);
        ListView listView = view.findViewById(R.id.lvTimKiem);
        adapter = new TimKiemAdapter(requireContext(), sanPhamList);
        listView.setAdapter(adapter);
        listenProducts();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productsListener != null) {
            productsListener.remove();
        }
    }

    private void listenProducts() {
        if (productsListener != null) {
            productsListener.remove();
        }
        productsListener = new CatalogCloudRepository(requireContext()).listenProducts(products -> {
            allProducts.clear();
            allProducts.addAll(products);
        });
    }

    public void timKiemSanPham(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        sanPhamList.clear();
        for (LocalProduct product : allProducts) {
            String name = product.getName();
            if (!product.isActive() || name == null) {
                continue;
            }
            if (!normalizedKeyword.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                continue;
            }
            sanPhamList.add(new SanPham(
                    product.getProductId(),
                    name,
                    String.valueOf(product.getBasePrice()),
                    product.getImageUrl()
            ));
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
