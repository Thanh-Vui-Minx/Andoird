package com.example.peterfood;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OnOrderClick {
        void onClick(Order order);
    }

    private final List<Order> orders;
    private final OnOrderClick listener;

    public OrderAdapter(List<Order> orders, OnOrderClick listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order o = orders.get(position);
        holder.tvId.setText("Đơn #" + o.getId());
        DateFormat df = DateFormat.getDateTimeInstance();
        holder.tvDate.setText(df.format(o.getCreatedAt()));
        holder.tvSummary.setText(o.getItems().size() + " mặt hàng — Tổng: " + o.getTotal() + " VNĐ");
        holder.tvStatus.setText("Trạng thái: " + o.getPaymentStatus());
        holder.itemView.setOnClickListener(v -> listener.onClick(o));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvDate, tvSummary, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvOrderId);
            tvDate = itemView.findViewById(R.id.tvOrderDate);
            tvSummary = itemView.findViewById(R.id.tvOrderSummary);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
        }
    }
}
