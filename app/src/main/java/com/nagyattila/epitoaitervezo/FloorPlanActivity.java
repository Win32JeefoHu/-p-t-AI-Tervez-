package com.nagyattila.epitoaitervezo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;

/** 2D CAD alaprajz. Copyright © Nagy Attila */
public class FloorPlanActivity extends AppCompatActivity {
    private TextView info;
    private FloorPlanView planView;
    private EditText scaleField;
    private BuildingElement.Type pendingPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor_plan);
        planView = findViewById(R.id.floorPlanView);
        info = findViewById(R.id.planInfo);
        scaleField = findViewById(R.id.scaleField);

        planView.setVertices(PlanStore.get().getFloorVertices(),
                PlanStore.get().getFloorVertices().size() >= 3);
        planView.setElements(PlanStore.get().getElements());
        planView.setSnapEnabled(true);
        planView.setShowDims(true);
        planView.setShowLayers(true);
        scaleField.setText(String.format(Locale.US, "%.4f", planView.getMetersPerPixel()));

        planView.setListener(this::updateInfo);
        findViewById(R.id.btnCloseRoom).setOnClickListener(v -> {
            planView.closeRoom();
            if (!planView.isClosed()) {
                Toast.makeText(this, "Legalább 3 sarok kell a záráshoz", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnUndo).setOnClickListener(v -> planView.undoLast());
        findViewById(R.id.btnClearPlan).setOnClickListener(v -> {
            planView.clear();
            PlanStore.get().clearFloor();
            PlanStore.get().clearElements();
            planView.setElements(PlanStore.get().getElements());
        });
        findViewById(R.id.btnSavePlan).setOnClickListener(v -> {
            if (!planView.isClosed()) {
                Toast.makeText(this, "Előbb zárd le a helyiséget", Toast.LENGTH_SHORT).show();
                return;
            }
            PlanStore.get().setFloorVertices(planView.getVertices());
            PlanStore.get().setElements(planView.getElements());
            Toast.makeText(this, "Alaprajz mentve", Toast.LENGTH_SHORT).show();
            updateInfo();
        });
        findViewById(R.id.btnExportFromPlan).setOnClickListener(v -> {
            if (planView.isClosed()) {
                PlanStore.get().setFloorVertices(planView.getVertices());
                PlanStore.get().setElements(planView.getElements());
            }
            startActivity(new Intent(this, ExportActivity.class));
        });
        findViewById(R.id.btnModeWall).setOnClickListener(v -> {
            pendingPlace = null;
            planView.setPlacementMode(false, null);
            planView.setDrawMode(true);
            Toast.makeText(this, "Fal rajzolás: koppints a sarkokra", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnPlaceDoor).setOnClickListener(v -> startPlace(BuildingElement.Type.DOOR));
        findViewById(R.id.btnPlaceWindow).setOnClickListener(v -> startPlace(BuildingElement.Type.WINDOW));
        findViewById(R.id.btnPlaceStair).setOnClickListener(v -> startPlace(BuildingElement.Type.STAIR));
        findViewById(R.id.btnPlaceFloor).setOnClickListener(v -> {
            BuildingElement e = new BuildingElement(BuildingElement.Type.FLOORING);
            planView.addElement(e);
            PlanStore.get().setElements(planView.getElements());
            Toast.makeText(this, "Padló hozzáadva", Toast.LENGTH_SHORT).show();
            updateInfo();
        });
        findViewById(R.id.btnToggleLayers).setOnClickListener(v -> {
            planView.setShowLayers(!planView.isShowLayers());
            Toast.makeText(this, planView.isShowLayers() ? "Rétegek: be" : "Rétegek: ki",
                    Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnCopySel).setOnClickListener(v -> {
            if (planView.copySelected()) {
                PlanStore.get().setElements(planView.getElements());
                Toast.makeText(this, "Kijelölt elem másolva", Toast.LENGTH_SHORT).show();
                updateInfo();
            } else {
                Toast.makeText(this, "Nincs kijelölt elem (koppints elemre)", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnSnap).setOnClickListener(v -> {
            planView.setSnapEnabled(!planView.isSnapEnabled());
            ((MaterialButton) v).setText(planView.isSnapEnabled() ? "45/90" : "szabad");
        });
        findViewById(R.id.btnApplyScale).setOnClickListener(v -> {
            try {
                float mpp = Float.parseFloat(scaleField.getText().toString().replace(',', '.'));
                if (mpp > 0.0001f && mpp < 1f) {
                    planView.setMetersPerPixel(mpp);
                    Toast.makeText(this, "Skála beállítva", Toast.LENGTH_SHORT).show();
                    updateInfo();
                } else {
                    Toast.makeText(this, "Érvénytelen skála", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Érvénytelen szám", Toast.LENGTH_SHORT).show();
            }
        });
        updateInfo();
    }

    private void startPlace(BuildingElement.Type type) {
        if (!planView.isClosed()) {
            Toast.makeText(this, "Előbb zárd le a helyiséget", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingPlace = type;
        planView.setDrawMode(false);
        planView.setPlacementMode(true, (mx, mz, wallIndex, tAlong) -> {
            BuildingElement e = new BuildingElement(type);
            e.x = mx; e.y = mz;
            if (e.isOpening()) {
                if (wallIndex < 0) {
                    Toast.makeText(this, "Koppints falra az ajtó/ablakhoz", Toast.LENGTH_SHORT).show();
                    return;
                }
                e.wallIndex = wallIndex;
                e.tAlongWall = tAlong;
            }
            planView.addElement(e);
            PlanStore.get().setElements(planView.getElements());
            planView.setPlacementMode(false, null);
            pendingPlace = null;
            Toast.makeText(this, e.typeLabelHu() + " elhelyezve", Toast.LENGTH_SHORT).show();
            updateInfo();
        });
        Toast.makeText(this, "Koppints a vászonra / falra", Toast.LENGTH_SHORT).show();
    }

    private void updateInfo() {
        info.setText(String.format(Locale.forLanguageTag("hu-HU"),
                "Sarkok: %d • %s • elemek: %d\nAlaprajz: %.2f m² • AR: %.2f m² • snap: %s • m/px: %.4f",
                planView.getVertices().size(),
                planView.isClosed() ? "zárva" : "nyitott",
                planView.getElements().size(),
                planView.getAreaM2(),
                PlanStore.get().getMeasuredPolygonAreaM2(),
                planView.isSnapEnabled() ? "45/90" : "ki",
                planView.getMetersPerPixel()));
    }
}
