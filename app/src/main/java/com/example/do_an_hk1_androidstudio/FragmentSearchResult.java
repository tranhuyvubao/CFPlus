package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FragmentSearchResult extends Fragment {

    private ListView listView;
    private TimKiemAdapter adapter;
    private List<SanPham> sanPhamList = new ArrayList<>();

    public FragmentSearchResult() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);
        listView = view.findViewById(R.id.lvTimKiem);
        adapter = new TimKiemAdapter(requireContext(), sanPhamList);
        listView.setAdapter(adapter);
        return view;
    }

    /**
     * Tìm kiếm sản phẩm theo tên (keyword).
     * Duyệt qua tất cả doc trong root-collection "SanPham",
     * rồi vào từng sub-collection (tên giống doc-id) để thu thập và filter.
     */
    public void timKiemSanPham(String keyword) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("SanPham")
                .get()
                .addOnSuccessListener(rootCats -> {
                    sanPhamList.clear();

                    // Tạo list các Task để fetch từng sub-collection
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                    for (DocumentSnapshot catDoc : rootCats) {
                        String catId = catDoc.getId();           // ví dụ "CaFe", "Matcha"…
                        // mỗi doc có sub-collection cùng tên
                        tasks.add(db.collection("SanPham")
                                .document(catId)
                                .collection(catId)
                                .get());
                    }

                    // Khi tất cả sub-collections đều fetch xong
                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                for (Object result : results) {
                                    QuerySnapshot qs = (QuerySnapshot) result;
                                    for (QueryDocumentSnapshot doc : qs) {
                                        String ten = doc.getString("Ten");
                                        if (ten != null &&
                                                ten.toLowerCase().contains(keyword.toLowerCase())) {
                                            String gia = doc.getString("Gia");
                                            String hinh = doc.getString("hinhAnh");
                                            sanPhamList.add(new SanPham(ten, gia, hinh));
                                        }
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                Log.d("SearchResult", "Found items: " + sanPhamList.size());
                            })
                            .addOnFailureListener(e -> {
                                Log.e("SearchResult", "Error fetching subcollections", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("SearchResult", "Error fetching root categories", e);
                });
    }
}
