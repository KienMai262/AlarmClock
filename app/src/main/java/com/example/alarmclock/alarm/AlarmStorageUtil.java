package com.example.alarmclock.alarm;

import android.content.Context;
import android.util.Log;
import android.widget.Toast; // Chỉ dùng Toast để debug nếu cần, không nên dùng trong logic chính

import com.example.alarmclock.Pair;
import com.example.alarmclock.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AlarmStorageUtil {

    private static final String TAG = "AlarmStorageUtil";
    private static final String FILENAME = "alarm_data.json";

    // --- Hàm Lưu Danh Sách Báo Thức ---
    public static synchronized void saveAlarmDataToFile(Context context, List<Pair<AlarmData, Boolean>> listToSave) {
        if (context == null || listToSave == null) {
            Log.e(TAG, "Context or list is null, cannot save data.");
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(listToSave);
        try {
            // Sử dụng applicationContext để an toàn hơn khi gọi từ background
            FileOutputStream fos = context.getApplicationContext().openFileOutput(FILENAME, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(jsonString);
            writer.close();
            Log.i(TAG, "Alarm data saved successfully to " + FILENAME);
        } catch (Exception e) {
            Log.e(TAG, "Error saving alarm data to file", e);
            // Tránh hiển thị Toast từ lớp tiện ích, hãy để lớp gọi xử lý thông báo lỗi nếu cần
            // Toast.makeText(context, R.string.error_saving_data, Toast.LENGTH_SHORT).show();
        }
    }

    // --- Hàm Tải Danh Sách Báo Thức ---
    public static synchronized List<Pair<AlarmData, Boolean>> loadAlarmsFromFile(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot load data.");
            return new ArrayList<>(); // Trả về list rỗng
        }
        List<Pair<AlarmData, Boolean>> loadedList = new ArrayList<>();
        try {
            // Sử dụng applicationContext
            FileInputStream fis = context.getApplicationContext().openFileInput(FILENAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            bufferedReader.close();
            isr.close();
            fis.close();

            String jsonString = sb.toString();
            if (!jsonString.isEmpty()) {
                Gson gson = new Gson();
                // QUAN TRỌNG: Định nghĩa Type đúng cho List<Pair<AlarmData, Boolean>>
                Type listType = new TypeToken<ArrayList<Pair<AlarmData, Boolean>>>(){}.getType();
                loadedList = gson.fromJson(jsonString, listType);
                // Kiểm tra null sau khi parse (trường hợp file JSON trống hoặc lỗi)
                if (loadedList == null) {
                    loadedList = new ArrayList<>();
                }
                Log.i(TAG, "Alarm data loaded successfully from " + FILENAME + ". Count: " + loadedList.size());
            } else {
                Log.i(TAG, FILENAME + " is empty. Returning empty list.");
            }

        } catch (FileNotFoundException e) {
            Log.i(TAG, FILENAME + " not found. Returning empty list. This is normal on first run.");
            // Không cần báo lỗi, chỉ là chưa có file
        } catch (Exception e) {
            Log.e(TAG, "Error loading alarm data from file", e);
            // Toast.makeText(context, R.string.error_loading_data, Toast.LENGTH_SHORT).show();
            loadedList = new ArrayList<>(); // Trả về list rỗng nếu lỗi
        }
        return loadedList;
    }
}