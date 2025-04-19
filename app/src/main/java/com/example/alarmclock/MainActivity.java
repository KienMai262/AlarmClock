package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

import com.example.alarmclock.alarm.AlarmReceiver;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.alarmclock.databinding.ActivityMainBinding;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String PREFS_NAME = "Settings";
    private static final String PREF_KEY_THEME = "AppTheme";
    private static final String PREF_KEY_LANGUAGE = "AppLanguage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int currentNightMode = prefs.getInt(PREF_KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(currentNightMode);

        String currentLanguageCode = prefs.getString(PREF_KEY_LANGUAGE, Locale.getDefault().getLanguage());
        setLocale(this, currentLanguageCode);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        checkAndRequestNotificationPermission();


    }

    public static void setLocale(Context context, String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            languageCode = Locale.getDefault().getLanguage();
        }
        Locale newLocale = new Locale(languageCode);
        Locale.setDefault(newLocale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(newLocale);

        res.updateConfiguration(config, res.getDisplayMetrics());

         Resources appRes = context.getApplicationContext().getResources();
         Configuration appConfig = new Configuration(appRes.getConfiguration());
         appConfig.setLocale(newLocale);
         appRes.updateConfiguration(appConfig, appRes.getDisplayMetrics());
    }

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Quyền chưa được cấp, yêu cầu người dùng
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                // Quyền đã được cấp
                Log.d("PermissionCheck", "POST_NOTIFICATIONS permission already granted.");
            }
        }
    }

    // Xử lý kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền đã được cấp
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Quyền bị từ chối
                Toast.makeText(this, "Notification permission denied. Alarms may not show notifications.", Toast.LENGTH_LONG).show();
                // Có thể hiển thị giải thích và hướng dẫn người dùng vào cài đặt
            }
        }
    }

}