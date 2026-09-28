package com.volumes.scheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

final class RuleStore {
    private static final String TAG = "RuleStore";
    private static final String PREFERENCES = "volume_schedules";
    private static final String RULES_KEY = "rules";

    private RuleStore() {}

    static List<ScheduleRule> load(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String stored = preferences.getString(RULES_KEY, "[]");
        List<ScheduleRule> rules = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(stored);
            for (int index = 0; index < array.length(); index++) {
                try {
                    rules.add(ScheduleRule.fromJson(array.getJSONObject(index)));
                } catch (JSONException exception) {
                    Log.e(TAG, "Skipping invalid saved volume schedule at index " + index, exception);
                }
            }
        } catch (JSONException exception) {
            Log.e(TAG, "Could not read saved volume schedules", exception);
        }
        return rules;
    }

    static void save(Context context, List<ScheduleRule> rules) {
        JSONArray array = new JSONArray();
        for (ScheduleRule rule : rules) {
            try {
                array.put(rule.toJson());
            } catch (JSONException exception) {
                throw new IllegalStateException("Could not save volume schedule", exception);
            }
        }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(RULES_KEY, array.toString())
                .apply();
    }
}
