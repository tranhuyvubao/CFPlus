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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class tabThree_Monngon extends Fragment {

    RecyclerView recyclerViewMonNgon;
    SanPhamAdapter adapter;
    List<SanPham> productList;
    FirebaseFirestore db;
    List<SanPham> allProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_three__monngon, container, false);

        recyclerViewMonNgon = view.findViewById(R.id.recyclerViewMonNgon);
        productList = new ArrayList<>();
        adapter = new SanPhamAdapter(getContext(), productList);
        
        // GridLayoutManager với 2 cột
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerViewMonNgon.setLayoutManager(gridLayoutManager);
        recyclerViewMonNgon.setAdapter(adapter);
        
        db = FirebaseFirestore.getInstance();
        loadRandomProducts();

        return view;
    }

    private void loadRandomProducts() {
        // Load tất cả sản phẩm từ các collection
        String[] collections = {"CaFe", "Trà sữa", "Matcha", "Topping"};
        String[] docNames = {"CaFe", "Trà sữa", "Matcha", "Topping"};
        String[] collectionNames = {"Cafe", "trasua", "matcha", "topping"};

        int[] completed = {0};
        int totalCollections = collections.length;

        for (int i = 0; i < collections.length; i++) {
            final int index = i;
            db.collection("SanPham")
                    .document(docNames[index])
                    .collection(collectionNames[index])
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                            String ten = snapshot.getString("Ten");
                            String gia = snapshot.getString("Gia");
                            String hinh = snapshot.getString("hinhAnh");
                            if (ten != null && gia != null && hinh != null) {
                                allProducts.add(new SanPham(ten, gia, hinh));
                            }
                        }

                        completed[0]++;
                        if (completed[0] == totalCollections) {
                            // Xáo trộn danh sách và hiển thị ngẫu nhiên
                            Collections.shuffle(allProducts, new Random());
                            int maxProducts = Math.min(10, allProducts.size());
                            productList.clear();
                            for (int j = 0; j < maxProducts; j++) {
                                productList.add(allProducts.get(j));
                            }
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }
}