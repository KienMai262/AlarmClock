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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclock.AlarmData;
import com.example.alarmclock.Pair;
import com.example.alarmclock.R;
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

    static List<AlarmData> alarmDataList = new ArrayList<>();
    protected AlarmData alarmData = new AlarmData("7:30:PM" , 0, false, false, null, -1);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Context context = requireContext();

        Button btnSave = binding.btnSave;

        btnSave.setOnClickListener(v -> {
            LocalTime timer = LocalTime.of(binding.timePicker.getHour(), binding.timePicker.getMinute());
            DateTimeFormatter storageFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
//            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("hh:mm a");

            String timerStringToStore = timer.format(storageFormatter);

            AlarmData newAlarm = new AlarmData("7:30:PM" , 0, false, false, null, -1);
            newAlarm.timerString = timerStringToStore;
            newAlarm.indexMusic = alarmData.indexMusic;
            newAlarm.knoll = alarmData.knoll;
            newAlarm.deleteAfterAlarm = alarmData.deleteAfterAlarm;
            newAlarm.note = alarmData.note;
            newAlarm.loopIndex = alarmData.loopIndex;
            newAlarm.optionOther = alarmData.optionOther;
            newAlarm.items = alarmData.items;

            CreateFragment.alarmDataList.add(newAlarm);
            saveAlarmData(context, CreateFragment.alarmDataList);

            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);

            Toast.makeText(getContext(), R.string.save_successfully, Toast.LENGTH_SHORT).show();
        });


        Button btnCancel = binding.btnCancel;
        btnCancel.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);
        });


        TextView btnRepeat = binding.btnRepeat;
        updateRepeatButtonText(context, btnRepeat, alarmData.loopIndex);
        btnRepeat.setOnClickListener(v -> {
            if (alarmData.loopOption == null) return;

            String[] loopOptionNames = new String[alarmData.loopOption.length];
            for (int i = 0; i < alarmData.loopOption.length; i++) {
                if (alarmData.loopOption[i] != null && alarmData.loopOption[i].first != null) {
                    Object value = alarmData.loopOption[i].first;
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
                    .setItems(loopOptionNames, (dialog, which) -> {
                        if (which == 3) { // Index của "Tuỳ chỉnh"
                            showCustomDaysDialog(btnRepeat);
                        } else {
                            alarmData.loopIndex = which;
                            updateRepeatButtonText(context, btnRepeat, which); // Cập nhật text nút
                        }
                    })
                    .show();
        });

        TextView btnMusic = binding.btnMusic;
        updateMusicButtonText(context, btnMusic, alarmData.indexMusic);

        btnMusic.setOnClickListener(v -> {
            if (alarmData.items == null) return;

            final String[] displayNames = new String[alarmData.items.length];
            for (int i = 0; i < alarmData.items.length; i++) {
                displayNames[i] = (alarmData.items[i] != null && alarmData.items[i].first != null) ? alarmData.items[i].first.toString() : "";
            }

            new AlertDialog.Builder(context)
                    .setTitle(R.string.choose_a_music)
                    .setItems(displayNames, (dialog, which) -> {
                        alarmData.indexMusic = which;
                        updateMusicButtonText(context, btnMusic, which);
                    })
                    .show();
        });


        SwitchCompat btnKnoll = binding.knoll;
        btnKnoll.setChecked(alarmData.knoll);
        btnKnoll.setOnCheckedChangeListener((buttonView, isChecked) -> alarmData.knoll = isChecked);

        SwitchCompat btnDeleteAfterAlarm = binding.deleteAfterAlarm;
        btnDeleteAfterAlarm.setChecked(alarmData.deleteAfterAlarm);
        btnDeleteAfterAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> alarmData.deleteAfterAlarm = isChecked);

        TextView btnNote = binding.btnNote;
        updateNoteButtonText(context, btnNote, alarmData.note);

        btnNote.setOnClickListener(v -> {
            final EditText input = new EditText(context);
            input.setText(alarmData.note != null ? alarmData.note : "");
            input.setHint(R.string.enter_the_note);

            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(R.string.enter_the_note)
                    .setView(input)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        String userInput = input.getText().toString().trim();
                        alarmData.note = userInput;
                        updateNoteButtonText(context, btnNote, userInput); // Cập nhật text nút
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
        return root;
    }
    private void showCustomDaysDialog(TextView btnRepeat) {
        Context context = requireContext();
        if (alarmData.optionOther == null) return;

        final int itemCount = alarmData.optionOther.length;

        String[] dayNames = new String[itemCount];
        boolean[] tempCheckedItems = new boolean[itemCount];

        for (int i = 0; i < itemCount; i++) {
            if (alarmData.optionOther[i] == null) {
                dayNames[i] = "Error";
                tempCheckedItems[i] = false;
                continue;
            }

            Object dayValue = alarmData.optionOther[i].first;
            Boolean isChecked = alarmData.optionOther[i].second;

            if (dayValue instanceof Integer) {
                try {
                    dayNames[i] = context.getString((Integer) dayValue);
                } catch (Resources.NotFoundException e) {
                    dayNames[i] = "ID:" + dayValue; // Hiển thị ID nếu không tìm thấy string
                }
            } else if (dayValue != null) {
                dayNames[i] = dayValue.toString(); // Nếu không phải Integer, thử toString
            } else {
                dayNames[i] = "Unknown Day";
            }

            tempCheckedItems[i] = (isChecked != null) ? isChecked : false;
        }


        new AlertDialog.Builder(context)
                .setTitle(R.string.choose_custom_date)
                .setMultiChoiceItems(dayNames, tempCheckedItems, (dialog, which, isChecked) -> {
                    if (which >= 0 && which < tempCheckedItems.length) {
                        tempCheckedItems[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    for (int i = 0; i < itemCount; i++) {
                        if (alarmData.optionOther[i] != null) {
                            // Tạo Pair mới vì Pair có thể là immutable
                            alarmData.optionOther[i] = new Pair<>(alarmData.optionOther[i].first, tempCheckedItems[i]);
                        }
                    }
                    alarmData.loopIndex = 3;
                    updateRepeatButtonText(context, btnRepeat, alarmData.loopIndex);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                })
                .show();
    }

    private void updateRepeatButtonText(Context context, TextView btnRepeat, int loopIndex) {
        if (alarmData.loopOption == null || loopIndex < 0 ) return;

        if (loopIndex == 3) {
            boolean hasSelection = false;
            List<String> selectedDaysShort = new ArrayList<>();
            if (alarmData.optionOther != null) {
                for (int i = 0; i < alarmData.optionOther.length; i++) {
                    if (alarmData.optionOther[i] != null && alarmData.optionOther[i].second != null && alarmData.optionOther[i].second) {
                        hasSelection = true;
                        Object dayValue = alarmData.optionOther[i].first;
                        String dayName = "";
                        if (dayValue instanceof Integer) {
                            try {
                                dayName = context.getString((Integer) dayValue);
                            } catch (Resources.NotFoundException e) { /* Xử lý lỗi */ }
                        } else if (dayValue instanceof String) {
                            dayName = (String) dayValue;
                        }

                        if (dayName.length() >= 2) {
                            selectedDaysShort.add(dayName.substring(0, 2));
                        } else {
                            selectedDaysShort.add(dayName);
                        }
                    }
                }
            }

            if (hasSelection) {
                String customText = String.join(", ", selectedDaysShort);
                btnRepeat.setText(context.getString(R.string.repeat_days_format, customText));
            } else {
                if (alarmData.loopOption.length > 3 && alarmData.loopOption[3] != null && alarmData.loopOption[3].first != null) {
                    btnRepeat.setText(context.getString(R.string.repeat_option_format, alarmData.loopOption[3].first.toString()));
                } else {
                    btnRepeat.setText(R.string.select_days_prompt); // Hoặc giá trị mặc định an toàn
                }
            }
        } else if (loopIndex < alarmData.loopOption.length && alarmData.loopOption[loopIndex] != null && alarmData.loopOption[loopIndex].first != null) {
            Object value = alarmData.loopOption[loopIndex].first;
            String optionName = "";

            if (value instanceof Integer) {
                try {
                    optionName = context.getString((Integer) value);
                }
                catch (Resources.NotFoundException e) {
                    optionName = "ID:" + value;
                }
            } else {
                optionName = value.toString();
            }
            String textToSet = context.getString(R.string.repeat_option_format, optionName);
            btnRepeat.setText(textToSet);
        } else {
            btnRepeat.setText(R.string.select_days_prompt);
        }
    }

    private void updateMusicButtonText(Context context, TextView btnMusic, int musicIndex) {
        if (alarmData.items == null || musicIndex < 0 || musicIndex >= alarmData.items.length || alarmData.items[musicIndex] == null || alarmData.items[musicIndex].first == null) {
            return;
        }
        String selectedDisplayName = alarmData.items[musicIndex].first.toString();
        final int MAX_DISPLAY_LENGTH = 15;
        String displayText = selectedDisplayName;
        if (selectedDisplayName.length() > MAX_DISPLAY_LENGTH) {
            displayText = selectedDisplayName.substring(0, MAX_DISPLAY_LENGTH) + "...";
        }
        btnMusic.setText(context.getString(R.string.music_display_format, displayText));
    }

    private void updateNoteButtonText(Context context, TextView btnNote, String note) {
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

    public void saveAlarmData(Context context, List<AlarmData> listToSave) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(listToSave);
        try {
            FileOutputStream fos = context.openFileOutput("alarm_data.json", Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(jsonString);
            writer.close();
            Log.i("CreateFragment", "Alarm data saved successfully.");
        } catch (Exception e) {
            Log.e("CreateFragment", "Error saving alarm data", e);
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

//Read data
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//
//String timeString = alarmData.timerString;
//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
//LocalTime timer = LocalTime.parse(timeString, formatter);
// String timerStringToDisplay = timer.format(displayFormatter);// Dùng khi cần hiển thị
//Object value = alarmData.loopOption[loopIndex].first; // Lấy Integer ID
//optionName = context.getString((Integer) value); // Lấy String từ ID ("One time")