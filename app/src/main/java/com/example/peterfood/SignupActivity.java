package com.example.peterfood;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends BaseActivity {

    private static final String TAG = "SignupActivity";
    private EditText etNewUsername, etNewPassword, etSDT;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        loadLogo();
        etNewUsername = findViewById(R.id.etNewUsername);
        etNewPassword = findViewById(R.id.etNewPassword);
        etSDT = findViewById(R.id.etSDT);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (etSDT == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy trường số điện thoại", Toast.LENGTH_LONG).show();
            Log.e(TAG, "etSDT is null");
        }
    }

    public void onSignUpClick(View view) {
        String username = etNewUsername.getText().toString().trim();
        String password = etNewPassword.getText().toString().trim();
        String sdt = etSDT.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || sdt.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!sdt.matches("\\d{10}")) {
            Toast.makeText(this, "Số điện thoại phải có đúng 10 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo email giả
        String email = username + "@peterfood.com";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Lưu thông tin vào Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", username);
                            userData.put("email", email);
                            userData.put("sdt", sdt);
                            userData.put("role", "user"); // Mặc định là user

                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Lưu thông tin user thành công: " + username);
                                        // Lưu role và username vào SharedPreferences
                                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                                .edit()
                                                .putString("role", "user")
                                                .putString("username", username)
                                                .apply();
                                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Lỗi lưu thông tin: " + e.getMessage(), e);
                                        Toast.makeText(this, "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.e(TAG, "Lỗi đăng ký: " + task.getException().getMessage());
                        Toast.makeText(this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void goBackToLogin(View view) {
        startActivity(new Intent(SignupActivity.this, MainActivity.class));
        finish();
    }
}