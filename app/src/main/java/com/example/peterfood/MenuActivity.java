package com.example.peterfood;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

        db = FirebaseFirestore.getInstance();
        foodList = new ArrayList<>();
        adapter = new MenuAdapter(foodList, this);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setAdapter(adapter);

        loadFoodList(); // Gọi để tải dữ liệu

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Button Quay lại clicked");
            finish();
        });
    }

    private void loadFoodList() {
        Log.d(TAG, "Starting to load food list from NewFoodDB");
        db.collection("NewFoodDB") // Sử dụng collection đúng
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

                            Log.d(TAG, "Processing document: " + doc.getId() + ", name: " + name);

                            if (name != null && price != null) {
                                FoodItem item = new FoodItem(
                                        name,
                                        description != null ? description : "Không có mô tả",
                                        price.intValue(),
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