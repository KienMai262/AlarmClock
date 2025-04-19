    package com.example.alarmclock.ui.home;

    import android.annotation.TargetApi;
    import android.content.Context;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.ImageButton;
    import android.widget.LinearLayout;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.appcompat.widget.SwitchCompat;
    import androidx.fragment.app.Fragment;
    import androidx.navigation.NavController;
    import androidx.navigation.fragment.NavHostFragment;

    import com.example.alarmclock.alarm.AlarmData;
    import com.example.alarmclock.Pair;
    import com.example.alarmclock.R;
    import com.example.alarmclock.alarm.AlarmSchedulerUtil;
    import com.example.alarmclock.items.PrefabItems;
    import com.example.alarmclock.ui.create.CreateFragment;
    import com.google.gson.Gson;
    import com.google.gson.reflect.TypeToken;

    import java.io.FileInputStream;
    import java.io.InputStreamReader;
    import java.lang.reflect.Type;
    import java.time.LocalTime;
    import java.time.format.DateTimeFormatter;
    import java.util.ArrayList;
    import java.util.List;

    @TargetApi(26)
    public class HomeFragment extends Fragment {

        private LinearLayout layoutAlarmList;
    //    private TextView textViewNoAlarms;

        // --- Thêm hàm load dữ liệu ---
        private void loadAlarmData(Context context) {
            try {
                FileInputStream fis = context.openFileInput("alarm_data.json");
                InputStreamReader reader = new InputStreamReader(fis);
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<Pair<AlarmData, Boolean>>>(){}.getType();
                CreateFragment.alarmDataList = gson.fromJson(reader, listType);
                reader.close();
                if (CreateFragment.alarmDataList == null) {
                    CreateFragment.alarmDataList = new ArrayList<>(); // Khởi tạo nếu file rỗng hoặc lỗi parse
                    Log.w("HomeFragment", "Loaded data but list was null, initialized new list.");
                } else {
                    Log.i("HomeFragment", "Alarm data loaded successfully. Count: " + CreateFragment.alarmDataList.size());
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error loading alarm data, initializing new list.", e);
                CreateFragment.alarmDataList = new ArrayList<>(); // Khởi tạo list mới nếu có lỗi đọc file
            }
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load dữ liệu khi fragment được tạo
            if (getContext() != null) {
                loadAlarmData(requireContext());
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.fragment_home, container, false);
            layoutAlarmList = view.findViewById(R.id.layoutAlarmList);
    //        textViewNoAlarms = view.findViewById(R.id.textViewNoAlarms); // Ánh xạ TextView thông báo trống

            // Luôn xóa các view cũ trước khi thêm mới để tránh trùng lặp khi quay lại fragment
            layoutAlarmList.removeAllViews();

            // Lấy danh sách báo thức (đã được load trong onCreate)
            List<Pair<AlarmData, Boolean>> alarmList = CreateFragment.alarmDataList;

            if (alarmList != null && !alarmList.isEmpty()) {
    //            textViewNoAlarms.setVisibility(View.GONE); // Ẩn thông báo trống nếu có báo thức
                layoutAlarmList.setVisibility(View.VISIBLE); // Hiện layout list

                for (int i = 0; i < alarmList.size(); i++) {
                    final Pair<AlarmData, Boolean> alarmPair = alarmList.get(i);
                    // Kiểm tra null phòng trường hợp dữ liệu trong list bị lỗi
                    if (alarmPair == null || alarmPair.first == null) {
                        Log.e("HomeFragment", "Skipping null alarm data at index: " + i);
                        continue;
                    }
                    final AlarmData currentAlarmData = alarmPair.first;
                    final int alarmIndex = i; // Lưu index hiện tại

                    // Tạo item view sử dụng PrefabItems
                    PrefabItems item = new PrefabItems(inflater, layoutAlarmList, currentAlarmData);

                    // --- Cập nhật UI cho item ---
                    try {
                        // Format thời gian (Đảm bảo timerString không null)
                        if (currentAlarmData.timerString != null && !currentAlarmData.timerString.isEmpty()) {
                            DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_LOCAL_TIME; // HH:mm:ss hoặc HH:mm
                            LocalTime time = LocalTime.parse(currentAlarmData.timerString, inputFormatter);
                            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("hh:mm a"); // "hh:mm AM/PM"
                            item.binding.textViewAlarmTime.setText(time.format(displayFormatter));
                        } else {
                            item.binding.textViewAlarmTime.setText(R.string.error); // Hiển thị lỗi nếu time string không hợp lệ
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error formatting time for alarm at index " + i, e);
                        item.binding.textViewAlarmTime.setText(R.string.error); // Hiển thị lỗi
                    }

                    // Hiển thị tùy chọn lặp lại
                    String optionName = getRepeatOptionName(requireContext(), currentAlarmData);
                    item.binding.textViewAlarmRepeat.setText(optionName);

                    String note = "";
                    if (currentAlarmData.note != null && !currentAlarmData.note.isEmpty()) {
                        if (currentAlarmData.note.length() > 15) {
                            note = currentAlarmData.note.substring(0, 15) + "...";
                        } else {
                            note = currentAlarmData.note;
                        }
                        item.binding.textNote.setText(note);
                        item.binding.textNote.setVisibility(View.VISIBLE);
                    } else {
                        item.binding.textNote.setVisibility(View.GONE); // Ẩn nếu không có note
                    }

                    // Thiết lập trạng thái ban đầu cho Switch (Đảm bảo alarmPair.second không null)
                    item.binding.switchAlarmEnabled.setChecked(alarmPair.second != null && alarmPair.second);

                    // Thêm item view vào layout chính
                    layoutAlarmList.addView(item.getView());

                    // --- Xử lý sự kiện ---

                    // Sự kiện thay đổi trạng thái Bật/Tắt
                    SwitchCompat switchAlarmEnabled = item.binding.switchAlarmEnabled;
                    switchAlarmEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        Log.d("HomeFragment", "Switch changed for index " + alarmIndex + " to " + isChecked);
                        // Cập nhật trạng thái trong danh sách gốc
                        alarmPair.second = isChecked;
                        if(isChecked){
                            AlarmSchedulerUtil.scheduleAlarm(getContext(), currentAlarmData, alarmIndex);
                        }
                        else{
                            AlarmSchedulerUtil.cancelAlarm(getContext(), alarmIndex);
                        }
                        // --- Lưu trạng thái mới ngay lập tức ---
                        if (getContext() != null) {
                            CreateFragment dummyCreateFragment = new CreateFragment(); // Tạo instance tạm
                            dummyCreateFragment.saveAlarmDataToFile(requireContext(), CreateFragment.alarmDataList);
                            // TODO: Cập nhật AlarmManager tại đây để đặt/hủy báo thức hệ thống
                        }
                    });

                    // Sự kiện Click vào thông tin báo thức để Chỉnh sửa
                    LinearLayout layoutAlarmInfo = item.binding.layoutAlarmInfo;
                    layoutAlarmInfo.setOnClickListener(v -> {
                        Log.d("HomeFragment", "Alarm item clicked at index: " + alarmIndex);
                        NavController navController = NavHostFragment.findNavController(HomeFragment.this);

                        // Tạo Bundle để chứa index
                        Bundle args = new Bundle();
                        args.putInt("alarmIndexToEdit", alarmIndex);

                        // Điều hướng đến màn hình CreateFragment VÀ truyền argument
                        try {
                            navController.navigate(R.id.action_home_to_createEditFragment, args);
                            Log.i("HomeFragment", "Navigating to CreateFragment with alarmIndexToEdit = " + alarmIndex);
                        } catch (IllegalArgumentException e) {
                            Log.e("HomeFragment", "Navigation failed! Check action ID or destination in nav_graph.xml", e);
                            Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show(); // Thêm string resource
                        }
                    });
                }
            } else {
                // Hiển thị thông báo nếu không có báo thức nào
    //            textViewNoAlarms.setVisibility(View.VISIBLE);
                layoutAlarmList.setVisibility(View.GONE); // Ẩn list view
                Log.i("HomeFragment", "No alarms found in the list.");
            }


            ImageButton btnSetting = view.findViewById(R.id.btnSetting);
            btnSetting.setOnClickListener(v->{
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.navigation_settings);
            });
            return view;
        }

        // Hàm lấy tên tùy chọn lặp lại (tương tự logic trong CreateFragment)
        private String getRepeatOptionName(Context context, AlarmData data) {
            Log.d("HomeFragment", "getRepeatOptionName called with data: " + context.getString(R.string.loop_option_daily));
            if (data == null) return context.getString(R.string.loop_option_daily); // Mặc định

            int loopIndex = data.loopIndex;

            if (loopIndex == 3) {
                StringBuilder customDays = new StringBuilder();
                boolean first = true;
                if (data.optionOther != null) {
                    for (Pair<Integer, Boolean> dayPair : data.optionOther) {
                        if (dayPair != null && dayPair.second != null && dayPair.second) { // Nếu ngày được chọn
                            if (!first) customDays.append(", ");
                            Object dayValue = dayPair.first;
                            String dayName = "";
                            if (dayValue instanceof Integer) { try { dayName = context.getString((Integer) dayValue); } catch (Exception e) {} }
                            else if (dayValue != null) { dayName = dayValue.toString(); }

                            if (dayName.length() >= 2) customDays.append(dayName.substring(0, 2));
                            else customDays.append(dayName);
                            first = false;
                        }
                    }
                }
                if (customDays.length() > 0) {
                    return customDays.toString();
                } else {
                    if (data.loopOption != null && data.loopOption.length > 3 && data.loopOption[3] != null && data.loopOption[3].first != null) {
                        Object value = data.loopOption[3].first;
                        return (value instanceof Integer) ? context.getString((Integer)value) : value.toString();
                    } else {
                        return context.getString(R.string.loop_option_custom);
                    }
                }
            } else if (data.loopOption != null && loopIndex >= 0 && loopIndex < data.loopOption.length && data.loopOption[loopIndex] != null && data.loopOption[loopIndex].first != null) {
                Object value = data.loopOption[loopIndex].first;
                Log.d("HomeFragment", "Value: " + value);
                if (value instanceof Integer) { try {
                    //TODO: fix this
                    Log.d("HomeFragment", "Integer value: " + context.getString((Integer) value));
                return context.getString((Integer) value); } catch (Exception e) { return "ID:" + value; } }
                else { return value.toString(); }
            }

            return context.getString(R.string.loop_option_daily); // Mặc định cuối cùng
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            layoutAlarmList = null;
    //        textViewNoAlarms = null;
            Log.d("HomeFragment", "onDestroyView called.");
        }
    }