package com.example.alarmclock.alarm;

import com.example.alarmclock.Pair;
import com.example.alarmclock.R;

public class AlarmData {
    public Pair<String, String>[] items = new Pair[] {
            new Pair<>("It's going to be a good day", "@raw/it_s_going_to_be_a_good_day"),
            new Pair<>("See you again meow", "@raw/see_you_again_meow"),
            new Pair<>("Squeaky computer chair", "@raw/squeaky_computer_chair")
    };
    public Pair<Integer, Boolean>[] loopOption = new Pair[] {
            new Pair<>(R.string.loop_option_one_time, false),
            new Pair<>(R.string.loop_option_daily, false),
            new Pair<>(R.string.loop_option_monday_to_saturday, false),
            new Pair<>(R.string.loop_option_custom, false)
    };
    public Pair<Integer, Boolean>[] optionOther = new Pair[] {
            new Pair<>(R.string.monday, false),
            new Pair<>(R.string.tuesday, false),
            new Pair<>(R.string.wednesday, false),
            new Pair<>(R.string.thursday, false),
            new Pair<>(R.string.friday, false),
            new Pair<>(R.string.saturday, false),
            new Pair<>(R.string.sunday, false)
    };

    public String timerString;
    public int indexMusic;
    public Boolean knoll = false;
    public Boolean deleteAfterAlarm = false;
    public String note;
    public int loopIndex = 0;

    public AlarmData(String timerString,int indexMusic, Boolean knoll, Boolean deleteAfterAlarm, String note, int loopIndex) {
        this.timerString = timerString;
        this.indexMusic = indexMusic;
        this.knoll = knoll;
        this.deleteAfterAlarm = deleteAfterAlarm;
        this.note = note;
        this.loopIndex = loopIndex;
    }
}
