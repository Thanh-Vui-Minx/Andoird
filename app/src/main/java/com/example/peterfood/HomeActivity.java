package com.example.peterfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Ánh xạ các view
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnMenu = findViewById(R.id.btnMenu);
        Button btnCart = findViewById(R.id.btnCart);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Kiểm tra các view
        if (tvWelcome == null) {
            Log.e(TAG, "TextView tvWelcome is null");
            Toast.makeText(this, "Lỗi: Không tìm thấy TextView chào mừng", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (btnMenu == null) {
            Log.e(TAG, "Button btnMenu is null");
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Xem Menu", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (btnCart == null) {
            Log.e(TAG, "Button btnCart is null");
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Giỏ Hàng", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (btnLogout == null) {
            Log.e(TAG, "Button btnLogout is null");
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Đăng Xuất", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Hiển thị câu chào với username
        String username = sharedPreferences.getString("username", "");
        if (!username.isEmpty()) {
            tvWelcome.setText("Xin chào, " + username);
            Log.d(TAG, "User logged in: " + username);
        } else {
            tvWelcome.setText("Xin chào, Khách");
            Log.w(TAG, "No username found in SharedPreferences");
            // Chuyển về MainActivity nếu không có username
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Xử lý nút Xem Menu
        btnMenu.setOnClickListener(v -> {
            Log.d(TAG, "Button Xem Menu clicked");
            Toast.makeText(this, "Đang chuyển sang Menu", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        // Xử lý nút Giỏ Hàng
        btnCart.setOnClickListener(v -> {
            Log.d(TAG, "Button Giỏ Hàng clicked");
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            startActivity(intent);
        });

        // Xử lý nút Đăng Xuất
        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Button Đăng Xuất clicked");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.remove("username");
            editor.remove("password");
            editor.apply();
            Toast.makeText(this, "Bạn đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}