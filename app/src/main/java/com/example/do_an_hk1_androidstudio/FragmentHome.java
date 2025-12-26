package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class FragmentHome extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private SearchView searchView;
    private ListView lvSearch;
    private TimKiemAdapter searchAdapter;
    private List<SanPham> searchList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tabLayout  = view.findViewById(R.id.tabLayout);
        viewPager2 = view.findViewById(R.id.viewPager2);
        searchView = view.findViewById(R.id.action_search);
        lvSearch   = view.findViewById(R.id.lvSearch);

        // Đảm bảo tabLayout và viewPager2 luôn hiển thị
        tabLayout.setVisibility(View.VISIBLE);
        viewPager2.setVisibility(View.VISIBLE);
        lvSearch.setVisibility(View.GONE);

        ViewPagerAdapter vpAdapter = new ViewPagerAdapter(requireActivity());
        viewPager2.setAdapter(vpAdapter);
        
        // Đảm bảo fragment đầu tiên được load ngay
        viewPager2.setCurrentItem(0, false);
        
        new TabLayoutMediator(tabLayout, viewPager2,
                (tab, pos) -> {
                    if (pos == 0)      tab.setText("Tất cả");
                    else if (pos == 1) tab.setText("Best Seller");
                    else               tab.setText("Món ngon phải thử");
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
                    tabLayout.setVisibility(View.VISIBLE);
                    viewPager2.setVisibility(View.VISIBLE);
                } else {
                    doSearch(newText);
                }
                return true;
            }
        });

        // Không cần setOnItemClickListener nữa vì đã xử lý click trong adapter

        return view;
    }

    private void doSearch(String keyword) {
        // Kiểm tra từ khóa hợp lệ để tránh ký tự nguy hiểm=> Bảo mật môn an toàn thông tin
        if (!isValidSearchKeyword(keyword)) {
            searchList.clear();
            searchAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Từ khóa không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        tabLayout.setVisibility(View.GONE);
        viewPager2.setVisibility(View.GONE);
        lvSearch.setVisibility(View.VISIBLE);

        // Clear danh sách kết quả trước
        searchList.clear();
        searchAdapter.notifyDataSetChanged();
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String keywordLower = removeAccent(keyword.toLowerCase().trim());
        
        // Nếu từ khóa rỗng, không tìm kiếm
        if (keywordLower.isEmpty()) {
            return;
        }
        
        // Tìm kiếm trong tất cả các collection sản phẩm
        db.collection("SanPham")
                .get()
                .addOnSuccessListener(rootCats -> {
                    // Dùng Map để track các sản phẩm đã thêm (tránh trùng lặp)
                    // Key: tên đã normalize hoàn toàn, Value: SanPhamWithScore
                    java.util.Map<String, SanPhamWithScore> productMap = new java.util.HashMap<>();
                    
                    // Tạo list các Task để fetch từng sub-collection
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                    for (DocumentSnapshot catDoc : rootCats) {
                        String catId = catDoc.getId(); // ví dụ "CaFe", "Matcha"…
                        // Mỗi doc có sub-collection cùng tên (nhưng cần map đúng tên)
                        String collectionName = getCollectionName(catId);
                        tasks.add(db.collection("SanPham")
                                .document(catId)
                                .collection(collectionName)
                                .get());
                    }
                    
                    // Khi tất cả sub-collections đều fetch xong
                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                for (Object result : results) {
                                    QuerySnapshot qs = (QuerySnapshot) result;
                                    for (QueryDocumentSnapshot doc : qs) {
                                        String ten = doc.getString("Ten");
                                        String gia = doc.getString("Gia");
                                        String hinh = doc.getString("hinhAnh");
                                        
                                        if (ten != null && gia != null && hinh != null) {
                                            // Normalize tên sản phẩm để so sánh (loại bỏ tất cả khác biệt)
                                            String tenNormalized = normalizeProductName(ten);
                                            
                                            // Kiểm tra nếu tên sản phẩm chứa từ khóa (tìm kiếm theo ký tự)
                                            if (tenNormalized.contains(keywordLower)) {
                                                // CHỈ thêm nếu chưa có trong map (bỏ qua hoàn toàn nếu trùng)
                                                if (!productMap.containsKey(tenNormalized)) {
                                                    // Tính điểm tương đồng
                                                    int score = calculateSimilarityScore(tenNormalized, keywordLower);
                                                    SanPhamWithScore newProduct = new SanPhamWithScore(new SanPham(ten, gia, hinh), score);
                                                    productMap.put(tenNormalized, newProduct);
                                                    Log.d("Search", "Added: " + ten + " -> normalized: [" + tenNormalized + "]");
                                                } else {
                                                    Log.d("Search", "SKIPPED DUPLICATE: " + ten + " -> normalized: [" + tenNormalized + "]");
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Chuyển từ Map sang List và sắp xếp
                                List<SanPhamWithScore> productsWithScore = new ArrayList<>(productMap.values());
                                productsWithScore.sort((a, b) -> Integer.compare(b.score, a.score));
                                
                                // Clear và thêm vào danh sách kết quả
                                searchList.clear();
                                for (SanPhamWithScore item : productsWithScore) {
                                    searchList.add(item.sanPham);
                                }
                                
                                searchAdapter.notifyDataSetChanged();
                                Log.d("Search", "Final result: " + searchList.size() + " unique products for keyword: " + keyword);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Search", "Error fetching subcollections", e);
                                Toast.makeText(getContext(), "Lỗi khi tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Search", "Error fetching root categories", e);
                    Toast.makeText(getContext(), "Lỗi khi tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    // Map tên document sang tên collection (giống như trong tabOne_TatCa)
    private String getCollectionName(String docName) {
        switch (docName) {
            case "CaFe":
                return "Cafe";
            case "Trà sữa":
                return "trasua";
            case "Matcha":
                return "matcha";
            case "Topping":
                return "topping";
            default:
                return docName.toLowerCase().replace(" ", "");
        }
    }

    // Regex: chỉ cho phép chữ cái, số, khoảng trắng=> Bảo mật môn an toàn thông tin
    private boolean isValidSearchKeyword(String keyword) {
        return keyword != null && keyword.matches("[\\p{L}\\p{N} ]{1,50}");
    }


    private String removeAccent(String s) {
        String tmp = Normalizer.normalize(s.toLowerCase().trim(), Normalizer.Form.NFD);
        tmp = tmp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return tmp.replace("đ", "d").replace("Đ", "D");
    }
    
    // Normalize tên sản phẩm để so sánh: loại bỏ tất cả khác biệt về dấu, khoảng trắng, HTML entities
    private String normalizeProductName(String name) {
        if (name == null || name.isEmpty()) return "";
        
        String normalized = name;
        
        // 1. Loại bỏ HTML entities nếu có
        if (normalized.contains("&#")) {
            try {
                normalized = android.text.Html.fromHtml(normalized, android.text.Html.FROM_HTML_MODE_LEGACY).toString();
            } catch (Exception e) {
                // Nếu lỗi, giữ nguyên
            }
        }
        
        // 2. Trim và loại bỏ khoảng trắng thừa
        normalized = normalized.trim();
        
        // 3. Lowercase
        normalized = normalized.toLowerCase();
        
        // 4. Remove accent (loại bỏ dấu tiếng Việt)
        normalized = removeAccent(normalized);
        
        // 5. Loại bỏ TẤT CẢ khoảng trắng và ký tự đặc biệt không cần thiết
        normalized = normalized.replaceAll("\\s+", ""); // Loại bỏ tất cả khoảng trắng
        normalized = normalized.replaceAll("[^a-z0-9]", ""); // Chỉ giữ lại chữ và số
        
        return normalized;
    }
    
    // Tính điểm tương đồng: sản phẩm có tên bắt đầu bằng từ khóa hoặc chứa từ khóa
    private int calculateSimilarityScore(String productName, String keyword) {
        // Nếu tên sản phẩm bắt đầu bằng từ khóa -> điểm cao nhất
        if (productName.startsWith(keyword)) {
            return 100 + keyword.length();
        }
        
        // Nếu tên sản phẩm chứa từ khóa ở vị trí gần đầu -> điểm cao
        int index = productName.indexOf(keyword);
        if (index >= 0 && index < 5) {
            return 80 + keyword.length();
        }
        
        // Nếu chứa từ khóa -> điểm trung bình
        if (productName.contains(keyword)) {
            return 50 + keyword.length();
        }
        
        // Kiểm tra từng từ trong tên sản phẩm
        String[] words = productName.split("\\s+");
        for (String word : words) {
            if (word.startsWith(keyword)) {
                return 30 + keyword.length();
            }
            if (word.contains(keyword)) {
                return 10 + keyword.length();
            }
        }
        return 0;
    }
    
    private static class SanPhamWithScore {
        SanPham sanPham;
        int score;
        
        SanPhamWithScore(SanPham sanPham, int score) {
            this.sanPham = sanPham;
            this.score = score;
        }
    }
}
