package com.example.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.data.dao.UserDao;
import com.example.data.model.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository to manage User account operations on background threads.
 */
@Singleton
public class UserRepository {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(@NonNull String errorMessage);
    }

    private final UserDao userDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    @Inject
    public UserRepository(@NonNull UserDao userDao) {
        this.userDao = userDao;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Registers a new user into the database if the username is unique.
     */
    public void registerUser(@NonNull final User user, @NonNull final Callback<User> callback) {
        executorService.execute(() -> {
            try {
                int existing = userDao.countByUsername(user.getUsername());
                if (existing > 0) {
                    postError(callback, "Username already exists. Please choose a different username.");
                    return;
                }

                long insertedId = userDao.insertUser(user);
                user.setId(insertedId);
                postSuccess(callback, user);
            } catch (Exception e) {
                postError(callback, "Registration failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    /**
     * Validates credentials against the database.
     */
    public void loginUser(@NonNull final String username, @NonNull final String password,
                          @NonNull final Callback<User> callback) {
        executorService.execute(() -> {
            try {
                User user = userDao.validateUser(username, password);
                if (user != null) {
                    postSuccess(callback, user);
                } else {
                    postError(callback, "Invalid username or password.");
                }
            } catch (Exception e) {
                postError(callback, "Login failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    /**
     * Updates an existing user's profile.
     */
    public void updateUser(@NonNull final User user, @NonNull final Callback<User> callback) {
        executorService.execute(() -> {
            try {
                userDao.updateUser(user);
                postSuccess(callback, user);
            } catch (Exception e) {
                postError(callback, "Failed to update profile: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches user by ID.
     */
    public void getUserById(final long id, @NonNull final Callback<User> callback) {
        executorService.execute(() -> {
            try {
                User user = userDao.getUserById(id);
                if (user != null) {
                    postSuccess(callback, user);
                } else {
                    postError(callback, "User not found.");
                }
            } catch (Exception e) {
                postError(callback, "Error fetching user: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches all registered users.
     */
    public void getAllUsers(@NonNull final Callback<List<User>> callback) {
        executorService.execute(() -> {
            try {
                List<User> users = userDao.getAllUsers();
                postSuccess(callback, users);
            } catch (Exception e) {
                postError(callback, "Error fetching users: " + e.getMessage());
            }
        });
    }

    private <T> void postSuccess(@NonNull final Callback<T> callback, final T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private <T> void postError(@NonNull final Callback<T> callback, @NonNull final String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
