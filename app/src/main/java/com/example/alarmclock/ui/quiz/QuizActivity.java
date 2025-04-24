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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final long TIME_PER_QUESTION = 10000; // 10 seconds
    private TextView questionTextView, timerTextView;
    private Button[] answerButtons;
    private List<Question> filteredQuestions;
    private int currentIndex = 0;
    private CountDownTimer countDownTimer;
    private int alarmId;

    public String subject, topic, difficulty;
    private int numQuestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        initQuestionsFileIfNeeded(); // Thực hiện nếu chưa có

        // Get data from Intent
        Intent intent = getIntent();
        alarmId = intent.getIntExtra("alarmId", -1);
        subject = intent.getStringExtra("subject");
        topic = intent.getStringExtra("topic");
        difficulty = intent.getStringExtra("difficulty");


        subject = "Math"; // For testing
        topic = "Algebra"; // For testing
        difficulty = "easy"; // For testing
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
        if(questions != null) {
            displayQuestions(questions);
            showNextQuestion();
        }
    }

    private String loadJSONFromSystemFile() {
        File file = new File(getFilesDir(), "questions.json");
        if (!file.exists()) {
            Log.e("QuizActivity", "questions.json not found in system folder");
            return null;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            Log.e("QuizActivity", "Error reading questions.json from system", e);
            return null;
        }
    }


    private List<Question> getFilteredQuestions(String subject, String topic, String difficulty, int numQuestions) {
        String jsonString = loadJSONFromSystemFile();
        List<Question> filteredQuestions = new ArrayList<>();

        if (jsonString != null) {
            Type questionListType = new TypeToken<List<Question>>() {
            }.getType();
            List<Question> allQuestions = new Gson().fromJson(jsonString, questionListType);

            for (Question question : allQuestions) {
                boolean matchesSubject = question.getSubject().equalsIgnoreCase(subject);
                boolean matchesTopic = question.getTopic().equalsIgnoreCase(topic);
                boolean matchesDifficulty = question.getDifficulty().equalsIgnoreCase(difficulty);

                if (matchesSubject && matchesTopic && matchesDifficulty) {
                    filteredQuestions.add(question);
                }
            }

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
            answerButtons[i].setText(q.answers.get(i));
            String selected = q.answers.get(i);
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
            answerButtons[i].setText(q.answers.get(i));  // Gán đáp án vào các nút
            String selected = q.answers.get(i); // Lưu đáp án để so sánh khi người dùng chọn

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

    private void initQuestionsFileIfNeeded() {
        File file = new File(getFilesDir(), "questions.json");

        if (!file.exists()) {
            // Dữ liệu mẫu ban đầu
            List<Question> initialQuestions = new ArrayList<>();

// MATH - ALGEBRA
            initialQuestions.add(new Question("Math", "Algebra", "Easy", "Solve for x: 2x + 3 = 7",
                    Arrays.asList("x = 1", "x = 2", "x = 3", "x = 4"), "x = 2"));

            initialQuestions.add(new Question("Math", "Algebra", "Easy", "Simplify: 3(x + 2) - 2(x - 1)",
                    Arrays.asList("x + 8", "x + 4", "3x + 4", "x + 6"), "x + 8"));

            initialQuestions.add(new Question("Math", "Algebra", "Easy", "Factor: x² - 9",
                    Arrays.asList("(x - 3)(x + 3)", "(x - 9)(x + 1)", "(x - 4.5)(x - 4.5)", "(x - 9)(x - 1)"), "(x - 3)(x + 3)"));

            initialQuestions.add(new Question("Math", "Algebra", "Medium", "Solve the system of equations: \n2x + y = 5 \nx - y = 1",
                    Arrays.asList("x = 2, y = 1", "x = 1, y = 3", "x = 3, y = -1", "x = 2, y = -1"), "x = 2, y = 1"));

            initialQuestions.add(new Question("Math", "Algebra", "Medium", "Simplify: (2x³y²)²(3xy⁴)³",
                    Arrays.asList("72x⁹y¹⁶", "108x⁹y¹⁴", "36x⁹y¹⁴", "108x⁹y¹⁶"), "108x⁹y¹⁶"));

            initialQuestions.add(new Question("Math", "Algebra", "Medium", "Solve for x: |2x - 6| = 8",
                    Arrays.asList("x = -1 or x = 7", "x = 1 or x = 7", "x = -1 or x = 5", "x = 1 or x = 5"), "x = -1 or x = 7"));

            initialQuestions.add(new Question("Math", "Algebra", "Hard", "Find all solutions to: 2x³ - 3x² - 12x + 8 = 0",
                    Arrays.asList("x = -2, x = 1, x = 2", "x = -2, x = 2, x = 4", "x = -4, x = 1, x = 2", "x = -2, x = -1, x = 2"), "x = -2, x = 1, x = 2"));

            initialQuestions.add(new Question("Math", "Algebra", "Hard", "If f(x) = 2x² - 3x + 1 and g(x) = x - 2, find (f∘g)(x)",
                    Arrays.asList("2x² - 11x + 13", "2x² - 7x + 5", "2x² - 7x + 9", "2x² - 11x + 9"), "2x² - 11x + 13"));

            initialQuestions.add(new Question("Math", "Algebra", "Nightmare", "Solve the equation for real values of x: x^x = 1000",
                    Arrays.asList("x ≈ 3.791", "x ≈ 4.655", "x ≈ 5.912", "x ≈ 6.064"), "x ≈ 4.655"));

            initialQuestions.add(new Question("Math", "Algebra", "Nightmare", "Find all solutions to the equation: log₂(x²) - 2log₂(x) = 3",
                    Arrays.asList("x = -4, x = 4", "x = -4 only", "x = 4 only", "x = -8, x = 8"), "x = -4, x = 4"));

// MATH - GEOMETRY
            initialQuestions.add(new Question("Math", "Geometry", "Easy", "Find the area of a circle with radius 5 cm.",
                    Arrays.asList("25π cm²", "10π cm²", "25 cm²", "5π cm²"), "25π cm²"));

            initialQuestions.add(new Question("Math", "Geometry", "Easy", "What is the sum of angles in a triangle?",
                    Arrays.asList("90°", "180°", "270°", "360°"), "180°"));

            initialQuestions.add(new Question("Math", "Geometry", "Easy", "Find the perimeter of a square with side length 6 units.",
                    Arrays.asList("36 units", "24 units", "12 units", "18 units"), "24 units"));

            initialQuestions.add(new Question("Math", "Geometry", "Medium", "In a right triangle, if one leg is 4 and the hypotenuse is 5, what is the length of the other leg?",
                    Arrays.asList("3", "√11", "√21", "√9"), "3"));

            initialQuestions.add(new Question("Math", "Geometry", "Medium", "Find the volume of a cone with radius 4 cm and height 9 cm.",
                    Arrays.asList("36π cm³", "48π cm³", "144π cm³", "16π cm³"), "48π cm³"));

            initialQuestions.add(new Question("Math", "Geometry", "Medium", "If the diagonals of a rhombus are 10 cm and 24 cm, what is its area?",
                    Arrays.asList("120 cm²", "240 cm²", "60 cm²", "130 cm²"), "120 cm²"));

            initialQuestions.add(new Question("Math", "Geometry", "Hard", "In a triangle ABC, if a = 8, b = 10, c = 12, what is the measure of angle C?",
                    Arrays.asList("60°", "90°", "120°", "45°"), "90°"));

            initialQuestions.add(new Question("Math", "Geometry", "Hard", "A cylinder with radius 3 cm and height h cm has the same volume as a sphere with radius 4 cm. Find h.",
                    Arrays.asList("8 cm", "16/3 cm", "32/3 cm", "16 cm"), "32/3 cm"));

            initialQuestions.add(new Question("Math", "Geometry", "Nightmare", "Find the area of the region bounded by the curves y = sin(x) and y = cos(x) from x = 0 to x = π/4.",
                    Arrays.asList("√2 - 1", "2 - √2", "2", "√2"), "2 - √2"));

            initialQuestions.add(new Question("Math", "Geometry", "Nightmare", "A pyramid has a square base with side length 10 units. If all edges from the apex to the base vertices have length 13 units, find the volume of the pyramid.",
                    Arrays.asList("200 units³", "400/3 units³", "100√6 units³", "120 units³"), "400/3 units³"));

// MATH - CALCULUS
            initialQuestions.add(new Question("Math", "Calculus", "Easy", "Find the derivative of f(x) = 3x² + 2x - 5",
                    Arrays.asList("f'(x) = 6x + 2", "f'(x) = 3x + 2", "f'(x) = 6x - 5", "f'(x) = 6x - 2"), "f'(x) = 6x + 2"));

            initialQuestions.add(new Question("Math", "Calculus", "Easy", "Evaluate: ∫(2x + 3)dx",
                    Arrays.asList("x² + 3x + C", "2x² + 3x + C", "x² + 3 + C", "2x + 3x + C"), "x² + 3x + C"));

            initialQuestions.add(new Question("Math", "Calculus", "Easy", "Find the derivative of f(x) = e^x",
                    Arrays.asList("f'(x) = e^x", "f'(x) = xe^(x-1)", "f'(x) = e^(x-1)", "f'(x) = xe^x"), "f'(x) = e^x"));

            initialQuestions.add(new Question("Math", "Calculus", "Medium", "Find the derivative of f(x) = ln(x²+1)",
                    Arrays.asList("f'(x) = 2x/(x²+1)", "f'(x) = 1/(x²+1)", "f'(x) = x/(x²+1)", "f'(x) = 2/(x²+1)"), "f'(x) = 2x/(x²+1)"));

            initialQuestions.add(new Question("Math", "Calculus", "Medium", "Evaluate: ∫₀^1 x·e^x dx",
                    Arrays.asList("e - 1", "1", "e", "e - 2"), "e - 1"));

            initialQuestions.add(new Question("Math", "Calculus", "Medium", "Find the local extrema of f(x) = x³ - 6x² + 9x + 1",
                    Arrays.asList("Local maximum at x = 1, local minimum at x = 3", "Local minimum at x = 1, local maximum at x = 3", "Local maximum at x = 1 only", "Local minimum at x = 3 only"), "Local maximum at x = 1, local minimum at x = 3"));

            initialQuestions.add(new Question("Math", "Calculus", "Hard", "Evaluate: lim(x→0) (sin(3x)/x)",
                    Arrays.asList("0", "1", "3", "∞"), "3"));

            initialQuestions.add(new Question("Math", "Calculus", "Hard", "Find the volume of the solid obtained by rotating the region bounded by y = x² and y = 4 about the y-axis.",
                    Arrays.asList("4π", "8π", "16π/3", "8π/3"), "8π"));

            initialQuestions.add(new Question("Math", "Calculus", "Nightmare", "Evaluate: ∫₀^∞ x²e^(-x) dx",
                    Arrays.asList("1", "2", "1/2", "2!"), "2"));

            initialQuestions.add(new Question("Math", "Calculus", "Nightmare", "Find the sum of the series: Σ(n=1 to ∞) n/(2^n)",
                    Arrays.asList("1", "2", "4", "∞"), "2"));

// ENGLISH - GRAMMAR
            initialQuestions.add(new Question("English", "Grammar", "Easy", "Which sentence has the correct use of apostrophes?",
                    Arrays.asList("The dog's are playing in the yard.", "The dogs' are playing in the yard.", "The dogs are playing in the yard's.", "The dogs' toys are in the yard."), "The dogs' toys are in the yard."));

            initialQuestions.add(new Question("English", "Grammar", "Easy", "Choose the correct form of the verb: They ___ to the store yesterday.",
                    Arrays.asList("go", "goes", "went", "going"), "went"));

            initialQuestions.add(new Question("English", "Grammar", "Easy", "Identify the pronoun in this sentence: 'She gave him her book.'",
                    Arrays.asList("gave", "book", "She, him, her", "She"), "She, him, her"));

            initialQuestions.add(new Question("English", "Grammar", "Medium", "Choose the sentence with correct subject-verb agreement:",
                    Arrays.asList("The team are playing well.", "The team is playing well.", "The team were playing well.", "The team have playing well."), "The team is playing well."));

            initialQuestions.add(new Question("English", "Grammar", "Medium", "Identify the correct sentence:",
                    Arrays.asList("Between you and I, the secret is safe.", "Between you and me, the secret is safe.", "Between you and myself, the secret is safe.", "Between you and mines, the secret is safe."), "Between you and me, the secret is safe."));

            initialQuestions.add(new Question("English", "Grammar", "Medium", "Choose the sentence with the correct use of the semicolon:",
                    Arrays.asList("I love cooking; and baking.", "I love cooking, baking is my passion.", "I love cooking; baking is my passion.", "I love cooking; but baking is my passion."), "I love cooking; baking is my passion."));

            initialQuestions.add(new Question("English", "Grammar", "Hard", "Identify the sentence with the correct use of the subjunctive mood:",
                    Arrays.asList("I wish I was taller.", "I wish I were taller.", "I wish I am taller.", "I wish I be taller."), "I wish I were taller."));

            initialQuestions.add(new Question("English", "Grammar", "Hard", "Which sentence contains a split infinitive?",
                    Arrays.asList("She decided to quickly run to the store.", "She quickly decided to run to the store.", "She decided quickly to run to the store.", "She decided to run quickly to the store."), "She decided to quickly run to the store."));

            initialQuestions.add(new Question("English", "Grammar", "Nightmare", "Choose the correct sentence with a properly placed participial phrase:",
                    Arrays.asList("Walking down the street, the flowers looked beautiful.", "Walking down the street, I saw beautiful flowers.", "The flowers, walking down the street, looked beautiful.", "I saw beautiful flowers walking down the street."), "Walking down the street, I saw beautiful flowers."));

            initialQuestions.add(new Question("English", "Grammar", "Nightmare", "Identify the correct usage of the past perfect subjunctive:",
                    Arrays.asList("If I had known, I will have helped.", "If I had known, I would help.", "If I had known, I had helped.", "If I had known, I would have helped."), "If I had known, I would have helped."));

// ENGLISH - VOCABULARY
            initialQuestions.add(new Question("English", "Vocabulary", "Easy", "What is the meaning of 'benevolent'?",
                    Arrays.asList("Cruel", "Kind", "Angry", "Fearful"), "Kind"));

            initialQuestions.add(new Question("English", "Vocabulary", "Easy", "Choose the synonym for 'happy':",
                    Arrays.asList("Sad", "Angry", "Joyful", "Tired"), "Joyful"));

            initialQuestions.add(new Question("English", "Vocabulary", "Easy", "What is the antonym of 'beautiful'?",
                    Arrays.asList("Pretty", "Ugly", "Handsome", "Attractive"), "Ugly"));

            initialQuestions.add(new Question("English", "Vocabulary", "Medium", "What is the meaning of 'ubiquitous'?",
                    Arrays.asList("Rare", "Found everywhere", "Unique", "Strange"), "Found everywhere"));

            initialQuestions.add(new Question("English", "Vocabulary", "Medium", "Choose the word that best completes the sentence: The detective found a ____ of evidence at the crime scene.",
                    Arrays.asList("plethora", "scarcity", "balance", "mixture"), "plethora"));

            initialQuestions.add(new Question("English", "Vocabulary", "Medium", "Which word is closest in meaning to 'loquacious'?",
                    Arrays.asList("Silent", "Talkative", "Intelligent", "Foolish"), "Talkative"));

            initialQuestions.add(new Question("English", "Vocabulary", "Hard", "What does the word 'obfuscate' mean?",
                    Arrays.asList("To clarify", "To confuse deliberately", "To congratulate", "To observe carefully"), "To confuse deliberately"));

            initialQuestions.add(new Question("English", "Vocabulary", "Hard", "Choose the correct definition of 'ephemeral':",
                    Arrays.asList("Lasting forever", "Extremely important", "Lasting for a very short time", "Extremely large"), "Lasting for a very short time"));

            initialQuestions.add(new Question("English", "Vocabulary", "Nightmare", "What is the definition of 'syzygy'?",
                    Arrays.asList("A literary device involving similar sounds", "A configuration of celestial bodies in a straight line", "A type of ancient musical instrument", "A branch of mathematics dealing with symmetry"), "A configuration of celestial bodies in a straight line"));

            initialQuestions.add(new Question("English", "Vocabulary", "Nightmare", "Which word best describes 'the quality of being insincere or deceitful in expressing beliefs'?",
                    Arrays.asList("Candor", "Rectitude", "Mendacity", "Duplicity"), "Duplicity"));

// ENGLISH - COMPREHENSION
            initialQuestions.add(new Question("English", "Reading", "Easy", "Read the passage: 'The cat sat on the mat. It was tired after playing all day.' What was tired?",
                    Arrays.asList("The mat", "The cat", "The day", "The passage"), "The cat"));

            initialQuestions.add(new Question("English", "Reading", "Easy", "In the sentence 'Mary likes apples, but John prefers oranges', what is the relationship between the two clauses?",
                    Arrays.asList("Causal", "Contrast", "Sequential", "Additive"), "Contrast"));

            initialQuestions.add(new Question("English", "Reading", "Easy", "What is the main idea of this passage: 'Regular exercise can improve your health, boost your mood, and increase your energy levels.'?",
                    Arrays.asList("Exercise makes you tired", "Exercise has multiple benefits", "Exercise is difficult", "Exercise is only for mood improvement"), "Exercise has multiple benefits"));

            initialQuestions.add(new Question("English", "Reading", "Medium", "Read the passage: 'Although technological innovations have improved our lives in many ways, they have also created new problems including privacy concerns and digital addiction.' What is the author's stance on technology?",
                    Arrays.asList("Completely positive", "Completely negative", "Balanced view of benefits and drawbacks", "Neutral with no opinion"), "Balanced view of benefits and drawbacks"));

            initialQuestions.add(new Question("English", "Reading", "Medium", "What can be inferred from this passage: 'The theater was nearly empty when the movie started, but by the time it ended, there wasn't an empty seat in the house.'?",
                    Arrays.asList("People left during the movie", "People arrived late to the movie", "The movie was unpopular", "The seats were uncomfortable"), "People arrived late to the movie"));

            initialQuestions.add(new Question("English", "Reading", "Medium", "Identify the tone of this passage: 'Once again, the government has failed to address the real issues affecting citizens, focusing instead on trivial matters that serve only to distract.'",
                    Arrays.asList("Optimistic", "Critical", "Humorous", "Neutral"), "Critical"));

            initialQuestions.add(new Question("English", "Reading", "Hard", "Read the passage: 'The quantum nature of reality, though counterintuitive to our everyday experience, has been consistently verified through experimentation. Yet, philosophers continue to debate the implications of these findings for our understanding of consciousness and free will.' What is the main tension described in this passage?",
                    Arrays.asList("Between scientists and philosophers", "Between quantum mechanics and classical physics", "Between scientific findings and philosophical implications", "Between consciousness and free will"), "Between scientific findings and philosophical implications"));

            initialQuestions.add(new Question("English", "Reading", "Hard", "What literary device is used in this sentence: 'The wind whispered through the trees'?",
                    Arrays.asList("Simile", "Metaphor", "Personification", "Hyperbole"), "Personification"));

            initialQuestions.add(new Question("English", "Reading", "Nightmare", "Read the passage: 'The paradox of tolerance states that if a society is tolerant without limit, its ability to be tolerant is eventually seized or destroyed by the intolerant. Karl Popper argued that in order to maintain a tolerant society, the society must be intolerant of intolerance.' What is the central philosophical problem addressed here?",
                    Arrays.asList("Whether tolerance is valuable", "How much intolerance should be tolerated", "Whether Karl Popper was correct", "How to define tolerance"), "How much intolerance should be tolerated"));

            initialQuestions.add(new Question("English", "Reading", "Nightmare", "Analyze this excerpt from T.S. Eliot's 'The Waste Land': 'April is the cruellest month, breeding / Lilacs out of the dead land, mixing / Memory and desire, stirring / Dull roots with spring rain.' Why might April be described as 'cruel'?",
                    Arrays.asList("Because spring weather is unpleasant", "Because it prolongs winter", "Because it awakens painful emotions and memories", "Because plants find growth painful"), "Because it awakens painful emotions and memories"));

// ENGLISH - LITERATURE
            initialQuestions.add(new Question("English", "Reading", "Easy", "Who wrote 'Romeo and Juliet'?",
                    Arrays.asList("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"), "William Shakespeare"));

            initialQuestions.add(new Question("English", "Reading", "Easy", "What genre is 'The Lord of the Rings'?",
                    Arrays.asList("Science Fiction", "Fantasy", "Historical Fiction", "Mystery"), "Fantasy"));

            initialQuestions.add(new Question("English", "Reading", "Easy", "Which of these is NOT one of the March sisters in 'Little Women'?",
                    Arrays.asList("Jo", "Beth", "Meg", "Susan"), "Susan"));

            initialQuestions.add(new Question("English", "Reading", "Medium", "Who authored '1984'?",
                    Arrays.asList("George Orwell", "Aldous Huxley", "Ray Bradbury", "Philip K. Dick"), "George Orwell"));

            initialQuestions.add(new Question("English", "Reading", "Medium", "What is the setting of 'The Great Gatsby'?",
                    Arrays.asList("1950s California", "1920s New York", "1930s Chicago", "1910s London"), "1920s New York"));

            initialQuestions.add(new Question("English", "Reading", "Medium", "Who is the protagonist in 'To Kill a Mockingbird'?",
                    Arrays.asList("Atticus Finch", "Scout Finch", "Jem Finch", "Boo Radley"), "Scout Finch"));

            initialQuestions.add(new Question("English", "Reading", "Hard", "Which literary movement did James Joyce belong to?",
                    Arrays.asList("Romanticism", "Realism", "Modernism", "Post-modernism"), "Modernism"));

            initialQuestions.add(new Question("English", "Reading", "Hard", "What is the significance of the green light in 'The Great Gatsby'?",
                    Arrays.asList("It represents jealousy", "It symbolizes Gatsby's wealth", "It represents Daisy's house and Gatsby's hopes", "It symbolizes the American Dream"), "It represents Daisy's house and Gatsby's hopes"));

            initialQuestions.add(new Question("English", "Reading", "Nightmare", "Which of these works exemplifies the literary technique known as 'stream of consciousness'?",
                    Arrays.asList("'Pride and Prejudice' by Jane Austen", "'Ulysses' by James Joyce", "'Moby-Dick' by Herman Melville", "'Oliver Twist' by Charles Dickens"), "'Ulysses' by James Joyce"));

            initialQuestions.add(new Question("English", "Reading", "Nightmare", "In Samuel Beckett's 'Waiting for Godot', who or what is 'Godot' commonly interpreted to represent?",
                    Arrays.asList("A political figure", "Death", "God or salvation", "There is no definitive interpretation"), "There is no definitive interpretation"));

            // Ghi vào file
            String jsonString = new Gson().toJson(initialQuestions);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonString);
                Log.d("QuizInit", "questions.json created in system directory");
            } catch (IOException e) {
                Log.e("QuizInit", "Error writing questions.json", e);
            }
        } else {
            Log.d("QuizInit", "questions.json already exists");
        }
    }


}


