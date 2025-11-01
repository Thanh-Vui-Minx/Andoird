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

    public VoucherAdapter(List<Voucher> list, Context ctx) {
        this.list = list;
        this.ctx = ctx;
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
        String detail = (v.isPercent() ? ("-" + v.getValue() + "%") : ("-" + v.getValue() + " VNĐ"));
        if (v.getExpiryMillis() > 0) {
            DateFormat df = DateFormat.getDateInstance();
            detail += " đến " + df.format(new Date(v.getExpiryMillis()));
        }
        holder.tvDetail.setText(detail);
        holder.toggleActive.setChecked(v.isActive());

        holder.toggleActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("vouchers").document(v.getId()).update("active", isChecked)
                    .addOnSuccessListener(aVoid -> Log.i("VoucherAdapter", "Toggled voucher " + v.getCode()))
                    .addOnFailureListener(e -> {
                        Log.e("VoucherAdapter", "Failed toggle: " + e.getMessage(), e);
                        holder.toggleActive.setChecked(!isChecked); // revert
                    });
        });

        holder.btnDelete.setOnClickListener(view -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("Xóa voucher")
                    .setMessage("Bạn có chắc muốn xóa voucher " + v.getCode() + "?")
                    .setPositiveButton("Xóa", (d, which) -> {
                        db.collection("vouchers").document(v.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    list.remove(position);
                                    notifyItemRemoved(position);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("VoucherAdapter", "Failed delete: " + e.getMessage(), e);
                                });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvDetail;
        ToggleButton toggleActive;
        Button btnDelete;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvVoucherCode);
            tvDetail = itemView.findViewById(R.id.tvVoucherDetail);
            toggleActive = itemView.findViewById(R.id.toggleActive);
            btnDelete = itemView.findViewById(R.id.btnDeleteVoucher);
        }
    }
}
