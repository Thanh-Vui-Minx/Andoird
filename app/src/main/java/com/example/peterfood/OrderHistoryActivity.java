package com.example.peterfood;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrderHistoryActivity extends AppCompatActivity {

    private static final String TAG = "OrderHistoryActivity";
    private RecyclerView rvOrders;
    private TextView tvEmptyOrders;
    private Button btnBackOrderHistory;
    private OrderAdapter adapter;
    private List<Order> orders = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        rvOrders = findViewById(R.id.rvOrders);
        tvEmptyOrders = findViewById(R.id.tvEmptyOrders);
    btnBackOrderHistory = findViewById(R.id.btnBackOrderHistory);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        adapter = new OrderAdapter(orders, this::showOrderDetailsDialog);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);

        loadOrders();

        if (btnBackOrderHistory != null) {
            btnBackOrderHistory.setOnClickListener(v -> finish());
        }
    }

    private void loadOrders() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvEmptyOrders.setText("Vui lòng đăng nhập để xem lịch sử");
            tvEmptyOrders.setVisibility(android.view.View.VISIBLE);
            return;
        }

        db.collection("orders")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    orders.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String id = doc.getId();
                            Map<String, Object> data = doc.getData();
                            Long total = data.get("total") instanceof Number ? ((Number) data.get("total")).longValue() : 0L;
                            String paymentMethod = data.get("paymentMethod") != null ? data.get("paymentMethod").toString() : "-";
                            String paymentStatus = data.get("paymentStatus") != null ? data.get("paymentStatus").toString() : "-";
                            String address = data.get("address") != null ? data.get("address").toString() : "-";
                            String bankRef = data.get("bankRef") != null ? data.get("bankRef").toString() : "-";
                            Date created = data.get("createdAt") instanceof com.google.firebase.Timestamp ? ((com.google.firebase.Timestamp) data.get("createdAt")).toDate() : new Date();
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

                            Order o = new Order(id, created, total, paymentMethod, paymentStatus, address, bankRef, items != null ? items : new ArrayList<>());
                            orders.add(o);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing order: " + e.getMessage(), e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (orders.isEmpty()) {
                        tvEmptyOrders.setVisibility(android.view.View.VISIBLE);
                    } else {
                        tvEmptyOrders.setVisibility(android.view.View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders: " + e.getMessage(), e);
                    tvEmptyOrders.setText("Lỗi khi tải lịch sử: " + e.getMessage());
                    tvEmptyOrders.setVisibility(android.view.View.VISIBLE);
                });
    }

    private void showOrderDetailsDialog(@NonNull Order order) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_order_details);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvId = dialog.findViewById(R.id.tvOrderIdDetail);
        TextView tvDate = dialog.findViewById(R.id.tvOrderDateDetail);
        TextView tvAddress = dialog.findViewById(R.id.tvOrderAddress);
        TextView tvPayment = dialog.findViewById(R.id.tvOrderPayment);
        TextView tvBankRef = dialog.findViewById(R.id.tvOrderBankRef);
        LinearLayout container = dialog.findViewById(R.id.containerOrderItems);
        TextView tvTotal = dialog.findViewById(R.id.tvOrderTotalDetail);
        Button btnClose = dialog.findViewById(R.id.btnCloseOrderDetail);

        DateFormat df = DateFormat.getDateTimeInstance();
        tvId.setText("Đơn #" + order.getId());
        tvDate.setText(df.format(order.getCreatedAt()));
        tvAddress.setText("Địa chỉ: " + order.getAddress());
        tvPayment.setText("Phương thức: " + order.getPaymentMethod() + " — " + order.getPaymentStatus());
        tvBankRef.setText("Mã tham chiếu: " + (order.getBankRef() != null && !order.getBankRef().isEmpty() ? order.getBankRef() : "-"));
        tvTotal.setText("Tổng: " + order.getTotal() + " VNĐ");

        container.removeAllViews();
        for (Map<String, Object> item : order.getItems()) {
            String name = item.get("name") != null ? item.get("name").toString() : "";
            Number price = item.get("price") instanceof Number ? (Number) item.get("price") : 0;
            Number qty = item.get("quantity") instanceof Number ? (Number) item.get("quantity") : 1;
            String note = item.get("note") != null ? item.get("note").toString() : "";

            TextView tv = new TextView(this);
            tv.setText(name + " x" + qty.intValue() + " — " + price.intValue() + " VNĐ" + (note.isEmpty() ? "" : "\nGhi chú: " + note));
            tv.setPadding(0, 8, 0, 8);
            container.addView(tv);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
