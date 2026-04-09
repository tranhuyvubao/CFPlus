package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class tabThree_Monngon extends Fragment {

    private final List<SanPham> productList = new ArrayList<>();
    private CatalogCloudRepository catalogRepository;
    private SanPhamAdapter adapter;
    private ListenerRegistration productsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_three__monngon, container, false);

        catalogRepository = new CatalogCloudRepository(requireContext());
        RecyclerView recyclerViewMonNgon = view.findViewById(R.id.recyclerViewMonNgon);
        adapter = new SanPhamAdapter(getContext(), productList);
        recyclerViewMonNgon.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerViewMonNgon.setAdapter(adapter);

        productsListener = catalogRepository.listenProducts(this::loadRandomProducts);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productsListener != null) {
            productsListener.remove();
        }
    }

    private void loadRandomProducts(List<LocalProduct> products) {
        List<LocalProduct> allProducts = new ArrayList<>();
        for (LocalProduct product : products) {
            if (product.isActive()) {
                allProducts.add(product);
            }
        }
        Collections.shuffle(allProducts, new Random());
        int maxProducts = Math.min(10, allProducts.size());
        productList.clear();
        for (int i = 0; i < maxProducts; i++) {
            LocalProduct product = allProducts.get(i);
            productList.add(new SanPham(product.getName(), String.valueOf(product.getBasePrice()), product.getImageUrl()));
        }
        adapter.notifyDataSetChanged();
    }
}
