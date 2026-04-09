package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class tabOne_TatCa extends Fragment {

    private LinearLayout linearCafe;
    private LinearLayout linearTraSua;
    private LinearLayout linearMatcha;
    private LinearLayout linearTopping;
    private CatalogCloudRepository catalogRepository;
    private ListenerRegistration productsListener;
    private final List<LocalProduct> allProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_one__tatca, container, false);

        catalogRepository = new CatalogCloudRepository(requireContext());
        linearCafe = view.findViewById(R.id.linearCafe);
        linearTraSua = view.findViewById(R.id.linearTraSua);
        linearMatcha = view.findViewById(R.id.linearMatcha);
        linearTopping = view.findViewById(R.id.linearTopping);

        productsListener = catalogRepository.listenProducts(products -> {
            allProducts.clear();
            for (LocalProduct product : products) {
                if (product.isActive()) {
                    allProducts.add(product);
                }
            }
            if (!isAdded()) {
                return;
            }
            LayoutInflater currentInflater = LayoutInflater.from(requireContext());
            loadProducts(linearCafe, currentInflater, "cafe");
            loadProducts(linearTraSua, currentInflater, "tra_sua");
            loadProducts(linearMatcha, currentInflater, "matcha");
            loadProducts(linearTopping, currentInflater, "topping");
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productsListener != null) {
            productsListener.remove();
        }
    }

    private void loadProducts(LinearLayout layout, LayoutInflater inflater, String categoryId) {
        layout.removeAllViews();
        for (LocalProduct product : allProducts) {
            if (!categoryId.equals(product.getCategoryId())) {
                continue;
            }

            View itemView = inflater.inflate(R.layout.listview_layout, null);
            ImageButton img = itemView.findViewById(R.id.imageButton1);
            TextView tvTen = itemView.findViewById(R.id.tvTen);
            TextView tvGia = itemView.findViewById(R.id.tvGia);

            tvTen.setText(product.getName());
            tvGia.setText(product.getBasePrice() + "đ");
            Glide.with(requireContext()).load(product.getImageUrl()).into(img);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), chitiet_sanpham.class);
                intent.putExtra("Ten", product.getName());
                intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
                intent.putExtra("hinhAnh", product.getImageUrl());
                intent.putExtra("productId", product.getProductId());
                startActivity(intent);
            });
            layout.addView(itemView);
        }
    }
}
