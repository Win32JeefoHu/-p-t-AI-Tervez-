package com.nagyattila.epitoaitervezo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Izometrikus 3D előnézet elemekkel. Copyright © Nagy Attila */
public class Model3DView extends View {
    private List<float[]> footprint;
    private List<BuildingElement> elements = new ArrayList<>();
    private float wallHeightM = 2.7f;
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint doorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint winPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Model3DView(Context c) { super(c); init(); }
    public Model3DView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(dp(2));
        edgePaint.setColor(Color.parseColor("#18C7D9"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#4418C7D9"));
        topPaint.setStyle(Paint.Style.FILL);
        topPaint.setColor(Color.parseColor("#6618C7D9"));
        floorPaint.setStyle(Paint.Style.FILL);
        floorPaint.setColor(Color.parseColor("#88A67C52"));
        doorPaint.setStyle(Paint.Style.FILL);
        doorPaint.setColor(Color.parseColor("#88FF9F43"));
        winPaint.setStyle(Paint.Style.FILL);
        winPaint.setColor(Color.parseColor("#664FC3F7"));
        stairPaint.setStyle(Paint.Style.FILL);
        stairPaint.setColor(Color.parseColor("#AACE93D8"));
        textPaint.setColor(Color.parseColor("#F4FAFC"));
        textPaint.setTextSize(dp(13));
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setFootprint(List<float[]> verts, float heightM) {
        footprint = verts;
        elements = PlanStore.get().getElements();
        wallHeightM = heightM > 0.5f ? heightM : 2.7f;
        invalidate();
    }

    public void setElements(List<BuildingElement> list) {
        elements = list != null ? new ArrayList<>(list) : new ArrayList<>();
        invalidate();
    }

    private float[] iso(float x, float y, float z, float cx, float cy, float scale) {
        float ix = (x - z) * 0.8660254f;
        float iy = y + (x + z) * 0.5f;
        return new float[]{cx + ix * scale, cy - iy * scale};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#0B1824"));
        if (footprint == null || footprint.size() < 3) {
            canvas.drawText("Nincs alaprajz — rajzolj a 2D modulban", dp(16), getHeight() / 2f, textPaint);
            return;
        }
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (float[] v : footprint) {
            minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
            minZ = Math.min(minZ, v[1]); maxZ = Math.max(maxZ, v[1]);
        }
        float span = Math.max(Math.max(maxX - minX, maxZ - minZ), wallHeightM);
        float scale = Math.min(getWidth(), getHeight()) * 0.55f / Math.max(span, 1f);
        float cx = getWidth() / 2f;
        float cy = getHeight() * 0.62f;
        float ox = (minX + maxX) / 2f;
        float oz = (minZ + maxZ) / 2f;
        int n = footprint.size();

        boolean hasFlooring = false;
        for (BuildingElement e : elements) {
            if (e.type == BuildingElement.Type.FLOORING) { hasFlooring = true; break; }
        }

        Path floor = new Path();
        for (int i = 0; i < n; i++) {
            float[] a = footprint.get(i);
            float[] p = iso(a[0] - ox, 0.02f, a[1] - oz, cx, cy, scale);
            if (i == 0) floor.moveTo(p[0], p[1]); else floor.lineTo(p[0], p[1]);
        }
        floor.close();
        canvas.drawPath(floor, hasFlooring ? floorPaint : fillPaint);
        canvas.drawPath(floor, edgePaint);

        for (int i = 0; i < n; i++) {
            float[] a = footprint.get(i);
            float[] b = footprint.get((i + 1) % n);
            float ax = a[0] - ox, az = a[1] - oz;
            float bx = b[0] - ox, bz = b[1] - oz;
            float[] p0 = iso(ax, 0, az, cx, cy, scale);
            float[] p1 = iso(bx, 0, bz, cx, cy, scale);
            float[] p2 = iso(bx, wallHeightM, bz, cx, cy, scale);
            float[] p3 = iso(ax, wallHeightM, az, cx, cy, scale);
            Path face = new Path();
            face.moveTo(p0[0], p0[1]);
            face.lineTo(p1[0], p1[1]);
            face.lineTo(p2[0], p2[1]);
            face.lineTo(p3[0], p3[1]);
            face.close();
            canvas.drawPath(face, fillPaint);
            canvas.drawPath(face, edgePaint);

            for (BuildingElement e : elements) {
                if (!e.isOpening() || e.wallIndex != i) continue;
                drawOpening(canvas, a, b, ox, oz, cx, cy, scale, e);
            }
        }

        for (BuildingElement e : elements) {
            if (e.type == BuildingElement.Type.STAIR) drawStairs(canvas, e, ox, oz, cx, cy, scale);
        }

        Path top = new Path();
        for (int i = 0; i < n; i++) {
            float[] a = footprint.get(i);
            float[] p = iso(a[0] - ox, wallHeightM, a[1] - oz, cx, cy, scale);
            if (i == 0) top.moveTo(p[0], p[1]); else top.lineTo(p[0], p[1]);
        }
        top.close();
        canvas.drawPath(top, topPaint);
        canvas.drawPath(top, edgePaint);

        float area = PlanStore.polygonArea(footprint);
        int doors = PlanStore.get().countByType(BuildingElement.Type.DOOR);
        int windows = PlanStore.get().countByType(BuildingElement.Type.WINDOW);
        int stairs = PlanStore.get().countByType(BuildingElement.Type.STAIR);
        String info = String.format(Locale.forLanguageTag("hu-HU"),
                "Extrudált modell • H=%.1f m • %.2f m² • Ajtó:%d Ablak:%d Lépcső:%d%s",
                wallHeightM, area, doors, windows, stairs, hasFlooring ? " • Padló" : "");
        canvas.drawText(info, dp(12), dp(24), textPaint);
    }

    private void drawOpening(Canvas canvas, float[] a, float[] b,
                             float ox, float oz, float cx, float cy, float scale,
                             BuildingElement e) {
        float dx = b[0] - a[0], dz = b[1] - a[1];
        float len = (float) Math.hypot(dx, dz);
        if (len < 0.01f) return;
        float t = Math.max(0.05f, Math.min(0.95f, e.tAlongWall));
        float half = Math.min(e.width * 0.5f, len * 0.4f);
        float t0 = Math.max(0.02f, t - half / len);
        float t1 = Math.min(0.98f, t + half / len);
        float sill = e.type == BuildingElement.Type.WINDOW ? Math.max(0f, e.sillHeight) : 0f;
        float topY = Math.min(wallHeightM - 0.05f, sill + Math.max(0.4f, e.height));
        float botY = Math.min(sill, topY - 0.2f);
        float x0 = a[0] + dx * t0 - ox, z0 = a[1] + dz * t0 - oz;
        float x1 = a[0] + dx * t1 - ox, z1 = a[1] + dz * t1 - oz;
        float[] q0 = iso(x0, botY, z0, cx, cy, scale);
        float[] q1 = iso(x1, botY, z1, cx, cy, scale);
        float[] q2 = iso(x1, topY, z1, cx, cy, scale);
        float[] q3 = iso(x0, topY, z0, cx, cy, scale);
        Path cut = new Path();
        cut.moveTo(q0[0], q0[1]);
        cut.lineTo(q1[0], q1[1]);
        cut.lineTo(q2[0], q2[1]);
        cut.lineTo(q3[0], q3[1]);
        cut.close();
        canvas.drawPath(cut, e.type == BuildingElement.Type.WINDOW ? winPaint : doorPaint);
        canvas.drawPath(cut, edgePaint);
    }

    private void drawStairs(Canvas canvas, BuildingElement e,
                            float ox, float oz, float cx, float cy, float scale) {
        float[] pos = e.resolvePosition(footprint);
        float sx = pos[0] - ox, sz = pos[1] - oz;
        int steps = Math.min(Math.max(e.stepCount, 3), 20);
        float rise = Math.max(0.12f, e.stepRise);
        float depth = Math.max(0.2f, e.stepDepth);
        float halfW = Math.max(0.4f, e.width) * 0.5f;
        for (int i = 0; i < steps; i++) {
            float y0 = i * rise;
            float y1 = (i + 1) * rise;
            if (y1 > wallHeightM) break;
            float z0 = sz + i * depth * 0.15f;
            float z1 = sz + (i + 1) * depth * 0.15f;
            float[] a = iso(sx - halfW, y0, z0, cx, cy, scale);
            float[] b = iso(sx + halfW, y0, z0, cx, cy, scale);
            float[] c = iso(sx + halfW, y1, z1, cx, cy, scale);
            float[] d = iso(sx - halfW, y1, z1, cx, cy, scale);
            Path step = new Path();
            step.moveTo(a[0], a[1]);
            step.lineTo(b[0], b[1]);
            step.lineTo(c[0], c[1]);
            step.lineTo(d[0], d[1]);
            step.close();
            canvas.drawPath(step, stairPaint);
            canvas.drawPath(step, edgePaint);
        }
    }
}
