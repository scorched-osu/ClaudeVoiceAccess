package com.claude.voiceaccess.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.claude.voiceaccess.R;
import com.claude.voiceaccess.ui.MainActivity;
import com.claude.voiceaccess.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceListenerService extends Service {

    private static final String TAG = "VoiceListenerService";
    public static final String CHANNEL_ID = "claude_voice_access_channel";
    public static final String ACTION_START_LISTENING = "com.claude.voiceaccess.START_LISTENING";
    public static final String ACTION_STOP_LISTENING = "com.claude.voiceaccess.STOP_LISTENING";
    public static final String ACTION_COMMAND_RECEIVED = "com.claude.voiceaccess.COMMAND_RECEIVED";

    private static final String WAKE_WORD = "hey claude";
    private static final long RESTART_DELAY_MS = 500;
    private static final int TIMEOUT_SECONDS = 30;

    public static boolean isListening = false;
    public static boolean isActive = false; // activated by wake word

    private SpeechRecognizer speechRecognizer;
    private Handler handler;
    private PreferenceManager preferenceManager;
    private boolean shouldKeepListening = true;
    private boolean wakeWordMode = true; // true = waiting for wake word, false = accepting commands
    private long lastCommandTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        preferenceManager = new PreferenceManager(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_LISTENING.equals(action)) {
                stopListeningMode();
                return START_NOT_STICKY;
            } else if (ACTION_START_LISTENING.equals(action)) {
                wakeWordMode = false;
                isActive = true;
                updateNotification(true);
            }
        }

        startForeground(1, buildNotification(false));
        shouldKeepListening = true;
        startSpeechRecognition();
        return START_STICKY;
    }

    private void startSpeechRecognition() {
        handler.post(() -> {
            if (!shouldKeepListening) return;
            if (speechRecognizer != null) {
                try { speechRecognizer.destroy(); } catch (Exception e) {}
                speechRecognizer = null;
            }

            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Log.e(TAG, "Speech recognition not available");
                handler.postDelayed(this::startSpeechRecognition, 3000);
                return;
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    Log.d(TAG, "Ready for speech, wakeWordMode=" + wakeWordMode);
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    Log.w(TAG, "Speech recognition error: " + error);
                    if (shouldKeepListening) {
                        long delay = (error == SpeechRecognizer.ERROR_NO_MATCH
                                || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) ? 100 : RESTART_DELAY_MS;
                        handler.postDelayed(VoiceListenerService.this::startSpeechRecognition, delay);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        processResult(matches.get(0));
                    }
                    if (shouldKeepListening) {
                        handler.postDelayed(VoiceListenerService.this::startSpeechRecognition, 100);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partial != null && !partial.isEmpty()) {
                        String text = partial.get(0).toLowerCase();
                        // Fast wake word detection on partial results
                        if (wakeWordMode && text.contains(WAKE_WORD)) {
                            activateFromWakeWord();
                        }
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0L);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L);

            try {
                speechRecognizer.startListening(recognizerIntent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to start listening", e);
                handler.postDelayed(this::startSpeechRecognition, 1000);
            }
        });
    }

    private void processResult(String text) {
        if (text == null) return;
        String lower = text.toLowerCase().trim();
        Log.d(TAG, "Recognized: " + lower + " | wakeWordMode=" + wakeWordMode);

        if (wakeWordMode) {
            if (lower.contains(WAKE_WORD)) {
                activateFromWakeWord();
            }
        } else {
            // We're in active command mode
            handleActiveCommand(lower);
        }
    }

    private void activateFromWakeWord() {
        Log.i(TAG, "Wake word detected! Activating...");
        wakeWordMode = false;
        isActive = true;
        lastCommandTime = System.currentTimeMillis();
        updateNotification(true);

        // Play activation sound or vibrate
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(AudioManager.FX_KEY_CLICK);

        // Schedule timeout back to wake word mode
        scheduleTimeout();

        // Notify the accessibility service
        if (ClaudeAccessibilityService.instance != null) {
            ClaudeAccessibilityService.instance.showFeedback("Listening…");
        }
    }

    private void handleActiveCommand(String command) {
        lastCommandTime = System.currentTimeMillis();

        // Check for deactivation commands
        if (command.equals("stop listening") || command.equals("hey claude stop")) {
            stopListeningMode();
            return;
        }

        Log.i(TAG, "Executing command: " + command);

        // Broadcast the command
        Intent broadcastIntent = new Intent(ACTION_COMMAND_RECEIVED);
        broadcastIntent.putExtra("command", command);
        sendBroadcast(broadcastIntent);

        // Execute via accessibility service
        if (ClaudeAccessibilityService.instance != null) {
            ClaudeAccessibilityService.instance.executeCommand(command);
        }

        // Reset timeout after each command
        scheduleTimeout();
    }

    private void scheduleTimeout() {
        handler.removeCallbacksAndMessages("timeout");
        if (preferenceManager.isTimeoutAfterNoSpeech()) {
            handler.postDelayed(() -> {
                if (isActive && System.currentTimeMillis() - lastCommandTime >= TIMEOUT_SECONDS * 1000L) {
                    Log.d(TAG, "Timeout: reverting to wake word mode");
                    stopListeningMode();
                }
            }, TIMEOUT_SECONDS * 1000L);
        }
    }

    private void stopListeningMode() {
        wakeWordMode = true;
        isActive = false;
        updateNotification(false);
        if (ClaudeAccessibilityService.instance != null) {
            ClaudeAccessibilityService.instance.showFeedback("Voice Access paused");
        }
    }

    private void updateNotification(boolean active) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, buildNotification(active));
    }

    private Notification buildNotification(boolean active) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Stop listening action
        Intent stopIntent = new Intent(this, VoiceListenerService.class);
        stopIntent.setAction(ACTION_STOP_LISTENING);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(active
                        ? getString(R.string.notification_text_listening)
                        : getString(R.string.notification_text_idle))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_pause,
                        active ? "Stop listening" : "Activate",
                        stopPending)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.notification_channel_desc));
        channel.setSound(null, null);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shouldKeepListening = false;
        isListening = false;
        isActive = false;
        handler.removeCallbacksAndMessages(null);
        if (speechRecognizer != null) {
            try { speechRecognizer.destroy(); } catch (Exception e) {}
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
