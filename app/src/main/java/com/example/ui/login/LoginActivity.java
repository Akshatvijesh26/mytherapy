package com.example.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.MyTherapyApp;
import com.example.data.model.User;
import com.example.data.repository.UserRepository;
import com.example.databinding.ActivityLoginBinding;
import com.example.ui.dashboard.DashboardActivity;
import com.example.ui.register.RegisterActivity;

import javax.inject.Inject;

/**
 * Login screen allowing users to validate credentials against the local Room database.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_USERNAME = "extra_prefill_username";

    @Inject
    UserRepository userRepository;

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Perform Dagger injection with fallback
        try {
            MyTherapyApp.getInstance().getComponent().inject(this);
        } catch (Throwable ignored) {
        }
        if (userRepository == null) {
            userRepository = MyTherapyApp.getInstance().getUserRepository();
        }

        // Check if user is already logged in across app restarts
        com.example.util.SessionManager sessionManager = new com.example.util.SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            User loggedInUser = sessionManager.getLoggedInUser();
            if (loggedInUser != null) {
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                intent.putExtra(DashboardActivity.EXTRA_USER, loggedInUser);
                intent.putExtra(DashboardActivity.EXTRA_USER_ID, loggedInUser.getId());
                startActivity(intent);
                finish();
                return;
            }
        }

        handleIncomingIntent(getIntent());
        setupListeners();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(@Nullable Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_PREFILL_USERNAME)) {
            String prefilled = intent.getStringExtra(EXTRA_PREFILL_USERNAME);
            if (!TextUtils.isEmpty(prefilled)) {
                binding.etUsername.setText(prefilled);
                binding.etPassword.requestFocus();
            }
        }
    }

    private void setupListeners() {
        // Login button queries database to validate credentials
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Clearly positioned button directly underneath the login form to navigate to Create Account
        binding.btnGotoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        com.example.util.KeyboardUtils.handleTouchOutsideEditText(this, ev);
        return super.dispatchTouchEvent(ev);
    }

    private void attemptLogin() {
        com.example.util.KeyboardUtils.hideKeyboard(this);

        // Reset errors
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tvError.setVisibility(View.GONE);

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
        }

        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.setError("Username is required");
            focusView = binding.etUsername;
            cancel = true;
        }

        if (cancel && focusView != null) {
            focusView.requestFocus();
            return;
        }

        setLoading(true);

        userRepository.loginUser(username, password, new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(@Nullable User user) {
                setLoading(false);
                if (user != null) {
                    // Persist user session across app openings
                    new com.example.util.SessionManager(LoginActivity.this).saveUserSession(user);

                    Toast.makeText(LoginActivity.this,
                            "Welcome back, " + user.getName() + "!",
                            Toast.LENGTH_SHORT).show();

                    // Navigate to Dashboard upon success
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra(DashboardActivity.EXTRA_USER, user);
                    intent.putExtra(DashboardActivity.EXTRA_USER_ID, user.getId());
                    startActivity(intent);
                    finish();
                } else {
                    showError("Invalid username or password.");
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnGotoRegister.setEnabled(!isLoading);
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
