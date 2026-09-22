package com.example.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.MyTherapyApp;
import com.example.data.model.User;
import com.example.data.repository.UserRepository;
import com.example.databinding.ActivityEditProfileBinding;

import javax.inject.Inject;

/**
 * Activity enabling users to update their profile information.
 */
public class EditProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER = "extra_user";

    @Inject
    UserRepository userRepository;

    private ActivityEditProfileBinding binding;
    private User currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Perform Dagger injection with fallback
        try {
            MyTherapyApp.getInstance().getComponent().inject(this);
        } catch (Throwable ignored) {
        }
        if (userRepository == null) {
            userRepository = MyTherapyApp.getInstance().getUserRepository();
        }

        if (getIntent() != null && getIntent().hasExtra(EXTRA_USER)) {
            currentUser = (User) getIntent().getSerializableExtra(EXTRA_USER);
        }

        setupToolbar();
        populateFields();
        setupSaveButton();
        setupSignOutButton();
    }

    private void setupSignOutButton() {
        binding.btnProfileSignOut.setOnClickListener(v -> {
            new com.example.util.SessionManager(this).clearSession();
            com.example.service.MedicineAlarmSoundService.stopAlarm(this);
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, com.example.ui.login.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupToolbar() {
        binding.toolbarEditProfile.setNavigationOnClickListener(v -> finish());
    }

    private void populateFields() {
        if (currentUser != null) {
            binding.tvProfileDisplayName.setText(currentUser.getName());
            binding.tvProfileDisplayUsername.setText("@" + currentUser.getUsername());
            binding.etUsername.setText(currentUser.getUsername());
            binding.etName.setText(currentUser.getName());
            binding.etAge.setText(String.valueOf(currentUser.getAge()));
            binding.etGender.setText(currentUser.getGender());
            if (currentUser.getPassword() != null) {
                binding.etPassword.setText(currentUser.getPassword());
            }
        }
    }

    private void setupSaveButton() {
        binding.btnSaveProfile.setOnClickListener(v -> attemptSaveProfile());
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        com.example.util.KeyboardUtils.handleTouchOutsideEditText(this, ev);
        return super.dispatchTouchEvent(ev);
    }

    private void attemptSaveProfile() {
        com.example.util.KeyboardUtils.hideKeyboard(this);

        binding.tilName.setError(null);
        binding.tilAge.setError(null);
        binding.tilGender.setError(null);
        binding.tilPassword.setError(null);

        String name = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String ageStr = binding.etAge.getText() != null
                ? binding.etAge.getText().toString().trim() : "";
        String gender = binding.etGender.getText() != null
                ? binding.etGender.getText().toString().trim() : "";
        String newPassword = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Full name is required");
            binding.etName.requestFocus();
            return;
        }

        int age = 0;
        if (TextUtils.isEmpty(ageStr)) {
            binding.tilAge.setError("Age is required");
            binding.etAge.requestFocus();
            return;
        } else {
            try {
                age = Integer.parseInt(ageStr);
                if (age <= 0 || age > 130) {
                    binding.tilAge.setError("Please enter a valid age");
                    binding.etAge.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.tilAge.setError("Invalid number format");
                binding.etAge.requestFocus();
                return;
            }
        }

        if (TextUtils.isEmpty(gender)) {
            binding.tilGender.setError("Gender is required");
            binding.etGender.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            binding.tilPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Unable to find user session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUser.setName(name);
        currentUser.setAge(age);
        currentUser.setGender(gender);
        currentUser.setPassword(newPassword);

        binding.btnSaveProfile.setEnabled(false);

        userRepository.updateUser(currentUser, new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(User updatedUser) {
                // Sync updated profile to Firebase Cloud Firestore
                com.example.data.remote.FirebaseSyncManager.getInstance().syncUserToCloud(updatedUser);

                // Update persisted user session so details are retained on next open
                new com.example.util.SessionManager(EditProfileActivity.this).saveUserSession(updatedUser);

                Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_USER, updatedUser);
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                binding.btnSaveProfile.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
