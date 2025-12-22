package com.example.peterfood;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    private List<FoodItem> foodList;
    private Context context;
    private OnItemClickListener listener;
    private Set<String> favorites = new HashSet<>();
    private OnFavoriteClick favoriteClickListener;
    private boolean showFavoriteIcon = true;

    public interface OnItemClickListener {
        void onItemClick(FoodItem item);
    }

    public interface OnFavoriteClick {
        void onFavoriteClick(FoodItem item, boolean newState);
    }

    public MenuAdapter(List<FoodItem> foodList, Context context, OnItemClickListener listener) {
        this.foodList = foodList;
        this.context = context;
        this.listener = listener;
    }

    public MenuAdapter(List<FoodItem> foodList, Context context, OnItemClickListener listener, Set<String> favorites, OnFavoriteClick favoriteClickListener) {
        this.foodList = foodList;
        this.context = context;
        this.listener = listener;
        if (favorites != null) this.favorites = favorites;
        this.favoriteClickListener = favoriteClickListener;
    }

    public MenuAdapter(List<FoodItem> foodList, Context context, OnItemClickListener listener, Set<String> favorites, OnFavoriteClick favoriteClickListener, boolean showFavoriteIcon) {
        this.foodList = foodList;
        this.context = context;
        this.listener = listener;
        if (favorites != null) this.favorites = favorites;
        this.favoriteClickListener = favoriteClickListener;
        this.showFavoriteIcon = showFavoriteIcon;
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
        if ("header_combo".equals(item.getDocumentId()) || 
            "header_limited_combo".equals(item.getDocumentId()) ||
            "header_regular_combo".equals(item.getDocumentId()) ||
            "header_food".equals(item.getDocumentId()) || 
            "header_drink".equals(item.getDocumentId())) {
            holder.tvName.setText(item.getName());
            holder.tvName.setTextSize(20);
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvName.setTextColor(context.getResources().getColor(R.color.primary));

            // Show only the header label — hide item views
            holder.tvDescription.setVisibility(View.GONE);
            holder.tvPrice.setVisibility(View.GONE);
            holder.tvRating.setVisibility(View.GONE);
            holder.ivImage.setVisibility(View.GONE);
            holder.tvComboBadge.setVisibility(View.GONE);
            holder.tvSavings.setVisibility(View.GONE);

            // For food/drink headers: prefix the title with the emoji and show a short subtitle
            if ("header_food".equals(item.getDocumentId())) {
                // hide the separate combo badge and prefix the title with emoji
                holder.tvComboBadge.setVisibility(View.GONE);
                holder.tvName.setText("\uD83C\uDF54" + item.getName());
                holder.tvName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                holder.tvDescription.setVisibility(View.VISIBLE);
                holder.tvDescription.setText("Khám phá các món ăn phong phú");
            } else if ("header_drink".equals(item.getDocumentId())) {
                holder.tvComboBadge.setVisibility(View.GONE);
                holder.tvName.setText("\uD83E\uDD64" + item.getName());
                holder.tvName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                holder.tvDescription.setVisibility(View.VISIBLE);
                holder.tvDescription.setText("Các loại đồ uống mát lạnh và thơm ngon");
            } else {
                // other headers (combo sections): show specific subtitles for limited vs regular combos
                holder.tvComboBadge.setVisibility(View.GONE);
                if ("header_limited_combo".equals(item.getDocumentId())) {
                    holder.tvDescription.setVisibility(View.VISIBLE);
                    holder.tvDescription.setText("Những combo đặt biệt chỉ có ở những sự kiện đặt biệt");
                } else if ("header_regular_combo".equals(item.getDocumentId())) {
                    holder.tvDescription.setVisibility(View.VISIBLE);
                    holder.tvDescription.setText("Trải nghiệm món ăn với giá thành rẻ hơn cho bạn");
                } else {
                    holder.tvDescription.setVisibility(View.VISIBLE);
                    holder.tvDescription.setText("Khám phá các combo hấp dẫn");
                }
            }
            // Hide favorite button for header rows
            if (holder.btnFavorite != null) {
                holder.btnFavorite.setVisibility(View.GONE);
            }
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

    // Clear any header icon for normal items
    holder.tvName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

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

        // === FAVORITE ICON ===
        if (holder.btnFavorite != null) {
            if (!showFavoriteIcon) {
                holder.btnFavorite.setVisibility(View.GONE);
            } else {
                holder.btnFavorite.setVisibility(View.VISIBLE);
                boolean isFav = favorites.contains(item.getDocumentId());
                holder.btnFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                holder.btnFavorite.setOnClickListener(v -> {
                    boolean newState = !favorites.contains(item.getDocumentId());
                    if (newState) favorites.add(item.getDocumentId()); else favorites.remove(item.getDocumentId());
                    holder.btnFavorite.setImageResource(newState ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                    if (favoriteClickListener != null) favoriteClickListener.onFavoriteClick(item, newState);
                });
            }
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
        ImageButton btnFavorite;
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
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}