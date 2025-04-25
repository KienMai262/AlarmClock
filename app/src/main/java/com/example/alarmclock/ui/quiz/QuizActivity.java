package com.example.alarmclock.ui.quiz;

import android.annotation.SuppressLint;
import android.content.Context;
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
// Import WorkManager classes
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.alarmclock.R;
import com.example.alarmclock.alarm.AlarmActionWorker; // Import the worker
import com.example.alarmclock.alarm.AlarmRingService;
import com.example.alarmclock.alarm.Question;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale; // Import Locale for String.format

public class QuizActivity extends AppCompatActivity {

    private static final long TIME_PER_QUESTION = 15000; // 15 seconds per question
    private static final String TAG = "QuizActivity";
    private static final String QUIZ_HISTORY_FILENAME = "quiz_history.json";
    private static final double PASS_PERCENTAGE_THRESHOLD = 50.0; // Example: 50% to pass

    private TextView questionTextView, timerTextView;
    private Button[] answerButtons;
    private List<Question> questionsForThisRound;
    private int currentIndex = 0;
    private CountDownTimer countDownTimer;

    // Data passed from previous activity
    private int alarmId;
    private String subject, topic, difficulty;
    private int numQuestionsRequested;
    private boolean shouldPerformActionAfterQuiz; // Flag to indicate if alarm should be deleted/disabled

    private QuizAttempt currentAttempt; // Object to store results of the current quiz round

    // Layouts for switching views
    private LinearLayout quizLayout;
    private LinearLayout quizResultLayout;

    // Views in the result layout
    private TextView scoreTextView;
    private Button dismissButton;
    private Button retryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        // initQuestionsFileIfNeeded(); // Consider if this JSON init is still needed if using CSV

        // Get data from Intent passed by AlarmRingActivity
        Intent intent = getIntent();
        alarmId = intent.getIntExtra("alarmId", -1);
        subject = intent.getStringExtra("subject");
        topic = intent.getStringExtra("topic");
        difficulty = intent.getStringExtra("difficulty");
        numQuestionsRequested = intent.getIntExtra("numQuestions", 5); // Default 5 questions
        // !!! GET THE FLAG TO DETERMINE ACTION AFTER QUIZ !!!
        shouldPerformActionAfterQuiz = intent.getBooleanExtra("deleteAfterAlarm", false);

        Log.d(TAG, "Received data - AlarmId: " + alarmId + ", Subject: " + subject + ", Topic: " + topic +
                ", Difficulty: " + difficulty + ", NumQuestions: " + numQuestionsRequested +
                ", PerformActionAfterQuiz: " + shouldPerformActionAfterQuiz); // Log the flag

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

        // Check if essential data is present
        if (subject == null || topic == null || difficulty == null || numQuestionsRequested <= 0) {
            Log.e(TAG, "Missing essential quiz parameters in Intent. Cannot start quiz.");
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
            // If quiz cannot start, maybe stop the alarm service immediately?
            sendStopAlarmCommand();
            finish(); // Close this activity
            return;
        }

        // Start the first round of the quiz
        startNewQuizRound();
    }

    /**
     * Starts a new round of the quiz. Loads, filters, and shuffles questions.
     * Initializes the current quiz attempt object.
     */
    private void startNewQuizRound() {
        Log.d(TAG, "Starting new quiz round...");
        currentIndex = 0; // Reset question index

        // Get filtered and shuffled questions for this round from CSV
        questionsForThisRound = getFilteredQuestionsFromCSV(this, subject, topic, difficulty, numQuestionsRequested);

        if (questionsForThisRound != null && !questionsForThisRound.isEmpty()) {
            // Create a new attempt object. The actual number of questions might be less than requested.
            currentAttempt = new QuizAttempt(subject, topic, difficulty, questionsForThisRound.size());
            Log.d(TAG, "Starting quiz with " + questionsForThisRound.size() + " questions.");

            // Ensure quiz layout is visible and result layout is hidden
            quizLayout.setVisibility(View.VISIBLE);
            quizResultLayout.setVisibility(View.GONE);

            displayCurrentQuestion(); // Display the first question
        } else {
            Log.w(TAG, "No suitable questions found for the criteria. Cannot start quiz.");
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show(); // Use string resource
            // If no questions, stop the alarm and finish
            sendStopAlarmCommand();
            finish();
        }
    }

    /**
     * Displays the current question and sets up the answer buttons.
     * Handles invalid question data and moves to the next question if needed.
     * Starts the timer for the current question.
     */
    private void displayCurrentQuestion() {
        // Check if the quiz can proceed
        if (currentAttempt == null || questionsForThisRound == null || currentIndex >= questionsForThisRound.size()) {
            Log.d(TAG, "No more questions or invalid state. Evaluating attempt.");
            evaluateQuizAttempt(); // Evaluate the result if out of questions or in error state
            return;
        }

        Question q = questionsForThisRound.get(currentIndex);

        // Validate question data
        if (q == null || q.getContent() == null || q.getAnswers() == null || q.getAnswers().size() < 4 || q.getTrueAnswer() == null) {
            Log.e(TAG, "Invalid question data encountered at index: " + currentIndex + ". Skipping.");
            currentIndex++; // Skip this invalid question
            displayCurrentQuestion(); // Try displaying the next one
            return;
        }

        // Display the question content
        questionTextView.setText(q.getContent());

        // Get the correct answer for comparison
        final String currentTrueAnswer = q.getTrueAnswer();

        // Setup answer buttons
        List<String> answers = q.getAnswers();
        for (int i = 0; i < answerButtons.length; i++) {
            if (i < answers.size()) {
                final String answerText = answers.get(i);
                answerButtons[i].setText(answerText);
                answerButtons[i].setVisibility(View.VISIBLE);
                answerButtons[i].setEnabled(true); // Ensure buttons are clickable

                answerButtons[i].setOnClickListener(v -> {
                    stopTimer(); // Stop timer when an answer is clicked
                    boolean isCorrect = answerText.trim().equalsIgnoreCase(currentTrueAnswer.trim());

                    // Record the answer in the current attempt
                    currentAttempt.addAnswer(q, answerText, isCorrect);
                    Log.d(TAG, "Answered Q" + (currentIndex + 1) + ": User='" + answerText + "', Correct='" + currentTrueAnswer + "', Result=" + isCorrect);
                    Log.d(TAG, "Current Score: " + currentAttempt.correctAnswers + "/" + currentAttempt.answeredQuestions.size());

                    // Move to the next question
                    currentIndex++;
                    handleEndOfQuestion(); // Display next or evaluate
                });
            } else {
                // Hide unused buttons
                answerButtons[i].setVisibility(View.GONE);
            }
        }

        startTimer(); // Start the countdown for this question
    }


    /**
     * Evaluates the quiz attempt, displays the results, and handles pass/fail actions.
     */
    @SuppressLint("SetTextI18n") // For String.format with Locale
    private void evaluateQuizAttempt() {
        stopTimer(); // Ensure timer is stopped

        if (currentAttempt == null) {
            Log.e(TAG, "Cannot evaluate null attempt. Finishing activity.");
            // Maybe stop service just in case?
            sendStopAlarmCommand();
            finish();
            return;
        }

        // Calculate score and determine pass/fail
        double scorePercent = currentAttempt.getScorePercentage();
        boolean passed = scorePercent >= PASS_PERCENTAGE_THRESHOLD;
        currentAttempt.finishAttempt(passed); // Mark attempt as finished

        Log.i(TAG, "Quiz attempt finished. Score: " + currentAttempt.correctAnswers + "/" + currentAttempt.totalQuestionsInThisAttempt +
                " (" + String.format(Locale.US, "%.1f", scorePercent) + "%). Passed: " + passed); // Use Locale.US for decimal point

        // Save the completed attempt to history
        saveQuizAttemptToHistory(currentAttempt);

        // ---- DISPLAY RESULTS ----
        quizLayout.setVisibility(View.GONE);       // Hide quiz view
        quizResultLayout.setVisibility(View.VISIBLE); // Show result view
        String scoreLabel = getString(R.string.score); // Use string resource
        scoreTextView.setText(String.format(Locale.US, scoreLabel +": %.1f%%", scorePercent)); // Display score

        // Show appropriate buttons based on pass/fail
        if (passed) {
            dismissButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            dismissButton.setOnClickListener(v -> {
                Log.d(TAG, "Dismiss button clicked (Quiz Passed).");

                // !!! SCHEDULE ALARM ACTION (DELETE/DISABLE) IF NEEDED !!!
                if (alarmId != -1 && shouldPerformActionAfterQuiz) {
                    // Schedule the worker ONLY if the flag is true and ID is valid
                    Log.i(TAG, "Scheduling alarm action worker from QuizActivity for alarmId: " + alarmId);
                    // Pass 'true' for shouldDelete because this flag currently implies deletion/disabling after one ring
                    scheduleAlarmActionWork(alarmId, true);
                } else {
                    Log.d(TAG, "No background action needed or invalid alarmId ("+ alarmId + ") for worker scheduling.");
                }
                // --------------------------------------------------------

                sendStopAlarmCommand(); // Stop the alarm sound/vibration service
                finish();               // Close the QuizActivity
            });
        } else {
            // Failed the quiz
            dismissButton.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);
            retryButton.setOnClickListener(v -> {
                Log.d(TAG, "Retry button clicked.");
                // Hide result, show quiz, start a new round
                quizResultLayout.setVisibility(View.GONE);
                quizLayout.setVisibility(View.VISIBLE);
                startNewQuizRound(); // Start over
            });
        }
    }

    /**
     * Sends a command to AlarmRingService to stop the alarm sound and vibration.
     */
    private void sendStopAlarmCommand() {
        Log.i(TAG,"Sending stop request to AlarmRingService.");
        Intent stopIntent = new Intent(this, AlarmRingService.class);
        stopIntent.setAction(AlarmRingService.ACTION_STOP_ALARM);
        try {
            startService(stopIntent);
        } catch (Exception e) {
            // Catch potential exceptions if the service cannot be started (e.g., background restrictions)
            Log.e(TAG, "Error sending stop command to AlarmRingService", e);
            // Consider alternative ways to stop if service fails (though unlikely if it was just running)
        }
    }

    /**
     * Schedules the AlarmActionWorker to run in the background.
     * @param alarmIdToProcess The ID (index) of the alarm to process.
     * @param shouldDelete True if the alarm should be deleted, false if it should just be disabled.
     */
    private void scheduleAlarmActionWork(int alarmIdToProcess, boolean shouldDelete) {
        Log.d(TAG, "Enqueueing background action for alarmId: " + alarmIdToProcess + ", shouldDelete: " + shouldDelete);

        // 1. Create input data for the worker
        Data workerData = new Data.Builder()
                .putInt(AlarmActionWorker.KEY_ALARM_ID, alarmIdToProcess)
                .putBoolean(AlarmActionWorker.KEY_SHOULD_DELETE, shouldDelete)
                .build();

        // 2. Create a one-time work request
        OneTimeWorkRequest alarmActionWorkRequest =
                new OneTimeWorkRequest.Builder(AlarmActionWorker.class)
                        .setInputData(workerData)
                        .addTag("alarm_action_" + alarmIdToProcess) // Optional tag for tracking/cancelling
                        .build();

        // 3. Enqueue the request with WorkManager
        try {
            WorkManager.getInstance(getApplicationContext()).enqueue(alarmActionWorkRequest);
            Log.i(TAG, "Work request enqueued successfully from QuizActivity for alarmId: " + alarmIdToProcess);
        } catch (Exception e){
            Log.e(TAG,"Error enqueuing work request from QuizActivity for alarm " + alarmIdToProcess, e);
            // Optionally notify the user if scheduling fails critically
            // Toast.makeText(this, R.string.error_scheduling_alarm_action, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Called when the timer runs out or all questions are answered.
     * Determines whether to show the next question or evaluate the results.
     */
    private void handleEndOfQuestion() {
        if (currentIndex >= questionsForThisRound.size()) {
            // All questions answered
            Log.d(TAG, "Reached end of questions.");
            evaluateQuizAttempt();
        } else {
            // More questions remain
            Log.d(TAG, "Moving to next question: " + (currentIndex + 1));
            displayCurrentQuestion();
        }
    }

    /**
     * Loads questions from the CSV file in assets, filters them based on criteria,
     * shuffles, and returns the requested number of questions.
     *
     * @param context Context to access assets.
     * @param subject Desired subject.
     * @param topic Desired topic.
     * @param difficulty Desired difficulty.
     * @param numQuestions Max number of questions to return.
     * @return A list of filtered and shuffled questions, or an empty list if none found or error.
     */
    private List<Question> getFilteredQuestionsFromCSV(Context context, String subject, String topic, String difficulty, int numQuestions) {
        List<Question> allQuestions = loadQuestionsFromAssets(context);
        List<Question> filtered = new ArrayList<>();

        // Ensure criteria are not null before filtering
        if (subject == null || topic == null || difficulty == null) {
            Log.e(TAG, "Cannot filter with null criteria.");
            return filtered; // Return empty list
        }

        // Filter questions matching all criteria (case-insensitive)
        for (Question q : allQuestions) {
            // Add null checks for question fields as well for robustness
            if (q != null && q.getSubject() != null && q.getTopic() != null && q.getDifficulty() != null &&
                    q.getSubject().equalsIgnoreCase(subject) &&
                    q.getTopic().equalsIgnoreCase(topic) &&
                    q.getDifficulty().equalsIgnoreCase(difficulty)) {
                filtered.add(q);
            }
        }

        Log.d(TAG, "Found " + filtered.size() + " questions matching criteria before shuffling.");

        // Shuffle the filtered list
        Collections.shuffle(filtered);

        // Return the requested number of questions, or fewer if not enough were found
        int questionsToReturn = Math.min(filtered.size(), numQuestions);
        return new ArrayList<>(filtered.subList(0, questionsToReturn)); // Return a new list
    }

    /**
     * Loads all questions from the "questions.csv" file in the assets folder.
     *
     * @param context Context to access assets.
     * @return A list of all questions loaded from the CSV, or an empty list on error.
     */
    private List<Question> loadQuestionsFromAssets(Context context) {
        List<Question> questions = new ArrayList<>();
        String csvFileName = "questions.csv"; // Define filename

        try (InputStream is = context.getAssets().open(csvFileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            boolean isFirstLine = true; // Flag to skip header row

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip the first line (header)
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                // Split by comma, -1 limit ensures trailing empty fields are included
                String[] parts = line.split(",", -1);

                // Expecting 9 columns: Subject,Topic,Difficulty,Content,OptA,OptB,OptC,OptD,CorrectAnswer
                if (parts.length >= 9) {
                    try {
                        Question question = new Question();
                        question.setSubject(parts[0].trim());
                        question.setTopic(parts[1].trim());
                        question.setDifficulty(parts[2].trim());
                        question.setContent(parts[3].trim()); // Question text

                        // Answers/Options
                        List<String> options = new ArrayList<>();
                        options.add(parts[4].trim()); // Option A
                        options.add(parts[5].trim()); // Option B
                        options.add(parts[6].trim()); // Option C
                        options.add(parts[7].trim()); // Option D
                        question.setAnswers(options);

                        // Correct Answer
                        question.setTrueAnswer(parts[8].trim());

                        // Add the valid question to the list
                        questions.add(question);
                    } catch (Exception e) {
                        // Catch potential errors during object creation/setting for a specific line
                        Log.e(TAG, "Error processing CSV line: '" + line + "'", e);
                        // Continue to the next line
                    }
                } else {
                    Log.w(TAG, "Skipping CSV line due to insufficient columns: '" + line + "' (Expected >= 9, Got " + parts.length + ")");
                }
            }
            Log.d(TAG, "Successfully loaded " + questions.size() + " questions from " + csvFileName);
        } catch (IOException e) {
            Log.e(TAG, "Error loading CSV file '" + csvFileName + "' from assets", e);
            // Consider showing an error message to the user if loading fails critically
            // Toast.makeText(context, R.string.error_loading_questions_file, Toast.LENGTH_LONG).show();
        }

        return questions;
    }

    // --- Timer Methods ---

    private void startTimer() {
        stopTimer(); // Ensure any existing timer is cancelled
        countDownTimer = new CountDownTimer(TIME_PER_QUESTION, 1000) { // 1 second interval
            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                // Update timer display (e.g., "00:14")
                timerTextView.setText(String.format(Locale.getDefault(), "00:%02d", seconds));
            }

            public void onFinish() {
                Log.d(TAG, "Timer finished for Q" + (currentIndex + 1));
                handleTimeout(); // Handle timeout event
            }
        };
        countDownTimer.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null; // Set to null after cancelling
        }
    }

    /**
     * Handles the situation when the timer for a question runs out.
     * Records the answer as incorrect/timeout and moves to the next question or evaluates.
     */
    private void handleTimeout() {
        Toast.makeText(this, R.string.time_out, Toast.LENGTH_SHORT).show(); // Use string resource

        // Record the timeout as an incorrect answer
        if (currentAttempt != null && questionsForThisRound != null && currentIndex < questionsForThisRound.size()) {
            Question currentQuestion = questionsForThisRound.get(currentIndex);
            if (currentQuestion != null) { // Check if question object is valid
                currentAttempt.addAnswer(currentQuestion, "[TIMEOUT]", false); // Record timeout
                Log.d(TAG, "Timeout on Q" + (currentIndex + 1));
                Log.d(TAG, "Current Score after timeout: " + currentAttempt.correctAnswers + "/" + currentAttempt.answeredQuestions.size());
            } else {
                Log.w(TAG, "Current question was null during timeout handling.");
            }
        } else {
            Log.w(TAG, "Could not record timeout: Invalid state (attempt=" + (currentAttempt != null) +
                    ", questions=" + (questionsForThisRound != null) + ", index=" + currentIndex + ")");
        }

        // Move to the next question or evaluate the quiz
        currentIndex++;
        handleEndOfQuestion();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer(); // Make sure timer is stopped when activity is destroyed
        Log.d(TAG, "onDestroy");
    }

    // --- JSON Init (Consider removing if only using CSV) ---
    /**
     * Initializes a default questions.json file in the app's internal storage
     * if it doesn't already exist. Useful for initial setup or testing.
     * Consider removing if questions are solely loaded from assets CSV.
     */
    private void initQuestionsFileIfNeeded() {
        String jsonFileName = "questions.json";
        File file = new File(getFilesDir(), jsonFileName);
        if (!file.exists()) {
            Log.d("QuizInit", jsonFileName + " not found. Creating initial file.");
            List<Question> initialQuestions = new ArrayList<>();
            // Add sample questions (replace with your actual defaults or leave empty)
            initialQuestions.add(new Question("Math", "Algebra", "Easy", "Solve for x: 2x + 3 = 7", Arrays.asList("x = 1", "x = 2", "x = 3", "x = 4"), "x = 2"));
            initialQuestions.add(new Question("English", "Vocabulary", "Medium", "What is the synonym of 'ubiquitous'?", Arrays.asList("Rare", "Scarce", "Everywhere", "Hidden"), "Everywhere"));
            initialQuestions.add(new Question("Math", "Geometry", "Hard", "What is the volume of a sphere with radius 3?", Arrays.asList("9π", "18π", "27π", "36π"), "36π"));


            // Use Gson to convert the list to JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(initialQuestions);

            // Write the JSON string to the file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonString);
                Log.d("QuizInit", jsonFileName + " created successfully.");
            } catch (IOException e) {
                Log.e("QuizInit", "Error writing initial " + jsonFileName, e);
            }
        } else {
            Log.d("QuizInit", jsonFileName + " already exists.");
        }
    }

    // --- Quiz History Persistence ---

    /**
     * Loads the quiz attempt history from the internal storage file.
     * @return A list of previous QuizAttempts, or an empty list if the file doesn't exist or an error occurs.
     */
    private List<QuizAttempt> loadQuizHistory() {
        File file = new File(getFilesDir(), QUIZ_HISTORY_FILENAME);
        if (!file.exists()) {
            Log.d(TAG, QUIZ_HISTORY_FILENAME + " not found. Returning empty history.");
            return new ArrayList<>();
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
            Log.e(TAG, "Error reading quiz history file: " + QUIZ_HISTORY_FILENAME, e);
            return new ArrayList<>();
        }

        String jsonString = sb.toString();
        if (jsonString.isEmpty()) {
            Log.d(TAG, QUIZ_HISTORY_FILENAME + " is empty. Returning empty history.");
            return new ArrayList<>();
        }

        try {
            Type historyListType = new TypeToken<ArrayList<QuizAttempt>>() {}.getType();
            Gson gson = new Gson();
            List<QuizAttempt> history = gson.fromJson(jsonString, historyListType);
            return (history != null) ? history : new ArrayList<>(); // Ensure non-null return
        } catch (Exception e) { // Catch broader JsonSyntaxException etc.
            Log.e(TAG, "Error parsing quiz history JSON from " + QUIZ_HISTORY_FILENAME, e);
            return new ArrayList<>();
        }
    }

    /**
     * Saves the provided QuizAttempt to the history file.
     * Reads the existing history, adds the new attempt, and overwrites the file.
     * @param attemptToSave The QuizAttempt object to add to the history.
     */
    private void saveQuizAttemptToHistory(QuizAttempt attemptToSave) {
        if (attemptToSave == null) {
            Log.w(TAG, "Attempt to save a null QuizAttempt was ignored.");
            return;
        }

        List<QuizAttempt> history = loadQuizHistory(); // Load existing history
        history.add(attemptToSave); // Add the new attempt

        // Optional: Sort history (e.g., by timestamp descending) if desired
        // Collections.sort(history, (a1, a2) -> Long.compare(a2.attemptTimestampStart, a1.attemptTimestampStart));

        // Convert the updated history list to JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Pretty print for readability
        String jsonString = gson.toJson(history);

        // Write the JSON string back to the file, overwriting previous content
        File file = new File(getFilesDir(), QUIZ_HISTORY_FILENAME);
        try (FileOutputStream fos = new FileOutputStream(file); // Overwrites the file
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(jsonString);
            Log.i(TAG, "Quiz history saved successfully to " + QUIZ_HISTORY_FILENAME);
        } catch (IOException e) {
            Log.e(TAG, "Error saving quiz history to file: " + QUIZ_HISTORY_FILENAME, e);
            // Consider notifying user if saving history is critical
            // Toast.makeText(this, R.string.error_saving_quiz_history, Toast.LENGTH_SHORT).show();
        }
    }
    // --- End Quiz History Persistence ---
}