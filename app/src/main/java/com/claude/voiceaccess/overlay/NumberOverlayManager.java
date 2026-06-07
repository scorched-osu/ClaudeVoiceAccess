package com.claude.voiceaccess.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import com.claude.voiceaccess.service.ClaudeAccessibilityService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NumberOverlayManager {

    private final Context context;
    private final WindowManager windowManager;
    private final List<View> overlayViews = new ArrayList<>();
    private final Map<Integer, AccessibilityNodeInfo> numberToNode = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public NumberOverlayManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
    }

    public void showNumbers(AccessibilityNodeInfo root) {
        hideNumbers();
        if (root == null) return;

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collectClickableNodes(root, nodes);

        int number = 1;
        for (AccessibilityNodeInfo node : nodes) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()) continue;

            final int num = number;
            numberToNode.put(num, node);

            View badge = createNumberBadge(num);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = bounds.left;
            params.y = bounds.top;

            try {
                windowManager.addView(badge, params);
                overlayViews.add(badge);
            } catch (Exception e) {
                // ignore
            }
            number++;
            if (number > 50) break; // Max 50 numbered elements
        }
    }

    public void hideNumbers() {
        for (View v : overlayViews) {
            try { windowManager.removeView(v); } catch (Exception e) {}
        }
        overlayViews.clear();
        for (AccessibilityNodeInfo node : numberToNode.values()) {
            if (node != null) try { node.recycle(); } catch (Exception e) {}
        }
        numberToNode.clear();
    }

    public boolean tapNumber(int number) {
        AccessibilityNodeInfo node = numberToNode.get(number);
        if (node != null) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            ClaudeAccessibilityService svc = ClaudeAccessibilityService.instance;
            if (svc != null) {
                svc.performTap(bounds.centerX(), bounds.centerY());
                return true;
            }
        }
        return false;
    }

    public String describeNumber(int number) {
        AccessibilityNodeInfo node = numberToNode.get(number);
        if (node == null) return "No element at number " + number;
        StringBuilder desc = new StringBuilder();
        if (node.getText() != null) desc.append(node.getText());
        if (node.getContentDescription() != null) {
            if (desc.length() > 0) desc.append(" - ");
            desc.append(node.getContentDescription());
        }
        if (node.getClassName() != null) {
            String cls = node.getClassName().toString();
            cls = cls.substring(cls.lastIndexOf('.') + 1);
            if (desc.length() > 0) desc.append(" (").append(cls).append(")");
            else desc.append(cls);
        }
        return desc.length() > 0 ? desc.toString() : "Element " + number;
    }

    private View createNumberBadge(int number) {
        TextView tv = new TextView(context);
        tv.setText(String.valueOf(number));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11f);
        tv.setPadding(6, 2, 6, 2);
        tv.setBackgroundColor(0xCC1A73E8); // Blue with transparency
        return tv;
    }

    private void collectClickableNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list) {
        if (node == null) return;
        if ((node.isClickable() || node.isLongClickable() || node.isEditable())
                && node.isVisibleToUser()) {
            list.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            collectClickableNodes(child, list);
        }
    }
}
