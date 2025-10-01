package com.example.peterfood;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";
    private RecyclerView rvMenu;
    private MenuAdapter adapter;
    private List<FoodItem> foodList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_menu);

        rvMenu = findViewById(R.id.rvMenu);
        Button btnBack = findViewById(R.id.btnBackToMain);

        if (rvMenu == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy RecyclerView", Toast.LENGTH_LONG).show();
            Log.e(TAG, "RecyclerView is null");
            finish();
            return;
        }

        if (btnBack == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Quay lại", Toast.LENGTH_LONG).show();
            Log.e(TAG, "btnBack is null");
            finish();
            return;
        }
        // Trong onCreate, sau btnBack.setOnClickListener
        Button btnGoToCart = findViewById(R.id.btnGoToCart);
        if (btnGoToCart == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy nút Giỏ Hàng", Toast.LENGTH_LONG).show();
            Log.e(TAG, "btnGoToCart is null");
            finish();
            return;
        }

        btnGoToCart.setOnClickListener(v -> {
            Log.d(TAG, "Button Xem Giỏ Hàng clicked");
            Intent intent = new Intent(MenuActivity.this, CartActivity.class);
            startActivity(intent);
        });
        db = FirebaseFirestore.getInstance();
        foodList = new ArrayList<>();
        adapter = new MenuAdapter(foodList, this, item -> {
            // Hiển thị dialog chi tiết khi click item
            showFoodDetailDialog(item);
        });
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setAdapter(adapter);

        loadFoodList();

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Button Quay lại clicked");
            finish();
        });
    }

    private void showFoodDetailDialog(FoodItem item) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_food_details);

        // Ánh xạ các view trong dialog
        ImageView ivImage = dialog.findViewById(R.id.ivFoodImage);
        TextView tvName = dialog.findViewById(R.id.tvFoodName);
        TextView tvDescription = dialog.findViewById(R.id.tvFoodDescription);
        TextView tvPrice = dialog.findViewById(R.id.tvFoodPrice);
        TextView tvRating = dialog.findViewById(R.id.tvFoodRating);
        Button btnAddToCart = dialog.findViewById(R.id.btnAddToCart);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        // Gán dữ liệu
        tvName.setText(item.getName());
        tvDescription.setText(item.getDescription());
        tvPrice.setText("Giá: " + item.getPrice() + " VNĐ");
        tvRating.setText("Đánh giá: " + item.getRating() + "/5");
        if (!item.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivImage);
        }

        // Xử lý thêm vào giỏ hàng
        btnAddToCart.setOnClickListener(v -> {
            CartItem cartItem = new CartItem(item.getName(), 1, item.getPrice());
            CartManager.getInstance().addToCart(cartItem);
            Toast.makeText(this, "Đã thêm " + item.getName() + " vào giỏ hàng", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Đóng dialog
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadFoodList() {
        Log.d(TAG, "Starting to load food list from NewFoodDB");
        db.collection("NewFoodDB")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Query success, documents found: " + queryDocumentSnapshots.size());
                    foodList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String name = doc.getString("name");
                            String description = doc.getString("description");
                            Number price = (Number) doc.get("price");
                            Number rating = (Number) doc.get("rating");
                            String imageUrl = doc.getString("imageUrl");

                            Log.d(TAG, "Processing document: " + doc.getId() + ", name: " + name);

                            if (name != null && price != null) {
                                FoodItem item = new FoodItem(
                                        name,
                                        description != null ? description : "Không có mô tả",
                                        price.intValue(),
                                        imageUrl != null ? imageUrl : "",
                                        rating != null ? rating.intValue() : 0
                                );
                                foodList.add(item);
                            } else {
                                Log.w(TAG, "Missing required fields in document: " + doc.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document", e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + foodList.size() + " items");
                    if (foodList.isEmpty()) {
                        Toast.makeText(MenuActivity.this, "Không có món ăn nào để hiển thị", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting food items: " + e.getMessage(), e);
                    Toast.makeText(MenuActivity.this, "Lỗi khi tải dữ liệu từ Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}