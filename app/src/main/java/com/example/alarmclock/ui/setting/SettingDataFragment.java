package com.example.alarmclock.ui.setting; // Hoặc package phù hợp

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclock.R;
import com.example.alarmclock.databinding.FragmentSettingDataBinding;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class SettingDataFragment extends Fragment {

    private FragmentSettingDataBinding binding;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "Settings";
    private static final String ALARM_DATA_FILENAME = "alarm_data.json";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingDataBinding.inflate(inflater, container, false);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupToolbar();
        setupShareButton();
        setupDeleteButton();

        return binding.getRoot();
    }

    private void setupToolbar() {
        binding.ivBackArrow.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack();
        });
        binding.tvToolbarTitle.setText(R.string.data);
    }

    private void setupShareButton() {
        binding.layoutShareData.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_share_data);
        });
    }

    private void setupDeleteButton() {
        binding.layoutDeleteData.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });
    }

    private String readJsonFileContent() {
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fis = requireContext().openFileInput(ALARM_DATA_FILENAME);
             InputStreamReader inputStreamReader = new InputStreamReader(fis);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            Log.e("DataFragment", "Alarm data file not found: " + e.getMessage());
            return getString(R.string.data_empty); // Trả về thông báo lỗi
        } catch (IOException e) {
            Log.e("DataFragment", "Error reading alarm data file: " + e.getMessage());
            return getString(R.string.data_empty); // Trả về thông báo lỗi
        }
        return stringBuilder.toString();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_deletion_title)
                .setMessage(R.string.confirm_deletion_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    performDeleteData();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert) // Icon cảnh báo
                .show();
    }

    private void performDeleteData() {
        boolean settingsCleared = prefs.edit().clear().commit();
        boolean fileDeleted = requireContext().deleteFile(ALARM_DATA_FILENAME);

        if (settingsCleared && fileDeleted) {
            Toast.makeText(requireContext(), R.string.data_deleted_success, Toast.LENGTH_SHORT).show(); // Thêm string
        } else if (settingsCleared) {
            // File có thể không tồn tại sẵn
            Toast.makeText(requireContext(), R.string.settings_deleted_file_not_found, Toast.LENGTH_SHORT).show(); // Thêm string
        } else {
            Toast.makeText(requireContext(), R.string.error_deleting_data, Toast.LENGTH_SHORT).show(); // Thêm string
        }

        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Quan trọng: Giải phóng binding
    }
}