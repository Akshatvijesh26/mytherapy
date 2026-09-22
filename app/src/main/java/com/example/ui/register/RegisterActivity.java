package com.example.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.MyTherapyApp;
import com.example.data.model.User;
import com.example.data.repository.UserRepository;
import com.example.databinding.ActivityRegisterBinding;
import com.example.ui.dashboard.DashboardActivity;

import javax.inject.Inject;

/**
 * Register screen allowing new users to create an account in the local Room database.
 */
public class RegisterActivity extends AppCompatActivity {

    @Inject
    UserRepository userRepository;

    private ActivityRegisterBinding binding;

    private static final String[] GENDER_OPTIONS = {
            "Female", "Male", "Non-Binary", "Prefer not to say", "Other"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Perform Dagger injection with fallback
        try {
            MyTherapyApp.getInstance().getComponent().inject(this);
        } catch (Throwable ignored) {
        }
        if (userRepository == null) {
            userRepository = MyTherapyApp.getInstance().getUserRepository();
        }

        setupGenderDropdown();
        setupListeners();
    }

    private void setupGenderDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                GENDER_OPTIONS
        );
        binding.etGender.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Seamless navigation button to navigate back to the Login page
        binding.btnNavigateLogin.setOnClickListener(v -> finish());

        // Submit/register button saves user into Room database
        binding.btnRegister.setOnClickListener(v -> attemptRegistration());
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        com.example.util.KeyboardUtils.handleTouchOutsideEditText(this, ev);
        return super.dispatchTouchEvent(ev);
    }

    private void attemptRegistration() {
        com.example.util.KeyboardUtils.hideKeyboard(this);

        // Clear previous errors
        binding.tilName.setError(null);
        binding.tilAge.setError(null);
        binding.tilGender.setError(null);
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tvError.setVisibility(View.GONE);

        String name = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String ageStr = binding.etAge.getText() != null
                ? binding.etAge.getText().toString().trim() : "";
        String gender = binding.etGender.getText() != null
                ? binding.etGender.getText().toString().trim() : "";
        String username = binding.etUsername.getText() != null
                ? binding.etUsername.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            focusView = binding.etPassword;
            cancel = true;
        } else if (password.length() < 4) {
            binding.tilPassword.setError("Password must be at least 4 characters");
            focusView = binding.etPassword;
            cancel = true;
        }

        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.setError("Username is required");
            focusView = binding.etUsername;
            cancel = true;
        } else if (username.length() < 3) {
            binding.tilUsername.setError("Username must be at least 3 characters");
            focusView = binding.etUsername;
            cancel = true;
        }

        if (TextUtils.isEmpty(gender)) {
            binding.tilGender.setError("Gender selection is required");
            if (focusView == null) focusView = binding.etGender;
            cancel = true;
        }

        int age = 0;
        if (TextUtils.isEmpty(ageStr)) {
            binding.tilAge.setError("Age is required");
            if (focusView == null) focusView = binding.etAge;
            cancel = true;
        } else {
            try {
                age = Integer.parseInt(ageStr);
                if (age <= 0 || age > 125) {
                    binding.tilAge.setError("Please enter a valid age between 1 and 125");
                    if (focusView == null) focusView = binding.etAge;
                    cancel = true;
                }
            } catch (NumberFormatException e) {
                binding.tilAge.setError("Invalid number format");
                if (focusView == null) focusView = binding.etAge;
                cancel = true;
            }
        }

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Full Name is required");
            if (focusView == null) focusView = binding.etName;
            cancel = true;
        }

        if (cancel && focusView != null) {
            focusView.requestFocus();
            return;
        }

        setLoading(true);

        User newUser = new User(name, age, gender, username, password);

        userRepository.registerUser(newUser, new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(@Nullable User savedUser) {
                setLoading(false);
                if (savedUser != null) {
                    // Persist user session across app openings
                    new com.example.util.SessionManager(RegisterActivity.this).saveUserSession(savedUser);

                    Toast.makeText(RegisterActivity.this,
                            "Account created successfully! Welcome to My Therapy.",
                            Toast.LENGTH_LONG).show();

                    // Automatically sync to Cloud Firestore
                    com.example.data.remote.FirebaseSyncManager.getInstance().syncUserToCloud(savedUser);

                    // Automatically redirects to the Dashboard page upon success
                    Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
                    intent.putExtra(DashboardActivity.EXTRA_USER, savedUser);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                setLoading(false);
                showError(errorMessage);
                if (errorMessage.toLowerCase().contains("username")) {
                    binding.tilUsername.setError(errorMessage);
                    binding.etUsername.requestFocus();
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!isLoading);
        binding.btnNavigateLogin.setEnabled(!isLoading);
        binding.etName.setEnabled(!isLoading);
        binding.etAge.setEnabled(!isLoading);
        binding.etGender.setEnabled(!isLoading);
        binding.etUsername.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
    }

    private void showError(@NonNull String message) {
        binding.tvError.setText(message);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
