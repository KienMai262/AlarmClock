package com.example.alarmclock.alarm;

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
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.alarmclock.MainActivity; // Để mở lại app khi nhấn notification
import com.example.alarmclock.R;

import java.io.IOException;

public class AlarmRingService extends Service {

    private static final String TAG = "AlarmRingService";
    private static final String CHANNEL_ID = "ALARM_RING_CHANNEL";
    private static final int NOTIFICATION_ID = 123; // ID duy nhất cho notification
    public static final String ACTION_STOP_ALARM = "com.example.alarmclock.STOP_ALARM";

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private int originalVolume;
    private boolean isVibrating = false; // Theo dõi trạng thái rung
    private int currentAlarmId = -1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Không dùng binding
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        createNotificationChannel();

        Log.e("ALARM_DEBUG", "!!!!!!!!!! AlarmRingService onCreate !!!!!!!!!!");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("ALARM_DEBUG", "!!!!!!!!!! AlarmRingService onStartCommand TOP !!!!!!!!!!");
        Log.d(TAG, "Service onStartCommand - Full Intent: " + (intent != null ? intent.toString() + " Extras: " + intent.getExtras() : "null intent")); // Log cả extras

        Log.d(TAG, "Service onStartCommand");

        // Xử lý action dừng báo thức (từ notification hoặc Activity)
        if (intent != null && ACTION_STOP_ALARM.equals(intent.getAction())) {
            Log.d(TAG, "Received stop action.");
            stopAlarmSound(); // Dừng nhạc và rung
            stopForeground(true); // Xóa notification
            stopSelf(); // Dừng service
            return START_NOT_STICKY;
        }

        // Lấy dữ liệu từ Intent được gửi bởi AlarmReceiver
        currentAlarmId = intent.getIntExtra("alarmId", -1);
        int soundResourceId = intent.getIntExtra("soundResourceId", -1);
        String alarmNote = intent.getStringExtra("alarmNote");
        if (alarmNote == null || alarmNote.isEmpty()) {
            alarmNote = "Báo thức!"; // Ghi chú mặc định
        }

        Log.i(TAG, "Starting foreground service for alarmId: " + currentAlarmId);
        Log.d(TAG, "Sound resource ID: " + soundResourceId);
        Log.d(TAG, "Alarm note: " + alarmNote);

        // Tạo và hiển thị notification foreground
        Log.d("ALARM_DEBUG", "Building notification...");
        Notification notification = buildNotification(alarmNote, currentAlarmId);
        if (notification == null) {
            Log.e("ALARM_DEBUG", "!!! buildNotification returned null! Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }
        Log.d("ALARM_DEBUG", "Calling startForeground...");
        try {
            startForeground(NOTIFICATION_ID, notification);
            Log.d("ALARM_DEBUG", "startForeground successful."); // Thêm log thành công
        } catch (Exception e) {
            Log.e("ALARM_DEBUG", "!!!!!!!!!! EXCEPTION during startForeground !!!!!!!!!!", e);
            Toast.makeText(this, "Lỗi khi hiển thị thông báo báo thức.", Toast.LENGTH_LONG).show();
            stopSelf(); // Dừng service nếu không thể chạy foreground
            return START_NOT_STICKY;
        }

        // Bắt đầu phát âm thanh và rung
        Log.d("ALARM_DEBUG", "Calling startAlarmSound...");
        startAlarmSound(soundResourceId);
        Log.d("ALARM_DEBUG", "Calling startVibration...");
        startVibration();

        // ----- THÊM ĐOẠN CODE NÀY ĐỂ KHỞI CHẠY ACTIVITY -----
        Log.d("ALARM_DEBUG", "Attempting to explicitly start AlarmRingActivity...");
        Intent ringActivityIntent = new Intent(this, AlarmRingActivity.class);
        // Quan trọng: Cần FLAG_ACTIVITY_NEW_TASK khi khởi chạy từ Service
        // Các flag khác giúp quản lý Activity stack (tùy chọn nhưng thường hữu ích)
        ringActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ringActivityIntent.putExtra("alarmId", currentAlarmId);
        ringActivityIntent.putExtra("alarmNote", alarmNote); // Truyền dữ liệu cần thiết



        // START_STICKY: Nếu service bị kill, hệ thống sẽ cố gắng khởi động lại nhưng intent sẽ là null
        // START_NOT_STICKY: Nếu service bị kill, nó sẽ không tự khởi động lại trừ khi có intent mới
        // Thường dùng START_NOT_STICKY cho báo thức để tránh nó kêu lại không mong muốn
        return START_NOT_STICKY;
    }

    private void startAlarmSound(int soundResourceId) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            Log.w(TAG,"MediaPlayer is already playing.");
            // Có thể bạn muốn dừng cái cũ trước khi bắt đầu cái mới nếu alarmId khác?
            // releaseMediaPlayer();
            return;
        }
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else {
            mediaPlayer.reset(); // Đặt lại nếu đã tồn tại
        }

        if (soundResourceId != -1 && soundResourceId != 0) {
            Log.i(TAG, "Attempting to play sound with resource ID: " + soundResourceId);

            // --- Tối đa hóa âm lượng ---
            if (audioManager != null) {
                try {
                    originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
                    Log.d(TAG, "Set ALARM stream volume to max: " + maxVolume);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting AudioManager volume", e);
                }
            } else {
                Log.e(TAG, "AudioManager is null.");
            }

            // --- Khởi tạo MediaPlayer ---
            try {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) // hoặc SONIFICATION
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);

                Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + soundResourceId);
                mediaPlayer.setDataSource(this, soundUri);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    Log.d(TAG, "MediaPlayer prepared. Starting playback.");
                    try {
                        mp.start();
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "IllegalStateException on MediaPlayer start after prepare", e);
                        stopAlarmSound(); // Dừng nếu lỗi
                    }
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                    Toast.makeText(this, "Lỗi phát âm thanh báo thức", Toast.LENGTH_SHORT).show();
                    stopAlarmSound(); // Dừng nếu lỗi
                    return true; // Đã xử lý lỗi
                });

            } catch (IOException | IllegalArgumentException | SecurityException | IllegalStateException e) {
                Log.e(TAG, "Error setting data source or preparing MediaPlayer", e);
                Toast.makeText(this, "Lỗi tải âm thanh báo thức", Toast.LENGTH_SHORT).show();
                releaseMediaPlayer();
            }
        } else {
            Log.e(TAG, "Invalid sound resource ID received: " + soundResourceId + ". Cannot play sound.");
            Toast.makeText(this, "Âm thanh báo thức không hợp lệ", Toast.LENGTH_SHORT).show();
            // Cân nhắc phát âm thanh mặc định của hệ thống ở đây
        }
    }

    private void startVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            // Mẫu rung: đợi 0ms, rung 1000ms, đợi 1000ms, rung 1000ms... Lặp lại từ chỉ mục 0
            long[] pattern = {0, 1000, 1000};
            int repeatIndex = 0; // Lặp lại toàn bộ mẫu

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex));
                } else {
                    // deprecated in API 26
                    vibrator.vibrate(pattern, repeatIndex);
                }
                isVibrating = true;
                Log.d(TAG, "Started vibration.");
            } catch (Exception e) {
                Log.e(TAG, "Error starting vibration", e);
                isVibrating = false;
            }
        } else {
            Log.w(TAG,"Vibrator not available or no permission.");
            isVibrating = false;
        }
    }


    private void stopAlarmSound() {
        Log.i(TAG, "Stopping alarm sound and vibration.");
        releaseMediaPlayer();
        stopVibration();

        // Khôi phục âm lượng gốc
        if (audioManager != null) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
                Log.d(TAG, "Restored ALARM stream volume to: " + originalVolume);
            } catch (Exception e) {
                Log.e(TAG, "Error restoring volume", e);
            }
        }
    }

    private void stopVibration() {
        if (vibrator != null && isVibrating) {
            try {
                vibrator.cancel();
                isVibrating = false;
                Log.d(TAG, "Stopped vibration.");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping vibration", e);
            }
        }
    }


    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            Log.d(TAG,"Releasing MediaPlayer...");
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                Log.d(TAG,"MediaPlayer released.");
            } catch (Exception e) { // Catch broader exception for safety
                Log.e(TAG, "Exception while releasing MediaPlayer", e);
            } finally {
                mediaPlayer = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        stopAlarmSound(); // Đảm bảo mọi thứ dừng lại khi service bị hủy
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.alarms); // Thêm chuỗi này vào strings.xml
            String description = getString(R.string.alarms); // Thêm chuỗi này vào strings.xml
            int importance = NotificationManager.IMPORTANCE_HIGH; // Quan trọng cao để hiển thị head-up
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Tùy chọn: cấu hình thêm (rung, đèn, ...) cho kênh
            channel.enableVibration(false); // Tắt rung mặc định của kênh vì ta tự quản lý

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG,"Notification channel created.");
            } else {
                Log.e(TAG, "NotificationManager is null, cannot create channel.");
            }
        }
    }

    private Notification buildNotification(String alarmNote, int alarmId) {
        // --- Intent để mở AlarmRingActivity khi nhấn vào notification ---
        Intent notificationIntent = new Intent(this, AlarmRingActivity.class);
        // Đảm bảo Activity nhận được dữ liệu cần thiết
        notificationIntent.putExtra("alarmId", alarmId);
        notificationIntent.putExtra("alarmNote", alarmNote);
        // Quan trọng: Thêm cờ để đảm bảo Activity được mở đúng cách từ nền
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Request code nên duy nhất nếu bạn có nhiều loại pending intent hoặc nhiều báo thức cùng lúc có thể hiển thị
        PendingIntent pendingIntent = PendingIntent.getActivity(this, alarmId,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent cho hành động "Stop" (Giữ nguyên như trước)
        Intent stopIntent = new Intent(this, AlarmRingService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, alarmId + 1000, // Request code khác
                stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Báo thức đang kêu!")
                .setContentText(alarmNote)
                .setSmallIcon(R.drawable.baseline_access_alarms_24) // Thay bằng icon của bạn
                .setContentIntent(pendingIntent) // <== Đặt PendingIntent để mở AlarmRingActivity
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
//                .addAction(R.drawable.ic_launcher_foreground, getString(R.string.ok), stopPendingIntent) // Nút dừng
                .setAutoCancel(false) // Không tự hủy khi nhấn vào notification chính
                // Cân nhắc thêm:
                .setFullScreenIntent(pendingIntent, true) // Quan trọng: Cố gắng hiển thị Activity toàn màn hình ngay lập tức
                .build();
    }
}