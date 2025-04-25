package com.example.alarmclock.ui.quiz;

import android.os.Bundle;
import android.util.Log;
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

    private final List<String> subjectKeys = Arrays.asList("English", "Math");
    private final Map<String, List<String>> topicKeysMap = new HashMap<>();
    private final List<String> difficultyKeys = Arrays.asList("Easy", "Medium", "Hard", "Nightmare");

    private final Map<String, Integer> keyToDisplayResIdMap = new HashMap<>();

    private AlarmData currentEditingAlarmData;
    private int currentAlarmIndex;
    private Spinner spinnerSubject, spinnerTopic, spinnerDifficulty;
    private EditText editTextNumQuestions;
    private Button btnConfirm;
    private Button btnBack;

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
        btnBack = view.findViewById(R.id.btn_back);


        btnBack.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
        });

        setupKeysAndMappings();
        setupSubjectSpinner();
        setupDifficultySpinner();


        btnConfirm.setOnClickListener(v -> {
            int subjectPosition = spinnerSubject.getSelectedItemPosition();
            int topicPosition = spinnerTopic.getSelectedItemPosition();
            int difficultyPosition = spinnerDifficulty.getSelectedItemPosition();

            String subjectKey = subjectKeys.get(subjectPosition);
            List<String> currentTopicKeys = topicKeysMap.get(subjectKey);
            if (currentTopicKeys == null) currentTopicKeys = new ArrayList<>(); // Đảm bảo không null
            String topicKey = (topicPosition >= 0 && topicPosition < currentTopicKeys.size()) ? currentTopicKeys.get(topicPosition) : ""; // Lấy khóa topic

            String difficultyKey = difficultyKeys.get(difficultyPosition);

            String numQuestionsStr = editTextNumQuestions.getText().toString().trim();

            if (numQuestionsStr.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.error_empty_questions), Toast.LENGTH_SHORT).show();
                return;
            }

            int numQuestions = Integer.parseInt(numQuestionsStr); // Hoặc xử lý lỗi nếu cần

            currentEditingAlarmData.subject = subjectKey;
            currentEditingAlarmData.topic = topicKey;
            currentEditingAlarmData.difficulty = difficultyKey;
            currentEditingAlarmData.numQuestions = numQuestions;

            Bundle result = new Bundle();
            result.putSerializable("alarmData", currentEditingAlarmData);
            result.putInt("alarmIndex", currentAlarmIndex);

            getParentFragmentManager().setFragmentResult("quizSetupResult", result);

            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();  // Quay lại màn trước

        });

        return view;
    }

    private void setupKeysAndMappings() {
        keyToDisplayResIdMap.put("English", R.string.subject_english);
        keyToDisplayResIdMap.put("Math", R.string.subject_math);

        keyToDisplayResIdMap.put("Vocabulary", R.string.topic_vocabulary);
        keyToDisplayResIdMap.put("Grammar", R.string.topic_grammar);
        keyToDisplayResIdMap.put("Reading", R.string.topic_reading);

        keyToDisplayResIdMap.put("Algebra", R.string.topic_algebra);
        keyToDisplayResIdMap.put("Geometry", R.string.topic_geometry);
        keyToDisplayResIdMap.put("Calculus", R.string.topic_calculus);

        keyToDisplayResIdMap.put("Easy", R.string.difficulty_easy);
        keyToDisplayResIdMap.put("Medium", R.string.difficulty_medium);
        keyToDisplayResIdMap.put("Hard", R.string.difficulty_hard);
        keyToDisplayResIdMap.put("Nightmare", R.string.difficulty_nightmare);

        topicKeysMap.put("English", Arrays.asList("Vocabulary", "Grammar", "Reading"));
        topicKeysMap.put("Math", Arrays.asList("Algebra", "Geometry", "Calculus"));
    }

    private void setupSubjectSpinner() {
        List<String> displaySubjects = new ArrayList<>();
        if (subjectKeys != null && keyToDisplayResIdMap != null) {
            for (String key : subjectKeys) {
                Integer resId = keyToDisplayResIdMap.get(key); // Lấy giá trị kiểu Integer

                if (resId != null) { // *** KIỂM TRA NULL ***
                    displaySubjects.add(getString(resId)); // Chỉ gọi getString khi resId không null
                } else {
                    Log.w("QuizSetup", "Không tìm thấy Resource ID cho subject key: " + key);
                    displaySubjects.add(key + " (Lỗi)"); // Hoặc chỉ hiển thị key
                }
            }
        } else {
            Log.e("QuizSetup", "Lỗi: subjectKeys hoặc keyToDisplayResIdMap bị null!");
        }

        // --- Phần còn lại của việc tạo và set Adapter ---
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, displaySubjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);

        // --- Listener ---
        spinnerSubject.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // Cẩn thận: Nếu displaySubjects có chứa chuỗi lỗi, việc lấy key bằng position có thể sai
                // Đảm bảo logic lấy key bằng position vẫn đúng hoặc điều chỉnh lại
                if (position >= 0 && position < subjectKeys.size()){ // Kiểm tra giới hạn an toàn
                    String selectedSubjectKey = subjectKeys.get(position);
                    updateTopicSpinner(selectedSubjectKey);
                } else {
                    Log.e("QuizSetup", "Vị trí không hợp lệ trong subject spinner: " + position);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // Mặc định chọn đầu tiên
        if (!subjectKeys.isEmpty() && !displaySubjects.isEmpty()) { // Kiểm tra cả subjectKeys không rỗng
            updateTopicSpinner(subjectKeys.get(0));
        }
    }

    private void updateTopicSpinner(String subjectKey) { // Nhận khóa tiếng Anh
        List<String> currentTopicKeys = topicKeysMap.get(subjectKey);
        if (currentTopicKeys == null) currentTopicKeys = new ArrayList<>(); // Đề phòng lỗi

        // Tạo danh sách chuỗi hiển thị topic theo ngôn ngữ
        List<String> displayTopics = new ArrayList<>();
        for (String key : currentTopicKeys) {
            displayTopics.add(getString(keyToDisplayResIdMap.get(key)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, displayTopics);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTopic.setAdapter(adapter);
    }

    private void setupDifficultySpinner() {
        List<String> displayDifficulties = new ArrayList<>();
        for (String key : difficultyKeys) {
            Integer displayStringResId = keyToDisplayResIdMap.get(key);if (displayStringResId != null) {
                displayDifficulties.add(getString(displayStringResId));
            } else {
                displayDifficulties.add(key);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                displayDifficulties
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerDifficulty.setAdapter(adapter);
    }

}

