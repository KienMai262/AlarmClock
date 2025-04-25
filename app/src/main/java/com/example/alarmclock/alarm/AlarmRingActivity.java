package com.example.alarmclock.alarm;

import android.annotation.SuppressLint;
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

import com.example.alarmclock.Pair;
import com.example.alarmclock.R; // Đảm bảo R được import
import com.example.alarmclock.ui.quiz.QuizActivity;

import java.util.List;

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
        String subject = null;
        String topic = null;
        String difficulty = null;
        int numQuestions = 5; // Giá trị mặc định

        if (currentAlarmId != -1) {
            List<Pair<AlarmData, Boolean>> alarms = AlarmStorageUtil.loadAlarmsFromFile(this);
            if (alarms != null && currentAlarmId >= 0 && currentAlarmId < alarms.size()) {
                Pair<AlarmData, Boolean> currentAlarmPair = alarms.get(currentAlarmId);
                if (currentAlarmPair != null && currentAlarmPair.first != null) {
                    AlarmData alarmData = currentAlarmPair.first;
                    subject = alarmData.subject;
                    topic = alarmData.topic;
                    difficulty = alarmData.difficulty;
                    // Lấy numQuestions, kiểm tra xem có giá trị hợp lệ không
                    if (alarmData.numQuestions > 0) {
                        numQuestions = alarmData.numQuestions;
                    }
                    Log.d(TAG, "Loaded from Storage - Subject: " + subject);
                    Log.d(TAG, "Loaded from Storage - Topic: " + topic);
                    Log.d(TAG, "Loaded from Storage - Difficulty: " + difficulty);
                    Log.d(TAG, "Loaded from Storage - NumQuestions: " + numQuestions);
                } else {
                    Log.e(TAG, "Alarm data pair or AlarmData is null for id: " + currentAlarmId);
                }
            } else {
                Log.e(TAG, "Could not load alarms or invalid alarmId: " + currentAlarmId);
            }
        } else {
            Log.e(TAG, "Received invalid alarmId: -1");
        }
        if (alarmNote == null || alarmNote.isEmpty()) {
            alarmNote = "Báo thức!"; // Ghi chú mặc định
        }

        Log.d(TAG, "Retrieved - alarmId: " + currentAlarmId);
        Log.d(TAG, "Retrieved - alarmNote: " + alarmNote);
        Log.d(TAG, "Retrieved - subject: " + subject); // QUAN TRỌNG
        Log.d(TAG, "Retrieved - topic: " + topic);     // QUAN TRỌNG
        Log.d(TAG, "Retrieved - difficulty: " + difficulty); // QUAN TRỌNG
        Log.d(TAG, "Retrieved - numQuestions: " + numQuestions); // QUAN TRỌNG

        TextView noteTextView = findViewById(R.id.alarmLabel);
        Button doQuizButton = findViewById(R.id.doQuizButton);

        if (noteTextView != null) {
            noteTextView.setText(alarmNote);
        } else {
            Log.w(TAG, "TextView with ID alarmLabel not found.");
        }

        // logic để hiển thị sang màn hình quiz
        if (doQuizButton != null) {
            final String finalSubject = subject;
            final String finalTopic = topic;
            final String finalDifficulty = difficulty;
            final int finalNumQuestions = numQuestions;

            doQuizButton.setOnClickListener(v -> {
                sendReduceVolumeCommand();

                Log.d(TAG, "Starting Quiz - numsQuestions: " + finalNumQuestions);
                Log.d(TAG, "Starting Quiz - subject: " + finalSubject); // Log này bây giờ nên đúng

                Intent quizIntent = new Intent(AlarmRingActivity.this, QuizActivity.class);
                quizIntent.putExtra("alarmId", currentAlarmId);
                quizIntent.putExtra("subject", finalSubject); // Truyền dữ liệu đã lấy từ storage
                quizIntent.putExtra("topic", finalTopic);
                quizIntent.putExtra("difficulty", finalDifficulty);
                quizIntent.putExtra("numQuestions", finalNumQuestions);
                startActivity(quizIntent);
                finish(); // đóng Alarm UI
            });

        } else {
            Log.e(TAG, "Button with ID stopButton not found.");
        }
    }

    private void sendReduceVolumeCommand() {
        Log.i(TAG,"Sending reduce volume request to AlarmRingService.");
        Intent reduceIntent = new Intent(this, AlarmRingService.class);
        reduceIntent.setAction(AlarmRingService.ACTION_REDUCE_VOLUME);
        startService(reduceIntent); // Gửi yêu cầu giảm âm lượng đến Service
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