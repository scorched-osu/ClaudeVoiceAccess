package com.claude.voiceaccess.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class LabelOverlayManager {

    private final Context context;
    private final WindowManager windowManager;
    private final List<View> overlayViews = new ArrayList<>();

    public LabelOverlayManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
    }

    public void showLabels(AccessibilityNodeInfo root) {
        hideLabels();
        if (root == null) return;

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collectLabelableNodes(root, nodes);

        int idx = 1;
        for (AccessibilityNodeInfo node : nodes) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()) continue;

            String labelText = getLabelText(node, idx);
            if (labelText.isEmpty()) { idx++; continue; }

            View badge = createLabelBadge(labelText, idx);
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
            } catch (Exception ignored) {}
            idx++;
            if (idx > 50) break;
        }
    }

    public void hideLabels() {
        for (View v : overlayViews) {
            try { windowManager.removeView(v); } catch (Exception e) {}
        }
        overlayViews.clear();
    }

    private String getLabelText(AccessibilityNodeInfo node, int fallbackNum) {
        if (node.getContentDescription() != null && node.getContentDescription().length() > 0) {
            return node.getContentDescription().toString();
        }
        if (node.getText() != null && node.getText().length() > 0) {
            return node.getText().toString();
        }
        // Try to get resource id part as label
        if (node.getViewIdResourceName() != null) {
            String id = node.getViewIdResourceName();
            int slash = id.lastIndexOf('/');
            if (slash >= 0) return id.substring(slash + 1).replace("_", " ");
        }
        return String.valueOf(fallbackNum);
    }

    private View createLabelBadge(String text, int number) {
        TextView tv = new TextView(context);
        tv.setText(number + " " + text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(10f);
        tv.setMaxLines(1);
        tv.setPadding(8, 3, 8, 3);
        tv.setBackgroundColor(0xDD1A3D6B);
        return tv;
    }

    private void collectLabelableNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list) {
        if (node == null) return;
        if (node.isClickable() && node.isVisibleToUser()) {
            list.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            collectLabelableNodes(child, list);
        }
    }
}
