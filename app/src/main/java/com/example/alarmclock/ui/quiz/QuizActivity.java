package com.example.alarmclock.ui.quiz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alarmclock.R;
import com.example.alarmclock.alarm.AlarmRingService;
import com.example.alarmclock.alarm.Question;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream; // Thêm import này
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter; // Thêm import này
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final long TIME_PER_QUESTION = 15000;
    private static final String TAG = "QuizActivity";
    private static final String QUIZ_HISTORY_FILENAME = "quiz_history.json";

    private TextView questionTextView, timerTextView;
    private Button[] answerButtons;
    private List<Question> questionsForThisRound;
    private int currentIndex = 0;
    private CountDownTimer countDownTimer;
    private int alarmId;
    private String subject, topic, difficulty;
    private int numQuestionsRequested;
    private QuizAttempt currentAttempt;

    private LinearLayout quizLayout;
    private LinearLayout quizResultLayout;
    private TextView scoreTextView;
    private Button dismissButton;
    private Button retryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        initQuestionsFileIfNeeded();

        // Get data from Intent
        Intent intent = getIntent();
        alarmId = intent.getIntExtra("alarmId", -1);
        subject = intent.getStringExtra("subject");
        topic = intent.getStringExtra("topic");
        difficulty = intent.getStringExtra("difficulty");
        numQuestionsRequested = intent.getIntExtra("numQuestions", 5); // Lưu lại số câu yêu cầu

        Log.d(TAG, "Received data - Subject: " + subject + ", Topic: " + topic + ", Difficulty: " + difficulty + ", NumQuestions: " + numQuestionsRequested);

        // Bind views
        timerTextView = findViewById(R.id.timerTextView);
        questionTextView = findViewById(R.id.questionTextView);
        answerButtons = new Button[]{
                findViewById(R.id.answerA),
                findViewById(R.id.answerB),
                findViewById(R.id.answerC),
                findViewById(R.id.answerD)
        };

        quizLayout = findViewById(R.id.quizLayout);
        quizResultLayout = findViewById(R.id.quizResultLayout);
        scoreTextView = findViewById(R.id.scoreTextView);
        dismissButton = findViewById(R.id.dismissButton);
        retryButton = findViewById(R.id.retryButton);

        startNewQuizRound();
    }

    // Hàm bắt đầu một lượt quiz mới (hoặc làm lại)
    private void startNewQuizRound() {
        Log.d(TAG, "Starting new quiz round...");
        currentIndex = 0; // Reset chỉ số câu hỏi

        // Lấy và lọc câu hỏi CHO LƯỢT NÀY
        questionsForThisRound = getFilteredQuestions(subject, topic, difficulty, numQuestionsRequested);

        if (questionsForThisRound != null && !questionsForThisRound.isEmpty()) {
            // Tạo đối tượng lưu kết quả cho lượt chơi MỚI
            // Số câu hỏi thực tế có thể ít hơn số yêu cầu nếu không đủ câu hỏi trong file JSON
            currentAttempt = new QuizAttempt(subject, topic, difficulty, questionsForThisRound.size());
            Log.d(TAG, "Starting quiz with " + questionsForThisRound.size() + " questions.");

            displayCurrentQuestion(); // Hiển thị câu hỏi đầu tiên
            // startTimer sẽ được gọi trong showNextQuestion sau khi hiển thị xong
        } else {
            Log.w(TAG, "No suitable questions found. Cannot start quiz.");
            Toast.makeText(this, "Không tìm thấy câu hỏi phù hợp.", Toast.LENGTH_LONG).show();
            // Có thể cần gọi một phương thức để tắt báo thức ngay lập tức ở đây nếu muốn
            finish(); // Kết thúc Activity nếu không có câu hỏi
        }
    }

    // Hiển thị câu hỏi hiện tại và cài đặt nút bấm
    private void displayCurrentQuestion() {
        // Kiểm tra trước khi truy cập
        if (currentAttempt == null || questionsForThisRound == null || currentIndex >= questionsForThisRound.size()) {
            Log.e(TAG, "Error displaying question: Invalid state.");
            // Xử lý lỗi, ví dụ kết thúc quiz
            evaluateQuizAttempt(); // Thử đánh giá kết quả hiện tại
            return;
        }

        Question q = questionsForThisRound.get(currentIndex);
        if (q == null || q.content == null || q.answers == null || q.answers.size() < 4 || q.trueAnswer == null) {
            Log.e(TAG, "Invalid question data at index: " + currentIndex);
            // Bỏ qua câu hỏi lỗi và chuyển sang câu tiếp theo
            currentIndex++;
            displayCurrentQuestion(); // Gọi lại để hiển thị câu tiếp
            return;
        }

        questionTextView.setText(q.content);

        final String currentTrueAnswer = q.trueAnswer; // Lưu đáp án đúng của câu này

        // Cài đặt các nút bấm cho câu hỏi hiện tại
        for (int i = 0; i < Math.min(answerButtons.length, q.answers.size()); i++) {
            final String answerText = q.answers.get(i); // Đáp án trên nút này
            answerButtons[i].setText(answerText);
            answerButtons[i].setVisibility(View.VISIBLE); // Đảm bảo nút hiện
            answerButtons[i].setEnabled(true); // Đảm bảo nút bấm được

            answerButtons[i].setOnClickListener(v -> {
                stopTimer();
                boolean isCorrect = answerText.trim().equalsIgnoreCase(currentTrueAnswer.trim());
                // Lưu kết quả câu trả lời này vào lượt chơi hiện tại
                currentAttempt.addAnswer(q, answerText, isCorrect);
                Log.d(TAG, "Answered Q" + (currentIndex+1) + ": User='" + answerText + "', Correct='" + currentTrueAnswer + "', Result=" + isCorrect);
                Log.d(TAG, "Current Score: " + currentAttempt.correctAnswers + "/" + currentAttempt.answeredQuestions.size());


                // Chuyển sang câu tiếp theo
                currentIndex++;
                displayCurrentQuestion(); // Hiển thị câu hỏi tiếp theo (nếu còn)
            });
        }

        // Ẩn các nút không dùng
        for (int i = q.answers.size(); i < answerButtons.length; i++) {
            answerButtons[i].setVisibility(View.GONE);
        }

        startTimer(); // Bắt đầu đếm giờ cho câu hỏi này
    }


    // Hàm này không còn cần thiết vì logic đã tích hợp vào displayCurrentQuestion
    // private void showNextQuestion() { ... }

    // Hàm này cũng không còn cần thiết vì logic đã tích hợp vào displayCurrentQuestion
    // private void displayQuestions() { ... }


    // Hàm đánh giá kết quả cuối lượt chơi
    @SuppressLint("DefaultLocale") // Cho String.format
    private void evaluateQuizAttempt() {
        stopTimer();
        if (currentAttempt == null) {
            Log.e(TAG, "Cannot evaluate null attempt.");
            finish();
            return;
        }

        double scorePercent = currentAttempt.getScorePercentage();
        boolean passed = scorePercent >= 50.0;
        currentAttempt.finishAttempt(passed);

        Log.i(TAG, "Quiz attempt finished. Score: " + currentAttempt.correctAnswers + "/" + currentAttempt.totalQuestionsInThisAttempt + " (" + String.format("%.1f", scorePercent) + "%). Passed: " + passed);
        saveQuizAttemptToHistory(currentAttempt);

        // ---- HIỂN THỊ KẾT QUẢ ----
        quizLayout.setVisibility(View.GONE); // Ẩn phần làm quiz
        quizResultLayout.setVisibility(View.VISIBLE); // Hiện phần kết quả
        scoreTextView.setText(String.format("Điểm: %.1f%%", scorePercent)); // Hiển thị điểm

        if (passed) {
            dismissButton.setVisibility(View.VISIBLE); // Hiện nút tắt
            retryButton.setVisibility(View.GONE);    // Ẩn nút thử lại
            dismissButton.setOnClickListener(v -> {
                Log.d(TAG, "Dismiss button clicked.");
                sendStopAlarmCommand(); // Gửi lệnh dừng service
                finish(); // Đóng màn hình quiz
            });
        } else {
            dismissButton.setVisibility(View.GONE);     // Ẩn nút tắt
            retryButton.setVisibility(View.VISIBLE);     // Hiện nút thử lại
            retryButton.setOnClickListener(v -> {
                Log.d(TAG, "Retry button clicked.");
                quizResultLayout.setVisibility(View.GONE); // Ẩn kết quả
                quizLayout.setVisibility(View.VISIBLE);    // Hiện lại quiz
                startNewQuizRound(); // Bắt đầu lượt mới
            });
        }
    }
    private void sendStopAlarmCommand() {
        Log.i(TAG,"Sending stop request to AlarmRingService.");
        Intent stopIntent = new Intent(this, AlarmRingService.class);
        stopIntent.setAction(AlarmRingService.ACTION_STOP_ALARM);
        startService(stopIntent);
    }

    // Được gọi khi hết giờ hoặc khi hết câu hỏi
    private void handleEndOfQuestion() {
        if (currentIndex >= questionsForThisRound.size()) {
            // Đã hết câu hỏi, đánh giá kết quả
            evaluateQuizAttempt();
        } else {
            // Vẫn còn câu hỏi, hiển thị câu tiếp theo
            displayCurrentQuestion();
        }
    }


    // --- Các hàm xử lý Timer, finishQuiz, onDestroy, initQuestionsFileIfNeeded, loadJSON, getFilteredQuestions giữ nguyên như phiên bản trước ---

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
    private void startTimer() {
        stopTimer();
        countDownTimer = new CountDownTimer(TIME_PER_QUESTION, 1000) {
            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerTextView.setText("00:" + String.format("%02d", seconds));
            }

            public void onFinish() {
                Log.d(TAG, "Timer finished for Q" + (currentIndex + 1));
                handleTimeout(); // Gọi hàm xử lý hết giờ
            }
        };
        countDownTimer.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    // Xử lý khi hết giờ cho một câu hỏi
    private void handleTimeout() {
        Toast.makeText(this, "Hết giờ!", Toast.LENGTH_SHORT).show();
        // Ghi nhận câu trả lời là sai (hoặc không trả lời)
        if (currentAttempt != null && currentIndex < questionsForThisRound.size()) {
            Question currentQuestion = questionsForThisRound.get(currentIndex);
            currentAttempt.addAnswer(currentQuestion, "[TIMEOUT]", false); // Ghi nhận là timeout và sai
            Log.d(TAG, "Timeout on Q" + (currentIndex+1));
            Log.d(TAG, "Current Score: " + currentAttempt.correctAnswers + "/" + currentAttempt.answeredQuestions.size());

        }
        // Chuyển sang câu tiếp theo hoặc đánh giá kết quả
        currentIndex++;
        handleEndOfQuestion();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    // initQuestionsFileIfNeeded giữ nguyên (đảm bảo có kiểm tra file.exists())
    private void initQuestionsFileIfNeeded() {
        File file = new File(getFilesDir(), "questions.json");
        if (!file.exists()) {
            Log.d("QuizInit", "questions.json not found. Creating initial file.");
            List<Question> initialQuestions = new ArrayList<>();
            // --- THÊM CÁC CÂU HỎI MẪU CỦA BẠN VÀO ĐÂY ---
            initialQuestions.add(new Question("Math", "Algebra", "Easy", "Solve for x: 2x + 3 = 7", Arrays.asList("x = 1", "x = 2", "x = 3", "x = 4"), "x = 2"));
            // ... (Thêm nhiều câu hỏi khác)
            initialQuestions.add(new Question("English", "Reading", "Nightmare", "In Samuel Beckett's 'Waiting for Godot', who or what is 'Godot' commonly interpreted to represent?", Arrays.asList("A political figure", "Death", "God or salvation", "There is no definitive interpretation"), "There is no definitive interpretation"));
            // --- KẾT THÚC PHẦN THÊM CÂU HỎI MẪU ---

            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(initialQuestions);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonString);
                Log.d("QuizInit", "questions.json created successfully.");
            } catch (IOException e) {
                Log.e("QuizInit", "Error writing initial questions.json", e);
            }
        } else {
            Log.d("QuizInit", "questions.json already exists.");
        }
    }

    // --- Các hàm đọc/ghi lịch sử Quiz ---

    // Hàm đọc lịch sử từ file JSON
    private List<QuizAttempt> loadQuizHistory() {
        File file = new File(getFilesDir(), QUIZ_HISTORY_FILENAME);
        if (!file.exists()) {
            return new ArrayList<>(); // Trả về list rỗng nếu file chưa tồn tại
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
            Log.e(TAG, "Error reading quiz history file", e);
            return new ArrayList<>(); // Trả về list rỗng nếu lỗi đọc
        }

        String jsonString = sb.toString();
        if (jsonString.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type historyListType = new TypeToken<ArrayList<QuizAttempt>>() {}.getType();
            Gson gson = new Gson();
            List<QuizAttempt> history = gson.fromJson(jsonString, historyListType);
            return (history != null) ? history : new ArrayList<>(); // Đảm bảo không trả về null
        } catch (Exception e) {
            Log.e(TAG, "Error parsing quiz history JSON", e);
            return new ArrayList<>(); // Trả về list rỗng nếu lỗi parse
        }
    }

    // Hàm lưu lượt chơi hiện tại vào lịch sử (ghi đè toàn bộ file)
    private void saveQuizAttemptToHistory(QuizAttempt attemptToSave) {
        if (attemptToSave == null) return;

        List<QuizAttempt> history = loadQuizHistory(); // Đọc lịch sử cũ
        history.add(attemptToSave); // Thêm lượt chơi mới vào cuối

        // Sắp xếp lịch sử theo thời gian bắt đầu (tùy chọn, mới nhất ở cuối)
        // Collections.sort(history, (a1, a2) -> Long.compare(a1.attemptTimestampStart, a2.attemptTimestampStart));

        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Dùng PrettyPrinting cho dễ đọc file
        String jsonString = gson.toJson(history);

        File file = new File(getFilesDir(), QUIZ_HISTORY_FILENAME);
        try (FileOutputStream fos = new FileOutputStream(file); // Không dùng MODE_PRIVATE vì đây là file mới
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(jsonString);
            Log.i(TAG, "Quiz history saved successfully to " + QUIZ_HISTORY_FILENAME);
        } catch (IOException e) {
            Log.e(TAG, "Error saving quiz history to file", e);
        }
    }
    // --- Kết thúc hàm đọc/ghi lịch sử Quiz ---
}