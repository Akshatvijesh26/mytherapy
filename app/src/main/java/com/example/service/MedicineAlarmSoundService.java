package com.example.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.R;
import com.example.ui.dashboard.DashboardActivity;

/**
 * Foreground Service that plays an audible alarm ringtone continuously
 * and vibrates until the user dismisses it or opens the app.
 */
public class MedicineAlarmSoundService extends Service {

    private static final String TAG = "AlarmSoundService";
    public static final String ACTION_START_ALARM = "com.example.action.START_ALARM";
    public static final String ACTION_STOP_ALARM = "com.example.action.STOP_ALARM";

    public static final String EXTRA_MEDICINE_ID = "extra_medicine_id";
    public static final String EXTRA_MEDICINE_NAME = "extra_medicine_name";
    public static final String EXTRA_DOSE_DETAILS = "extra_dose_details";

    private static final String ALARM_CHANNEL_ID = "medicine_alarm_audio_channel";
    private static final int ALARM_NOTIFICATION_ID = 99991;

    private MediaPlayer mediaPlayer;
    private Ringtone fallbackRingtone;
    private Vibrator vibrator;

    public static void startAlarm(Context context, long medicineId, String medicineName, String doseDetails) {
        Intent intent = new Intent(context, MedicineAlarmSoundService.class);
        intent.setAction(ACTION_START_ALARM);
        intent.putExtra(EXTRA_MEDICINE_ID, medicineId);
        intent.putExtra(EXTRA_MEDICINE_NAME, medicineName);
        intent.putExtra(EXTRA_DOSE_DETAILS, doseDetails);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopAlarm(Context context) {
        Intent intent = new Intent(context, MedicineAlarmSoundService.class);
        intent.setAction(ACTION_STOP_ALARM);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createAlarmNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_ALARM.equals(intent.getAction())) {
            stopAlarmPlayback();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        String medicineName = intent != null ? intent.getStringExtra(EXTRA_MEDICINE_NAME) : null;
        if (medicineName == null || medicineName.isEmpty()) {
            medicineName = "Scheduled Medication";
        }
        String doseDetails = intent != null ? intent.getStringExtra(EXTRA_DOSE_DETAILS) : "Time for your scheduled dose";

        Notification notification = buildAlarmNotification(medicineName, doseDetails);
        startForeground(ALARM_NOTIFICATION_ID, notification);

        playAlarmSoundAndVibrate();

        return START_NOT_STICKY;
    }

    private void playAlarmSoundAndVibrate() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            // Ensure alarm audio stream has audible volume
            try {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                    int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    if (currentVol < maxVol / 3) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (int) (maxVol * 0.8), 0);
                    }
                }
            } catch (Exception ignored) {
            }

            // Initialize MediaPlayer with looping
            if (mediaPlayer == null && alarmUri != null) {
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(getApplicationContext(), alarmUri);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AudioAttributes attributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build();
                        mediaPlayer.setAudioAttributes(attributes);
                    } else {
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    }
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (Exception e) {
                    Log.w(TAG, "MediaPlayer initialization failed, falling back to Ringtone: " + e.getMessage());
                    if (mediaPlayer != null) {
                        try { mediaPlayer.release(); } catch (Exception ignored) {}
                        mediaPlayer = null;
                    }
                }
            }

            // Fallback to Ringtone if MediaPlayer failed
            if (mediaPlayer == null && fallbackRingtone == null && alarmUri != null) {
                fallbackRingtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && fallbackRingtone != null) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    fallbackRingtone.setAudioAttributes(attributes);
                }
                if (fallbackRingtone != null && !fallbackRingtone.isPlaying()) {
                    fallbackRingtone.play();
                }
            }

            // Vibrate pattern (repeating)
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 800, 400, 800, 400, 800};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 means repeat
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound/vibrate: " + e.getMessage(), e);
        }
    }

    private void stopAlarmPlayback() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception ignored) {
        }

        try {
            if (fallbackRingtone != null && fallbackRingtone.isPlaying()) {
                fallbackRingtone.stop();
            }
            fallbackRingtone = null;
        } catch (Exception ignored) {
        }

        try {
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception ignored) {
        }
    }

    private Notification buildAlarmNotification(String medicineName, String doseDetails) {
        Intent openIntent = new Intent(this, DashboardActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingOpen = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent dismissIntent = new Intent(this, MedicineAlarmSoundService.class);
        dismissIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent pendingDismiss = PendingIntent.getService(
                this,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle("⏰ Medicine Alarm: " + medicineName)
                .setContentText(doseDetails)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingOpen)
                .addAction(R.drawable.ic_check, "Dismiss Alarm", pendingDismiss)
                .build();
    }

    private void createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        ALARM_CHANNEL_ID,
                        "Medicine Dose Alarms",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("High-priority alarm alerts for medicine reminders");
                channel.setBypassDnd(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        stopAlarmPlayback();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
