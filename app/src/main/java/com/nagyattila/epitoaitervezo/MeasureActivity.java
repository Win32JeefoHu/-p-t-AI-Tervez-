package com.nagyattila.epitoaitervezo;

import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.media.Image;
import android.content.pm.PackageManager;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.nagyattila.epitoaitervezo.ar.BackgroundRenderer;
import com.nagyattila.epitoaitervezo.ar.DisplayRotationHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MeasureActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final int CAMERA_PERMISSION_CODE = 21;
    private static final String TAG = "MeasureActivity";
    private LinearLayout buttonBar;
    private Button clearButton;
    private TextView crosshair;
    private DisplayRotationHelper displayRotationHelper;
    private volatile boolean fingerActive;
    private volatile float fingerX;
    private volatile float fingerY;
    private boolean firstFrameShown;
    private LinearLayout hud;
    private boolean installRequested;
    private TextView messageBody;
    private LinearLayout messagePanel;
    private TextView messageTitle;
    private MeasurePathOverlay pathOverlay;
    private volatile boolean pendingPlace;
    private Button pointButton;
    private TextView result;
    private TextView segmentsView;
    private Session session;
    private boolean sessionConfigured;
    private TextView status;
    private GLSurfaceView surfaceView;
    private String lastArException = "";
    private boolean preferDepth;
    private boolean depthConfigured;
    private volatile boolean depthFrameReady;
    private int depthSampleCounter;
    private TextView depthBadge;
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final List<Anchor> anchors = new ArrayList();
    private final float[] viewMatrix = new float[16];
    private final float[] projMatrix = new float[16];
    private final float[] worldHom = new float[4];
    private final float[] viewPos = new float[4];
    private final float[] clip = new float[4];

    /* renamed from: com.nagyattila.epitoaitervezo.MeasureActivity$1, reason: invalid class name */

    private String buildDistanceSummary() {
        if (this.anchors.size() < 2) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        float f2 = 0.0f;
        int i2 = 1;
        while (i2 < this.anchors.size()) {
            float distanceMeters = distanceMeters(this.anchors.get(i2 - 1).getPose(), this.anchors.get(i2).getPose());
            f2 += distanceMeters;
            int i3 = R.string.measure_segment_line;
            Integer valueOf = Integer.valueOf(i2);
            i2++;
            sb.append(getString(i3, valueOf, Integer.valueOf(i2), formatMeters(distanceMeters)));
            sb.append('\n');
        }
        if (this.anchors.size() >= 3) {
            List<Anchor> list = this.anchors;
            float distanceMeters2 = distanceMeters(list.get(list.size() - 1).getPose(), this.anchors.get(0).getPose());
            f2 += distanceMeters2;
            sb.append(getString(R.string.measure_segment_line, Integer.valueOf(this.anchors.size()), 1, formatMeters(distanceMeters2)));
            sb.append('\n');
            sb.append(String.format(Locale.forLanguageTag("hu-HU"), "Terület: %.2f m²\n", Float.valueOf(PlanStore.get().getMeasuredPolygonAreaM2())));
        }
        sb.append(getString(R.string.measure_total, formatMeters(f2)));
        return sb.toString();
    }

    private void clearAllPoints() {
        Iterator<Anchor> it = this.anchors.iterator();
        while (it.hasNext()) {
            it.next().detach();
        }
        this.anchors.clear();
        this.pendingPlace = false;
        this.pathOverlay.clearPath();
        PlanStore.get().setMeasuredPolygonAreaM2(0.0f);
        this.result.setText(R.string.measure_distance_zero);
        this.segmentsView.setText("");
        updateUiForCount();
        this.status.setText(R.string.measure_move_phone);
    }

    private static float distanceMeters(Pose pose, Pose pose2) {
        float tx = pose.tx() - pose2.tx();
        float ty = pose.ty() - pose2.ty();
        float tz = pose.tz() - pose2.tz();
        return (float) Math.sqrt((tz * tz) + (ty * ty) + (tx * tx));
    }

    private String formatMeters(float f2) {
        return String.format(Locale.forLanguageTag("hu-HU"), "%.2f m", Float.valueOf(f2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$0(View view, MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 0) {
            if (actionMasked != 1) {
                if (actionMasked != 2) {
                    if (actionMasked != 3) {
                        return false;
                    }
                }
            }
            this.fingerActive = false;
            return true;
        }
        this.fingerActive = true;
        this.fingerX = motionEvent.getX();
        this.fingerY = motionEvent.getY();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(View view) {
        if (this.session == null) {
            showArError("Nincs AR munkamenet", this.lastArException);
        } else {
            this.pendingPlace = true;
            this.status.setText(R.string.measure_placing);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(View view) {
        clearAllPoints();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onDrawFrame$6(List list, float f2, float f3, boolean z2) {
        this.pathOverlay.setConfirmedScreenPoints(list);
        this.pathOverlay.setAim(f2, f3, z2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onDrawFrame$7() {
        showArError("Kamera hiba", this.lastArException);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onSurfaceCreated$5() {
        showArError("OpenGL hiba", this.lastArException);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showArErrorWithBasicOption$4(String str, View view) {
        offerBasicFallback(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showCameraUi$3(View view) {
        if (this.session == null) {
            showArErrorWithBasicOption("Nincs AR munkamenet", this.lastArException);
        } else {
            this.pendingPlace = true;
            this.status.setText(R.string.measure_placing);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$tryPlacePoint$8() {
        this.status.setText(R.string.measure_no_plane);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$tryPlacePoint$9(int i2, String str, String str2) {
        this.status.setText(getString(R.string.measure_point_set, Integer.valueOf(i2)));
        this.result.setText(str);
        this.segmentsView.setText(str2);
        updateUiForCount();
    }

    private String lastSegmentText() {
        if (this.anchors.size() < 2) {
            return getString(R.string.measure_distance_zero);
        }
        List<Anchor> list = this.anchors;
        String string = getString(R.string.measure_last_segment, formatMeters(distanceMeters(list.get(list.size() - 2).getPose(), this.anchors.get(list.size() - 1).getPose())));
        if (this.anchors.size() < 3) {
            return string;
        }
        return string + " • " + String.format(Locale.forLanguageTag("hu-HU"), "%.2f m²", Float.valueOf(PlanStore.get().getMeasuredPolygonAreaM2()));
    }

    private void offerBasicFallback(String str) {
        Log.i(TAG, "Falling back to BasicMeasureActivity: " + str);
        startActivity(new Intent(this, (Class<?>) BasicMeasureActivity.class));
        finish();
    }

    private void publishMeasuredArea() {
        if (this.anchors.size() < 3) {
            PlanStore.get().setMeasuredPolygonAreaM2(0.0f);
            return;
        }
        ArrayList arrayList = new ArrayList(this.anchors.size());
        Iterator<Anchor> it = this.anchors.iterator();
        while (it.hasNext()) {
            Pose pose = it.next().getPose();
            arrayList.add(new float[]{pose.tx(), pose.tz()});
        }
        PlanStore.get().setMeasuredPolygonAreaM2(PlanStore.polygonArea(arrayList));
    }

    private void showArError(String str, String str2) {
        Log.e(TAG, "AR error UI: " + str + " | " + str2);
        this.messagePanel.setVisibility(0);
        this.messageTitle.setText(str);
        TextView textView = this.messageBody;
        if (str2 == null || str2.isEmpty()) {
            str2 = getString(R.string.measure_ar_fail_body);
        }
        textView.setText(str2);
        this.surfaceView.setVisibility(8);
        this.hud.setVisibility(8);
        this.buttonBar.setVisibility(8);
        this.crosshair.setVisibility(8);
        this.pathOverlay.setVisibility(8);
        this.pointButton.setEnabled(false);
    }

    private void showArErrorWithBasicOption(String str, String str2) {
        showArError(str, str2);
        this.buttonBar.setVisibility(0);
        this.pointButton.setEnabled(true);
        this.pointButton.setText(R.string.chooser_basic_btn);
        this.pointButton.setOnClickListener(v -> offerBasicFallback(str2));
        this.clearButton.setVisibility(8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showCameraUi() {
        this.messagePanel.setVisibility(8);
        this.surfaceView.setVisibility(0);
        this.hud.setVisibility(0);
        this.buttonBar.setVisibility(0);
        this.crosshair.setVisibility(0);
        this.pathOverlay.setVisibility(0);
        this.clearButton.setVisibility(0);
        this.pointButton.setEnabled(true);
        this.pointButton.setText(this.anchors.isEmpty() ? R.string.measure_btn_first : R.string.measure_btn_next);
        this.pointButton.setOnClickListener(this::lambda$showCameraUi$3);
    }

    private void showLoading() {
        this.messagePanel.setVisibility(0);
        this.messageTitle.setText(R.string.measure_loading_title);
        this.messageBody.setText(R.string.measure_loading_body);
        this.surfaceView.setVisibility(0);
        this.hud.setVisibility(8);
        this.buttonBar.setVisibility(8);
        this.crosshair.setVisibility(8);
        this.pathOverlay.setVisibility(8);
    }


    /** Ritka mintavétel a mélységképből → overlay pontok (nem LiDAR-claim). */
    private void sampleDepthOverlay(Frame frame) {
        if (!this.depthConfigured || this.pathOverlay == null) return;
        Image depth = null;
        try {
            depth = frame.acquireDepthImage16Bits();
            if (depth == null) return;
            this.depthFrameReady = true;
            int w = depth.getWidth();
            int h = depth.getHeight();
            Image.Plane plane0 = depth.getPlanes()[0];
            java.nio.ByteBuffer buf = plane0.getBuffer();
            int rowStride = plane0.getRowStride();
            int pixelStride = plane0.getPixelStride();
            int viewW = Math.max(this.surfaceView.getWidth(), 1);
            int viewH = Math.max(this.surfaceView.getHeight(), 1);
            java.util.ArrayList<float[]> dots = new java.util.ArrayList<>(48);
            for (int gy = 1; gy <= 6; gy++) {
                for (int gx = 1; gx <= 8; gx++) {
                    int x = (w * gx) / 9;
                    int y = (h * gy) / 7;
                    int idx = y * rowStride + x * pixelStride;
                    if (idx + 1 >= buf.limit()) continue;
                    int depthMm = (buf.get(idx) & 0xff) | ((buf.get(idx + 1) & 0xff) << 8);
                    if (depthMm < 200 || depthMm > 8000) continue;
                    float sx = (x / (float) w) * viewW;
                    float sy = (y / (float) h) * viewH;
                    dots.add(new float[]{sx, sy});
                }
            }
            final java.util.List<float[]> out = dots;
            runOnUiThread(() -> {
                this.pathOverlay.setDepthDots(out);
                if (this.depthBadge != null && this.depthBadge.getVisibility() != android.view.View.VISIBLE) {
                    this.depthBadge.setVisibility(android.view.View.VISIBLE);
                    this.depthBadge.setText(R.string.measure_depth_badge);
                }
            });
        } catch (com.google.ar.core.exceptions.NotYetAvailableException e) {
            // még nincs mélységkép
        } catch (Throwable th) {
            Log.w(TAG, "depth sample", th);
        } finally {
            if (depth != null) {
                try { depth.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private void tryPlacePoint(Frame frame) {
        HitResult hitResult;
        List<HitResult> hitTest = frame.hitTest(this.fingerActive ? this.fingerX : Math.max(this.surfaceView.getWidth(), 1) / 2.0f, this.fingerActive ? this.fingerY : Math.max(this.surfaceView.getHeight(), 1) / 2.0f);
        HitResult depthHit = null;
        HitResult planeHit = null;
        for (HitResult hr : hitTest) {
            if (this.depthConfigured && hr.getTrackable() instanceof DepthPoint) {
                if (depthHit == null) depthHit = hr;
            } else if (hr.getTrackable() instanceof Plane) {
                Plane plane = (Plane) hr.getTrackable();
                if (plane.isPoseInPolygon(hr.getHitPose()) && plane.getTrackingState() == TrackingState.TRACKING) {
                    if (planeHit == null) planeHit = hr;
                }
            }
        }
        if (this.depthConfigured && depthHit != null) {
            hitResult = depthHit;
        } else if (planeHit != null) {
            hitResult = planeHit;
        } else if (!hitTest.isEmpty()) {
            hitResult = hitTest.get(0);
        } else {
            hitResult = null;
        }
        if (hitResult == null) {
            runOnUiThread(this::lambda$tryPlacePoint$8);
            return;
        }
        this.anchors.add(hitResult.createAnchor());
        publishMeasuredArea();
        final int size = this.anchors.size();
        final String buildDistanceSummary = buildDistanceSummary();
        final String lastSegmentText = lastSegmentText();
        runOnUiThread(new Runnable() { // from class: com.nagyattila.epitoaitervezo.q
            @Override // java.lang.Runnable
            public final void run() {
                MeasureActivity.this.lambda$tryPlacePoint$9(size, lastSegmentText, buildDistanceSummary);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryStartAr() {
        try {
            ArCoreApk.Availability checkAvailability = ArCoreApk.getInstance().checkAvailability(this);
            Log.i(TAG, "ARCore availability=" + checkAvailability);
            if (checkAvailability.isTransient()) {
                this.messageBody.setText(R.string.measure_ar_checking);
                this.surfaceView.postDelayed(this::tryStartAr, 200L);
                return;
            }
            if (checkAvailability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
                Log.w(TAG, "Device not ARCore capable — offering basic measure");
                offerBasicFallback(getString(R.string.chooser_ar_unavailable_hint));
                return;
            }
            if (ArCoreApk.getInstance().requestInstall(this, !this.installRequested) == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                this.installRequested = true;
                this.messageTitle.setText(R.string.measure_arcore_install);
                this.messageBody.setText(R.string.measure_loading_body);
                Log.i(TAG, "ARCore INSTALL_REQUESTED");
                return;
            }
            if (this.session == null) {
                this.session = new Session(this);
                Log.i(TAG, "ARCore Session created");
            }
            if (!this.sessionConfigured) {
                Config config = new Config(this.session);
                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
                config.setFocusMode(Config.FocusMode.AUTO);
                this.depthConfigured = false;
                boolean depthOk = this.session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
                if (depthOk) {
                    // Elérhető Depth API → bekapcsolás (nem LiDAR-claim nem támogatott gépen)
                    config.setDepthMode(Config.DepthMode.AUTOMATIC);
                    this.depthConfigured = true;
                    Log.i(TAG, "DepthMode.AUTOMATIC enabled (preferDepth=" + this.preferDepth + ")");
                } else {
                    config.setDepthMode(Config.DepthMode.DISABLED);
                    if (this.preferDepth) {
                        Log.w(TAG, "Depth requested but not supported on this device/session");
                    }
                }
                this.session.configure(config);
                this.sessionConfigured = true;
            }
            this.surfaceView.setVisibility(0);
            this.surfaceView.onResume();
            this.displayRotationHelper.onResume();
            this.session.resume();
            Log.i(TAG, "ARCore session resumed");
            showCameraUi();
            if (this.depthConfigured) {
                this.status.setText(R.string.measure_depth_active);
                if (this.depthBadge != null) {
                    this.depthBadge.setVisibility(android.view.View.VISIBLE);
                    this.depthBadge.setText(R.string.measure_depth_badge);
                }
            } else {
                this.status.setText(R.string.measure_move_phone);
                if (this.depthBadge != null) {
                    this.depthBadge.setVisibility(android.view.View.GONE);
                }
            }
        } catch (Throwable th) {
            this.lastArException = th.getClass().getSimpleName() + ": " + String.valueOf(th.getMessage());
            StringBuilder sb = new StringBuilder("ARCore session creation/resume FAILED: ");
            sb.append(this.lastArException);
            Log.e(TAG, sb.toString(), th);
            Session session = this.session;
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable unused) {
                }
                this.session = null;
                this.sessionConfigured = false;
            }
            if (ArCoreHelper.isFatalUnsupportedFailure(th)) {
                offerBasicFallback(this.lastArException);
            } else {
                showArErrorWithBasicOption("ARCore hiba", this.lastArException);
            }
        }
    }

    private void updateUiForCount() {
        this.pointButton.setText(this.anchors.isEmpty() ? R.string.measure_btn_first : R.string.measure_btn_next);
    }

    private boolean worldToScreen(float f2, float f3, float f4, int i2, int i3, float[] fArr) {
        float[] fArr2 = this.worldHom;
        fArr2[0] = f2;
        fArr2[1] = f3;
        fArr2[2] = f4;
        fArr2[3] = 1.0f;
        Matrix.multiplyMV(this.viewPos, 0, this.viewMatrix, 0, fArr2, 0);
        Matrix.multiplyMV(this.clip, 0, this.projMatrix, 0, this.viewPos, 0);
        if (Math.abs(this.clip[3]) < 1.0E-6f) {
            return false;
        }
        float[] fArr3 = this.clip;
        float f5 = fArr3[0];
        float f6 = fArr3[3];
        float f7 = fArr3[1] / f6;
        fArr[0] = ((f5 / f6) + 1.0f) * 0.5f * i2;
        fArr[1] = (1.0f - f7) * 0.5f * i3;
        return f6 > 0.0f;
    }

    @Override // androidx.fragment.app.F, androidx.activity.ComponentActivity, o.l, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_measure);
        this.messagePanel = (LinearLayout) findViewById(R.id.messagePanel);
        this.messageTitle = (TextView) findViewById(R.id.messageTitle);
        this.messageBody = (TextView) findViewById(R.id.messageBody);
        this.hud = (LinearLayout) findViewById(R.id.hud);
        this.buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
        this.status = (TextView) findViewById(R.id.status);
        this.result = (TextView) findViewById(R.id.result);
        this.segmentsView = (TextView) findViewById(R.id.segments);
        this.crosshair = (TextView) findViewById(R.id.crosshair);
        this.pointButton = (Button) findViewById(R.id.pointButton);
        this.clearButton = (Button) findViewById(R.id.clearButton);
        this.surfaceView = (GLSurfaceView) findViewById(R.id.cameraSurface);
        this.pathOverlay = (MeasurePathOverlay) findViewById(R.id.pathOverlay);
        this.displayRotationHelper = new DisplayRotationHelper(this);
        this.surfaceView.setPreserveEGLContextOnPause(true);
        this.surfaceView.setEGLContextClientVersion(2);
        this.surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.surfaceView.setRenderer(this);
        this.surfaceView.setRenderMode(1);
        this.pathOverlay.setOnTouchListener(this::lambda$onCreate$0);
        this.pointButton.setOnClickListener(this::lambda$onCreate$1);
        this.clearButton.setOnClickListener(this::lambda$onCreate$2);
        this.depthBadge = (TextView) findViewById(R.id.depthBadge);
        this.preferDepth = getIntent() != null
                && getIntent().getBooleanExtra(MeasureChooserActivity.EXTRA_PREFER_DEPTH, false);
        // AR mód is: ha van depth támogatás, automatikusan bekapcsolható később a sessionnél
        if (!this.preferDepth && getIntent() != null
                && getIntent().getBooleanExtra("auto_depth_if_available", false)) {
            this.preferDepth = true;
        }
        showLoading();
        updateUiForCount();
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.F, android.app.Activity
    public void onDestroy() {
        clearAllPoints();
        Session session = this.session;
        if (session != null) {
            try {
                session.close();
            } catch (Throwable unused) {
            }
            this.session = null;
            this.sessionConfigured = false;
        }
        super.onDestroy();
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(16640);
        if (this.session == null || !this.backgroundRenderer.isTextureInitialized()) {
            return;
        }
        this.displayRotationHelper.updateSessionIfNeeded(this.session);
        try {
            this.session.setCameraTextureName(this.backgroundRenderer.getTextureId());
            Frame update = this.session.update();
            Camera camera = update.getCamera();
            this.backgroundRenderer.draw(update);
            if (!this.firstFrameShown && update.getTimestamp() != 0) {
                this.firstFrameShown = true;
                runOnUiThread(this::showCameraUi);
            }
            if (this.depthConfigured) {
                this.depthSampleCounter++;
                if ((this.depthSampleCounter & 7) == 0) {
                    sampleDepthOverlay(update);
                }
            }
            if (camera.getTrackingState() == TrackingState.TRACKING && this.pendingPlace) {
                this.pendingPlace = false;
                tryPlacePoint(update);
            }
            int max = Math.max(this.surfaceView.getWidth(), 1);
            int max2 = Math.max(this.surfaceView.getHeight(), 1);
            camera.getViewMatrix(this.viewMatrix, 0);
            camera.getProjectionMatrix(this.projMatrix, 0, 0.1f, 100.0f);
            final ArrayList arrayList = new ArrayList(this.anchors.size());
            for (Anchor anchor : this.anchors) {
                if (anchor.getTrackingState() == TrackingState.TRACKING) {
                    Pose pose = anchor.getPose();
                    float[] fArr = new float[2];
                    if (worldToScreen(pose.tx(), pose.ty(), pose.tz(), max, max2, fArr)) {
                        arrayList.add(fArr);
                    }
                }
            }
            boolean z2 = this.fingerActive;
            final float f2 = z2 ? this.fingerX : max / 2.0f;
            final float f3 = z2 ? this.fingerY : max2 / 2.0f;
            final boolean z3 = !arrayList.isEmpty();
            runOnUiThread(new Runnable() { // from class: com.nagyattila.epitoaitervezo.n
                @Override // java.lang.Runnable
                public final void run() {
                    MeasureActivity.this.lambda$onDrawFrame$6(arrayList, f2, f3, z3);
                }
            });
        } catch (CameraNotAvailableException e) {
            String str = "CameraNotAvailableException: " + e.getMessage();
            this.lastArException = str;
            Log.e(TAG, str, e);
            runOnUiThread(this::lambda$onDrawFrame$7);
        } catch (Throwable th) {
            this.lastArException = th.getClass().getSimpleName() + ": " + th.getMessage();
            Log.e(TAG, "onDrawFrame", th);
        }
    }

    @Override // androidx.fragment.app.F, android.app.Activity
    public void onPause() {
        super.onPause();
        try {
            if (this.session != null) {
                this.displayRotationHelper.onPause();
                this.surfaceView.onPause();
                this.session.pause();
            }
        } catch (Throwable th) {
            Log.e(TAG, "onPause", th);
        }
    }

    @Override // androidx.fragment.app.F, androidx.activity.ComponentActivity, android.app.Activity
    public void onRequestPermissionsResult(int i2, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i2, strArr, iArr);
        if (i2 == CAMERA_PERMISSION_CODE) {
            if (iArr.length <= 0 || iArr[0] != 0) {
                showArError(getString(R.string.measure_camera_permission_title), getString(R.string.measure_camera_denied));
            } else {
                showLoading();
                tryStartAr();
            }
        }
    }

    @Override // androidx.fragment.app.F, android.app.Activity
    public void onResume() {
        super.onResume();
        this.firstFrameShown = false;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showArError(getString(R.string.measure_camera_permission_title), getString(R.string.measure_camera_permission_body));
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            showLoading();
            tryStartAr();
        }
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 gl10, int i2, int i3) {
        this.displayRotationHelper.onSurfaceChanged(i2, i3);
        GLES20.glViewport(0, 0, i2, i3);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        GLES20.glClearColor(0.035f, 0.078f, 0.118f, 1.0f);
        try {
            this.backgroundRenderer.createOnGlThread();
        } catch (Throwable th) {
            this.lastArException = th.getClass().getSimpleName() + ": " + th.getMessage();
            Log.e(TAG, "GL background init failed", th);
            runOnUiThread(this::lambda$onSurfaceCreated$5);
        }
    }
}
