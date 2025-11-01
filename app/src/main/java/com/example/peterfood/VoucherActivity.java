package com.example.peterfood;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoucherActivity extends AppCompatActivity {

    private static final String TAG = "VoucherActivity";
    private RecyclerView rvVouchers;
    private VoucherAdapter adapter;
    private List<Voucher> vouchers = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher_list);

        db = FirebaseFirestore.getInstance();

    rvVouchers = findViewById(R.id.rvVouchers);
    Button btnAdd = findViewById(R.id.btnAddVoucher);
    // Back button moved to bottom taskbar (btnBackVouchers)
    Button btnBack = findViewById(R.id.btnBackVouchers);
        tvEmpty = findViewById(R.id.tvEmptyVouchers);

        adapter = new VoucherAdapter(vouchers, this);
        rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        rvVouchers.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddVoucherDialog());
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadVouchers();
    }

    private void loadVouchers() {
        vouchers.clear();
        db.collection("vouchers").get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String id = doc.getId();
                            String code = doc.getString("code");
                            Boolean percent = doc.getBoolean("percent");
                            Number value = (Number) doc.get("value");
                            Number expiry = (Number) doc.get("expiryMillis");
                            Boolean active = doc.getBoolean("active");

                            Voucher v = new Voucher(id,
                                    code != null ? code : "",
                                    percent != null ? percent : false,
                                    value != null ? value.intValue() : 0,
                                    expiry != null ? expiry.longValue() : 0L,
                                    active != null ? active : true);
                            vouchers.add(v);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing voucher: " + e.getMessage(), e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(vouchers.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading vouchers: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi tải voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddVoucherDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_voucher);
        Window w = dialog.getWindow();
        if (w != null) w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final EditText etCode = dialog.findViewById(R.id.etVoucherCode);
        final RadioButton rbPercent = dialog.findViewById(R.id.rbPercent);
        final RadioButton rbFixed = dialog.findViewById(R.id.rbFixed);
        final EditText etValue = dialog.findViewById(R.id.etVoucherValue);
        final EditText etExpiry = dialog.findViewById(R.id.etVoucherExpiry);
        Button btnSave = dialog.findViewById(R.id.btnSaveVoucher);
        Button btnCancel = dialog.findViewById(R.id.btnCancelVoucher);

        etExpiry.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, dayOfMonth, 0, 0, 0);
                etExpiry.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth));
                etExpiry.setTag(sel.getTimeInMillis());
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnSave.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (TextUtils.isEmpty(code)) { Toast.makeText(this, "Nhập mã voucher", Toast.LENGTH_SHORT).show(); return; }
            String valStr = etValue.getText().toString().trim();
            if (TextUtils.isEmpty(valStr)) { Toast.makeText(this, "Nhập giá trị", Toast.LENGTH_SHORT).show(); return; }
            int val;
            try { val = Integer.parseInt(valStr); } catch (NumberFormatException e) { Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show(); return; }
            boolean isPercent = rbPercent.isChecked();
            long expiry = 0L;
            if (etExpiry.getTag() != null && etExpiry.getTag() instanceof Long) expiry = (Long) etExpiry.getTag();

            Map<String, Object> doc = new HashMap<>();
            doc.put("code", code);
            doc.put("percent", isPercent);
            doc.put("value", val);
            doc.put("expiryMillis", expiry);
            doc.put("active", true);

            db.collection("vouchers").add(doc)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Đã thêm voucher", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadVouchers();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi thêm voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
