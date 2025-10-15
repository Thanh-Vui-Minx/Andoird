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
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_menu);

        // Tải logo tự động từ BaseActivity
        loadLogo();

        rvMenu = findViewById(R.id.rvMenu);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnGoToCart = findViewById(R.id.btnGoToCart);
        Button btnAddFood = findViewById(R.id.btnAddFood);
        etSearch = findViewById(R.id.etSearch);

        if (rvMenu == null || btnLogout == null || btnGoToCart == null || btnAddFood == null || etSearch == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy view", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Một hoặc nhiều view bị null");
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
        Log.d(TAG, "Role in MenuActivity: " + role);
        if (!"admin".equals(role)) {
            btnAddFood.setVisibility(View.GONE);
            Toast.makeText(this, "Chỉ admin được quản lý sản phẩm", Toast.LENGTH_SHORT).show();
        } else {
            btnAddFood.setVisibility(View.VISIBLE);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { filterFoodList(s.toString()); }
        });

        loadFoodList();

        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Button Đăng xuất clicked");
            mAuth.signOut();
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            startActivity(intent);
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
        EditText etReview = dialog.findViewById(R.id.etReview);
        Button btnSubmitReview = dialog.findViewById(R.id.btnSubmitReview);
        TextView tvComments = dialog.findViewById(R.id.tvComments);
        LinearLayout llCommentSection = dialog.findViewById(R.id.llCommentSection);
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
        if (!item.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivImage);
        }

        // Cài đặt Spinner cho size
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.food_sizes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSize.setAdapter(adapter);
        spSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSize = parent.getItemAtPosition(position).toString();
                int price = item.getSizePrices().getOrDefault(selectedSize, item.getPrice());
                tvSelectedPrice.setText("Giá đã chọn (" + selectedSize + "): " + price + " VNĐ");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvSelectedPrice.setText("Giá đã chọn: " + item.getPrice() + " VNĐ");
            }
        });
        tvSelectedPrice.setText("Giá đã chọn (Small): " + item.getPrice() + " VNĐ");

        // Xử lý đánh giá cho user
        if ("user".equals(role)) {
            etReview.setVisibility(View.VISIBLE);
            btnSubmitReview.setVisibility(View.VISIBLE);
            btnSubmitReview.setOnClickListener(v -> {
                String review = etReview.getText().toString().trim();
                if (!review.isEmpty()) {
                    Map<String, Object> comment = new HashMap<>();
                    comment.put("userId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "Anonymous");
                    comment.put("comment", review);
                    comment.put("approved", false); // Mặc định chưa duyệt
                    item.getComments().add(comment);
                    db.collection("NewFoodDB").document(item.getDocumentId())
                            .update("comments", item.getComments())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đánh giá đã gửi, chờ duyệt!", Toast.LENGTH_SHORT).show();
                                etReview.setText("");
                                loadFoodList(); // Cập nhật lại để hiển thị bình luận
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(this, "Vui lòng nhập đánh giá!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Hiển thị và duyệt bình luận cho admin
        if ("admin".equals(role)) {
            llCommentSection.setVisibility(View.VISIBLE);
            tvComments.setVisibility(View.VISIBLE);
            for (Map<String, Object> comment : item.getComments()) {
                TextView commentView = new TextView(this);
                String userId = (String) comment.get("userId");
                String text = (String) comment.get("comment");
                boolean approved = comment.get("approved") != null && (boolean) comment.get("approved");
                commentView.setText(userId + ": " + text + " (Đã duyệt: " + approved + ")");
                commentView.setPadding(0, 8, 0, 8);
                llCommentSection.addView(commentView);

                Button btnApprove = new Button(this);
                btnApprove.setText(approved ? "Ẩn" : "Duyệt");
                btnApprove.setOnClickListener(v -> {
                    comment.put("approved", !approved);
                    db.collection("NewFoodDB").document(item.getDocumentId())
                            .update("comments", item.getComments())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cập nhật trạng thái bình luận!", Toast.LENGTH_SHORT).show();
                                llCommentSection.removeAllViews();
                                showFoodDetailDialog(item); // Tải lại dialog để cập nhật
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
                llCommentSection.addView(btnApprove);
            }
        }

        if (!"admin".equals(role)) {
            btnDeleteFood.setVisibility(View.GONE);
            btnEditFood.setVisibility(View.GONE);
        } else {
            btnDeleteFood.setVisibility(View.VISIBLE);
            btnEditFood.setVisibility(View.VISIBLE);
        }

        btnAddToCart.setOnClickListener(v -> {
            String selectedSize = spSize.getSelectedItem().toString();
            int price = item.getSizePrices().getOrDefault(selectedSize, item.getPrice());
            CartItem cartItem = new CartItem(item.getName() + " (" + selectedSize + ")", 1, price);
            CartManager.getInstance().addToCart(cartItem);
            Toast.makeText(this, "Đã thêm " + item.getName() + " (" + selectedSize + ") vào giỏ hàng", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnDeleteFood.setOnClickListener(v -> {
            new AlertDialog.Builder(MenuActivity.this)
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
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi khi xóa món ăn: " + e.getMessage(), e);
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        btnEditFood.setOnClickListener(v -> showEditFoodDialog(item));
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
        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription); // Thêm trường mới
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice); // Giá Medium
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice); // Giá Large
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        if (!"admin".equals(role)) {
            etFoodSalePrice.setVisibility(View.GONE);
            etMediumPrice.setVisibility(View.GONE);
            etLargePrice.setVisibility(View.GONE);
        }

        btnSaveFood.setOnClickListener(v -> {
            String name = etFoodName.getText().toString().trim();
            String description = etFoodDescription.getText().toString().trim();
            String priceStr = etFoodPrice.getText().toString().trim();
            String salePriceStr = etFoodSalePrice.getText().toString().trim();
            String imageUrl = etFoodImageUrl.getText().toString().trim();
            String ratingStr = etFoodRating.getText().toString().trim();
            String detailedDescription = etDetailedDescription.getText().toString().trim();
            String mediumPriceStr = etMediumPrice.getText().toString().trim();
            String largePriceStr = etLargePrice.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || ratingStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ Tên, Giá và Đánh giá", Toast.LENGTH_SHORT).show();
                return;
            }

            int price, rating;
            Integer salePrice = null, mediumPrice = null, largePrice = null;
            try {
                price = Integer.parseInt(priceStr);
                rating = Integer.parseInt(ratingStr);
                if (!salePriceStr.isEmpty()) salePrice = Integer.parseInt(salePriceStr);
                if (!mediumPriceStr.isEmpty()) mediumPrice = Integer.parseInt(mediumPriceStr);
                if (!largePriceStr.isEmpty()) largePrice = Integer.parseInt(largePriceStr);
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

            Map<String, Integer> sizePrices = new HashMap<>();
            sizePrices.put("Small", price);
            if (mediumPrice != null) sizePrices.put("Medium", mediumPrice);
            if (largePrice != null) sizePrices.put("Large", largePrice);

            Map<String, Object> foodData = new HashMap<>();
            foodData.put("name", name);
            foodData.put("description", description.isEmpty() ? "Không có mô tả" : description);
            foodData.put("price", price);
            foodData.put("salePrice", salePrice);
            foodData.put("imageUrl", imageUrl.isEmpty() ? "" : imageUrl);
            foodData.put("rating", rating);
            foodData.put("detailedDescription", detailedDescription.isEmpty() ? "Chưa có mô tả chi tiết" : detailedDescription);
            foodData.put("sizePrices", sizePrices);
            foodData.put("comments", new ArrayList<>());

            db.collection("NewFoodDB")
                    .add(foodData)
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

        EditText etFoodName = dialog.findViewById(R.id.etFoodName);
        EditText etFoodDescription = dialog.findViewById(R.id.etFoodDescription);
        EditText etFoodPrice = dialog.findViewById(R.id.etFoodPrice);
        EditText etFoodSalePrice = dialog.findViewById(R.id.etFoodSalePrice);
        EditText etFoodImageUrl = dialog.findViewById(R.id.etFoodImageUrl);
        EditText etFoodRating = dialog.findViewById(R.id.etFoodRating);
        EditText etDetailedDescription = dialog.findViewById(R.id.etDetailedDescription);
        EditText etMediumPrice = dialog.findViewById(R.id.etMediumPrice);
        EditText etLargePrice = dialog.findViewById(R.id.etLargePrice);
        Button btnSaveFood = dialog.findViewById(R.id.btnSaveFood);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        etFoodName.setText(item.getName());
        etFoodDescription.setText(item.getDescription());
        etFoodPrice.setText(String.valueOf(item.getPrice()));
        etFoodSalePrice.setText(item.getSalePrice() != null ? String.valueOf(item.getSalePrice()) : "");
        etFoodImageUrl.setText(item.getImageUrl());
        etFoodRating.setText(String.valueOf(item.getRating()));
        etDetailedDescription.setText(item.getDetailedDescription());
        etMediumPrice.setText(String.valueOf(item.getSizePrices().getOrDefault("Medium", 0)));
        etLargePrice.setText(String.valueOf(item.getSizePrices().getOrDefault("Large", 0)));

        if (!"admin".equals(role)) {
            etFoodSalePrice.setVisibility(View.GONE);
            etMediumPrice.setVisibility(View.GONE);
            etLargePrice.setVisibility(View.GONE);
        }

        btnSaveFood.setText("Cập nhật");

        btnSaveFood.setOnClickListener(v -> {
            String name = etFoodName.getText().toString().trim();
            String description = etFoodDescription.getText().toString().trim();
            String priceStr = etFoodPrice.getText().toString().trim();
            String salePriceStr = etFoodSalePrice.getText().toString().trim();
            String imageUrl = etFoodImageUrl.getText().toString().trim();
            String ratingStr = etFoodRating.getText().toString().trim();
            String detailedDescription = etDetailedDescription.getText().toString().trim();
            String mediumPriceStr = etMediumPrice.getText().toString().trim();
            String largePriceStr = etLargePrice.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || ratingStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ Tên, Giá và Đánh giá", Toast.LENGTH_SHORT).show();
                return;
            }

            int price, rating;
            Integer salePrice = null, mediumPrice = null, largePrice = null;
            try {
                price = Integer.parseInt(priceStr);
                rating = Integer.parseInt(ratingStr);
                if (!salePriceStr.isEmpty()) salePrice = Integer.parseInt(salePriceStr);
                if (!mediumPriceStr.isEmpty()) mediumPrice = Integer.parseInt(mediumPriceStr);
                if (!largePriceStr.isEmpty()) largePrice = Integer.parseInt(largePriceStr);
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

            Map<String, Integer> sizePrices = new HashMap<>();
            sizePrices.put("Small", price);
            if (mediumPrice != null) sizePrices.put("Medium", mediumPrice);
            if (largePrice != null) sizePrices.put("Large", largePrice);

            Map<String, Object> foodData = new HashMap<>();
            foodData.put("name", name);
            foodData.put("description", description.isEmpty() ? "Không có mô tả" : description);
            foodData.put("price", price);
            foodData.put("salePrice", salePrice);
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
                            Map<String, Integer> sizePrices = (Map<String, Integer>) doc.get("sizePrices");
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