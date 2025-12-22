package com.example.peterfood;

import android.annotation.SuppressLint;
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
import androidx.appcompat.widget.Toolbar;

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
    private Button btnEdit, btnSave, btnCancel, btnAddAddress, btnBackToMenu, btnOrderHistory, btnVouchers, btnViewFavorites;

    private EditText etUsername, etFullname, etPhone;
    private List<EditText> addressEditTexts = new ArrayList<>();
    private List<String> addressList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private boolean isEditing = false;
    private androidx.appcompat.widget.Toolbar toolbarProfile;
    private androidx.recyclerview.widget.RecyclerView rvFavorites;
    private MenuAdapter favoritesAdapter;
    private java.util.Set<String> favoritesSet = new java.util.HashSet<>();

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

    @SuppressLint("SuspiciousIndentation")
    private void initViews() {
        Log.d(TAG, "initViews: Bắt đầu tìm view...");

        this.toolbarProfile = (Toolbar) findViewById(R.id.toolbarProfile);
        if (this.toolbarProfile != null) {
            this.toolbarProfile.setNavigationOnClickListener(v -> {
                if (isEditing) {
                    // act as cancel: discard unsaved edits and reload server data
                    exitEditMode(true);
                } else {
                    finish();
                }
            });
            // ensure initial nav icon is back
            this.toolbarProfile.setNavigationIcon(android.R.drawable.ic_menu_revert);
        }

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
        rvFavorites = findViewById(R.id.rvFavorites);
        btnViewFavorites = findViewById(R.id.btnViewFavorites);

        Log.d(TAG, "btnBackToMenu: " + btnBackToMenu);
        Log.d(TAG, "btnEdit: " + btnEdit);
         }

    private void setupClickListeners() {
        if (btnEdit != null) btnEdit.setOnClickListener(v -> enterEditMode());
        if (btnSave != null) btnSave.setOnClickListener(v -> saveProfile());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> exitEditMode(true));
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
        Button btnContact = findViewById(R.id.btnContact);
        if (btnContact != null) {
            btnContact.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, ContactActivity.class)));
        }
        if (btnViewFavorites != null) {
            btnViewFavorites.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, FavoritesActivity.class)));
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
                        // Favorites
                        @SuppressWarnings("unchecked")
                        List<String> favs = (List<String>) documentSnapshot.get("favorites");
                        favoritesSet.clear();
                        if (favs != null && !favs.isEmpty()) {
                            favoritesSet.addAll(favs);
                            loadFavoriteItems(favs);
                        } else {
                            // clear favorites view
                            if (rvFavorites != null) rvFavorites.setVisibility(View.GONE);
                        }
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
                final LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                row.setPadding(0, 4, 0, 4);

                final EditText etAddr = new EditText(this);
                etAddr.setText(addr);
                etAddr.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                addressEditTexts.add(etAddr);
                row.addView(etAddr);

                final Button btnRemove = new Button(this);
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

    private void loadFavoriteItems(List<String> favIds) {
        if (favIds == null || favIds.isEmpty()) return;
        rvFavorites.setVisibility(View.VISIBLE);
        final List<FoodItem> favItems = new java.util.ArrayList<>();

        // If list size <= 10 we can use whereIn, otherwise fetch individually
        if (favIds.size() <= 10) {
            db.collection("NewFoodDB").whereIn(com.google.firebase.firestore.FieldPath.documentId(), favIds)
                    .get()
                    .addOnSuccessListener(q -> {
                        favItems.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : q.getDocuments()) {
                            try {
                                String id = doc.getId();
                                String name = doc.getString("name");
                                Number price = (Number) doc.get("price");
                                Number salePrice = (Number) doc.get("salePrice");
                                String imageUrl = doc.getString("imageUrl");
                                Number rating = (Number) doc.get("rating");
                                FoodItem item = new FoodItem(id, name, doc.getString("description"), price != null ? price.intValue() : 0, imageUrl != null ? imageUrl : "", rating != null ? rating.intValue() : 0, salePrice != null ? salePrice.intValue() : null);
                                favItems.add(item);
                            } catch (Exception e) {
                                // ignore individual failures
                            }
                        }
                        // Setup adapter
                        favoritesAdapter = new MenuAdapter(favItems, this, item -> {
                            // open detail (ask MenuActivity to open the item)
                            Intent intent = new Intent(ProfileActivity.this, MenuActivity.class);
                            intent.putExtra("open_item", item.getDocumentId());
                            startActivity(intent);
                        }, favoritesSet, new MenuAdapter.OnFavoriteClick() {
                            @Override
                            public void onFavoriteClick(FoodItem item, boolean newState) {
                                // update UI and reload profile favorites
                                loadUserData();
                            }
                        }, false);
                        rvFavorites.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
                        rvFavorites.setAdapter(favoritesAdapter);
                    });
        } else {
            // fallback: fetch documents individually
            favItems.clear();
            final int[] remaining = {favIds.size()};
            for (String id : favIds) {
                db.collection("NewFoodDB").document(id).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            String name = doc.getString("name");
                            Number price = (Number) doc.get("price");
                            Number salePrice = (Number) doc.get("salePrice");
                            String imageUrl = doc.getString("imageUrl");
                            Number rating = (Number) doc.get("rating");
                            FoodItem item = new FoodItem(doc.getId(), name, doc.getString("description"), price != null ? price.intValue() : 0, imageUrl != null ? imageUrl : "", rating != null ? rating.intValue() : 0, salePrice != null ? salePrice.intValue() : null);
                            favItems.add(item);
                        } catch (Exception e) {}
                    }
                    remaining[0]--;
                        if (remaining[0] == 0) {
                        favoritesAdapter = new MenuAdapter(favItems, this, item -> {
                            Intent intent = new Intent(ProfileActivity.this, MenuActivity.class);
                            intent.putExtra("open_item", item.getDocumentId());
                            startActivity(intent);
                        }, favoritesSet, (itm, ns) -> loadUserData(), false);
                        rvFavorites.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
                        rvFavorites.setAdapter(favoritesAdapter);
                    }
                });
            }
        }
    }

    private void enterEditMode() {
        isEditing = true;
        btnEdit.setVisibility(View.GONE);
        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);

        // change toolbar nav icon to a 'cancel' icon while editing
        if (toolbarProfile != null) {
            toolbarProfile.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
        }

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
        // set the primary/default address so checkout can pick it up easily
        if (!newAddresses.isEmpty()) {
            data.put("address", newAddresses.get(0));
        }

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

        // restore toolbar nav icon to back
        if (toolbarProfile != null) {
            toolbarProfile.setNavigationIcon(android.R.drawable.ic_menu_revert);
        }

        if (reload) {
            loadUserData();
        } else {
            displayAddresses();
        }
    }
}