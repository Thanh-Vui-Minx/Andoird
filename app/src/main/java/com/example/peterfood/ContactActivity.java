package com.example.peterfood;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class ContactActivity extends AppCompatActivity {

    private EditText etShopName, etShopPhone, etShopAddress, etShopEmail;
    private Button btnSave, btnBack;
    private FirebaseFirestore db;
    private String role;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        db = FirebaseFirestore.getInstance();
        role = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("role", "user");

        etShopName = findViewById(R.id.etShopName);
        etShopPhone = findViewById(R.id.etShopPhone);
        etShopAddress = findViewById(R.id.etShopAddress);
        etShopEmail = findViewById(R.id.etShopEmail);
        btnSave = findViewById(R.id.btnSaveContact);
        btnBack = findViewById(R.id.btnBackContact);

        // Only admin can save
        if (!"admin".equals(role)) {
            btnSave.setVisibility(View.GONE);
            etShopName.setEnabled(false);
            etShopPhone.setEnabled(false);
            etShopAddress.setEnabled(false);
            etShopEmail.setEnabled(false);
        }

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveShopInfo());

        loadShopInfo();
    }

    private void loadShopInfo() {
        // Read shop info from `app_config/shop_info`
        db.collection("app_config").document("shop_info").get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etShopName.setText(doc.getString("name") != null ? doc.getString("name") : "");
                        etShopPhone.setText(doc.getString("phone") != null ? doc.getString("phone") : "");
                        etShopAddress.setText(doc.getString("address") != null ? doc.getString("address") : "");
                        etShopEmail.setText(doc.getString("email") != null ? doc.getString("email") : "");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Không thể tải thông tin cửa hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveShopInfo() {
        if (!"admin".equals(role)) return;

        String name = etShopName.getText().toString().trim();
        String phone = etShopPhone.getText().toString().trim();
        String address = etShopAddress.getText().toString().trim();
        String email = etShopEmail.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(address) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("name", name);
        doc.put("phone", phone);
        doc.put("address", address);
        doc.put("email", email);
        doc.put("updatedBy", FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "");
        doc.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("app_config").document("shop_info").set(doc, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Cập nhật thông tin cửa hàng thành công", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
