package com.example.peterfood;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.function.Consumer;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private final List<CartItem> cartItems;
    private final Context context;
    private final Consumer<Integer> removeListener;

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
        CartItem currentItem = cartItems.get(position); // ĐỔI TÊN BIẾN

        // Load hình
        if (holder.ivImage != null) {
            String imageUrl = currentItem.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(holder.ivImage);
            } else {
                holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        if (holder.tvName != null) holder.tvName.setText(currentItem.getName());
        if (holder.tvPrice != null) holder.tvPrice.setText(currentItem.getPrice() + " VNĐ");
        if (holder.tvQuantity != null) holder.tvQuantity.setText(String.valueOf(currentItem.getQuantity()));
        if (holder.tvTotal != null) holder.tvTotal.setText(currentItem.getTotalPrice() + " VNĐ");

        // GHI CHÚ
        if (holder.tvNote != null) {
            if (!TextUtils.isEmpty(currentItem.getNote())) {
                holder.tvNote.setText("Ghi chú: " + currentItem.getNote());
                holder.tvNote.setVisibility(View.VISIBLE);
            } else {
                holder.tvNote.setVisibility(View.GONE);
            }
        }

        // NÚT TĂNG
        if (holder.btnPlus != null) {
            holder.btnPlus.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    CartItem item = cartItems.get(pos);
                    item.setQuantity(item.getQuantity() + 1);
                    notifyItemChanged(pos);
                    if (context instanceof CartActivity) {
                        ((CartActivity) context).updateTotalPriceAndSave();
                    }
                }
            });
        }

        // NÚT GIẢM
        if (holder.btnMinus != null) {
            holder.btnMinus.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    CartItem item = cartItems.get(pos);
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        notifyItemChanged(pos);
                        if (context instanceof CartActivity) {
                            ((CartActivity) context).updateTotalPriceAndSave();
                        }
                    }
                }
            });
        }

        // NÚT XÓA
        if (holder.btnRemove != null) {
            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    cartItems.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, cartItems.size());
                    removeListener.accept(pos);
                    if (context instanceof CartActivity) {
                        ((CartActivity) context).updateTotalPriceAndSave();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvQuantity, tvTotal, tvNote;
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
            tvNote = itemView.findViewById(R.id.tvNote);
        }
    }
}