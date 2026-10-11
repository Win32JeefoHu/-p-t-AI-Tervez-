package com.nagyattila.epitoaitervezo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Copyright © Nagy Attila */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        findViewById(R.id.startMeasure).setOnClickListener(v ->
                startActivity(new Intent(this, MeasureChooserActivity.class)));
        findViewById(R.id.btnFloorPlan).setOnClickListener(v ->
                startActivity(new Intent(this, FloorPlanActivity.class)));
        findViewById(R.id.btnElements).setOnClickListener(v ->
                startActivity(new Intent(this, ElementsActivity.class)));
        findViewById(R.id.btnModel3d).setOnClickListener(v ->
                startActivity(new Intent(this, Model3DActivity.class)));
        findViewById(R.id.btnInsulation).setOnClickListener(v ->
                startActivity(new Intent(this, MaterialCalcActivity.class)));
        findViewById(R.id.btnPhases).setOnClickListener(v ->
                startActivity(new Intent(this, WorkPhasesActivity.class)));
        findViewById(R.id.btnCost).setOnClickListener(v ->
                startActivity(new Intent(this, CostEstimateActivity.class)));
        findViewById(R.id.btnExport).setOnClickListener(v ->
                startActivity(new Intent(this, ExportActivity.class)));
        findViewById(R.id.btnDocumentImport).setOnClickListener(v ->
                startActivity(new Intent(this, DocumentImportActivity.class)));
    }
}
