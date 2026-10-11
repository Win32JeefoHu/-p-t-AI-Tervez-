package com.nagyattila.epitoaitervezo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 2D alaprajz + CAD. Copyright © Nagy Attila */
public class FloorPlanView extends View {
    public interface Listener { void onPlanChanged(); }
    public interface PlacementListener {
        void onPlace(float mx, float mz, int wallIndex, float tAlong);
    }

    private final List<float[]> vertices = new ArrayList<>();
    private final List<BuildingElement> elements = new ArrayList<>();
    private boolean closed;
    private Listener listener;
    private PlacementListener placementListener;
    private boolean placementMode;
    private boolean drawMode = true;
    private boolean snapEnabled = true;
    private boolean showDims = true;
    private boolean showLayers = true;
    private int selectedElement = -1;
    private float metersPerPixel = 0.02f;
    private float originXPx;
    private float originYPx;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flooringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vertexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint doorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public FloorPlanView(Context c) { super(c); init(); }
    public FloorPlanView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setWillNotDraw(false);
        gridPaint.setColor(Color.parseColor("#1A2A3A"));
        wallPaint.setColor(Color.parseColor("#18C7D9"));
        wallPaint.setStyle(Paint.Style.STROKE);
        wallPaint.setStrokeWidth(dp(3));
        previewPaint.setColor(Color.parseColor("#FFEB3B"));
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeWidth(dp(2));
        previewPaint.setPathEffect(new DashPathEffect(new float[]{dp(10), dp(6)}, 0));
        fillPaint.setColor(Color.parseColor("#3318C7D9"));
        fillPaint.setStyle(Paint.Style.FILL);
        flooringPaint.setColor(Color.parseColor("#44A67C52"));
        flooringPaint.setStyle(Paint.Style.FILL);
        vertexPaint.setColor(Color.parseColor("#FF9F43"));
        textPaint.setColor(Color.parseColor("#F4FAFC"));
        textPaint.setTextSize(dp(12));
        dimPaint.setColor(Color.parseColor("#90CAF9"));
        dimPaint.setTextSize(dp(11));
        dimPaint.setTextAlign(Paint.Align.CENTER);
        doorPaint.setColor(Color.parseColor("#FF9F43"));
        doorPaint.setStyle(Paint.Style.STROKE);
        doorPaint.setStrokeWidth(dp(2.5f));
        windowPaint.setColor(Color.parseColor("#4FC3F7"));
        windowPaint.setStyle(Paint.Style.STROKE);
        windowPaint.setStrokeWidth(dp(2.5f));
        stairPaint.setColor(Color.parseColor("#CE93D8"));
        stairPaint.setStyle(Paint.Style.STROKE);
        stairPaint.setStrokeWidth(dp(2));
        gapPaint.setColor(Color.parseColor("#0B1824"));
        gapPaint.setStyle(Paint.Style.STROKE);
        gapPaint.setStrokeWidth(dp(5));
        selPaint.setColor(Color.parseColor("#FFFF00"));
        selPaint.setStyle(Paint.Style.STROKE);
        selPaint.setStrokeWidth(dp(2));
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setListener(Listener l) { listener = l; }
    public void setDrawMode(boolean enabled) { drawMode = enabled; }
    public void setSnapEnabled(boolean e) { snapEnabled = e; invalidate(); }
    public boolean isSnapEnabled() { return snapEnabled; }
    public void setShowDims(boolean e) { showDims = e; invalidate(); }
    public void setShowLayers(boolean e) { showLayers = e; invalidate(); }
    public boolean isShowLayers() { return showLayers; }
    public float getMetersPerPixel() { return metersPerPixel; }
    public void setMetersPerPixel(float mpp) {
        metersPerPixel = Math.max(0.0005f, mpp);
        invalidate();
        notifyChanged();
    }

    public void setPlacementMode(boolean enabled, PlacementListener pl) {
        placementMode = enabled;
        placementListener = pl;
        invalidate();
    }

    public void setElements(List<BuildingElement> list) {
        elements.clear();
        if (list != null) elements.addAll(list);
        invalidate();
    }

    public List<BuildingElement> getElements() {
        return new ArrayList<>(elements);
    }

    public void addElement(BuildingElement e) {
        if (e != null) {
            elements.add(e);
            selectedElement = elements.size() - 1;
            invalidate();
            notifyChanged();
        }
    }

    public boolean copySelected() {
        if (selectedElement < 0 || selectedElement >= elements.size()) return false;
        BuildingElement src = elements.get(selectedElement);
        BuildingElement copy = new BuildingElement(src.type);
        copy.width = src.width; copy.height = src.height; copy.sillHeight = src.sillHeight;
        copy.stepCount = src.stepCount; copy.stepDepth = src.stepDepth; copy.stepRise = src.stepRise;
        copy.material = src.material;
        copy.x = src.x + 0.3f; copy.y = src.y + 0.3f;
        copy.wallIndex = src.wallIndex;
        copy.tAlongWall = Math.min(0.95f, src.tAlongWall + 0.08f);
        elements.add(copy);
        selectedElement = elements.size() - 1;
        invalidate();
        notifyChanged();
        return true;
    }

    public List<float[]> getVertices() {
        List<float[]> copy = new ArrayList<>(vertices.size());
        for (float[] v : vertices) copy.add(new float[]{v[0], v[1]});
        return copy;
    }

    public boolean isClosed() { return closed; }

    public float getAreaM2() {
        return closed ? PlanStore.polygonArea(vertices) : 0f;
    }

    public void setVertices(List<float[]> verts, boolean closedFlag) {
        vertices.clear();
        if (verts != null) {
            for (float[] v : verts) {
                if (v != null && v.length >= 2) vertices.add(new float[]{v[0], v[1]});
            }
        }
        closed = closedFlag && vertices.size() >= 3;
        fitView();
        invalidate();
        notifyChanged();
    }

    public void clear() {
        vertices.clear();
        closed = false;
        selectedElement = -1;
        invalidate();
        notifyChanged();
    }

    public void undoLast() {
        if (placementMode) return;
        if (closed) closed = false;
        else if (!vertices.isEmpty()) vertices.remove(vertices.size() - 1);
        invalidate();
        notifyChanged();
    }

    public void closeRoom() {
        if (vertices.size() >= 3) {
            closed = true;
            invalidate();
            notifyChanged();
        }
    }

    private void notifyChanged() {
        if (listener != null) listener.onPlanChanged();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitView();
    }

    private void fitView() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        originXPx = w / 2f;
        originYPx = h / 2f;
        if (vertices.isEmpty()) {
            metersPerPixel = 8f / Math.min(w, h);
            return;
        }
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (float[] v : vertices) {
            minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
            minZ = Math.min(minZ, v[1]); maxZ = Math.max(maxZ, v[1]);
        }
        float spanX = Math.max(maxX - minX, 1f);
        float spanZ = Math.max(maxZ - minZ, 1f);
        metersPerPixel = Math.max(spanX * 1.2f / w, spanZ * 1.2f / h);
        originXPx = w / 2f - ((minX + maxX) / 2f) / metersPerPixel;
        originYPx = h / 2f - ((minZ + maxZ) / 2f) / metersPerPixel;
    }

    private float[] screenToMeters(float sx, float sy) {
        return new float[]{(sx - originXPx) * metersPerPixel, (sy - originYPx) * metersPerPixel};
    }

    private float[] metersToScreen(float mx, float mz) {
        return new float[]{originXPx + mx / metersPerPixel, originYPx + mz / metersPerPixel};
    }

    private float[] snapPoint(float mx, float mz) {
        if (!snapEnabled || vertices.isEmpty()) return new float[]{mx, mz};
        float[] last = vertices.get(vertices.size() - 1);
        float dx = mx - last[0], dz = mz - last[1];
        float len = (float) Math.hypot(dx, dz);
        if (len < 1e-4f) return new float[]{mx, mz};
        double ang = Math.atan2(dz, dx);
        double step = Math.PI / 4.0; // 45°
        double snapped = Math.round(ang / step) * step;
        return new float[]{
                last[0] + (float) (Math.cos(snapped) * len),
                last[1] + (float) (Math.sin(snapped) * len)
        };
    }

    private float[] nearestWall(float mx, float mz) {
        int n = vertices.size();
        if (!closed || n < 2) return null;
        float bestDist = Float.MAX_VALUE;
        int bestI = -1;
        float bestT = 0.5f;
        for (int i = 0; i < n; i++) {
            float[] a = vertices.get(i);
            float[] b = vertices.get((i + 1) % n);
            float dx = b[0] - a[0], dz = b[1] - a[1];
            float len2 = dx * dx + dz * dz;
            float t = len2 < 1e-8f ? 0.5f : ((mx - a[0]) * dx + (mz - a[1]) * dz) / len2;
            t = Math.max(0f, Math.min(1f, t));
            float px = a[0] + dx * t, pz = a[1] + dz * t;
            float dist = (float) Math.hypot(mx - px, mz - pz);
            if (dist < bestDist) { bestDist = dist; bestI = i; bestT = t; }
        }
        return new float[]{bestI, bestT, bestDist};
    }

    private int hitElement(float mx, float mz) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            BuildingElement e = elements.get(i);
            if (e.type == BuildingElement.Type.FLOORING) continue;
            float[] pos = e.resolvePosition(vertices);
            if (Math.hypot(mx - pos[0], mz - pos[1]) < 0.45) return i;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event);
        float[] m = screenToMeters(event.getX(), event.getY());

        if (placementMode && placementListener != null) {
            float[] wall = nearestWall(m[0], m[1]);
            int wi = -1;
            float t = 0.5f;
            if (wall != null && wall[2] < 0.8f) {
                wi = (int) wall[0];
                t = wall[1];
                float[] a = vertices.get(wi);
                float[] b = vertices.get((wi + 1) % vertices.size());
                m[0] = a[0] + (b[0] - a[0]) * t;
                m[1] = a[1] + (b[1] - a[1]) * t;
            }
            placementListener.onPlace(m[0], m[1], wi, t);
            return true;
        }

        if (closed) {
            int hit = hitElement(m[0], m[1]);
            if (hit >= 0) {
                selectedElement = hit;
                invalidate();
                return true;
            }
            return true;
        }

        if (!drawMode) return true;

        if (vertices.size() >= 3) {
            float[] first = metersToScreen(vertices.get(0)[0], vertices.get(0)[1]);
            float dx = event.getX() - first[0], dy = event.getY() - first[1];
            if (dx * dx + dy * dy < dp(28) * dp(28)) {
                closed = true;
                invalidate();
                notifyChanged();
                return true;
            }
        }
        float[] snapped = snapPoint(m[0], m[1]);
        vertices.add(snapped);
        invalidate();
        notifyChanged();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#0B1824"));
        if (showLayers) drawGrid(canvas);
        int n = vertices.size();
        if (n == 0) {
            canvas.drawText("Érintsd a vásznat a sarkok felvételéhez (45/90 snap)", dp(16), getHeight() / 2f, textPaint);
            return;
        }

        boolean hasFlooring = false;
        String floorMat = "Padló";
        if (showLayers) {
            for (BuildingElement e : elements) {
                if (e.type == BuildingElement.Type.FLOORING) {
                    hasFlooring = true;
                    if (e.material != null && !e.material.isEmpty()) floorMat = e.material;
                }
            }
        }

        path.reset();
        float[] s0 = metersToScreen(vertices.get(0)[0], vertices.get(0)[1]);
        path.moveTo(s0[0], s0[1]);
        for (int i = 1; i < n; i++) {
            float[] s = metersToScreen(vertices.get(i)[0], vertices.get(i)[1]);
            path.lineTo(s[0], s[1]);
        }
        if (closed) {
            path.close();
            canvas.drawPath(path, hasFlooring ? flooringPaint : fillPaint);
            canvas.drawPath(path, wallPaint);
            if (hasFlooring) {
                Paint lab = new Paint(textPaint);
                lab.setTextAlign(Paint.Align.CENTER);
                float cx = 0, cy = 0;
                for (float[] v : vertices) {
                    float[] s = metersToScreen(v[0], v[1]);
                    cx += s[0]; cy += s[1];
                }
                canvas.drawText(floorMat, cx / n, cy / n, lab);
            }
            if (showLayers) {
                for (BuildingElement e : elements) {
                    if (!e.isOpening() || e.wallIndex < 0) continue;
                    int i = e.wallIndex % n;
                    float[] a = vertices.get(i), b = vertices.get((i + 1) % n);
                    float dx = b[0] - a[0], dz = b[1] - a[1];
                    float len = (float) Math.hypot(dx, dz);
                    if (len < 0.01f) continue;
                    float ux = dx / len, uz = dz / len;
                    float half = Math.min(e.width, len * 0.9f) * 0.5f;
                    float t = Math.max(0f, Math.min(1f, e.tAlongWall));
                    float mx = a[0] + dx * t, mz = a[1] + dz * t;
                    float[] sA = metersToScreen(mx - ux * half, mz - uz * half);
                    float[] sB = metersToScreen(mx + ux * half, mz + uz * half);
                    canvas.drawLine(sA[0], sA[1], sB[0], sB[1], gapPaint);
                }
            }
        } else {
            canvas.drawPath(path, wallPaint);
            if (n >= 2) {
                float[] last = metersToScreen(vertices.get(n - 1)[0], vertices.get(n - 1)[1]);
                canvas.drawLine(last[0], last[1], s0[0], s0[1], previewPaint);
            }
        }

        if (showDims && n >= 2) {
            int edges = closed ? n : n - 1;
            for (int i = 0; i < edges; i++) {
                float[] a = vertices.get(i);
                float[] b = vertices.get((i + 1) % n);
                float len = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
                float[] sa = metersToScreen(a[0], a[1]);
                float[] sb = metersToScreen(b[0], b[1]);
                canvas.drawText(String.format(Locale.forLanguageTag("hu-HU"), "%.2f m", len),
                        (sa[0] + sb[0]) / 2f, (sa[1] + sb[1]) / 2f - dp(6), dimPaint);
            }
        }

        if (showLayers) {
            for (int i = 0; i < elements.size(); i++) {
                drawElement(canvas, elements.get(i), n);
                if (i == selectedElement) {
                    float[] pos = elements.get(i).resolvePosition(vertices);
                    float[] s = metersToScreen(pos[0], pos[1]);
                    canvas.drawCircle(s[0], s[1], dp(16), selPaint);
                }
            }
        }

        for (int i = 0; i < n; i++) {
            float[] s = metersToScreen(vertices.get(i)[0], vertices.get(i)[1]);
            canvas.drawCircle(s[0], s[1], dp(7), vertexPaint);
            canvas.drawText(String.valueOf(i + 1), s[0] + dp(10), s[1] - dp(6), textPaint);
        }
        if (closed) {
            canvas.drawText(String.format(Locale.forLanguageTag("hu-HU"), "Terület: %.2f m²", getAreaM2()),
                    dp(12), getHeight() - dp(16), textPaint);
        }
        if (placementMode) {
            canvas.drawText("Elem elhelyezés: koppints a vászonra / falra", dp(12), dp(22), textPaint);
        }
    }

    private void drawElement(Canvas canvas, BuildingElement e, int nVerts) {
        if (e == null || e.type == null || e.type == BuildingElement.Type.FLOORING) return;
        float[] pos = e.resolvePosition(vertices);
        float[] s = metersToScreen(pos[0], pos[1]);
        switch (e.type) {
            case DOOR: {
                float angle = 0f;
                if (e.wallIndex >= 0 && closed && nVerts >= 2) {
                    float[] a = vertices.get(e.wallIndex % nVerts);
                    float[] b = vertices.get((e.wallIndex + 1) % nVerts);
                    angle = (float) Math.atan2(b[1] - a[1], b[0] - a[0]);
                }
                float r = Math.max(e.width, 0.6f) / metersPerPixel * 0.55f;
                canvas.save();
                canvas.translate(s[0], s[1]);
                canvas.rotate((float) Math.toDegrees(angle));
                canvas.drawArc(new RectF(0, -r, r * 2, r), 180, 90, false, doorPaint);
                canvas.drawLine(0, 0, 0, -r, doorPaint);
                canvas.restore();
                canvas.drawText("A", s[0] + dp(4), s[1] - dp(4), textPaint);
                break;
            }
            case WINDOW: {
                float half = Math.max(e.width, 0.5f) * 0.5f / metersPerPixel;
                float angle = 0f;
                if (e.wallIndex >= 0 && closed && nVerts >= 2) {
                    float[] a = vertices.get(e.wallIndex % nVerts);
                    float[] b = vertices.get((e.wallIndex + 1) % nVerts);
                    angle = (float) Math.atan2(b[1] - a[1], b[0] - a[0]);
                }
                canvas.save();
                canvas.translate(s[0], s[1]);
                canvas.rotate((float) Math.toDegrees(angle));
                canvas.drawLine(-half, -dp(3), half, -dp(3), windowPaint);
                canvas.drawLine(-half, dp(3), half, dp(3), windowPaint);
                canvas.drawLine(0, -dp(3), 0, dp(3), windowPaint);
                canvas.restore();
                canvas.drawText("W", s[0] + dp(4), s[1] - dp(4), textPaint);
                break;
            }
            case STAIR: {
                float w = Math.min(Math.max(e.width, 0.8f) / metersPerPixel, dp(50));
                float d = Math.min(Math.max(1, e.stepCount) * Math.max(0.15f, e.stepDepth) / metersPerPixel, dp(80));
                float left = s[0] - w / 2f, top = s[1] - d / 2f;
                int steps = Math.min(Math.max(e.stepCount, 3), 16);
                for (int i = 0; i <= steps; i++) {
                    float yy = top + d * i / (float) steps;
                    canvas.drawLine(left, yy, left + w, yy, stairPaint);
                }
                canvas.drawRect(left, top, left + w, top + d, stairPaint);
                canvas.drawText("L", s[0] + w / 2f + dp(4), s[1], textPaint);
                break;
            }
            default: break;
        }
    }

    private void drawGrid(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float stepPx = 1f / metersPerPixel;
        if (stepPx < dp(20)) stepPx = 2f / metersPerPixel;
        for (float x = originXPx % stepPx; x < w; x += stepPx) canvas.drawLine(x, 0, x, h, gridPaint);
        for (float y = originYPx % stepPx; y < h; y += stepPx) canvas.drawLine(0, y, w, y, gridPaint);
    }

    public Bitmap exportBitmap(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        layout(0, 0, width, height);
        draw(new Canvas(bmp));
        return bmp;
    }
}
