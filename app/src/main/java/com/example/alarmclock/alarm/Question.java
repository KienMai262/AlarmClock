package com.example.alarmclock.alarm;

import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    public String subject;
    public String topic;
    public String difficulty;

    public String content;
    public List<String> answers;

    public Question() {
    }

    public Question(String subject, String topic, String difficulty, String content, List<String> answers, String trueAnswer) {
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.content = content;
        this.answers = answers;
        this.trueAnswer = trueAnswer;
    }

    public Question(String subject) {
        this.subject = subject;
    }

    public String trueAnswer;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public String getTrueAnswer() {
        return trueAnswer;
    }

    public void setTrueAnswer(String trueAnswer) {
        this.trueAnswer = trueAnswer;
    }

}

