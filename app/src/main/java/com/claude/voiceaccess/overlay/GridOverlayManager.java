package com.claude.voiceaccess.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.claude.voiceaccess.service.ClaudeAccessibilityService;

import java.util.ArrayList;
import java.util.List;

public class GridOverlayManager {

    private final Context context;
    private final WindowManager windowManager;
    private View gridView;
    private int cols = 4;
    private int rows = 8;
    private List<Rect> gridCells = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());

    public GridOverlayManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
    }

    public void showGrid() {
        hideGrid();
        gridView = createGridView();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        try {
            windowManager.addView(gridView, params);
        } catch (Exception e) {
            // ignore
        }
    }

    public void hideGrid() {
        if (gridView != null) {
            try { windowManager.removeView(gridView); } catch (Exception e) {}
            gridView = null;
        }
        gridCells.clear();
    }

    public void moreSquares() {
        cols++;
        rows++;
        if (gridView != null) {
            showGrid();
        }
    }

    public void fewerSquares() {
        cols = Math.max(2, cols - 1);
        rows = Math.max(4, rows - 1);
        if (gridView != null) {
            showGrid();
        }
    }

    /** Tap on a grid square by its number */
    public boolean tapGridSquare(int squareNumber) {
        if (squareNumber < 1 || squareNumber > gridCells.size()) return false;
        Rect cell = gridCells.get(squareNumber - 1);
        ClaudeAccessibilityService svc = ClaudeAccessibilityService.instance;
        if (svc != null) {
            svc.performTap(cell.centerX(), cell.centerY());
            return true;
        }
        return false;
    }

    public Rect getCellBounds(int squareNumber) {
        if (squareNumber < 1 || squareNumber > gridCells.size()) return null;
        return gridCells.get(squareNumber - 1);
    }

    private View createGridView() {
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenW = size.x;
        int screenH = size.y;

        int cellW = screenW / cols;
        int cellH = screenH / rows;

        gridCells.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                gridCells.add(new Rect(
                        c * cellW, r * cellH,
                        (c + 1) * cellW, (r + 1) * cellH
                ));
            }
        }

        return new View(context) {
            private final Paint linePaint;
            private final Paint numPaint;
            private final Paint bgPaint;

            {
                linePaint = new Paint();
                linePaint.setColor(Color.argb(150, 100, 100, 200));
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeWidth(2f);
                linePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{8, 8}, 0));

                numPaint = new Paint();
                numPaint.setColor(Color.argb(180, 80, 80, 180));
                numPaint.setTextSize(28f);
                numPaint.setAntiAlias(true);

                bgPaint = new Paint();
                bgPaint.setColor(Color.argb(5, 26, 115, 232));
                bgPaint.setStyle(Paint.Style.FILL);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                for (int i = 0; i < gridCells.size(); i++) {
                    Rect cell = gridCells.get(i);
                    canvas.drawRect(cell, bgPaint);
                    canvas.drawRect(cell, linePaint);
                    // Draw number in center
                    String num = String.valueOf(i + 1);
                    float textW = numPaint.measureText(num);
                    canvas.drawText(num,
                            cell.centerX() - textW / 2,
                            cell.centerY() + numPaint.getTextSize() / 3,
                            numPaint);
                }
            }
        };
    }
}
