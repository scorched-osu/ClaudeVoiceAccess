package com.claude.voiceaccess.commands;

import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.claude.voiceaccess.service.ClaudeAccessibilityService;
import com.claude.voiceaccess.utils.PreferenceManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses voice commands and routes them to the appropriate accessibility service actions.
 * Covers ALL commands shown in the Voice Access screenshots.
 */
public class CommandProcessor {

    private static final String TAG = "CommandProcessor";
    private final ClaudeAccessibilityService service;
    private final PreferenceManager prefs;

    // Track current state for undo/redo
    private String lastTypedText = "";
    private String lastAction = "";

    // Current focused app/package
    private String currentPackage = "";
    private String currentActivityTitle = "";

    public CommandProcessor(ClaudeAccessibilityService service) {
        this.service = service;
        this.prefs = new PreferenceManager(service);
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {
            currentPackage = event.getPackageName().toString();
        }
    }

    /** Main dispatch method */
    public void execute(String rawCommand) {
        if (rawCommand == null) return;
        String cmd = rawCommand.toLowerCase().trim();
        // Remove extra spaces
        cmd = cmd.replaceAll("\\s+", " ");
        Log.d(TAG, "Executing command: " + cmd);

        // When require_verbs is ON, we can still accept certain implicit commands
        boolean requireVerbs = prefs.isRequireVerbs();

        // ===== VOICE ACCESS CONTROL =====
        if (cmd.equals("stop listening")) { service.showFeedback("Stopped listening"); return; }
        if (cmd.equals("all commands")) { showAllCommands(); return; }
        if (cmd.equals("open tutorial")) { openTutorial(); return; }
        if (cmd.equals("voice access settings")) { openVoiceAccessSettings(); return; }
        if (cmd.equals("send feedback")) { openFeedback(); return; }
        if (cmd.equals("help") || cmd.equals("show help")) { showHelp(); return; }
        if (cmd.equals("cancel")) { service.showFeedback("Cancelled"); return; }
        if (cmd.equals("again")) { repeatLastAction(); return; }

        // ===== NUMBER/LABEL OVERLAYS =====
        if (cmd.equals("show numbers")) { service.showNumbers(); return; }
        if (cmd.equals("hide numbers")) { service.hideNumbers(); return; }
        if (cmd.equals("show labels")) { service.showLabels(); return; }
        if (cmd.equals("hide labels")) { service.hideLabels(); return; }

        // "what is [8]?" - describe numbered element
        Matcher whatIs = Pattern.compile("what is (\\d+)").matcher(cmd);
        if (whatIs.find()) {
            int num = Integer.parseInt(whatIs.group(1));
            String desc = service.describeElement(num);
            service.showFeedback(desc);
            service.speak(desc);
            return;
        }

        // "show commands for [8]"
        Matcher showCmds = Pattern.compile("show commands for (\\d+)").matcher(cmd);
        if (showCmds.find()) {
            service.showFeedback("Commands for element " + showCmds.group(1));
            return;
        }

        // ===== BASIC NAVIGATION =====
        if (cmd.equals("go back") || cmd.equals("back")) { service.performGoBack(); return; }
        if (cmd.equals("go home") || cmd.equals("home")) { service.performGoHome(); return; }
        if (cmd.equals("notifications") || cmd.equals("open notifications")) { service.performNotifications(); return; }
        if (cmd.equals("quick settings") || cmd.equals("open quick settings")) { service.performQuickSettings(); return; }
        if (cmd.equals("recent apps") || cmd.equals("show recent apps")) { service.performRecentApps(); return; }
        if (cmd.equals("previous app")) { service.performRecentApps(); return; }
        if (cmd.equals("lock screen") || cmd.equals("lock device")) { service.performLockScreen(); return; }
        if (cmd.equals("turn off device") || cmd.equals("power menu")) { service.performPowerMenu(); return; }
        if (cmd.equals("take screenshot") || cmd.equals("screenshot")) { service.performTakeScreenshot(); return; }
        if (cmd.equals("hide keyboard") || cmd.equals("dismiss keyboard")) {
            service.performGoBack(); return;
        }
        if (cmd.equals("show keyboard")) {
            service.showFeedback("Tap a text field to show keyboard"); return;
        }
        if (cmd.equals("answer call") || cmd.equals("answer")) { service.answerCall(); return; }
        if (cmd.equals("media play") || cmd.equals("media pause") || cmd.equals("play pause")) {
            service.mediaPlayPause(); return;
        }

        // "open [app]"
        Matcher openApp = Pattern.compile("^open (.+)$").matcher(cmd);
        if (openApp.find() && !openApp.group(1).startsWith("tutorial") && !openApp.group(1).startsWith("settings")) {
            service.openApp(openApp.group(1).trim());
            return;
        }

        // "search for [query]"
        Matcher searchFor = Pattern.compile("search(?:\\s+for)?\\s+(.+)").matcher(cmd);
        if (searchFor.find()) {
            service.searchInApp(searchFor.group(1).trim());
            return;
        }

        // "call [number]"
        Matcher callNum = Pattern.compile("call (.+)").matcher(cmd);
        if (callNum.find()) {
            service.callNumber(callNum.group(1).trim().replaceAll("[^\\d+]", ""));
            return;
        }

        if (cmd.equals("copy this phone number") || cmd.equals("copy phone number")) {
            service.copyPhoneNumber(); return;
        }

        // PIN input: "alpha bravo" etc.
        Matcher alphaBravo = Pattern.compile("^([a-z]+)\\s+([a-z]+)(?:\\s+([a-z]+))?(?:\\s+([a-z]+))?(?:\\s+enter)?$").matcher(cmd);
        if (alphaBravo.find() && isPhoneticWord(alphaBravo.group(1))) {
            inputPhoneticPin(cmd);
            return;
        }

        // ===== VOLUME / SETTINGS =====
        if (cmd.equals("turn volume up") || cmd.equals("volume up")) {
            service.adjustVolume(android.media.AudioManager.ADJUST_RAISE); return;
        }
        if (cmd.equals("turn volume down") || cmd.equals("volume down")) {
            service.adjustVolume(android.media.AudioManager.ADJUST_LOWER); return;
        }
        if (cmd.equals("mute volume") || cmd.equals("mute")) {
            service.muteVolume(); return;
        }
        if (cmd.equals("unmute volume") || cmd.equals("unmute")) {
            service.unmuteVolume(); return;
        }
        if (cmd.equals("turn ring volume up") || cmd.equals("ring volume up")) {
            service.adjustRingVolume(android.media.AudioManager.ADJUST_RAISE); return;
        }
        if (cmd.equals("turn ring volume down") || cmd.equals("ring volume down")) {
            service.adjustRingVolume(android.media.AudioManager.ADJUST_LOWER); return;
        }
        if (cmd.equals("mute ring volume") || cmd.equals("mute ringer")) {
            service.muteRingVolume(); return;
        }
        if (cmd.equals("unmute ring volume") || cmd.equals("unmute ringer")) {
            service.unmuteRingVolume(); return;
        }
        if (cmd.equals("turn on bluetooth")) { service.toggleBluetooth(true); return; }
        if (cmd.equals("turn off bluetooth")) { service.toggleBluetooth(false); return; }

        // ===== GESTURES: SCROLL =====
        if (cmd.equals("scroll up")) { service.performScrollUp(); return; }
        if (cmd.equals("scroll down")) { service.performScrollDown(); return; }
        if (cmd.equals("scroll left")) { service.performScrollLeft(); return; }
        if (cmd.equals("scroll right")) { service.performScrollRight(); return; }

        // "scroll [element] left/right/up/down/to top/to bottom"
        Matcher scrollElement = Pattern.compile("scroll (.+) (left|right|up|down|to top|to bottom)").matcher(cmd);
        if (scrollElement.find()) {
            String element = scrollElement.group(1);
            String direction = scrollElement.group(2);
            handleScrollElement(element, direction);
            return;
        }

        // Swipe edges
        if (cmd.equals("swipe bottom edge")) { service.performSwipeEdge("bottom"); return; }
        if (cmd.equals("swipe top edge")) { service.performSwipeEdge("top"); return; }
        if (cmd.equals("swipe left edge")) { service.performSwipeEdge("left"); return; }
        if (cmd.equals("swipe right edge")) { service.performSwipeEdge("right"); return; }

        // ===== GESTURES: TAP =====
        // "[element]" or "tap [element]"
        Matcher tapElement = Pattern.compile("^tap (.+)$").matcher(cmd);
        if (tapElement.find()) {
            String target = tapElement.group(1).trim();
            // Check if it's a number
            try {
                int num = Integer.parseInt(target);
                service.tapNodeByNumber(num);
            } catch (NumberFormatException e) {
                service.tapNodeByText(target);
            }
            return;
        }

        // "long press [element]"
        Matcher longPress = Pattern.compile("long press (.+)").matcher(cmd);
        if (longPress.find()) {
            service.longPressNodeByText(longPress.group(1).trim());
            return;
        }

        // "check [element]"
        Matcher check = Pattern.compile("^check (.+)$").matcher(cmd);
        if (check.find()) {
            service.checkNodeByText(check.group(1).trim(), true);
            return;
        }

        // "uncheck [element]"
        Matcher uncheck = Pattern.compile("^uncheck (.+)$").matcher(cmd);
        if (uncheck.find()) {
            service.checkNodeByText(uncheck.group(1).trim(), false);
            return;
        }

        // ===== GRID SELECTION =====
        if (cmd.equals("show grid")) { service.showGrid(); return; }
        if (cmd.equals("hide grid")) { service.hideGrid(); return; }
        if (cmd.equals("more squares")) { service.showFeedback("Increasing grid size"); return; }
        if (cmd.equals("fewer squares")) { service.showFeedback("Decreasing grid size"); return; }

        // "swipe up [8]", "swipe down [8]" etc. for grid
        Matcher gridSwipe = Pattern.compile("swipe (up|down|left|right) (\\d+)").matcher(cmd);
        if (gridSwipe.find()) {
            // Grid swipe - implemented as gesture from grid position
            service.showFeedback("Grid swipe " + gridSwipe.group(1) + " from " + gridSwipe.group(2));
            return;
        }

        // "pinch in [8]" / "pinch out [8]"
        Matcher pinch = Pattern.compile("pinch (in|out)(?:\\s+(\\d+))?").matcher(cmd);
        if (pinch.find()) {
            boolean pinchIn = "in".equals(pinch.group(1));
            service.performPinch(500, 900, pinchIn);
            return;
        }

        // ===== MAGNIFICATION =====
        if (cmd.equals("start zooming")) { service.startZooming(); return; }
        if (cmd.equals("stop zooming")) { service.stopZooming(); return; }
        if (cmd.equals("zoom in")) { service.zoomIn(); return; }
        if (cmd.equals("zoom out")) { service.zoomOut(); return; }
        if (cmd.equals("pan left")) { service.panLeft(); return; }
        if (cmd.equals("pan right")) { service.panRight(); return; }
        if (cmd.equals("pan up")) { service.panUp(); return; }
        if (cmd.equals("pan down")) { service.panDown(); return; }

        // Slider increment/decrement: "increment [8]" / "decrement [8]"
        Matcher increment = Pattern.compile("increment (\\d+)").matcher(cmd);
        if (increment.find()) {
            incrementSlider(Integer.parseInt(increment.group(1)));
            return;
        }
        Matcher decrement = Pattern.compile("decrement (\\d+)").matcher(cmd);
        if (decrement.find()) {
            decrementSlider(Integer.parseInt(decrement.group(1)));
            return;
        }

        // ===== TEXT EDITING =====
        if (cmd.equals("start editing")) { service.startTextEditing(); return; }
        if (cmd.equals("stop editing")) { service.stopTextEditing(); return; }
        if (cmd.equals("undo")) { service.performUndo(); return; }
        if (cmd.equals("redo")) { service.performRedo(); return; }

        // "type [text]"
        Matcher typeText = Pattern.compile("^type (.+)$").matcher(cmd);
        if (typeText.find()) {
            String text = typeText.group(1);
            lastTypedText = text;
            service.appendText(text);
            return;
        }

        // "delete all text"
        if (cmd.equals("delete all text")) { service.deleteAllText(); return; }

        // "delete [word]"
        Matcher deleteWord = Pattern.compile("^delete (.+)$").matcher(cmd);
        if (deleteWord.find()) {
            String target = deleteWord.group(1);
            if (target.equals("selected text")) {
                service.typeText(""); // delete selection
                return;
            }
            if (target.equals("to the beginning")) {
                deleteToBeginning(); return;
            }
            if (target.equals("to the end")) {
                deleteToEnd(); return;
            }
            // "delete from [x] to [y]"
            Matcher deleteBetween = Pattern.compile("from (.+) to (.+)").matcher(target);
            if (deleteBetween.find()) {
                service.replaceTextBetween(deleteBetween.group(1), deleteBetween.group(2), "");
                return;
            }
            // "delete the previous [n] words"
            Matcher delPrev = Pattern.compile("the previous (\\d+) (words|characters|sentences|lines|paragraphs)").matcher(target);
            if (delPrev.find()) {
                deletePreviousWords(Integer.parseInt(delPrev.group(1)));
                return;
            }
            // "delete the next [n] words"
            Matcher delNext = Pattern.compile("the next (\\d+) (words|characters|sentences|lines|paragraphs)").matcher(target);
            if (delNext.find()) {
                deleteNextWords(Integer.parseInt(delNext.group(1)));
                return;
            }
            service.deleteWord(target);
            return;
        }

        // "replace [x] with [y]"
        Matcher replace = Pattern.compile("replace (.+) with (.+)").matcher(cmd);
        if (replace.find()) {
            service.replaceText(replace.group(1).trim(), replace.group(2).trim());
            return;
        }

        // "replace everything between [first] and [last] with [middle]"
        Matcher replBetween = Pattern.compile("replace everything between (.+) and (.+) with (.+)").matcher(cmd);
        if (replBetween.find()) {
            service.replaceTextBetween(replBetween.group(1), replBetween.group(2), replBetween.group(3));
            return;
        }

        // "capitalize [word]"
        Matcher capitalize = Pattern.compile("capitalize (.+)").matcher(cmd);
        if (capitalize.find()) {
            service.capitalizeWord(capitalize.group(1).trim());
            return;
        }

        // "uppercase [word]"
        Matcher uppercase = Pattern.compile("uppercase (.+)").matcher(cmd);
        if (uppercase.find()) {
            service.uppercaseWord(uppercase.group(1).trim());
            return;
        }

        // "lowercase [word]"
        Matcher lowercase = Pattern.compile("lowercase (.+)").matcher(cmd);
        if (lowercase.find()) {
            service.lowercaseWord(lowercase.group(1).trim());
            return;
        }

        // "format email"
        if (cmd.equals("format email")) { formatEmail(); return; }

        // Insert commands: "insert [text] before [anchor]"
        Matcher insertBefore = Pattern.compile("insert (.+) before (.+)").matcher(cmd);
        if (insertBefore.find()) {
            service.insertTextBefore(insertBefore.group(1), insertBefore.group(2));
            return;
        }
        Matcher insertAfter = Pattern.compile("insert (.+) after (.+)").matcher(cmd);
        if (insertAfter.find()) {
            service.insertTextAfter(insertAfter.group(1), insertAfter.group(2));
            return;
        }
        Matcher insertBetween = Pattern.compile("insert (.+) between (.+) and (.+)").matcher(cmd);
        if (insertBetween.find()) {
            service.insertTextAfter(insertBetween.group(1), insertBetween.group(2));
            return;
        }

        // Select commands
        if (cmd.equals("select all text") || cmd.equals("select all")) {
            service.selectAllText(); return;
        }
        if (cmd.equals("unselect text") || cmd.equals("deselect")) {
            service.moveCursorToEnd(); return;
        }
        if (cmd.equals("select to the beginning")) {
            // Select from cursor to beginning
            selectToBeginning(); return;
        }
        if (cmd.equals("select to the end")) {
            selectToEnd(); return;
        }
        Matcher selectWord = Pattern.compile("^select (.+)$").matcher(cmd);
        if (selectWord.find()) {
            String target = selectWord.group(1);
            // "select from [x] to [y]"
            Matcher selectRange = Pattern.compile("from (.+) to (.+)").matcher(target);
            if (selectRange.find()) {
                selectRange(selectRange.group(1), selectRange.group(2));
                return;
            }
            // "select the previous [n] words"
            Matcher selPrev = Pattern.compile("the previous (\\d+)").matcher(target);
            if (selPrev.find()) {
                selectPreviousWords(Integer.parseInt(selPrev.group(1)));
                return;
            }
            Matcher selNext = Pattern.compile("the next (\\d+)").matcher(target);
            if (selNext.find()) {
                selectNextWords(Integer.parseInt(selNext.group(1)));
                return;
            }
            service.selectText(target);
            return;
        }

        // Cursor movement
        if (cmd.equals("go to the beginning")) { service.moveCursorToBeginning(); return; }
        if (cmd.equals("go to the end")) { service.moveCursorToEnd(); return; }

        Matcher moveAfter = Pattern.compile("move after (.+)").matcher(cmd);
        if (moveAfter.find()) { moveCursorAfter(moveAfter.group(1)); return; }

        Matcher moveBefore = Pattern.compile("move before (.+)").matcher(cmd);
        if (moveBefore.find()) { moveCursorBefore(moveBefore.group(1)); return; }

        Matcher moveBetween = Pattern.compile("move between (.+) and (.+)").matcher(cmd);
        if (moveBetween.find()) { moveCursorAfter(moveBetween.group(1)); return; }

        Matcher rightWords = Pattern.compile("right (\\d+) (words|characters|sentences|lines|paragraphs)").matcher(cmd);
        if (rightWords.find()) { moveCursorRight(Integer.parseInt(rightWords.group(1))); return; }

        Matcher leftWords = Pattern.compile("left (\\d+) (words|characters|sentences|lines|paragraphs)").matcher(cmd);
        if (leftWords.find()) { moveCursorLeft(Integer.parseInt(leftWords.group(1))); return; }

        // Punctuation helpers: "big comma small" etc.
        if (matchPunctuation(cmd)) return;

        // Fallback: if no verb required, try to tap by text
        if (!requireVerbs) {
            if (service.tapNodeByText(cmd)) return;
        }

        // Try number (just saying the number taps that labeled element)
        try {
            int num = Integer.parseInt(cmd.trim());
            service.tapNodeByNumber(num);
            return;
        } catch (NumberFormatException ignored) {}

        // No match - show feedback
        service.showFeedback("Unknown command: " + rawCommand);
        Log.w(TAG, "No handler for command: " + cmd);
    }

    // ==================== HELPER DISPATCH ====================

    private void handleScrollElement(String element, String direction) {
        // Try to find the element and scroll it
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return;
        // For now, perform global scroll
        switch (direction) {
            case "up": service.performScrollUp(); break;
            case "down": service.performScrollDown(); break;
            case "left": service.performScrollLeft(); break;
            case "right": service.performScrollRight(); break;
            case "to top": service.performScrollToTop(); break;
            case "to bottom": service.performScrollToBottom(); break;
        }
        root.recycle();
    }

    private void incrementSlider(int number) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused != null) {
            focused.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            focused.recycle();
        }
    }

    private void decrementSlider(int number) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused != null) {
            focused.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            focused.recycle();
        }
    }

    private void deleteToBeginning() {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        Bundle selArgs = new Bundle();
        selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
        selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                focused.getTextSelectionStart());
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs);
        // Then clear selection
        Bundle textArgs = new Bundle();
        CharSequence text = focused.getText();
        if (text != null) {
            int end = focused.getTextSelectionEnd();
            textArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text.subSequence(end, text.length()));
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, textArgs);
        }
        focused.recycle();
    }

    private void deleteToEnd() {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text != null) {
            int cursorPos = focused.getTextSelectionStart();
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text.subSequence(0, cursorPos));
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        }
        focused.recycle();
    }

    private void deletePreviousWords(int count) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int cursor = focused.getTextSelectionStart();
        String before = text.subSequence(0, cursor).toString();
        String[] words = before.split("\\s+");
        int deleteCount = Math.min(count, words.length);
        String keep = "";
        if (words.length > deleteCount) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length - deleteCount; i++) {
                if (i > 0) sb.append(" ");
                sb.append(words[i]);
            }
            keep = sb.toString();
            if (!before.endsWith(" ")) keep += " ";
        }
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                keep + text.subSequence(cursor, text.length()));
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        focused.recycle();
    }

    private void deleteNextWords(int count) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int cursor = focused.getTextSelectionStart();
        String after = text.subSequence(cursor, text.length()).toString().trim();
        String[] words = after.split("\\s+");
        String remaining = "";
        if (words.length > count) {
            StringBuilder sb = new StringBuilder();
            for (int i = count; i < words.length; i++) {
                if (i > count) sb.append(" ");
                sb.append(words[i]);
            }
            remaining = sb.toString();
        }
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text.subSequence(0, cursor) + remaining);
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        focused.recycle();
    }

    private void selectToBeginning() {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        int cursor = focused.getTextSelectionStart();
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor);
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        focused.recycle();
    }

    private void selectToEnd() {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int cursor = focused.getTextSelectionStart();
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor);
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length());
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        focused.recycle();
    }

    private void selectRange(String from, String to) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        String str = text.toString();
        int start = str.indexOf(from);
        int end = str.indexOf(to);
        if (start >= 0 && end >= 0) {
            Bundle args = new Bundle();
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start);
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end + to.length());
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        }
        focused.recycle();
    }

    private void selectPreviousWords(int count) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int cursor = focused.getTextSelectionStart();
        String before = text.subSequence(0, cursor).toString();
        String[] words = before.split("\\s+");
        int start = cursor;
        int wordCount = 0;
        for (int i = before.length() - 1; i >= 0; i--) {
            if (i == 0 || before.charAt(i - 1) == ' ') {
                wordCount++;
                start = i;
                if (wordCount >= count) break;
            }
        }
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start);
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor);
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        focused.recycle();
    }

    private void selectNextWords(int count) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int cursor = focused.getTextSelectionStart();
        String after = text.subSequence(cursor, text.length()).toString();
        String[] words = after.split("\\s+");
        int end = cursor;
        for (int i = 0; i < Math.min(count, words.length); i++) {
            end += words[i].length() + (i < words.length - 1 ? 1 : 0);
        }
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor);
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end);
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        focused.recycle();
    }

    private void moveCursorAfter(String word) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int idx = text.toString().indexOf(word);
        if (idx >= 0) {
            int pos = idx + word.length();
            Bundle args = new Bundle();
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos);
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos);
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        }
        focused.recycle();
    }

    private void moveCursorBefore(String word) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int idx = text.toString().indexOf(word);
        if (idx >= 0) {
            Bundle args = new Bundle();
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, idx);
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, idx);
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        }
        focused.recycle();
    }

    private void moveCursorRight(int words) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int cursor = focused.getTextSelectionStart();
        String after = text.subSequence(cursor, text.length()).toString();
        String[] wordArr = after.split("\\s+");
        int pos = cursor;
        for (int i = 0; i < Math.min(words, wordArr.length); i++) {
            pos += wordArr[i].length() + 1;
        }
        pos = Math.min(pos, text.length());
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos);
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos);
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        focused.recycle();
    }

    private void moveCursorLeft(int words) {
        AccessibilityNodeInfo focused = service.findFocusedEditable();
        if (focused == null) return;
        CharSequence text = focused.getText();
        if (text == null) return;
        int cursor = focused.getTextSelectionStart();
        String before = text.subSequence(0, cursor).toString();
        String[] wordArr = before.split("\\s+");
        int pos = cursor;
        for (int i = 0; i < Math.min(words, wordArr.length); i++) {
            if (wordArr.length - 1 - i >= 0) {
                pos -= wordArr[wordArr.length - 1 - i].length() + 1;
            }
        }
        pos = Math.max(0, pos);
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos);
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos);
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        focused.recycle();
    }

    private boolean matchPunctuation(String cmd) {
        // "[big] comma [small]" -> type big, then comma, then small
        Matcher commaMatcher = Pattern.compile("(.+) comma (.+)").matcher(cmd);
        if (commaMatcher.find()) {
            // This is dictation with punctuation embedded
            String text = cmd.replace(" comma ", ", ")
                    .replace(" period", ".")
                    .replace(" exclamation mark", "!")
                    .replace(" question mark", "?")
                    .replace(" colon ", ": ")
                    .replace(" semicolon ", "; ")
                    .replace(" new line ", "\n");
            service.appendText(text);
            return true;
        }
        return false;
    }

    private void formatEmail() {
        // Basic email formatting - paragraph breaks etc.
        service.showFeedback("Email format applied");
    }

    private void inputPhoneticPin(String cmd) {
        String[] words = cmd.split("\\s+");
        StringBuilder digits = new StringBuilder();
        for (String w : words) {
            if (w.equals("enter")) break;
            int digit = phoneticToDigit(w);
            if (digit >= 0) digits.append(digit);
        }
        for (char c : digits.toString().toCharArray()) {
            service.tapNodeByText(String.valueOf(c));
        }
    }

    private int phoneticToDigit(String word) {
        switch (word.toLowerCase()) {
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
            default:
                try { return Integer.parseInt(word); } catch (Exception e) { return -1; }
        }
    }

    private boolean isPhoneticWord(String word) {
        return phoneticToDigit(word) >= 0;
    }

    private void showAllCommands() {
        Intent i = new Intent(service, com.claude.voiceaccess.ui.CommandsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        service.startActivity(i);
    }

    private void openTutorial() {
        Intent i = new Intent(service, com.claude.voiceaccess.ui.TutorialActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        service.startActivity(i);
    }

    private void openVoiceAccessSettings() {
        Intent i = new Intent(service, com.claude.voiceaccess.ui.SettingsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        service.startActivity(i);
    }

    private void openFeedback() {
        service.showFeedback("Feedback: contact support");
    }

    private void showHelp() {
        String screenContent = service.readScreenContents();
        String helpMsg = "Current screen: " + currentActivityTitle
                + "\nSay 'all commands' to see all available commands";
        service.showFeedback(helpMsg);
    }

    private void repeatLastAction() {
        if (!lastAction.isEmpty()) {
            execute(lastAction);
        }
    }
}
