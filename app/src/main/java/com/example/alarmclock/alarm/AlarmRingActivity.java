package com.example.alarmclock.alarm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alarmclock.R; // Đảm bảo R được import

public class AlarmRingActivity extends AppCompatActivity {
    private static final String TAG = "AlarmRingActivity";
    private int currentAlarmId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");

        // --- Hiển thị trên màn hình khóa và bật màn hình ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
             KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
             if(keyguardManager!= null) keyguardManager.requestDismissKeyguard(this, null); // Cân nhắc nếu muốn tự bỏ qua màn hình khóa
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_alarm_ring); // Đảm bảo layout này tồn tại

        // --- Lấy dữ liệu từ Intent (gửi bởi AlarmRingService) ---
        Intent intent = getIntent();
        currentAlarmId = intent.getIntExtra("alarmId", -1);
        String alarmNote = intent.getStringExtra("alarmNote");
        if (alarmNote == null || alarmNote.isEmpty()) {
            alarmNote = "Báo thức!"; // Ghi chú mặc định
        }

        Log.d(TAG,"Displaying UI for alarmId: " + currentAlarmId);
        Log.d(TAG,"Alarm note: " + alarmNote);

        TextView noteTextView = findViewById(R.id.alarmLabel);
        Button stopButton = findViewById(R.id.stopButton);

        if (noteTextView != null) {
            noteTextView.setText(alarmNote);
        } else {
            Log.w(TAG, "TextView with ID alarmLabel not found.");
        }

        // --- Logic nút Stop ---
        if (stopButton != null) {
            stopButton.setOnClickListener(v -> {
                Log.d(TAG, "Stop button clicked for alarmId: " + currentAlarmId);
                stopAlarmService();
                finish(); // Đóng Activity sau khi yêu cầu dừng Service
            });
        } else {
            Log.e(TAG, "Button with ID stopButton not found.");
        }
    }

    // Gửi yêu cầu dừng đến Service
    private void stopAlarmService() {
        Log.i(TAG,"Sending stop request to AlarmRingService.");
        Intent stopIntent = new Intent(this, AlarmRingService.class);
        stopIntent.setAction(AlarmRingService.ACTION_STOP_ALARM);
        // Bạn không cần truyền dữ liệu gì thêm, action là đủ
        startService(stopIntent); // Gửi yêu cầu dừng đến Service
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
        // Không cần giải phóng MediaPlayer/AudioManager ở đây nữa
    }

    // Ngăn chặn việc đóng Activity bằng nút Back mà không dừng Service
    @Override
    public void onBackPressed() {
         super.onBackPressed(); // Không gọi super để ngăn chặn Back
        Log.d(TAG, "Back button pressed. Use Stop button.");
        // Thêm chuỗi use_stop_button_to_dismiss vào strings.xml
        android.widget.Toast.makeText(this, "Nhấn nút Stop để tắt báo thức", android.widget.Toast.LENGTH_SHORT).show();
        // Không làm gì cả hoặc có thể gọi stopAlarmService() nếu muốn nút Back hoạt động như Stop
        // stopAlarmService();
        // finish();
    }
}