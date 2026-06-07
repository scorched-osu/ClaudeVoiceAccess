package com.claude.voiceaccess.ui;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.claude.voiceaccess.R;
import com.claude.voiceaccess.utils.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.voice_access_settings);
        }

        prefs = new PreferenceManager(this);
        setupUI();
    }

    private void setupUI() {
        Switch requireVerbsSwitch = findViewById(R.id.switch_require_verbs);
        Switch timeoutSwitch = findViewById(R.id.switch_timeout);
        Switch activateCallsSwitch = findViewById(R.id.switch_activate_calls);
        Switch duringCallsSwitch = findViewById(R.id.switch_during_calls);
        Switch recognizeIconsSwitch = findViewById(R.id.switch_recognize_icons);
        TextView listeningBehaviorText = findViewById(R.id.text_listening_behavior);

        requireVerbsSwitch.setChecked(prefs.isRequireVerbs());
        timeoutSwitch.setChecked(prefs.isTimeoutAfterNoSpeech());
        activateCallsSwitch.setChecked(prefs.isActivateForIncomingCalls());
        duringCallsSwitch.setChecked(prefs.isActiveDuringCalls());
        recognizeIconsSwitch.setChecked(prefs.isRecognizeCommonIcons());

        String listenBehavior = prefs.getListeningBehaviorOnWake();
        listeningBehaviorText.setText("Always start listening".equals(listenBehavior)
                ? "Always start listening" : "Don't start automatically");

        requireVerbsSwitch.setOnCheckedChangeListener((btn, checked) -> prefs.setRequireVerbs(checked));
        timeoutSwitch.setOnCheckedChangeListener((btn, checked) -> prefs.setTimeoutAfterNoSpeech(checked));
        activateCallsSwitch.setOnCheckedChangeListener((btn, checked) -> prefs.setActivateForIncomingCalls(checked));
        duringCallsSwitch.setOnCheckedChangeListener((btn, checked) -> prefs.setActiveDuringCalls(checked));
        recognizeIconsSwitch.setOnCheckedChangeListener((btn, checked) -> prefs.setRecognizeCommonIcons(checked));

        listeningBehaviorText.setOnClickListener(v -> {
            String current = prefs.getListeningBehaviorOnWake();
            if ("always".equals(current)) {
                prefs.setListeningBehaviorOnWake("never");
                listeningBehaviorText.setText("Don't start automatically");
            } else {
                prefs.setListeningBehaviorOnWake("always");
                listeningBehaviorText.setText("Always start listening");
            }
        });

        // Help items
        findViewById(R.id.btn_voice_commands).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CommandsActivity.class)));
        findViewById(R.id.btn_open_tutorial).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, TutorialActivity.class)));
        findViewById(R.id.btn_setup_voice_access).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, PermissionSetupActivity.class)));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
