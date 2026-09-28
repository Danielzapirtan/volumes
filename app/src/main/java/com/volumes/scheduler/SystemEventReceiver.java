package com.volumes.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class SystemEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        VolumeScheduler.rescheduleAll(context);
    }
}
