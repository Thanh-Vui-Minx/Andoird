package com.example.peterfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        Button btnMenu = findViewById(R.id.btnMenu);
        Button btnCart = findViewById(R.id.btnCart);

        if (btnMenu == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Xem menu", Toast.LENGTH_LONG).show();
            Log.e("HomeActivity", "btnMenu is null");
            return;
        }

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("HomeActivity", "Button Xem menu clicked");
                Toast.makeText(HomeActivity.this, "Đang chuyển sang Menu", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeActivity.this, MenuActivity.class);
                startActivity(intent);
            }
        });

        btnCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("HomeActivity", "Button Giỏ hàng clicked");
                Intent intent = new Intent(HomeActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });
    }

    public void onLogoutClick(View view) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}