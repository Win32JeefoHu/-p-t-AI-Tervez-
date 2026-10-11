package com.nagyattila.epitoaitervezo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.ar.core.ArCoreApk;

/** AR / Mélység / Alap mérés választó. Copyright © Nagy Attila */
public class MeasureChooserActivity extends AppCompatActivity {
    public static final String EXTRA_PREFER_DEPTH = "prefer_depth";

    private TextView status;
    private TextView depthHint;
    private TextView depthBadge;
    private MaterialButton btnDepth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_chooser);
        status = findViewById(R.id.chooserStatus);
        depthHint = findViewById(R.id.depthHint);
        depthBadge = findViewById(R.id.depthBadge);
        btnDepth = findViewById(R.id.btnDepthMeasure);
        status.setText(R.string.chooser_checking);
        depthBadge.setText(R.string.chooser_depth_checking);

        findViewById(R.id.btnArMeasure).setOnClickListener(v -> openAr(false));
        btnDepth.setOnClickListener(v -> openAr(true));
        findViewById(R.id.btnBasicMeasure).setOnClickListener(v ->
                startActivity(new Intent(this, BasicMeasureActivity.class)));
        findViewById(R.id.btnChooserBack).setOnClickListener(v -> finish());
        status.postDelayed(this::refreshArStatus, 250);
    }

    void refreshAvailability() { refreshArStatus(); }

    private void refreshArStatus() {
        ArCoreApk.Availability st = ArCoreHelper.check(this);
        if (st == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            status.setText(R.string.chooser_ar_available_status);
        } else if (st == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            status.setText(R.string.chooser_ar_unavailable_status);
        } else {
            status.setText(R.string.chooser_ar_unknown_status);
        }
        refreshDepthUi();
    }

    private void refreshDepthUi() {
        boolean arOk = ArCoreHelper.isSupported(this)
                && !ArCoreHelper.isDeviceUnsupported(this);
        boolean depthOk = arOk && ArCoreHelper.isDepthApiSupported(this);
        if (depthOk) {
            depthBadge.setText(R.string.chooser_depth_available);
            depthHint.setText(R.string.chooser_depth_body);
            btnDepth.setEnabled(true);
            btnDepth.setAlpha(1f);
        } else {
            depthBadge.setText(R.string.chooser_depth_unavailable_badge);
            depthHint.setText(R.string.chooser_depth_unavailable_body);
            btnDepth.setEnabled(false);
            btnDepth.setAlpha(0.45f);
        }
    }

    private void openAr(boolean preferDepth) {
        if (ArCoreHelper.isDeviceUnsupported(this)) {
            Toast.makeText(this, R.string.chooser_ar_unavailable_toast, Toast.LENGTH_LONG).show();
            return;
        }
        if (preferDepth && !ArCoreHelper.isDepthApiSupported(this)) {
            Toast.makeText(this, R.string.chooser_depth_unavailable_toast, Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, MeasureActivity.class);
        i.putExtra(EXTRA_PREFER_DEPTH, preferDepth);
        startActivity(i);
    }
}
