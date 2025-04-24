package com.example.alarmclock.ui.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclock.R;
import com.example.alarmclock.alarm.AlarmData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizSetupFragment extends Fragment {

    private final Map<String, List<String>> topicMap = new HashMap<String, List<String>>() {{
        put("English", Arrays.asList("Vocabulary", "Grammar", "Reading"));
        put("Math", Arrays.asList("Algebra", "Geometry", "Calculus"));
    }};
    private AlarmData currentEditingAlarmData;
    private int currentAlarmIndex;
    private Spinner spinnerSubject, spinnerTopic, spinnerDifficulty;
    private EditText editTextNumQuestions;
    private Button btnConfirm;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz_setup, container, false);

        Bundle args = getArguments();
        if (args != null) {
            currentEditingAlarmData = (AlarmData) args.getSerializable("alarmData");
            currentAlarmIndex = args.getInt("alarmIndex", -1);
        }

        spinnerSubject = view.findViewById(R.id.spinner_subject);
        spinnerTopic = view.findViewById(R.id.spinner_topic);
        spinnerDifficulty = view.findViewById(R.id.spinner_difficulty);
        editTextNumQuestions = view.findViewById(R.id.edit_text_number_questions);
        btnConfirm = view.findViewById(R.id.btn_confirm_quiz_setup);

        setupSubjectSpinner();
        setupDifficultySpinner();

        btnConfirm.setOnClickListener(v -> {
            String subject = spinnerSubject.getSelectedItem().toString();
            String topic = spinnerTopic.getSelectedItem().toString();
            String difficulty = spinnerDifficulty.getSelectedItem().toString();
            String numQuestionsStr = editTextNumQuestions.getText().toString().trim();

            if (numQuestionsStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter number of questions", Toast.LENGTH_SHORT).show();
                return;
            }

            int numQuestions = Integer.parseInt(numQuestionsStr);

            // Gắn các thông tin quiz vào AlarmData
            currentEditingAlarmData.subject = subject;
            currentEditingAlarmData.topic = topic;
            currentEditingAlarmData.difficulty = difficulty;
            currentEditingAlarmData.numQuestions = numQuestions;

            // Chuyển đến màn hình tiếp theo (ví dụ như QuizPreviewFragment)
            Bundle result = new Bundle();
            result.putSerializable("alarmData", currentEditingAlarmData);
            result.putInt("alarmIndex", currentAlarmIndex);

            getParentFragmentManager().setFragmentResult("quizSetupResult", result);

            // Quay về fragment trước (CreateAlarmFragment)
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();  // Quay lại màn trước

        });

        return view;
    }

    private void setupSubjectSpinner() {
        List<String> subjects = new ArrayList<>(topicMap.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);

        spinnerSubject.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedSubject = subjects.get(position);
                updateTopicSpinner(selectedSubject);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Mặc định chọn đầu tiên
        if (!subjects.isEmpty()) updateTopicSpinner(subjects.get(0));
    }

    private void updateTopicSpinner(String subject) {
        List<String> topics = topicMap.get(subject);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, topics);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTopic.setAdapter(adapter);
    }

    private void setupDifficultySpinner() {
        List<String> difficulties = Arrays.asList("Easy", "Medium", "Hard", "Nightmare");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, difficulties);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
    }

}

