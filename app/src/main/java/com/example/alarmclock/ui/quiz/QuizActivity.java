package com.example.alarmclock.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alarmclock.R;
import com.example.alarmclock.alarm.Question;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuizActivity extends AppCompatActivity {

    private TextView questionTextView, timerTextView;
    private Button[] answerButtons;
    private List<Question> filteredQuestions;
    private int currentIndex = 0;
    private CountDownTimer countDownTimer;
    private static final long TIME_PER_QUESTION = 10000; // 10 seconds

    private int alarmId;
    private String subject, topic, difficulty;
    private int numQuestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Get data from Intent
        Intent intent = getIntent();
        alarmId = intent.getIntExtra("alarmId", -1);
        subject = intent.getStringExtra("subject");
        topic = intent.getStringExtra("topic");
        difficulty = intent.getStringExtra("difficulty");
        numQuestions = intent.getIntExtra("numQuestions", 5);

        // Bind views
        timerTextView = findViewById(R.id.timerTextView);
        questionTextView = findViewById(R.id.questionTextView);
        answerButtons = new Button[]{
                findViewById(R.id.answerA),
                findViewById(R.id.answerB),
                findViewById(R.id.answerC),
                findViewById(R.id.answerD)
        };

        // Load questions
        List<Question> questions = getFilteredQuestions(subject, topic, difficulty, numQuestions);

        // Xử lý câu hỏi (hiển thị câu hỏi, tạo giao diện quiz, v.v.)
        displayQuestions(questions);
        showNextQuestion();
    }

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("questions.json"); // Đọc từ assets
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e("QuizActivity", "Error reading questions.json", ex);
        }
        return json;
    }

    private List<Question> getFilteredQuestions(String subject, String topic, String difficulty, int numQuestions) {
        // Đọc JSON từ assets
        String jsonString = loadJSONFromAsset();
        List<Question> filteredQuestions = new ArrayList<>();

        if (jsonString != null) {
            // Parse JSON thành danh sách câu hỏi
            Type questionListType = new TypeToken<List<Question>>() {}.getType();
            List<Question> allQuestions = new Gson().fromJson(jsonString, questionListType);

            // Lọc câu hỏi theo điều kiện
            for (Question question : allQuestions) {
                boolean matchesSubject = question.getSubject().equalsIgnoreCase(subject);
                boolean matchesTopic = question.getTopic().equalsIgnoreCase(topic);
                boolean matchesDifficulty = question.getDifficulty().equalsIgnoreCase(difficulty);

                if (matchesSubject && matchesTopic && matchesDifficulty) {
                    filteredQuestions.add(question);
                }
            }

            // Giới hạn số lượng câu hỏi nếu cần
            if (filteredQuestions.size() > numQuestions) {
                filteredQuestions = filteredQuestions.subList(0, numQuestions);
            }
        }

        return filteredQuestions;
    }



    private void showNextQuestion() {
        if (currentIndex >= filteredQuestions.size()) {
            finishQuiz();
            return;
        }

        Question q = filteredQuestions.get(currentIndex);
        questionTextView.setText(q.content);

        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(q.options.get(i));
            String selected = q.options.get(i);
            answerButtons[i].setOnClickListener(v -> {
                stopTimer(); // Dừng timer khi câu trả lời được chọn
                if (selected.equals(q.trueAnswer)) {
                    // Đáp án đúng
                    currentIndex++;
                    showNextQuestion(); // Chuyển sang câu hỏi tiếp theo
                } else {
                    // Đáp án sai -> Tùy chọn logic xử lý
                    restartAlarm(); // Hoặc xử lý theo cách khác (ví dụ: báo sai)
                }
            });
        }

        startTimer(); // Khởi động lại bộ đếm thời gian cho câu hỏi mới
    }

    private void displayQuestions(List<Question> questions) {
        // Kiểm tra danh sách câu hỏi không rỗng
        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "Không có câu hỏi để hiển thị", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy câu hỏi đầu tiên và hiển thị nó
        Question q = questions.get(currentIndex);
        questionTextView.setText(q.content);  // Hiển thị nội dung câu hỏi

        // Hiển thị các đáp án cho câu hỏi
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(q.options.get(i));  // Gán đáp án vào các nút
            String selected = q.options.get(i); // Lưu đáp án để so sánh khi người dùng chọn

            // Đặt sự kiện click cho mỗi nút đáp án
            answerButtons[i].setOnClickListener(v -> {
                stopTimer();  // Dừng đồng hồ đếm ngược khi người dùng chọn đáp án
                if (selected.trim().equalsIgnoreCase(q.trueAnswer.trim())) {
                    // Nếu đúng, chuyển sang câu hỏi tiếp theo
                    currentIndex++;
                    showNextQuestion();
                } else {
                    // Nếu sai, gọi lại báo thức hoặc xử lý khác
                    restartAlarm();
                }
            });
        }

        // Bắt đầu bộ đếm thời gian cho câu hỏi hiện tại
        startTimer();
    }


    private void startTimer() {
        countDownTimer = new CountDownTimer(TIME_PER_QUESTION, 1000) {
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("00:" + (millisUntilFinished / 1000));
            }

            public void onFinish() {
                restartAlarm(); // Time's up!
            }
        };
        countDownTimer.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void restartAlarm() {
        // You can re-trigger alarm logic here
        Toast.makeText(this, "Hết thời gian! Báo thức lại.", Toast.LENGTH_LONG).show();
        // Gọi lại báo thức
//        AlarmSchedulerUtil.scheduleAlarm(this, alarmId);
        finish();
    }

    private void finishQuiz() {
        Toast.makeText(this, "Bạn đã hoàn thành quiz!", Toast.LENGTH_SHORT).show();
        // Quay lại Home hoặc màn kết thúc
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}


