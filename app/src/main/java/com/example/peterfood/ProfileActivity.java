package com.example.peterfood;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private EditText etEmail, etUsername, etFullname, etPassword, etPhone, etAddress;
    private Button btnSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etFullname = findViewById(R.id.etFullname);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);

        if (currentUser != null) {
            loadUserData();
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thông tin", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSave.setOnClickListener(v -> updateUserProfile());
    }

    private void loadUserData() {
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etEmail.setText(documentSnapshot.getString("email"));
                        etUsername.setText(documentSnapshot.getString("username"));
                        etFullname.setText(documentSnapshot.getString("fullname"));
                        etPhone.setText(documentSnapshot.getString("phone"));
                        etAddress.setText(documentSnapshot.getString("address"));
                        // Mật khẩu không hiển thị trực tiếp, giữ nguyên giá trị cũ nếu không thay đổi
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải dữ liệu: " + e.getMessage());
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserProfile() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String fullname = etFullname.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) || TextUtils.isEmpty(fullname) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("username", username);
        userData.put("fullname", fullname);
        userData.put("phone", phone);
        userData.put("address", address);
        if (!password.isEmpty()) {
            // Cập nhật mật khẩu (cần hash trước khi lưu, ví dụ dùng Firebase Authentication)
            currentUser.updatePassword(password)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Mật khẩu đã được cập nhật"))
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        db.collection("users").document(currentUser.getUid())
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thông tin đã được cập nhật", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật thông tin: " + e.getMessage());
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}