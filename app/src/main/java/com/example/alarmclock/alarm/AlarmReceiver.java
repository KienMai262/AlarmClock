package com.example.alarmclock.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

// Import lớp AlarmData và Pair từ package của bạn
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
        String timerString = intent.getStringExtra("timerString"); // <= NHẬN timerString
        int loopIndex = intent.getIntExtra("loopIndex", -1);
        boolean deleteAfterAlarm = intent.getBooleanExtra("deleteAfterAlarm", false);
        boolean[] customDays = intent.getBooleanArrayExtra("customDays");
        int indexMusic = intent.getIntExtra("indexMusic", -1);
        boolean knoll = intent.getBooleanExtra("knoll", false);

        Log.d(TAG, "Received alarmId: " + alarmId);
        Log.d(TAG, "Received timerString: " + timerString); // Log timerString
        Log.d(TAG, "Received soundResourceId: " + soundResourceId);
        Log.d(TAG, "Received alarmNote: " + alarmNote);
        Log.d(TAG, "Received loopIndex: " + loopIndex);
        Log.d(TAG, "Received deleteAfterAlarm: " + deleteAfterAlarm);
        Log.d(TAG, "Received customDays: " + (customDays != null ? Arrays.toString(customDays) : "null"));
        Log.d(TAG, "Received indexMusic: " + indexMusic);
        Log.d(TAG, "Received knoll: " + knoll);

        // --- PHÂN TÍCH timerString thành hour và minute ---
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
                        hour = -1; minute = -1; // Đánh dấu không hợp lệ
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

        // --- Kiểm tra dữ liệu cơ bản ---
        // Báo thức một lần (loopIndex=0) không cần kiểm tra customDays
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
            return;
        }

        // Kiểm tra soundResourceId (chấp nhận cả ID mặc định)
        if (soundResourceId <= 0) {
            Log.w(TAG, "Received invalid soundResourceId ("+ soundResourceId +"). Will use default if needed, but logging warning.");
            // Không return ở đây, vì Service có thể dùng default
        }


        // --- WakeLock ---
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            wakeLock.acquire(WAKE_LOCK_TIMEOUT);
            Log.d(TAG, "WakeLock acquired");
        } else {
            Log.w(TAG,"PowerManager is null, cannot acquire WakeLock");
        }


        // --- Quyết định có nên đổ chuông hôm nay không ---
        boolean shouldRingToday = checkShouldRingToday(loopIndex, customDays);
        Log.d(TAG, "Alarm " + alarmId + ": Should ring today? " + shouldRingToday);

        if (shouldRingToday) {
            // --- Khởi chạy Foreground Service ---
            Log.i(TAG, "Conditions met for alarm " + alarmId + ". Starting AlarmRingService.");
            Intent serviceIntent = new Intent(context, AlarmRingService.class);
            serviceIntent.putExtra("alarmId", alarmId);
            // Sử dụng soundResourceId đã kiểm tra (có thể là default)
            serviceIntent.putExtra("soundResourceId", (soundResourceId <= 0) ? DEFAULT_SOUND_RESOURCE_ID : soundResourceId);
            serviceIntent.putExtra("alarmNote", alarmNote);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting AlarmRingService for alarm " + alarmId, e);
                // Có thể lỗi quyền FOREGROUND_SERVICE hoặc lỗi khác
            }
        } else {
            Log.i(TAG, "Alarm " + alarmId + " should not ring today based on repeat settings.");
        }


        // --- Lên lịch lại cho lần xuất hiện tiếp theo (nếu cần) ---
        if (loopIndex != 0) { // Báo thức lặp lại
            Log.d(TAG, "Rescheduling repeating alarm with id: " + alarmId);
            // Gọi hàm reschedule, dùng hour, minute đã parse
            rescheduleNextAlarm(context, alarmId, hour, minute, loopIndex, customDays, indexMusic, knoll, deleteAfterAlarm, alarmNote);
        } else {
            /// --- Xử lý báo thức một lần ---
            Log.i(TAG,"One-time alarm " + alarmId + " triggered.");

            // Hủy PendingIntent trước
            AlarmSchedulerUtil.cancelAlarm(context.getApplicationContext(), alarmId);

            // --- Gửi yêu cầu đến WorkManager để xử lý xóa/tắt ---
            Log.d(TAG, "Enqueueing background work for alarmId: " + alarmId + ", shouldDelete: " + deleteAfterAlarm);

            // 1. Tạo dữ liệu đầu vào cho Worker
            Data workerData = new Data.Builder()
                    .putInt(AlarmActionWorker.KEY_ALARM_ID, alarmId)
                    .putBoolean(AlarmActionWorker.KEY_SHOULD_DELETE, deleteAfterAlarm)
                    .build();

            // 2. (Tùy chọn) Tạo ràng buộc cho Worker (ví dụ: chỉ chạy khi có mạng - không cần thiết cho việc này)
            // Constraints constraints = new Constraints.Builder()
            //        .setRequiredNetworkType(NetworkType.CONNECTED)
            //        .build();

            // 3. Tạo WorkRequest
            OneTimeWorkRequest alarmActionWorkRequest =
                    new OneTimeWorkRequest.Builder(AlarmActionWorker.class)
                            .setInputData(workerData)
                            // .setConstraints(constraints) // Thêm ràng buộc nếu cần
                            .addTag("alarm_action_" + alarmId) // Thêm tag để có thể theo dõi/hủy nếu cần
                            .build();

            // 4. Gửi yêu cầu đến WorkManager
            try {
                WorkManager.getInstance(context.getApplicationContext()).enqueue(alarmActionWorkRequest);
                Log.i(TAG, "Work request enqueued successfully for alarmId: " + alarmId);
            } catch (Exception e){
                Log.e(TAG,"Error enqueuing work request for alarm " + alarmId, e);
                // Xử lý lỗi nếu không gửi được yêu cầu (hiếm khi xảy ra)
            }
        }

        // --- Giải phóng WakeLock ---
        // Di chuyển vào finally để đảm bảo luôn được giải phóng
        // Tuy nhiên, với acquire(timeout), nó sẽ tự giải phóng, nhưng gọi release vẫn tốt
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
                Log.d(TAG, "WakeLock released");
            } catch (Exception e) {
                Log.w(TAG,"Error releasing WakeLock (might already be released by timeout)", e);
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
            case 2: return today != Calendar.SUNDAY; // T2-T7
            case 3:
                if (customDays == null || customDays.length != 7) return false;
                int dayIndex = (today == Calendar.SUNDAY) ? 6 : today - 2; // Ánh xạ 0=T2..6=CN
                return (dayIndex >= 0 && dayIndex < 7) && customDays[dayIndex];
            default: return false;
        }
    }

    // --- Hàm rescheduleNextAlarm (Giữ nguyên logic tái tạo AlarmData) ---
    private void rescheduleNextAlarm(Context context, int alarmId, int hour, int minute, int loopIndex, boolean[] customDays, int indexMusic, boolean knoll, boolean deleteAfterAlarm, String note) {
        Log.d(TAG, "Attempting to reschedule alarm ID: " + alarmId + " by reconstructing AlarmData.");
        String timerStringReconstructed = String.format("%02d:%02d", hour, minute);
        AlarmData reconstructedData = new AlarmData(timerStringReconstructed, indexMusic, knoll, deleteAfterAlarm, note, loopIndex);
        if (loopIndex == 3 && customDays != null && customDays.length == 7) {
            try {
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