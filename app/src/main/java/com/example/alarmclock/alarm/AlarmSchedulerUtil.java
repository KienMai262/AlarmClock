package com.example.alarmclock.alarm;

import static com.example.alarmclock.ui.create.CreateFragment.alarmDataList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

// Import lớp AlarmData và Pair từ package của bạn
import com.example.alarmclock.alarm.AlarmData;
import com.example.alarmclock.Pair;
import com.example.alarmclock.R;
// Có thể cần MainActivity cho context để start activity xin quyền
import com.example.alarmclock.MainActivity;


import java.util.Calendar;

public class AlarmSchedulerUtil {
    private static final String TAG = "AlarmSchedulerUtil";

    // rescheduleAllAlarms gọi scheduleAlarm đã được sửa đổi
    public static void rescheduleAllAlarms(Context context) {
        if (alarmDataList != null) {
            synchronized (alarmDataList) {
                Log.i(TAG, "Rescheduling all alarms. Current list size: " + alarmDataList.size());
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager == null) {
                    Log.e(TAG, "AlarmManager is null, cannot reschedule.");
                    return;
                }

                for (int i = 0; i < alarmDataList.size(); i++) {
                    Pair<AlarmData, Boolean> pair = alarmDataList.get(i);
                    if (pair != null && pair.first != null && pair.second != null) { // Thêm kiểm tra pair.second != null
                        if (pair.second) { // Chỉ lên lịch lại nếu đang bật (true)
                            AlarmData alarm = pair.first;
                            // Hủy lịch cũ trước khi đặt lịch mới để tránh trùng lặp
                            cancelAlarm(context, i);
                            scheduleAlarm(context, alarm, i); // Gọi hàm schedule đã sửa
                            Log.d(TAG, "Attempted reschedule for enabled alarm at index " + i + " with timerString: " + alarm.timerString);
                        } else {
                            // Nếu báo thức bị tắt (false), đảm bảo hủy mọi lịch trình còn sót lại
                            cancelAlarm(context, i);
                            Log.d(TAG, "Cancelled any pending alarm for disabled alarm at index " + i);
                        }
                    } else {
                        Log.w(TAG, "Skipping reschedule for index " + i + ". Data pair, alarm data, or enabled status is null.");
                        // Vẫn nên hủy nếu có thể có lịch cũ
                        cancelAlarm(context, i);
                    }
                }
                Log.i(TAG, "Finished rescheduling all alarms.");
            }
        } else {
            Log.w(TAG, "alarmDataList is null, cannot reschedule alarms.");
        }
    }

    // scheduleAlarm - SỬA ĐỔI để gửi timerString, không gửi hour/minute
    public static void scheduleAlarm(Context context, AlarmData alarm, int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null, cannot schedule alarm index: " + alarmId);
            return;
        }
        if (alarm == null) {
            Log.e(TAG, "AlarmData is null for index: " + alarmId + ". Cannot schedule.");
            return;
        }

        // --- Kiểm tra quyền (Giữ nguyên) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted. Requesting...");
                Toast.makeText(context, R.string.alarms, Toast.LENGTH_LONG).show(); // Thêm string
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                // Cố gắng lấy MainActivity context nếu có thể
                if (context instanceof MainActivity) {
                    context.startActivity(intent);
                } else {
                    // Nếu không, thông báo người dùng cần vào cài đặt
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Cần flag này nếu không phải từ Activity
                    try {
                        context.startActivity(intent); // Có thể vẫn hoạt động trên một số thiết bị
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot start ACTION_REQUEST_SCHEDULE_EXACT_ALARM from this context. User needs to grant manually.", e);
                        Toast.makeText(context, R.string.alarms, Toast.LENGTH_LONG).show(); // Thêm string
                    }
                }
                return; // Không tiếp tục nếu thiếu quyền
            }
        }
        // --- Kết thúc kiểm tra quyền ---


        // --- 1. Lấy timerString (Không cần parse ở đây nữa) ---
        String timerString = alarm.timerString;
        if (timerString == null || !timerString.contains(":")) {
            Log.e(TAG, "Invalid or missing timerString for alarm index " + alarmId + ": '" + timerString + "'. Cannot schedule.");
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show(); // Thêm string
            return;
        }

        // --- 2. Phân giải ID âm thanh ---
        int soundResourceId = -1;
        if (alarm.items != null && alarm.indexMusic >= 0 && alarm.indexMusic < alarm.items.length
                && alarm.items[alarm.indexMusic] != null && alarm.items[alarm.indexMusic].second != null) {
            soundResourceId = getSoundResourceIdFromString(context, alarm.items[alarm.indexMusic].second);
        } else {
            Log.w(TAG, "Invalid music items or index for alarm index " + alarmId + ". Cannot resolve sound.");
        }
        // Sử dụng ID mặc định nếu không phân giải được hoặc không hợp lệ
        if (soundResourceId <= 0) { // getIdentifier trả về 0 nếu không tìm thấy
            Log.w(TAG, "Using default sound resource for alarm index " + alarmId);
            soundResourceId = R.raw.see_you_again_meo; // ID mặc định của bạn
        }


        // --- 3. Tính toán thời gian trigger tiếp theo ---
        // Hàm calculateNextAlarmTime giờ sẽ nhận AlarmData để tự parse timerString
        Calendar calendar = calculateNextAlarmTime(alarm);
        if (calendar == null) {
            Log.e(TAG, "Could not calculate next alarm time for alarm index: " + alarmId);
            return;
        }
        Log.d(TAG, "Calculated next trigger time for alarm " + alarmId + ": " + calendar.getTime());


        // --- 4. Tạo Intent và thêm Extras ---
        Intent intent = new Intent(context, AlarmReceiver.class);
        // Đặt action để rõ ràng hơn (tùy chọn)
        intent.setAction("ALARM_TRIGGER_" + alarmId);

        intent.putExtra("alarmId", alarmId);
        intent.putExtra("soundResourceId", soundResourceId);
        intent.putExtra("alarmNote", alarm.note);
        intent.putExtra("timerString", timerString);           // <= GỬI timerString
        intent.putExtra("loopIndex", alarm.loopIndex);
        intent.putExtra("deleteAfterAlarm", alarm.deleteAfterAlarm != null && alarm.deleteAfterAlarm);
        intent.putExtra("indexMusic", alarm.indexMusic);
        intent.putExtra("knoll", alarm.knoll != null && alarm.knoll);

        // Chỉ thêm customDays nếu là kiểu tùy chỉnh
        if (alarm.loopIndex == 3) {
            boolean[] customDays = getCustomDaysArray(alarm.optionOther);
            if (customDays != null) {
                intent.putExtra("customDays", customDays);
            } else {
                Log.w(TAG,"Could not generate customDays array for alarm " + alarmId);
                // Có thể không lên lịch nếu customDays là bắt buộc? Hoặc bỏ qua extra này.
            }
        }
        Log.d(TAG,"Intent extras prepared for PendingIntent for alarm " + alarmId);

        // --- 5. Tạo PendingIntent ---
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // --- 6. Lên lịch báo thức ---
        try {
            // Sử dụng setExactAndAllowWhileIdle cho độ tin cậy cao nhất
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
            Log.i(TAG, "Alarm scheduled successfully for index " + alarmId + " at " + calendar.getTime());
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException: Cannot schedule exact alarm for index " + alarmId + ". Check permissions.", se);
            Toast.makeText(context, R.string.alarms, Toast.LENGTH_LONG).show(); // Thêm string
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm for index " + alarmId, e);
            Toast.makeText(context, R.string.alarms, Toast.LENGTH_SHORT).show(); // Thêm string
        }
    }

    // --- Hàm tính toán thời gian tiếp theo (SỬA ĐỔI để nhận AlarmData) ---
    private static Calendar calculateNextAlarmTime(AlarmData alarm) {
        if (alarm == null || alarm.timerString == null || !alarm.timerString.contains(":")) {
            Log.e(TAG, "calculateNextAlarmTime: Invalid AlarmData or timerString.");
            return null;
        }

        int hour = -1;
        int minute = -1;
        try {
            String[] parts = alarm.timerString.split(":");
            if (parts.length >= 2) {
                hour = Integer.parseInt(parts[0].trim());
                minute = Integer.parseInt(parts[1].trim());
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                Log.e(TAG, "calculateNextAlarmTime: Parsed invalid time: " + hour + ":" + minute);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "calculateNextAlarmTime: Error parsing timerString: " + alarm.timerString, e);
            return null;
        }

        Calendar nextTime = Calendar.getInstance();
        // Đặt giờ, phút, giây, mili giây
        nextTime.set(Calendar.HOUR_OF_DAY, hour);
        nextTime.set(Calendar.MINUTE, minute);
        nextTime.set(Calendar.SECOND, 0);
        nextTime.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();

        // Nếu là báo thức một lần VÀ thời gian đã qua hôm nay -> không hợp lệ để đặt nữa?
        // Hoặc nếu là báo thức một lần thì chỉ cần đảm bảo nó chưa qua
        if (alarm.loopIndex == 0) {
            if (nextTime.before(now)) {
                Log.w(TAG, "calculateNextAlarmTime: One-time alarm for " + alarm.timerString + " is in the past. Cannot schedule.");
                // Có thể trả về null hoặc một dấu hiệu lỗi khác
                // Hoặc nếu logic cho phép đặt báo thức một lần cho quá khứ (để test?), thì giữ lại.
                // Tạm thời trả về null để tránh đặt báo thức quá khứ.
                return null;
            } else {
                return nextTime; // Thời gian hợp lệ trong tương lai
            }
        }

        // Xử lý cho báo thức lặp lại:
        // Nếu thời gian đã qua trong ngày hôm nay, bắt đầu tính từ ngày mai
        if (nextTime.before(now)) {
            nextTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Tìm ngày hợp lệ tiếp theo dựa trên loopIndex
        if (alarm.loopIndex == 1) { // Hàng ngày
            return nextTime;
        } else if (alarm.loopIndex == 2) { // T2-T7
            while (nextTime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                nextTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            return nextTime;
        } else if (alarm.loopIndex == 3) { // Tùy chỉnh
            boolean[] customDays = getCustomDaysArray(alarm.optionOther);
            if (customDays == null) {
                Log.e(TAG, "calculateNextAlarmTime: Cannot get custom days for loopIndex 3.");
                return null; // Không thể tính nếu không có ngày tùy chỉnh
            }
            // Lặp tối đa 7 ngày để tránh vòng lặp vô hạn nếu không ngày nào được chọn
            for (int i = 0; i < 7; i++) {
                int dayOfWeek = nextTime.get(Calendar.DAY_OF_WEEK); // 1=CN, 2=T2, ... 7=T7
                int dayIndex = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - 2; // Ánh xạ sang 0=T2..6=CN

                if (dayIndex >= 0 && dayIndex < 7 && customDays[dayIndex]) {
                    return nextTime; // Tìm thấy ngày hợp lệ
                }
                // Nếu không hợp lệ, thử ngày tiếp theo
                nextTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            // Nếu sau 7 ngày vẫn không tìm thấy ngày hợp lệ (không ngày nào được check)
            Log.w(TAG, "calculateNextAlarmTime: No valid day found in customDays within the next 7 days.");
            return null; // Không có ngày nào hợp lệ để lên lịch
        }

        Log.e(TAG, "calculateNextAlarmTime: Unknown loopIndex: " + alarm.loopIndex);
        return null; // Loại lặp không xác định
    }


    // cancelAlarm - logic vẫn ổn (Giữ nguyên)
    public static void cancelAlarm(Context context, int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null, cannot cancel alarm index: " + alarmId);
            return;
        }
        // Tạo lại Intent tương tự như khi đặt lịch nhưng không cần extras
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("ALARM_TRIGGER_" + alarmId); // Đặt action nếu có dùng

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId, // Sử dụng cùng chỉ mục/request code
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE // FLAG_NO_CREATE để kiểm tra tồn tại
        );

        if (pendingIntent != null) {
            // Nếu tồn tại, hủy nó
            PendingIntent cancelIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if(cancelIntent != null){
                alarmManager.cancel(cancelIntent);
                cancelIntent.cancel(); // Hủy cả PendingIntent object
                Log.i(TAG, "Cancelled alarm with index (alarmId): " + alarmId);
            } else {
                Log.w(TAG, "Could not get PendingIntent with FLAG_CANCEL_CURRENT for index: " + alarmId);
            }

        } else {
            Log.d(TAG, "No pending alarm found to cancel for index (alarmId): " + alarmId); // Dùng Log.d vì đây là trường hợp bình thường
        }
    }

    // --- Hàm tiện ích getSoundResourceIdFromString (Giữ nguyên) ---
    public static int getSoundResourceIdFromString(Context context, String rawResourceString) {
        // ... (code như trước) ...
        if (rawResourceString == null || !rawResourceString.startsWith("@raw/")) return -1;
        String resourceName = rawResourceString.substring(5);
        if (resourceName.isEmpty()) return -1;
        try {
            int resId = context.getResources().getIdentifier(resourceName, "raw", context.getPackageName());
            return (resId == 0) ? -1 : resId; // Trả về -1 nếu không tìm thấy (resId=0)
        } catch (Exception e) {
            Log.e(TAG, "Error getSoundResourceIdFromString: " + resourceName, e);
            return -1;
        }
    }

    // --- Hàm tiện ích getCustomDaysArray (Giữ nguyên) ---
    private static boolean[] getCustomDaysArray(Pair<Integer, Boolean>[] optionOther) {
        // ... (code như trước) ...
        if (optionOther == null || optionOther.length != 7) return null;
        boolean[] customDays = new boolean[7];
        try {
            for (int i = 0; i < 7; i++) {
                customDays[i] = (optionOther[i] != null && optionOther[i].second != null) ? optionOther[i].second : false;
            }
            return customDays;
        } catch (Exception e) {
            Log.e(TAG, "Error getCustomDaysArray", e);
            return null;
        }
    }
}