package com.example.peterfood;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends BaseActivity { // ĐỔI TỪ AppCompatActivity
    private static final String TAG = "ProfileActivity";
    private TextView tvEmail, tvUsername, tvFullname, tvPhone;
    private LinearLayout containerAddresses;
    private Button btnEdit, btnSave, btnCancel, btnAddAddress, btnBackToMenu, btnOrderHistory, btnVouchers;

    private EditText etUsername, etFullname, etPhone;
    private List<EditText> addressEditTexts = new ArrayList<>();
    private List<String> addressList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // ĐẢM BẢO ĐÚNG FILE

        loadLogo();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "onCreate: Layout đã được set");
        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        Log.d(TAG, "initViews: Bắt đầu tìm view...");

        tvEmail = findViewById(R.id.tvEmail);
        tvUsername = findViewById(R.id.tvUsername);
        tvFullname = findViewById(R.id.tvFullname);
        tvPhone = findViewById(R.id.tvPhone);
        containerAddresses = findViewById(R.id.containerAddresses);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    btnAddAddress = findViewById(R.id.btnAddAddress);
    btnBackToMenu = findViewById(R.id.btnBackToMenu);
    btnOrderHistory = findViewById(R.id.btnOrderHistory);
    btnVouchers = findViewById(R.id.btnVouchers);

        Log.d(TAG, "btnBackToMenu: " + btnBackToMenu);
        Log.d(TAG, "btnEdit: " + btnEdit);
    }

    private void setupClickListeners() {
        if (btnEdit != null) btnEdit.setOnClickListener(v -> enterEditMode());
        if (btnSave != null) btnSave.setOnClickListener(v -> saveProfile());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> exitEditMode(false));
        if (btnAddAddress != null) btnAddAddress.setOnClickListener(v -> addAddressField());

        if (btnBackToMenu != null) {
            Log.d(TAG, "btnBackToMenu: Đã gán sự kiện!");
            btnBackToMenu.setOnClickListener(v -> {
                Log.d(TAG, "NÚT BACK TO MENU ĐƯỢC NHẤN!");
                startActivity(new Intent(ProfileActivity.this, MenuActivity.class));
                finish();
            });
        } else {
            Log.e(TAG, "btnBackToMenu is NULL! Không thể gán sự kiện!");
        }

        if (btnOrderHistory != null) {
            btnOrderHistory.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, OrderHistoryActivity.class));
            });
        }

        if (btnVouchers != null) {
            btnVouchers.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, VoucherActivity.class)));
        }
    }

    private void loadUserData() {
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvEmail.setText(documentSnapshot.getString("email") != null ?
                                documentSnapshot.getString("email") : currentUser.getEmail());
                        tvUsername.setText(documentSnapshot.getString("username") != null ?
                                documentSnapshot.getString("username") : "Chưa đặt");
                        tvFullname.setText(documentSnapshot.getString("fullname") != null ?
                                documentSnapshot.getString("fullname") : "Chưa đặt");
                        tvPhone.setText(documentSnapshot.getString("phone") != null ?
                                documentSnapshot.getString("phone") : "Chưa có");

                        // Địa chỉ: mảng
                        List<String> addresses = (List<String>) documentSnapshot.get("addresses");
                        addressList.clear();
                        if (addresses != null && !addresses.isEmpty()) {
                            addressList.addAll(addresses);
                        } else {
                            addressList.add("Chưa có địa chỉ");
                        }
                        displayAddresses();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, e.getMessage());
                });
    }

    private void displayAddresses() {
        containerAddresses.removeAllViews();
        for (int i = 0; i < addressList.size(); i++) {
            String addr = addressList.get(i);

            if (isEditing) {
                // Chế độ sửa: EditText + Xóa
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                row.setPadding(0, 4, 0, 4);

                EditText etAddr = new EditText(this);
                etAddr.setText(addr);
                etAddr.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                addressEditTexts.add(etAddr);
                row.addView(etAddr);

                Button btnRemove = new Button(this);
                btnRemove.setText("Xóa");
                btnRemove.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
                btnRemove.setTextColor(getColor(android.R.color.white));
                int finalI = i;
                btnRemove.setOnClickListener(v -> {
                    addressList.remove(finalI);
                    containerAddresses.removeView(row);
                    addressEditTexts.remove(etAddr);
                });
                row.addView(btnRemove);

                containerAddresses.addView(row);
            } else {
                // Chế độ xem
                TextView tvAddr = new TextView(this);
                tvAddr.setText("• " + addr);
                tvAddr.setTextColor(getColor(android.R.color.black));
                tvAddr.setPadding(0, 8, 0, 8);
                containerAddresses.addView(tvAddr);
            }
        }
        btnAddAddress.setVisibility(isEditing ? View.VISIBLE : View.GONE);
    }

    private void enterEditMode() {
        isEditing = true;
        btnEdit.setVisibility(View.GONE);
        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);

        // Chuyển TextView → EditText
        replaceWithEditText(tvUsername, etUsername = new EditText(this));
        replaceWithEditText(tvFullname, etFullname = new EditText(this));
        replaceWithEditText(tvPhone, etPhone = new EditText(this));

        displayAddresses();
    }

    private void replaceWithEditText(TextView tv, EditText et) {
        LinearLayout parent = (LinearLayout) tv.getParent();
        int index = parent.indexOfChild(tv);
        et.setText(tv.getText());
        et.setLayoutParams(tv.getLayoutParams());
        parent.removeView(tv);
        parent.addView(et, index);
    }

    private void addAddressField() {
        if (addressList.size() >= 2) {
            Toast.makeText(this, "Chỉ được thêm tối đa 2 địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }
        addressList.add("");
        displayAddresses();
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String fullname = etFullname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(fullname) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> newAddresses = new ArrayList<>();
        for (EditText et : addressEditTexts) {
            String addr = et.getText().toString().trim();
            if (!TextUtils.isEmpty(addr)) {
                newAddresses.add(addr);
            }
        }
        if (newAddresses.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ít nhất 1 địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("fullname", fullname);
        data.put("phone", phone);
        data.put("addresses", newAddresses);

        db.collection("users").document(currentUser.getUid())
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    exitEditMode(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void exitEditMode(boolean reload) {
        isEditing = false;
        btnEdit.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        addressEditTexts.clear();

        if (reload) {
            loadUserData();
        } else {
            displayAddresses();
        }
    }
}