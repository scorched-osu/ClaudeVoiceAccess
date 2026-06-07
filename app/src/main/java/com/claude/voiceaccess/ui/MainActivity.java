package com.claude.voiceaccess.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.claude.voiceaccess.R;
import com.claude.voiceaccess.service.ClaudeAccessibilityService;
import com.claude.voiceaccess.service.VoiceListenerService;
import com.claude.voiceaccess.utils.PreferenceManager;
import com.claude.voiceaccess.utils.RootCommandRunner;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 1001;
    private Switch mainSwitch;
    private TextView statusText;
    private TextView wakeWordHint;
    private ImageView micIcon;
    private PreferenceManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferenceManager(this);

        mainSwitch = findViewById(R.id.main_switch);
        statusText = findViewById(R.id.status_text);
        wakeWordHint = findViewById(R.id.wake_word_hint);
        micIcon = findViewById(R.id.mic_icon);

        setupClickListeners();
        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void setupClickListeners() {
        mainSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                enableVoiceAccess();
            } else {
                disableVoiceAccess();
            }
        });

        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        findViewById(R.id.btn_commands).setOnClickListener(v -> {
            startActivity(new Intent(this, CommandsActivity.class));
        });

        findViewById(R.id.btn_tutorial).setOnClickListener(v -> {
            startActivity(new Intent(this, TutorialActivity.class));
        });

        findViewById(R.id.btn_permissions).setOnClickListener(v -> {
            startActivity(new Intent(this, PermissionSetupActivity.class));
        });
    }

    private void enableVoiceAccess() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "Please enable accessibility permission first", Toast.LENGTH_LONG).show();
            mainSwitch.setChecked(false);
            openAccessibilitySettings();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
            mainSwitch.setChecked(false);
            return;
        }
        prefs.setServiceEnabled(true);
        Intent serviceIntent = new Intent(this, VoiceListenerService.class);
        startForegroundService(serviceIntent);
        updateStatus();
    }

    private void disableVoiceAccess() {
        prefs.setServiceEnabled(false);
        Intent serviceIntent = new Intent(this, VoiceListenerService.class);
        stopService(serviceIntent);
        updateStatus();
    }

    private void updateStatus() {
        boolean accessEnabled = isAccessibilityEnabled();
        boolean serviceRunning = VoiceListenerService.isListening || prefs.isServiceEnabled();

        mainSwitch.setChecked(serviceRunning && accessEnabled);

        if (!accessEnabled) {
            statusText.setText("Accessibility not enabled");
            statusText.setTextColor(getColor(R.color.claude_red));
            wakeWordHint.setText("Tap permissions to set up");
        } else if (serviceRunning) {
            statusText.setText(VoiceListenerService.isActive
                    ? getString(R.string.listening)
                    : getString(R.string.say_hey_claude));
            statusText.setTextColor(VoiceListenerService.isActive
                    ? getColor(R.color.claude_accent)
                    : getColor(R.color.claude_on_surface));
            wakeWordHint.setText("Say \"Hey Claude\" to activate");
        } else {
            statusText.setText(getString(R.string.tap_to_start));
            statusText.setTextColor(getColor(R.color.claude_on_surface_variant));
            wakeWordHint.setText("Toggle the switch to begin");
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabledServices != null
                    && enabledServices.contains("com.claude.voiceaccess");
        } catch (Exception e) {
            return ClaudeAccessibilityService.instance != null;
        }
    }

    private void openAccessibilitySettings() {
        // Try root first
        RootCommandRunner.enableAccessibilityService(
                "com.claude.voiceaccess/.service.ClaudeAccessibilityService");
        // Also open settings so user can verify
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
        }
        // Request overlay permission if not granted
        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(overlayIntent);
        }
        // Try to grant permissions via root
        RootCommandRunner.grantOverlayPermission(getPackageName());
        RootCommandRunner.grantWriteSettingsPermission(getPackageName());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableVoiceAccess();
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show();
            }
        }
    }
}
