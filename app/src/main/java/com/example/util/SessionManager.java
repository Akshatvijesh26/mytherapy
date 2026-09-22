package com.example.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.data.model.User;

/**
 * Manages the current logged-in user session with SharedPreferences
 * so users stay logged in across app launches until they explicitly sign out.
 */
public final class SessionManager {

    private static final String PREF_NAME = "my_therapy_user_session";
    private static final String KEY_IS_LOGGED_IN = "key_is_logged_in";
    private static final String KEY_USER_ID = "key_user_id";
    private static final String KEY_USERNAME = "key_username";
    private static final String KEY_NAME = "key_name";
    private static final String KEY_AGE = "key_age";
    private static final String KEY_GENDER = "key_gender";
    private static final String KEY_PASSWORD = "key_password";

    private final SharedPreferences prefs;

    public SessionManager(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserSession(@NonNull User user) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_USER_ID, user.getId())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_NAME, user.getName())
                .putInt(KEY_AGE, user.getAge())
                .putString(KEY_GENDER, user.getGender())
                .putString(KEY_PASSWORD, user.getPassword())
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && prefs.getLong(KEY_USER_ID, -1) != -1;
    }

    public long getLoggedInUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    @Nullable
    public User getLoggedInUser() {
        if (!isLoggedIn()) {
            return null;
        }
        User user = new User(
                prefs.getString(KEY_NAME, ""),
                prefs.getInt(KEY_AGE, 0),
                prefs.getString(KEY_GENDER, ""),
                prefs.getString(KEY_USERNAME, ""),
                prefs.getString(KEY_PASSWORD, "")
        );
        user.setId(prefs.getLong(KEY_USER_ID, -1));
        return user;
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
