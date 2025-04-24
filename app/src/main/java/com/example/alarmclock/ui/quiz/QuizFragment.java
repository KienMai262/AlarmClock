package com.example.alarmclock.ui.quiz;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.example.alarmclock.alarm.AlarmData;
import com.example.alarmclock.alarm.Question;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlarmData alarmData = (AlarmData) getArguments().getSerializable("alarmData");
        if (alarmData != null) {
            List<Question> allQuestions = loadQuestionsFromJson(requireContext());
            List<Question> filtered = filterQuestions(allQuestions, alarmData);

            // Lưu vào biến toàn cục để hiển thị cho người dùng quiz
        }
    }

    private List<Question> filterQuestions(List<Question> allQuestions, AlarmData data) {
        List<Question> filtered = new ArrayList<>();
        for (Question q : allQuestions) {
            if (q.getSubject().equals(data.subject)
                    && q.getTopic().equals(data.topic)
                    && q.getDifficulty().equals(data.difficulty)) {
                filtered.add(q);
            }
        }

        // Trộn và cắt số lượng câu hỏi đúng với `numQuestions`
        Collections.shuffle(filtered);
        return filtered.subList(0, Math.min(data.numQuestions, filtered.size()));
    }

    private List<Question> loadQuestionsFromJson(Context context) {
        List<Question> questionList = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Question>>(){}.getType();
            questionList = gson.fromJson(json, listType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return questionList;
    }

}
