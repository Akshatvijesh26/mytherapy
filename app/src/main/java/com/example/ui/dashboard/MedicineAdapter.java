package com.example.ui.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.data.model.Medicine;
import com.example.databinding.ItemMedicineBinding;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying medicines in Ongoing and Passed sections.
 */
public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    public interface OnMedicineActionListener {
        void onToggleStatus(@NonNull Medicine medicine);
        void onDeleteMedicine(@NonNull Medicine medicine);
    }

    private final List<Medicine> items = new ArrayList<>();
    private final OnMedicineActionListener listener;

    public MedicineAdapter(@NonNull OnMedicineActionListener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<Medicine> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicineBinding binding = ItemMedicineBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MedicineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class MedicineViewHolder extends RecyclerView.ViewHolder {

        private final ItemMedicineBinding binding;

        MedicineViewHolder(@NonNull ItemMedicineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull final Medicine medicine) {
            Context context = itemView.getContext();

            binding.tvMedicineName.setText(medicine.getMedicineName());
            binding.tvTimingRelation.setText(medicine.getTimingRelation());
            binding.tvFrequency.setText(medicine.getFrequencyPerDay() + "x per day");
            binding.tvTimeSlots.setText(medicine.getTimeSlots());
            String reminderTimes = medicine.getReminderTime();
            if (!TextUtils.isEmpty(reminderTimes) && reminderTimes.contains(",")) {
                binding.tvReminderTime.setText("Doses: " + reminderTimes);
            } else {
                binding.tvReminderTime.setText("Dose at " + reminderTimes);
            }

            // Load photo or set fallback
            loadMedicinePhoto(medicine.getPhotoUri());

            // Configure status action button and appearance
            if (medicine.isPassed()) {
                // Passed/Completed appearance
                binding.btnToggleStatus.setText("Completed ✓");
                binding.btnToggleStatus.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.secondary_container)
                );
                binding.btnToggleStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.on_secondary_container)
                );
                binding.btnToggleStatus.setIconTint(
                        ContextCompat.getColorStateList(context, R.color.on_secondary_container)
                );
                binding.tvTimingRelation.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.secondary_container)
                );
                binding.tvTimingRelation.setTextColor(
                        ContextCompat.getColor(context, R.color.on_secondary_container)
                );
            } else {
                // Ongoing (upcoming) appearance
                binding.btnToggleStatus.setText("Mark Taken");
                binding.btnToggleStatus.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.primary_container)
                );
                binding.btnToggleStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.primary)
                );
                binding.btnToggleStatus.setIconTint(
                        ContextCompat.getColorStateList(context, R.color.primary)
                );
                binding.tvTimingRelation.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.primary_container)
                );
                binding.tvTimingRelation.setTextColor(
                        ContextCompat.getColor(context, R.color.on_primary_container)
                );
            }

            binding.btnToggleStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleStatus(medicine);
                }
            });

            binding.btnDeleteMedicine.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteMedicine(medicine);
                }
            });
        }

        private void loadMedicinePhoto(String photoUri) {
            if (TextUtils.isEmpty(photoUri)) {
                binding.ivMedicinePhoto.setImageResource(R.drawable.ic_medication);
                return;
            }

            try {
                if (photoUri.startsWith("file://") || photoUri.startsWith("/")) {
                    File file = new File(photoUri.replace("file://", ""));
                    if (file.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bitmap != null) {
                            binding.ivMedicinePhoto.setImageBitmap(bitmap);
                            return;
                        }
                    }
                } else if (photoUri.startsWith("content://")) {
                    Uri uri = Uri.parse(photoUri);
                    InputStream is = itemView.getContext().getContentResolver().openInputStream(uri);
                    if (is != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        is.close();
                        if (bitmap != null) {
                            binding.ivMedicinePhoto.setImageBitmap(bitmap);
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // Fallback
            binding.ivMedicinePhoto.setImageResource(R.drawable.ic_medication);
        }
    }
}
