package com.example.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.data.model.Medicine;
import com.example.receiver.MedicineReminderReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for scheduling and canceling local medication dose alarms with AlarmManager.
 */
public final class MedicineReminderScheduler {

    private static final String TAG = "MedicineReminderSched";

    private MedicineReminderScheduler() {
    }

    public static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                // Delete old channel if it was created without sound
                android.app.NotificationChannel existing = manager.getNotificationChannel(MedicineReminderReceiver.CHANNEL_ID);
                if (existing != null && existing.getSound() == null) {
                    manager.deleteNotificationChannel(MedicineReminderReceiver.CHANNEL_ID);
                }

                if (manager.getNotificationChannel(MedicineReminderReceiver.CHANNEL_ID) == null) {
                    NotificationChannel channel = new NotificationChannel(
                            MedicineReminderReceiver.CHANNEL_ID,
                            MedicineReminderReceiver.CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Reminders for scheduled medication and therapy doses");
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
                    channel.enableLights(true);
                    channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                    channel.setBypassDnd(true);

                    android.net.Uri alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
                    if (alarmSound == null) {
                        alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
                    }
                    if (alarmSound == null) {
                        alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
                    }

                    if (alarmSound != null) {
                        android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .build();
                        channel.setSound(alarmSound, audioAttributes);
                    }

                    manager.createNotificationChannel(channel);
                }
            }
        }
    }

    /**
     * Schedules an exact Alarm for the medicine reminder time.
     *
     * @param context  Application or Activity context
     * @param medicine The Medicine item to schedule
     * @param hourOfDay The 24-hour format hour (0-23)
     * @param minute    The minute (0-59)
     */
    public static void scheduleReminder(
            @NonNull Context context,
            @NonNull Medicine medicine,
            int hourOfDay,
            int minute
    ) {
        createNotificationChannel(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            return;
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        targetCal.set(Calendar.MINUTE, minute);
        targetCal.set(Calendar.SECOND, 0);
        targetCal.set(Calendar.MILLISECOND, 0);

        // If the scheduled time is earlier than current time today, schedule for tomorrow
        if (targetCal.getTimeInMillis() <= System.currentTimeMillis()) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, MedicineReminderReceiver.class);
        intent.putExtra(MedicineReminderReceiver.EXTRA_MEDICINE_ID, medicine.getId());
        intent.putExtra(MedicineReminderReceiver.EXTRA_MEDICINE_NAME, medicine.getMedicineName());
        intent.putExtra(MedicineReminderReceiver.EXTRA_PHOTO_URI, medicine.getPhotoUri());
        intent.putExtra(MedicineReminderReceiver.EXTRA_FREQUENCY, medicine.getFrequencyPerDay());
        intent.putExtra(MedicineReminderReceiver.EXTRA_TIME_SLOTS, medicine.getTimeSlots());
        intent.putExtra(MedicineReminderReceiver.EXTRA_TIMING_RELATION, medicine.getTimingRelation());
        intent.putExtra(MedicineReminderReceiver.EXTRA_REMINDER_TIME, medicine.getReminderTime());

        int requestCode = (int) (medicine.getId() != 0 ? medicine.getId() : System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.getTimeInMillis(),
                        pendingIntent
                );
            }
            Log.d(TAG, "Scheduled alarm for " + medicine.getMedicineName() + " at " + targetCal.getTime().toString());
        } catch (SecurityException se) {
            Log.w(TAG, "Exact alarm permission restriction: " + se.getMessage());
            // Fallback to inexact alarm if exact alarm permission is restricted
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetCal.getTimeInMillis(),
                    pendingIntent
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Helper to schedule an immediate test trigger or parsed string schedule.
     */
    public static void scheduleFromTimeString(
            @NonNull Context context,
            @NonNull Medicine medicine,
            @NonNull String timeString
    ) {
        int hour = 8;
        int minute = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf.parse(timeString);
            if (date != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            }
        } catch (Exception ignored) {
        }
        scheduleReminder(context, medicine, hour, minute);
    }

    /**
     * Schedules alarms for all comma-separated reminder times of a medicine.
     * (e.g. "08:00 AM, 01:00 PM, 08:30 PM")
     */
    public static void scheduleAllRemindersForMedicine(
            @NonNull Context context,
            @NonNull Medicine medicine
    ) {
        String reminderTimes = medicine.getReminderTime();
        if (TextUtils.isEmpty(reminderTimes)) {
            return;
        }

        String[] times = reminderTimes.split(",");
        for (int i = 0; i < times.length; i++) {
            String timeStr = times[i].trim();
            if (!timeStr.isEmpty()) {
                scheduleSingleReminder(context, medicine, timeStr, i);
            }
        }
    }

    private static void scheduleSingleReminder(
            @NonNull Context context,
            @NonNull Medicine medicine,
            @NonNull String timeString,
            int index
    ) {
        int hour = 8;
        int minute = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf.parse(timeString);
            if (date != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            }
        } catch (Exception ignored) {
        }
        scheduleReminderWithIndex(context, medicine, hour, minute, timeString, index);
    }

    public static void scheduleReminderWithIndex(
            @NonNull Context context,
            @NonNull Medicine medicine,
            int hourOfDay,
            int minute,
            @NonNull String specificTimeString,
            int index
    ) {
        createNotificationChannel(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            return;
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        targetCal.set(Calendar.MINUTE, minute);
        targetCal.set(Calendar.SECOND, 0);
        targetCal.set(Calendar.MILLISECOND, 0);

        if (targetCal.getTimeInMillis() <= System.currentTimeMillis()) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, MedicineReminderReceiver.class);
        intent.putExtra(MedicineReminderReceiver.EXTRA_MEDICINE_ID, medicine.getId());
        intent.putExtra(MedicineReminderReceiver.EXTRA_MEDICINE_NAME, medicine.getMedicineName());
        intent.putExtra(MedicineReminderReceiver.EXTRA_PHOTO_URI, medicine.getPhotoUri());
        intent.putExtra(MedicineReminderReceiver.EXTRA_FREQUENCY, medicine.getFrequencyPerDay());
        intent.putExtra(MedicineReminderReceiver.EXTRA_TIME_SLOTS, medicine.getTimeSlots());
        intent.putExtra(MedicineReminderReceiver.EXTRA_TIMING_RELATION, medicine.getTimingRelation());
        intent.putExtra(MedicineReminderReceiver.EXTRA_REMINDER_TIME, specificTimeString);

        // Derive unique request code for each dose index (max 20 per medicine)
        int requestCode = (int) (medicine.getId() * 100 + index);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.getTimeInMillis(),
                        pendingIntent
                );
            }
            Log.d(TAG, "Scheduled alarm [" + index + "] for " + medicine.getMedicineName() + " at " + targetCal.getTime().toString());
        } catch (SecurityException se) {
            Log.w(TAG, "Exact alarm permission restriction: " + se.getMessage());
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetCal.getTimeInMillis(),
                    pendingIntent
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels all alarms associated with a medicine across multiple dose indexes.
     */
    public static void cancelReminder(@NonNull Context context, long medicineId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, MedicineReminderReceiver.class);

        // Cancel legacy single alarm (requestCode = medicineId)
        PendingIntent legacyIntent = PendingIntent.getBroadcast(
                context,
                (int) medicineId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (legacyIntent != null) {
            alarmManager.cancel(legacyIntent);
            legacyIntent.cancel();
        }

        // Cancel up to 10 potential separate dose alarms (requestCode = medicineId * 100 + i)
        for (int i = 0; i < 10; i++) {
            int requestCode = (int) (medicineId * 100 + i);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
        Log.d(TAG, "Canceled all alarms for medicine id: " + medicineId);
    }
}
