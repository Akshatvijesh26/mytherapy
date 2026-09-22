package com.example.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.data.dao.MedicineDao;
import com.example.data.model.Medicine;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository to manage Medicine entity operations on background threads.
 */
@Singleton
public class MedicineRepository {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(@NonNull String errorMessage);
    }

    private final MedicineDao medicineDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    @Inject
    public MedicineRepository(@NonNull MedicineDao medicineDao) {
        this.medicineDao = medicineDao;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Inserts a new medicine.
     */
    public void addMedicine(@NonNull final Medicine medicine, @NonNull final Callback<Medicine> callback) {
        executorService.execute(() -> {
            try {
                long id = medicineDao.insertMedicine(medicine);
                medicine.setId(id);
                postSuccess(callback, medicine);
            } catch (Exception e) {
                postError(callback, "Failed to save medicine: " + e.getMessage());
            }
        });
    }

    /**
     * Updates an existing medicine.
     */
    public void updateMedicine(@NonNull final Medicine medicine, @NonNull final Callback<Medicine> callback) {
        executorService.execute(() -> {
            try {
                medicineDao.updateMedicine(medicine);
                postSuccess(callback, medicine);
            } catch (Exception e) {
                postError(callback, "Failed to update medicine: " + e.getMessage());
            }
        });
    }

    /**
     * Toggles or sets passed/completed status.
     */
    public void setPassedStatus(final long id, final boolean isPassed, @NonNull final Callback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                medicineDao.setMedicinePassedStatus(id, isPassed);
                postSuccess(callback, true);
            } catch (Exception e) {
                postError(callback, "Failed to update status: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes a medicine.
     */
    public void deleteMedicine(@NonNull final Medicine medicine, @NonNull final Callback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                medicineDao.deleteMedicine(medicine);
                postSuccess(callback, true);
            } catch (Exception e) {
                postError(callback, "Failed to delete medicine: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches ongoing (upcoming) medicines for user.
     */
    public void getOngoingMedicines(final long userId, @NonNull final Callback<List<Medicine>> callback) {
        executorService.execute(() -> {
            try {
                List<Medicine> list = medicineDao.getOngoingMedicines(userId);
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Failed to load ongoing medicines: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches passed (completed) medicines for user.
     */
    public void getPassedMedicines(final long userId, @NonNull final Callback<List<Medicine>> callback) {
        executorService.execute(() -> {
            try {
                List<Medicine> list = medicineDao.getPassedMedicines(userId);
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Failed to load passed medicines: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches all medicines for user.
     */
    public void getAllMedicinesForUser(final long userId, @NonNull final Callback<List<Medicine>> callback) {
        executorService.execute(() -> {
            try {
                List<Medicine> list = medicineDao.getAllMedicinesForUser(userId);
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Failed to load medicines: " + e.getMessage());
            }
        });
    }

    /**
     * Fetches all medicines in the database.
     */
    public void getAllMedicines(@NonNull final Callback<List<Medicine>> callback) {
        executorService.execute(() -> {
            try {
                List<Medicine> list = medicineDao.getAllMedicines();
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Failed to load medicines: " + e.getMessage());
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
