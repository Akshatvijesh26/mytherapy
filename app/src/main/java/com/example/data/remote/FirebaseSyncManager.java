package com.example.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.data.model.Medicine;
import com.example.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper manager to synchronize user data and medicines with Cloud Firestore.
 */
public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSyncManager";
    private static final String USERS_COLLECTION = "users";
    private static final String MEDICINES_COLLECTION = "medicines";

    private static FirebaseSyncManager instance;
    private FirebaseFirestore firestore;

    private FirebaseSyncManager() {
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "Firestore initialization notice: " + e.getMessage());
        }
    }

    public static synchronized FirebaseSyncManager getInstance() {
        if (instance == null) {
            instance = new FirebaseSyncManager();
        }
        return instance;
    }

    /**
     * Uploads or updates user profile in Firestore.
     */
    public void syncUserToCloud(@NonNull User user) {
        if (firestore == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("name", user.getName());
        userData.put("age", user.getAge());
        userData.put("gender", user.getGender());
        userData.put("username", user.getUsername());
        userData.put("updatedAt", System.currentTimeMillis());

        firestore.collection(USERS_COLLECTION)
                .document(user.getUsername())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User synced to Firebase: " + user.getUsername()))
                .addOnFailureListener(e -> Log.w(TAG, "Failed syncing user to Firebase: " + e.getMessage()));
    }

    /**
     * Uploads or updates a medicine record in Firestore under user document.
     */
    public void syncMedicineToCloud(@NonNull String username, @NonNull Medicine medicine) {
        if (firestore == null) return;

        Map<String, Object> medData = new HashMap<>();
        medData.put("id", medicine.getId());
        medData.put("userId", medicine.getUserId());
        medData.put("medicineName", medicine.getMedicineName());
        medData.put("frequencyPerDay", medicine.getFrequencyPerDay());
        medData.put("timeSlots", medicine.getTimeSlots());
        medData.put("timingRelation", medicine.getTimingRelation());
        medData.put("reminderTime", medicine.getReminderTime());
        medData.put("isPassed", medicine.isPassed());
        medData.put("createdAt", medicine.getCreatedAt());
        medData.put("photoUri", medicine.getPhotoUri() != null ? medicine.getPhotoUri() : "");

        firestore.collection(USERS_COLLECTION)
                .document(username)
                .collection(MEDICINES_COLLECTION)
                .document(String.valueOf(medicine.getId()))
                .set(medData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Medicine synced to Firebase: " + medicine.getMedicineName()))
                .addOnFailureListener(e -> Log.w(TAG, "Failed syncing medicine: " + e.getMessage()));
    }

    /**
     * Deletes a medicine from Firestore under user document.
     */
    public void deleteMedicineFromCloud(@NonNull String username, long medicineId) {
        if (firestore == null) return;

        firestore.collection(USERS_COLLECTION)
                .document(username)
                .collection(MEDICINES_COLLECTION)
                .document(String.valueOf(medicineId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Medicine deleted from Firebase: " + medicineId))
                .addOnFailureListener(e -> Log.w(TAG, "Failed deleting medicine from Firebase: " + e.getMessage()));
    }
}
