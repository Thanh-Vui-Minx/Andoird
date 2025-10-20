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
import java.util.function.Consumer;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private List<CartItem> cartItems;
    private Context context;
    private Consumer<Integer> removeListener;

    public CartAdapter(List<CartItem> cartItems, Context context, Consumer<Integer> removeListener) {
        this.cartItems = cartItems;
        this.context = context;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        if (holder.ivImage != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivImage); // ✅ LOAD HÌNH
        }
        if (holder.tvName != null) holder.tvName.setText(item.getName());
        if (holder.tvPrice != null) holder.tvPrice.setText(item.getPrice() + " VNĐ");
        if (holder.tvQuantity != null) holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        if (holder.tvTotal != null) holder.tvTotal.setText(item.getTotalPrice() + " VNĐ");

        if (holder.btnPlus != null) {
            holder.btnPlus.setOnClickListener(v -> {
                item.setQuantity(item.getQuantity() + 1);
                notifyItemChanged(position);
                ((CartActivity) context).updateTotalPrice();
            });
        }

        if (holder.btnMinus != null) {
            holder.btnMinus.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    notifyItemChanged(position);
                    ((CartActivity) context).updateTotalPrice();
                }
            });
        }

        if (holder.btnRemove != null) {
            holder.btnRemove.setOnClickListener(v -> removeListener.accept(position));
        }
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage; // ✅ THÊM
        TextView tvName, tvPrice, tvQuantity, tvTotal;
        Button btnPlus, btnMinus, btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}