package com.volumes.scheduler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

final class ScheduleRule {
    final String id;
    final int hour;
    final int minute;
    final int volumePercent;
    final boolean enabled;

    ScheduleRule(String id, int hour, int minute, int volumePercent, boolean enabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.volumePercent = volumePercent;
        this.enabled = enabled;
    }

    static ScheduleRule create(int hour, int minute, int volumePercent) {
        return new ScheduleRule(UUID.randomUUID().toString(), hour, minute, volumePercent, true);
    }

    ScheduleRule withEnabled(boolean value) {
        return new ScheduleRule(id, hour, minute, volumePercent, value);
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("hour", hour);
        json.put("minute", minute);
        json.put("volumePercent", volumePercent);
        json.put("enabled", enabled);
        return json;
    }

    static ScheduleRule fromJson(JSONObject json) throws JSONException {
        String id = json.getString("id");
        int hour = json.getInt("hour");
        int minute = json.getInt("minute");
        int volumePercent = json.getInt("volumePercent");
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59
                || volumePercent < 0 || volumePercent > 100) {
            throw new JSONException("Schedule value out of range");
        }
        return new ScheduleRule(id, hour, minute, volumePercent, json.optBoolean("enabled", true));
    }
}
