package com.claude.voiceaccess.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class PreferenceManager {

    private static final String PREF_REQUIRE_VERBS = "require_verbs";
    private static final String PREF_TIMEOUT_NO_SPEECH = "timeout_no_speech";
    private static final String PREF_LISTENING_ON_WAKE = "listening_on_wake";
    private static final String PREF_ACTIVATE_FOR_CALLS = "activate_for_calls";
    private static final String PREF_ACTIVE_DURING_CALLS = "active_during_calls";
    private static final String PREF_RECOGNIZE_ICONS = "recognize_icons";
    private static final String PREF_SERVICE_ENABLED = "service_enabled";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isRequireVerbs() { return prefs.getBoolean(PREF_REQUIRE_VERBS, true); }
    public void setRequireVerbs(boolean v) { prefs.edit().putBoolean(PREF_REQUIRE_VERBS, v).apply(); }

    public boolean isTimeoutAfterNoSpeech() { return prefs.getBoolean(PREF_TIMEOUT_NO_SPEECH, false); }
    public void setTimeoutAfterNoSpeech(boolean v) { prefs.edit().putBoolean(PREF_TIMEOUT_NO_SPEECH, v).apply(); }

    public String getListeningBehaviorOnWake() { return prefs.getString(PREF_LISTENING_ON_WAKE, "always"); }
    public void setListeningBehaviorOnWake(String v) { prefs.edit().putString(PREF_LISTENING_ON_WAKE, v).apply(); }

    public boolean isActivateForIncomingCalls() { return prefs.getBoolean(PREF_ACTIVATE_FOR_CALLS, true); }
    public void setActivateForIncomingCalls(boolean v) { prefs.edit().putBoolean(PREF_ACTIVATE_FOR_CALLS, v).apply(); }

    public boolean isActiveDuringCalls() { return prefs.getBoolean(PREF_ACTIVE_DURING_CALLS, true); }
    public void setActiveDuringCalls(boolean v) { prefs.edit().putBoolean(PREF_ACTIVE_DURING_CALLS, v).apply(); }

    public boolean isRecognizeCommonIcons() { return prefs.getBoolean(PREF_RECOGNIZE_ICONS, true); }
    public void setRecognizeCommonIcons(boolean v) { prefs.edit().putBoolean(PREF_RECOGNIZE_ICONS, v).apply(); }

    public boolean isServiceEnabled() { return prefs.getBoolean(PREF_SERVICE_ENABLED, false); }
    public void setServiceEnabled(boolean v) { prefs.edit().putBoolean(PREF_SERVICE_ENABLED, v).apply(); }
}
