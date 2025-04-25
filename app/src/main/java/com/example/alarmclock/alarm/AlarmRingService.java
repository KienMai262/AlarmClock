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

// import com.example.alarmclock.MainActivity; // Không cần nếu không dùng
import com.example.alarmclock.R;

import java.io.IOException;

public class AlarmRingService extends Service {

    private static final String TAG = "AlarmRingService";
    private static final String CHANNEL_ID = "ALARM_RING_CHANNEL";
    private static final int NOTIFICATION_ID = 123; // ID duy nhất cho notification
    public static final String ACTION_STOP_ALARM = "com.example.alarmclock.STOP_ALARM";
    public static final String ACTION_REDUCE_VOLUME = "com.example.alarmclock.REDUCE_VOLUME"; // Action mới

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private int originalVolume = -1; // Khởi tạo -1 để biết chưa lưu
    private boolean isVibrating = false; // Theo dõi trạng thái rung
    private int currentAlarmId = -1;
    private boolean isVolumeReduced = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Không dùng binding
    }

    @Override
    public void onCreate() {
        // Không cần gọi super.onCreate() hai lần
        Log.d(TAG, "Service onCreate");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        createNotificationChannel();
        Log.e("ALARM_DEBUG", "!!!!!!!!!! AlarmRingService onCreate !!!!!!!!!!");
        // super.onCreate(); // <= XÓA DÒNG NÀY
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("ALARM_DEBUG", "!!!!!!!!!! AlarmRingService onStartCommand TOP !!!!!!!!!!");

        // Kiểm tra intent null ngay từ đầu
        if (intent == null) {
            Log.e(TAG, "onStartCommand called with a null intent. Stopping service.");
            stopSelf(); // Dừng service nếu intent là null (thường xảy ra khi service bị khởi động lại bởi hệ thống với START_STICKY)
            return START_NOT_STICKY; // Nên trả về START_NOT_STICKY
        }

        Log.d(TAG, "Service onStartCommand - Full Intent: " + intent.toString() + " Extras: " + intent.getExtras()); // Log cả extras

        // Xử lý action dừng báo thức hoặc giảm âm lượng
        String action = intent.getAction();
        if (ACTION_STOP_ALARM.equals(action)) {
            Log.d(TAG, "Received stop action.");
            stopAlarmSound(true); // Dừng nhạc, rung, khôi phục âm lượng
            stopForeground(true); // Gỡ bỏ foreground notification
            stopSelf(); // Dừng service
            return START_NOT_STICKY;
        } else if (ACTION_REDUCE_VOLUME.equals(action)) {
            Log.d(TAG, "Received reduce volume action.");
            reduceVolume();
            stopVibration(); // Tắt rung khi bắt đầu quiz
            // Không dừng service, chỉ giảm âm lượng và tắt rung
            return START_STICKY; // Giữ service chạy để có thể dừng sau khi quiz xong
        }

        // Nếu không phải là action đặc biệt, xử lý như một lần khởi chạy báo thức mới

        // Lấy dữ liệu từ Intent được gửi bởi AlarmReceiver
        currentAlarmId = intent.getIntExtra("alarmId", -1);
        int soundResourceId = intent.getIntExtra("soundResourceId", -1);
        String alarmNote = intent.getStringExtra("alarmNote");
        String subject = intent.getStringExtra("subject");
        String topic = intent.getStringExtra("topic");
        String difficulty = intent.getStringExtra("difficulty");
        int numQuestions = intent.getIntExtra("numQuestions", 5);
        // !!! NHẬN THÊM CỜ deleteAfterAlarm !!!
        boolean deleteAfterAlarm = intent.getBooleanExtra("deleteAfterAlarm", false);

        // Log thông tin nhận được
        Log.i(TAG, "Starting/Processing foreground service for alarmId: " + currentAlarmId);
        Log.d(TAG, "Sound resource ID: " + soundResourceId);
        Log.d(TAG, "Alarm note: " + alarmNote);
        Log.d(TAG, "Subject: " + subject);
        Log.d(TAG, "Topic: " + topic);
        Log.d(TAG, "Difficulty: " + difficulty);
        Log.d(TAG, "NumQuestions: " + numQuestions);
        Log.d(TAG, "DeleteAfterAlarm Flag: " + deleteAfterAlarm); // Log cờ mới

        // Kiểm tra alarmId hợp lệ
        if (currentAlarmId == -1) {
            Log.e(TAG, "Invalid alarmId (-1) received. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Ghi chú mặc định
        if (alarmNote == null || alarmNote.isEmpty()) {
            alarmNote = getString(R.string.note_default); // Dùng string resource
        }

        // Tạo và hiển thị notification foreground
        Log.d("ALARM_DEBUG", "Building notification...");
        Notification notification = buildNotification(alarmNote, currentAlarmId, subject, topic, difficulty, numQuestions, deleteAfterAlarm); // Truyền thêm dữ liệu
        if (notification == null) {
            Log.e("ALARM_DEBUG", "!!! buildNotification returned null! Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }
        Log.d("ALARM_DEBUG", "Calling startForeground...");
        try {
            startForeground(NOTIFICATION_ID, notification);
            Log.d("ALARM_DEBUG", "startForeground successful.");
        } catch (Exception e) {
            Log.e("ALARM_DEBUG", "!!!!!!!!!! EXCEPTION during startForeground !!!!!!!!!!", e);
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show(); // Dùng string resource
            stopSelf();
            return START_NOT_STICKY;
        }

        // Bắt đầu phát âm thanh và rung (chỉ khi chưa giảm âm lượng)
        if (!isVolumeReduced) {
            Log.d("ALARM_DEBUG", "Calling startAlarmSound...");
            startAlarmSound(soundResourceId);
            Log.d("ALARM_DEBUG", "Calling startVibration...");
            startVibration();
        } else {
            Log.d("ALARM_DEBUG", "Volume already reduced, not restarting sound/vibration.");
        }


        // ----- KHỞI CHẠY ALARM RING ACTIVITY -----
        // Logic này có thể cần xem xét lại. Có nên luôn khởi chạy Activity mỗi khi onStartCommand?
        // Hay chỉ khởi chạy lần đầu? Hiện tại nó sẽ chạy mỗi lần onStartCommand (nếu không phải action đặc biệt).
        // Nếu Activity đã mở, FLAG_ACTIVITY_SINGLE_TOP sẽ đưa nó lên trước thay vì tạo mới.
        Log.d("ALARM_DEBUG", "Attempting to explicitly start AlarmRingActivity...");
        Intent ringActivityIntent = new Intent(this, AlarmRingActivity.class);
        ringActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ringActivityIntent.putExtra("alarmId", currentAlarmId);
        ringActivityIntent.putExtra("alarmNote", alarmNote);
        ringActivityIntent.putExtra("subject", subject);
        ringActivityIntent.putExtra("topic", topic);
        ringActivityIntent.putExtra("difficulty", difficulty);
        ringActivityIntent.putExtra("numQuestions", numQuestions);
        // !!! TRUYỀN TIẾP CỜ deleteAfterAlarm !!!
        ringActivityIntent.putExtra("deleteAfterAlarm", deleteAfterAlarm);

        try {
            startActivity(ringActivityIntent);
            Log.d(TAG, "Started AlarmRingActivity successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error starting AlarmRingActivity", e);
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show(); // Dùng string resource
            // Cân nhắc dừng service nếu không mở được UI?
            // stopSelf();
            // return START_NOT_STICKY;
        }

        return START_NOT_STICKY; // Báo thức không nên tự khởi động lại nếu bị kill
    }

    // --- Hàm reduceVolume (Giữ nguyên) ---
    private void reduceVolume() {
        if (mediaPlayer != null && mediaPlayer.isPlaying() && audioManager != null && !isVolumeReduced) {
            try {
                // Lưu âm lượng gốc nếu chưa lưu
                if (originalVolume == -1) {
                    originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                }
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                // Giảm âm lượng xuống mức thấp hơn (ví dụ 20% hoặc một giá trị cố định thấp)
                int reducedVolume = Math.max(1, maxVolume / 5); // Ví dụ giảm còn 20%
                // Hoặc có thể dùng mức cố định thấp, ví dụ 2 hoặc 3 tùy thuộc vào maxVolume
                // int reducedVolume = Math.min(3, maxVolume); // Ví dụ: tối đa là 3

                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, reducedVolume, 0);
                isVolumeReduced = true;
                Log.d(TAG, "Reduced ALARM stream volume to: " + reducedVolume);
            } catch (Exception e) {
                Log.e(TAG, "Error reducing volume", e);
            }
        } else {
            Log.w(TAG, "Cannot reduce volume: MediaPlayer not playing, AudioManager null, or volume already reduced.");
        }
    }


    // --- Hàm startAlarmSound (Giữ nguyên, có thể tối ưu hóa) ---
    private void startAlarmSound(int soundResourceId) {
        // Kiểm tra nếu đang chạy và chưa giảm âm lượng thì mới bắt đầu
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            Log.w(TAG,"MediaPlayer is already playing.");
            // Không nên return ngay, vì có thể cần cập nhật âm lượng nếu chưa max
        }

        // Nếu chưa có mediaPlayer hoặc đã release thì tạo mới
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else {
            // Nếu đang chạy nhưng âm lượng đã giảm thì không làm gì cả
            if (mediaPlayer.isPlaying() && isVolumeReduced) {
                Log.d(TAG, "MediaPlayer playing but volume reduced, not restarting.");
                return;
            }
            mediaPlayer.reset(); // Reset nếu không chạy hoặc âm lượng chưa giảm
        }


        if (soundResourceId != -1 && soundResourceId != 0) {
            Log.i(TAG, "Attempting to play sound with resource ID: " + soundResourceId);

            // Tối đa hóa âm lượng (chỉ nếu chưa giảm)
            if (audioManager != null && !isVolumeReduced) {
                try {
                    // Lưu âm lượng gốc nếu chưa lưu
                    if (originalVolume == -1) {
                        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    }
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
                    Log.d(TAG, "Set ALARM stream volume to max: " + maxVolume);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting AudioManager volume", e);
                }
            } else if (audioManager == null) {
                Log.e(TAG, "AudioManager is null.");
            }

            // Khởi tạo MediaPlayer
            try {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);

                Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + soundResourceId);
                mediaPlayer.setDataSource(this, soundUri);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepareAsync(); // Chuẩn bị bất đồng bộ

                mediaPlayer.setOnPreparedListener(mp -> {
                    Log.d(TAG, "MediaPlayer prepared. Starting playback.");
                    try {
                        // Chỉ start nếu service chưa bị yêu cầu dừng trong lúc prepare
                        if (mediaPlayer != null) { // Kiểm tra lại mediaPlayer phòng trường hợp bị release
                            mp.start();
                        }
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "IllegalStateException on MediaPlayer start after prepare", e);
                        stopAlarmSound(true); // Dừng nếu lỗi
                    }
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show(); // Dùng string resource
                    stopAlarmSound(true); // Dừng nếu lỗi
                    return true; // Đã xử lý lỗi
                });

            } catch (IOException | IllegalArgumentException | SecurityException | IllegalStateException e) {
                Log.e(TAG, "Error setting data source or preparing MediaPlayer", e);
                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show(); // Dùng string resource
                releaseMediaPlayer();
            }
        } else {
            Log.e(TAG, "Invalid sound resource ID received: " + soundResourceId + ". Cannot play sound.");
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show(); // Dùng string resource
            // Cân nhắc phát âm thanh mặc định của hệ thống ở đây nếu muốn
        }
    }

    // --- Hàm startVibration (Giữ nguyên) ---
    private void startVibration() {
        // Chỉ rung nếu chưa giảm âm lượng (logic mới)
        if (isVolumeReduced) {
            Log.d(TAG,"Volume reduced, skipping vibration start.");
            return;
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 1000};
            int repeatIndex = 0;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex));
                } else {
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


    // --- Hàm stopAlarmSound (Giữ nguyên) ---
    private void stopAlarmSound(boolean restoreVolume) {
        Log.i(TAG, "Stopping alarm sound. Restore volume: " + restoreVolume);
        releaseMediaPlayer(); // Dừng và giải phóng media player
        stopVibration();      // Dừng rung

        // Khôi phục âm lượng gốc CHỈ KHI được yêu cầu
        if (restoreVolume && audioManager != null && originalVolume != -1) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
                Log.d(TAG, "Restored ALARM stream volume to: " + originalVolume);
            } catch (Exception e) {
                Log.e(TAG, "Error restoring volume", e);
            } finally {
                // Reset lại các trạng thái liên quan đến âm lượng/rung
                originalVolume = -1;
                isVolumeReduced = false;
            }
        } else if (!restoreVolume){
            Log.d(TAG, "Volume not restored (likely entering quiz or already restored).");
            // Không cần reset isVolumeReduced ở đây vì nó sẽ được reset khi khôi phục thực sự
        }
    }

    // --- Hàm stopVibration (Giữ nguyên) ---
    private void stopVibration() {
        if (vibrator != null && isVibrating) { // Chỉ cancel nếu đang rung
            try {
                vibrator.cancel();
                Log.d(TAG, "Stopped vibration.");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping vibration", e);
            } finally {
                isVibrating = false; // Luôn đặt lại cờ sau khi cố gắng cancel
            }
        }
    }


    // --- Hàm releaseMediaPlayer (Giữ nguyên) ---
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            Log.d(TAG,"Releasing MediaPlayer...");
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset(); // Reset trạng thái trước khi release
                mediaPlayer.release(); // Giải phóng tài nguyên
                Log.d(TAG,"MediaPlayer released.");
            } catch (Exception e) {
                Log.e(TAG, "Exception while releasing MediaPlayer", e);
            } finally {
                mediaPlayer = null; // Đặt về null để có thể tạo mới lần sau
            }
        }
    }

    // --- Hàm onDestroy (Giữ nguyên) ---
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        stopAlarmSound(true); // Đảm bảo mọi thứ dừng và khôi phục khi service bị hủy
        super.onDestroy();
    }

    // --- Hàm createNotificationChannel (Giữ nguyên) ---
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.alarms); // Thêm chuỗi này vào strings.xml
            String description = getString(R.string.alarms); // Thêm chuỗi này vào strings.xml
            int importance = NotificationManager.IMPORTANCE_HIGH; // Quan trọng cao để hiển thị head-up
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
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

    // --- Hàm buildNotification (Cập nhật để truyền thêm dữ liệu) ---
    private Notification buildNotification(String alarmNote, int alarmId, String subject, String topic, String difficulty, int numQuestions, boolean deleteAfterAlarm) {
        // Intent để mở AlarmRingActivity khi nhấn vào notification
        Intent notificationIntent = new Intent(this, AlarmRingActivity.class);
        // Đưa TẤT CẢ dữ liệu cần thiết cho Activity vào Intent này
        notificationIntent.putExtra("alarmId", alarmId);
        notificationIntent.putExtra("alarmNote", alarmNote);
        notificationIntent.putExtra("subject", subject);
        notificationIntent.putExtra("topic", topic);
        notificationIntent.putExtra("difficulty", difficulty);
        notificationIntent.putExtra("numQuestions", numQuestions);
        // !!! TRUYỀN CỜ deleteAfterAlarm VÀO INTENT NÀY !!!
        notificationIntent.putExtra("deleteAfterAlarm", deleteAfterAlarm);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // PendingIntent để mở Activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, alarmId, // Dùng alarmId làm request code
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // PendingIntent cho hành động "Stop"
        Intent stopIntent = new Intent(this, AlarmRingService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        // Request code cần khác với PendingIntent mở Activity
        PendingIntent stopPendingIntent = PendingIntent.getService(this, alarmId + 1000,
                stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Xây dựng Notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Báo thức đang kêu!")
                .setContentText(alarmNote)
                .setSmallIcon(R.drawable.baseline_access_alarms_24) // Thay bằng icon của bạn
                .setContentIntent(pendingIntent) // <== Đặt PendingIntent để mở AlarmRingActivity
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false) // Không tự hủy khi nhấn vào notification chính
                .setFullScreenIntent(pendingIntent, true) // Quan trọng: Cố gắng hiển thị Activity toàn màn hình ngay lập tức
                .build();
    }
}