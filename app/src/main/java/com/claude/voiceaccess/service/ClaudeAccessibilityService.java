package com.claude.voiceaccess.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.claude.voiceaccess.R;
import com.claude.voiceaccess.commands.CommandProcessor;
import com.claude.voiceaccess.overlay.GridOverlayManager;
import com.claude.voiceaccess.overlay.LabelOverlayManager;
import com.claude.voiceaccess.overlay.NumberOverlayManager;
import com.claude.voiceaccess.utils.PreferenceManager;
import com.claude.voiceaccess.utils.RootCommandRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClaudeAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClaudeAccessService";
    public static ClaudeAccessibilityService instance;

    private CommandProcessor commandProcessor;
    private GridOverlayManager gridOverlayManager;
    private LabelOverlayManager labelOverlayManager;
    private NumberOverlayManager numberOverlayManager;
    private TextToSpeech tts;
    private WindowManager windowManager;
    private PreferenceManager preferenceManager;
    private Handler mainHandler;

    // State flags
    private boolean isShowingGrid = false;
    private boolean isShowingLabels = false;
    private boolean isShowingNumbers = false;
    private boolean isZooming = false;
    private float currentZoomLevel = 1.0f;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        mainHandler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        preferenceManager = new PreferenceManager(this);

        // Configure the accessibility service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
                | AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
                | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME
                | AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        // Initialize components
        commandProcessor = new CommandProcessor(this);
        gridOverlayManager = new GridOverlayManager(this, windowManager);
        labelOverlayManager = new LabelOverlayManager(this, windowManager);
        numberOverlayManager = new NumberOverlayManager(this, windowManager);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        // Start the voice listener service
        Intent intent = new Intent(this, VoiceListenerService.class);
        intent.putExtra("accessibilityReady", true);
        startForegroundService(intent);

        Log.i(TAG, "ClaudeAccessibilityService connected and ready");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        // Forward to command processor for context tracking
        if (commandProcessor != null) {
            commandProcessor.onAccessibilityEvent(event);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (gridOverlayManager != null) gridOverlayManager.hideGrid();
        if (labelOverlayManager != null) labelOverlayManager.hideLabels();
        if (numberOverlayManager != null) numberOverlayManager.hideNumbers();
    }

    // ==================== PUBLIC COMMAND METHODS ====================

    /** Execute a parsed voice command */
    public void executeCommand(String command) {
        mainHandler.post(() -> {
            if (commandProcessor != null) {
                commandProcessor.execute(command);
            }
        });
    }

    // ==================== NAVIGATION COMMANDS ====================

    public void performGoBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public void performGoHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public void performRecentApps() {
        performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    public void performNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    public void performQuickSettings() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
    }

    public void performLockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    public void performPowerMenu() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }

    public void performTakeScreenshot() {
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }

    public void performToggleSplitScreen() {
        performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
    }

    // ==================== GESTURE COMMANDS ====================

    public void performTap(float x, float y) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x, y);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        dispatchGesture(builder.build(), null, null);
    }

    public void performLongPress(float x, float y) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x, y);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1000));
        dispatchGesture(builder.build(), null, null);
    }

    public void performSwipe(float startX, float startY, float endX, float endY) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
        dispatchGesture(builder.build(), null, null);
    }

    public void performScrollUp() {
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        float cx = size.x / 2f;
        float cy = size.y / 2f;
        performSwipe(cx, cy + 300, cx, cy - 300);
    }

    public void performScrollDown() {
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        float cx = size.x / 2f;
        float cy = size.y / 2f;
        performSwipe(cx, cy - 300, cx, cy + 300);
    }

    public void performScrollLeft() {
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        float cx = size.x / 2f;
        float cy = size.y / 2f;
        performSwipe(cx + 300, cy, cx - 300, cy);
    }

    public void performScrollRight() {
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        float cx = size.x / 2f;
        float cy = size.y / 2f;
        performSwipe(cx - 300, cy, cx + 300, cy);
    }

    public void performScrollToTop() {
        // Scroll far up
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        float cx = size.x / 2f;
        performSwipe(cx, 200, cx, size.y - 200);
        mainHandler.postDelayed(() -> performSwipe(cx, 200, cx, size.y - 200), 350);
        mainHandler.postDelayed(() -> performSwipe(cx, 200, cx, size.y - 200), 700);
    }

    public void performScrollToBottom() {
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        float cx = size.x / 2f;
        performSwipe(cx, size.y - 200, cx, 200);
        mainHandler.postDelayed(() -> performSwipe(cx, size.y - 200, cx, 200), 350);
        mainHandler.postDelayed(() -> performSwipe(cx, size.y - 200, cx, 200), 700);
    }

    public void performSwipeEdge(String edge) {
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        switch (edge) {
            case "bottom":
                performSwipe(size.x / 2f, size.y - 10, size.x / 2f, size.y - 200);
                break;
            case "top":
                performSwipe(size.x / 2f, 10, size.x / 2f, 200);
                break;
            case "left":
                performSwipe(10, size.y / 2f, 200, size.y / 2f);
                break;
            case "right":
                performSwipe(size.x - 10, size.y / 2f, size.x - 200, size.y / 2f);
                break;
        }
    }

    public void performPinch(float x, float y, boolean pinchIn) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        float offset = pinchIn ? 200 : 50;
        float target = pinchIn ? 50 : 200;

        Path path1 = new Path();
        path1.moveTo(x - offset, y);
        path1.lineTo(x - target, y);

        Path path2 = new Path();
        path2.moveTo(x + offset, y);
        path2.lineTo(x + target, y);

        builder.addStroke(new GestureDescription.StrokeDescription(path1, 0, 300));
        builder.addStroke(new GestureDescription.StrokeDescription(path2, 0, 300));
        dispatchGesture(builder.build(), null, null);
    }

    // ==================== TAP BY NODE LABEL/TEXT ====================

    public boolean tapNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo node = findNodeByText(root, text);
        if (node != null) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            performTap(bounds.centerX(), bounds.centerY());
            node.recycle();
            root.recycle();
            return true;
        }
        root.recycle();
        return false;
    }

    public boolean tapNodeByNumber(int number) {
        if (numberOverlayManager != null) {
            return numberOverlayManager.tapNumber(number);
        }
        return false;
    }

    public boolean longPressNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo node = findNodeByText(root, text);
        if (node != null) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            performLongPress(bounds.centerX(), bounds.centerY());
            node.recycle();
            root.recycle();
            return true;
        }
        root.recycle();
        return false;
    }

    public boolean checkNodeByText(String text, boolean check) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        findNodesContainingText(root, text, nodes);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isCheckable()) {
                boolean isChecked = node.isChecked();
                if (check != isChecked) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                node.recycle();
                root.recycle();
                return true;
            }
            node.recycle();
        }
        root.recycle();
        return false;
    }

    // ==================== TEXT EDITING COMMANDS ====================

    public void startTextEditing() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo editField = findFirstEditText(root);
        if (editField != null) {
            editField.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            editField.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            editField.recycle();
        }
        root.recycle();
    }

    public void stopTextEditing() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo focused = findFocusedNode(root);
        if (focused != null) {
            focused.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
            performGlobalAction(GLOBAL_ACTION_BACK);
            focused.recycle();
        }
        root.recycle();
    }

    public void typeText(String text) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            focused.recycle();
        } else {
            // Fallback: try clipboard paste
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("typed", text));
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo ed = findFirstEditText(root);
                if (ed != null) {
                    ed.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                    ed.recycle();
                }
                root.recycle();
            }
        }
    }

    public void appendText(String text) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            String newText = (current != null ? current.toString() : "") + text;
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            focused.recycle();
        }
    }

    public void deleteAllText() {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            focused.recycle();
        }
    }

    public void deleteWord(String word) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String newText = current.toString().replace(word, "");
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            }
            focused.recycle();
        }
    }

    public void replaceText(String from, String to) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String newText = current.toString().replace(from, to);
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            }
            focused.recycle();
        }
    }

    public void replaceTextBetween(String first, String last, String replacement) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String text = current.toString();
                int startIdx = text.indexOf(first);
                int endIdx = text.indexOf(last);
                if (startIdx >= 0 && endIdx >= 0 && endIdx > startIdx) {
                    String newText = text.substring(0, startIdx) + replacement
                            + text.substring(endIdx + last.length());
                    Bundle args = new Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            }
            focused.recycle();
        }
    }

    public void selectAllText() {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            focused.performAction(AccessibilityNodeInfo.ACTION_SELECT);
            Bundle args = new Bundle();
            CharSequence text = focused.getText();
            if (text != null) {
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length());
                focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
            }
            focused.recycle();
        }
    }

    public void selectText(String word) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence text = focused.getText();
            if (text != null) {
                int idx = text.toString().indexOf(word);
                if (idx >= 0) {
                    Bundle args = new Bundle();
                    args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, idx);
                    args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, idx + word.length());
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
                }
            }
            focused.recycle();
        }
    }

    public void moveCursorToBeginning() {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            Bundle args = new Bundle();
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 0);
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
            focused.recycle();
        }
    }

    public void moveCursorToEnd() {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence text = focused.getText();
            if (text != null) {
                Bundle args = new Bundle();
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, text.length());
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length());
                focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
            }
            focused.recycle();
        }
    }

    public void capitalizeWord(String word) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String text = current.toString();
                int idx = text.indexOf(word);
                if (idx >= 0) {
                    String capitalized = Character.toUpperCase(word.charAt(0)) + word.substring(1);
                    String newText = text.substring(0, idx) + capitalized + text.substring(idx + word.length());
                    Bundle args = new Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            }
            focused.recycle();
        }
    }

    public void uppercaseWord(String word) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String text = current.toString();
                int idx = text.indexOf(word);
                if (idx >= 0) {
                    String newText = text.substring(0, idx) + word.toUpperCase() + text.substring(idx + word.length());
                    Bundle args = new Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            }
            focused.recycle();
        }
    }

    public void lowercaseWord(String word) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String text = current.toString();
                int idx = text.indexOf(word);
                if (idx >= 0) {
                    String newText = text.substring(0, idx) + word.toLowerCase() + text.substring(idx + word.length());
                    Bundle args = new Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            }
            focused.recycle();
        }
    }

    public void performUndo() {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            // Use keyboard shortcut Ctrl+Z via root or action
            RootCommandRunner.run("input keyevent --longpress KEYCODE_Z");
            focused.recycle();
        }
    }

    public void performRedo() {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            RootCommandRunner.run("input keyevent KEYCODE_Y");
            focused.recycle();
        }
    }

    public void insertTextBefore(String insertText, String anchor) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String text = current.toString();
                int idx = text.indexOf(anchor);
                if (idx >= 0) {
                    String newText = text.substring(0, idx) + insertText + text.substring(idx);
                    Bundle args = new Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            }
            focused.recycle();
        }
    }

    public void insertTextAfter(String insertText, String anchor) {
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused != null) {
            CharSequence current = focused.getText();
            if (current != null) {
                String text = current.toString();
                int idx = text.indexOf(anchor);
                if (idx >= 0) {
                    String newText = text.substring(0, idx + anchor.length()) + insertText
                            + text.substring(idx + anchor.length());
                    Bundle args = new Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            }
            focused.recycle();
        }
    }

    // ==================== AUDIO / SETTINGS COMMANDS ====================

    public void adjustVolume(int direction) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction,
                AudioManager.FLAG_SHOW_UI);
    }

    public void adjustRingVolume(int direction) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, direction,
                AudioManager.FLAG_SHOW_UI);
    }

    public void muteVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
    }

    public void unmuteVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);
    }

    public void muteRingVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING,
                AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
    }

    public void unmuteRingVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING,
                AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);
    }

    public void toggleBluetooth(boolean on) {
        RootCommandRunner.run("svc bluetooth " + (on ? "enable" : "disable"));
    }

    // ==================== GRID / LABEL / NUMBER OVERLAYS ====================

    public void showGrid() {
        isShowingGrid = true;
        if (gridOverlayManager != null) gridOverlayManager.showGrid();
    }

    public void hideGrid() {
        isShowingGrid = false;
        if (gridOverlayManager != null) gridOverlayManager.hideGrid();
    }

    public void showLabels() {
        isShowingLabels = true;
        if (labelOverlayManager != null) labelOverlayManager.showLabels(getRootInActiveWindow());
    }

    public void hideLabels() {
        isShowingLabels = false;
        if (labelOverlayManager != null) labelOverlayManager.hideLabels();
    }

    public void showNumbers() {
        isShowingNumbers = true;
        if (numberOverlayManager != null) numberOverlayManager.showNumbers(getRootInActiveWindow());
    }

    public void hideNumbers() {
        isShowingNumbers = false;
        if (numberOverlayManager != null) numberOverlayManager.hideNumbers();
    }

    public boolean isShowingGrid() { return isShowingGrid; }
    public boolean isShowingLabels() { return isShowingLabels; }
    public boolean isShowingNumbers() { return isShowingNumbers; }

    // ==================== MAGNIFICATION ====================

    public void startZooming() {
        isZooming = true;
        currentZoomLevel = 2.0f;
        getMagnificationController().setScale(currentZoomLevel, true);
    }

    public void stopZooming() {
        isZooming = false;
        currentZoomLevel = 1.0f;
        getMagnificationController().reset(true);
    }

    public void zoomIn() {
        currentZoomLevel = Math.min(currentZoomLevel + 0.5f, 8.0f);
        getMagnificationController().setScale(currentZoomLevel, true);
    }

    public void zoomOut() {
        currentZoomLevel = Math.max(currentZoomLevel - 0.5f, 1.0f);
        if (currentZoomLevel == 1.0f) {
            stopZooming();
        } else {
            getMagnificationController().setScale(currentZoomLevel, true);
        }
    }

    public void panLeft() {
        MagnificationController mc = getMagnificationController();
        mc.setCenter(mc.getCenterX() - 200, mc.getCenterY(), true);
    }

    public void panRight() {
        MagnificationController mc = getMagnificationController();
        mc.setCenter(mc.getCenterX() + 200, mc.getCenterY(), true);
    }

    public void panUp() {
        MagnificationController mc = getMagnificationController();
        mc.setCenter(mc.getCenterX(), mc.getCenterY() - 200, true);
    }

    public void panDown() {
        MagnificationController mc = getMagnificationController();
        mc.setCenter(mc.getCenterX(), mc.getCenterY() + 200, true);
    }

    // ==================== APP LAUNCHING ====================

    public void openApp(String appName) {
        android.content.pm.PackageManager pm = getPackageManager();
        List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (android.content.pm.ApplicationInfo app : apps) {
            String label = pm.getApplicationLabel(app).toString().toLowerCase();
            if (label.equalsIgnoreCase(appName) || label.contains(appName.toLowerCase())) {
                Intent launchIntent = pm.getLaunchIntentForPackage(app.packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    return;
                }
            }
        }
        showFeedback("App not found: " + appName);
    }

    public void callNumber(String number) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(android.net.Uri.parse("tel:" + number));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(callIntent);
    }

    public void answerCall() {
        RootCommandRunner.run("input keyevent KEYCODE_CALL");
    }

    public void searchInApp(String query) {
        // Attempt to find a search box and fill it
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        List<AccessibilityNodeInfo> searchNodes = new ArrayList<>();
        findSearchFields(root, searchNodes);
        if (!searchNodes.isEmpty()) {
            AccessibilityNodeInfo search = searchNodes.get(0);
            search.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mainHandler.postDelayed(() -> typeText(query), 300);
            search.recycle();
        }
        root.recycle();
    }

    public void copyPhoneNumber() {
        // Find phone number on screen and copy it
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        // Look for clickable phone number patterns
        String phone = findPhoneNumberOnScreen(root);
        if (phone != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("phone", phone));
            showFeedback("Phone number copied: " + phone);
        }
        root.recycle();
    }

    // ==================== MEDIA COMMANDS ====================

    public void mediaPlayPause() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        android.view.KeyEvent keyEvent = new android.view.KeyEvent(
                android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        am.dispatchMediaKeyEvent(keyEvent);
        keyEvent = new android.view.KeyEvent(
                android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        am.dispatchMediaKeyEvent(keyEvent);
    }

    // ==================== PIN INPUT (Alpha-bravo) ====================

    public void inputPinLabel(String label) {
        // Map phonetic alphabet to digits
        int digit = phoneticToDigit(label);
        if (digit >= 0) {
            tapNodeByNumber(digit);
        }
    }

    // ==================== WHAT IS / DESCRIBE ====================

    public String describeElement(int number) {
        if (numberOverlayManager != null) {
            return numberOverlayManager.describeNumber(number);
        }
        return "Unknown element";
    }

    // ==================== FEEDBACK ====================

    public void showFeedback(String message) {
        mainHandler.post(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    public void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // ==================== HELPER METHODS ====================

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo root, String text) {
        if (root == null) return null;
        CharSequence nodeText = root.getText();
        CharSequence nodeDesc = root.getContentDescription();
        if ((nodeText != null && nodeText.toString().equalsIgnoreCase(text))
                || (nodeDesc != null && nodeDesc.toString().equalsIgnoreCase(text))) {
            if (root.isClickable() || root.isFocusable()) {
                return root;
            }
        }
        // Also check partial match
        if ((nodeText != null && nodeText.toString().toLowerCase().contains(text.toLowerCase()))
                || (nodeDesc != null && nodeDesc.toString().toLowerCase().contains(text.toLowerCase()))) {
            if (root.isClickable()) return root;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            AccessibilityNodeInfo result = findNodeByText(child, text);
            if (result != null) {
                if (child != result) child.recycle();
                return result;
            }
            if (child != null) child.recycle();
        }
        return null;
    }

    private void findNodesContainingText(AccessibilityNodeInfo node, String text, List<AccessibilityNodeInfo> results) {
        if (node == null) return;
        CharSequence nodeText = node.getText();
        CharSequence nodeDesc = node.getContentDescription();
        if ((nodeText != null && nodeText.toString().toLowerCase().contains(text.toLowerCase()))
                || (nodeDesc != null && nodeDesc.toString().toLowerCase().contains(text.toLowerCase()))) {
            results.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            findNodesContainingText(child, text, results);
        }
    }

    private AccessibilityNodeInfo findFirstEditText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if ("android.widget.EditText".equals(node.getClassName())
                || node.isEditable()) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findFirstEditText(child);
            if (result != null) {
                if (child != result) child.recycle();
                return result;
            }
            if (child != null) child.recycle();
        }
        return null;
    }

    public AccessibilityNodeInfo findFocusedEditable() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && focused.isEditable()) return focused;
        // Fallback: find any editable
        AccessibilityNodeInfo result = findFirstEditText(root);
        root.recycle();
        return result;
    }

    private AccessibilityNodeInfo findFocusedNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        if (root.isFocused()) return root;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            AccessibilityNodeInfo result = findFocusedNode(child);
            if (result != null) {
                if (child != result) child.recycle();
                return result;
            }
            if (child != null) child.recycle();
        }
        return null;
    }

    private void findSearchFields(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> results) {
        if (node == null) return;
        String cls = node.getClassName() != null ? node.getClassName().toString() : "";
        String hint = node.getHintText() != null ? node.getHintText().toString().toLowerCase() : "";
        String desc = node.getContentDescription() != null ? node.getContentDescription().toString().toLowerCase() : "";
        if (node.isEditable() && (hint.contains("search") || desc.contains("search") || cls.contains("Search"))) {
            results.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            findSearchFields(child, results);
        }
    }

    private String findPhoneNumberOnScreen(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence text = node.getText();
        if (text != null) {
            String str = text.toString();
            if (str.matches(".*\\d[\\d\\s\\-().+]{7,}.*")) {
                return str.replaceAll("[^\\d+]", "");
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            String result = findPhoneNumberOnScreen(child);
            if (result != null) return result;
        }
        return null;
    }

    private int phoneticToDigit(String phonetic) {
        switch (phonetic.toLowerCase()) {
            case "alpha": return 1;
            case "bravo": return 2;
            case "charlie": return 3;
            case "delta": return 4;
            case "echo": return 5;
            case "foxtrot": return 6;
            case "golf": return 7;
            case "hotel": return 8;
            case "india": return 9;
            case "juliet": return 0;
            default: return -1;
        }
    }

    // ==================== SCREEN CONTENT READER ====================

    public String readScreenContents() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        extractText(root, sb);
        root.recycle();
        return sb.toString();
    }

    private void extractText(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (text != null && text.length() > 0) sb.append(text).append(" ");
        else if (desc != null && desc.length() > 0) sb.append(desc).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            extractText(child, sb);
            if (child != null) child.recycle();
        }
    }

    public List<AccessibilityNodeInfo> getAllInteractiveNodes() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        if (root != null) {
            collectInteractiveNodes(root, nodes);
        }
        return nodes;
    }

    private void collectInteractiveNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list) {
        if (node == null) return;
        if (node.isClickable() || node.isLongClickable() || node.isEditable()
                || node.isFocusable() || node.isScrollable()) {
            list.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            collectInteractiveNodes(child, list);
        }
    }
}
