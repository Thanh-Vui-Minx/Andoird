package com.example.peterfood;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText etUsername, etPassword;
    private ImageView ivLogo;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String LOGO_URL = "https://drive.google.com/uc?export=download&id=1FiI3mfuagF1zQhCKkXAvtY4LbpkT9WlX"; // Link logo Google Drive

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        ivLogo = findViewById(R.id.ivLogo);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Tải logo từ Google Drive với cache
        if (ivLogo != null) {
            Log.d(TAG, "Loading logo from: " + LOGO_URL);
            Glide.with(this)
                    .load(LOGO_URL)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache ảnh gốc và resize
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Glide load failed: " + (e != null ? e.getMessage() : "Unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Glide loaded logo from: " + dataSource); // MEMORY_CACHE, DISK_CACHE, hoặc REMOTE
                            return false;
                        }
                    })
                    .into(ivLogo);
        } else {
            Log.e(TAG, "ImageView ivLogo is null");
        }

        // Kiểm tra nếu đã đăng nhập
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserDataAndStartHome(currentUser);
        }
    }

    public void onLoginClick(View view) {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo email giả từ username
        String email = username + "@peterfood.com";

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loadUserDataAndStartHome(user);
                        }
                    } else {
                        Log.e(TAG, "Lỗi đăng nhập: " + task.getException().getMessage());
                        Toast.makeText(this, "Tên người dùng hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserDataAndStartHome(FirebaseUser user) {
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    String displayUsername = documentSnapshot.getString("username");
                    if (displayUsername == null) {
                        Log.w(TAG, "No username found in Firestore for user: " + user.getUid());
                        displayUsername = "User"; // Giá trị mặc định nếu không có username
                    }
                    // Lưu role và username vào SharedPreferences
                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("role", role != null ? role : "user")
                            .putString("username", displayUsername)
                            .apply();
                    Log.d(TAG, "Lưu SharedPreferences - username: " + displayUsername + ", role: " + role);
                    startHomeActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lấy thông tin user: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi lấy thông tin user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Vẫn cho vào HomeActivity với giá trị mặc định
                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("role", "user")
                            .putString("username", "User")
                            .apply();
                    startHomeActivity();
                });
    }

    public void onSignupClick(View view) {
        Intent intent = new Intent(MainActivity.this, SignupActivity.class);
        startActivity(intent);
    }

    private void startHomeActivity() {
        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
        startActivity(intent);
        finish();
    }
}