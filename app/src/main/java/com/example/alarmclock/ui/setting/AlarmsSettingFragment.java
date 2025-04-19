package com.example.alarmclock.ui.setting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView; // Thêm import TextView

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Thêm import Nullable
import androidx.appcompat.app.AppCompatDelegate; // Thêm import AppCompatDelegate
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclock.R;
import com.example.alarmclock.databinding.FragmentSettingAlarmBinding;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class AlarmsSettingFragment extends Fragment {

    private FragmentSettingAlarmBinding binding;
    private SharedPreferences prefs;


    private static final String PREFS_NAME = "Settings";
    private static final String PREF_KEY_THEME = "AppTheme";
    private static final String PREF_KEY_LANGUAGE = "AppLanguage";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingAlarmBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Context context = requireContext();

        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupToolbar();
        setupThemeSwitch();
        setupLanguageSelection();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadInitialThemeState();
    }


    private void setupToolbar() {
        binding.ivBackArrow.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack();
        });
    }

    private void loadInitialThemeState() {
        int currentNightMode = prefs.getInt(PREF_KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        boolean isCurrentlyDarkMode = (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES);

        binding.switchTheme.setChecked(isCurrentlyDarkMode);
        updateThemeValueText(isCurrentlyDarkMode);
    }

    private void setupThemeSwitch() {
        binding.switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int selectedMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_KEY_THEME, selectedMode);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(selectedMode);

            updateThemeValueText(isChecked);

//             if (getActivity() != null) {
//                 getActivity().recreate();
//             }
        });
    }

    // Helper method để cập nhật Text hiển thị theme
    private void updateThemeValueText(boolean isDarkMode) {
        if (isDarkMode) {
            binding.tvThemeValue.setText(R.string.night);
        } else {
            binding.tvThemeValue.setText(R.string.light);
        }
    }


    private void setupLanguageSelection() {
        binding.layoutLanguage.setOnClickListener(v -> {
            final String[] languages = {"English", "Tiếng Việt", "한국어"};
            final String[] languageCodes = {"en", "vi", "ko"};

            String currentLangCode = getCurrentLanguageCode();
            int currentLanguageIndex = 0; // Mặc định là English nếu không tìm thấy
            for (int i = 0; i < languageCodes.length; i++) {
                if (languageCodes[i].equals(currentLangCode)) {
                    currentLanguageIndex = i;
                    break;
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.choose_a_language);

            builder.setSingleChoiceItems(languages, currentLanguageIndex, (dialog, which) -> {
                String selectedLanguageCode = languageCodes[which];
                saveLanguagePreference(selectedLanguageCode);
                applyLanguageChange(selectedLanguageCode);
                dialog.dismiss();
            });

            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private String getCurrentLanguageCode() {
        // Mặc định là 'en' nếu chưa có lựa chọn nào được lưu
        return prefs.getString(PREF_KEY_LANGUAGE, Locale.getDefault().getLanguage());
    }

    private void saveLanguagePreference(String languageCode) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_LANGUAGE, languageCode);
        editor.apply();
    }

    private void applyLanguageChange(String languageCode) {
        Locale newLocale = new Locale(languageCode);
        Locale.setDefault(newLocale);
        Resources res = requireContext().getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(newLocale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}