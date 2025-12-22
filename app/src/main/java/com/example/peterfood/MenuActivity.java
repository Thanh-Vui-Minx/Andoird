package com.example.peterfood;

import java.util.stream.Collectors;  // Để dùng stream (nếu Java 8+)
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuActivity extends BaseActivity {
    private List<ComboItem> comboList = new ArrayList<>();
    private static final String TAG = "MenuActivity";
    private RecyclerView rvMenu;
    private MenuAdapter adapter;
    private List<FoodItem> foodList;
    private List<FoodItem> filteredFoodList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private java.util.Set<String> favoritesSet = new java.util.HashSet<>();
    private String role;
    private EditText etSearch;
    private String pendingOpenItemId = null;
    private List<FoodItem> comboSectionList = new ArrayList<>();  // Combo riêng
    private List<FoodItem> regularFoodList = new ArrayList<>();   // Món thường
    private List<FoodItem> drinkList = new ArrayList<>(); // Nước uống riêng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        loadLogo();

        rvMenu = findViewById(R.id.rvMenu);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        ImageButton btnGoToCart = findViewById(R.id.btnGoToCart);
        ImageButton btnAddFood = findViewById(R.id.btnAddFood);
        TextView tvAddFoodLabel = findViewById(R.id.tvAddFoodLabel);
        View containerAddFood = findViewById(R.id.containerAddFood);
        View containerCart = findViewById(R.id.containerCart);
        View containerProfile = findViewById(R.id.containerProfile);
        View containerLogout = findViewById(R.id.containerLogout);
        etSearch = findViewById(R.id.etSearch);

        if (rvMenu == null || btnLogout == null || btnGoToCart == null || btnAddFood == null || etSearch == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy view", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        foodList = new ArrayList<>();
        filteredFoodList = new ArrayList<>();
        adapter = new MenuAdapter(filteredFoodList, this, item -> showFoodDetailDialog(item), favoritesSet, new MenuAdapter.OnFavoriteClick() {
            @Override
            public void onFavoriteClick(FoodItem item, boolean newState) {
                handleFavoriteToggle(item, newState);
            }
        });
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setAdapter(adapter);

        // Check if launched with a request to open a specific item
        if (getIntent() != null) {
            pendingOpenItemId = getIntent().getStringExtra("open_item");
        }

        role = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("role", "user");
        if (!"admin".equals(role)) {
            // hide the entire add-food container so other items re-distribute evenly
            if (containerAddFood != null) containerAddFood.setVisibility(View.GONE);
            else {
                btnAddFood.setVisibility(View.GONE);
                if (tvAddFoodLabel != null) tvAddFoodLabel.setVisibility(View.GONE);
            }
        } else {
            if (containerAddFood != null) containerAddFood.setVisibility(View.VISIBLE);
            else {
                btnAddFood.setVisibility(View.VISIBLE);
                if (tvAddFoodLabel != null) tvAddFoodLabel.setVisibility(View.VISIBLE);
            }
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { filterFoodList(s.toString()); }
        });

        loadFoodList();
        loadUserFavorites();

        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, ProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(MenuActivity.this, MainActivity.class));
            finish();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        });

        btnGoToCart.setOnClickListener(v -> startActivity(new Intent(MenuActivity.this, CartActivity.class)));
        btnAddFood.setOnClickListener(v -> {
            // Show dialog with options: Add Food or Add Combo
            if ("admin".equals(role)) {
                String[] options = {"Thêm Món Ăn", "Thêm Combo"};
                new AlertDialog.Builder(this)
                        .setTitle("Chọn loại")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                showAddFoodDialog();
                            } else {
                                showAddComboDialog();
                            }
                        })
                        .show();
            } else {
                showAddFoodDialog();
            }
        });
    }

    private void filterFoodList(String query) {
        filteredFoodList.clear();
        if (query.isEmpty()) {
            filteredFoodList.addAll(foodList);
        } else {
            for (FoodItem item : foodList) {
                if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredFoodList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        if (filteredFoodList.isEmpty() && !query.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy món ăn nào", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFoodDetailDialog(FoodItem item) {
        Log.d("Dialog", "Opening detail for: " + item.getName());

        // Skip headers
        if ("header_combo".equals(item.getDocumentId()) || 
            "header_limited_combo".equals(item.getDocumentId()) ||
            "header_regular_combo".equals(item.getDocumentId()) ||
            "header_food".equals(item.getDocumentId()) || 
            "header_drink".equals(item.getDocumentId())) {
            return;
        }

        // Handle combo items
        if (item.isCombo()) {
            showComboDetailDialog(item);
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_food_details);

        // === LẤY KÍCH THƯỚC MÀN HÌNH ===
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // === SET 95% MÀN HÌNH ===
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (screenWidth * 0.95);   // 95% chiều rộng
            params.height = (int) (screenHeight * 0.90); // 90% chiều cao (match combo dialog)
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // === TÌM VIEW ===
        ImageView ivFoodImage = dialog.findViewById(R.id.ivFoodImage);
        TextView tvFoodName = dialog.findViewById(R.id.tvFoodName);
        TextView tvDetailedDescription = dialog.findViewById(R.id.tvDetailedDescription);
        TextView tvFoodPrice = dialog.findViewById(R.id.tvFoodPrice);
        TextView tvFoodSalePrice = dialog.findViewById(R.id.tvFoodSalePrice);
        TextView tvFoodRating = dialog.findViewById(R.id.tvFoodRating);
        TextView tvSelectedPrice = dialog.findViewById(R.id.tvSelectedPrice);
        Spinner spSize = dialog.findViewById(R.id.spSize);
        EditText etNote = dialog.findViewById(R.id.etNote);
        Button btnAddToCart = dialog.findViewById(R.id.btnAddToCart);
        Button btnEditFood = dialog.findViewById(R.id.btnEditFood);
        Button btnDeleteFood = dialog.findViewById(R.id.btnDeleteFood);
        Button btnClose = dialog.findViewById(R.id.btnClose);
        LinearLayout llBottomButtons = dialog.findViewById(R.id.llBottomButtons);

        if (tvFoodName == null || btnAddToCart == null) {
            Toast.makeText(this, "Lỗi layout", Toast.LENGTH_LONG).show();
            return;
        }

        // === HIỂN THỊ NỘI DUNG ===
        Glide.with(this).load(item.getImageUrl()).into(ivFoodImage);
        tvFoodName.setText(item.getName());
        tvDetailedDescription.setText(item.getDetailedDescription());
        tvFoodRating.setText("Đánh giá: " + item.getRating() + "/5");

        // === GIÁ + SIZE ===
        final int price = item.getPrice();
        final Integer salePrice = item.getSalePrice();

        if (salePrice != null && salePrice < price) {
            tvFoodPrice.setText(salePrice + " VNĐ");
            tvFoodSalePrice.setText("Giá gốc: " + price + " VNĐ");
            tvFoodSalePrice.setPaintFlags(tvFoodSalePrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvFoodSalePrice.setVisibility(View.VISIBLE);
        } else {
            tvFoodPrice.setText(price + " VNĐ");
            tvFoodSalePrice.setVisibility(View.GONE);
        }

        // === SPINNER SIZE ===
        Map<String, Long> sizePrices = item.getSizePrices();
        if (sizePrices == null || sizePrices.isEmpty()) {
            sizePrices = new HashMap<>();
            sizePrices.put("Small", (long) price);
        }
        List<String> sizes = new ArrayList<>(sizePrices.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSize.setAdapter(adapter);

        final Map<String, Long> finalSizePrices = sizePrices;
        spSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String size = sizes.get(pos);
                long selectedPrice = finalSizePrices.get(size);
                tvSelectedPrice.setText("Giá đã chọn: " + selectedPrice + " VNĐ");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // === GHI CHÚ ===
        final String[] note = {""};
        etNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { note[0] = s.toString(); }
        });

        // === ADMIN: 4 NÚT ===
        if ("admin".equals(role)) {
            btnEditFood.setVisibility(View.VISIBLE);
            btnDeleteFood.setVisibility(View.VISIBLE);

            // Căn đều 4 nút
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(8, 0, 8, 0);
            btnAddToCart.setLayoutParams(params);
            btnEditFood.setLayoutParams(params);
            btnDeleteFood.setLayoutParams(params);
            btnClose.setLayoutParams(params);

            btnEditFood.setOnClickListener(v -> { dialog.dismiss(); showEditFoodDialog(item); });
            btnDeleteFood.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa món")
                        .setMessage("Xóa " + item.getName() + "?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            db.collection("NewFoodDB").document(item.getDocumentId()).delete()
                                    .addOnSuccessListener(a -> {
                                        Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        loadFoodList();
                                    });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            // === USER: 2 NÚT, CĂN GIỮA ===
            btnEditFood.setVisibility(View.GONE);
            btnDeleteFood.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(32, 0, 32, 0);  // Khoảng cách lớn hơn
            btnAddToCart.setLayoutParams(params);
            btnClose.setLayoutParams(params);
        }

        // === THÊM VÀO GIỎ ===
        btnAddToCart.setOnClickListener(v -> {
            String size = (String) spSize.getSelectedItem();
            if (size == null) {
                Toast.makeText(this, "Chọn kích cỡ", Toast.LENGTH_SHORT).show();
                return;
            }
            long selectedPrice = finalSizePrices.get(size);

            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("foodId", item.getDocumentId());
            cartItem.put("name", item.getName());
            cartItem.put("price", selectedPrice);
            cartItem.put("size", size);
            cartItem.put("imageUrl", item.getImageUrl());
            if (!note[0].isEmpty()) cartItem.put("note", note[0]);

            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .update("cart", FieldValue.arrayUnion(cartItem))
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Đã thêm!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadUserFavorites() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> favs = (java.util.List<String>) doc.get("favorites");
                        favoritesSet.clear();
                        if (favs != null) favoritesSet.addAll(favs);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                });
    }

    private void handleFavoriteToggle(FoodItem item, boolean newState) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng chức năng yêu thích", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        DocumentReference userRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        if (newState) {
            userRef.update("favorites", FieldValue.arrayUnion(item.getDocumentId()))
                    .addOnSuccessListener(a -> Toast.makeText(this, "Đã thêm vào Yêu thích", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            userRef.update("favorites", FieldValue.arrayRemove(item.getDocumentId()))
                    .addOnSuccessListener(a -> Toast.makeText(this, "Đã xóa khỏi Yêu thích", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
    private void showAddFoodDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_food);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView ivFoodImageAdd = dialog.findViewById(R.id.ivFoodImageAdd);
        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice);
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        Spinner spFoodTag = dialog.findViewById(R.id.spFoodTag);
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        if (etFoodName == null || etFoodDescription == null || etFoodPrice == null || etFoodRating == null || btnSaveFood == null || btnCancel == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy view", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        // Xem trước ảnh khi nhập URL
        etFoodImageUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    Glide.with(MenuActivity.this)
                            .load(url)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(ivFoodImageAdd);
                } else {
                    ivFoodImageAdd.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        });

        btnSaveFood.setOnClickListener(v -> {
            String name = etFoodName.getText().toString().trim();
            String description = etFoodDescription.getText().toString().trim();
            String detailedDescription = etDetailedDescription.getText().toString().trim();
            String priceStr = etFoodPrice.getText().toString().trim();
            String mediumPriceStr = etMediumPrice.getText().toString().trim();
            String largePriceStr = etLargePrice.getText().toString().trim();
            String salePriceStr = etFoodSalePrice.getText().toString().trim();
            String imageUrl = etFoodImageUrl.getText().toString().trim();
            String ratingStr = etFoodRating.getText().toString().trim();
            String tag = spFoodTag.getSelectedItem().toString();

            if (name.isEmpty() || priceStr.isEmpty() || ratingStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            int price, rating;
            Long salePrice = null, mediumPrice = null, largePrice = null;
            try {
                price = Integer.parseInt(priceStr);
                rating = Integer.parseInt(ratingStr);
                if (!salePriceStr.isEmpty()) salePrice = Long.parseLong(salePriceStr);
                if (!mediumPriceStr.isEmpty()) mediumPrice = Long.parseLong(mediumPriceStr);
                if (!largePriceStr.isEmpty()) largePrice = Long.parseLong(largePriceStr);
                if (salePrice != null && salePrice >= price) {
                    Toast.makeText(this, "Giá giảm phải nhỏ hơn giá gốc", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (rating < 0 || rating > 5) {
                    Toast.makeText(this, "Đánh giá phải từ 0 đến 5", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá phải là số hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Long> sizePrices = new HashMap<>();
            sizePrices.put("Small", (long) price);
            if (mediumPrice != null) sizePrices.put("Medium", mediumPrice);
            if (largePrice != null) sizePrices.put("Large", largePrice);

            Map<String, Object> foodData = new HashMap<>();
            foodData.put("name", name);
            foodData.put("description", description.isEmpty() ? "Không có mô tả" : description);
            foodData.put("detailedDescription", detailedDescription.isEmpty() ? "Chưa có mô tả chi tiết" : detailedDescription);
            foodData.put("tag", tag);
            foodData.put("price", price);
            foodData.put("salePrice", salePrice != null ? salePrice.intValue() : null);
            foodData.put("imageUrl", imageUrl.isEmpty() ? "" : imageUrl);
            foodData.put("rating", rating);
            foodData.put("sizePrices", sizePrices);
            foodData.put("comments", new ArrayList<>()); // Danh sách bình luận rỗng

            db.collection("NewFoodDB").add(foodData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Đã thêm món ăn: " + name, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadFoodList();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi thêm món ăn: " + e.getMessage(), e);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditFoodDialog(FoodItem item) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_food);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView ivFoodImageAdd = dialog.findViewById(R.id.ivFoodImageAdd);
        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice);
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        Spinner spFoodTag = dialog.findViewById(R.id.spFoodTag);
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        // Set giá trị hiện tại
        etFoodName.setText(item.getName());
        etFoodDescription.setText(item.getDescription());
        etDetailedDescription.setText(item.getDetailedDescription());
        etFoodPrice.setText(String.valueOf(item.getPrice()));
        etFoodSalePrice.setText(item.getSalePrice() != null ? String.valueOf(item.getSalePrice()) : "");
        etFoodImageUrl.setText(item.getImageUrl());
        etFoodRating.setText(String.valueOf(item.getRating()));

        // Set giá kích cỡ
        etMediumPrice.setText(item.getSizePrices().get("Medium") != null ? String.valueOf(item.getSizePrices().get("Medium")) : "");
        etLargePrice.setText(item.getSizePrices().get("Large") != null ? String.valueOf(item.getSizePrices().get("Large")) : "");

        // Set tag
        int tagPosition = 0;
        if ("nước".equals(item.getTag())) tagPosition = 1;
        spFoodTag.setSelection(tagPosition);

        // Xem trước ảnh
        Glide.with(this)
                .load(item.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivFoodImageAdd);

        // Xem trước ảnh khi nhập URL
        etFoodImageUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    Glide.with(MenuActivity.this)
                            .load(url)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(ivFoodImageAdd);
                } else {
                    ivFoodImageAdd.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        });

        btnSaveFood.setText("Cập nhật");
        btnSaveFood.setOnClickListener(v -> {
            String name = etFoodName.getText().toString().trim();
            String description = etFoodDescription.getText().toString().trim();
            String detailedDescription = etDetailedDescription.getText().toString().trim();
            String priceStr = etFoodPrice.getText().toString().trim();
            String mediumPriceStr = etMediumPrice.getText().toString().trim();
            String largePriceStr = etLargePrice.getText().toString().trim();
            String salePriceStr = etFoodSalePrice.getText().toString().trim();
            String imageUrl = etFoodImageUrl.getText().toString().trim();
            String ratingStr = etFoodRating.getText().toString().trim();
            String tag = spFoodTag.getSelectedItem().toString();

            if (name.isEmpty() || priceStr.isEmpty() || ratingStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            int price, rating;
            Long salePrice = null, mediumPrice = null, largePrice = null;
            try {
                price = Integer.parseInt(priceStr);
                rating = Integer.parseInt(ratingStr);
                if (!salePriceStr.isEmpty()) salePrice = Long.parseLong(salePriceStr);
                if (!mediumPriceStr.isEmpty()) mediumPrice = Long.parseLong(mediumPriceStr);
                if (!largePriceStr.isEmpty()) largePrice = Long.parseLong(largePriceStr);
                if (salePrice != null && salePrice >= price) {
                    Toast.makeText(this, "Giá giảm phải nhỏ hơn giá gốc", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (rating < 0 || rating > 5) {
                    Toast.makeText(this, "Đánh giá phải từ 0 đến 5", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá phải là số hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Long> sizePrices = new HashMap<>();
            sizePrices.put("Small", (long) price);
            if (mediumPrice != null) sizePrices.put("Medium", mediumPrice);
            if (largePrice != null) sizePrices.put("Large", largePrice);

            Map<String, Object> foodData = new HashMap<>();
            foodData.put("name", name);
            foodData.put("description", description.isEmpty() ? "Không có mô tả" : description);
            foodData.put("price", price);
            foodData.put("salePrice", salePrice != null ? salePrice.intValue() : null);
            foodData.put("imageUrl", imageUrl.isEmpty() ? "" : imageUrl);
            foodData.put("rating", rating);
            foodData.put("detailedDescription", detailedDescription.isEmpty() ? "Chưa có mô tả chi tiết" : detailedDescription);
            foodData.put("sizePrices", sizePrices);
            foodData.put("comments", item.getComments());

            db.collection("NewFoodDB").document(item.getDocumentId())
                    .set(foodData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật món ăn: " + name, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadFoodList();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi cập nhật món ăn: " + e.getMessage(), e);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    private void generateSmartCombos() {
        comboSectionList.clear();  // Xóa combo cũ

        List<FoodItem> foods = foodList.stream()
                .filter(item -> "thức ăn".equals(item.getTag()))
                .collect(Collectors.toList());
        List<FoodItem> drinks = foodList.stream()
                .filter(item -> "nước".equals(item.getTag()))
                .collect(Collectors.toList());

        // === COMBO 1+1 ===
        if (!foods.isEmpty() && !drinks.isEmpty()) {
            FoodItem f = foods.get(0);
            FoodItem d = drinks.get(0);
            int original = f.getPrice() + d.getPrice();
            int comboPrice = (int) (original * 0.9);

            ComboItem combo = new ComboItem(
                    "combo_1_1",
                    f.getName() + " + " + d.getName(),
                    "Tiết kiệm 10%",
                    comboPrice, original, f.getImageUrl(),
                    List.of(f.getDocumentId(), d.getDocumentId()),
                    Map.of(f.getDocumentId(), 1, d.getDocumentId(), 1)
            );
            comboSectionList.add(new FoodItem(combo));
        }

        // === COMBO 2+3 ===
        if (foods.size() >= 2 && drinks.size() >= 3) {
            List<FoodItem> fc = foods.subList(0, 2);
            List<FoodItem> dc = drinks.subList(0, 3);
            int original = fc.stream().mapToInt(FoodItem::getPrice).sum() + dc.stream().mapToInt(FoodItem::getPrice).sum();
            int comboPrice = (int) (original * 0.85);

            String name = fc.get(0).getName() + " + " + fc.get(1).getName() + " + 3 nước";
            ComboItem combo = new ComboItem("combo_2_3", name, "Tiết kiệm 15%", comboPrice, original, fc.get(0).getImageUrl(),
                    java.util.stream.Stream.concat(fc.stream(), dc.stream()).map(FoodItem::getDocumentId).collect(Collectors.toList()),
                    java.util.stream.Stream.concat(fc.stream(), dc.stream()).collect(Collectors.toMap(FoodItem::getDocumentId, i -> 1))
            );
            comboSectionList.add(new FoodItem(combo));
        }

        // === COMBO 4+6 ===
        if (foods.size() >= 4 && drinks.size() >= 6) {
            List<FoodItem> fc = foods.subList(0, 4);
            List<FoodItem> dc = drinks.subList(0, 6);
            int original = fc.stream().mapToInt(FoodItem::getPrice).sum() + dc.stream().mapToInt(FoodItem::getPrice).sum();
            int comboPrice = (int) (original * 0.8);

            ComboItem combo = new ComboItem("combo_4_6", "Combo Gia Đình", "Tiết kiệm 20%", comboPrice, original, fc.get(0).getImageUrl(),
                    java.util.stream.Stream.concat(fc.stream(), dc.stream()).map(FoodItem::getDocumentId).collect(Collectors.toList()),
                    java.util.stream.Stream.concat(fc.stream(), dc.stream()).collect(Collectors.toMap(FoodItem::getDocumentId, i -> 1))
            );
            comboSectionList.add(new FoodItem(combo));
        }
    }
    private void updateMenuWithSections() {
        filteredFoodList.clear();

        // === THÊM SECTION COMBO GIỚI HẠN (nếu có) ===
        if (!limitedCombos.isEmpty()) {
            FoodItem limitedHeader = new FoodItem("header_limited_combo", "⏰ COMBO GIỚI HẠN", "", 0, "", 0, null);
            limitedHeader.setIsCombo(true);
            filteredFoodList.add(limitedHeader);
            
            for (ComboItem combo : limitedCombos) {
                filteredFoodList.add(new FoodItem(combo));
            }
        }

        // === THÊM SECTION COMBO THƯỜNG XUYÊN (nếu có) ===
        if (!regularCombos.isEmpty()) {
            FoodItem regularHeader = new FoodItem("header_regular_combo", "🎉 COMBO ", "", 0, "", 0, null);
            regularHeader.setIsCombo(true);
            filteredFoodList.add(regularHeader);
            
            for (ComboItem combo : regularCombos) {
                filteredFoodList.add(new FoodItem(combo));
            }
        }

        // === THÊM COMBO TỰ ĐỘNG (từ generateSmartCombos) ===
        if (!comboSectionList.isEmpty()) {
            FoodItem comboHeader = new FoodItem("header_combo", " COMBO GỢI Ý", "", 0, "", 0, null);
            comboHeader.setIsCombo(true);
            filteredFoodList.add(comboHeader);
            filteredFoodList.addAll(comboSectionList);
        }

        // === THÊM SECTION MÓN ĂN ===
        FoodItem foodHeader = new FoodItem("header_food", " THỨC ĂN", "", 0, "", 0, null);
        foodHeader.setIsCombo(true);
        filteredFoodList.add(foodHeader);
        filteredFoodList.addAll(regularFoodList);

        // === THÊM SECTION NƯỚC UỐNG ===
        if (!drinkList.isEmpty()) {
            FoodItem drinkHeader = new FoodItem("header_drink", " NƯỚC UỐNG", "", 0, "", 0, null);
            drinkHeader.setIsCombo(true);
            filteredFoodList.add(drinkHeader);
            filteredFoodList.addAll(drinkList);
        }
    }

    private void addComboToCart(ComboItem combo) {
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("isCombo", true);
        cartItem.put("comboId", combo.getId());
        cartItem.put("name", combo.getName());
        cartItem.put("price", combo.getComboPrice());
        cartItem.put("imageUrl", combo.getImageUrl());
        cartItem.put("foodIds", combo.getFoodIds());
        cartItem.put("quantities", combo.getQuantities());

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .update("cart", FieldValue.arrayUnion(cartItem))
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã thêm combo vào giỏ! Tiết kiệm " + (combo.getOriginalPrice() - combo.getComboPrice()) + " VNĐ", Toast.LENGTH_SHORT).show());
    }
    private void loadFoodList() {
        db.collection("NewFoodDB")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 1. XÓA DỮ LIỆU CŨ
                    foodList.clear();
                    comboSectionList.clear();
                    regularFoodList.clear();
                    drinkList.clear();

                    // 2. ĐỌC TỪ FIRESTORE → CHỈ LÀ MÓN THƯỜNG
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String name = doc.getString("name");
                            String description = doc.getString("description");
                            String tag = doc.getString("tag");
                            Number price = (Number) doc.get("price");
                            Number salePrice = (Number) doc.get("salePrice");
                            String imageUrl = doc.getString("imageUrl");
                            Number rating = (Number) doc.get("rating");
                            String detailedDescription = doc.getString("detailedDescription");
                            @SuppressWarnings("unchecked")
                            Map<String, Long> sizePrices = (Map<String, Long>) doc.get("sizePrices");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> comments = (List<Map<String, Object>>) doc.get("comments");

                            if (name != null && price != null) {
                                FoodItem item = new FoodItem(
                                        doc.getId(),
                                        name,
                                        description != null ? description : "Không có mô tả",
                                        price.intValue(),
                                        imageUrl != null ? imageUrl : "",
                                        rating != null ? rating.intValue() : 0,
                                        salePrice != null ? salePrice.intValue() : null
                                );
                                item.setTag(tag != null ? tag : "thức ăn");
                                item.setDetailedDescription(detailedDescription != null ? detailedDescription : "");
                                item.setSizePrices(sizePrices != null ? sizePrices : new HashMap<>());
                                item.setComments(comments != null ? comments : new ArrayList<>());

                                // CHỈ THÊM VÀO foodList và phân loại food/drink
                                foodList.add(item);
                                if ("nước".equals(item.getTag())) {
                                    drinkList.add(item);
                                } else {
                                    regularFoodList.add(item);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document: " + e.getMessage(), e);
                        }
                    }

                    // 3. SINH COMBO TỪ DỮ LIỆU THỰC
                    generateSmartCombos();  // ← Tạo combo → thêm vào comboSectionList

                    // 4. TẢI COMBOS TỪ FIREBASE
                    loadCombosFromFirebase();

                    // 5. TẠO SECTION: COMBO + MÓN ĂN
                    updateMenuWithSections();

                    // 6. CẬP NHẬT ADAPTER
                    adapter.notifyDataSetChanged();

                    // If there is a pending open request (from FavoritesActivity), try to open it
                    if (pendingOpenItemId != null) {
                        for (FoodItem it : filteredFoodList) {
                            if (pendingOpenItemId.equals(it.getDocumentId())) {
                                // open detail on UI thread after adapter updated
                                showFoodDetailDialog(it);
                                pendingOpenItemId = null;
                                break;
                            }
                        }
                    }

                    if (filteredFoodList.isEmpty()) {
                        Toast.makeText(this, "Không có món ăn nào", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting food items: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==================== COMBO MANAGEMENT ====================
    private List<ComboItem> regularCombos = new ArrayList<>();
    private List<ComboItem> limitedCombos = new ArrayList<>();

    private void showComboDetailDialog(FoodItem item) {
        // Get the combo item from the FoodItem wrapper
        ComboItem combo = null;
        
        // Search in both lists
        for (ComboItem c : regularCombos) {
            if (c.getId() != null && c.getId().equals(item.getDocumentId())) {
                combo = c;
                break;
            }
        }
        if (combo == null) {
            for (ComboItem c : limitedCombos) {
                if (c.getId() != null && c.getId().equals(item.getDocumentId())) {
                    combo = c;
                    break;
                }
            }
        }

        if (combo == null) {
            Toast.makeText(this, "Không tìm thấy thông tin combo", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_combo_detail);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            params.width = (int) (metrics.widthPixels * 0.95);
            params.height = (int) (metrics.heightPixels * 0.90);
            window.setAttributes(params);
            // Make window background transparent so the dialog layout's rounded corners and shadows show consistently
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Get views
        ImageView ivComboDetailImage = dialog.findViewById(R.id.ivComboDetailImage);
        TextView tvComboDetailName = dialog.findViewById(R.id.tvComboDetailName);
        TextView tvComboDetailDescription = dialog.findViewById(R.id.tvComboDetailDescription);
        LinearLayout llLimitedInfo = dialog.findViewById(R.id.llLimitedInfo);
        TextView tvTimeRemaining = dialog.findViewById(R.id.tvTimeRemaining);
        TextView tvComboDetailPrice = dialog.findViewById(R.id.tvComboDetailPrice);
        TextView tvComboDetailOriginalPrice = dialog.findViewById(R.id.tvComboDetailOriginalPrice);
        TextView tvComboDetailSavings = dialog.findViewById(R.id.tvComboDetailSavings);
        RecyclerView rvComboFoodItems = dialog.findViewById(R.id.rvComboFoodItems);
        Button btnAddComboToCart = dialog.findViewById(R.id.btnAddComboToCart);
        Button btnEditCombo = dialog.findViewById(R.id.btnEditCombo);
        Button btnDeleteCombo = dialog.findViewById(R.id.btnDeleteCombo);
        Button btnCloseCombo = dialog.findViewById(R.id.btnCloseCombo);

        final ComboItem finalCombo = combo;

        // Set data
        tvComboDetailName.setText(combo.getName());
        tvComboDetailDescription.setText(combo.getDescription());
        tvComboDetailPrice.setText(String.format("%,d VNĐ", combo.getComboPrice()));
        tvComboDetailOriginalPrice.setText(String.format("%,d VNĐ", combo.getOriginalPrice()));
        
        int savings = combo.getSavings();
        int percent = (int) ((savings * 100.0) / combo.getOriginalPrice());
        tvComboDetailSavings.setText(String.format("💰 Tiết kiệm: %,d VNĐ (%d%%)", savings, percent));

        // Image
        if (combo.getImageUrl() != null && !combo.getImageUrl().isEmpty()) {
            Glide.with(this).load(combo.getImageUrl()).into(ivComboDetailImage);
        }

        // Limited time info
        if (combo.isLimitedTime()) {
            llLimitedInfo.setVisibility(View.VISIBLE);
            tvTimeRemaining.setText("Còn lại: " + combo.getFormattedRemainingTime());
        } else {
            llLimitedInfo.setVisibility(View.GONE);
        }

        // Load food items in combo
        // TODO: Create a simple adapter for combo food items
        rvComboFoodItems.setLayoutManager(new LinearLayoutManager(this));

        // Build list of FoodItem for combo
        List<FoodItem> comboFoods = new ArrayList<>();
        Map<String, Integer> quantities = combo.getQuantities() != null ? combo.getQuantities() : new HashMap<>();
        if (combo.getFoodIds() != null && !combo.getFoodIds().isEmpty()) {
            for (String fid : combo.getFoodIds()) {
                // Try to find in loaded foodList
                FoodItem found = null;
                for (FoodItem f : foodList) {
                    if (fid.equals(f.getDocumentId())) {
                        found = f;
                        break;
                    }
                }
                if (found != null) comboFoods.add(found);
            }
        }

        if (!comboFoods.isEmpty()) {
            ComboFoodAdapter foodAdapter = new ComboFoodAdapter(this, comboFoods, quantities, role, new ComboFoodAdapter.OnItemAction() {
                @Override
                public void onAddToCart(FoodItem item, int quantity) {
                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("foodId", item.getDocumentId());
                    cartItem.put("name", item.getName());
                    cartItem.put("price", item.getFinalPrice());
                    cartItem.put("size", "Small");
                    cartItem.put("imageUrl", item.getImageUrl());
                    cartItem.put("quantity", quantity);

                    db.collection("users").document(mAuth.getCurrentUser().getUid())
                            .update("cart", FieldValue.arrayUnion(cartItem))
                            .addOnSuccessListener(a -> Toast.makeText(MenuActivity.this, "Đã thêm vào giỏ: " + item.getName(), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(MenuActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onRemoveFromCombo(FoodItem item) {
                    // For now, removing an item should be done via Edit Combo. Show hint to admin.
                    Toast.makeText(MenuActivity.this, "Vui lòng dùng chức năng Sửa để thay đổi món trong combo.", Toast.LENGTH_SHORT).show();
                }
            });
            rvComboFoodItems.setAdapter(foodAdapter);
            rvComboFoodItems.setVisibility(View.VISIBLE);
        } else {
            rvComboFoodItems.setVisibility(View.GONE);
        }

        // Admin buttons
        if ("admin".equals(role)) {
            btnEditCombo.setVisibility(View.VISIBLE);
            btnDeleteCombo.setVisibility(View.VISIBLE);

            btnEditCombo.setOnClickListener(v -> {
                dialog.dismiss();
                showEditComboDialog(finalCombo);
            });

            btnDeleteCombo.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa combo")
                        .setMessage("Xóa " + finalCombo.getName() + "?")
                        .setPositiveButton("Xóa", (d, which) -> {
                            db.collection("combos").document(finalCombo.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Đã xóa combo", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        loadCombosFromFirebase();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            btnEditCombo.setVisibility(View.GONE);
            btnDeleteCombo.setVisibility(View.GONE);
        }

        // Add to cart
        btnAddComboToCart.setOnClickListener(v -> {
            addComboToCart(finalCombo);
            dialog.dismiss();
        });

        btnCloseCombo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadCombosFromFirebase() {
        db.collection("combos")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    regularCombos.clear();
                    limitedCombos.clear();
                    comboSectionList.clear();

                    for (DocumentSnapshot doc : querySnapshot) {
                        try {
                            ComboItem combo = doc.toObject(ComboItem.class);
                            if (combo == null) combo = new ComboItem();
                            combo.setId(doc.getId());

                            // Normalize fields in case Firestore stored different keys or Timestamp types
                            try {
                                // active
                                Boolean activeField = doc.contains("active") ? doc.getBoolean("active") : null;
                                if (activeField != null) combo.setActive(activeField);

                                // limited time flag can be stored as 'isLimitedTime' or 'isLimited' or 'limitedTime'
                                Boolean limitedFlag = null;
                                if (doc.contains("isLimitedTime")) limitedFlag = doc.getBoolean("isLimitedTime");
                                else if (doc.contains("isLimited")) limitedFlag = doc.getBoolean("isLimited");
                                else if (doc.contains("limitedTime")) limitedFlag = doc.getBoolean("limitedTime");
                                if (limitedFlag != null) combo.setLimitedTime(limitedFlag);

                                // startDate / endDate may be stored as Timestamp or Long
                                Object s = doc.get("startDate");
                                if (s instanceof com.google.firebase.Timestamp) combo.setStartDate(((com.google.firebase.Timestamp) s).toDate().getTime());
                                else if (s instanceof Number) combo.setStartDate(((Number) s).longValue());

                                Object e = doc.get("endDate");
                                if (e instanceof com.google.firebase.Timestamp) combo.setEndDate(((com.google.firebase.Timestamp) e).toDate().getTime());
                                else if (e instanceof Number) combo.setEndDate(((Number) e).longValue());

                                // createdAt normalization
                                Object c = doc.get("createdAt");
                                if (c instanceof com.google.firebase.Timestamp) combo.setCreatedAt(((com.google.firebase.Timestamp) c).toDate().getTime());
                                else if (c instanceof Number) combo.setCreatedAt(((Number) c).longValue());
                            } catch (Exception ex) {
                                Log.w(TAG, "Normalization warning for combo doc " + doc.getId() + ": " + ex.getMessage());
                            }

                            // Debug logging for visibility
                            Log.d(TAG, "Loaded combo doc=" + doc.getId() + " name=" + combo.getName() + " active=" + combo.isActive() + " limited=" + combo.isLimitedTime() + " start=" + combo.getStartDate() + " end=" + combo.getEndDate());

                            // Chỉ thêm combo còn hợp lệ
                            if (combo.isValid()) {
                                if (combo.isLimitedTime()) {
                                    limitedCombos.add(combo);
                                } else {
                                    regularCombos.add(combo);
                                }
                            } else {
                                Log.i(TAG, "Skipping combo (not valid or inactive): " + combo.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing combo: " + e.getMessage(), e);
                        }
                    }

                    // Cập nhật UI
                    updateMenuWithSections();
                    adapter.notifyDataSetChanged();

                    // If there is a pending open request (from FavoritesActivity), try to open it
                    if (pendingOpenItemId != null) {
                        for (FoodItem it : filteredFoodList) {
                            if (pendingOpenItemId.equals(it.getDocumentId())) {
                                showFoodDetailDialog(it);
                                pendingOpenItemId = null;
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading combos: " + e.getMessage(), e);
                });
    }

    private void showAddComboDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_combo);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Get views
        EditText etComboName = dialog.findViewById(R.id.etComboName);
        EditText etComboDescription = dialog.findViewById(R.id.etComboDescription);
        EditText etComboImageUrl = dialog.findViewById(R.id.etComboImageUrl);
        ImageView ivComboImagePreview = dialog.findViewById(R.id.ivComboImagePreview);
        EditText etComboOriginalPrice = dialog.findViewById(R.id.etComboOriginalPrice);
        EditText etComboPrice = dialog.findViewById(R.id.etComboPrice);
        CheckBox cbLimitedTime = dialog.findViewById(R.id.cbLimitedTime);
        LinearLayout llDateRange = dialog.findViewById(R.id.llDateRange);
        Button btnStartDate = dialog.findViewById(R.id.btnStartDate);
        Button btnEndDate = dialog.findViewById(R.id.btnEndDate);
        TextView tvDateRange = dialog.findViewById(R.id.tvDateRange);
        Button btnSelectFoodItems = dialog.findViewById(R.id.btnSelectFoodItems);
        RecyclerView rvSelectedFoodItems = dialog.findViewById(R.id.rvSelectedFoodItems);
        TextView tvSelectedItemsInfo = dialog.findViewById(R.id.tvSelectedItemsInfo);
        Button btnSaveCombo = dialog.findViewById(R.id.btnSaveCombo);
        Button btnCancelCombo = dialog.findViewById(R.id.btnCancelCombo);

        // Image preview
        etComboImageUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    Glide.with(MenuActivity.this)
                            .load(url)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .into(ivComboImagePreview);
                }
            }
        });

        // Limited time toggle
        cbLimitedTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            llDateRange.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Date selection
        final Long[] startDate = {null};
        final Long[] endDate = {null};

        btnStartDate.setOnClickListener(v -> showDateTimePicker(date -> {
            startDate[0] = date;
            updateDateRangeText(tvDateRange, startDate[0], endDate[0]);
        }));

        btnEndDate.setOnClickListener(v -> showDateTimePicker(date -> {
            endDate[0] = date;
            updateDateRangeText(tvDateRange, startDate[0], endDate[0]);
        }));

        // Food selection
        final List<String> selectedFoodIds = new ArrayList<>();
        final Map<String, Integer> quantities = new HashMap<>();

        btnSelectFoodItems.setOnClickListener(v -> {
            showFoodSelectionDialog(selectedFoodIds, quantities, () -> {
                tvSelectedItemsInfo.setText(selectedFoodIds.size() + " món đã chọn");
            });
        });

        // Save combo
        btnSaveCombo.setOnClickListener(v -> {
            String name = etComboName.getText().toString().trim();
            String description = etComboDescription.getText().toString().trim();
            String imageUrl = etComboImageUrl.getText().toString().trim();
            String originalPriceStr = etComboOriginalPrice.getText().toString().trim();
            String comboPriceStr = etComboPrice.getText().toString().trim();

            if (name.isEmpty() || originalPriceStr.isEmpty() || comboPriceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedFoodIds.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 món ăn", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int originalPrice = Integer.parseInt(originalPriceStr);
                int comboPrice = Integer.parseInt(comboPriceStr);

                if (comboPrice >= originalPrice) {
                    Toast.makeText(this, "Giá combo phải nhỏ hơn giá gốc", Toast.LENGTH_SHORT).show();
                    return;
                }

                ComboItem combo = new ComboItem();
                combo.setName(name);
                combo.setDescription(description);
                combo.setImageUrl(imageUrl);
                combo.setOriginalPrice(originalPrice);
                combo.setComboPrice(comboPrice);
                combo.setFoodIds(selectedFoodIds);
                combo.setQuantities(quantities);
                combo.setActive(true);
                combo.setCreatedAt(System.currentTimeMillis());

                if (cbLimitedTime.isChecked()) {
                    if (startDate[0] == null || endDate[0] == null) {
                        Toast.makeText(this, "Vui lòng chọn thời gian bắt đầu và kết thúc", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    combo.setLimitedTime(true);
                    combo.setStartDate(startDate[0]);
                    combo.setEndDate(endDate[0]);
                }

                // Save to Firebase
                Map<String, Object> comboData = new HashMap<>();
                comboData.put("name", combo.getName());
                comboData.put("description", combo.getDescription());
                comboData.put("imageUrl", combo.getImageUrl());
                comboData.put("originalPrice", combo.getOriginalPrice());
                comboData.put("comboPrice", combo.getComboPrice());
                comboData.put("foodIds", combo.getFoodIds());
                comboData.put("quantities", combo.getQuantities());
                comboData.put("active", combo.isActive());
                comboData.put("isLimitedTime", combo.isLimitedTime());
                comboData.put("startDate", combo.getStartDate());
                comboData.put("endDate", combo.getEndDate());
                comboData.put("createdAt", combo.getCreatedAt());

                db.collection("combos").add(comboData)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "Đã thêm combo: " + name, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadCombosFromFirebase();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá phải là số hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelCombo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditComboDialog(ComboItem existingCombo) {
        if (existingCombo == null) return;

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_combo);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Get views (same IDs as add dialog)
        EditText etComboName = dialog.findViewById(R.id.etComboName);
        EditText etComboDescription = dialog.findViewById(R.id.etComboDescription);
        EditText etComboImageUrl = dialog.findViewById(R.id.etComboImageUrl);
        ImageView ivComboImagePreview = dialog.findViewById(R.id.ivComboImagePreview);
        EditText etComboOriginalPrice = dialog.findViewById(R.id.etComboOriginalPrice);
        EditText etComboPrice = dialog.findViewById(R.id.etComboPrice);
        CheckBox cbLimitedTime = dialog.findViewById(R.id.cbLimitedTime);
        LinearLayout llDateRange = dialog.findViewById(R.id.llDateRange);
        Button btnStartDate = dialog.findViewById(R.id.btnStartDate);
        Button btnEndDate = dialog.findViewById(R.id.btnEndDate);
        TextView tvDateRange = dialog.findViewById(R.id.tvDateRange);
        Button btnSelectFoodItems = dialog.findViewById(R.id.btnSelectFoodItems);
        RecyclerView rvSelectedFoodItems = dialog.findViewById(R.id.rvSelectedFoodItems);
        TextView tvSelectedItemsInfo = dialog.findViewById(R.id.tvSelectedItemsInfo);
        Button btnSaveCombo = dialog.findViewById(R.id.btnSaveCombo);
        Button btnCancelCombo = dialog.findViewById(R.id.btnCancelCombo);

        // Prefill values
        etComboName.setText(existingCombo.getName());
        etComboDescription.setText(existingCombo.getDescription());
        etComboImageUrl.setText(existingCombo.getImageUrl());
        etComboOriginalPrice.setText(String.valueOf(existingCombo.getOriginalPrice()));
        etComboPrice.setText(String.valueOf(existingCombo.getComboPrice()));
        if (existingCombo.isLimitedTime()) {
            cbLimitedTime.setChecked(true);
            llDateRange.setVisibility(View.VISIBLE);
            updateDateRangeText(tvDateRange, existingCombo.getStartDate(), existingCombo.getEndDate());
        } else {
            cbLimitedTime.setChecked(false);
            llDateRange.setVisibility(View.GONE);
        }

        // Image preview
        Glide.with(this).load(existingCombo.getImageUrl()).placeholder(android.R.drawable.ic_menu_gallery).into(ivComboImagePreview);

        // Prepare selectedFoodIds and quantities
        final List<String> selectedFoodIds = new ArrayList<>();
        final Map<String, Integer> quantities = new HashMap<>();
        if (existingCombo.getFoodIds() != null) {
            selectedFoodIds.addAll(existingCombo.getFoodIds());
        }
        if (existingCombo.getQuantities() != null) {
            quantities.putAll(existingCombo.getQuantities());
        }
        tvSelectedItemsInfo.setText(selectedFoodIds.size() + " món đã chọn");

        // Food selection button updates tvSelectedItemsInfo when done
        btnSelectFoodItems.setOnClickListener(v -> showFoodSelectionDialog(selectedFoodIds, quantities, () -> tvSelectedItemsInfo.setText(selectedFoodIds.size() + " món đã chọn")));

        // Date pickers
        final Long[] startDate = {existingCombo.getStartDate()};
        final Long[] endDate = {existingCombo.getEndDate()};
        btnStartDate.setOnClickListener(v -> showDateTimePicker(date -> { startDate[0] = date; updateDateRangeText(tvDateRange, startDate[0], endDate[0]); }));
        btnEndDate.setOnClickListener(v -> showDateTimePicker(date -> { endDate[0] = date; updateDateRangeText(tvDateRange, startDate[0], endDate[0]); }));

        btnSaveCombo.setText("Cập nhật");
        btnSaveCombo.setOnClickListener(v -> {
            String name = etComboName.getText().toString().trim();
            String description = etComboDescription.getText().toString().trim();
            String imageUrl = etComboImageUrl.getText().toString().trim();
            String originalPriceStr = etComboOriginalPrice.getText().toString().trim();
            String comboPriceStr = etComboPrice.getText().toString().trim();

            if (name.isEmpty() || originalPriceStr.isEmpty() || comboPriceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedFoodIds.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 món ăn", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int originalPrice = Integer.parseInt(originalPriceStr);
                int comboPrice = Integer.parseInt(comboPriceStr);

                if (comboPrice >= originalPrice) {
                    Toast.makeText(this, "Giá combo phải nhỏ hơn giá gốc", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> comboData = new HashMap<>();
                comboData.put("name", name);
                comboData.put("description", description);
                comboData.put("imageUrl", imageUrl);
                comboData.put("originalPrice", originalPrice);
                comboData.put("comboPrice", comboPrice);
                comboData.put("foodIds", selectedFoodIds);
                comboData.put("quantities", quantities);
                comboData.put("active", existingCombo.isActive());
                comboData.put("isLimitedTime", cbLimitedTime.isChecked());
                comboData.put("startDate", cbLimitedTime.isChecked() ? startDate[0] : null);
                comboData.put("endDate", cbLimitedTime.isChecked() ? endDate[0] : null);
                comboData.put("createdAt", existingCombo.getCreatedAt());

                // Update existing document
                db.collection("combos").document(existingCombo.getId())
                        .set(comboData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Đã cập nhật combo: " + name, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadCombosFromFirebase();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá phải là số hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelCombo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDateTimePicker(OnDateSelectedListener listener) {
        android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(this);
        datePicker.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 23, 59, 59);
            listener.onDateSelected(calendar.getTimeInMillis());
        });
        datePicker.show();
    }

    private void updateDateRangeText(TextView tvDateRange, Long startDate, Long endDate) {
        if (startDate == null && endDate == null) {
            tvDateRange.setText("Chưa chọn");
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            String start = startDate != null ? sdf.format(new java.util.Date(startDate)) : "?";
            String end = endDate != null ? sdf.format(new java.util.Date(endDate)) : "?";
            tvDateRange.setText(start + " - " + end);
        }
    }

    private void showFoodSelectionDialog(List<String> selectedFoodIds, Map<String, Integer> quantities, Runnable onUpdate) {
        boolean[] checkedItems = new boolean[foodList.size()];
        String[] foodNames = new String[foodList.size()];

        for (int i = 0; i < foodList.size(); i++) {
            FoodItem item = foodList.get(i);
            foodNames[i] = item.getName() + " (" + item.getPrice() + " VNĐ)";
            checkedItems[i] = selectedFoodIds.contains(item.getDocumentId());
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn món ăn cho combo")
                .setMultiChoiceItems(foodNames, checkedItems, (dialog, which, isChecked) -> {
                    FoodItem item = foodList.get(which);
                    if (isChecked) {
                        if (!selectedFoodIds.contains(item.getDocumentId())) {
                            selectedFoodIds.add(item.getDocumentId());
                            quantities.put(item.getDocumentId(), 1);
                        }
                    } else {
                        selectedFoodIds.remove(item.getDocumentId());
                        quantities.remove(item.getDocumentId());
                    }
                })
                .setPositiveButton("Xong", (dialog, which) -> {
                    if (onUpdate != null) onUpdate.run();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    interface OnDateSelectedListener {
        void onDateSelected(long timestamp);
    }
}