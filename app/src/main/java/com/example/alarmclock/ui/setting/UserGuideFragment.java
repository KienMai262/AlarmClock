package com.example.alarmclock.ui.setting; // Thay package cho phù hợp

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView; // Chỉ cần nếu bạn muốn truy cập TextView trong code, không cần thiết nếu chỉ set text qua XML

import com.example.alarmclock.R; // Import R
import com.example.alarmclock.databinding.FragmentSettingBinding;
import com.example.alarmclock.databinding.FragmentUserGuideBinding;

public class UserGuideFragment extends Fragment {

    private FragmentUserGuideBinding binding;

    public UserGuideFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserGuideBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.ivBackArrow.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack();
        });
        // Inflate the layout for this fragment
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Lấy tham chiếu View nếu cần (ví dụ: thay đổi text động)
        // textViewGuideTitle = view.findViewById(R.id.textViewGuideTitle);
        // textViewGuideContent = view.findViewById(R.id.textViewGuideContent);

        // !!! Quan trọng: Không cần setText ở đây nữa nếu bạn đã đặt android:text="@string/..." trong XML.
        // Android sẽ tự động xử lý việc tải đúng ngôn ngữ khi Fragment được tạo hoặc ngôn ngữ thay đổi.
        // textViewGuideTitle.setText(R.string.user_guide_title);
        // textViewGuideContent.setText(R.string.user_guide_content);
    }
}