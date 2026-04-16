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
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class tabTwo_BestSeller extends Fragment {

    private final List<SanPham> productList = new ArrayList<>();
    private final List<LocalProduct> allProducts = new ArrayList<>();
    private CatalogCloudRepository catalogRepository;
    private OrderCloudRepository orderRepository;
    private SanPhamAdapter adapter;
    private ListenerRegistration productsListener;
    private ListenerRegistration ordersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_two__best_seller, container, false);

        catalogRepository = new CatalogCloudRepository(requireContext());
        orderRepository = new OrderCloudRepository(requireContext());

        RecyclerView recyclerViewBestSeller = view.findViewById(R.id.recyclerViewBestSeller);
        adapter = new SanPhamAdapter(getContext(), productList);
        recyclerViewBestSeller.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerViewBestSeller.setAdapter(adapter);

        productsListener = catalogRepository.listenProducts(products -> {
            allProducts.clear();
            for (LocalProduct product : products) {
                if (product.isActive()) {
                    allProducts.add(product);
                }
            }
            loadBestSellerProducts(new ArrayList<>());
        });
        ordersListener = orderRepository.listenAllOrders(this::loadBestSellerProducts);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productsListener != null) productsListener.remove();
        if (ordersListener != null) ordersListener.remove();
    }

    private void loadBestSellerProducts(List<LocalOrder> orders) {
        Map<String, ProductCount> productCountMap = new HashMap<>();
        for (LocalOrder order : orders) {
            for (LocalOrderItem item : order.getItems()) {
                String key = item.getProductId() != null ? item.getProductId() : item.getProductName();
                ProductCount current = productCountMap.get(key);
                if (current == null) {
                    productCountMap.put(key, new ProductCount(
                            item.getProductId(),
                            item.getProductName(),
                            item.getUnitPrice(),
                            item.getImageUrl(),
                            item.getQty()
                    ));
                } else {
                    current.count += item.getQty();
                }
            }
        }

        List<ProductCount> sortedProducts = new ArrayList<>(productCountMap.values());
        sortedProducts.sort((a, b) -> Integer.compare(b.count, a.count));

        productList.clear();
        int maxProducts = Math.min(10, sortedProducts.size());
        for (int i = 0; i < maxProducts; i++) {
            ProductCount product = sortedProducts.get(i);
            productList.add(new SanPham(product.productId, product.ten, String.valueOf(product.gia), product.hinhAnh));
        }

        if (productList.isEmpty()) {
            int maxFallback = Math.min(10, allProducts.size());
            for (int i = 0; i < maxFallback; i++) {
                LocalProduct product = allProducts.get(i);
                productList.add(new SanPham(product.getProductId(), product.getName(), String.valueOf(product.getBasePrice()), product.getImageUrl()));
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static class ProductCount {
        String productId;
        String ten;
        int gia;
        String hinhAnh;
        int count;

        ProductCount(String productId, String ten, int gia, String hinhAnh, int count) {
            this.productId = productId;
            this.ten = ten;
            this.gia = gia;
            this.hinhAnh = hinhAnh;
            this.count = count;
        }
    }
}
