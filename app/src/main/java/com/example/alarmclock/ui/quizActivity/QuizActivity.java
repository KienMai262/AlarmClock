package com.example.alarmclock.ui.quizActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alarmclock.R;

public class QuizActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        Spinner topicSpinner = findViewById(R.id.topic_spinner);
        EditText questionCountInput = findViewById(R.id.question_count);
        Spinner difficultySpinner = findViewById(R.id.difficulty_spinner);

        // Ví dụ: Lấy danh sách chủ đề và mức độ khó từ tài nguyên
        ArrayAdapter<CharSequence> topicAdapter = ArrayAdapter.createFromResource(this,
                R.array.quiz_topics, android.R.layout.simple_spinner_item);
        topicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(topicAdapter);

        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(this,
                R.array.quiz_difficulties, android.R.layout.simple_spinner_item);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);

        // Thêm logic để tạo danh sách câu hỏi dựa trên lựa chọn
        findViewById(R.id.start_quiz_button).setOnClickListener(v -> {
            String selectedTopic = topicSpinner.getSelectedItem().toString();
            int questionCount = Integer.parseInt(questionCountInput.getText().toString());
            String selectedDifficulty = difficultySpinner.getSelectedItem().toString();

            // Lấy danh sách câu hỏi từ cơ sở dữ liệu hoặc danh sách
            List<Question> questions = getQuestions(selectedTopic, questionCount, selectedDifficulty);

            // Hiển thị câu hỏi
            displayQuestions(questions);
        });
    }

    // Hàm lấy danh sách câu hỏi
    private List<Question> getQuestions(String topic, int count, String difficulty) {
        // Lọc câu hỏi theo chủ đề, số lượng và mức độ khó
        // Đảm bảo không lặp lại câu hỏi đã sử dụng gần đây
        return questionRepository.getFilteredQuestions(topic, count, difficulty);
    }
}
