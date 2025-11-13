package com.example.peterfood;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ComboAdapter extends RecyclerView.Adapter<ComboAdapter.ViewHolder> {

    private List<ComboItem> comboList;
    private Context context;
    private OnComboClickListener listener;

    public interface OnComboClickListener {
        void onComboClick(ComboItem combo);
    }

    public ComboAdapter(List<ComboItem> comboList, Context context, OnComboClickListener listener) {
        this.comboList = comboList;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_combo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ComboItem combo = comboList.get(position);

        // Tên combo
        holder.tvComboName.setText(combo.getName());
        
        // Mô tả
        holder.tvComboDescription.setText(combo.getDescription());

        // Giá combo
        holder.tvComboPrice.setText(String.format("%,d VNĐ", combo.getComboPrice()));
        
        // Giá gốc (gạch ngang)
        holder.tvOriginalPrice.setText(String.format("%,d VNĐ", combo.getOriginalPrice()));
        holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        
        // Số tiền tiết kiệm
        int savings = combo.getSavings();
        int savingsPercent = (int) ((savings * 100.0) / combo.getOriginalPrice());
        holder.tvSavingsAmount.setText(String.format("💰 Tiết kiệm %,d VNĐ", savings));
        
        // Badge tiết kiệm %
        holder.tvSavingsBadge.setText(String.format("Tiết kiệm %d%%", savingsPercent));

        // Limited time badge & countdown
        if (combo.isLimitedTime()) {
            holder.tvLimitedBadge.setVisibility(View.VISIBLE);
            
            if (combo.isValid()) {
                holder.llCountdown.setVisibility(View.VISIBLE);
                holder.tvCountdown.setText(combo.getFormattedRemainingTime());
            } else {
                holder.llCountdown.setVisibility(View.GONE);
            }
        } else {
            holder.tvLimitedBadge.setVisibility(View.GONE);
            holder.llCountdown.setVisibility(View.GONE);
        }

        // Ảnh combo
        if (combo.getImageUrl() != null && !combo.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(combo.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivComboImage);
        } else {
            holder.ivComboImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onComboClick(combo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return comboList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivComboImage;
        TextView tvComboName, tvComboDescription;
        TextView tvComboPrice, tvOriginalPrice, tvSavingsAmount;
        TextView tvLimitedBadge, tvSavingsBadge, tvCountdown;
        LinearLayout llCountdown;

        public ViewHolder(View itemView) {
            super(itemView);
            ivComboImage = itemView.findViewById(R.id.ivComboImage);
            tvComboName = itemView.findViewById(R.id.tvComboName);
            tvComboDescription = itemView.findViewById(R.id.tvComboDescription);
            tvComboPrice = itemView.findViewById(R.id.tvComboPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvSavingsAmount = itemView.findViewById(R.id.tvSavingsAmount);
            tvLimitedBadge = itemView.findViewById(R.id.tvLimitedBadge);
            tvSavingsBadge = itemView.findViewById(R.id.tvSavingsBadge);
            tvCountdown = itemView.findViewById(R.id.tvCountdown);
            llCountdown = itemView.findViewById(R.id.llCountdown);
        }
    }
}
