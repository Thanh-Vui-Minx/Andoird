package com.example.peterfood;

import java.util.stream.Collectors;  // Để dùng stream (nếu Java 8+)
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
    private String role;
    private EditText etSearch;
    private List<FoodItem> comboSectionList = new ArrayList<>();  // Combo riêng
    private List<FoodItem> regularFoodList = new ArrayList<>();   // Món thường

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
        adapter = new MenuAdapter(filteredFoodList, this, item -> showFoodDetailDialog(item));
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setAdapter(adapter);

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
        btnAddFood.setOnClickListener(v -> showAddFoodDialog());
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
        Log.d("Dialog", "showFoodDetailDialog called for: " + item.getName());

        // BỎ QUA HEADER
        if ("header_combo".equals(item.getDocumentId()) || "header_food".equals(item.getDocumentId())) {
            Log.d("Dialog", "Header clicked - ignored");
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_food_details);

        // === TÌM TẤT CẢ VIEW ===
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

        // === KIỂM TRA NULL ===
        if (tvFoodName == null || tvFoodPrice == null || btnAddToCart == null) {
            Log.e("Dialog", "Một số view bị null! Kiểm tra dialog_food_details.xml");
            Toast.makeText(this, "Lỗi layout dialog", Toast.LENGTH_LONG).show();
            return;
        }

        // === ẢNH ===
        Glide.with(this)
                .load(item.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivFoodImage);

        // === ĐÁNH GIÁ ===
        tvFoodRating.setText("Đánh giá: " + item.getRating() + "/5");

        // === BIẾN FINAL CHO LAMBDA ===
        final FoodItem finalItem = item;
        final Integer salePrice = item.getSalePrice();
        final int price = item.getPrice();

        // === XỬ LÝ COMBO ===
        if (item.isCombo() && item.getComboData() != null) {
            ComboItem combo = item.getComboData();

            tvFoodName.setText("COMBO " + combo.getName());
            tvDetailedDescription.setText(combo.getDescription());

            // GIÁ COMBO: KHÔNG GẠCH
            tvFoodPrice.setText(combo.getComboPrice() + " VNĐ");
            tvFoodPrice.setPaintFlags(0); // XÓA GẠCH

            // GIÁ GỐC: CÓ GẠCH
            tvFoodSalePrice.setText(combo.getOriginalPrice() + " VNĐ");
            tvFoodSalePrice.setPaintFlags(tvFoodSalePrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvFoodSalePrice.setVisibility(View.VISIBLE);

            // Ẩn size
            tvSelectedPrice.setVisibility(View.GONE);
            spSize.setVisibility(View.GONE);

            btnAddToCart.setOnClickListener(v -> {
                addComboToCart(combo);
                dialog.dismiss();
            });

        } else {
            // === MÓN ĂN THƯỜNG ===
            tvFoodName.setText(finalItem.getName());
            tvDetailedDescription.setText(finalItem.getDetailedDescription());

            // Giá gốc + giảm
            if (salePrice != null && salePrice < price) {
                tvFoodPrice.setText(salePrice + " VNĐ");
                tvFoodSalePrice.setText("Giá gốc: " + price + " VNĐ");
                tvFoodSalePrice.setPaintFlags(tvFoodSalePrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvFoodPrice.setText(price + " VNĐ");
                tvFoodSalePrice.setVisibility(View.GONE);
            }

            // === SPINNER SIZE ===
            Map<String, Long> sizePrices = finalItem.getSizePrices();
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

            // === THÊM VÀO GIỎ ===
            btnAddToCart.setOnClickListener(v -> {
                String size = (String) spSize.getSelectedItem();
                if (size == null) {
                    Toast.makeText(this, "Vui lòng chọn kích cỡ", Toast.LENGTH_SHORT).show();
                    return;
                }
                long selectedPrice = finalSizePrices.get(size);

                Map<String, Object> cartItem = new HashMap<>();
                cartItem.put("foodId", finalItem.getDocumentId());
                cartItem.put("name", finalItem.getName());
                cartItem.put("price", selectedPrice);
                cartItem.put("size", size);
                cartItem.put("imageUrl", finalItem.getImageUrl());
                if (!note[0].isEmpty()) cartItem.put("note", note[0]);

                db.collection("users").document(mAuth.getCurrentUser().getUid())
                        .update("cart", FieldValue.arrayUnion(cartItem))
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this, "Đã thêm: " + finalItem.getName() + " (" + size + ")", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        // === ADMIN: SỬA / XÓA ===
        if ("admin".equals(role)) {
            btnEditFood.setVisibility(View.VISIBLE);
            btnDeleteFood.setVisibility(View.VISIBLE);

            btnEditFood.setOnClickListener(v -> {
                dialog.dismiss();
                showEditFoodDialog(finalItem);
            });

            btnDeleteFood.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa món")
                        .setMessage("Xóa " + finalItem.getName() + "?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            db.collection("NewFoodDB").document(finalItem.getDocumentId())
                                    .delete()
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
            btnEditFood.setVisibility(View.GONE);
            btnDeleteFood.setVisibility(View.GONE);
        }

        // === NÚT ĐÓNG ===
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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

        // === THÊM SECTION COMBO ===
        if (!comboSectionList.isEmpty()) {
            // Tạo header COMBO (dùng documentId để phân biệt)
            FoodItem comboHeader = new FoodItem("header_combo", "COMBO HOT", "", 0, "", 0, null);
            comboHeader.setIsCombo(true);  // Đánh dấu là header
            filteredFoodList.add(comboHeader);
            filteredFoodList.addAll(comboSectionList);
        }

        // === THÊM SECTION MÓN ĂN ===
        FoodItem foodHeader = new FoodItem("header_food", "TẤT CẢ MÓN ĂN", "", 0, "", 0, null);
        foodHeader.setIsCombo(true);
        filteredFoodList.add(foodHeader);
        filteredFoodList.addAll(regularFoodList);
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

                                // CHỈ THÊM VÀO foodList & regularFoodList
                                foodList.add(item);
                                regularFoodList.add(item);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document: " + e.getMessage(), e);
                        }
                    }

                    // 3. SINH COMBO TỪ DỮ LIỆU THỰC
                    generateSmartCombos();  // ← Tạo combo → thêm vào comboSectionList

                    // 4. TẠO SECTION: COMBO + MÓN ĂN
                    updateMenuWithSections();

                    // 5. CẬP NHẬT ADAPTER
                    adapter.notifyDataSetChanged();

                    if (filteredFoodList.isEmpty()) {
                        Toast.makeText(this, "Không có món ăn nào", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting food items: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}