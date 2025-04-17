package com.example.alarmclock.ui.create;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Thêm import này
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclock.alarm.AlarmData;
import com.example.alarmclock.Pair;
import com.example.alarmclock.R;
// import com.example.alarmclock.alarm.AlarmStateManager; // Không cần thiết cho logic CREATE/EDIT nữa
import com.example.alarmclock.alarm.AlarmSchedulerUtil;
import com.example.alarmclock.databinding.FragmentCreateBinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@TargetApi(26)
public class CreateFragment extends Fragment {

    private FragmentCreateBinding binding;

    public static List<Pair<AlarmData, Boolean>> alarmDataList = new ArrayList<>();


    protected AlarmData currentEditingAlarmData = new AlarmData("07:30:00", 0, false, false, null, 0);

    // Biến thành viên để lưu index nhận được từ argument
    private int currentAlarmIndex = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Nhận argument ngay trong onCreate
        if (getArguments() != null) {
            // Lấy index từ argument, key là "alarmIndexToEdit" như trong nav_graph.xml
            currentAlarmIndex = getArguments().getInt("alarmIndexToEdit", -1);
            Log.i("CreateFragment", "Received arguments. alarmIndexToEdit = " + currentAlarmIndex);
        } else {
            Log.i("CreateFragment", "No arguments received. Assuming CREATE mode.");
            currentAlarmIndex = -1;
        }

        // Nếu là EDIT mode, tải dữ liệu vào currentEditingAlarmData sớm
        // (Việc này an toàn hơn là làm trong onCreateView vì data sẵn sàng trước khi UI được tạo)
        if (currentAlarmIndex != -1) {
            // Kiểm tra list và index trước khi truy cập
            if (alarmDataList != null && currentAlarmIndex >= 0 && currentAlarmIndex < alarmDataList.size()) {
                // Lấy dữ liệu gốc
                AlarmData dataToEdit = alarmDataList.get(currentAlarmIndex).first;
                // Sao chép dữ liệu vào currentEditingAlarmData để tránh sửa đổi trực tiếp item gốc trong list
                try {
                    this.currentEditingAlarmData = new AlarmData(
                            dataToEdit.timerString, dataToEdit.indexMusic, dataToEdit.knoll,
                            dataToEdit.deleteAfterAlarm, dataToEdit.note, dataToEdit.loopIndex
                    );
                    if (dataToEdit.optionOther != null) {
                        this.currentEditingAlarmData.optionOther = new Pair[dataToEdit.optionOther.length];
                        for(int i=0; i< dataToEdit.optionOther.length; i++){
                            if(dataToEdit.optionOther[i] != null){
                                this.currentEditingAlarmData.optionOther[i] = new Pair<>(dataToEdit.optionOther[i].first, dataToEdit.optionOther[i].second);
                            }
                        }
                    } else {
                        this.currentEditingAlarmData.optionOther = null;
                    }

                    if (dataToEdit.items != null) {
                        this.currentEditingAlarmData.items = new Pair[dataToEdit.items.length];
                        for(int i=0; i< dataToEdit.items.length; i++){
                            if(dataToEdit.items[i] != null){
                                this.currentEditingAlarmData.items[i] = new Pair<>(dataToEdit.items[i].first, dataToEdit.items[i].second);
                            }
                        }
                    } else {
                        this.currentEditingAlarmData.items = null;
                    }

                    Log.d("CreateFragment", "Successfully copied data for EDIT mode into currentEditingAlarmData.");
                } catch (Exception e) {
                    Log.e("CreateFragment", "Error copying AlarmData for edit in onCreate", e);
                    Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show();
                    currentAlarmIndex = -1;
                    this.currentEditingAlarmData = new AlarmData("07:30:00", 0, false, false, null, 0); // Reset về mặc định
                }
            } else {
                // Index không hợp lệ hoặc list null
                Log.e("CreateFragment", "Invalid index (" + currentAlarmIndex + ") or null list for EDIT mode in onCreate.");
                Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show(); // Thêm string resource
                currentAlarmIndex = -1; // Chuyển về trạng thái CREATE
                this.currentEditingAlarmData = new AlarmData("07:30:00", 0, false, false, null, 0); // Reset về mặc định
            }
        } else {
            Log.d("CreateFragment", "CREATE mode detected in onCreate. Initializing default alarm data.");
            this.currentEditingAlarmData = new AlarmData("07:30:00", 0, false, false, null, 0);
            // TODO: Có thể cần load danh sách nhạc/ngày mặc định vào currentEditingAlarmData.items/optionOther ở đây nếu cần
        }
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Context context = requireContext();

        Log.d("CreateFragment", "onCreateView called. Mode: " + (currentAlarmIndex == -1 ? "CREATE" : "EDIT"));

        if (currentAlarmIndex == -1) { // CREATE Mode
            binding.buttonDelete.setVisibility(View.GONE);
        } else { // EDIT Mode
            binding.buttonDelete.setVisibility(View.VISIBLE);
            binding.buttonDelete.setOnClickListener(v -> {
                Log.d("CreateFragment", "Delete button clicked (EDIT mode). Index: " + currentAlarmIndex);
                DeleteAlarmHere(currentAlarmIndex);
            });
        }

        // --- Điền dữ liệu từ currentEditingAlarmData vào UI ---
        // Việc này cần thực hiện cho cả CREATE (dữ liệu mặc định) và EDIT (dữ liệu đã load)
        populateUiFromData(context);

        // --- Thiết lập Listeners cho các nút và controls ---
        binding.btnSave.setOnClickListener(v -> {
            Log.d("CreateFragment", "Save button clicked. Mode: " + (currentAlarmIndex == -1 ? "CREATE" : "EDIT"));
            saveAlarm(context);
        });


        binding.btnCancel.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);
        });


        TextView btnRepeat = binding.btnRepeat;
        btnRepeat.setOnClickListener(v -> {
            if (currentEditingAlarmData.loopOption == null) {
                Log.w("CreateFragment", "loopOption is null, cannot show repeat dialog.");
                return;
            }

            String[] loopOptionNames = new String[currentEditingAlarmData.loopOption.length];
            for (int i = 0; i < currentEditingAlarmData.loopOption.length; i++) {
                if (currentEditingAlarmData.loopOption[i] != null && currentEditingAlarmData.loopOption[i].first != null) {
                    Object value = currentEditingAlarmData.loopOption[i].first;
                    if (value instanceof Integer) {
                        try {
                            loopOptionNames[i] = context.getString((Integer) value);
                        } catch (Resources.NotFoundException e) {
                            loopOptionNames[i] = "ID:" + value;
                        }
                    } else {
                        loopOptionNames[i] = value.toString();
                    }
                } else {
                    loopOptionNames[i] = "";
                }
            }

            new AlertDialog.Builder(context)
                    .setTitle(R.string.choose_a_loop)
                    // Dùng checkedItem là loopIndex hiện tại của dữ liệu đang sửa
                    .setSingleChoiceItems(loopOptionNames, currentEditingAlarmData.loopIndex, (dialog, which) -> {
                        if (which == 3) {
                            // Đóng dialog hiện tại trước khi mở dialog mới
                            dialog.dismiss();
                            showCustomDaysDialog(btnRepeat);
                        } else {
                            currentEditingAlarmData.loopIndex = which;
                            updateRepeatButtonText(context, btnRepeat, which);
                            dialog.dismiss();
                        }
                    })
                    .show();
        });

        TextView btnMusic = binding.btnMusic;
        btnMusic.setOnClickListener(v -> {
            // Sử dụng currentEditingAlarmData
            if (currentEditingAlarmData.items == null) {
                Log.w("CreateFragment", "Music items (currentEditingAlarmData.items) is null.");
                return;
            }

            final String[] displayNames = new String[currentEditingAlarmData.items.length];
            for (int i = 0; i < currentEditingAlarmData.items.length; i++) {
                displayNames[i] = (currentEditingAlarmData.items[i] != null && currentEditingAlarmData.items[i].first != null) ? currentEditingAlarmData.items[i].first.toString() : "";
            }

            new AlertDialog.Builder(context)
                    .setTitle(R.string.choose_a_music)
                    // Dùng checkedItem là indexMusic hiện tại
                    .setSingleChoiceItems(displayNames, currentEditingAlarmData.indexMusic, (dialog, which) -> {
                        currentEditingAlarmData.indexMusic = which;
                        updateMusicButtonText(context, btnMusic, which);
                        dialog.dismiss();
                    })
                    .show();
        });


        SwitchCompat btnKnoll = binding.knoll;
        btnKnoll.setOnCheckedChangeListener((buttonView, isChecked) -> currentEditingAlarmData.knoll = isChecked);

        SwitchCompat btnDeleteAfterAlarm = binding.deleteAfterAlarm;
        btnDeleteAfterAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> currentEditingAlarmData.deleteAfterAlarm = isChecked);

        TextView btnNote = binding.btnNote;
        btnNote.setOnClickListener(v -> {
            final EditText input = new EditText(context);
            // Sử dụng currentEditingAlarmData
            input.setText(currentEditingAlarmData.note != null ? currentEditingAlarmData.note : "");
            input.setHint(R.string.enter_the_note);

            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(R.string.enter_the_note)
                    .setView(input)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        String userInput = input.getText().toString().trim();
                        // Cập nhật dữ liệu đang sửa/tạo
                        currentEditingAlarmData.note = userInput;
                        updateNoteButtonText(context, btnNote, userInput); // Cập nhật text nút
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });


        return root;
    }

    // Hàm mới để điền dữ liệu vào UI từ currentEditingAlarmData
    private void populateUiFromData(Context context) {
        if (binding == null || currentEditingAlarmData == null) {
            Log.e("CreateFragment", "Cannot populate UI, binding or data is null.");
            return;
        }
        Log.d("CreateFragment", "Populating UI with data: " + currentEditingAlarmData.timerString);
        try {
            // Time Picker
            // Cần kiểm tra timerString null hoặc trống
            if (currentEditingAlarmData.timerString != null && !currentEditingAlarmData.timerString.isEmpty()) {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_LOCAL_TIME; // HH:mm:ss hoặc HH:mm
                try {
                    LocalTime time = LocalTime.parse(currentEditingAlarmData.timerString, inputFormatter);
                    binding.timePicker.setHour(time.getHour());
                    binding.timePicker.setMinute(time.getMinute());
                } catch (java.time.format.DateTimeParseException e) {
                    Log.e("CreateFragment", "Error parsing time string: " + currentEditingAlarmData.timerString, e);
                    // Đặt thời gian mặc định nếu lỗi
                    binding.timePicker.setHour(7);
                    binding.timePicker.setMinute(30);
                }
            } else {
                // Đặt thời gian mặc định nếu timerString null/trống
                binding.timePicker.setHour(7);
                binding.timePicker.setMinute(30);
            }


            // Switches
            binding.knoll.setChecked(currentEditingAlarmData.knoll);
            binding.deleteAfterAlarm.setChecked(currentEditingAlarmData.deleteAfterAlarm);

            // Text Buttons
            updateRepeatButtonText(context, binding.btnRepeat, currentEditingAlarmData.loopIndex);
            updateMusicButtonText(context, binding.btnMusic, currentEditingAlarmData.indexMusic);
            updateNoteButtonText(context, binding.btnNote, currentEditingAlarmData.note);

            Log.i("CreateFragment", "Successfully populated UI elements.");

        } catch (Exception e) {
            Log.e("CreateFragment", "Error populating UI", e);
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show(); // Thêm string
        }
    }


    private void saveAlarm(Context context) {
        // 1. Thu thập dữ liệu từ UI và cập nhật vào currentEditingAlarmData
        LocalTime timer = LocalTime.of(binding.timePicker.getHour(), binding.timePicker.getMinute());
        DateTimeFormatter storageFormatter = DateTimeFormatter.ISO_LOCAL_TIME; // Lưu dạng HH:mm:ss hoặc HH:mm
        currentEditingAlarmData.timerString = timer.format(storageFormatter);

        // 2. Thực hiện lưu hoặc cập nhật vào danh sách static
        if (currentAlarmIndex == -1) { // CREATE Mode
            if (alarmDataList == null) {
                alarmDataList = new ArrayList<>();
            }
            alarmDataList.add(new Pair<>(currentEditingAlarmData, true));
            AlarmSchedulerUtil.scheduleAlarm(getContext(), currentEditingAlarmData, alarmDataList.size() - 1);
            Log.i("CreateFragment", "New alarm added to list.");

        } else { // EDIT Mode
            // Kiểm tra index hợp lệ một lần nữa trước khi cập nhật
            if (alarmDataList != null && currentAlarmIndex >= 0 && currentAlarmIndex < alarmDataList.size()) {
                AlarmSchedulerUtil.cancelAlarm(getContext(), currentAlarmIndex);
                // Lấy Pair cũ để giữ nguyên trạng thái bật/tắt
                Pair<AlarmData, Boolean> oldPair = alarmDataList.get(currentAlarmIndex);
                boolean previousEnabledState = oldPair.second; // Lấy trạng thái cũ

                // Tạo Pair mới với dữ liệu đã cập nhật và trạng thái bật/tắt cũ
                Pair<AlarmData, Boolean> updatedPair = new Pair<>(currentEditingAlarmData, previousEnabledState);

                // Thay thế item cũ bằng item mới trong danh sách
                alarmDataList.set(currentAlarmIndex, updatedPair);
                AlarmSchedulerUtil.scheduleAlarm(getContext(), currentEditingAlarmData, currentAlarmIndex);
                Log.i("CreateFragment", "Alarm at index " + currentAlarmIndex + " updated.");
            } else {
                Log.e("CreateFragment", "Invalid index (" + currentAlarmIndex + ") for saving EDIT. Aborting save.");
                Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show(); // Thêm string
                return;
            }
        }

        // 3. Lưu toàn bộ danh sách vào file JSON
        saveAlarmDataToFile(context, alarmDataList);

        // 4. Hiển thị thông báo và quay về Home
        Toast.makeText(getContext(), R.string.save_successfully, Toast.LENGTH_SHORT).show();
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.navigation_home);
    }


    public void saveAlarmDataToFile(Context context, List<Pair<AlarmData, Boolean>> listToSave) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(listToSave);
        try {
            FileOutputStream fos = context.openFileOutput("alarm_data.json", Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(jsonString);
            writer.close();
            Log.i("CreateFragment", "Alarm data saved successfully to alarm_data.json");
        } catch (Exception e) {
            Log.e("CreateFragment", "Error saving alarm data to file", e);
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
        }
    }

    public void DeleteAlarmHere(int index) {
        Context context = getContext();
        if (context == null) return;

        AlarmSchedulerUtil.cancelAlarm(context, index);

        // --- Safety Check ---
        if (alarmDataList != null && index >= 0 && index < alarmDataList.size()) {
            Log.d("CreateFragment", "Removing alarm at index: " + index);
            alarmDataList.remove(index);
            // Lưu lại danh sách sau khi xóa
            saveAlarmDataToFile(context, alarmDataList);

            // Quay về HomeFragment
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);
            Toast.makeText(context, R.string.delete_successfully, Toast.LENGTH_SHORT).show(); // Thêm string resource
        } else {
            Log.e("CreateFragment", "Attempted to delete alarm with invalid index: " + index);
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show(); // Thêm string resource
        }
    }


    private void showCustomDaysDialog(TextView btnRepeat) {
        Context context = requireContext();
        // Sử dụng currentEditingAlarmData
        if (currentEditingAlarmData.optionOther == null) {
            Log.w("CreateFragment", "Custom days options (optionOther) is null.");
            return;
        }

        final int itemCount = currentEditingAlarmData.optionOther.length;
        String[] dayNames = new String[itemCount];
        boolean[] tempCheckedItems = new boolean[itemCount]; // Mảng tạm để lưu trạng thái check trong dialog

        // Lấy trạng thái hiện tại từ currentEditingAlarmData
        for (int i = 0; i < itemCount; i++) {
            if (currentEditingAlarmData.optionOther[i] == null) {
                dayNames[i] = "Error";
                tempCheckedItems[i] = false;
                continue;
            }
            Object dayValue = currentEditingAlarmData.optionOther[i].first;
            Boolean isChecked = currentEditingAlarmData.optionOther[i].second;

            if (dayValue instanceof Integer) {dayNames[i] = context.getString((Integer) dayValue); }
            else if (dayValue != null) { dayNames[i] = dayValue.toString(); }
            else { dayNames[i] = "Unknown"; }

            tempCheckedItems[i] = (isChecked != null) ? isChecked : false;
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.choose_custom_date)
                .setMultiChoiceItems(dayNames, tempCheckedItems, (dialog, which, isChecked) -> {
                    // Cập nhật trạng thái trong mảng tạm khi người dùng check/uncheck
                    if (which >= 0 && which < tempCheckedItems.length) {
                        tempCheckedItems[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    // --- Cập nhật currentEditingAlarmData.optionOther với giá trị từ tempCheckedItems ---
                    boolean customDaySelected = false;
                    for (int i = 0; i < itemCount; i++) {
                        if (currentEditingAlarmData.optionOther[i] != null) {
                            // Tạo Pair mới vì Pair có thể là immutable hoặc để đảm bảo thay đổi được ghi nhận
                            currentEditingAlarmData.optionOther[i] = new Pair<>(currentEditingAlarmData.optionOther[i].first, tempCheckedItems[i]);
                            if(tempCheckedItems[i]) customDaySelected = true;
                        }
                    }
                    // Chỉ đặt loopIndex là 3 nếu có ít nhất một ngày được chọn
                    if(customDaySelected) {
                        currentEditingAlarmData.loopIndex = 3;
                    } else {
                        currentEditingAlarmData.loopIndex = 0;
                        Log.w("CreateFragment", "No custom days selected, reverting loopIndex to 0.");
                    }
                    updateRepeatButtonText(context, btnRepeat, currentEditingAlarmData.loopIndex); // Cập nhật lại text nút Repeat
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateRepeatButtonText(Context context, TextView btnRepeat, int loopIndex) {
        // Sử dụng currentEditingAlarmData
        if (currentEditingAlarmData == null) return; // Thêm kiểm tra null

        if (loopIndex == 3) { // Custom
            boolean hasSelection = false;
            List<String> selectedDaysShort = new ArrayList<>();
            if (currentEditingAlarmData.optionOther != null) {
                for (int i = 0; i < currentEditingAlarmData.optionOther.length; i++) {
                    if (currentEditingAlarmData.optionOther[i] != null && currentEditingAlarmData.optionOther[i].second != null && currentEditingAlarmData.optionOther[i].second) {
                        hasSelection = true;
                        Object dayValue = currentEditingAlarmData.optionOther[i].first;
                        String dayName = "";
                        if (dayValue instanceof Integer) { try { dayName = context.getString((Integer) dayValue); } catch (Exception e) {} }
                        else if (dayValue != null) { dayName = dayValue.toString(); }

                        if (dayName.length() >= 2) selectedDaysShort.add(dayName.substring(0, 2));
                        else selectedDaysShort.add(dayName);
                    }
                }
            }

            if (hasSelection) {
                String customText = String.join(", ", selectedDaysShort);
                btnRepeat.setText(context.getString(R.string.repeat_days_format, customText));
            } else {
                // Hiển thị text "Custom" hoặc "Select days" nếu không ngày nào được chọn
                if (currentEditingAlarmData.loopOption != null && currentEditingAlarmData.loopOption.length > 3 && currentEditingAlarmData.loopOption[3] != null && currentEditingAlarmData.loopOption[3].first != null) {
                    Object value = currentEditingAlarmData.loopOption[3].first;
                    String customOptionName = (value instanceof Integer) ? context.getString((Integer)value) : value.toString();
                    btnRepeat.setText(context.getString(R.string.repeat_option_format, customOptionName)); // "Lặp lại: Tuỳ chỉnh"
                } else {
                    btnRepeat.setText(R.string.select_days_prompt);
                }
            }
        } else if (currentEditingAlarmData.loopOption != null && loopIndex >= 0 && loopIndex < currentEditingAlarmData.loopOption.length && currentEditingAlarmData.loopOption[loopIndex] != null && currentEditingAlarmData.loopOption[loopIndex].first != null) {
            Object value = currentEditingAlarmData.loopOption[loopIndex].first;
            String optionName = "";
            if (value instanceof Integer) { try { optionName = context.getString((Integer) value); } catch (Exception e) { optionName = "ID:" + value; } }
            else { optionName = value.toString(); }
            btnRepeat.setText(context.getString(R.string.repeat_option_format, optionName));
        } else {
            btnRepeat.setText(R.string.select_days_prompt);
        }
    }

    private void updateMusicButtonText(Context context, TextView btnMusic, int musicIndex) {
        // Sử dụng currentEditingAlarmData
        if (currentEditingAlarmData == null || currentEditingAlarmData.items == null || musicIndex < 0 || musicIndex >= currentEditingAlarmData.items.length || currentEditingAlarmData.items[musicIndex] == null || currentEditingAlarmData.items[musicIndex].first == null) {
            btnMusic.setText(context.getString(R.string.music_display_format, context.getString(R.string.note_default))); // Hiển thị tên mặc định
            return;
        }
        // Logic hiển thị tên nhạc
        String selectedDisplayName = currentEditingAlarmData.items[musicIndex].first.toString();
        final int MAX_DISPLAY_LENGTH = 15;
        String displayText = selectedDisplayName;
        if (selectedDisplayName.length() > MAX_DISPLAY_LENGTH) {
            displayText = selectedDisplayName.substring(0, MAX_DISPLAY_LENGTH) + "...";
        }
        btnMusic.setText(context.getString(R.string.music_display_format, displayText));
    }

    private void updateNoteButtonText(Context context, TextView btnNote, String note) {
        // Sử dụng currentEditingAlarmData.note
        // Logic hiển thị note
        if (note != null && !note.isEmpty()) {
            final int MAX_DISPLAY_LENGTH = 15;
            String displayText = note;
            if (note.length() > MAX_DISPLAY_LENGTH) {
                displayText = note.substring(0, MAX_DISPLAY_LENGTH) + "...";
            }
            btnNote.setText(context.getString(R.string.note_display_format, displayText));
        } else {
            btnNote.setText(R.string.note_display_default_with_arrow);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d("CreateFragment", "onDestroyView called, binding set to null.");
    }
}