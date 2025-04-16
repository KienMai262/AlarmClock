package com.example.alarmclock.items;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.alarmclock.alarm.AlarmData;
import com.example.alarmclock.databinding.ItemAlarmBinding;


public class PrefabItems {

    public ItemAlarmBinding binding;
    public AlarmData alarmData;
    public boolean isChecked = false;

    public PrefabItems(LayoutInflater inflater, ViewGroup parent, AlarmData alarmData) {
        binding = ItemAlarmBinding.inflate(inflater, parent, false);
        this.alarmData = alarmData;
    }




    public View getView() {
        return binding.getRoot();
    }
}
