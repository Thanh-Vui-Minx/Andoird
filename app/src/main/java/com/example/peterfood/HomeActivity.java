package com.example.peterfood;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends BaseActivity {

    private static final String TAG = "HomeActivity";
    private TextView tvWelcome;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        loadLogo(); // Tải logo từ BaseActivity

        tvWelcome = findViewById(R.id.tvWelcome);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo nút Xem giỏ hàng
        Button btnCart = findViewById(R.id.btnCart);
        if (btnCart == null) {
            Log.e(TAG, "Button btnCart is null");
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Xem giỏ hàng", Toast.LENGTH_SHORT).show();
        } else {
            btnCart.setOnClickListener(v -> {
                Log.d(TAG, "Button Xem giỏ hàng clicked");
                Intent intent = new Intent(HomeActivity.this, CartActivity.class);
                startActivity(intent);
            });
        }

        // Lấy username từ SharedPreferences
        String username = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getString("username", null);

        if (username != null && !username.isEmpty()) {
            tvWelcome.setText("Xin chào, " + username);
            Log.d(TAG, "Hiển thị username từ SharedPreferences: " + username);
        } else {
            Log.w(TAG, "No username found in SharedPreferences, fetching from Firestore");
            // Lấy username từ Firestore
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                db.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String displayUsername = documentSnapshot.getString("username");
                            if (displayUsername != null && !displayUsername.isEmpty()) {
                                tvWelcome.setText("Xin chào, " + displayUsername);
                                // Lưu lại vào SharedPreferences
                                getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("username", displayUsername)
                                        .apply();
                                Log.d(TAG, "Hiển thị username từ Firestore: " + displayUsername);
                            } else {
                                tvWelcome.setText("Xin chào, User");
                                Log.w(TAG, "No username found in Firestore for user: " + user.getUid());
                            }
                        })
                        .addOnFailureListener(e -> {
                            tvWelcome.setText("Xin chào, User");
                            Log.e(TAG, "Lỗi lấy username từ Firestore: " + e.getMessage(), e);
                        });
            } else {
                tvWelcome.setText("Xin chào, User");
                Log.e(TAG, "No user logged in");
            }
        }

        // Xử lý nút chuyển sang MenuActivity
        Button btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu == null) {
            Log.e(TAG, "Button btnMenu is null");
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Menu", Toast.LENGTH_SHORT).show();
        } else {
            btnMenu.setOnClickListener(v -> {
                Log.d(TAG, "Button Menu clicked");
                Intent intent = new Intent(HomeActivity.this, MenuActivity.class);
                startActivity(intent);
            });
        }
    }

    // Xử lý nút đăng xuất
    public void onLogoutClick(View view) {
        Log.d(TAG, "Button Đăng xuất clicked");
        mAuth.signOut();
        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}