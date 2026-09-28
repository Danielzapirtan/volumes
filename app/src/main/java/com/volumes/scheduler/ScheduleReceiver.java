package com.volumes.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public final class ScheduleReceiver extends BroadcastReceiver {
    private static final String TAG = "ScheduleReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!VolumeScheduler.ACTION_APPLY_VOLUME.equals(intent.getAction())) {
            return;
        }

        String ruleId = intent.getStringExtra(VolumeScheduler.EXTRA_RULE_ID);
        if (ruleId == null) {
            return;
        }
        ScheduleRule rule = VolumeScheduler.findRule(context, ruleId);
        if (rule == null || !rule.enabled) {
            return;
        }

        AudioManager audioManager = context.getSystemService(AudioManager.class);
        try {
            if (audioManager != null) {
                int maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int target = Math.round(maximum * (rule.volumePercent / 100f));
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
            }
        } catch (SecurityException exception) {
            Log.e(TAG, "Unable to set music volume for schedule " + rule.id, exception);
        } finally {
            VolumeScheduler.scheduleNext(context, rule);
        }
    }
}
