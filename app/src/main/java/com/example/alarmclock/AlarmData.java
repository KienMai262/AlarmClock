package com.example.alarmclock;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Date;
import java.util.Timer;

public class AlarmData {
    public Pair<String, String>[] items = new Pair[] {
            new Pair<>("It's going to be a good day", "@raw/it_s_going_to_be_a_good_day"),
            new Pair<>("See you again meow", "@raw/see_you_again_meow"),
            new Pair<>("Squeaky computer chair", "@raw/squeaky_computer_chair")
    };
    public Pair<String, Boolean>[] loopOption = new Pair[] {
            new Pair<>("One time", false),
            new Pair<>("Daily", false),
            new Pair<>("Monday to Saturday", false),
            new Pair<>("Custom", false)
    };
    public Pair<String, Boolean>[] optionOther = new Pair[] {
            new Pair<>("Monday", false),
            new Pair<>("Tuesday", false),
            new Pair<>("Wednesday", false),
            new Pair<>("Thursday", false),
            new Pair<>("Friday", false),
            new Pair<>("Saturday", false),
            new Pair<>("Sunday", false)
    };

    public String timerString;
    public int indexMusic;
    public Boolean knoll = false;
    public Boolean deleteAfterAlarm = false;
    public String note;
    public int loopIndex = -1;

    public AlarmData(String timerString,int indexMusic, Boolean knoll, Boolean deleteAfterAlarm, String note, int loopIndex) {
        this.timerString = timerString;
        this.indexMusic = indexMusic;
        this.knoll = knoll;
        this.deleteAfterAlarm = deleteAfterAlarm;
        this.note = note;
        this.loopIndex = loopIndex;
    }
}
