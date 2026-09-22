package com.example.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.MyTherapyApp;
import com.example.data.model.Medicine;
import com.example.data.model.User;
import com.example.data.repository.MedicineRepository;
import com.example.data.repository.UserRepository;
import com.example.databinding.ActivityDashboardBinding;
import com.example.ui.login.LoginActivity;
import com.example.ui.medicine.AddMedicineActivity;
import com.example.ui.profile.EditProfileActivity;
import com.example.util.MedicineReminderScheduler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Main Dashboard screen for My Therapy app displaying ongoing doses,
 * passed medicine history, and quick access to Add Medicine and Profile editing.
 */
public class DashboardActivity extends AppCompatActivity {

    public static final String EXTRA_USER = "extra_user";
    public static final String EXTRA_USER_ID = "extra_user_id";

    @Inject
    MedicineRepository medicineRepository;

    @Inject
    UserRepository userRepository;

    private ActivityDashboardBinding binding;
    private User currentUser;

    private MedicineAdapter ongoingAdapter;
    private MedicineAdapter passedAdapter;

    private final ActivityResultLauncher<Intent> addMedicineLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadMedicines();
                }
            });

    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().hasExtra(EditProfileActivity.EXTRA_USER)) {
                        currentUser = (User) result.getData().getSerializableExtra(EditProfileActivity.EXTRA_USER);
                        populateUserGreeting();
                    }
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    android.widget.Toast.makeText(this, "Notification permission is required for medicine alarm alerts", android.widget.Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Perform Dagger injection with fallback
        try {
            MyTherapyApp.getInstance().getComponent().inject(this);
        } catch (Throwable ignored) {
        }
        if (medicineRepository == null) {
            medicineRepository = MyTherapyApp.getInstance().getMedicineRepository();
        }
        if (userRepository == null) {
            userRepository = MyTherapyApp.getInstance().getUserRepository();
        }

        extractUserFromIntent();
        populateUserGreeting();
        checkNotificationPermission();
        setupRecyclerViews();
        setupListeners();
        loadMedicines();
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicines();
    }

    private void extractUserFromIntent() {
        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_USER)) {
                currentUser = (User) getIntent().getSerializableExtra(EXTRA_USER);
            } else if (getIntent().hasExtra(EXTRA_USER_ID)) {
                long userId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
                if (userId != -1 && userRepository != null) {
                    userRepository.getUserById(userId, new UserRepository.Callback<User>() {
                        @Override
                        public void onSuccess(User result) {
                            currentUser = result;
                            populateUserGreeting();
                            loadMedicines();
                        }

                        @Override
                        public void onError(@NonNull String errorMessage) {
                        }
                    });
                }
            }
        }

        if (currentUser == null) {
            com.example.util.SessionManager sessionManager = new com.example.util.SessionManager(this);
            currentUser = sessionManager.getLoggedInUser();
        }
    }

    private void populateUserGreeting() {
        if (currentUser != null && currentUser.getName() != null && !currentUser.getName().isEmpty()) {
            binding.tvGreeting.setText("Welcome, " + currentUser.getName() + "!");
        } else {
            binding.tvGreeting.setText("Welcome to My Therapy");
        }
    }

    private void setupRecyclerViews() {
        // Ongoing medicines adapter
        ongoingAdapter = new MedicineAdapter(new MedicineAdapter.OnMedicineActionListener() {
            @Override
            public void onToggleStatus(@NonNull Medicine medicine) {
                // Mark ongoing dose as completed/passed
                medicineRepository.setPassedStatus(medicine.getId(), true, new MedicineRepository.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (currentUser != null) {
                            medicine.setPassed(true);
                            com.example.data.remote.FirebaseSyncManager.getInstance()
                                    .syncMedicineToCloud(currentUser.getUsername(), medicine);
                        }
                        Toast.makeText(DashboardActivity.this,
                                medicine.getMedicineName() + " marked as taken!",
                                Toast.LENGTH_SHORT).show();
                        loadMedicines();
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDeleteMedicine(@NonNull Medicine medicine) {
                medicineRepository.deleteMedicine(medicine, new MedicineRepository.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        MedicineReminderScheduler.cancelReminder(DashboardActivity.this, medicine.getId());
                        if (currentUser != null) {
                            com.example.data.remote.FirebaseSyncManager.getInstance()
                                    .deleteMedicineFromCloud(currentUser.getUsername(), medicine.getId());
                        }
                        Toast.makeText(DashboardActivity.this, "Medicine removed", Toast.LENGTH_SHORT).show();
                        loadMedicines();
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        binding.rvOngoingMedicines.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOngoingMedicines.setAdapter(ongoingAdapter);

        // Passed medicines adapter
        passedAdapter = new MedicineAdapter(new MedicineAdapter.OnMedicineActionListener() {
            @Override
            public void onToggleStatus(@NonNull Medicine medicine) {
                // Toggle back to ongoing if desired
                medicineRepository.setPassedStatus(medicine.getId(), false, new MedicineRepository.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (currentUser != null) {
                            medicine.setPassed(false);
                            com.example.data.remote.FirebaseSyncManager.getInstance()
                                    .syncMedicineToCloud(currentUser.getUsername(), medicine);
                        }
                        Toast.makeText(DashboardActivity.this,
                                medicine.getMedicineName() + " moved to ongoing schedule",
                                Toast.LENGTH_SHORT).show();
                        loadMedicines();
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDeleteMedicine(@NonNull Medicine medicine) {
                medicineRepository.deleteMedicine(medicine, new MedicineRepository.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        MedicineReminderScheduler.cancelReminder(DashboardActivity.this, medicine.getId());
                        if (currentUser != null) {
                            com.example.data.remote.FirebaseSyncManager.getInstance()
                                    .deleteMedicineFromCloud(currentUser.getUsername(), medicine.getId());
                        }
                        Toast.makeText(DashboardActivity.this, "Medicine removed", Toast.LENGTH_SHORT).show();
                        loadMedicines();
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        binding.rvPassedMedicines.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPassedMedicines.setAdapter(passedAdapter);
    }

    private void setupListeners() {
        // Profile icon button -> navigates to Edit Profile page
        binding.btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, EditProfileActivity.class);
            if (currentUser != null) {
                intent.putExtra(EditProfileActivity.EXTRA_USER, currentUser);
            }
            editProfileLauncher.launch(intent);
        });

        // Logout button
        binding.btnLogout.setOnClickListener(v -> {
            new com.example.util.SessionManager(DashboardActivity.this).clearSession();
            com.example.service.MedicineAlarmSoundService.stopAlarm(DashboardActivity.this);
            Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Prominent FAB -> opens Add Medicine page
        binding.fabAddMedicine.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddMedicineActivity.class);
            if (currentUser != null) {
                intent.putExtra(AddMedicineActivity.EXTRA_USER_ID, currentUser.getId());
                intent.putExtra(AddMedicineActivity.EXTRA_USER_NAME, currentUser.getUsername());
            }
            addMedicineLauncher.launch(intent);
        });

        // Test Alarm Button
        binding.btnTestAlarm.setOnClickListener(v -> {
            com.example.service.MedicineAlarmSoundService.startAlarm(
                    DashboardActivity.this,
                    9999,
                    "Test Medication Alarm",
                    "This is an alarm test verifying sound and vibration work properly!"
            );
            Toast.makeText(DashboardActivity.this, "Alarm ringing! Tap notification 'Dismiss' or open app to stop.", Toast.LENGTH_LONG).show();
        });
    }

    private void loadMedicines() {
        if (medicineRepository == null) return;

        long userId = (currentUser != null) ? currentUser.getId() : 0;

        // Fetch ongoing medicines
        medicineRepository.getOngoingMedicines(userId, new MedicineRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> ongoingList) {
                if (ongoingList == null) ongoingList = new ArrayList<>();
                ongoingAdapter.setItems(ongoingList);
                binding.tvOngoingCount.setText(String.valueOf(ongoingList.size()));

                if (ongoingList.isEmpty()) {
                    binding.rvOngoingMedicines.setVisibility(View.GONE);
                    binding.layoutEmptyOngoing.setVisibility(View.VISIBLE);
                } else {
                    binding.rvOngoingMedicines.setVisibility(View.VISIBLE);
                    binding.layoutEmptyOngoing.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
            }
        });

        // Fetch passed medicines
        medicineRepository.getPassedMedicines(userId, new MedicineRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> passedList) {
                if (passedList == null) passedList = new ArrayList<>();
                passedAdapter.setItems(passedList);
                binding.tvPassedCount.setText(String.valueOf(passedList.size()));

                if (passedList.isEmpty()) {
                    binding.rvPassedMedicines.setVisibility(View.GONE);
                    binding.layoutEmptyPassed.setVisibility(View.VISIBLE);
                } else {
                    binding.rvPassedMedicines.setVisibility(View.VISIBLE);
                    binding.layoutEmptyPassed.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
