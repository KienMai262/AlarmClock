package com.example.alarmclock.ui.create;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
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

        Button btnSave = binding.btnSave;

        btnSave.setOnClickListener(v -> {
            LocalTime timer = LocalTime.of(binding.timePicker.getHour(), binding.timePicker.getMinute());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
            String timerString = timer.format(formatter);

            AlarmData newAlarm = new AlarmData("7:30:PM" , 0, false, false, null, -1);
            newAlarm.timerString = timerString;
            newAlarm.indexMusic = alarmData.indexMusic;
            newAlarm.knoll = alarmData.knoll;
            newAlarm.deleteAfterAlarm = alarmData.deleteAfterAlarm;
            newAlarm.note = alarmData.note;
            newAlarm.loopIndex = alarmData.loopIndex;
            newAlarm.optionOther = alarmData.optionOther;
            newAlarm.items = alarmData.items;

            alarmDataList.add(newAlarm);
            saveAlarmData(getContext());

            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);

            Toast.makeText(getContext(), "Lưu thành công", Toast.LENGTH_SHORT).show();
        });


        Button btnCancel = binding.btnCancel;
        btnCancel.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);
        });


        // Lấy TextView từ ViewBinding
        TextView btnRepeat = binding.btnRepeat;

        btnRepeat.setOnClickListener(v -> {
            String[] displayNames = new String[alarmData.loopOption.length];
            for (int i = 0; i < alarmData.loopOption.length; i++) {
                displayNames[i] = alarmData.loopOption[i].first;
            }
            new AlertDialog.Builder(requireContext()) // Dùng requireContext() trong Fragment an toàn hơn
                    .setTitle("Chọn kiểu lặp lại")
                    .setItems(displayNames, (dialog, which) -> {
                        if (which == 3) {
                            showCustomDaysDialog(btnRepeat);
                        } else {
                            alarmData.loopIndex = which;
                            String selectedText = alarmData.loopOption[which].first;
                            btnRepeat.setText(selectedText + " >");
                        }
                    })
                    .show();
        });

        TextView btnMusic = binding.btnMusic;
        btnMusic.setOnClickListener(v -> {

            final String[] displayNames = new String[alarmData.items.length];
            for (int i = 0; i < alarmData.items.length; i++) {
                displayNames[i] = alarmData.items[i].first;
            }

            new AlertDialog.Builder(getContext())
                    .setTitle("Chọn nhạc chuông")
                    .setItems(displayNames, (dialog, which) -> {
                        alarmData.indexMusic = which;
                        String selectedDisplayName = alarmData.items[which].first;
                        final int MAX_DISPLAY_LENGTH = 15;
                        String displayText;
                        if(selectedDisplayName.length() > MAX_DISPLAY_LENGTH) {
                            displayText = selectedDisplayName.substring(0, MAX_DISPLAY_LENGTH) + "...";
                        }
                        else {
                            displayText = selectedDisplayName;
                        }
                        btnMusic.setText(displayText + " >");

                    })
                    .show();
        });


        SwitchCompat btnKnoll = binding.knoll;
        btnKnoll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarmData.knoll = isChecked;
            }
        });

        SwitchCompat btnDeleteAfterAlarm = binding.deleteAfterAlarm;
        btnDeleteAfterAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarmData.deleteAfterAlarm = isChecked;
            }
        });
        TextView btnNote = binding.btnNote;
        btnNote.setOnClickListener(v -> {
            // Tạo EditText để nhập
            final EditText input;
            if(alarmData.note != null) {
                input = new EditText(getContext());
                input.setText(alarmData.note);
            } else {
                input = new EditText(getContext());
            }
            input.setHint("Nhập nhãn");

            // Tạo AlertDialog có EditText
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Tự nhập nhãn")
                    .setView(input)
                    .setPositiveButton("OK", (dialog, which) -> {
                        String userInput = input.getText().toString().trim();
                        alarmData.note = userInput;
                        if (!userInput.isEmpty()) {
                            final int MAX_DISPLAY_LENGTH = 15;
                            String displayText;

                            if (userInput.length() > MAX_DISPLAY_LENGTH) {
                                displayText = userInput.substring(0, MAX_DISPLAY_LENGTH) + "...";
                            } else {
                                displayText = userInput;
                            }

                            btnNote.setText(displayText + " >");

                        } else {
                            btnNote.setText("Mặc định >");
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        return root;
    }
    private void showCustomDaysDialog(TextView btnRepeat) {
        String[] dayNames = new String[alarmData.optionOther.length];
        boolean[] checkedItems = new boolean[alarmData.optionOther.length];

        for (int i = 0; i < alarmData.optionOther.length; i++) {
            dayNames[i] = alarmData.optionOther[i].first;
            checkedItems[i] = alarmData.optionOther[i].second;
        }

        boolean[] tempCheckedItems = checkedItems.clone();
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn ngày tùy chỉnh")
                .setMultiChoiceItems(dayNames, tempCheckedItems, (dialog, which, isChecked) -> {
                    tempCheckedItems[which] = isChecked;
                })
                .setPositiveButton("Đồng ý", (dialog, id) -> {
                    boolean hasSelection = false;
                    List<String> selectedDaysShort = new ArrayList<>();

                    for (int i = 0; i < alarmData.optionOther.length; i++) {
                        alarmData.optionOther[i] = new Pair<>(alarmData.optionOther[i].first, tempCheckedItems[i]);
                        if (tempCheckedItems[i]) {
                            hasSelection = true;
                            selectedDaysShort.add(alarmData.optionOther[i].first.substring(0, 2));
                        }
                    }

                    alarmData.loopIndex = 3;

                    if (hasSelection) {
                        String customText = String.join(", ", selectedDaysShort); // Nối các ngày lại, ví dụ: "T2, T4, CN"
                        btnRepeat.setText(customText + " >");
                    } else {
                        btnRepeat.setText(alarmData.loopOption[3].first + " >");
                    }

                })
                .setNegativeButton("Hủy", (dialog, id) -> {
                })
                .show();
    }

    public void saveAlarmData(Context context) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(alarmDataList);
        try {
            FileOutputStream fos = context.openFileOutput("alarm_data.json", Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(jsonString);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
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