package com.example.alarmclock.ui.setting;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclock.R;
import com.example.alarmclock.databinding.FragmentSettingBinding;

public class SettingFragment extends Fragment {

    private FragmentSettingBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Context context = requireContext();

        ImageView ivBackArrow = binding.ivBackArrow;
        ivBackArrow.setOnClickListener( v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);
        });

        RelativeLayout layoutAlarms = binding.layoutAlarms;
        layoutAlarms.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_setting_alarm);
        });

        RelativeLayout layoutDate = binding.layoutYourData;
        layoutDate.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_setting_data);
        });
        return root;
    }
}
