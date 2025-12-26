package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class tabTwo_BestSeller extends Fragment {

    RecyclerView recyclerViewBestSeller;
    SanPhamAdapter adapter;
    List<SanPham> productList;
    FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_two__best_seller, container, false);

        recyclerViewBestSeller = view.findViewById(R.id.recyclerViewBestSeller);
        productList = new ArrayList<>();
        adapter = new SanPhamAdapter(getContext(), productList);
        
        // GridLayoutManager với 2 cột
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerViewBestSeller.setLayoutManager(gridLayoutManager);
        recyclerViewBestSeller.setAdapter(adapter);
        
        db = FirebaseFirestore.getInstance();
        loadBestSellerProducts();

        return view;
    }

    private void loadBestSellerProducts() {
        // Đếm số lần đặt của mỗi sản phẩm từ đơn hàng
        db.collection("Đơn hàng")
                .document("Giỏ hàng")
                .collection("Sản phẩm")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Map để đếm số lần đặt: key = tên sản phẩm, value = số lần + thông tin sản phẩm
                    Map<String, ProductCount> productCountMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String tenSanPham = doc.getString("Tên sản phẩm");
                        String hinhAnh = doc.getString("hinhAnh");
                        
                        // Xử lý giá có thể là String hoặc Long
                        String gia = null;
                        Object giaObj = doc.get("Giá");
                        if (giaObj instanceof String) {
                            gia = (String) giaObj;
                        } else if (giaObj instanceof Long) {
                            gia = String.valueOf((Long) giaObj);
                        } else if (giaObj instanceof Number) {
                            gia = String.valueOf(((Number) giaObj).intValue());
                        }
                        
                        Long soLuong = doc.getLong("Số lượng");
                        int count = soLuong != null ? soLuong.intValue() : 1;

                        if (tenSanPham != null && gia != null) {
                            if (productCountMap.containsKey(tenSanPham)) {
                                ProductCount pc = productCountMap.get(tenSanPham);
                                pc.count += count;
                            } else {
                                productCountMap.put(tenSanPham, new ProductCount(tenSanPham, gia, hinhAnh, count));
                            }
                        }
                    }

                    // Sắp xếp theo số lần đặt giảm dần
                    List<ProductCount> sortedProducts = new ArrayList<>(productCountMap.values());
                    sortedProducts.sort((a, b) -> Integer.compare(b.count, a.count));

                    // Hiển thị top sản phẩm (tối đa 10 sản phẩm)
                    int maxProducts = Math.min(10, sortedProducts.size());
                    productList.clear();
                    for (int i = 0; i < maxProducts; i++) {
                        ProductCount pc = sortedProducts.get(i);
                        productList.add(new SanPham(pc.ten, pc.gia, pc.hinhAnh));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Nếu không có đơn hàng, load tất cả sản phẩm
                    loadAllProductsAsFallback();
                });
    }

    private void loadAllProductsAsFallback() {
        // Load tất cả sản phẩm nếu chưa có đơn hàng
        String[] collections = {"CaFe", "Trà sữa", "Matcha", "Topping"};
        String[] docNames = {"CaFe", "Trà sữa", "Matcha", "Topping"};
        String[] collectionNames = {"Cafe", "trasua", "matcha", "topping"};

        int[] completed = {0};
        int totalCollections = collections.length;
        productList.clear();

        for (int i = 0; i < collections.length; i++) {
            final int index = i;
            db.collection("SanPham")
                    .document(docNames[index])
                    .collection(collectionNames[index])
                    .limit(3)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                            String ten = snapshot.getString("Ten");
                            String gia = snapshot.getString("Gia");
                            String hinh = snapshot.getString("hinhAnh");
                            if (ten != null && gia != null && hinh != null) {
                                productList.add(new SanPham(ten, gia, hinh));
                            }
                        }
                        completed[0]++;
                        if (completed[0] == totalCollections) {
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private static class ProductCount {
        String ten;
        String gia;
        String hinhAnh;
        int count;

        ProductCount(String ten, String gia, String hinhAnh, int count) {
            this.ten = ten;
            this.gia = gia;
            this.hinhAnh = hinhAnh;
            this.count = count;
        }
    }
}