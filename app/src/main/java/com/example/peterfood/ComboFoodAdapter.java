package com.example.peterfood;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class ComboFoodAdapter extends RecyclerView.Adapter<ComboFoodAdapter.ViewHolder> {
    public interface OnItemAction {
        void onAddToCart(FoodItem item, int quantity);
        void onRemoveFromCombo(FoodItem item);
    }

    private final Context ctx;
    private final List<FoodItem> items;
    private final Map<String, Integer> quantities;
    private final OnItemAction callback;
    private final String role;

    public ComboFoodAdapter(Context ctx, List<FoodItem> items, Map<String,Integer> quantities, String role, OnItemAction callback) {
        this.ctx = ctx;
        this.items = items;
        this.quantities = quantities;
        this.callback = callback;
        this.role = role;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_combo_food, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem item = items.get(position);
        holder.tvName.setText(item.getName());
        int qty = 1;
        if (quantities != null && quantities.containsKey(item.getDocumentId())) {
            Integer q = quantities.get(item.getDocumentId());
            if (q != null) qty = q;
        }
        final int finalQty = qty; // must be effectively final for lambda use
        holder.tvQuantity.setText("x" + finalQty);
        long price = item.getFinalPrice();
        holder.tvPrice.setText(String.format("%,d VNĐ", price));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(ctx).load(item.getImageUrl()).into(holder.iv);
        } else {
            holder.iv.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (callback != null) callback.onAddToCart(item, finalQty);
        });

        // Remove visible only for admin (but we keep view hidden unless role==admin)
        if ("admin".equals(role)) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> {
                if (callback != null) callback.onRemoveFromCombo(item);
            });
        } else {
            holder.btnRemove.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tvName, tvPrice, tvQuantity;
        Button btnRemove;

        ViewHolder(View v) {
            super(v);
            iv = v.findViewById(R.id.ivFoodItemImage);
            tvName = v.findViewById(R.id.tvFoodItemName);
            tvPrice = v.findViewById(R.id.tvFoodItemPrice);
            tvQuantity = v.findViewById(R.id.tvFoodItemQuantity);
            btnRemove = v.findViewById(R.id.btnRemoveFoodItem);
        }
    }
}
