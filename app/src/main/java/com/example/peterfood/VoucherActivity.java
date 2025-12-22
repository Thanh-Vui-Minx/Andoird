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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
    private java.util.Map<String, Voucher> voucherMap = new java.util.HashMap<>();
    private FirebaseFirestore db;
    private TextView tvEmpty;
    private String role;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher_list);

        db = FirebaseFirestore.getInstance();

    rvVouchers = findViewById(R.id.rvVouchers);
    Button btnAdd = findViewById(R.id.btnAddVoucher);
    // Back button moved to bottom taskbar (btnBackVouchers)
    Button btnBack = findViewById(R.id.btnBackVouchers);
    role = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("role", "user");
        tvEmpty = findViewById(R.id.tvEmptyVouchers);

    adapter = new VoucherAdapter(vouchers, this);
        rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        rvVouchers.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            if ("admin".equals(role)) {
                showAddVoucherDialog();
            } else {
                showApplyVoucherDialog();
            }
        });
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Show voucher list to all roles (users will have limited controls)
        rvVouchers.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);

    loadVouchers();
    }

    /**
     * For regular users: allow entering only a voucher code and validate it against Firestore.
     * If valid (active and not expired) show details; otherwise show a Toast error.
     */
    private void showApplyVoucherDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Áp dụng voucher");
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Nhập mã voucher");
        b.setView(et);
        b.setPositiveButton("Áp dụng", (dialog, which) -> {
            String code = et.getText().toString().trim();
            if (code.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập mã voucher", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("vouchers")
                    .whereEqualTo("code", code)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            // User requests disabled: simply inform the user the voucher is not valid
                            android.widget.Toast.makeText(this, "Voucher thêm vào không hợp lệ", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        Boolean active = doc.getBoolean("active");
                        Number expiry = (Number) doc.get("expiryMillis");
                        long expiryMillis = expiry != null ? expiry.longValue() : 0L;
                        long now = System.currentTimeMillis();
                        if (active == null || !active) {
                            android.widget.Toast.makeText(this, "Voucher không hợp lệ", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (expiryMillis > 0 && expiryMillis < now) {
                            android.widget.Toast.makeText(this, "Voucher đã hết hạn", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // For regular users: add this voucher to their account (claimed vouchers)
                        String uid = null;
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        if (uid == null) {
                            android.widget.Toast.makeText(this, "Vui lòng đăng nhập để áp dụng voucher", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String voucherId = doc.getId();
                        // make final copies for lambda capture
                        final String voucherIdFinal = voucherId;
                        final String uidFinal = uid;
                        // Prevent duplicate claims: read user doc and check existing vouchers
                        db.collection("users").document(uidFinal).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        @SuppressWarnings("unchecked")
                                        List<String> existing = (List<String>) userDoc.get("vouchers");
                                        if (existing != null && existing.contains(voucherIdFinal)) {
                                            android.widget.Toast.makeText(VoucherActivity.this, "Bạn đã có voucher này trong tài khoản", android.widget.Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }
                                    // add voucher id to users/{uid}.vouchers array
                                    db.collection("users").document(uidFinal)
                                            .update("vouchers", com.google.firebase.firestore.FieldValue.arrayUnion(voucherIdFinal))
                                            .addOnSuccessListener(aVoid -> {
                                                android.widget.Toast.makeText(VoucherActivity.this, "Voucher đã được thêm vào tài khoản của bạn", android.widget.Toast.LENGTH_SHORT).show();
                                                // refresh the user's claimed list immediately
                                                loadVouchers();
                                            })
                                            .addOnFailureListener(e -> {
                                                android.widget.Toast.makeText(VoucherActivity.this, "Không thể thêm voucher vào tài khoản: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> android.widget.Toast.makeText(VoucherActivity.this, "Lỗi truy vấn tài khoản: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> android.widget.Toast.makeText(this, "Lỗi kiểm tra voucher: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());
        });
        b.setNegativeButton("Hủy", null);
        b.show();
    }

    private void submitVoucherRequest(String code) {
        // User-request feature disabled. Inform and log.
        Log.i(TAG, "submitVoucherRequest called but feature disabled. code=" + code);
        android.widget.Toast.makeText(this, "Chức năng gửi yêu cầu voucher đã bị tắt.", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void savePendingVoucherLocally(String code, String uid) throws JSONException {
        android.content.SharedPreferences prefs = getSharedPreferences("peterfood_prefs", MODE_PRIVATE);
        String raw = prefs.getString("pending_vouchers", null);
        JSONArray arr = raw != null ? new JSONArray(raw) : new JSONArray();
        JSONObject o = new JSONObject();
        o.put("code", code);
        if (uid != null) o.put("createdBy", uid);
        o.put("ts", System.currentTimeMillis());
        arr.put(o);
        prefs.edit().putString("pending_vouchers", arr.toString()).apply();
    }

    private void retryPendingVouchers() {
        android.content.SharedPreferences prefs = getSharedPreferences("peterfood_prefs", MODE_PRIVATE);
        String raw = prefs.getString("pending_vouchers", null);
        if (raw == null) return;
        try {
            JSONArray arr = new JSONArray(raw);
            if (arr.length() == 0) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String code = o.optString("code", null);
                String createdBy = o.optString("createdBy", null);
                long ts = o.optLong("ts", 0L);
                if (code == null) continue;
                // ensure current user signed in before retrying
                String currentUid = null;
                if (FirebaseAuth.getInstance().getCurrentUser() != null) currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (currentUid == null) {
                    Log.i(TAG, "Skipping retry for pending voucher (user not signed in): code=" + code);
                    continue; // leave pending for later when user signs in
                }
                Map<String, Object> doc = new HashMap<>();
                doc.put("code", code);
                doc.put("percent", false);
                doc.put("value", 0);
                doc.put("expiryMillis", 0);
                doc.put("active", false);
                // override createdBy with current authenticated uid to satisfy security rules
                doc.put("createdBy", currentUid);
                doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                final String matchCode = code;
                final long matchTs = ts;
                db.collection("voucher_requests").add(doc)
                        .addOnSuccessListener(ref -> {
                            // remove this pending entry from prefs
                            removePendingVoucherEntry(matchCode, matchTs);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Retry add voucher failed: " + e.getMessage(), e);
                        });
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing pending vouchers: " + e.getMessage(), e);
        }
    }

    private void removePendingVoucherEntry(String code, long ts) {
        android.content.SharedPreferences prefs = getSharedPreferences("peterfood_prefs", MODE_PRIVATE);
        String raw = prefs.getString("pending_vouchers", null);
        if (raw == null) return;
        try {
            JSONArray arr = new JSONArray(raw);
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String c = o.optString("code", null);
                long t = o.optLong("ts", 0L);
                if (code != null && code.equals(c) && t == ts) {
                    // skip (remove)
                } else {
                    out.put(o);
                }
            }
            prefs.edit().putString("pending_vouchers", out.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error removing pending voucher entry: " + e.getMessage(), e);
        }
    }

    private void loadVouchers() {

        // Load live-updating lists for official vouchers and user requests (admin)
        // For regular users we will instead load only claimed vouchers from users/{uid}.vouchers
        voucherMap.clear();

        if (!"admin".equals(role)) {
            // user flow: load vouchers the user has claimed
            String uid = null;
            if (FirebaseAuth.getInstance().getCurrentUser() != null) uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (uid == null) return;
            vouchers.clear();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        List<String> claimed = (List<String>) documentSnapshot.get("vouchers");
                        if (claimed == null || claimed.isEmpty()) {
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(android.view.View.VISIBLE);
                            return;
                        }
                        tvEmpty.setVisibility(android.view.View.GONE);
                        // fetch each voucher document
                        vouchers.clear();
                        for (String vid : claimed) {
                            db.collection("vouchers").document(vid).get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
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
                                                    active != null ? active : false);
                                            v.setPendingRequest(false);
                                            vouchers.add(v);
                                            adapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching claimed voucher: " + e.getMessage(), e));
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error loading user claimed vouchers: " + e.getMessage(), e));
            return;
        }

        db.collection("vouchers").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "Error listening vouchers: " + e.getMessage(), e);
                    return;
                }
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
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
                            v.setPendingRequest(false);
                            voucherMap.put("v:" + id, v);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing voucher: " + ex.getMessage(), ex);
                        }
                    }
                }
                // debug log all voucher docs received
                if (snapshots != null) {
                    for (DocumentSnapshot d : snapshots.getDocuments()) {
                        Log.i(TAG, "Voucher doc: id=" + d.getId() + " data=" + d.getData());
                    }
                }
                rebuildListFromMap();
            }
        });

        // Note: user-submitted voucher requests feature disabled — admin only sees official vouchers.
    }

    private void rebuildListFromMap() {
        vouchers.clear();
        vouchers.addAll(voucherMap.values());
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(vouchers.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
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

    // Open edit dialog pre-filled with the voucher values. If the voucher is a pending request,
    // saving will create an official voucher in `vouchers` and remove the request doc.
    public void showEditVoucherDialog(Voucher v) {
        if (v == null) return;
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

        // Prefill
        etCode.setText(v.getCode());
        if (v.isPercent()) {
            rbPercent.setChecked(true);
            rbFixed.setChecked(false);
        } else {
            rbPercent.setChecked(false);
            rbFixed.setChecked(true);
        }
        etValue.setText(String.valueOf(v.getValue()));
        if (v.getExpiryMillis() > 0) {
            java.text.DateFormat df = java.text.DateFormat.getDateInstance();
            etExpiry.setText(df.format(new java.util.Date(v.getExpiryMillis())));
            etExpiry.setTag(v.getExpiryMillis());
        }

        etExpiry.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, dayOfMonth, 0, 0, 0);
                etExpiry.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth));
                etExpiry.setTag(sel.getTimeInMillis());
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnSave.setOnClickListener(click -> {
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
            // For edited/promo items, admin decides active state by saving; default to true for created vouchers
            doc.put("active", true);

            if (v.isPendingRequest()) {
                // promote request -> create new voucher then delete request
                db.collection("vouchers").add(doc)
                        .addOnSuccessListener(ref -> {
                            // delete original request doc
                            db.collection("voucher_requests").document(v.getId()).delete()
                                    .addOnSuccessListener(a -> {
                                        Toast.makeText(this, "Đã tạo voucher chính thức và xóa yêu cầu", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        loadVouchers();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Tạo voucher thành công nhưng không thể xóa yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                        loadVouchers();
                                    });
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tạo voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                // update existing voucher
                db.collection("vouchers").document(v.getId()).update(doc)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Đã cập nhật voucher", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadVouchers();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        btnCancel.setOnClickListener(x -> dialog.dismiss());
        dialog.show();
    }
}
