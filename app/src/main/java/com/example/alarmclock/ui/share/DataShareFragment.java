package com.example.alarmclock.ui.share; // Hoặc package phù hợp

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclock.R;
import com.example.alarmclock.databinding.FragmentShareDataBinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class DataShareFragment extends Fragment {

    private FragmentShareDataBinding binding;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "Settings";
    private static final String ALARM_DATA_FILENAME = "alarm_data.json";

    private String selectedFormat = "csv"; // Mặc định là CSV
    private final String[] formats = {"CSV", "JSON"};
    private final String[] formatCodes = {"csv", "json"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShareDataBinding.inflate(inflater, container, false); // Đổi tên Binding
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupToolbar();
        setupFormatSelection();
        setupCopyButton();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Khởi tạo hiển thị format mặc định
        updateFormatDisplay();
    }

    private void setupToolbar() {
        binding.ivBackArrow.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.tvToolbarTitle.setText(R.string.share_data); // Cập nhật tiêu đề phù hợp
    }

    private void setupFormatSelection() {
        binding.layoutFormatSelection.setOnClickListener(v -> {
            int currentFormatIndex = 0;
            for (int i = 0; i < formatCodes.length; i++) {
                if (formatCodes[i].equals(selectedFormat)) {
                    currentFormatIndex = i;
                    break;
                }
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.format)
                    .setSingleChoiceItems(formats, currentFormatIndex, (dialog, which) -> {
                        selectedFormat = formatCodes[which];
                        updateFormatDisplay();
                        // Không cần lưu vào SharedPreferences nếu chỉ là lựa chọn tạm thời
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void updateFormatDisplay() {
        // Hiển thị tên đầy đủ (CSV hoặc JSON)
        for (int i = 0; i < formatCodes.length; i++) {
            if (formatCodes[i].equals(selectedFormat)) {
                binding.tvFormatValueDisplay.setText(formats[i]);
                break;
            }
        }
    }

    private void setupCopyButton() {
        binding.btnCopyToClipboard.setOnClickListener(v -> {
            copyDataToClipboard();
        });
    }

    // Hàm chuyển đổi JSON của Alarms sang CSV (CẦN ĐIỀU CHỈNH THEO CẤU TRÚC JSON CỦA BẠN)
    // Giả định alarm_data.json là một MẢNG CÁC ĐỐI TƯỢNG JSON, mỗi đối tượng có "id", "time", "label", "enabled"
    private String alarmJsonToCsvString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return getString(R.string.data_empty) + "\n";
        }
        StringBuilder csv = new StringBuilder();
        // Gson gson = new Gson(); // Có thể không cần Gson ở đây nếu parse thủ công

        try {
            JsonParser parser = new JsonParser();
            JsonElement parsedElement = parser.parse(jsonString); // Parse trước một lần

            if (!parsedElement.isJsonArray()) {
                Log.e("DataShareFragment", "Alarm data is not a valid JSON array.");
                return getString(R.string.format) + "\n";
            }
            JsonArray jsonArray = parsedElement.getAsJsonArray();

            if (jsonArray.size() > 0) {
                // === Headers mới dựa trên các trường của AlarmData ===
                // Chọn các trường bạn muốn đưa vào CSV
                csv.append("Index,Enabled,Time,MusicName,LoopIndex,IsCustomLoop,Knoll,DeleteAfterAlarm,Note\n");

                // === Duyệt qua từng báo thức ===
                for (int i = 0; i < jsonArray.size(); i++) {
                    if (!jsonArray.get(i).isJsonObject()) {
                        Log.w("DataShareFragment", "Skipping non-object element in alarms array at index " + i);
                        continue;
                    }
                    JsonObject alarmEntryObject = jsonArray.get(i).getAsJsonObject();

                    // Lấy dữ liệu chi tiết ("first") và trạng thái enabled ("second")
                    JsonObject alarmDetails = null;
                    if (alarmEntryObject.has("first") && alarmEntryObject.get("first").isJsonObject()) {
                        alarmDetails = alarmEntryObject.getAsJsonObject("first");
                    } else {
                        Log.w("DataShareFragment", "Missing or invalid 'first' object in alarm entry at index " + i);
                        // Có thể bỏ qua dòng này hoặc ghi lỗi vào CSV
                        csv.append(i).append(",ERROR_MissingDetails,,,,,,,,\n");
                        continue;
                    }

                    boolean isEnabled = false;
                    if (alarmEntryObject.has("second") && alarmEntryObject.get("second").isJsonPrimitive() && alarmEntryObject.get("second").getAsJsonPrimitive().isBoolean()) {
                        isEnabled = alarmEntryObject.get("second").getAsBoolean();
                    } else {
                        Log.w("DataShareFragment", "Missing or invalid 'second' boolean in alarm entry at index " + i);
                    }
                    String enabledStr = String.valueOf(isEnabled);


                    // --- Trích xuất các trường từ alarmDetails ---
                    String indexStr = String.valueOf(i);
                    String timeStr = getStringFromJson(alarmDetails, "timerString");
                    String noteStr = getStringFromJson(alarmDetails, "note"); // Lấy Note
                    int loopIndexInt = getIntFromJson(alarmDetails, "loopIndex", 0); // Lấy loopIndex, mặc định 0
                    boolean knollBool = getBooleanFromJson(alarmDetails, "knoll", false); // Lấy knoll, mặc định false
                    boolean deleteAfterAlarmBool = getBooleanFromJson(alarmDetails, "deleteAfterAlarm", false); // Lấy deleteAfterAlarm, mặc định false

                    // Lấy tên nhạc
                    String musicName = "";
                    if (alarmDetails.has("indexMusic") && alarmDetails.has("items") && alarmDetails.get("items").isJsonArray()) {
                        try {
                            int musicIndex = alarmDetails.get("indexMusic").getAsInt();
                            JsonArray itemsArray = alarmDetails.getAsJsonArray("items");
                            if (musicIndex >= 0 && musicIndex < itemsArray.size() && itemsArray.get(musicIndex).isJsonObject()) {
                                JsonObject musicItem = itemsArray.get(musicIndex).getAsJsonObject();
                                // Giả định tên nhạc nằm trong key "first" của Pair trong mảng items
                                musicName = getStringFromJson(musicItem, "first");
                            } else {
                                Log.w("DataShareFragment", "Music index out of bounds or item not object at index " + i);
                            }
                        } catch (Exception e) {
                            Log.e("DataShareFragment", "Error getting music name at index " + i, e);
                        }
                    }

                    // Xác định IsCustomLoop (giả sử index 3 của loopOption là custom)
                    // Thay đổi số 3 nếu index của "Custom" trong mảng loopOption của bạn khác đi
                    boolean isCustomLoop = (loopIndexInt == 3);

                    String loopIndexStr = String.valueOf(loopIndexInt);
                    String isCustomLoopStr = String.valueOf(isCustomLoop);
                    String knollStr = String.valueOf(knollBool);
                    String deleteAfterAlarmStr = String.valueOf(deleteAfterAlarmBool);


                    // === Thêm dòng dữ liệu vào CSV ===
                    csv.append(escapeCsvValue(indexStr)).append(",");
                    csv.append(escapeCsvValue(enabledStr)).append(","); // Đưa Enabled lên trước Time cho dễ nhìn
                    csv.append(escapeCsvValue(timeStr)).append(",");
                    csv.append(escapeCsvValue(musicName)).append(",");
                    csv.append(escapeCsvValue(loopIndexStr)).append(",");
                    csv.append(escapeCsvValue(isCustomLoopStr)).append(",");
                    csv.append(escapeCsvValue(knollStr)).append(",");
                    csv.append(escapeCsvValue(deleteAfterAlarmStr)).append(",");
                    csv.append(escapeCsvValue(noteStr)).append("\n");
                }
            } else {
                return getString(R.string.data_empty) + "\n";
            }
        } catch (JsonSyntaxException | IllegalStateException | ClassCastException | NullPointerException e) { // Bắt nhiều lỗi hơn
            Log.e("DataShareFragment", "Fatal error parsing/processing alarm JSON for CSV: " + e.getMessage(), e); // Log cả stacktrace
            return getString(R.string.alarm_data_conversion_error) + "\n"; // Thêm string mới
        }
        return csv.toString();
    }

    // --- Các hàm helper để lấy giá trị từ JsonObject an toàn hơn ---
    private String getStringFromJson(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isString())
                ? obj.get(key).getAsString() : "";
    }

    private int getIntFromJson(JsonObject obj, String key, int defaultValue) {
        return (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isNumber())
                ? obj.get(key).getAsInt() : defaultValue;
    }

    private boolean getBooleanFromJson(JsonObject obj, String key, boolean defaultValue) {
        return (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isBoolean())
                ? obj.get(key).getAsBoolean() : defaultValue;
    }


    // Hàm escapeCsvValue giữ nguyên như trước
    private String escapeCsvValue(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\""); // Escape double quotes
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\""; // Enclose in double quotes
        }
        return escaped;
    }

    // --- Hàm truy cập dữ liệu riêng biệt (giữ nguyên) ---
    private Map<String, ?> getSettingsDataMap() {
        return prefs.getAll();
    }
    private String getAlarmJsonDataString() {
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fis = requireContext().openFileInput(ALARM_DATA_FILENAME);
             InputStreamReader inputStreamReader = new InputStreamReader(fis);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line); // Không thêm newline để giữ nguyên JSON
            }
        } catch (FileNotFoundException e) {
            Log.w("DataShareFragment", "Alarm data file not found: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e("DataShareFragment", "Error reading alarm data file: " + e.getMessage());
            return null;
        }
        if (stringBuilder.length() == 0) {
            Log.w("DataShareFragment", "Alarm data file is empty.");
            return null;
        }
        return stringBuilder.toString();
    }


    // --- Hàm định dạng Settings (giữ nguyên) ---
    private String settingsMapToJsonString(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(map);
    }

    private String settingsMapToCsvString(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder csv = new StringBuilder();
        csv.append("SettingKey,SettingValue\n");
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            csv.append(escapeCsvValue(entry.getKey())).append(",");
            csv.append(escapeCsvValue(entry.getValue().toString())).append("\n");
        }
        return csv.toString();
    }


    // --- Cập nhật hàm copyDataToClipboard để xử lý lỗi Clipboard ---
    private void copyDataToClipboard() {
        String settingsData;
        String alarmData;
        String combinedOutput = "";
        String formatLabel = "";

        Map<String, ?> settingsMap = getSettingsDataMap();
        String alarmJson = getAlarmJsonDataString(); // Lấy chuỗi JSON gốc

        if ("csv".equals(selectedFormat)) {
            formatLabel = "CSV";
            settingsData = settingsMapToCsvString(settingsMap); // Settings -> CSV
            alarmData = alarmJsonToCsvString(alarmJson); // Alarm JSON -> CSV (dùng hàm MỚI)
            combinedOutput = "--- Settings (CSV) ---\n" + settingsData + "\n" +
                    "--- Alarms (CSV) ---\n" + alarmData;

        } else { // json format
            formatLabel = "JSON";
            settingsData = settingsMapToJsonString(settingsMap); // Settings -> JSON
            alarmData = (alarmJson == null || alarmJson.isEmpty()) ? "[]" : alarmJson; // Dùng mảng rỗng nếu null/trống

            JsonObject root = new JsonObject();
            JsonElement settingsElement = null;
            JsonElement alarmsElement = null;
            try {
                settingsElement = JsonParser.parseString(settingsData);
                alarmsElement = JsonParser.parseString(alarmData);
            } catch(JsonSyntaxException e) {
                Log.e("DataShareFragment", "Error parsing generated JSON strings before combining", e);
                Toast.makeText(requireContext(), "Error preparing final JSON data.", Toast.LENGTH_SHORT).show();
                return; // Không copy nếu có lỗi
            }


            root.add("settings", settingsElement);
            root.add("alarms", alarmsElement);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            combinedOutput = gson.toJson(root);
        }

        // --- Copy to clipboard với xử lý lỗi ---
        ClipboardManager clipboard = null;
        try {
            clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        } catch (Exception e) {
            Log.e("DataShareFragment", "Error getting ClipboardManager", e);
        }

        if (clipboard != null) {
            try {
                ClipData clip = ClipData.newPlainText("AppData", combinedOutput);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), getString(R.string.data_copied_as_format, formatLabel), Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                // Lỗi này thường xảy ra trên Android 10+ nếu app không ở nền trước khi cố truy cập clipboard
                Log.e("DataShareFragment", "Clipboard access denied (App not in foreground?): " + e.getMessage());
                Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_LONG).show(); // String mới
            } catch (Exception e) {
                Log.e("DataShareFragment", "Failed to copy to clipboard: " + e.getMessage(), e);
                Toast.makeText(requireContext(), R.string.clipboard_error, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("DataShareFragment", "ClipboardManager is null, cannot copy.");
            // Thông báo lỗi này nếu không lấy được service
            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show(); // String mới
        }
    }

    // ... hàm onDestroyView ...
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}