package com.claude.voiceaccess.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.claude.voiceaccess.R;
import com.claude.voiceaccess.utils.RootCommandRunner;

public class PermissionSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Set up Claude Voice Access");
        }

        setupButtons();
        // Try auto-grant via root
        autoGrantPermissions();
    }

    private void autoGrantPermissions() {
        new Thread(() -> {
            boolean rootAvailable = RootCommandRunner.isRootAvailable();
            if (rootAvailable) {
                RootCommandRunner.enableAccessibilityService(
                        "com.claude.voiceaccess/.service.ClaudeAccessibilityService");
                RootCommandRunner.grantOverlayPermission(getPackageName());
                RootCommandRunner.grantWriteSettingsPermission(getPackageName());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Permissions auto-granted via root", Toast.LENGTH_SHORT).show();
                    updatePermissionStatus();
                });
            } else {
                runOnUiThread(this::updatePermissionStatus);
            }
        }).start();
    }

    private void setupButtons() {
        Button btnAccessibility = findViewById(R.id.btn_grant_accessibility);
        Button btnOverlay = findViewById(R.id.btn_grant_overlay);
        Button btnMicrophone = findViewById(R.id.btn_grant_microphone);
        Button btnDone = findViewById(R.id.btn_done);

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        btnMicrophone.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        });

        btnDone.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void updatePermissionStatus() {
        boolean accessEnabled = isAccessibilityEnabled();
        boolean overlayEnabled = Settings.canDrawOverlays(this);
        boolean micEnabled = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        updateStatus(R.id.status_accessibility, R.id.btn_grant_accessibility, accessEnabled);
        updateStatus(R.id.status_overlay, R.id.btn_grant_overlay, overlayEnabled);
        updateStatus(R.id.status_microphone, R.id.btn_grant_microphone, micEnabled);
    }

    private void updateStatus(int statusId, int btnId, boolean granted) {
        ImageView status = findViewById(statusId);
        Button btn = findViewById(btnId);
        if (granted) {
            status.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            status.setColorFilter(getColor(R.color.claude_accent));
            btn.setEnabled(false);
            btn.setText("Granted ✓");
        } else {
            status.setImageResource(android.R.drawable.ic_dialog_alert);
            status.setColorFilter(getColor(R.color.claude_red));
            btn.setEnabled(true);
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabledServices != null && enabledServices.contains("com.claude.voiceaccess");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
