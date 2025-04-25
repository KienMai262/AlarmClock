package com.example.alarmclock.alarm;

import android.annotation.SuppressLint; // Keep if needed, maybe not anymore
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast; // Import Toast

import androidx.appcompat.app.AppCompatActivity;

// import com.example.alarmclock.Pair; // No longer needed if not loading from file
import com.example.alarmclock.R; // Đảm bảo R được import
import com.example.alarmclock.ui.quiz.QuizActivity;

// import java.util.List; // No longer needed if not loading from file

public class AlarmRingActivity extends AppCompatActivity {
    private static final String TAG = "AlarmRingActivity";
    private int currentAlarmId = -1;
    // Không cần lưu các biến subject, topic,... ở đây nữa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");

        // --- Hiển thị trên màn hình khóa và bật màn hình (Giữ nguyên) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            // Cân nhắc lại việc tự động dismiss keyguard, có thể gây phiền
            // if(keyguardManager!= null) keyguardManager.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_alarm_ring); // Đảm bảo layout này tồn tại

        // --- Lấy dữ liệu TRỰC TIẾP từ Intent (gửi bởi AlarmRingService) ---
        Intent intent = getIntent();
        currentAlarmId = intent.getIntExtra("alarmId", -1);
        String alarmNote = intent.getStringExtra("alarmNote");
        // Lấy thông tin quiz trực tiếp
        String subject = intent.getStringExtra("subject");
        String topic = intent.getStringExtra("topic");
        String difficulty = intent.getStringExtra("difficulty");
        int numQuestions = intent.getIntExtra("numQuestions", 5); // Lấy giá trị hoặc default
        // !!! LẤY CỜ deleteAfterAlarm !!!
        boolean deleteAfterAlarm = intent.getBooleanExtra("deleteAfterAlarm", false);

        // --- XOÁ BỎ ĐOẠN CODE ĐỌC TỪ FILE ---
        /*
        if (currentAlarmId != -1) {
            List<Pair<AlarmData, Boolean>> alarms = AlarmStorageUtil.loadAlarmsFromFile(this);
            // ... logic đọc từ file đã bị xóa ...
        } else {
            Log.e(TAG, "Received invalid alarmId: -1");
        }
        */

        // Ghi chú mặc định
        if (alarmNote == null || alarmNote.isEmpty()) {
            alarmNote = getString(R.string.note_default); // Dùng string resource
        }

        // Log dữ liệu nhận được trực tiếp
        Log.d(TAG, "Received Direct - alarmId: " + currentAlarmId);
        Log.d(TAG, "Received Direct - alarmNote: " + alarmNote);
        Log.d(TAG, "Received Direct - subject: " + subject);
        Log.d(TAG, "Received Direct - topic: " + topic);
        Log.d(TAG, "Received Direct - difficulty: " + difficulty);
        Log.d(TAG, "Received Direct - numQuestions: " + numQuestions);
        Log.d(TAG, "Received Direct - deleteAfterAlarm: " + deleteAfterAlarm); // Log cờ mới

        // --- Hiển thị thông tin ---
        TextView noteTextView = findViewById(R.id.alarmLabel);
        Button doQuizButton = findViewById(R.id.doQuizButton); // Đảm bảo ID đúng trong layout

        if (noteTextView != null) {
            noteTextView.setText(alarmNote);
        } else {
            Log.w(TAG, "TextView with ID alarmLabel not found.");
        }

        // --- Xử lý nút "Làm Quiz" ---
        if (doQuizButton != null) {
            // Không cần biến final nữa vì đã lấy trực tiếp
            doQuizButton.setOnClickListener(v -> {
                // Kiểm tra các tham số quiz trước khi bắt đầu
                if (subject == null || topic == null || difficulty == null || numQuestions <= 0) {
                    Log.e(TAG, "Cannot start quiz due to missing or invalid parameters!");
                    Log.e(TAG, "Subject: " + subject + ", Topic: " + topic + ", Difficulty: " + difficulty + ", NumQ: " + numQuestions);
                    Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show(); // Dùng string resource
                    // Cân nhắc dừng báo thức nếu không thể bắt đầu quiz
                    stopAlarmService();
                    finish();
                    return; // Không tiếp tục
                }

                // Gửi lệnh giảm âm lượng và tắt rung
                sendReduceVolumeCommand();

                Log.d(TAG, "Starting Quiz - numQuestions: " + numQuestions);
                Log.d(TAG, "Starting Quiz - subject: " + subject);
                Log.d(TAG, "Starting Quiz - topic: " + topic);
                Log.d(TAG, "Starting Quiz - difficulty: " + difficulty);
                Log.d(TAG, "Starting Quiz - deleteAfterAlarm: " + deleteAfterAlarm);


                // Tạo Intent cho QuizActivity
                Intent quizIntent = new Intent(AlarmRingActivity.this, QuizActivity.class);
                quizIntent.putExtra("alarmId", currentAlarmId);
                // Truyền dữ liệu quiz đã nhận trực tiếp
                quizIntent.putExtra("subject", subject);
                quizIntent.putExtra("topic", topic);
                quizIntent.putExtra("difficulty", difficulty);
                quizIntent.putExtra("numQuestions", numQuestions);
                // !!! TRUYỀN CỜ deleteAfterAlarm SANG QUIZACTIVITY !!!
                quizIntent.putExtra("deleteAfterAlarm", deleteAfterAlarm);

                startActivity(quizIntent);
                finish(); // Đóng màn hình UI báo thức sau khi chuyển sang Quiz
            });

        } else {
            // Đảm bảo ID nút trong layout là doQuizButton
            Log.e(TAG, "Button with ID doQuizButton not found.");
            // Nếu không có nút quiz, có thể nên có nút Stop thay thế?
            // Hoặc tự động dừng báo thức nếu không có cách tắt?
            // stopAlarmService();
            // finish();
        }
    }

    // --- Hàm sendReduceVolumeCommand (Giữ nguyên) ---
    private void sendReduceVolumeCommand() {
        Log.i(TAG,"Sending reduce volume request to AlarmRingService.");
        Intent reduceIntent = new Intent(this, AlarmRingService.class);
        reduceIntent.setAction(AlarmRingService.ACTION_REDUCE_VOLUME);
        startService(reduceIntent); // Gửi yêu cầu giảm âm lượng đến Service
    }

    // --- Hàm stopAlarmService (Giữ nguyên) ---
    private void stopAlarmService() {
        Log.i(TAG,"Sending stop request to AlarmRingService.");
        Intent stopIntent = new Intent(this, AlarmRingService.class);
        stopIntent.setAction(AlarmRingService.ACTION_STOP_ALARM);
        startService(stopIntent); // Gửi yêu cầu dừng đến Service
    }

    // --- Hàm onDestroy (Giữ nguyên) ---
    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    // --- Hàm onBackPressed (Giữ nguyên hoặc thay đổi tùy ý) ---
    @SuppressLint("MissingSuperCall") // Thêm nếu bạn cố ý không gọi super.onBackPressed()
    @Override
    public void onBackPressed() {
        // Hiện tại đang ngăn chặn nút Back
        // super.onBackPressed(); // Không gọi super để ngăn chặn Back
        Log.d(TAG, "Back button pressed. Action prevented.");
        // Thêm chuỗi use_stop_button_to_dismiss vào strings.xml nếu chưa có
        Toast.makeText(this, R.string.alarms, Toast.LENGTH_SHORT).show(); // Thông báo rõ hơn
        // Nếu muốn nút Back hoạt động như nút Stop (không khuyến khích lắm):
        // stopAlarmService();
        // finish();
    }
}