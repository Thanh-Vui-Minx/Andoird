package com.example.peterfood;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private static final String TAG = "CartActivity";
    private RecyclerView rvCart;
    private CartAdapter cartAdapter;
    private TextView tvTotalPrice;
    private Button btnCheckout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TextView tvDeliveryAddress;
    private List<CartItem> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        rvCart = findViewById(R.id.rvCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnCheckout = findViewById(R.id.btnCheckout);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);

        if (rvCart == null || tvTotalPrice == null || btnCheckout == null || tvDeliveryAddress == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy view", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Một hoặc nhiều view bị null");
            finish();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem giỏ hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCartFromFirebase();

        btnCheckout.setOnClickListener(v -> showCheckoutDialog());
    }

    private void loadCartFromFirebase() {
        DocumentReference userDoc = db.collection("users").document(currentUser.getUid());
        userDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<Map<String, Object>> cartData = (List<Map<String, Object>>) documentSnapshot.get("cart");
                cartItems.clear();
                if (cartData != null && !cartData.isEmpty()) {
                    for (Map<String, Object> itemData : cartData) {
                        String name = (String) itemData.get("name");
                        int price = itemData.get("price") != null ? ((Long) itemData.get("price")).intValue() : 0;
                        int quantity = itemData.get("quantity") != null ? ((Long) itemData.get("quantity")).intValue() : 1;
                        String imageUrl = (String) itemData.get("imageUrl"); // ✅ THÊM LOAD IMAGEURL
                        cartItems.add(new CartItem(name, price, quantity, imageUrl != null ? imageUrl : "")); // ✅ THÊM VÀO CONSTRUCTOR
                    }
                } else {
                    userDoc.update("cart", new ArrayList<>());
                    Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                }
                setupAdapter();
                updateTotalPrice();
                loadDeliveryAddress();
            } else {
                Map<String, Object> userData = new HashMap<>();
                userData.put("cart", new ArrayList<>());
                userDoc.set(userData);
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi load cart: " + e.getMessage());
            Toast.makeText(this, "Lỗi load giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAdapter() {
        cartAdapter = new CartAdapter(cartItems, this, position -> {
            cartItems.remove(position);
            cartAdapter.notifyDataSetChanged();
            updateTotalPrice(); // Sẽ gọi saveCartToFirebase()
        });
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(cartAdapter);
    }

    public void updateTotalPrice() {
        int total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        tvTotalPrice.setText("Tổng cộng: " + total + " VNĐ");
        saveCartToFirebase();
    }

    private void saveCartToFirebase() {
        DocumentReference userDoc = db.collection("users").document(currentUser.getUid());
        List<Map<String, Object>> cartData = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.getName());
            itemData.put("price", item.getPrice());
            itemData.put("quantity", item.getQuantity());
            itemData.put("imageUrl", item.getImageUrl()); // ✅ THÊM SAVE IMAGEURL
            cartData.add(itemData);
        }
        userDoc.update("cart", cartData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cart saved successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi save cart: " + e.getMessage());
                    Toast.makeText(this, "Lỗi save giỏ hàng", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDeliveryAddress() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String address = documentSnapshot.getString("address");
                            tvDeliveryAddress.setText("Địa chỉ giao hàng: " + (address != null ? address : "Chưa cập nhật"));
                        } else {
                            tvDeliveryAddress.setText("Địa chỉ giao hàng: Chưa cập nhật");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi tải địa chỉ: " + e.getMessage());
                        tvDeliveryAddress.setText("Địa chỉ giao hàng: Lỗi tải dữ liệu");
                    });
        }
    }

    private void showCheckoutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_checkout);

        TextView tvOrderSummary = dialog.findViewById(R.id.tvOrderSummary);
        TextView tvDeliveryAddressDialog = dialog.findViewById(R.id.tvDeliveryAddressDialog);
        EditText etUpdateAddress = dialog.findViewById(R.id.etUpdateAddress);
        Button btnConfirmOrder = dialog.findViewById(R.id.btnConfirmOrder);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String address = documentSnapshot.getString("address");
                            tvDeliveryAddressDialog.setText("Địa chỉ hiện tại: " + (address != null ? address : "Chưa cập nhật"));
                            etUpdateAddress.setText(address != null ? address : "");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi tải địa chỉ: " + e.getMessage()));
        }

        StringBuilder summary = new StringBuilder("Tóm tắt đơn hàng:\n");
        int total = 0;
        for (CartItem item : cartItems) {
            summary.append(item.getName()).append(" x").append(item.getQuantity())
                    .append(" = ").append(item.getTotalPrice()).append(" VNĐ\n");
            total += item.getTotalPrice();
        }
        summary.append("Tổng cộng: ").append(total).append(" VNĐ");
        tvOrderSummary.setText(summary.toString());

        btnConfirmOrder.setOnClickListener(v -> {
            String newAddress = etUpdateAddress.getText().toString().trim();
            if (TextUtils.isEmpty(newAddress)) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("address", newAddress);
            db.collection("users").document(currentUser.getUid())
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đơn hàng đã được đặt! Địa chỉ cập nhật thành: " + newAddress, Toast.LENGTH_SHORT).show();
                        cartItems.clear();
                        cartAdapter.notifyDataSetChanged();
                        saveCartToFirebase(); // ✅ Clear và save empty cart
                        dialog.dismiss();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi cập nhật địa chỉ: " + e.getMessage());
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}