package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.WishlistCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity {

    private final List<LocalProduct> favorites = new ArrayList<>();
    private final List<LocalProduct> allProducts = new ArrayList<>();
    private final Set<String> favoriteIds = new LinkedHashSet<>();
    private CatalogCloudRepository catalogRepository;
    private WishlistCloudRepository wishlistCloudRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration productsListener;
    private ListenerRegistration favoritesListener;
    private FavoriteAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        InsetsHelper.applyActivityRootPadding(this);

        catalogRepository = new CatalogCloudRepository(this);
        wishlistCloudRepository = new WishlistCloudRepository(this);
        sessionManager = new LocalSessionManager(this);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        tvEmpty = findViewById(R.id.tvFavoritesEmpty);
        RecyclerView rvFavorites = findViewById(R.id.rvFavorites);
        rvFavorites.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new FavoriteAdapter();
        rvFavorites.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (productsListener != null) {
            productsListener.remove();
        }
        productsListener = catalogRepository.listenProducts(products -> runOnUiThread(() -> {
            allProducts.clear();
            allProducts.addAll(products);
            bindFavorites();
        }));
        String customerId = sessionManager.getCurrentUserId();
        if (customerId != null) {
            favoritesListener = wishlistCloudRepository.listenFavoriteIds(customerId, ids -> runOnUiThread(() -> {
                favoriteIds.clear();
                favoriteIds.addAll(ids);
                bindFavorites();
            }));
        } else {
            favoriteIds.clear();
            bindFavorites();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
        }
        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }
    }

    private void bindFavorites() {
        favorites.clear();
        for (LocalProduct product : allProducts) {
            if (product.isActive() && favoriteIds.contains(product.getProductId())) {
                favorites.add(product);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class FavoriteAdapter extends RecyclerView.Adapter<FavoriteViewHolder> {
        @NonNull
        @Override
        public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_layout, parent, false);
            return new FavoriteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
            holder.bind(favorites.get(position));
        }

        @Override
        public int getItemCount() {
            return favorites.size();
        }
    }

    private class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton imgProduct;
        private final ImageButton btnAdd;
        private final ImageButton btnFavorite;
        private final TextView tvName;
        private final TextView tvPrice;
        private final TextView tvBadge;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imageButton1);
            btnAdd = itemView.findViewById(R.id.imageButton2);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvName = itemView.findViewById(R.id.tvTen);
            tvPrice = itemView.findViewById(R.id.tvGia);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }

        void bind(LocalProduct product) {
            tvName.setText(product.getName());
            tvPrice.setText(MoneyFormatter.format(product.getBasePrice()));
            tvBadge.setText("Yêu thích");
            Glide.with(FavoritesActivity.this)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.cfplus4)
                    .error(R.drawable.cfplus4)
                    .into(imgProduct);

            btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            btnFavorite.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                String customerId = sessionManager.getCurrentUserId();
                if (customerId == null) {
                    android.widget.Toast.makeText(
                            FavoritesActivity.this,
                            "Vui lòng đăng nhập để đồng bộ món yêu thích.",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                favoriteIds.remove(product.getProductId());
                favorites.remove(position);
                adapter.notifyItemRemoved(position);
                tvEmpty.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);

                wishlistCloudRepository.setFavorite(customerId, product.getProductId(), false, (success, message) -> runOnUiThread(() -> {
                    if (!success) {
                        favoriteIds.add(product.getProductId());
                        bindFavorites();
                        android.widget.Toast.makeText(
                                FavoritesActivity.this,
                                message == null ? "Chưa đồng bộ được món yêu thích lên Firebase." : message,
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                }));
            });
            btnAdd.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, chitiet_sanpham.class);
                intent.putExtra("Ten", product.getName());
                intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
                intent.putExtra("hinhAnh", product.getImageUrl());
                intent.putExtra("productId", product.getProductId());
                startActivity(intent);
            });
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, chitiet_sanpham.class);
                intent.putExtra("Ten", product.getName());
                intent.putExtra("Gia", String.valueOf(product.getBasePrice()));
                intent.putExtra("hinhAnh", product.getImageUrl());
                intent.putExtra("productId", product.getProductId());
                startActivity(intent);
            });
        }
    }
}
