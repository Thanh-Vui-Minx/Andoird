package com.example.peterfood;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VH> {

    private List<Voucher> list = new ArrayList<>();
    private Context ctx;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String role;

    public VoucherAdapter(List<Voucher> list, Context ctx) {
        this.list = list;
        this.ctx = ctx;
        this.role = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("role", "user");
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Voucher v = list.get(position);
        holder.tvCode.setText(v.getCode());
        holder.tvCode.setText(v.getCode() == null || v.getCode().isEmpty() ? "(không có mã)" : v.getCode());
        // separate the value and expiry into two lines
        String valueText = (v.isPercent() ? ("-" + v.getValue() + "%") : ("-" + v.getValue() + " VNĐ"));
        String expiryText;
        if (v.getExpiryMillis() > 0) {
            DateFormat df = DateFormat.getDateInstance();
            expiryText = "đến " + df.format(new Date(v.getExpiryMillis()));
        } else {
            expiryText = "Không hết hạn";
        }
        if (v.isPendingRequest()) {
            expiryText += "  (Yêu cầu từ user)";
        }
        holder.tvValue.setText(valueText);
        holder.tvExpiry.setText(expiryText);
        holder.toggleActive.setChecked(v.isActive());

        // Only admins can change active state for official vouchers (not pending requests)
        if ("admin".equals(role) && !v.isPendingRequest()) {
            holder.toggleActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                db.collection("vouchers").document(v.getId()).update("active", isChecked)
                        .addOnSuccessListener(aVoid -> Log.i("VoucherAdapter", "Toggled voucher " + v.getCode()))
                        .addOnFailureListener(e -> {
                            Log.e("VoucherAdapter", "Failed toggle: " + e.getMessage(), e);
                            android.widget.Toast.makeText(ctx, "Không có quyền thay đổi voucher: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                            holder.toggleActive.setChecked(!isChecked); // revert
                        });
            });
            holder.toggleActive.setEnabled(true);
        } else {
            // regular users and pending requests cannot toggle
            holder.toggleActive.setOnCheckedChangeListener(null);
            holder.toggleActive.setEnabled(false);
        }

        // Edit button: visible only to admin
        if ("admin".equals(role)) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(view -> {
                if (ctx instanceof VoucherActivity) {
                    ((VoucherActivity) ctx).showEditVoucherDialog(v);
                } else {
                    android.widget.Toast.makeText(ctx, "Không thể mở chế độ sửa ở đây", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(view -> {
            if (!"admin".equals(role)) {
                android.widget.Toast.makeText(ctx, "Bạn không có quyền xóa voucher", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(ctx)
                    .setTitle("Xóa voucher")
                    .setMessage("Bạn có chắc muốn xóa voucher " + v.getCode() + "?")
                    .setPositiveButton("Xóa", (d, which) -> {
                        // delete from vouchers or voucher_requests depending on item type
                        String collection = v.isPendingRequest() ? "voucher_requests" : "vouchers";
                        db.collection(collection).document(v.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    list.remove(position);
                                    notifyItemRemoved(position);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("VoucherAdapter", "Failed delete: " + e.getMessage(), e);
                                    android.widget.Toast.makeText(ctx, "Không thể xóa voucher: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvValue, tvExpiry;
        ToggleButton toggleActive;
        Button btnDelete;
        Button btnEdit;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvVoucherCode);
            tvValue = itemView.findViewById(R.id.tvVoucherValue);
            tvExpiry = itemView.findViewById(R.id.tvVoucherExpiry);
            toggleActive = itemView.findViewById(R.id.toggleActive);
            btnDelete = itemView.findViewById(R.id.btnDeleteVoucher);
            btnEdit = itemView.findViewById(R.id.btnEditVoucher);
        }
    }
}
