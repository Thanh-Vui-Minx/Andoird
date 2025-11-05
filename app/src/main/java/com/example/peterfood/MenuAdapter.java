package com.example.peterfood;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    private List<FoodItem> foodList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FoodItem item);
    }

    public MenuAdapter(List<FoodItem> foodList, Context context, OnItemClickListener listener) {
        this.foodList = foodList;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FoodItem item = foodList.get(position);

        // === HIỂN THỊ TÊN ===
        if (item.isCombo()) {
            holder.tvName.setText("COMBO " + item.getName());
        } else {
            holder.tvName.setText(item.getName());
        }

        // === MÔ TẢ ===
        holder.tvDescription.setText(item.getDescription());

        // === GIÁ & KHUYẾN MÃI ===
        if (item.isCombo()) {
            int comboPrice = item.getPrice();           // Giá đã giảm
            int originalPrice = item.getSalePrice();    // Giá gốc
            int savings = originalPrice - comboPrice;

            // Giá combo
            holder.tvPrice.setText(comboPrice + " VNĐ");
            // Gạch ngang giá gốc
            holder.tvPrice.setPaintFlags(holder.tvPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Tiết kiệm
            holder.tvSavings.setText("Tiết kiệm " + savings + " VNĐ");
            holder.tvSavings.setVisibility(View.VISIBLE);

            // Badge COMBO
            holder.tvComboBadge.setVisibility(View.VISIBLE);
        } else {
            // Món thường
            if (item.getSalePrice() != null && item.getSalePrice() < item.getPrice()) {
                holder.tvPrice.setText(item.getSalePrice() + " VNĐ");
                holder.tvPrice.setPaintFlags(holder.tvPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvSavings.setText("Giảm " + (item.getPrice() - item.getSalePrice()) + " VNĐ");
                holder.tvSavings.setVisibility(View.VISIBLE);
            } else {
                holder.tvPrice.setText(item.getPrice() + " VNĐ");
                holder.tvPrice.setPaintFlags(0); // Xóa gạch
                holder.tvSavings.setVisibility(View.GONE);
            }
            holder.tvComboBadge.setVisibility(View.GONE);
        }

        // === ĐÁNH GIÁ ===
        holder.tvRating.setText("Đánh giá: " + item.getRating() + "/5");

        // === ẢNH ===
        if (!item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // === CLICK ===
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    // ==================== VIEWHOLDER ĐÃ CẬP NHẬT ====================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvDescription, tvPrice, tvRating;
        TextView tvComboBadge, tvSavings;  // MỚI: Badge + tiết kiệm

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFoodImage);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvDescription = itemView.findViewById(R.id.tvFoodDescription);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvRating = itemView.findViewById(R.id.tvFoodRating);

            // THÊM MỚI: Badge và tiết kiệm
            tvComboBadge = itemView.findViewById(R.id.tvComboBadge);
            tvSavings = itemView.findViewById(R.id.tvSavings);
        }
    }
}