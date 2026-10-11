package com.nagyattila.epitoaitervezo;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BasicMeasureActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 42;
    private static final String TAG = "BasicMeasureActivity";
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CameraCaptureSession captureSession;
    private MeasurePathOverlay pathOverlay;
    private CaptureRequest.Builder previewRequestBuilder;
    private Size previewSize;
    private TextView result;
    private EditText scaleInput;
    private TextView segmentsView;
    private TextView status;
    private TextureView textureView;
    private final List<float[]> points = new ArrayList();
    private float refMeters = 0.0f;
    private float metersPerPixel = 0.0f;
    private final TextureView.SurfaceTextureListener surfaceListener = new TextureView.SurfaceTextureListener() { // from class: com.nagyattila.epitoaitervezo.BasicMeasureActivity.2
        @Override // android.view.TextureView.SurfaceTextureListener
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i2, int i3) {
            BasicMeasureActivity.this.openCamera();
        }

        @Override // android.view.TextureView.SurfaceTextureListener
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override // android.view.TextureView.SurfaceTextureListener
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i2, int i3) {
        }

        @Override // android.view.TextureView.SurfaceTextureListener
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new AnonymousClass3();

    /* renamed from: com.nagyattila.epitoaitervezo.BasicMeasureActivity$3, reason: invalid class name */
    public class AnonymousClass3 extends CameraDevice.StateCallback {
        public AnonymousClass3() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$0() {
            BasicMeasureActivity.this.status.setText(R.string.measure_camera_busy);
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            BasicMeasureActivity.this.cameraDevice = null;
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public void onError(CameraDevice cameraDevice, int i2) {
            cameraDevice.close();
            BasicMeasureActivity.this.cameraDevice = null;
            BasicMeasureActivity.this.runOnUiThread(this::lambda$onError$0);
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public void onOpened(CameraDevice cameraDevice) {
            BasicMeasureActivity.this.cameraDevice = cameraDevice;
            BasicMeasureActivity.this.startPreview();
        }
    }

    /* renamed from: com.nagyattila.epitoaitervezo.BasicMeasureActivity$4, reason: invalid class name */
    public class AnonymousClass4 extends CameraCaptureSession.StateCallback {
        public AnonymousClass4() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onConfigureFailed$0() {
            BasicMeasureActivity.this.status.setText(R.string.measure_camera_busy);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            BasicMeasureActivity.this.runOnUiThread(this::lambda$onConfigureFailed$0);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            if (BasicMeasureActivity.this.cameraDevice == null) {
                return;
            }
            BasicMeasureActivity.this.captureSession = cameraCaptureSession;
            try {
                BasicMeasureActivity.this.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, 4);
                cameraCaptureSession.setRepeatingRequest(BasicMeasureActivity.this.previewRequestBuilder.build(), null, BasicMeasureActivity.this.backgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(BasicMeasureActivity.TAG, "setRepeatingRequest", e);
            }
        }
    }

    private void addPoint(float f2, float f3) {
        this.points.add(new float[]{f2, f3});
        recomputeScaleFactor();
        recomputeAndPublish();
        if (this.points.size() == 1) {
            this.status.setText(R.string.basic_measure_place_second);
        } else if (this.points.size() != 2 || this.refMeters > 0.0f) {
            this.status.setText(getString(R.string.measure_point_set, Integer.valueOf(this.points.size())));
        } else {
            this.status.setText(R.string.basic_measure_enter_scale);
            this.scaleInput.requestFocus();
        }
    }

    private Size chooseOptimalSize(Size[] sizeArr, int i2, int i3) {
        if (sizeArr == null || sizeArr.length == 0) {
            return new Size(1280, 720);
        }
        final int max = Math.max(i3, 1) * Math.max(i2, 1);
        List<Size> asList = Arrays.asList(sizeArr);
        Collections.sort(asList, Comparator.comparingLong(size -> BasicMeasureActivity.lambda$chooseOptimalSize$3(max, (Size) size)));
        for (Size size : asList) {
            if (size.getWidth() >= 640 && size.getHeight() >= 480) {
                if (size.getHeight() * size.getWidth() <= 2073600) {
                    return size;
                }
            }
        }
        return (Size) asList.get(0);
    }

    private void clearAll() {
        this.points.clear();
        this.metersPerPixel = 0.0f;
        this.pathOverlay.clearPath();
        PlanStore.get().setMeasuredPolygonAreaM2(0.0f);
        this.result.setText(R.string.measure_distance_zero);
        this.segmentsView.setText("");
        this.status.setText(R.string.basic_measure_hint);
    }

    private void closeCamera() {
        CameraCaptureSession cameraCaptureSession = this.captureSession;
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            this.captureSession = null;
        }
        CameraDevice cameraDevice = this.cameraDevice;
        if (cameraDevice != null) {
            cameraDevice.close();
            this.cameraDevice = null;
        }
    }

    private String formatMeters(float f2) {
        return String.format(Locale.forLanguageTag("hu-HU"), "%.2f m", Float.valueOf(f2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ long lambda$chooseOptimalSize$3(int i2, Size size) {
        return Math.abs((size.getWidth() * size.getHeight()) - i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$0(View view, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 1) {
            addPoint(motionEvent.getX(), motionEvent.getY());
            return true;
        }
        if (motionEvent.getActionMasked() != 0 && motionEvent.getActionMasked() != 2) {
            return false;
        }
        this.pathOverlay.setAim(motionEvent.getX(), motionEvent.getY(), !this.points.isEmpty());
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(View view) {
        undoLast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(View view) {
        clearAll();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        CameraManager cameraManager = (CameraManager) getSystemService("camera");
        try {
            String pickBackCamera = pickBackCamera(cameraManager);
            this.cameraId = pickBackCamera;
            if (pickBackCamera == null) {
                this.status.setText(R.string.measure_camera_busy);
                return;
            }
            StreamConfigurationMap streamConfigurationMap = (StreamConfigurationMap) cameraManager.getCameraCharacteristics(pickBackCamera).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            this.previewSize = chooseOptimalSize(streamConfigurationMap != null ? streamConfigurationMap.getOutputSizes(SurfaceTexture.class) : null, this.textureView.getWidth(), this.textureView.getHeight());
            cameraManager.openCamera(this.cameraId, this.stateCallback, this.backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera", e);
            this.status.setText(R.string.measure_camera_busy);
        } catch (SecurityException e) {
            Log.e(TAG, "openCamera", e);
            this.status.setText(R.string.measure_camera_busy);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseScale() {
        String replace = this.scaleInput.getText() == null ? "" : this.scaleInput.getText().toString().trim().replace(',', '.');
        try {
            this.refMeters = replace.isEmpty() ? 0.0f : Float.parseFloat(replace);
        } catch (NumberFormatException unused) {
            this.refMeters = 0.0f;
        }
        recomputeScaleFactor();
    }

    private String pickBackCamera(CameraManager cameraManager) {
        String str = null;
        try {
            for (String str2 : cameraManager.getCameraIdList()) {
                Integer num = (Integer) cameraManager.getCameraCharacteristics(str2).get(CameraCharacteristics.LENS_FACING);
                if (num != null && num.intValue() == 1) {
                    return str2;
                }
                if (str == null) {
                    str = str2;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "pickBackCamera", e);
        }
        return str;
    }

    private static float pixelDistance(float[] fArr, float[] fArr2) {
        float f2 = fArr[0] - fArr2[0];
        float f3 = fArr[1] - fArr2[1];
        return (float) Math.sqrt((f3 * f3) + (f2 * f2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recomputeAndPublish() {
        float f2;
        updateOverlay();
        if (this.points.size() < 2 || this.metersPerPixel <= 0.0f) {
            if (this.points.size() < 2) {
                this.result.setText(R.string.measure_distance_zero);
                this.segmentsView.setText("");
            } else {
                List<float[]> list = this.points;
                float[] fArr = list.get(list.size() - 2);
                List<float[]> list2 = this.points;
                this.result.setText(String.format(Locale.forLanguageTag("hu-HU"), "%.0f px (skála kell)", Float.valueOf(pixelDistance(fArr, list2.get(list2.size() - 1)))));
                this.segmentsView.setText(R.string.basic_measure_enter_scale);
            }
            PlanStore.get().setMeasuredPolygonAreaM2(0.0f);
            return;
        }
        StringBuilder sb = new StringBuilder();
        float f3 = 0.0f;
        int i2 = 1;
        while (i2 < this.points.size()) {
            float pixelDistance = pixelDistance(this.points.get(i2 - 1), this.points.get(i2)) * this.metersPerPixel;
            f3 += pixelDistance;
            int i3 = R.string.measure_segment_line;
            Integer valueOf = Integer.valueOf(i2);
            i2++;
            sb.append(getString(i3, valueOf, Integer.valueOf(i2), formatMeters(pixelDistance)));
            sb.append('\n');
        }
        if (this.points.size() >= 3) {
            List<float[]> list3 = this.points;
            float pixelDistance2 = pixelDistance(list3.get(list3.size() - 1), this.points.get(0)) * this.metersPerPixel;
            f3 += pixelDistance2;
            sb.append(getString(R.string.measure_segment_line, Integer.valueOf(this.points.size()), 1, formatMeters(pixelDistance2)));
            sb.append('\n');
            f2 = PlanStore.polygonArea(toMeterCoords());
            PlanStore.get().setMeasuredPolygonAreaM2(f2);
            sb.append(String.format(Locale.forLanguageTag("hu-HU"), "Terület: %.2f m²\n", Float.valueOf(f2)));
        } else {
            PlanStore.get().setMeasuredPolygonAreaM2(0.0f);
            f2 = 0.0f;
        }
        sb.append(getString(R.string.measure_total, formatMeters(f3)));
        this.segmentsView.setText(sb.toString());
        List<float[]> list4 = this.points;
        float[] fArr2 = list4.get(list4.size() - 2);
        List<float[]> list5 = this.points;
        String string = getString(R.string.measure_last_segment, formatMeters(pixelDistance(fArr2, list5.get(list5.size() - 1)) * this.metersPerPixel));
        if (f2 > 0.0f) {
            string = string + " • " + String.format(Locale.forLanguageTag("hu-HU"), "%.2f m²", Float.valueOf(f2));
        }
        this.result.setText(string);
    }

    private void recomputeScaleFactor() {
        this.metersPerPixel = 0.0f;
        if (this.refMeters <= 0.0f || this.points.size() < 2) {
            return;
        }
        float pixelDistance = pixelDistance(this.points.get(0), this.points.get(1));
        if (pixelDistance > 1.0f) {
            this.metersPerPixel = this.refMeters / pixelDistance;
        }
    }

    private void startBackgroundThread() {
        HandlerThread handlerThread = new HandlerThread("BasicCam");
        this.backgroundThread = handlerThread;
        handlerThread.start();
        this.backgroundHandler = new Handler(this.backgroundThread.getLooper());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startPreview() {
        if (this.cameraDevice == null || !this.textureView.isAvailable()) {
            return;
        }
        try {
            SurfaceTexture surfaceTexture = this.textureView.getSurfaceTexture();
            if (surfaceTexture == null) {
                return;
            }
            surfaceTexture.setDefaultBufferSize(this.previewSize.getWidth(), this.previewSize.getHeight());
            Surface surface = new Surface(surfaceTexture);
            CaptureRequest.Builder createCaptureRequest = this.cameraDevice.createCaptureRequest(1);
            this.previewRequestBuilder = createCaptureRequest;
            createCaptureRequest.addTarget(surface);
            this.cameraDevice.createCaptureSession(Collections.singletonList(surface), new AnonymousClass4(), this.backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "startPreview", e);
        }
    }

    private void stopBackgroundThread() {
        HandlerThread handlerThread = this.backgroundThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                this.backgroundThread.join();
            } catch (InterruptedException unused) {
                Thread.currentThread().interrupt();
            }
            this.backgroundThread = null;
            this.backgroundHandler = null;
        }
    }

    private List<float[]> toMeterCoords() {
        ArrayList arrayList = new ArrayList(this.points.size());
        float f2 = this.points.get(0)[0];
        float f3 = this.points.get(0)[1];
        for (float[] fArr : this.points) {
            float f4 = fArr[0] - f2;
            float f5 = this.metersPerPixel;
            arrayList.add(new float[]{f4 * f5, (fArr[1] - f3) * f5});
        }
        return arrayList;
    }

    private void undoLast() {
        if (this.points.isEmpty()) {
            return;
        }
        this.points.remove(this.points.size() - 1);
        recomputeScaleFactor();
        recomputeAndPublish();
        this.status.setText(this.points.isEmpty() ? getString(R.string.basic_measure_hint) : getString(R.string.measure_point_set, Integer.valueOf(this.points.size())));
    }

    private void updateOverlay() {
        this.pathOverlay.setConfirmedScreenPoints(this.points);
        if (this.points.isEmpty()) {
            return;
        }
        List<float[]> list = this.points;
        float[] fArr = list.get(list.size() - 1);
        this.pathOverlay.setAim(fArr[0], fArr[1], false);
    }

    @Override // androidx.fragment.app.F, androidx.activity.ComponentActivity, o.l, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_basic_measure);
        this.textureView = (TextureView) findViewById(R.id.cameraPreview);
        this.pathOverlay = (MeasurePathOverlay) findViewById(R.id.pathOverlay);
        this.status = (TextView) findViewById(R.id.status);
        this.result = (TextView) findViewById(R.id.result);
        this.segmentsView = (TextView) findViewById(R.id.segments);
        this.scaleInput = (EditText) findViewById(R.id.scaleInput);
        TextView textView = (TextView) findViewById(R.id.basicBanner);
        if (ArCoreHelper.isDeviceUnsupported(this)) {
            textView.setText(R.string.basic_measure_banner);
        } else {
            textView.setText(R.string.basic_measure_banner_optional);
        }
        this.scaleInput.addTextChangedListener(new TextWatcher() { // from class: com.nagyattila.epitoaitervezo.BasicMeasureActivity.1
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                BasicMeasureActivity.this.parseScale();
                BasicMeasureActivity.this.recomputeAndPublish();
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
            }
        });
        this.pathOverlay.setOnTouchListener(this::lambda$onCreate$0);
        findViewById(R.id.undoButton).setOnClickListener(this::lambda$onCreate$1);
        findViewById(R.id.clearButton).setOnClickListener(this::lambda$onCreate$2);
        this.status.setText(R.string.basic_measure_hint);
        updateOverlay();
    }

    @Override // androidx.fragment.app.F, android.app.Activity
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override // androidx.fragment.app.F, androidx.activity.ComponentActivity, android.app.Activity
    public void onRequestPermissionsResult(int i2, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i2, strArr, iArr);
        if (i2 == CAMERA_PERMISSION_CODE) {
            if (iArr.length <= 0 || iArr[0] != 0) {
                Toast.makeText(this, R.string.measure_camera_denied, 1).show();
                this.status.setText(R.string.measure_camera_permission_body);
            } else if (this.textureView.isAvailable()) {
                openCamera();
            } else {
                this.textureView.setSurfaceTextureListener(this.surfaceListener);
            }
        }
    }

    @Override // androidx.fragment.app.F, android.app.Activity
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else if (this.textureView.isAvailable()) {
            openCamera();
        } else {
            this.textureView.setSurfaceTextureListener(this.surfaceListener);
        }
    }
}
