package com.example.alarmclock.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.alarmclock.alarm.AlarmData;
import com.example.alarmclock.Pair;
import com.example.alarmclock.R; // Import R

import java.util.Calendar;
import java.util.Arrays;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String WAKE_LOCK_TAG = "AlarmClock:AlarmReceiverWakeLock";
    private static final long WAKE_LOCK_TIMEOUT = 10000L; // 10 giây

    private static final int DEFAULT_SOUND_RESOURCE_ID = R.raw.see_you_again_meo; // Đảm bảo ID này tồn tại

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmReceiver triggered. Action: " + intent.getAction());

        // --- Xử lý khởi động lại thiết bị (Giữ nguyên) ---
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed received. Rescheduling alarms...");
            AlarmSchedulerUtil.rescheduleAllAlarms(context.getApplicationContext()); // Gọi hàm reschedule
            return;
        }

        // --- Lấy dữ liệu từ Intent ---
        int alarmId = intent.getIntExtra("alarmId", -1);
        int soundResourceId = intent.getIntExtra("soundResourceId", DEFAULT_SOUND_RESOURCE_ID);
        String alarmNote = intent.getStringExtra("alarmNote");
        String timerString = intent.getStringExtra("timerString");
        int loopIndex = intent.getIntExtra("loopIndex", -1);
        // Lấy cờ deleteAfterAlarm để truyền đi
        boolean deleteAfterAlarm = intent.getBooleanExtra("deleteAfterAlarm", false);
        boolean[] customDays = intent.getBooleanArrayExtra("customDays");
        int indexMusic = intent.getIntExtra("indexMusic", -1);
        boolean knoll = intent.getBooleanExtra("knoll", false);
        // Lấy thông tin quiz để truyền đi
        String subject = intent.getStringExtra("subject");
        String topic = intent.getStringExtra("topic");
        String difficulty = intent.getStringExtra("difficulty");
        int numQuestions = intent.getIntExtra("numQuestions", 5);

        // Log thông tin nhận được (bao gồm cả deleteAfterAlarm)
        Log.d(TAG, "Received alarmId: " + alarmId);
        Log.d(TAG, "Received timerString: " + timerString);
        Log.d(TAG, "Received soundResourceId: " + soundResourceId);
        Log.d(TAG, "Received alarmNote: " + alarmNote);
        Log.d(TAG, "Received loopIndex: " + loopIndex);
        Log.d(TAG, "Received deleteAfterAlarm: " + deleteAfterAlarm); // Log giá trị này
        Log.d(TAG, "Received customDays: " + (customDays != null ? Arrays.toString(customDays) : "null"));
        Log.d(TAG, "Received indexMusic: " + indexMusic);
        Log.d(TAG, "Received knoll: " + knoll);
        Log.d(TAG, "Received Subject: " + subject);
        Log.d(TAG, "Received Topic: " + topic);
        Log.d(TAG, "Received Difficulty: " + difficulty);
        Log.d(TAG, "Received NumQuestions: " + numQuestions);


        // --- PHÂN TÍCH timerString thành hour và minute (Giữ nguyên) ---
        int hour = -1;
        int minute = -1;
        if (timerString != null && timerString.contains(":")) {
            try {
                String[] parts = timerString.split(":");
                if (parts.length >= 2) {
                    hour = Integer.parseInt(parts[0].trim());
                    minute = Integer.parseInt(parts[1].trim());
                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        Log.e(TAG, "Parsed time is invalid: " + hour + ":" + minute);
                        hour = -1; minute = -1;
                    } else {
                        Log.d(TAG, "Parsed time: " + hour + ":" + minute);
                    }
                } else {
                    Log.w(TAG, "timerString doesn't have enough parts: " + timerString);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Error parsing timerString: " + timerString, e);
            }
        } else {
            Log.e(TAG, "Received null or invalid format timerString: '" + timerString + "'");
        }

        // --- Kiểm tra dữ liệu cơ bản (Giữ nguyên) ---
        boolean customDaysCheckNeeded = (loopIndex == 3);
        boolean customDaysValidOrNotNeeded = !customDaysCheckNeeded || (customDays != null && customDays.length == 7);

        if (alarmId == -1 || hour == -1 || minute == -1 || loopIndex == -1 || indexMusic == -1 || !customDaysValidOrNotNeeded) {
            Log.e(TAG, "Invalid essential data received or parsed. Aborting." +
                    " alarmId=" + alarmId +
                    ", timerString='" + timerString + "'" +
                    ", parsedHour=" + hour +
                    ", parsedMinute=" + minute +
                    ", loopIndex=" + loopIndex +
                    ", indexMusic=" + indexMusic +
                    ", customDaysValidOrNotNeeded=" + customDaysValidOrNotNeeded);
            // Giải phóng WakeLock nếu đã giữ trước khi return
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock tempWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG + "_temp");
                if (tempWakeLock.isHeld()) { // Kiểm tra xem có đang giữ bởi tag này không (khó chính xác)
                    try { tempWakeLock.release(); } catch (Exception ignored) {} // Cố gắng giải phóng
                }
            }
            return;
        }

        // Kiểm tra soundResourceId (Giữ nguyên)
        if (soundResourceId <= 0) {
            Log.w(TAG, "Received invalid soundResourceId ("+ soundResourceId +"). Will use default if needed, but logging warning.");
        }


        // --- WakeLock (Giữ nguyên) ---
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        try { // Sử dụng try-finally để đảm bảo WakeLock được giải phóng
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
                wakeLock.acquire(WAKE_LOCK_TIMEOUT);
                Log.d(TAG, "WakeLock acquired");
            } else {
                Log.w(TAG,"PowerManager is null, cannot acquire WakeLock");
            }

            // --- Quyết định có nên đổ chuông hôm nay không (Giữ nguyên) ---
            boolean shouldRingToday = checkShouldRingToday(loopIndex, customDays);
            Log.d(TAG, "Alarm " + alarmId + ": Should ring today? " + shouldRingToday);

            if (shouldRingToday) {
                // --- Khởi chạy Foreground Service ---
                Log.i(TAG, "Conditions met for alarm " + alarmId + ". Starting AlarmRingService.");
                Intent serviceIntent = new Intent(context, AlarmRingService.class);
                serviceIntent.putExtra("alarmId", alarmId);
                serviceIntent.putExtra("soundResourceId", (soundResourceId <= 0) ? DEFAULT_SOUND_RESOURCE_ID : soundResourceId);
                serviceIntent.putExtra("alarmNote", alarmNote);
                // Truyền thông tin Quiz VÀ cờ deleteAfterAlarm
                serviceIntent.putExtra("subject", subject);
                serviceIntent.putExtra("topic", topic);
                serviceIntent.putExtra("difficulty", difficulty);
                serviceIntent.putExtra("numQuestions", numQuestions);
                serviceIntent.putExtra("deleteAfterAlarm", deleteAfterAlarm); // <-- TRUYỀN CỜ QUAN TRỌNG NÀY
                // serviceIntent.putExtra("loopIndex", loopIndex); // Có thể cần nếu Service cần thông tin này

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error starting AlarmRingService for alarm " + alarmId, e);
                }
            } else {
                Log.i(TAG, "Alarm " + alarmId + " should not ring today based on repeat settings.");
            }

            // --- Lên lịch lại cho lần xuất hiện tiếp theo (nếu cần) ---
            if (loopIndex != 0) { // Báo thức lặp lại
                Log.d(TAG, "Rescheduling repeating alarm with id: " + alarmId);
                // Gọi hàm reschedule, dùng hour, minute đã parse (Giữ nguyên)
                rescheduleNextAlarm(context, alarmId, hour, minute, loopIndex, customDays, indexMusic, knoll, deleteAfterAlarm, alarmNote, subject, topic, difficulty, numQuestions);
            } else {
                /// --- Xử lý báo thức một lần ---
                Log.i(TAG,"One-time alarm " + alarmId + " triggered.");

                // Chỉ hủy PendingIntent. Không lên lịch worker ở đây nữa.
                AlarmSchedulerUtil.cancelAlarm(context.getApplicationContext(), alarmId);
                Log.d(TAG,"One-time alarm pending intent cancelled. Action will be handled by QuizActivity if needed.");

                // !!! ĐÃ XOÁ KHỐI LÊN LỊCH WORKER Ở ĐÂY !!!
            }

        } finally {
            // --- Giải phóng WakeLock ---
            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                    Log.d(TAG, "WakeLock released in finally block");
                } catch (Exception e) {
                    Log.w(TAG,"Error releasing WakeLock in finally block", e);
                }
            }
        }
    }

    // --- Hàm checkShouldRingToday (Giữ nguyên) ---
    private boolean checkShouldRingToday(int loopIndex, boolean[] customDays) {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK); // SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        switch (loopIndex) {
            case 0: return true; // Một lần thì luôn là hôm nay nếu được trigger
            case 1: return true; // Hàng ngày
            case 2: return today != Calendar.SUNDAY && today != Calendar.SATURDAY; // T2-T6 (Sửa lại nếu logic cũ là T2-T7)
            case 3:
                if (customDays == null || customDays.length != 7) return false;
                // Ánh xạ Calendar.DAY_OF_WEEK (1=CN, 2=T2, ..., 7=T7) sang index của mảng customDays (0=T2, ..., 5=T7, 6=CN)
                int dayIndex;
                if (today == Calendar.SUNDAY) {
                    dayIndex = 6; // Chủ Nhật là index 6
                } else {
                    dayIndex = today - 2; // T2 là index 0, T3 là 1, ... T7 là 5
                }
                return (dayIndex >= 0 && dayIndex < 7) && customDays[dayIndex];
            default: return false;
        }
    }

    // --- Hàm rescheduleNextAlarm (Giữ nguyên) ---
    private void rescheduleNextAlarm(Context context, int alarmId, int hour, int minute, int loopIndex, boolean[] customDays, int indexMusic, boolean knoll, boolean deleteAfterAlarm, String note, String subject, String topic, String difficulty, int numQuestions) {
        Log.d(TAG, "Attempting to reschedule alarm ID: " + alarmId + " by reconstructing AlarmData.");
        String timerStringReconstructed = String.format("%02d:%02d", hour, minute);
        AlarmData reconstructedData = new AlarmData(timerStringReconstructed, indexMusic, knoll, deleteAfterAlarm, note, loopIndex, subject, topic, difficulty, numQuestions);
        if (loopIndex == 3 && customDays != null && customDays.length == 7) {
            try {
                // Giả sử mapping: 0=T2, 1=T3, ..., 5=T7, 6=CN
                reconstructedData.optionOther[0] = new Pair<>(R.string.monday,    customDays[0]);
                reconstructedData.optionOther[1] = new Pair<>(R.string.tuesday,   customDays[1]);
                reconstructedData.optionOther[2] = new Pair<>(R.string.wednesday, customDays[2]);
                reconstructedData.optionOther[3] = new Pair<>(R.string.thursday,  customDays[3]);
                reconstructedData.optionOther[4] = new Pair<>(R.string.friday,    customDays[4]);
                reconstructedData.optionOther[5] = new Pair<>(R.string.saturday,  customDays[5]);
                reconstructedData.optionOther[6] = new Pair<>(R.string.sunday,    customDays[6]);
            } catch (Exception e) { Log.e(TAG,"Error reconstructing optionOther.", e); return; }
        }
        Log.d(TAG, "Calling AlarmSchedulerUtil.scheduleAlarm with fully reconstructed AlarmData for alarmId: " + alarmId);
        AlarmSchedulerUtil.scheduleAlarm(context.getApplicationContext(), reconstructedData, alarmId);
    }
}