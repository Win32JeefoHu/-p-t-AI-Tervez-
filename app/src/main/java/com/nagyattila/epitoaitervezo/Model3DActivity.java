package com.nagyattila.epitoaitervezo;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class Model3DActivity extends AppCompatActivity {
    @Override // androidx.fragment.app.F, androidx.activity.ComponentActivity, o.l, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_model3d);
        final Model3DView model3DView = (Model3DView) findViewById(R.id.model3dView);
        final TextView textView = (TextView) findViewById(R.id.heightLabel);
        SeekBar seekBar = (SeekBar) findViewById(R.id.heightSeek);
        final float[] fArr = {2.7f};
        model3DView.setFootprint(PlanStore.get().getFloorVertices(), fArr[0]);
        textView.setText(String.format(Locale.forLanguageTag("hu-HU"), "Belmagasság: %.1f m", Float.valueOf(fArr[0])));
        seekBar.setMax(40);
        seekBar.setProgress(17);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: com.nagyattila.epitoaitervezo.Model3DActivity.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar2, int i2, boolean z2) {
                fArr[0] = (i2 * 0.1f) + 1.0f;
                textView.setText(String.format(Locale.forLanguageTag("hu-HU"), "Belmagasság: %.1f m", Float.valueOf(fArr[0])));
                model3DView.setFootprint(PlanStore.get().getFloorVertices(), fArr[0]);
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar2) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar2) {
            }
        });
    }
}
