package com.example.alarmclock.ui.quiz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alarmclock.R;
import com.example.alarmclock.alarm.Question;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections; // Thêm import này
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final long TIME_PER_QUESTION = 10000; // 10 seconds
    private static final String TAG = "QuizActivity"; // Thêm TAG để Log

    private TextView questionTextView, timerTextView;
    private Button[] answerButtons;
    // Biến thành viên để lưu danh sách câu hỏi đã lọc
    private List<Question> filteredQuestions;
    private int currentIndex = 0;
    private CountDownTimer countDownTimer;
    private int alarmId;

    // Các biến này không cần public, chỉ cần dùng trong Activity này
    private String subject, topic, difficulty;
    private int numQuestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        initQuestionsFileIfNeeded(); // Tạo file câu hỏi mẫu nếu chưa tồn tại

        // Get data from Intent
        Intent intent = getIntent();
        alarmId = intent.getIntExtra("alarmId", -1);
        subject = intent.getStringExtra("subject");
        topic = intent.getStringExtra("topic");
        difficulty = intent.getStringExtra("difficulty");
        numQuestions = intent.getIntExtra("numQuestions", 5);

        Log.d(TAG, "Received data - Subject: " + subject + ", Topic: " + topic + ", Difficulty: " + difficulty + ", NumQuestions: " + numQuestions);

        // Bind views
        timerTextView = findViewById(R.id.timerTextView);
        questionTextView = findViewById(R.id.questionTextView);
        answerButtons = new Button[]{
                findViewById(R.id.answerA),
                findViewById(R.id.answerB),
                findViewById(R.id.answerC),
                findViewById(R.id.answerD)
        };

        // Load and filter questions, GÁN VÀO BIẾN THÀNH VIÊN
        filteredQuestions = getFilteredQuestions(subject, topic, difficulty, numQuestions);

        // KIỂM TRA NULL VÀ RỖNG TRƯỚC KHI BẮT ĐẦU QUIZ
        if (filteredQuestions != null && !filteredQuestions.isEmpty()) {
            Log.d(TAG, "Starting quiz with " + filteredQuestions.size() + " questions.");
            displayQuestions(); // Không cần truyền tham số nữa vì dùng biến thành viên
            showNextQuestion();
        } else {
            Log.w(TAG, "No suitable questions found or error loading questions.");
            Toast.makeText(this, "Không tìm thấy câu hỏi phù hợp.", Toast.LENGTH_LONG).show();
            finish(); // Kết thúc Activity nếu không có câu hỏi
        }
    }

    private String loadJSONFromSystemFile() {
        File file = new File(getFilesDir(), "questions.json");
        if (!file.exists()) {
            Log.e(TAG, "questions.json not found in system folder");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading questions.json from system", e);
            return null;
        }
        return sb.toString();
    }


    private List<Question> getFilteredQuestions(String subject, String topic, String difficulty, int numQuestions) {
        // Kiểm tra đầu vào cơ bản
        if (subject == null || topic == null || difficulty == null) {
            Log.e(TAG, "Cannot filter questions with null parameters.");
            return new ArrayList<>(); // Trả về list rỗng
        }

        String jsonString = loadJSONFromSystemFile();
        List<Question> matchingQuestions = new ArrayList<>(); // Danh sách chứa câu hỏi khớp

        if (jsonString != null && !jsonString.isEmpty()) {
            try {
                Type questionListType = new TypeToken<List<Question>>() {}.getType();
                // Nên dùng try-catch khi parse JSON
                List<Question> allQuestions = new Gson().fromJson(jsonString, questionListType);

                if (allQuestions != null) { // Kiểm tra null sau khi parse
                    for (Question question : allQuestions) {
                        // Thêm kiểm tra null cho các trường của question để tránh lỗi
                        if (question != null && question.getSubject() != null && question.getTopic() != null && question.getDifficulty() != null) {
                            boolean matchesSubject = question.getSubject().equalsIgnoreCase(subject);
                            boolean matchesTopic = question.getTopic().equalsIgnoreCase(topic);
                            boolean matchesDifficulty = question.getDifficulty().equalsIgnoreCase(difficulty);

                            if (matchesSubject && matchesTopic && matchesDifficulty) {
                                matchingQuestions.add(question);
                            }
                        } else {
                            Log.w(TAG, "Skipping invalid question object in JSON data.");
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to parse JSON string into question list.");
                }
            } catch (Exception e) { // Bắt lỗi chung khi parse hoặc xử lý
                Log.e(TAG, "Error processing questions JSON", e);
                // Có thể trả về list rỗng hoặc xử lý khác
                return new ArrayList<>();
            }

            // Xáo trộn danh sách câu hỏi đã lọc
            Collections.shuffle(matchingQuestions);

            // Cắt lấy số lượng câu hỏi mong muốn
            if (matchingQuestions.size() > numQuestions) {
                // Tạo sublist và trả về một ArrayList mới từ sublist đó
                return new ArrayList<>(matchingQuestions.subList(0, numQuestions));
            }
        } else {
            Log.w(TAG, "JSON string is null or empty. Cannot load questions.");
        }

        return matchingQuestions; // Trả về danh sách đã lọc (có thể rỗng)
    }

    // showNextQuestion bây giờ sử dụng biến thành viên filteredQuestions
    private void showNextQuestion() {
        // Kiểm tra lại filteredQuestions ở đây cho chắc chắn (dù đã kiểm tra ở onCreate)
        if (filteredQuestions == null || currentIndex >= filteredQuestions.size()) {
            finishQuiz();
            return;
        }

        Question q = filteredQuestions.get(currentIndex);
        // Thêm kiểm tra null cho các trường của q
        if (q == null || q.content == null || q.answers == null || q.answers.size() < 4 || q.trueAnswer == null) {
            Log.e(TAG, "Invalid question data at index: " + currentIndex);
            // Bỏ qua câu hỏi lỗi và chuyển sang câu tiếp theo
            currentIndex++;
            showNextQuestion();
            return;
        }

        questionTextView.setText(q.content);

        // Đảm bảo có đủ 4 nút và 4 đáp án
        for (int i = 0; i < Math.min(answerButtons.length, q.answers.size()); i++) {
            String answerText = q.answers.get(i);
            answerButtons[i].setText(answerText);
            // Lưu lại đáp án đúng của câu hỏi này để so sánh trong listener
            final String currentTrueAnswer = q.trueAnswer;

            answerButtons[i].setOnClickListener(v -> {
                stopTimer(); // Dừng timer khi câu trả lời được chọn
                Button clickedButton = (Button) v;
                String selectedAnswer = clickedButton.getText().toString();

                if (selectedAnswer.trim().equalsIgnoreCase(currentTrueAnswer.trim())) {
                    Log.d(TAG, "Correct answer selected!");
                    currentIndex++;
                    showNextQuestion(); // Chuyển sang câu hỏi tiếp theo
                } else {
                    Log.d(TAG, "Incorrect answer selected.");
                    restartAlarm(); // Đáp án sai
                }
            });
        }
        // Ẩn các nút thừa nếu câu hỏi có ít hơn 4 đáp án (trường hợp hiếm)
        for (int i = q.answers.size(); i < answerButtons.length; i++) {
            answerButtons[i].setVisibility(View.GONE);
        }
        // Hiện lại các nút nếu trước đó bị ẩn
        for (int i = 0; i < q.answers.size() && i < answerButtons.length; i++) {
            answerButtons[i].setVisibility(View.VISIBLE);
        }


        startTimer(); // Khởi động lại bộ đếm thời gian cho câu hỏi mới
    }

    // displayQuestions bây giờ sử dụng biến thành viên filteredQuestions
    private void displayQuestions() {
        // Không cần kiểm tra null/empty ở đây nữa vì đã làm ở onCreate
        // Chỉ cần hiển thị câu hỏi đầu tiên (currentIndex = 0)
        Question q = filteredQuestions.get(currentIndex);

        // Thêm kiểm tra null cho các trường của q
        if (q == null || q.content == null || q.answers == null || q.answers.size() < 4 || q.trueAnswer == null) {
            Log.e(TAG, "Invalid initial question data at index: 0");
            Toast.makeText(this, "Lỗi dữ liệu câu hỏi.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        questionTextView.setText(q.content);  // Hiển thị nội dung câu hỏi

        // Hiển thị các đáp án cho câu hỏi đầu tiên
        for (int i = 0; i < Math.min(answerButtons.length, q.answers.size()); i++) {
            answerButtons[i].setText(q.answers.get(i));
            // Listener sẽ được đặt lại trong showNextQuestion cho từng câu
        }

        // Không gọi startTimer ở đây nữa, nó sẽ được gọi trong showNextQuestion
    }


    private void startTimer() {
        stopTimer(); // Dừng timer cũ nếu có
        countDownTimer = new CountDownTimer(TIME_PER_QUESTION, 1000) {
            @SuppressLint("SetTextI18n") // Bỏ qua cảnh báo hardcoded string "00:"
            public void onTick(long millisUntilFinished) {
                // Format thời gian còn lại thành MM:SS hoặc chỉ SS
                long seconds = millisUntilFinished / 1000;
                timerTextView.setText("00:" + String.format("%02d", seconds)); // Luôn hiển thị 2 chữ số giây
            }

            public void onFinish() {
                Log.d(TAG, "Timer finished.");
                restartAlarm(); // Hết giờ
            }
        };
        countDownTimer.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null; // Đặt lại thành null
        }
    }

    private void restartAlarm() {
        Toast.makeText(this, "Sai hoặc hết thời gian!", Toast.LENGTH_SHORT).show(); // Thay đổi thông báo
        // Hiện tại chỉ kết thúc quiz, không reschedule báo thức
        finish();
    }

    private void finishQuiz() {
        Toast.makeText(this, "Bạn đã hoàn thành quiz!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer(); // Đảm bảo timer được dừng khi activity bị hủy
    }

    // initQuestionsFileIfNeeded cần kiểm tra file tồn tại trước khi ghi đè
    private void initQuestionsFileIfNeeded() {
        File file = new File(getFilesDir(), "questions.json");

        // ---> THÊM KIỂM TRA NÀY <---
        if (!file.exists()) {
            Log.d("QuizInit", "questions.json not found. Creating initial file.");
            // Dữ liệu mẫu ban đầu
            List<Question> initialQuestions = new ArrayList<>();

            // ... (TOÀN BỘ PHẦN TẠO initialQuestions của bạn giữ nguyên ở đây) ...
            // MATH - ALGEBRA
            initialQuestions.add(new Question("Math", "Algebra", "Easy", "Solve for x: 2x + 3 = 7",
                    Arrays.asList("x = 1", "x = 2", "x = 3", "x = 4"), "x = 2"));
            // ... Thêm các câu hỏi khác ...
            initialQuestions.add(new Question("English", "Reading", "Nightmare", "In Samuel Beckett's 'Waiting for Godot', who or what is 'Godot' commonly interpreted to represent?",
                    Arrays.asList("A political figure", "Death", "God or salvation", "There is no definitive interpretation"), "There is no definitive interpretation"));


            // Ghi vào file
            // Sử dụng GsonBuilder để định dạng JSON cho đẹp (tùy chọn)
            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(initialQuestions);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonString);
                Log.d("QuizInit", "questions.json created successfully in system directory.");
            } catch (IOException e) {
                Log.e("QuizInit", "Error writing initial questions.json", e);
            }
        } else {
            Log.d("QuizInit", "questions.json already exists. Skipping creation.");
        }
    }
}