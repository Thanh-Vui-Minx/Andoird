package com.example.peterfood;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
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

    private static final String TAG = "MenuActivity";
    private RecyclerView rvMenu;
    private MenuAdapter adapter;
    private List<FoodItem> foodList;
    private List<FoodItem> filteredFoodList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String role;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        loadLogo();

        rvMenu = findViewById(R.id.rvMenu);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        ImageButton btnGoToCart = findViewById(R.id.btnGoToCart);
        ImageButton btnAddFood = findViewById(R.id.btnAddFood);
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
            btnAddFood.setVisibility(View.GONE);
        } else {
            btnAddFood.setVisibility(View.VISIBLE);
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
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_food_details);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView ivImage = dialog.findViewById(R.id.ivFoodImage);
        TextView tvName = dialog.findViewById(R.id.tvFoodName);
        TextView tvDetailedDescription = dialog.findViewById(R.id.tvDetailedDescription);
        TextView tvPrice = dialog.findViewById(R.id.tvFoodPrice);
        TextView tvSalePrice = dialog.findViewById(R.id.tvFoodSalePrice);
        TextView tvRating = dialog.findViewById(R.id.tvFoodRating);
        Spinner spSize = dialog.findViewById(R.id.spSize);
        TextView tvSelectedPrice = dialog.findViewById(R.id.tvSelectedPrice);
        EditText etNote = dialog.findViewById(R.id.etNote); // GHI CHÚ MỚI
        Button btnAddToCart = dialog.findViewById(R.id.btnAddToCart);
        Button btnDeleteFood = dialog.findViewById(R.id.btnDeleteFood);
        Button btnEditFood = dialog.findViewById(R.id.btnEditFood);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        tvName.setText(item.getName());
        tvDetailedDescription.setText(item.getDetailedDescription().isEmpty() ? "Chưa có mô tả chi tiết." : item.getDetailedDescription());
        tvPrice.setText("Giá cơ bản: " + item.getPrice() + " VNĐ");
        if (item.getSalePrice() != null && item.getSalePrice() < item.getPrice()) {
            tvSalePrice.setText("Giá giảm: " + item.getSalePrice() + " VNĐ");
            tvSalePrice.setVisibility(View.VISIBLE);
        } else {
            tvSalePrice.setVisibility(View.GONE);
        }
        tvRating.setText("Đánh giá: " + item.getRating() + "/5");

        Log.d(TAG, "FoodItem ImageUrl: " + item.getImageUrl());
        if (!item.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivImage);
        }

        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(this,
                R.array.food_sizes, android.R.layout.simple_spinner_item);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSize.setAdapter(sizeAdapter);
        spSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSize = parent.getItemAtPosition(position).toString();
                Long price = item.getSizePrices().get(selectedSize);
                int finalPrice = (price != null) ? price.intValue() : item.getPrice();
                tvSelectedPrice.setText("Giá đã chọn (" + selectedSize + "): " + finalPrice + " VNĐ");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvSelectedPrice.setText("Giá đã chọn (Small): " + item.getPrice() + " VNĐ");
            }
        });
        tvSelectedPrice.setText("Giá đã chọn (Small): " + item.getPrice() + " VNĐ");

        // BỎ PHẦN BÌNH LUẬN HOÀN TOÀN
        // Không còn etReview, btnSubmitReview, llCommentSection

        if (!"admin".equals(role)) {
            btnDeleteFood.setVisibility(View.GONE);
            btnEditFood.setVisibility(View.GONE);
        } else {
            btnDeleteFood.setVisibility(View.VISIBLE);
            btnEditFood.setVisibility(View.VISIBLE);
        }

        // THÊM VÀO GIỎ + GHI CHÚ
        btnAddToCart.setOnClickListener(v -> {
            String selectedSize = spSize.getSelectedItem().toString();
            Long price = item.getSizePrices().get(selectedSize);
            int finalPrice = (price != null) ? price.intValue() : item.getPrice();
            String note = etNote.getText().toString().trim(); // LẤY GHI CHÚ

            CartItem cartItem = new CartItem(
                    item.getName() + " (" + selectedSize + ")",
                    finalPrice,
                    1,
                    item.getImageUrl(),
                    note // TRUYỀN GHI CHÚ
            );
            Log.d(TAG, "Adding to cart: " + cartItem.getName() + ", Note: " + note);
            addItemToCart(cartItem);
            Toast.makeText(this, "Đã thêm vào giỏ!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnDeleteFood.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa món " + item.getName() + "?")
                    .setPositiveButton("Xóa", (dialogInner, which) -> {
                        db.collection("NewFoodDB").document(item.getDocumentId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã xóa món ăn: " + item.getName(), Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadFoodList();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        btnEditFood.setOnClickListener(v -> showEditFoodDialog(item));
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void addItemToCart(CartItem newItem) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userDoc = db.collection("users").document(mAuth.getCurrentUser().getUid());
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("name", newItem.getName());
        itemData.put("price", newItem.getPrice());
        itemData.put("quantity", newItem.getQuantity());
        itemData.put("imageUrl", newItem.getImageUrl());
        itemData.put("note", newItem.getNote()); // LƯU GHI CHÚ

        Log.d(TAG, "Saving cart item: " + newItem.getName() + ", Note: " + newItem.getNote());

        userDoc.update("cart", FieldValue.arrayUnion(itemData))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cart item saved with note: " + newItem.getNote());
                    Toast.makeText(this, "Đã thêm vào giỏ!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi add item: " + e.getMessage());
                    Toast.makeText(this, "Lỗi thêm vào giỏ", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddFoodDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_food);

        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice);
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        btnSaveFood.setOnClickListener(v -> {
            String name = etFoodName.getText().toString().trim();
            String description = etFoodDescription.getText().toString().trim();
            String detailedDescription = etDetailedDescription.getText().toString().trim();
            String priceStr = etFoodPrice.getText().toString().trim();
            String salePriceStr = etFoodSalePrice.getText().toString().trim();
            String imageUrl = etFoodImageUrl.getText().toString().trim();
            String ratingStr = etFoodRating.getText().toString().trim();
            String mediumPriceStr = etMediumPrice.getText().toString().trim();
            String largePriceStr = etLargePrice.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || ratingStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ Tên, Giá và Đánh giá", Toast.LENGTH_SHORT).show();
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
            foodData.put("comments", new ArrayList<>()); // Giữ lại nếu cần sau

            db.collection("NewFoodDB")
                    .add(foodData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Added food: " + name);
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

        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice);
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        etFoodName.setText(item.getName());
        etFoodDescription.setText(item.getDescription());
        etFoodPrice.setText(String.valueOf(item.getPrice()));
        etFoodSalePrice.setText(item.getSalePrice() != null ? String.valueOf(item.getSalePrice()) : "");
        etFoodImageUrl.setText(item.getImageUrl());
        etFoodRating.setText(String.valueOf(item.getRating()));
        etDetailedDescription.setText(item.getDetailedDescription());
        etMediumPrice.setText(item.getSizePrices().get("Medium") != null ? String.valueOf(item.getSizePrices().get("Medium")) : "");
        etLargePrice.setText(item.getSizePrices().get("Large") != null ? String.valueOf(item.getSizePrices().get("Large")) : "");

        btnSaveFood.setText("Cập nhật");

        btnSaveFood.setOnClickListener(v -> {
            String name = etFoodName.getText().toString().trim();
            String description = etFoodDescription.getText().toString().trim();
            String detailedDescription = etDetailedDescription.getText().toString().trim();
            String priceStr = etFoodPrice.getText().toString().trim();
            String salePriceStr = etFoodSalePrice.getText().toString().trim();
            String imageUrl = etFoodImageUrl.getText().toString().trim();
            String ratingStr = etFoodRating.getText().toString().trim();
            String mediumPriceStr = etMediumPrice.getText().toString().trim();
            String largePriceStr = etLargePrice.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || ratingStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ Tên, Giá và Đánh giá", Toast.LENGTH_SHORT).show();
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

    private void loadFoodList() {
        db.collection("NewFoodDB")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    foodList.clear();
                    filteredFoodList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String name = doc.getString("name");
                            String description = doc.getString("description");
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
                                item.setDetailedDescription(detailedDescription != null ? detailedDescription : "");
                                item.setSizePrices(sizePrices != null ? sizePrices : new HashMap<>());
                                item.setComments(comments != null ? comments : new ArrayList<>());
                                foodList.add(item);
                                filteredFoodList.add(item);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document: " + e.getMessage(), e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (filteredFoodList.isEmpty()) {
                        Toast.makeText(this, "Không có món ăn nào để hiển thị", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting food items: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}