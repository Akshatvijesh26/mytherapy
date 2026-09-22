package com.example.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.MyTherapyApp;
import com.example.data.model.Medicine;
import com.example.data.repository.MedicineRepository;
import com.example.util.MedicineReminderScheduler;

import java.util.List;

/**
 * Reschedules all ongoing medicine reminders upon device reboot or time change.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {

            Log.d(TAG, "Device rebooted/time changed (" + action + "). Rescheduling all reminders...");

            try {
                MedicineRepository repository = MyTherapyApp.getInstance().getMedicineRepository();
                if (repository != null) {
                    repository.getAllMedicines(new MedicineRepository.Callback<List<Medicine>>() {
                        @Override
                        public void onSuccess(List<Medicine> medicines) {
                            if (medicines != null) {
                                for (Medicine med : medicines) {
                                    if (!med.isPassed()) {
                                        MedicineReminderScheduler.scheduleAllRemindersForMedicine(context, med);
                                    }
                                }
                            }
                            Log.d(TAG, "All ongoing medicine reminders successfully rescheduled.");
                        }

                        @Override
                        public void onError(@NonNull String errorMessage) {
                            Log.e(TAG, "Failed to load medicines for rescheduling: " + errorMessage);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in BootCompletedReceiver: " + e.getMessage(), e);
            }
        }
    }
}
