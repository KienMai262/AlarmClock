package com.example.alarmclock.ui.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alarmclock.R;
import com.example.alarmclock.databinding.FragmentCreateBinding;

public class CreateFragment extends Fragment {

    private FragmentCreateBinding binding;

    protected Boolean knoll;
    protected Boolean deleteAfterAlarm;
    protected String note;

    final int MAX_DISPLAY_LENGTH = 20;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CreateViewModel createViewModel =
                new ViewModelProvider(this).get(CreateViewModel.class);

        binding = FragmentCreateBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Lấy TextView từ ViewBinding
        TextView btnRepeat = binding.btnRepeat;

        btnRepeat.setOnClickListener(v -> {
            final String[] items = {"Một lần", "Hàng ngày", "Thứ hai đến Thứ sáu", "Tùy chỉnh"};

            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Chọn kiểu lặp lại")
                    .setItems(items, (dialog, which) -> {
                        btnRepeat.setText(items[which] + " >");
                    })
                    .show();
        });

        TextView btnMusic = binding.btnMusic;
        btnMusic.setOnClickListener(v -> {
            final String[] items = {"Nhạc 1", "Nhạc 2", "Nhạc 3"};
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Chọn nhạc chuông")
                    .setItems(items, (dialog, which) -> {
                        btnMusic.setText(items[which] + " >");
                    })
                    .show();
        });

        SwitchCompat btnKnoll = binding.knoll;
        knoll = btnKnoll.isChecked();

        SwitchCompat btnDeleteAfterAlarm = binding.deleteAfterAlarm;
        deleteAfterAlarm = btnDeleteAfterAlarm.isChecked();

        TextView btnNote = binding.btnNote;
        btnNote.setOnClickListener(v -> {
            // Tạo EditText để nhập
            final EditText input;
            if(note != null) {
                input = new EditText(getContext());
                input.setText(note);
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
                        note = userInput;
                        if (!userInput.isEmpty()) {
                            final int MAX_DISPLAY_LENGTH = 15; // Đặt giới hạn ký tự hiển thị ở đây
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



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}