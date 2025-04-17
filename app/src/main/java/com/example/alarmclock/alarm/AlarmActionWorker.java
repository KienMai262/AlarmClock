package com.example.alarmclock.alarm;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.alarmclock.Pair; // Đảm bảo import đúng

import java.util.List;

import javax.xml.transform.Result;

public class AlarmActionWorker extends Worker {

    private static final String TAG = "AlarmActionWorker";
    public static final String KEY_ALARM_ID = "alarmId";
    public static final String KEY_SHOULD_DELETE = "shouldDelete";

    public AlarmActionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Lấy dữ liệu đầu vào từ WorkManager
        Data inputData = getInputData();
        int alarmId = inputData.getInt(KEY_ALARM_ID, -1);
        boolean shouldDelete = inputData.getBoolean(KEY_SHOULD_DELETE, false);

        Log.i(TAG, "Worker started for alarmId: " + alarmId + ", shouldDelete: " + shouldDelete);

        if (alarmId == -1) {
            Log.e(TAG, "Invalid alarmId received. Failing work.");
            return Result.failure();
        }

        try {
            // Lấy application context
            Context appContext = getApplicationContext();

            // Tải danh sách báo thức hiện tại từ file
            List<Pair<AlarmData, Boolean>> currentAlarms = AlarmStorageUtil.loadAlarmsFromFile(appContext);

            // --- Thực hiện hành động (Xóa hoặc Tắt) ---
            boolean listModified = false;
            synchronized (this) { // Đồng bộ hóa nếu có thể có nhiều worker chạy (ít khả năng nhưng an toàn)
                if (alarmId >= 0 && alarmId < currentAlarms.size()) {
                    if (shouldDelete) {
                        Log.d(TAG, "Worker: Deleting alarm at index: " + alarmId);
                        currentAlarms.remove(alarmId);
                        listModified = true;
                        // Cảnh báo: Việc xóa làm thay đổi index. Nếu dùng index làm ID, cần xử lý cẩn thận.
                        // Nếu đã chuyển sang ID cố định thì tìm theo ID đó thay vì index.
                    } else {
                        // Chỉ tắt báo thức (đặt cờ enable thành false)
                        Log.d(TAG, "Worker: Disabling alarm at index: " + alarmId);
                        Pair<AlarmData, Boolean> pair = currentAlarms.get(alarmId);
                        if (pair != null && pair.second != null && pair.second) { // Chỉ tắt nếu đang bật
                            Pair<AlarmData, Boolean> updatedPair = new Pair<>(pair.first, false);
                            currentAlarms.set(alarmId, updatedPair);
                            listModified = true;
                        } else {
                            Log.d(TAG,"Alarm "+ alarmId + " already disabled or pair invalid.");
                        }
                    }
                } else {
                    Log.e(TAG, "Worker: Invalid alarmId (" + alarmId + ") for current list size (" + currentAlarms.size() + ").");
                    // Có thể trả về failure nếu ID không hợp lệ
                    // return Result.failure();
                    // Hoặc coi như thành công vì không có gì để làm
                    return Result.success();
                }
            } // end synchronized

            // Nếu danh sách đã bị sửa đổi, lưu lại vào file
            if (listModified) {
                AlarmStorageUtil.saveAlarmDataToFile(appContext, currentAlarms);
                Log.i(TAG, "Worker: Successfully modified and saved alarm list for alarmId: " + alarmId);
            }

            return Result.success(); // Báo cáo công việc thành công

        } catch (Exception e) {
            Log.e(TAG, "Worker failed for alarmId: " + alarmId, e);
            return Result.failure(); // Báo cáo công việc thất bại
        }
    }
}