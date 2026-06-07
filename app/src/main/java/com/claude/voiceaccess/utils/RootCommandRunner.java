package com.claude.voiceaccess.utils;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Runs shell commands under root (su) context.
 * Used for system-level operations that require elevated permissions.
 */
public class RootCommandRunner {

    private static final String TAG = "RootCommandRunner";

    /**
     * Run a single shell command as root.
     * @param command The shell command to execute
     * @return true if execution was attempted
     */
    public static boolean run(String command) {
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            su.waitFor();
            Log.d(TAG, "Root command executed: " + command);
            return true;
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Root command failed: " + command, e);
            return false;
        }
    }

    /**
     * Run multiple commands as root in one session.
     */
    public static boolean runMultiple(String... commands) {
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            for (String cmd : commands) {
                os.writeBytes(cmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            su.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Root commands failed", e);
            return false;
        }
    }

    /**
     * Check if root access is available.
     */
    public static boolean isRootAvailable() {
        try {
            Process p = Runtime.getRuntime().exec("su -c id");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Grant accessibility permission via adb/root
     */
    public static void grantAccessibilityPermission(String packageName, String serviceName) {
        run("settings put secure enabled_accessibility_services "
                + packageName + "/" + serviceName);
        run("settings put secure accessibility_enabled 1");
    }

    /**
     * Grant overlay permission
     */
    public static void grantOverlayPermission(String packageName) {
        run("appops set " + packageName + " SYSTEM_ALERT_WINDOW allow");
    }

    /**
     * Grant write settings permission
     */
    public static void grantWriteSettingsPermission(String packageName) {
        run("appops set " + packageName + " WRITE_SETTINGS allow");
    }

    /**
     * Enable specific accessibility service
     */
    public static void enableAccessibilityService(String flattenedComponent) {
        runMultiple(
            "settings put secure accessibility_enabled 1",
            "settings put secure enabled_accessibility_services " + flattenedComponent,
            "am broadcast -a android.server.accessibility.AccessibilityManagerService --ei 1 1"
        );
    }
}
