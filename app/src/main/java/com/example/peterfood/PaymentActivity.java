package com.example.peterfood;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        TextView tvTotal = findViewById(R.id.tvTotal);
        Button btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        Button btnBack = findViewById(R.id.btnBack);

        // Nhận tổng tiền từ CartActivity (nếu truyền)
        int total = getIntent().getIntExtra("totalAmount", 0);
        tvTotal.setText("Tổng tiền: " + total + " VNĐ");

        btnConfirmPayment.setOnClickListener(v -> {
            Toast.makeText(this, "Thanh toán thành công! Tổng: " + total + " VNĐ", Toast.LENGTH_LONG).show();
            CartManager.getInstance().clearCart(); // Xóa giỏ sau thanh toán
            finish();
        });

        btnBack.setOnClickListener(v -> finish());
    }
}