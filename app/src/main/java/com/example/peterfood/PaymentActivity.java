package com.example.peterfood;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

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
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid())
                        .update("cart", new ArrayList<>())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Thanh toán thành công! Tổng: " + total + " VNĐ", Toast.LENGTH_LONG).show();
                            CartManager.getInstance().clearCart(); // Xóa giỏ local
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Thanh toán nhưng không thể xóa giỏ trên server: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            CartManager.getInstance().clearCart();
                            finish();
                        });
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_LONG).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }
}