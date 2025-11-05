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

        // === SECTION HEADER: "COMBO GỢI Ý" HOẶC "TẤT CẢ MÓN ĂN" ===
        if ("header_combo".equals(item.getDocumentId()) || "header_food".equals(item.getDocumentId())) {
            holder.tvName.setText(item.getName());
            holder.tvName.setTextSize(20);
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvName.setTextColor(context.getResources().getColor(R.color.primary));

            holder.tvDescription.setVisibility(View.GONE);
            holder.tvPrice.setVisibility(View.GONE);
            holder.tvRating.setVisibility(View.GONE);
            holder.ivImage.setVisibility(View.GONE);
            holder.tvComboBadge.setVisibility(View.GONE);
            holder.tvSavings.setVisibility(View.GONE);
            return;
        }

        // === HIỂN THỊ BÌNH THƯỜNG: COMBO HOẶC MÓN THƯỜNG ===
        holder.tvDescription.setVisibility(View.VISIBLE);
        holder.tvPrice.setVisibility(View.VISIBLE);
        holder.tvRating.setVisibility(View.VISIBLE);
        holder.ivImage.setVisibility(View.VISIBLE);

        // === TÊN ===
        if (item.isCombo()) {
            holder.tvName.setText(" " + item.getName());
            holder.tvName.setTextColor(context.getResources().getColor(R.color.combo_text));
        } else {
            holder.tvName.setText(item.getName());
            holder.tvName.setTextColor(context.getResources().getColor(android.R.color.black));
        }

        // === MÔ TẢ ===
        holder.tvDescription.setText(item.getDescription());


        // === COMBO ===
        if (item.isCombo()) {
            int comboPrice = item.getPrice();
            int originalPrice = item.getSalePrice(); // Lưu giá gốc ở đây
            int savings = originalPrice - comboPrice;

            holder.tvPrice.setText(comboPrice + " VNĐ");
            holder.tvPrice.setPaintFlags(0); // KHÔNG GẠCH GIÁ COMBO

            holder.tvSavings.setText("Giá gốc: " + originalPrice + " VNĐ");
            holder.tvSavings.setPaintFlags(holder.tvSavings.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvSavings.setVisibility(View.VISIBLE);

            holder.tvComboBadge.setVisibility(View.VISIBLE);
        } else {
            // MÓN THƯỜNG: GIÁ GIẢM → GẠCH GIÁ GỐC
            if (item.getSalePrice() != null && item.getSalePrice() < item.getPrice()) {
                holder.tvPrice.setText(item.getSalePrice() + " VNĐ");
                holder.tvPrice.setPaintFlags(0);

                holder.tvSavings.setText("Giá gốc: " + item.getPrice() + " VNĐ");
                holder.tvSavings.setPaintFlags(holder.tvSavings.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvSavings.setVisibility(View.VISIBLE);
            } else {
                holder.tvPrice.setText(item.getPrice() + " VNĐ");
                holder.tvPrice.setPaintFlags(0);
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
        TextView tvComboBadge, tvSavings;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFoodImage);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvDescription = itemView.findViewById(R.id.tvFoodDescription);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvRating = itemView.findViewById(R.id.tvFoodRating);
            tvComboBadge = itemView.findViewById(R.id.tvComboBadge);
            tvSavings = itemView.findViewById(R.id.tvSavings);
        }
    }
}