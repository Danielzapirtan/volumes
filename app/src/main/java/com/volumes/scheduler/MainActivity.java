package com.volumes.scheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(28, 37, 55);
    private static final int MUTED = Color.rgb(111, 122, 143);
    private static final int ACCENT = Color.rgb(79, 99, 242);
    private static final int BACKGROUND = Color.rgb(246, 247, 251);
    private static final int BORDER = Color.rgb(230, 233, 241);

    private LinearLayout content;
    private LinearLayout schedulesContainer;
    private TextView currentVolume;
    private LinearLayout exactAlarmNotice;
    private List<ScheduleRule> rules;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        rules = RuleStore.load(this);
        buildScreen();
        renderSchedules();
        updateCurrentVolume();
        updateExactAlarmNotice();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VolumeScheduler.rescheduleAll(this);
        updateCurrentVolume();
        updateExactAlarmNotice();
        renderSchedules();
    }

    private void buildScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets systemBars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(0, systemBars.top, 0, systemBars.bottom);
                return insets;
            });
        }

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(30), dp(22), dp(30));
        scrollView.addView(content);

        TextView eyebrow = text("DAILY AUDIO ROUTINES", 12, ACCENT, true);
        eyebrow.setLetterSpacing(0.12f);
        content.addView(eyebrow, matchWrap());

        TextView title = text("Music volume,\non your schedule.", 32, INK, true);
        title.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(10);
        content.addView(title, titleParams);

        TextView intro = text(
                "Set it once. Your phone adjusts music volume automatically every day.",
                15, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams introParams = matchWrap();
        introParams.topMargin = dp(10);
        introParams.bottomMargin = dp(22);
        content.addView(intro, introParams);

        content.addView(buildCurrentVolumeCard(), matchWrap());
        exactAlarmNotice = buildExactAlarmNotice();
        LinearLayout.LayoutParams noticeParams = matchWrap();
        noticeParams.topMargin = dp(14);
        content.addView(exactAlarmNotice, noticeParams);

        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        sectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        TextView sectionTitle = text("Your schedule", 20, INK, true);
        sectionHeader.addView(sectionTitle, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView addButton = text("+  Add time", 14, ACCENT, true);
        addButton.setGravity(Gravity.CENTER);
        addButton.setPadding(dp(12), 0, dp(12), 0);
        addButton.setBackground(rounded(Color.rgb(233, 236, 255), 14));
        addButton.setOnClickListener(view -> showRuleDialog(null));
        sectionHeader.addView(addButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)));
        LinearLayout.LayoutParams sectionParams = matchWrap();
        sectionParams.topMargin = dp(24);
        sectionParams.bottomMargin = dp(10);
        content.addView(sectionHeader, sectionParams);

        schedulesContainer = new LinearLayout(this);
        schedulesContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(schedulesContainer, matchWrap());

        TextView footer = text(
                "Schedules repeat daily and are restored after your phone restarts. "
                        + "Volume changes affect media playback only.",
                13, MUTED, false);
        footer.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(16);
        content.addView(footer, footerParams);

        setContentView(scrollView);
    }

    private View buildCurrentVolumeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(15), dp(18), dp(15));
        card.setBackground(rounded(Color.WHITE, 18));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView label = text("CURRENT MUSIC VOLUME", 11, MUTED, true);
        label.setLetterSpacing(0.06f);
        labels.addView(label, matchWrap());
        TextView hint = text("On this device right now", 13, MUTED, false);
        LinearLayout.LayoutParams hintParams = matchWrap();
        hintParams.topMargin = dp(4);
        labels.addView(hint, hintParams);
        card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

        currentVolume = text("—%", 25, INK, true);
        currentVolume.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        card.addView(currentVolume, new LinearLayout.LayoutParams(-2, dp(46)));
        return card;
    }

    private LinearLayout buildExactAlarmNotice() {
        LinearLayout notice = new LinearLayout(this);
        notice.setOrientation(LinearLayout.VERTICAL);
        notice.setPadding(dp(16), dp(14), dp(16), dp(14));
        notice.setBackground(rounded(Color.rgb(255, 246, 224), 16));

        TextView heading = text("Allow precise schedule times", 15, INK, true);
        notice.addView(heading, matchWrap());
        TextView message = text(
                "Android currently allows approximate alarms. Enable exact alarms so volume "
                        + "changes happen close to the time you choose.",
                13, MUTED, false);
        message.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams messageParams = matchWrap();
        messageParams.topMargin = dp(5);
        notice.addView(message, messageParams);
        TextView action = text("Open alarm access settings", 14, ACCENT, true);
        action.setPadding(0, dp(12), 0, 0);
        action.setOnClickListener(view -> requestExactAlarmAccess());
        notice.addView(action, matchWrap());
        return notice;
    }

    private void updateExactAlarmNotice() {
        if (exactAlarmNotice != null) {
            exactAlarmNotice.setVisibility(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            && !VolumeScheduler.hasExactAlarmAccess(this)
                            ? View.VISIBLE : View.GONE);
        }
    }

    private void requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        try {
            Intent intent = new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "Could not open alarm access settings.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateCurrentVolume() {
        AudioManager audioManager = getSystemService(AudioManager.class);
        if (audioManager == null || currentVolume == null) {
            return;
        }
        int maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int percentage = maximum == 0 ? 0 : Math.round(current * 100f / maximum);
        currentVolume.setText(percentage + "%");
    }

    private void renderSchedules() {
        if (schedulesContainer == null) {
            return;
        }
        rules = RuleStore.load(this);
        schedulesContainer.removeAllViews();
        if (rules.isEmpty()) {
            LinearLayout emptyCard = new LinearLayout(this);
            emptyCard.setGravity(Gravity.CENTER);
            emptyCard.setOrientation(LinearLayout.VERTICAL);
            emptyCard.setPadding(dp(20), dp(26), dp(20), dp(26));
            emptyCard.setBackground(rounded(Color.WHITE, 18));
            TextView emptyTitle = text("Nothing scheduled yet", 16, INK, true);
            emptyCard.addView(emptyTitle, wrapWrap());
            TextView emptyHint = text("Add a time to set your daily music volume.", 13, MUTED, false);
            LinearLayout.LayoutParams hintParams = wrapWrap();
            hintParams.topMargin = dp(6);
            emptyCard.addView(emptyHint, hintParams);
            schedulesContainer.addView(emptyCard, matchWrap());
            return;
        }

        Collections.sort(rules, (first, second) -> {
            int firstMinute = first.hour * 60 + first.minute;
            int secondMinute = second.hour * 60 + second.minute;
            return Integer.compare(firstMinute, secondMinute);
        });
        for (ScheduleRule rule : rules) {
            schedulesContainer.addView(buildRuleCard(rule), matchWrap());
        }
    }

    private View buildRuleCard(ScheduleRule rule) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(15), dp(18), dp(13));
        card.setBackground(rounded(Color.WHITE, 18));
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView time = text(formatTime(rule.hour, rule.minute), 27,
                rule.enabled ? INK : MUTED, true);
        topRow.addView(time, new LinearLayout.LayoutParams(0, dp(40), 1));
        Switch toggle = new Switch(this);
        toggle.setContentDescription((rule.enabled ? "Disable" : "Enable")
                + " schedule at " + formatTime(rule.hour, rule.minute));
        toggle.setChecked(rule.enabled);
        toggle.setOnCheckedChangeListener((button, checked) -> updateRule(rule.withEnabled(checked)));
        topRow.addView(toggle, wrapWrap());
        card.addView(topRow, matchWrap());

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView volume = text("Music volume  ·  " + rule.volumePercent + "%", 14, MUTED, false);
        bottomRow.addView(volume, new LinearLayout.LayoutParams(0, dp(40), 1));
        TextView edit = smallAction("Edit", view -> showRuleDialog(rule));
        bottomRow.addView(edit, wrapWrap());
        TextView delete = smallAction("Delete", view -> confirmDelete(rule));
        LinearLayout.LayoutParams deleteParams = wrapWrap();
        deleteParams.leftMargin = dp(14);
        bottomRow.addView(delete, deleteParams);
        card.addView(bottomRow, matchWrap());
        return card;
    }

    private TextView smallAction(String label, View.OnClickListener listener) {
        TextView action = text(label, 13, ACCENT, true);
        action.setGravity(Gravity.CENTER);
        action.setMinHeight(dp(40));
        action.setMinWidth(dp(44));
        action.setOnClickListener(listener);
        return action;
    }

    private void showRuleDialog(ScheduleRule existing) {
        Calendar initial = Calendar.getInstance();
        if (existing != null) {
            initial.set(Calendar.HOUR_OF_DAY, existing.hour);
            initial.set(Calendar.MINUTE, existing.minute);
        } else {
            initial.add(Calendar.HOUR_OF_DAY, 1);
            initial.set(Calendar.MINUTE, 0);
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(8));
        TimePicker timePicker = new TimePicker(this);
        timePicker.setIs24HourView(DateFormat.is24HourFormat(this));
        timePicker.setHour(initial.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(initial.get(Calendar.MINUTE));
        form.addView(timePicker, new LinearLayout.LayoutParams(-1, -2));

        TextView volumeLabel = text("", 15, INK, true);
        LinearLayout.LayoutParams volumeLabelParams = matchWrap();
        volumeLabelParams.topMargin = dp(12);
        form.addView(volumeLabel, volumeLabelParams);
        SeekBar volumeSlider = new SeekBar(this);
        volumeSlider.setMax(100);
        volumeSlider.setProgress(existing == null ? 50 : existing.volumePercent);
        volumeLabel.setText("Music volume  ·  " + volumeSlider.getProgress() + "%");
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeLabel.setText("Music volume  ·  " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        form.addView(volumeSlider, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add daily time" : "Edit daily time")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(existing == null ? "Add schedule" : "Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    for (ScheduleRule rule : rules) {
                        if ((existing == null || !rule.id.equals(existing.id))
                                && rule.hour == hour && rule.minute == minute) {
                            Toast.makeText(this, "A schedule already uses that time.", Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                    }

                    ScheduleRule updated = existing == null
                            ? ScheduleRule.create(hour, minute, volumeSlider.getProgress())
                            : new ScheduleRule(existing.id, hour, minute,
                                    volumeSlider.getProgress(), existing.enabled);
                    if (existing == null) {
                        rules.add(updated);
                    } else {
                        replaceRule(updated);
                    }
                    RuleStore.save(this, rules);
                    VolumeScheduler.rescheduleAll(this);
                    renderSchedules();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void confirmDelete(ScheduleRule rule) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this time?")
                .setMessage("The " + formatTime(rule.hour, rule.minute) + " volume change will be removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    VolumeScheduler.cancel(this, rule.id);
                    Iterator<ScheduleRule> iterator = rules.iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().id.equals(rule.id)) {
                            iterator.remove();
                            break;
                        }
                    }
                    RuleStore.save(this, rules);
                    renderSchedules();
                })
                .show();
    }

    private void updateRule(ScheduleRule updated) {
        replaceRule(updated);
        RuleStore.save(this, rules);
        if (updated.enabled) {
            VolumeScheduler.scheduleNext(this, updated);
        } else {
            VolumeScheduler.cancel(this, updated.id);
        }
        renderSchedules();
    }

    private void replaceRule(ScheduleRule updated) {
        List<ScheduleRule> replacement = new ArrayList<>(rules.size());
        for (ScheduleRule rule : rules) {
            replacement.add(rule.id.equals(updated.id) ? updated : rule);
        }
        rules = replacement;
    }

    private String formatTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return DateFormat.getTimeFormat(this).format(calendar.getTime());
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (color == Color.WHITE) {
            drawable.setStroke(dp(1), BORDER);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
