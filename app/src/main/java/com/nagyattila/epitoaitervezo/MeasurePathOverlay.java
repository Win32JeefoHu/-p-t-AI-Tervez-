package com.nagyattila.epitoaitervezo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MeasurePathOverlay extends View {
    private float aimX;
    private float aimY;
    private final List<float[]> confirmed = new ArrayList<>();
    private final List<float[]> depthDots = new ArrayList<>();
    private final Paint confirmedPaint = new Paint(1);
    private final Paint markerFill = new Paint(1);
    private final Paint markerPaint = new Paint(1);
    private final Path path = new Path();
    private final Paint previewPaint = new Paint(1);
    private final Paint depthDotPaint = new Paint(1);
    private boolean showPreview;

    public MeasurePathOverlay(Context context) {
        super(context);
        init();
    }

    public MeasurePathOverlay(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public MeasurePathOverlay(Context context, AttributeSet attributeSet, int i2) {
        super(context, attributeSet, i2);
        init();
    }

    private float dp(float f2) {
        return f2 * getResources().getDisplayMetrics().density;
    }

    private void init() {
        setWillNotDraw(false);
        Paint.Style stroke = Paint.Style.STROKE;
        confirmedPaint.setStyle(stroke);
        confirmedPaint.setStrokeWidth(dp(3.0f));
        confirmedPaint.setColor(Color.parseColor("#18C7D9"));
        confirmedPaint.setPathEffect(new DashPathEffect(new float[]{dp(12.0f), dp(8.0f)}, 0.0f));
        confirmedPaint.setStrokeCap(Paint.Cap.ROUND);
        confirmedPaint.setStrokeJoin(Paint.Join.ROUND);
        previewPaint.setStyle(stroke);
        previewPaint.setStrokeWidth(dp(2.5f));
        previewPaint.setColor(Color.parseColor("#FFEB3B"));
        previewPaint.setPathEffect(new DashPathEffect(new float[]{dp(10.0f), dp(7.0f)}, 0.0f));
        previewPaint.setStrokeCap(Paint.Cap.ROUND);
        markerPaint.setStyle(stroke);
        markerPaint.setStrokeWidth(dp(2.0f));
        markerPaint.setColor(Color.parseColor("#18C7D9"));
        markerFill.setStyle(Paint.Style.FILL);
        markerFill.setColor(Color.parseColor("#6618C7D9"));
        depthDotPaint.setStyle(Paint.Style.FILL);
        depthDotPaint.setColor(Color.parseColor("#88FF9F43"));
    }

    public void clearPath() {
        confirmed.clear();
        depthDots.clear();
        showPreview = false;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float depthR = dp(2.5f);
        for (float[] d : depthDots) {
            canvas.drawCircle(d[0], d[1], depthR, depthDotPaint);
        }
        int size = confirmed.size();
        if (size >= 2) {
            path.reset();
            float[] first = confirmed.get(0);
            path.moveTo(first[0], first[1]);
            for (int i2 = 1; i2 < size; i2++) {
                float[] pt = confirmed.get(i2);
                path.lineTo(pt[0], pt[1]);
            }
            if (size >= 3) {
                path.lineTo(first[0], first[1]);
            }
            canvas.drawPath(path, confirmedPaint);
        }
        if (showPreview && size >= 1) {
            float[] last = confirmed.get(size - 1);
            canvas.drawLine(last[0], last[1], aimX, aimY, previewPaint);
        }
        float r = dp(8.0f);
        for (float[] pt : confirmed) {
            canvas.drawCircle(pt[0], pt[1], r, markerFill);
            canvas.drawCircle(pt[0], pt[1], r, markerPaint);
        }
    }

    public void setAim(float f2, float f3, boolean z2) {
        aimX = f2;
        aimY = f3;
        showPreview = z2;
        invalidate();
    }

    public void setConfirmedScreenPoints(List<float[]> list) {
        confirmed.clear();
        if (list != null) {
            for (float[] fArr : list) {
                if (fArr != null && fArr.length >= 2) {
                    confirmed.add(new float[]{fArr[0], fArr[1]});
                }
            }
        }
        invalidate();
    }

    public void setDepthDots(List<float[]> list) {
        depthDots.clear();
        if (list != null) {
            for (float[] fArr : list) {
                if (fArr != null && fArr.length >= 2) {
                    depthDots.add(new float[]{fArr[0], fArr[1]});
                }
            }
        }
        invalidate();
    }
}
