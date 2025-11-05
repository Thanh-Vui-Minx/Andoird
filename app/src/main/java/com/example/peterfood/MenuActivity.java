package com.example.peterfood;

import java.util.stream.Collectors;  // Để dùng stream (nếu Java 8+)
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
    private void generateSmartCombos() {
        List<FoodItem> foods = foodList.stream()
                .filter(item -> "thức ăn".equals(item.getTag()))
                .collect(Collectors.toList());
        List<FoodItem> drinks = foodList.stream()
                .filter(item -> "nước".equals(item.getTag()))
                .collect(Collectors.toList());

        comboList.clear();

        // Combo 1: 1 ăn + 1 nước → giảm 10%
        if (!foods.isEmpty() && !drinks.isEmpty()) {
            FoodItem food = foods.get(0);  // Lấy món đầu tiên (có thể randomize sau)
            FoodItem drink = drinks.get(0);
            int original = food.getPrice() + drink.getPrice();
            int comboPrice = (int) (original * 0.9);
            ComboItem combo = new ComboItem(
                    "combo_1_1",
                    food.getName() + " + " + drink.getName(),
                    "Combo đôi - Tiết kiệm 10%",
                    comboPrice,
                    original,
                    food.getImageUrl(),  // Ảnh từ món ăn chính
                    List.of(food.getDocumentId(), drink.getDocumentId()),
                    Map.of(food.getDocumentId(), 1, drink.getDocumentId(), 1)
            );
            comboList.add(combo);
        }

        // Combo 2: 2 ăn + 3 nước → giảm 15%
        if (foods.size() >= 2 && drinks.size() >= 3) {
            List<FoodItem> foodCombo = foods.subList(0, 2);
            List<FoodItem> drinkCombo = drinks.subList(0, 3);
            int original = foodCombo.stream().mapToInt(FoodItem::getPrice).sum() +
                    drinkCombo.stream().mapToInt(FoodItem::getPrice).sum();
            int comboPrice = (int) (original * 0.85);
            String name = String.join(" + ", foodCombo.stream().map(FoodItem::getName).collect(Collectors.toList())) +
                    " + " + String.join(" + ", drinkCombo.stream().map(FoodItem::getName).collect(Collectors.toList()));
            if (name.length() > 50) name = name.substring(0, 47) + "...";
            ComboItem combo = new ComboItem(
                    "combo_2_3",
                    name,
                    "Combo lớn - Tiết kiệm 15%",
                    comboPrice,
                    original,
                    foodCombo.get(0).getImageUrl(),
                    java.util.stream.Stream.concat(
                            foodCombo.stream().map(FoodItem::getDocumentId),
                            drinkCombo.stream().map(FoodItem::getDocumentId)
                    ).collect(Collectors.toList()),
                    java.util.stream.Stream.concat(
                            foodCombo.stream().map(f -> java.util.Map.entry(f.getDocumentId(), 1)),
                            drinkCombo.stream().map(d -> java.util.Map.entry(d.getDocumentId(), 1))
                    ).collect(Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue))
            );
            comboList.add(combo);
        }

        // Combo 3: 4 ăn + 6 nước → giảm 20%
        if (foods.size() >= 4 && drinks.size() >= 6) {
            List<FoodItem> foodCombo = foods.subList(0, 4);
            List<FoodItem> drinkCombo = drinks.subList(0, 6);
            int original = foodCombo.stream().mapToInt(FoodItem::getPrice).sum() +
                    drinkCombo.stream().mapToInt(FoodItem::getPrice).sum();
            int comboPrice = (int) (original * 0.8);
            ComboItem combo = new ComboItem(
                    "combo_4_6",
                    "Combo Gia Đình Siêu Tiết Kiệm",
                    "4 ăn + 6 nước - Tiết kiệm 20%",
                    comboPrice,
                    original,
                    foodCombo.get(0).getImageUrl(),
                    java.util.stream.Stream.concat(
                            foodCombo.stream().map(FoodItem::getDocumentId),
                            drinkCombo.stream().map(FoodItem::getDocumentId)
                    ).collect(Collectors.toList()),
                    java.util.stream.Stream.concat(
                            foodCombo.stream().map(f -> java.util.Map.entry(f.getDocumentId(), 1)),
                            drinkCombo.stream().map(d -> java.util.Map.entry(d.getDocumentId(), 1))
                    ).collect(Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue))
            );
            comboList.add(combo);
        }
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
    final EditText etReview = dialog.findViewById(R.id.etReview);
    final Button btnSubmitReview = dialog.findViewById(R.id.btnSubmitReview);
    final LinearLayout llCommentSection = dialog.findViewById(R.id.llCommentSection);
    final android.widget.RatingBar rbReviewRating = dialog.findViewById(R.id.rbReviewRating);
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

        // Show review input for logged-in non-admin users
        if (mAuth.getCurrentUser() != null && !"admin".equals(role)) {
            etReview.setVisibility(View.VISIBLE);
            btnSubmitReview.setVisibility(View.VISIBLE);
            rbReviewRating.setVisibility(View.VISIBLE);
        } else {
            etReview.setVisibility(View.GONE);
            btnSubmitReview.setVisibility(View.GONE);
            rbReviewRating.setVisibility(View.GONE);
        }

        // Render approved comments into llCommentSection
        llCommentSection.removeAllViews();
        List<Map<String, Object>> comments = item.getComments();
        if (comments != null && !comments.isEmpty()) {
            llCommentSection.setVisibility(View.VISIBLE);
            for (Map<String, Object> c : comments) {
                boolean approved = false;
                if (c.get("approved") instanceof Boolean) approved = (Boolean) c.get("approved");
                // show only approved comments to regular users; admins could see all but keep simple
                if (approved) {
                    String userId = c.get("userId") != null ? c.get("userId").toString() : "User";
                    String commentText = c.get("comment") != null ? c.get("comment").toString() : "";
                    String ratingStr = c.get("rating") != null ? String.valueOf(((Number) c.get("rating")).intValue()) : "0";
                    TextView tv = new TextView(this);
                    tv.setText("★" + ratingStr + " — " + commentText + "\n- " + userId);
                    tv.setPadding(0, 8, 0, 8);
                    llCommentSection.addView(tv);
                } else {
                    // if current user posted this comment and it's not approved yet, show pending
                    if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getUid().equals(String.valueOf(c.get("userId")))) {
                        TextView tv = new TextView(this);
                        tv.setText("(Đang chờ duyệt) " + (c.get("comment") != null ? c.get("comment").toString() : ""));
                        tv.setPadding(0, 8, 0, 8);
                        llCommentSection.addView(tv);
                    }
                }
            }
        } else {
            llCommentSection.setVisibility(View.GONE);
        }

        if (!"admin".equals(role)) {
            btnDeleteFood.setVisibility(View.GONE);
            btnEditFood.setVisibility(View.GONE);
        } else {
            btnDeleteFood.setVisibility(View.VISIBLE);
            btnEditFood.setVisibility(View.VISIBLE);
        }

        // Submit review handler
        btnSubmitReview.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
                return;
            }
            String reviewText = etReview.getText().toString().trim();
            int reviewRating = (int) rbReviewRating.getRating();
            if (reviewText.isEmpty() || reviewRating <= 0) {
                Toast.makeText(this, "Vui lòng nhập nội dung đánh giá và chọn số sao", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("userId", mAuth.getCurrentUser().getUid());
            commentMap.put("comment", reviewText);
            commentMap.put("approved", false); // admin will approve
            commentMap.put("rating", reviewRating);
            commentMap.put("timestamp", FieldValue.serverTimestamp());

            db.collection("NewFoodDB").document(item.getDocumentId())
                    .update("comments", FieldValue.arrayUnion(commentMap))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã gửi đánh giá. Vui lòng chờ duyệt.", Toast.LENGTH_SHORT).show();
                        // show pending locally
                        TextView tv = new TextView(this);
                        tv.setText("(Đang chờ duyệt) " + reviewText);
                        tv.setPadding(0, 8, 0, 8);
                        llCommentSection.setVisibility(View.VISIBLE);
                        llCommentSection.addView(tv, 0);
                        etReview.setText("");
                        rbReviewRating.setRating(0);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // THÊM VÀO GIỎ + GHI CHÚ
        btnAddToCart.setOnClickListener(v -> {
            if (item.isCombo()) {
                addComboToCart(item.getComboData());
            }
            else {
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
            }
            dialog.dismiss();
        });
        if (item.isCombo()) {
            tvDetailedDescription.setText("Gồm: " + item.getComboData().getDescription() + "\nTiết kiệm: " + (item.getSalePrice() - item.getPrice()) + " VNĐ");
            tvSalePrice.setVisibility(View.VISIBLE);  // Hiển thị giá gốc bị gạch
        }
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
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice);
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
        ImageView ivFoodImageAdd = dialog.findViewById(R.id.ivFoodImageAdd);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Spinner spFoodTag = dialog.findViewById(R.id.spFoodTag);
        // Preview ảnh khi nhập URL
        etFoodImageUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    Glide.with(MenuActivity.this)
                            .load(url)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
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
            String salePriceStr = etFoodSalePrice.getText().toString().trim();
            String imageUrl = etFoodImageUrl.getText().toString().trim();
            String ratingStr = etFoodRating.getText().toString().trim();
            String mediumPriceStr = etMediumPrice.getText().toString().trim();
            String largePriceStr = etLargePrice.getText().toString().trim();
            String tag = spFoodTag.getSelectedItem().toString();
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
            foodData.put("tag", tag);
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
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice);
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
    ImageView ivFoodImageAdd = dialog.findViewById(R.id.ivFoodImageAdd);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Spinner spFoodTag = dialog.findViewById(R.id.spFoodTag);

        etFoodName.setText(item.getName());
        etFoodDescription.setText(item.getDescription());
        etFoodPrice.setText(String.valueOf(item.getPrice()));
        etFoodSalePrice.setText(item.getSalePrice() != null ? String.valueOf(item.getSalePrice()) : "");
        etFoodImageUrl.setText(item.getImageUrl());
        // Load preview ảnh hiện tại
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivFoodImageAdd);
        }

        // Cập nhật preview khi sửa URL
        etFoodImageUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    Glide.with(MenuActivity.this)
                            .load(url)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .into(ivFoodImageAdd);
                } else {
                    ivFoodImageAdd.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        });
        int tagPosition = 0; // Default "thức ăn"
        if ("nước".equals(item.getTag())) {
            tagPosition = 1;
        }
        spFoodTag.setSelection(tagPosition);
        etFoodRating.setText(String.valueOf(item.getRating()));
        etDetailedDescription.setText(item.getDetailedDescription());
        etMediumPrice.setText(item.getSizePrices().get("Medium") != null ? String.valueOf(item.getSizePrices().get("Medium")) : "");
        etLargePrice.setText(item.getSizePrices().get("Large") != null ? String.valueOf(item.getSizePrices().get("Large")) : "");

        btnSaveFood.setText("Cập nhật");

        btnSaveFood.setOnClickListener(v -> {
            String name = etFoodName.getText().toString().trim();
            String description = etFoodDescription.getText().toString().trim();
            String detailedDescription = etDetailedDescription.getText().toString().trim();
            String tag = spFoodTag.getSelectedItem().toString();
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
            foodData.put("tag", tag);
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
                    foodList.clear();
                    filteredFoodList.clear();
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
                                item.setTag(tag != null ? tag : "thức ăn"); // Default nếu chưa có
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
                    // ... code cũ (parse foodList và filteredFoodList)

                    generateSmartCombos();  // Tạo combo từ dữ liệu

                    // Gộp combo vào filteredFoodList (hiển thị cuối menu)
                    for (ComboItem c : comboList) {
                        FoodItem comboAsFood = new FoodItem(c);
                        filteredFoodList.add(comboAsFood);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting food items: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
}