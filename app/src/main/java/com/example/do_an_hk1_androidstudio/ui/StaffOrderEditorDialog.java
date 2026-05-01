package com.example.do_an_hk1_androidstudio.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.R;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StaffOrderEditorDialog {

    public interface SaveCallback {
        void onSave(@NonNull List<LocalOrderItem> updatedItems);
    }

    private StaffOrderEditorDialog() {
    }

    public static void show(@NonNull Context context,
                            @NonNull String orderLabel,
                            @NonNull List<LocalOrderItem> sourceItems,
                            @NonNull List<LocalProduct> products,
                            boolean allowProductChange,
                            @NonNull SaveCallback callback) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_staff_edit_order, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tvEditOrderTitle);
        TextView tvSummary = dialogView.findViewById(R.id.tvEditOrderSummary);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEditOrderEmpty);
        TextView tvSave = dialogView.findViewById(R.id.btnSaveEditedOrder);
        RecyclerView rvItems = dialogView.findViewById(R.id.rvEditOrderItems);
        rvItems.setLayoutManager(new LinearLayoutManager(context));

        List<EditableItem> editableItems = cloneItems(sourceItems);
        EditOrderAdapter adapter = new EditOrderAdapter(context, editableItems, products, allowProductChange, () -> {
            tvSummary.setText(buildSummary(editableItems));
            tvEmpty.setVisibility(editableItems.isEmpty() ? View.VISIBLE : View.GONE);
        });
        rvItems.setAdapter(adapter);

        tvTitle.setText("Sửa đơn " + orderLabel);
        tvSummary.setText(buildSummary(editableItems));
        tvEmpty.setVisibility(editableItems.isEmpty() ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancelEditedOrder).setOnClickListener(v -> dialog.dismiss());
        tvSave.setOnClickListener(v -> {
            callback.onSave(toOrderItems(editableItems));
            dialog.dismiss();
        });
        dialog.show();
    }

    @NonNull
    private static String buildSummary(@NonNull List<EditableItem> items) {
        int totalQty = 0;
        int totalAmount = 0;
        for (EditableItem item : items) {
            totalQty += item.qty;
            totalAmount += item.qty * item.unitPrice;
        }
        return totalQty + " món • " + MoneyFormatter.format(totalAmount);
    }

    @NonNull
    private static List<EditableItem> cloneItems(@NonNull List<LocalOrderItem> sourceItems) {
        List<EditableItem> result = new ArrayList<>();
        for (LocalOrderItem item : sourceItems) {
            result.add(new EditableItem(item));
        }
        return result;
    }

    @NonNull
    private static List<LocalOrderItem> toOrderItems(@NonNull List<EditableItem> editableItems) {
        List<LocalOrderItem> result = new ArrayList<>();
        for (EditableItem item : editableItems) {
            result.add(new LocalOrderItem(
                    item.itemId,
                    item.orderId,
                    item.productId,
                    item.productName,
                    item.variantName,
                    item.qty,
                    item.unitPrice,
                    item.note,
                    item.qty * item.unitPrice,
                    item.imageUrl
            ));
        }
        return result;
    }

    private static final class EditableItem {
        private final String itemId;
        private final String orderId;
        private String productId;
        private String productName;
        private String variantName;
        private int qty;
        private int unitPrice;
        private String note;
        private String imageUrl;

        private EditableItem(@NonNull LocalOrderItem item) {
            this.itemId = item.getItemId();
            this.orderId = item.getOrderId();
            this.productId = item.getProductId();
            this.productName = item.getProductName();
            this.variantName = item.getVariantName();
            this.qty = Math.max(1, item.getQty());
            this.unitPrice = Math.max(0, item.getUnitPrice());
            this.note = item.getNote();
            this.imageUrl = item.getImageUrl();
        }
    }

    private static final class EditOrderAdapter extends RecyclerView.Adapter<EditOrderAdapter.EditOrderViewHolder> {
        private final Context context;
        private final List<EditableItem> items;
        private final List<LocalProduct> products;
        private final boolean allowProductChange;
        private final Runnable onChanged;

        private EditOrderAdapter(@NonNull Context context,
                                 @NonNull List<EditableItem> items,
                                 @NonNull List<LocalProduct> products,
                                 boolean allowProductChange,
                                 @NonNull Runnable onChanged) {
            this.context = context;
            this.items = items;
            this.products = products;
            this.allowProductChange = allowProductChange;
            this.onChanged = onChanged;
        }

        @NonNull
        @Override
        public EditOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff_edit_order_line, parent, false);
            return new EditOrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EditOrderViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private final class EditOrderViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imgProduct;
            private final TextView tvName;
            private final TextView tvMeta;
            private final TextView tvQty;
            private final TextView tvLineTotal;
            private final TextView btnMinus;
            private final TextView btnPlus;
            private final TextView btnChangeProduct;
            private final TextView btnEditNote;
            private final TextView btnDelete;

            private EditOrderViewHolder(@NonNull View itemView) {
                super(itemView);
                imgProduct = itemView.findViewById(R.id.imgEditOrderProduct);
                tvName = itemView.findViewById(R.id.tvEditOrderProductName);
                tvMeta = itemView.findViewById(R.id.tvEditOrderMeta);
                tvQty = itemView.findViewById(R.id.tvEditOrderQty);
                tvLineTotal = itemView.findViewById(R.id.tvEditOrderLineTotal);
                btnMinus = itemView.findViewById(R.id.btnEditOrderMinus);
                btnPlus = itemView.findViewById(R.id.btnEditOrderPlus);
                btnChangeProduct = itemView.findViewById(R.id.btnEditOrderChangeProduct);
                btnEditNote = itemView.findViewById(R.id.btnEditOrderNote);
                btnDelete = itemView.findViewById(R.id.btnEditOrderDelete);
            }

            private void bind(@NonNull EditableItem item) {
                tvName.setText(item.productName);
                tvQty.setText(String.valueOf(item.qty));
                tvLineTotal.setText(MoneyFormatter.format(item.qty * item.unitPrice));
                tvMeta.setText(buildMeta(item));

                if (TextUtils.isEmpty(item.imageUrl)) {
                    String fallbackUrl = findProductImageUrl(item.productId, item.productName);
                    if (!TextUtils.isEmpty(fallbackUrl)) {
                        item.imageUrl = fallbackUrl;
                    }
                }

                CfPlusImageLoader.load(imgProduct, item.imageUrl, R.drawable.cfplus4);

                btnMinus.setOnClickListener(v -> {
                    item.qty = Math.max(1, item.qty - 1);
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        notifyItemChanged(position);
                    }
                    onChanged.run();
                });

                btnPlus.setOnClickListener(v -> {
                    item.qty += 1;
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        notifyItemChanged(position);
                    }
                    onChanged.run();
                });

                btnDelete.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        return;
                    }
                    items.remove(position);
                    notifyItemRemoved(position);
                    onChanged.run();
                });

                btnEditNote.setOnClickListener(v -> showNoteEditor(item));
                btnChangeProduct.setVisibility(allowProductChange ? View.VISIBLE : View.GONE);
                btnChangeProduct.setOnClickListener(v -> {
                    if (allowProductChange) {
                        showProductPicker(item);
                    }
                });
            }

            private void showNoteEditor(@NonNull EditableItem item) {
                final EditText input = new EditText(context);
                input.setHint("Nhập ghi chú cho món");
                input.setText(item.note == null ? "" : item.note);
                new AlertDialog.Builder(context)
                        .setTitle("Sửa ghi chú")
                        .setView(input)
                        .setNegativeButton("Huỷ", null)
                        .setPositiveButton("Lưu", (dialog, which) -> {
                            item.note = input.getText().toString().trim();
                            int position = getBindingAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                notifyItemChanged(position);
                            }
                            onChanged.run();
                        })
                        .show();
            }

            private void showProductPicker(@NonNull EditableItem item) {
                List<LocalProduct> activeProducts = new ArrayList<>();
                for (LocalProduct product : products) {
                    if (product.isActive()) {
                        activeProducts.add(product);
                    }
                }
                if (activeProducts.isEmpty()) {
                    Toast.makeText(context, "Chưa có món khả dụng để thay đổi.", Toast.LENGTH_SHORT).show();
                    return;
                }

                View pickerView = LayoutInflater.from(context).inflate(R.layout.dialog_staff_product_picker, null, false);
                EditText edtSearch = pickerView.findViewById(R.id.edtSearchProduct);
                TextView tvEmpty = pickerView.findViewById(R.id.tvPickerEmpty);
                RecyclerView rvProducts = pickerView.findViewById(R.id.rvPickProducts);
                rvProducts.setLayoutManager(new LinearLayoutManager(context));

                ProductPickerAdapter adapter = new ProductPickerAdapter(activeProducts, selected -> {
                    item.productId = selected.getProductId();
                    item.productName = selected.getName();
                    item.unitPrice = selected.getBasePrice();
                    item.imageUrl = selected.getImageUrl();
                    item.variantName = null;
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        notifyItemChanged(position);
                    }
                    onChanged.run();
                });
                rvProducts.setAdapter(adapter);
                tvEmpty.setVisibility(activeProducts.isEmpty() ? View.VISIBLE : View.GONE);

                AlertDialog pickerDialog = new AlertDialog.Builder(context)
                        .setView(pickerView)
                        .create();
                Window window = pickerDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawableResource(android.R.color.transparent);
                }

                edtSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        adapter.filter(s == null ? "" : s.toString());
                        tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                pickerView.findViewById(R.id.btnCloseProductPicker).setOnClickListener(v -> pickerDialog.dismiss());
                adapter.setOnProductPicked(selected -> {
                    item.productId = selected.getProductId();
                    item.productName = selected.getName();
                    item.unitPrice = selected.getBasePrice();
                    item.imageUrl = selected.getImageUrl();
                    item.variantName = null;
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        notifyItemChanged(position);
                    }
                    onChanged.run();
                    pickerDialog.dismiss();
                });
                pickerDialog.show();
            }

            @Nullable
            private String findProductImageUrl(@Nullable String productId, @Nullable String productName) {
                for (LocalProduct product : products) {
                    boolean matchId = !TextUtils.isEmpty(productId) && productId.equals(product.getProductId());
                    boolean matchName = !TextUtils.isEmpty(productName)
                            && productName.trim().equalsIgnoreCase(product.getName() == null ? "" : product.getName().trim());
                    if ((matchId || matchName) && !TextUtils.isEmpty(product.getImageUrl())) {
                        return product.getImageUrl();
                    }
                }
                return null;
            }

            @NonNull
            private String buildMeta(@NonNull EditableItem item) {
                StringBuilder builder = new StringBuilder();
                builder.append(MoneyFormatter.format(item.unitPrice));
                if (!TextUtils.isEmpty(item.variantName)) {
                    builder.append(" • ").append(item.variantName);
                }
                if (!TextUtils.isEmpty(item.note)) {
                    builder.append("\nGhi chú: ").append(item.note);
                }
                return builder.toString();
            }
        }
    }

    private static final class ProductPickerAdapter extends RecyclerView.Adapter<ProductPickerAdapter.ProductViewHolder> {
        interface PickListener {
            void onPicked(@NonNull LocalProduct product);
        }

        private final List<LocalProduct> allProducts;
        private final List<LocalProduct> visibleProducts = new ArrayList<>();
        private PickListener onProductPicked;

        private ProductPickerAdapter(@NonNull List<LocalProduct> products, @NonNull PickListener listener) {
            this.allProducts = new ArrayList<>(products);
            this.visibleProducts.addAll(products);
            this.onProductPicked = listener;
        }

        void setOnProductPicked(@NonNull PickListener listener) {
            this.onProductPicked = listener;
        }

        void filter(@NonNull String keyword) {
            visibleProducts.clear();
            String normalized = keyword.trim().toLowerCase(Locale.getDefault());
            for (LocalProduct product : allProducts) {
                String name = product.getName() == null ? "" : product.getName().toLowerCase(Locale.getDefault());
                if (normalized.isEmpty() || name.contains(normalized)) {
                    visibleProducts.add(product);
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff_product_pick, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            holder.bind(visibleProducts.get(position));
        }

        @Override
        public int getItemCount() {
            return visibleProducts.size();
        }

        private final class ProductViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imgProduct;
            private final TextView tvName;
            private final TextView tvPrice;

            private ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                imgProduct = itemView.findViewById(R.id.imgPickProduct);
                tvName = itemView.findViewById(R.id.tvPickProductName);
                tvPrice = itemView.findViewById(R.id.tvPickProductPrice);
            }

            private void bind(@NonNull LocalProduct product) {
                tvName.setText(product.getName());
                tvPrice.setText(MoneyFormatter.format(product.getBasePrice()));
                CfPlusImageLoader.load(imgProduct, product.getImageUrl(), R.drawable.cfplus4);
                itemView.setOnClickListener(v -> {
                    if (onProductPicked != null) {
                        onProductPicked.onPicked(product);
                    }
                });
            }
        }
    }
}
