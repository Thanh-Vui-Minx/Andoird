package com.example.peterfood;

import android.content.Context;
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
        holder.tvName.setText(item.getName());
        holder.tvDescription.setText(item.getDescription());
        if (item.getSalePrice() != null && item.getSalePrice() < item.getPrice()) {
            holder.tvPrice.setText("Giá: " + item.getSalePrice() + " VNĐ (Gốc: " + item.getPrice() + " VNĐ)");
        } else {
            holder.tvPrice.setText("Giá: " + item.getPrice() + " VNĐ");
        }
        holder.tvRating.setText("Đánh giá: " + item.getRating() + "/5");

        if (!item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivImage);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvDescription, tvPrice, tvRating;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFoodImage);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvDescription = itemView.findViewById(R.id.tvFoodDescription);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvRating = itemView.findViewById(R.id.tvFoodRating);
        }
    }
}