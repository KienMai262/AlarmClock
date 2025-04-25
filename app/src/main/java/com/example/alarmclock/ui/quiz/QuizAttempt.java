package com.example.alarmclock.ui.quiz;

import com.example.alarmclock.alarm.Question;

import java.util.ArrayList;
import java.util.List;

public class QuizAttempt implements java.io.Serializable {
    public long attemptTimestampStart;
    public long attemptTimestampEnd;
    public String subject;
    public String topic;
    public String difficulty;
    public int totalQuestionsInThisAttempt; // Đổi tên cho rõ ràng
    public int correctAnswers;
    public boolean passed; // >= 50% là true
    public List<AnsweredQuestion> answeredQuestions;

    public QuizAttempt(String subject, String topic, String difficulty, int totalQuestions) {
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.totalQuestionsInThisAttempt = totalQuestions; // Số câu hỏi trong lượt này
        this.answeredQuestions = new ArrayList<>();
        this.attemptTimestampStart = System.currentTimeMillis();
        this.correctAnswers = 0; // Khởi tạo điểm
    }
    public QuizAttempt() {}

    public void addAnswer(Question question, String userAnswer, boolean wasCorrect) {
        this.answeredQuestions.add(new AnsweredQuestion(question, userAnswer, wasCorrect));
        if (wasCorrect) {
            this.correctAnswers++;
        }
    }

    public void finishAttempt(boolean passed) {
        this.passed = passed;
        this.attemptTimestampEnd = System.currentTimeMillis();
    }

    public double getScorePercentage() {
        if (totalQuestionsInThisAttempt == 0) return 0.0;
        return ((double) correctAnswers / totalQuestionsInThisAttempt) * 100.0;
    }
}
