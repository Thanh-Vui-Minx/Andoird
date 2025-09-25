package com.example.peterfood;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class CartActivity extends AppCompatActivity {

    private ListView lvCart;
    private TextView tvTotal;
    private List<CartItem> cartItems;
    private CartAdapter adapter;
    private RadioGroup rgDeliveryOption;
    private boolean isDelivery = false; // Mặc định pickup
    private static final int SHIP_FEE = 20000; // Phí ship
    private static final double DISCOUNT_RATE = 0.1; // 10%
    private static final int DISCOUNT_THRESHOLD = 200000; // Giảm nếu > 200k

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        lvCart = findViewById(R.id.lvCart);
        tvTotal = findViewById(R.id.tvTotal);
        Button btnCheckout = findViewById(R.id.btnCheckout);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnBack = findViewById(R.id.btnBack);
        rgDeliveryOption = findViewById(R.id.rgDeliveryOption);

        // Lấy giỏ hàng từ CartManager
        cartItems = CartManager.getInstance().getCartItems();
        adapter = new CartAdapter(cartItems, this);
        lvCart.setAdapter(adapter);

        // Xử lý thay đổi tùy chọn delivery
        rgDeliveryOption.setOnCheckedChangeListener((group, checkedId) -> {
            isDelivery = (checkedId == R.id.rbDelivery);
            updateTotal();
        });

        updateTotal();

        btnCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show();
            } else {
                // Tính total cuối cùng
                int finalTotal = calculateTotal();
                Toast.makeText(this, "Thanh toán thành công! Tổng: " + finalTotal + " VNĐ" + (isDelivery ? " (bao gồm phí ship)" : ""), Toast.LENGTH_LONG).show();
                CartManager.getInstance().clearCart();
                adapter.notifyDataSetChanged();
                updateTotal();
            }
        });

        btnClear.setOnClickListener(v -> {
            CartManager.getInstance().clearCart();
            adapter.notifyDataSetChanged();
            updateTotal();
            Toast.makeText(this, "Đã xóa giỏ hàng", Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> finish());
    }

    void updateTotal() {
        tvTotal.setText("Tổng tiền: " + calculateTotal() + " VNĐ");
    }

    private int calculateTotal() {
        int subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getPrice() * item.getQuantity();
        }

        // Áp dụng khuyến mãi
        int discount = 0;
        if (subtotal > DISCOUNT_THRESHOLD) {
            discount = (int) (subtotal * DISCOUNT_RATE);
        }
        subtotal -= discount;

        // Thêm phí ship nếu delivery
        if (isDelivery) {
            subtotal += SHIP_FEE;
        }

        return subtotal;
    }
}