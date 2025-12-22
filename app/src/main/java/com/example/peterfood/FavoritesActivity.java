package com.example.peterfood;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity {
    private static final String TAG = "FavoritesActivity";
    private RecyclerView rvFavoritesFull;
    private MenuAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Set<String> favoritesSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvFavoritesFull = findViewById(R.id.rvFavoritesFull);
        rvFavoritesFull.setLayoutManager(new LinearLayoutManager(this));

        android.view.View btnBack = findViewById(R.id.btnBackFavorites);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem yêu thích", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFavoritesAndShow();
    }

    private void loadFavoritesAndShow() {
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    @SuppressWarnings("unchecked")
                    List<String> favIds = (List<String>) doc.get("favorites");
                    if (favIds == null || favIds.isEmpty()) return;
                    // fetch NewFoodDB and combos
                    final List<FoodItem> items = new ArrayList<>();
                    // try whereIn in batches of 10
                    List<String> batch = new ArrayList<>();
                    for (String id : favIds) batch.add(id);
                    // We'll attempt to fetch from NewFoodDB first
                    db.collection("NewFoodDB").whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                            .get().addOnSuccessListener(q -> {
                        for (QueryDocumentSnapshot d : q) {
                            try {
                                String id = d.getId();
                                String name = d.getString("name");
                                Number price = (Number) d.get("price");
                                Number salePrice = (Number) d.get("salePrice");
                                String imageUrl = d.getString("imageUrl");
                                Number rating = (Number) d.get("rating");
                                FoodItem fi = new FoodItem(id, name, d.getString("description"), price != null ? price.intValue() : 0, imageUrl != null ? imageUrl : "", rating != null ? rating.intValue() : 0, salePrice != null ? salePrice.intValue() : null);
                                items.add(fi);
                            } catch (Exception e) { Log.w(TAG, "parse error", e); }
                        }
                        // For any ids not found in NewFoodDB, try combos
                        for (String id : favIds) {
                            boolean found = false;
                            for (FoodItem f : items) if (id.equals(f.getDocumentId())) { found = true; break; }
                            if (!found) {
                                db.collection("combos").document(id).get().addOnSuccessListener(docc -> {
                                    if (docc.exists()) {
                                        try {
                                            ComboItem combo = docc.toObject(ComboItem.class);
                                            if (combo != null) {
                                                combo.setId(docc.getId());
                                                items.add(new FoodItem(combo));
                                            }
                                        } catch (Exception e) { Log.w(TAG, "combo parse", e); }
                                    }
                                    // finalize adapter once done (this simple approach may add adapter multiple times but OK)
                                    runOnUiThread(() -> {
                                        adapter = new MenuAdapter(items, FavoritesActivity.this, item -> {
                                            // open MenuActivity and ask it to open this item
                                            Intent i = new Intent(FavoritesActivity.this, MenuActivity.class);
                                            i.putExtra("open_item", item.getDocumentId());
                                            startActivity(i);
                                        }, null, (itm, ns) -> {
                                            // refresh current view after change
                                            loadFavoritesAndShow();
                                        }, false);
                                        rvFavoritesFull.setAdapter(adapter);
                                    });
                                });
                            }
                        }
                        // set adapter for items already found
                        if (!items.isEmpty()) {
                            adapter = new MenuAdapter(items, FavoritesActivity.this, item -> {
                                Intent i = new Intent(FavoritesActivity.this, MenuActivity.class);
                                i.putExtra("open_item", item.getDocumentId());
                                startActivity(i);
                            }, null, (itm, ns) -> loadFavoritesAndShow(), false);
                            rvFavoritesFull.setAdapter(adapter);
                        }
                    }).addOnFailureListener(e -> Log.e(TAG, "fav load err", e));
                }).addOnFailureListener(e -> Log.e(TAG, "user doc err", e));
    }
}
