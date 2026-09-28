package com.volumes.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

final class VolumeScheduler {
    static final String ACTION_APPLY_VOLUME = "com.volumes.scheduler.APPLY_VOLUME";
    static final String EXTRA_RULE_ID = "rule_id";

    private VolumeScheduler() {}

    static boolean hasExactAlarmAccess(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        return manager != null && manager.canScheduleExactAlarms();
    }

    static void rescheduleAll(Context context) {
        for (ScheduleRule rule : RuleStore.load(context)) {
            cancel(context, rule.id);
            if (rule.enabled) {
                scheduleNext(context, rule);
            }
        }
    }

    static void scheduleNext(Context context, ScheduleRule rule) {
        if (!rule.enabled) {
            cancel(context, rule.id);
            return;
        }
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) {
            return;
        }
        Calendar nextRun = Calendar.getInstance();
        nextRun.set(Calendar.HOUR_OF_DAY, rule.hour);
        nextRun.set(Calendar.MINUTE, rule.minute);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);
        if (nextRun.getTimeInMillis() <= System.currentTimeMillis()) {
            nextRun.add(Calendar.DAY_OF_YEAR, 1);
        }

        PendingIntent pendingIntent = pendingIntent(context, rule.id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRun.getTimeInMillis(), pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, nextRun.getTimeInMillis(), pendingIntent);
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP, nextRun.getTimeInMillis(), pendingIntent);
        }
    }

    static void cancel(Context context, String ruleId) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) {
            manager.cancel(pendingIntent(context, ruleId));
        }
    }

    private static PendingIntent pendingIntent(Context context, String ruleId) {
        Intent intent = new Intent(context, ScheduleReceiver.class)
                .setAction(ACTION_APPLY_VOLUME)
                .setData(Uri.parse("volumes://schedule/" + Uri.encode(ruleId)))
                .putExtra(EXTRA_RULE_ID, ruleId);
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static ScheduleRule findRule(Context context, String ruleId) {
        List<ScheduleRule> rules = RuleStore.load(context);
        for (ScheduleRule rule : rules) {
            if (rule.id.equals(ruleId)) {
                return rule;
            }
        }
        return null;
    }
}
