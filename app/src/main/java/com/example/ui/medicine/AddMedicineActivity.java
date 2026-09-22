package com.example.ui.medicine;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.MyTherapyApp;
import com.example.R;
import com.example.data.model.Medicine;
import com.example.data.model.User;
import com.example.data.repository.MedicineRepository;
import com.example.databinding.ActivityAddMedicineBinding;
import com.example.databinding.ItemCustomDoseTimeBinding;
import com.example.util.MedicineReminderScheduler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

/**
 * Activity allowing users to add a new medication entry with a photo (gallery or camera),
 * frequency dropdown, time slot checkboxes, timing relation radio buttons, and exact notification time.
 * On save, schedules an exact local alarm with AlarmManager and persists to Room database.
 */
public class AddMedicineActivity extends AppCompatActivity {

    private static final String TAG = "AddMedicineActivity";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USER_NAME = "extra_user_name";

    @Inject
    MedicineRepository medicineRepository;

    private ActivityAddMedicineBinding binding;
    private long currentUserId = -1;
    private String currentUsername = "";
    private String selectedPhotoPath = null;

    private static class DoseTimeEntry {
        String label;
        int hour;
        int minute;

        DoseTimeEntry(String label, int hour, int minute) {
            this.label = label;
            this.hour = hour;
            this.minute = minute;
        }

        String getFormattedTime() {
            String amPm = hour >= 12 ? "PM" : "AM";
            int formattedHour = hour % 12;
            if (formattedHour == 0) formattedHour = 12;
            return String.format(Locale.getDefault(), "%02d:%02d %s", formattedHour, minute, amPm);
        }
    }

    private final List<DoseTimeEntry> doseTimeEntries = new ArrayList<>();

    // Frequency options for dropdown
    private static final String[] FREQUENCY_OPTIONS = new String[]{
            "1 time a day",
            "2 times a day",
            "3 times a day",
            "4 times a day",
            "5 times a day",
            "6 times a day"
    };

    // ActivityResultLauncher for Android Photo Picker
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    saveGalleryImageLocally(uri);
                }
            });

    // ActivityResultLauncher for Camera capture intent
    private final ActivityResultLauncher<Void> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    saveCameraBitmapLocally(bitmap);
                }
            });

    // Permission launcher for Android 13+ POST_NOTIFICATIONS
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notifications permission denied. Reminders may not appear in system bar.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMedicineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Perform Dagger injection with fallback
        try {
            MyTherapyApp.getInstance().getComponent().inject(this);
        } catch (Throwable ignored) {
        }
        if (medicineRepository == null) {
            medicineRepository = MyTherapyApp.getInstance().getMedicineRepository();
        }

        if (getIntent() != null) {
            currentUserId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
            currentUsername = getIntent().getStringExtra(EXTRA_USER_NAME);
            if (currentUsername == null) {
                currentUsername = "";
            }
        }

        if (currentUserId == -1) {
            com.example.util.SessionManager sessionManager = new com.example.util.SessionManager(this);
            User loggedIn = sessionManager.getLoggedInUser();
            if (loggedIn != null) {
                currentUserId = loggedIn.getId();
                if (currentUsername.isEmpty()) {
                    currentUsername = loggedIn.getUsername();
                }
            }
        }

        checkNotificationPermission();

        setupToolbar();
        setupPhotoOptions();
        setupFrequencyDropdown();
        setupTimeSlotsAndSeparateTimes();
        setupSaveButton();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupToolbar() {
        binding.toolbarAddMedicine.setNavigationOnClickListener(v -> finish());
        binding.toolbarAddMedicine.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save_medicine) {
                attemptSaveMedicine();
                return true;
            }
            return false;
        });
    }

    private void setupPhotoOptions() {
        // 1. Image picker from Gallery
        binding.btnSelectPhoto.setOnClickListener(v -> {
            pickMediaLauncher.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()
            );
        });

        // 2. Camera intent option to capture photo
        binding.btnTakePhoto.setOnClickListener(v -> {
            try {
                takePhotoLauncher.launch(null);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to launch camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGalleryImageLocally(@NonNull Uri sourceUri) {
        try {
            InputStream is = getContentResolver().openInputStream(sourceUri);
            if (is != null) {
                File storageDir = new File(getFilesDir(), "medicines");
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }

                File destFile = new File(storageDir, "med_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(destFile);

                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
                fos.close();
                is.close();

                selectedPhotoPath = destFile.getAbsolutePath();

                Bitmap bitmap = BitmapFactory.decodeFile(destFile.getAbsolutePath());
                if (bitmap != null) {
                    binding.ivPhotoPreview.setImageBitmap(bitmap);
                }
                Toast.makeText(this, "Photo attached successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save gallery image: " + e.getMessage(), e);
            Toast.makeText(this, "Could not load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCameraBitmapLocally(@NonNull Bitmap bitmap) {
        try {
            File storageDir = new File(getFilesDir(), "medicines");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            File destFile = new File(storageDir, "med_cam_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            selectedPhotoPath = destFile.getAbsolutePath();
            binding.ivPhotoPreview.setImageBitmap(bitmap);
            Toast.makeText(this, "Camera photo captured and attached", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save camera bitmap: " + e.getMessage(), e);
            Toast.makeText(this, "Could not save photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFrequencyDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                FREQUENCY_OPTIONS
        );
        binding.actvFrequencyDropdown.setAdapter(adapter);
        binding.actvFrequencyDropdown.setText(FREQUENCY_OPTIONS[0], false);

        binding.actvFrequencyDropdown.setOnItemClickListener((parent, view, position, id) -> {
            int desiredCount = position + 1; // 1 to 6
            adjustDoseTimesForFrequency(desiredCount);
        });
    }

    private void adjustDoseTimesForFrequency(int desiredCount) {
        if (doseTimeEntries.size() == desiredCount) {
            return;
        }

        if (desiredCount < doseTimeEntries.size()) {
            while (doseTimeEntries.size() > desiredCount) {
                doseTimeEntries.remove(doseTimeEntries.size() - 1);
            }
        } else {
            int[] defaultHours = new int[]{8, 13, 20, 10, 16, 22};
            String[] defaultNames = new String[]{"Morning", "Afternoon", "Night", "Mid-day", "Evening", "Bedtime"};

            while (doseTimeEntries.size() < desiredCount) {
                int nextIdx = doseTimeEntries.size();
                String label = nextIdx < defaultNames.length ? defaultNames[nextIdx] : "Custom Dose " + (nextIdx + 1);
                int hour = nextIdx < defaultHours.length ? defaultHours[nextIdx] : (8 + nextIdx * 2) % 24;
                doseTimeEntries.add(new DoseTimeEntry(label, hour, 0));
            }
        }
        syncCheckboxesWithEntries();
        renderDoseTimeList();
    }

    private void syncFrequencyWithDoseCount() {
        int count = doseTimeEntries.size();
        if (count >= 1 && count <= FREQUENCY_OPTIONS.length) {
            binding.actvFrequencyDropdown.setText(FREQUENCY_OPTIONS[count - 1], false);
        }
    }

    private void syncCheckboxesWithEntries() {
        boolean hasMorning = false;
        boolean hasAfternoon = false;
        boolean hasNight = false;

        for (DoseTimeEntry entry : doseTimeEntries) {
            if (entry.label.equalsIgnoreCase("Morning")) hasMorning = true;
            if (entry.label.equalsIgnoreCase("Afternoon")) hasAfternoon = true;
            if (entry.label.equalsIgnoreCase("Night")) hasNight = true;
        }

        binding.cbSlotMorning.setChecked(hasMorning);
        binding.cbSlotAfternoon.setChecked(hasAfternoon);
        binding.cbSlotNight.setChecked(hasNight);
    }

    private int getSelectedFrequency() {
        String selected = binding.actvFrequencyDropdown.getText() != null
                ? binding.actvFrequencyDropdown.getText().toString() : "";
        for (int i = 0; i < FREQUENCY_OPTIONS.length; i++) {
            if (FREQUENCY_OPTIONS[i].equalsIgnoreCase(selected)) {
                return i + 1;
            }
        }
        // Fallback parse first digit
        if (!TextUtils.isEmpty(selected)) {
            char firstChar = selected.charAt(0);
            if (Character.isDigit(firstChar)) {
                return Character.getNumericValue(firstChar);
            }
        }
        return Math.max(1, doseTimeEntries.size());
    }

    private void setupTimeSlotsAndSeparateTimes() {
        // Initialize default entry for Morning
        doseTimeEntries.clear();
        doseTimeEntries.add(new DoseTimeEntry("Morning", 8, 0));

        // Listen for checkbox changes to automatically sync default slots
        binding.cbSlotMorning.setOnCheckedChangeListener((buttonView, isChecked) -> onSlotCheckboxChanged("Morning", isChecked, 8, 0));
        binding.cbSlotAfternoon.setOnCheckedChangeListener((buttonView, isChecked) -> onSlotCheckboxChanged("Afternoon", isChecked, 13, 0));
        binding.cbSlotNight.setOnCheckedChangeListener((buttonView, isChecked) -> onSlotCheckboxChanged("Night", isChecked, 20, 0));

        // Add custom time button: immediately launches MaterialTimePicker to pick the exact custom time
        binding.btnAddTimeSlot.setOnClickListener(v -> {
            com.example.util.KeyboardUtils.hideKeyboard(this);
            int currentCount = doseTimeEntries.size();
            String customLabel = "Custom Dose " + (currentCount + 1);

            Calendar now = Calendar.getInstance();
            int defaultHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24;

            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(defaultHour)
                    .setMinute(0)
                    .setTitleText("Add Custom Reminder Time")
                    .build();

            picker.addOnPositiveButtonClickListener(dialogView -> {
                int hour = picker.getHour();
                int minute = picker.getMinute();
                DoseTimeEntry newEntry = new DoseTimeEntry(customLabel, hour, minute);
                doseTimeEntries.add(newEntry);
                syncFrequencyWithDoseCount();
                renderDoseTimeList();
                Toast.makeText(this, "Added: " + customLabel + " (" + newEntry.getFormattedTime() + ")", Toast.LENGTH_SHORT).show();
            });

            picker.show(getSupportFragmentManager(), "ADD_CUSTOM_TIME_PICKER");
        });

        renderDoseTimeList();
    }

    private void onSlotCheckboxChanged(String slotName, boolean isChecked, int defaultHour, int defaultMinute) {
        if (isChecked) {
            // Check if already present
            boolean found = false;
            for (DoseTimeEntry entry : doseTimeEntries) {
                if (entry.label.equalsIgnoreCase(slotName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                doseTimeEntries.add(new DoseTimeEntry(slotName, defaultHour, defaultMinute));
            }
        } else {
            // Remove the slot
            for (int i = 0; i < doseTimeEntries.size(); i++) {
                if (doseTimeEntries.get(i).label.equalsIgnoreCase(slotName)) {
                    doseTimeEntries.remove(i);
                    break;
                }
            }
        }
        syncFrequencyWithDoseCount();
        renderDoseTimeList();
    }

    private void renderDoseTimeList() {
        binding.containerDoseTimes.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < doseTimeEntries.size(); i++) {
            final int index = i;
            final DoseTimeEntry entry = doseTimeEntries.get(i);

            ItemCustomDoseTimeBinding itemBinding = ItemCustomDoseTimeBinding.inflate(
                    inflater,
                    binding.containerDoseTimes,
                    false
            );

            itemBinding.tvDoseLabel.setText(entry.label);
            itemBinding.btnDoseTime.setText(entry.getFormattedTime());

            // Tapping on label or edit icon allows renaming the dose
            itemBinding.layoutDoseLabel.setOnClickListener(v -> {
                com.example.util.KeyboardUtils.hideKeyboard(this);
                showRenameDoseDialog(entry);
            });

            // On clicking time button, open MaterialTimePicker for this specific dose
            itemBinding.btnDoseTime.setOnClickListener(v -> {
                com.example.util.KeyboardUtils.hideKeyboard(this);
                MaterialTimePicker picker = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_12H)
                        .setHour(entry.hour)
                        .setMinute(entry.minute)
                        .setTitleText("Change Time for " + entry.label)
                        .build();

                picker.addOnPositiveButtonClickListener(dialogView -> {
                    entry.hour = picker.getHour();
                    entry.minute = picker.getMinute();
                    itemBinding.btnDoseTime.setText(entry.getFormattedTime());
                    Toast.makeText(this, entry.label + " updated to " + entry.getFormattedTime(), Toast.LENGTH_SHORT).show();
                });

                picker.show(getSupportFragmentManager(), "EDIT_DOSE_TIME_" + index);
            });

            // If there is more than 1 dose time, allow removing extra entries
            if (doseTimeEntries.size() > 1) {
                itemBinding.btnRemoveDoseTime.setVisibility(View.VISIBLE);
                itemBinding.btnRemoveDoseTime.setOnClickListener(v -> {
                    doseTimeEntries.remove(entry);
                    // Also uncheck checkbox if it matches a standard slot
                    if (entry.label.equalsIgnoreCase("Morning")) {
                        binding.cbSlotMorning.setChecked(false);
                    } else if (entry.label.equalsIgnoreCase("Afternoon")) {
                        binding.cbSlotAfternoon.setChecked(false);
                    } else if (entry.label.equalsIgnoreCase("Night")) {
                        binding.cbSlotNight.setChecked(false);
                    }
                    syncFrequencyWithDoseCount();
                    renderDoseTimeList();
                });
            } else {
                itemBinding.btnRemoveDoseTime.setVisibility(View.GONE);
            }

            binding.containerDoseTimes.addView(itemBinding.getRoot());
        }
    }

    private void showRenameDoseDialog(@NonNull DoseTimeEntry entry) {
        final EditText input = new EditText(this);
        input.setText(entry.label);
        input.setSelection(entry.label.length());
        input.setSingleLine(true);
        input.setHint("e.g. Breakfast, Lunch, Bedtime, Custom");

        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = paddingPx;
        params.rightMargin = paddingPx;
        params.topMargin = paddingPx / 2;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename Dose Time")
                .setMessage("Enter a custom label for this reminder:")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        entry.label = newName;
                        renderDoseTimeList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        com.example.util.KeyboardUtils.handleTouchOutsideEditText(this, ev);
        return super.dispatchTouchEvent(ev);
    }

    private void setupSaveButton() {
        binding.btnSaveMedicine.setOnClickListener(v -> attemptSaveMedicine());
    }

    private void attemptSaveMedicine() {
        com.example.util.KeyboardUtils.hideKeyboard(this);

        binding.tilMedicineName.setError(null);

        // 1. Validate Medicine Name
        String medicineName = binding.etMedicineName.getText() != null
                ? binding.etMedicineName.getText().toString().trim() : "";

        if (TextUtils.isEmpty(medicineName)) {
            binding.tilMedicineName.setError("Please enter the medicine name");
            binding.etMedicineName.requestFocus();
            return;
        }

        // 2. Read Frequency from Dropdown
        int frequency = getSelectedFrequency();

        // 3. Read Time Slots (ensuring all configured dose labels are preserved)
        List<String> selectedSlots = new ArrayList<>();
        for (DoseTimeEntry entry : doseTimeEntries) {
            if (!selectedSlots.contains(entry.label)) {
                selectedSlots.add(entry.label);
            }
        }

        if (selectedSlots.isEmpty() || doseTimeEntries.isEmpty()) {
            Toast.makeText(this, "Please set at least one reminder time", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeSlotsStr = TextUtils.join(", ", selectedSlots);

        // 4. Read Timing Relation from Radio Buttons (Before Eat vs. After Eat)
        String timingRelation = "After Eat";
        if (binding.rbTimingBefore.isChecked()) {
            timingRelation = "Before Eat";
        }

        // 5. Build comma-separated separate reminder times string
        List<String> formattedTimesList = new ArrayList<>();
        for (DoseTimeEntry entry : doseTimeEntries) {
            formattedTimesList.add(entry.getFormattedTime());
        }
        String separateReminderTimesStr = TextUtils.join(", ", formattedTimesList);

        // 6. Construct Medicine Entity
        Medicine newMedicine = new Medicine(
                currentUserId,
                medicineName,
                selectedPhotoPath,
                frequency,
                timeSlotsStr,
                timingRelation,
                separateReminderTimesStr,
                false // starts as Ongoing
        );

        binding.btnSaveMedicine.setEnabled(false);

        // 7. Save to Room database
        medicineRepository.addMedicine(newMedicine, new MedicineRepository.Callback<Medicine>() {
            @Override
            public void onSuccess(Medicine savedMedicine) {
                // 8. Schedule separate local Android Alarms for each customizable dose time
                for (int i = 0; i < doseTimeEntries.size(); i++) {
                    DoseTimeEntry entry = doseTimeEntries.get(i);
                    MedicineReminderScheduler.scheduleReminderWithIndex(
                            AddMedicineActivity.this,
                            savedMedicine,
                            entry.hour,
                            entry.minute,
                            entry.getFormattedTime(),
                            i
                    );
                }

                // 9. Sync to Firebase Cloud Firestore
                if (currentUsername != null && !currentUsername.isEmpty()) {
                    com.example.data.remote.FirebaseSyncManager.getInstance()
                            .syncMedicineToCloud(currentUsername, savedMedicine);
                }

                Toast.makeText(
                        AddMedicineActivity.this,
                        "Reminders scheduled for " + separateReminderTimesStr,
                        Toast.LENGTH_SHORT
                ).show();

                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                binding.btnSaveMedicine.setEnabled(true);
                Toast.makeText(AddMedicineActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
