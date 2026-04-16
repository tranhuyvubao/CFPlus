package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalCategory;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class tabOne_TatCa extends Fragment {

    private final List<LocalCategory> categories = new ArrayList<>();
    private final List<LocalProduct> allProducts = new ArrayList<>();

    private CatalogCloudRepository catalogRepository;
    private ListenerRegistration categoriesListener;
    private ListenerRegistration productsListener;
    private LinearLayout linearDynamicSections;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_one__tatca, container, false);
        catalogRepository = new CatalogCloudRepository(requireContext());
        linearDynamicSections = view.findViewById(R.id.linearDynamicSections);
        listenCategories();
        listenProducts();
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
    }

    private void listenCategories() {
        categoriesListener = catalogRepository.listenCategories(fetchedCategories -> {
            categories.clear();
            for (LocalCategory category : fetchedCategories) {
                if (category.isActive()) {
                    categories.add(category);
                }
            }
            categories.sort(Comparator.comparing(LocalCategory::getName, String::compareToIgnoreCase));
            if (isAdded()) {
                renderSections();
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
                renderSections();
            }
        });
    }

    private void renderSections() {
        if (linearDynamicSections == null || getContext() == null) {
            return;
        }
        linearDynamicSections.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (LocalCategory category : categories) {
            List<LocalProduct> categoryProducts = new ArrayList<>();
            for (LocalProduct product : allProducts) {
                if (category.getCategoryId().equals(product.getCategoryId())) {
                    categoryProducts.add(product);
                }
            }
            if (categoryProducts.isEmpty()) {
                continue;
            }

            TextView heading = new TextView(requireContext());
            heading.setText(category.getName());
            heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            heading.setTextColor(getResources().getColor(R.color.brown, null));
            heading.setTypeface(heading.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            headingParams.setMargins(dp(8), dp(10), dp(8), dp(8));
            heading.setLayoutParams(headingParams);
            linearDynamicSections.addView(heading);

            HorizontalScrollView horizontalScrollView = new HorizontalScrollView(requireContext());
            horizontalScrollView.setHorizontalScrollBarEnabled(false);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            horizontalScrollView.setLayoutParams(scrollParams);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            for (LocalProduct product : categoryProducts) {
                row.addView(createProductCard(inflater, product));
            }

            horizontalScrollView.addView(row);
            linearDynamicSections.addView(horizontalScrollView);
        }

        if (linearDynamicSections.getChildCount() == 0) {
            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có món nào để hiển thị.");
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            empty.setTextColor(getResources().getColor(R.color.coffee_muted, null));
            empty.setPadding(dp(16), dp(24), dp(16), dp(24));
            linearDynamicSections.addView(empty);
        }
    }

    private View createProductCard(LayoutInflater inflater, LocalProduct product) {
        View itemView = inflater.inflate(R.layout.listview_layout, linearDynamicSections, false);
        ImageButton img = itemView.findViewById(R.id.imageButton1);
        TextView tvTen = itemView.findViewById(R.id.tvTen);
        TextView tvGia = itemView.findViewById(R.id.tvGia);

        tvTen.setText(product.getName());
        tvGia.setText(MoneyFormatter.format(product.getBasePrice()));
        Glide.with(requireContext())
                .load(product.getImageUrl())
                .placeholder(R.drawable.cfplus4)
                .error(R.drawable.cfplus4)
                .into(img);

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), chitiet_sanpham.class);
            intent.putExtra("Ten", product.getName());
            intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
            intent.putExtra("hinhAnh", product.getImageUrl());
            intent.putExtra("productId", product.getProductId());
            startActivity(intent);
        });
        return itemView;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
