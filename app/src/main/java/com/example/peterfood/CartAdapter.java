package com.example.peterfood;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

public class CartAdapter extends BaseAdapter {
    private List<CartItem> cartItems;
    private Context context;

    public CartAdapter(List<CartItem> cartItems, Context context) {
        this.cartItems = cartItems;
        this.context = context;
    }

    @Override
    public int getCount() {
        return cartItems.size();
    }

    @Override
    public Object getItem(int position) {
        return cartItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        }

        CartItem item = cartItems.get(position);
        TextView tvName = convertView.findViewById(R.id.tvCartName);
        TextView tvQuantity = convertView.findViewById(R.id.tvCartQuantity);
        TextView tvPrice = convertView.findViewById(R.id.tvCartPrice);
        Button btnRemove = convertView.findViewById(R.id.btnRemove);
        Button btnMinus = convertView.findViewById(R.id.btnMinus);
        Button btnPlus = convertView.findViewById(R.id.btnPlus);

        tvName.setText(item.getName());
        tvQuantity.setText("Số lượng: " + item.getQuantity());
        tvPrice.setText("Giá: " + (item.getPrice() * item.getQuantity()) + " VNĐ");

        btnPlus.setOnClickListener(v -> {
            item.setQuantity(item.getQuantity() + 1);
            notifyDataSetChanged();
            ((CartActivity) context).updateTotal();
        });

        btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                notifyDataSetChanged();
                ((CartActivity) context).updateTotal();
            }
        });

        btnRemove.setOnClickListener(v -> {
            cartItems.remove(position);
            notifyDataSetChanged();
            ((CartActivity) context).updateTotal();
        });

        return convertView;
    }
}