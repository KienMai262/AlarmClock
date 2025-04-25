package com.example.alarmclock.ui.quiz;

import com.example.alarmclock.alarm.Question;

import java.util.ArrayList;
import java.util.List;

public class AnsweredQuestion implements java.io.Serializable { // Implement Serializable để lưu vào JSON dễ dàng
    public String questionContent;
    public List<String> answers;
    public String trueAnswer;
    public String userAnswer;
    public boolean wasCorrect;
    public long timestampAnswered;

    public AnsweredQuestion(Question question, String userAnswer, boolean wasCorrect) {
        this.questionContent = question.content;
        this.answers = new ArrayList<>(question.answers);
        this.trueAnswer = question.trueAnswer;
        this.userAnswer = userAnswer;
        this.wasCorrect = wasCorrect;
        this.timestampAnswered = System.currentTimeMillis();
    }
    public AnsweredQuestion() {}
}
