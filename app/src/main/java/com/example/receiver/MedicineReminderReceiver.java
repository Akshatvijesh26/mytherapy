package com.example.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.R;
import com.example.ui.dashboard.DashboardActivity;

import java.io.File;
import java.io.InputStream;

/**
 * BroadcastReceiver triggered by AlarmManager to post a local medication reminder notification.
 */
public class MedicineReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "MedicineReminderReceiver";
    public static final String CHANNEL_ID = "medicine_reminders_channel";
    public static final String CHANNEL_NAME = "Medicine Reminders";

    public static final String EXTRA_MEDICINE_ID = "extra_medicine_id";
    public static final String EXTRA_MEDICINE_NAME = "extra_medicine_name";
    public static final String EXTRA_PHOTO_URI = "extra_photo_uri";
    public static final String EXTRA_FREQUENCY = "extra_frequency";
    public static final String EXTRA_TIME_SLOTS = "extra_time_slots";
    public static final String EXTRA_TIMING_RELATION = "extra_timing_relation";
    public static final String EXTRA_REMINDER_TIME = "extra_reminder_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        long medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, System.currentTimeMillis());
        String medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME);
        if (TextUtils.isEmpty(medicineName)) {
            medicineName = "Scheduled Medication";
        }
        String photoUri = intent.getStringExtra(EXTRA_PHOTO_URI);
        int frequency = intent.getIntExtra(EXTRA_FREQUENCY, 1);
        String timeSlots = intent.getStringExtra(EXTRA_TIME_SLOTS);
        if (TextUtils.isEmpty(timeSlots)) {
            timeSlots = "General Dose";
        }
        String timingRelation = intent.getStringExtra(EXTRA_TIMING_RELATION);
        if (TextUtils.isEmpty(timingRelation)) {
            timingRelation = "Take with water";
        }
        String reminderTime = intent.getStringExtra(EXTRA_REMINDER_TIME);
        if (TextUtils.isEmpty(reminderTime)) {
            reminderTime = "Now";
        }

        createNotificationChannelIfNeeded(context);

        // Intent to open DashboardActivity on notification tap
        Intent tapIntent = new Intent(context, DashboardActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        tapIntent.putExtra(EXTRA_MEDICINE_ID, medicineId);

        PendingIntent pendingTapIntent = PendingIntent.getActivity(
                context,
                (int) (medicineId ^ 0xA5A5),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String shortSummary = timingRelation + " • " + timeSlots + " (" + frequency + "x/day)";

        // Start continuous audible alarm ringtone & vibration service like a regular alarm
        try {
            com.example.service.MedicineAlarmSoundService.startAlarm(
                    context,
                    medicineId,
                    medicineName,
                    shortSummary + " • " + reminderTime
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MedicineAlarmSoundService: " + e.getMessage(), e);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle("⏰ Alarm: Time to take " + medicineName)
                .setContentText(shortSummary)
                .setSubText("Medicine Dose Due")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingTapIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Load medicine photo if available
        Bitmap photoBitmap = loadPhotoBitmap(context, photoUri);
        if (photoBitmap != null) {
            builder.setLargeIcon(photoBitmap);
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                    .bigPicture(photoBitmap)
                    .setBigContentTitle("Time to take: " + medicineName)
                    .setSummaryText("Dosage: " + timingRelation + " • Slots: " + timeSlots + " • Time: " + reminderTime);
            builder.setStyle(bigPictureStyle);
        } else {
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Time to take: " + medicineName)
                    .bigText("Dosage Instructions:\n• " + timingRelation + "\n• Time Slots: " + timeSlots + "\n• Scheduled: " + reminderTime + "\n• Daily Frequency: " + frequency + " dose(s)");
            builder.setStyle(bigTextStyle);
        }

        int notificationId = (int) (medicineId % Integer.MAX_VALUE);
        if (notificationId == 0) {
            notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        }

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification successfully posted for medicine: " + medicineName);
        } catch (SecurityException se) {
            Log.w(TAG, "Missing notification permission: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to post notification: " + e.getMessage(), e);
        }
    }

    private void createNotificationChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                // Delete old channel if it was created without sound
                NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
                if (existing != null && existing.getSound() == null) {
                    manager.deleteNotificationChannel(CHANNEL_ID);
                }

                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Reminders for scheduled medication and therapy doses");
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
                    channel.enableLights(true);
                    channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                    channel.setBypassDnd(true);

                    Uri alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
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

    private Bitmap loadPhotoBitmap(Context context, String photoUri) {
        if (TextUtils.isEmpty(photoUri)) return null;

        try {
            if (photoUri.startsWith("file://") || photoUri.startsWith("/")) {
                String cleanPath = photoUri.replace("file://", "");
                File f = new File(cleanPath);
                if (f.exists()) {
                    return BitmapFactory.decodeFile(f.getAbsolutePath());
                }
            } else if (photoUri.startsWith("content://")) {
                Uri uri = Uri.parse(photoUri);
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    return bmp;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to decode photo bitmap for notification: " + e.getMessage());
        }
        return null;
    }
}
