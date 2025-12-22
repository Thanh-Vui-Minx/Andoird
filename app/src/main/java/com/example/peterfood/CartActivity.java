package com.example.peterfood;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.firestore.FirebaseFirestoreException;

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

    // Attempt to resend any pending orders saved locally (from previous permission failures)
    retryPendingOrders();
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

    /**
     * Save a failed order locally (SharedPreferences) so it can be retried later.
     */
    private void savePendingOrderLocally(Map<String, Object> orderData) {
        SharedPreferences prefs = getSharedPreferences("peterfood_prefs", Context.MODE_PRIVATE);
        String pendingJson = prefs.getString("pending_orders", "[]");
        try {
            JSONArray arr = new JSONArray(pendingJson);
            // JSONObject supports constructing from a Map
            JSONObject obj = new JSONObject(orderData);
            arr.put(obj);
            prefs.edit().putString("pending_orders", arr.toString()).apply();
            Log.w(TAG, "Order saved locally to pending_orders (will retry later)");
        } catch (JSONException ex) {
            Log.e(TAG, "Không thể lưu đơn tạm: " + ex.getMessage(), ex);
        }
    }

    /**
     * Read pending orders from SharedPreferences and attempt to resend them to Firestore.
     * Successful sends are removed from the pending list.
     */
    private void retryPendingOrders() {
        if (currentUser == null) return;
        SharedPreferences prefs = getSharedPreferences("peterfood_prefs", Context.MODE_PRIVATE);
        String pendingJson = prefs.getString("pending_orders", "[]");
        try {
            final JSONArray arr = new JSONArray(pendingJson);
            if (arr.length() == 0) return;

            // We'll iterate and attempt to resend; on success remove that element and persist immediately
            for (int i = arr.length() - 1; i >= 0; i--) {
                final int idx = i;
                final JSONObject obj = arr.getJSONObject(i);
                try {
                    final Map<String, Object> orderData = new HashMap<>();
                    if (obj.has("userId")) orderData.put("userId", obj.getString("userId"));
                    if (obj.has("address")) orderData.put("address", obj.getString("address"));
                    if (obj.has("paymentMethod")) orderData.put("paymentMethod", obj.getString("paymentMethod"));
                    if (obj.has("paymentStatus")) orderData.put("paymentStatus", obj.getString("paymentStatus"));
                    if (obj.has("bankRef")) orderData.put("bankRef", obj.getString("bankRef"));
                    if (obj.has("total")) orderData.put("total", obj.getInt("total"));

                    // items
                    final List<Map<String, Object>> items = new ArrayList<>();
                    if (obj.has("items")) {
                        final JSONArray itemsArr = obj.getJSONArray("items");
                        for (int j = 0; j < itemsArr.length(); j++) {
                            JSONObject it = itemsArr.getJSONObject(j);
                            Map<String, Object> im = new HashMap<>();
                            if (it.has("name")) im.put("name", it.getString("name"));
                            if (it.has("price")) im.put("price", it.getInt("price"));
                            if (it.has("quantity")) im.put("quantity", it.getInt("quantity"));
                            if (it.has("note")) im.put("note", it.getString("note"));
                            if (it.has("imageUrl")) im.put("imageUrl", it.getString("imageUrl"));
                            items.add(im);
                        }
                    }
                    orderData.put("items", items);

                    // set createdAt now
                    orderData.put("createdAt", FieldValue.serverTimestamp());

                    // ensure userId matches current user for security
                    orderData.put("userId", currentUser.getUid());

                    // attempt to write
                    db.collection("orders").add(orderData)
                            .addOnSuccessListener(orderRef -> {
                                try {
                                    arr.remove(idx);
                                    // persist updated pending list immediately
                                    prefs.edit().putString("pending_orders", arr.toString()).apply();
                                    Log.i(TAG, "Pending order resent successfully: " + orderRef.getId());
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error removing pending order from array: " + ex.getMessage(), ex);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Retrying pending order failed: " + e.getMessage());
                            });
                } catch (JSONException je) {
                    Log.e(TAG, "Invalid pending order JSON: " + je.getMessage(), je);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse pending_orders: " + e.getMessage(), e);
        }
    }
    private void loadCartFromFirebase() {
        DocumentReference userDoc = db.collection("users").document(currentUser.getUid());
        userDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cartData = (List<Map<String, Object>>) documentSnapshot.get("cart");
                cartItems.clear();
                if (cartData != null && !cartData.isEmpty()) {
                    for (Map<String, Object> itemData : cartData) {
                        String name = (String) itemData.get("name");
                        int price = itemData.get("price") != null ? ((Long) itemData.get  ("price")).intValue() : 0;
                        int quantity = itemData.get("quantity") != null ? ((Long) itemData.get("quantity")).intValue() : 1;
                        String imageUrl = (String) itemData.get("imageUrl");
                        String note = (String) itemData.get("note"); // ĐỌC GHI CHÚ
                        Log.d(TAG, "Item: " + name + ", Price: " + price + ", Quantity: " + quantity + ", Note: " + note);
                        cartItems.add(new CartItem(name, price, quantity, imageUrl != null ? imageUrl : "", note != null ? note : ""));
                    }
                    setupAdapter();
                    updateTotalPrice();
                    loadDeliveryAddress();
                } else {
                    userDoc.update("cart", new ArrayList<>());
                    Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                    setupAdapter();
                    updateTotalPrice();
                    loadDeliveryAddress();
                }
            } else {
                Map<String, Object> userData = new HashMap<>();
                userData.put("cart", new ArrayList<>());
                userDoc.set(userData);
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                setupAdapter();
                updateTotalPrice();
                loadDeliveryAddress();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi load cart: " + e.getMessage());
            Toast.makeText(this, "Lỗi load giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAdapter() {
        if (rvCart != null) {
            cartAdapter = new CartAdapter(cartItems, this, position -> {
                // Xóa trong list
                cartItems.remove(position);
                cartAdapter.notifyItemRemoved(position);
                cartAdapter.notifyItemRangeChanged(position, cartItems.size());
                // Lưu ngay
                saveCartToFirebase();
                updateTotalPrice();
            });
            rvCart.setLayoutManager(new LinearLayoutManager(this));
            rvCart.setAdapter(cartAdapter);
        }
    }
    public void updateTotalPrice() {
        int total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        tvTotalPrice.setText("Tổng cộng: " + total + " VNĐ");
        saveCartToFirebase();
    }
    // THÊM HÀM NÀY VÀO LỚP CartActivity
    public void updateTotalPriceAndSave() {
        updateTotalPrice(); // Gọi hàm cũ
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

        final TextView tvOrderSummary = dialog.findViewById(R.id.tvOrderSummary);
        final TextView tvDeliveryAddressDialog = dialog.findViewById(R.id.tvDeliveryAddressDialog);
    final EditText etUpdateAddress = dialog.findViewById(R.id.etUpdateAddress);
    final EditText etRecipientName = dialog.findViewById(R.id.etRecipientName);
    final EditText etRecipientPhone = dialog.findViewById(R.id.etRecipientPhone);
        final android.widget.RadioGroup rgPaymentMethod = dialog.findViewById(R.id.rgPaymentMethod);
        final android.widget.RadioButton rbCOD = dialog.findViewById(R.id.rbCOD);
        final android.widget.RadioButton rbATM = dialog.findViewById(R.id.rbATM);
        final TextView tvBankInfo = dialog.findViewById(R.id.tvBankInfo);
        final EditText etBankRef = dialog.findViewById(R.id.etBankRef);
    final Button btnConfirmOrder = dialog.findViewById(R.id.btnConfirmOrder);
    final Button btnCancel = dialog.findViewById(R.id.btnCancel);
    final Button btnAddVoucher = dialog.findViewById(R.id.btnAddVoucher);
    final TextView tvAppliedVoucher = dialog.findViewById(R.id.tvAppliedVoucher);

        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String address = documentSnapshot.getString("address");
                            String fullname = documentSnapshot.getString("fullname");
                            String phone = documentSnapshot.getString("phone");

                            tvDeliveryAddressDialog.setText("Địa chỉ hiện tại: " + (address != null ? address : "Chưa cập nhật"));
                            etUpdateAddress.setText(address != null ? address : "");
                            etRecipientName.setText(fullname != null ? fullname : "");
                            etRecipientPhone.setText(phone != null ? phone : "");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi tải địa chỉ: " + e.getMessage()));
        }

    final StringBuilder summary = new StringBuilder("Tóm tắt đơn hàng:\n");
    int summaryTotal = 0;
    for (CartItem item : cartItems) {
        summary.append(item.getName()).append(" x").append(item.getQuantity())
            .append(" = ").append(item.getTotalPrice()).append(" VNĐ\n");
        summaryTotal += item.getTotalPrice();
    }
    summary.append("Tổng cộng: ").append(summaryTotal).append(" VNĐ");
        tvOrderSummary.setText(summary.toString());

        // voucher state for this checkout dialog
        final int[] currentTotal = new int[]{summaryTotal};
        final String[] appliedVoucherId = new String[]{null};
        final int[] appliedDiscount = new int[]{0};

    btnConfirmOrder.setOnClickListener(v -> {
            String newAddress = etUpdateAddress.getText().toString().trim();
            String recipientName = etRecipientName.getText().toString().trim();
            String recipientPhone = etRecipientPhone.getText().toString().trim();

            if (TextUtils.isEmpty(recipientName)) {
                Toast.makeText(this, "Vui lòng nhập tên người nhận", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(recipientPhone) || recipientPhone.length() < 7) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(newAddress)) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("address", newAddress);
            updateData.put("fullname", recipientName);
            updateData.put("phone", recipientPhone);
            // add to addresses array for quick access (merge will create array if missing)
            updateData.put("addresses", FieldValue.arrayUnion(newAddress));

            // Determine payment method
            String paymentMethod; // default
            String paymentStatus = "unpaid";
            String bankRef = "";
            if (rbATM.isChecked()) {
                paymentMethod = "ATM";
                paymentStatus = "pending_payment"; // waiting for bank transfer
                bankRef = etBankRef.getText().toString().trim();
            } else if (rbCOD.isChecked()) {
                paymentMethod = "COD";
                paymentStatus = "unpaid"; // will be paid on delivery
            } else {
                paymentMethod = "COD";
            }

            // build order data
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("userId", currentUser.getUid());
            orderData.put("address", newAddress);
            orderData.put("recipientName", recipientName);
            orderData.put("recipientPhone", recipientPhone);
            orderData.put("paymentMethod", paymentMethod);
            orderData.put("paymentStatus", paymentStatus);
            orderData.put("bankRef", bankRef);
            orderData.put("items", new ArrayList<>());
            int orderTotal = 0;
            for (CartItem item : cartItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("name", item.getName());
                itemMap.put("price", item.getPrice());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("note", item.getNote());
                itemMap.put("imageUrl", item.getImageUrl());
                ((List<Map<String, Object>>) orderData.get("items")).add(itemMap);
                orderTotal += item.getTotalPrice();
            }
            orderData.put("total", orderTotal - appliedDiscount[0]);
            if (appliedVoucherId[0] != null) orderData.put("voucherId", appliedVoucherId[0]);
            orderData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // update user address/phone/name then create order
            // Use set(..., SetOptions.merge()) so it won't fail if the user doc did not previously exist
            db.collection("users").document(currentUser.getUid())
                    .set(updateData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        // create order document
                        db.collection("orders").add(orderData)
                                .addOnSuccessListener(orderRef -> {
                                    Toast.makeText(this, "Đơn hàng đã được đặt! Phương thức: " + paymentMethod, Toast.LENGTH_SHORT).show();
                                    // clear cart on server then locally
                                    Map<String, Object> emptyCart = new HashMap<>();
                                    emptyCart.put("cart", new ArrayList<>());
                                    db.collection("users").document(currentUser.getUid())
                                            .set(emptyCart, SetOptions.merge())
                                            .addOnSuccessListener(aVoid2 -> {
                                                // If a voucher was applied and it should be consumed for a single use, remove it from user's claimed vouchers
                                                if (appliedVoucherId[0] != null) {
                                                    String usedVoucherId = appliedVoucherId[0];
                                                    // remove from user's claimed vouchers list
                                                    db.collection("users").document(currentUser.getUid())
                                                            .update("vouchers", com.google.firebase.firestore.FieldValue.arrayRemove(usedVoucherId))
                                                            .addOnSuccessListener(aVoid3 -> {
                                                                // check voucher doc for single-use flag and mark it inactive / record usedBy
                                                                db.collection("vouchers").document(usedVoucherId).get()
                                                                        .addOnSuccessListener(vDoc -> {
                                                                            if (vDoc.exists()) {
                                                                                Boolean singleUse = vDoc.getBoolean("singleUse");
                                                                                if (singleUse != null && singleUse) {
                                                                                    Map<String, Object> vUpdates = new HashMap<>();
                                                                                    vUpdates.put("active", false);
                                                                                    vUpdates.put("usedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUser.getUid()));
                                                                                    db.collection("vouchers").document(usedVoucherId).update(vUpdates);
                                                                                } else {
                                                                                    // Optionally record use by this user for analytics
                                                                                    db.collection("vouchers").document(usedVoucherId).update("usedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUser.getUid()));
                                                                                }
                                                                            }
                                                                        });
                                                            })
                                                            .addOnFailureListener(e -> Log.w(TAG, "Không thể xóa voucher khỏi tài khoản người dùng: " + e.getMessage()));
                                                }

                                                cartItems.clear();
                                                if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
                                                // also persist local clear
                                                saveCartToFirebase();
                                                dialog.dismiss();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Even if clearing server cart fails, clear local to avoid stuck state client-side
                                                Log.e(TAG, "Không thể xóa cart trên server: " + e.getMessage());
                                                cartItems.clear();
                                                if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
                                                saveCartToFirebase();
                                                dialog.dismiss();
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi tạo đơn hàng: " + e.getMessage(), e);
                                    // If permission denied, save order locally for retry and give clearer guidance
                                    if (e instanceof FirebaseFirestoreException) {
                                        FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
                                        try {
                                            if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                                savePendingOrderLocally(orderData);
                                                Toast.makeText(this, "Không có quyền ghi lên Firestore. Đơn hàng đã được lưu tạm. Vui lòng kiểm tra Firestore rules hoặc quyền người dùng.", Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                        } catch (Exception inner) {
                                            Log.e(TAG, "Lỗi khi kiểm tra mã lỗi Firestore: " + inner.getMessage(), inner);
                                        }
                                    }

                                    Toast.makeText(this, "Lỗi tạo đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi cập nhật địa chỉ: " + e.getMessage());
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Payment method toggle behaviour: show/hide bank info and ref field
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbATM) {
                tvBankInfo.setVisibility(android.view.View.VISIBLE);
                etBankRef.setVisibility(android.view.View.VISIBLE);
            } else {
                tvBankInfo.setVisibility(android.view.View.GONE);
                etBankRef.setVisibility(android.view.View.GONE);
            }
        });

        // Add voucher button: lets user pick from their claimed vouchers
        int finalSummaryTotal = summaryTotal;
        btnAddVoucher.setOnClickListener(v -> {
            Log.d(TAG, "btnAddVoucher clicked");
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để áp dụng voucher", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đang tải voucher...", Toast.LENGTH_SHORT).show();
            // fetch user's claimed voucher ids
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        List<String> claimed = (List<String>) doc.get("vouchers");
                        if (claimed == null || claimed.isEmpty()) {
                            Toast.makeText(this, "Bạn không có voucher đã thêm", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // fetch voucher docs
                        List<String> ids = new ArrayList<>(claimed);
                        final CharSequence[] items = new CharSequence[ids.size()];
                        final String[] vidMap = new String[ids.size()];
                        final int[] discountVals = new int[ids.size()];
                        final boolean[] isPercent = new boolean[ids.size()];
                        final int[] loaded = new int[]{0};

                        for (int i = 0; i < ids.size(); i++) {
                            final int idx = i;
                            final String vid = ids.get(i);
                            vidMap[idx] = vid;
                            // load each voucher doc
                            db.collection("vouchers").document(vid).get()
                                    .addOnSuccessListener(vdoc -> {
                                        if (vdoc.exists()) {
                                            String code = vdoc.getString("code");
                                            Boolean percent = vdoc.getBoolean("percent");
                                            Number value = (Number) vdoc.get("value");
                                            int val = value != null ? value.intValue() : 0;
                                            discountVals[idx] = val;
                                            isPercent[idx] = percent != null ? percent : false;
                                            items[idx] = code + " (" + (isPercent[idx] ? ("-" + val + "%") : ("-" + val + " VNĐ")) + ")";
                                        } else {
                                            items[idx] = "(đã xóa)";
                                        }
                                        loaded[0]++;
                                        if (loaded[0] == ids.size()) {
                                            // all loaded, show selection
                                            new android.app.AlertDialog.Builder(this)
                                                    .setTitle("Chọn voucher")
                                                    .setItems(items, (d, which) -> {
                                                        String chosenId = vidMap[which];
                                                        boolean pct = isPercent[which];
                                                        int val = discountVals[which];
                                                        int discount = 0;
                                                        if (pct) {
                                                            discount = (int) Math.round(currentTotal[0] * (val / 100.0));
                                                        } else {
                                                            discount = val;
                                                        }
                                                        appliedVoucherId[0] = chosenId;
                                                        appliedDiscount[0] = discount;
                                                        currentTotal[0] = finalSummaryTotal - discount;
                                                        tvAppliedVoucher.setVisibility(android.view.View.VISIBLE);
                                                        tvAppliedVoucher.setText("Voucher áp dụng: " + items[which] + " → Giảm: " + discount + " VNĐ\nTổng sau giảm: " + currentTotal[0] + " VNĐ");
                                                    })
                                                    .setNegativeButton("Hủy", null)
                                                    .show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        items[idx] = "(lỗi tải)";
                                        loaded[0]++;
                                        if (loaded[0] == ids.size()) {
                                            new android.app.AlertDialog.Builder(this)
                                                    .setTitle("Chọn voucher")
                                                    .setItems(items, (d, which) -> {
                                                        String chosenId = vidMap[which];
                                                        boolean pct = isPercent[which];
                                                        int val = discountVals[which];
                                                        int discount = 0;
                                                        if (pct) {
                                                            discount = (int) Math.round(currentTotal[0] * (val / 100.0));
                                                        } else {
                                                            discount = val;
                                                        }
                                                        appliedVoucherId[0] = chosenId;
                                                        appliedDiscount[0] = discount;
                                                        currentTotal[0] = finalSummaryTotal - discount;
                                                        tvAppliedVoucher.setVisibility(android.view.View.VISIBLE);
                                                        tvAppliedVoucher.setText("Voucher áp dụng: " + items[which] + " → Giảm: " + discount + " VNĐ\nTổng sau giảm: " + currentTotal[0] + " VNĐ");
                                                    })
                                                    .setNegativeButton("Hủy", null)
                                                    .show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Không thể tải voucher của bạn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        // make dialog full-width to match other screens
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}