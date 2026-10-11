package com.nagyattila.epitoaitervezo;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.Locale;

/** Építőelemek. Copyright © Nagy Attila */
public class ElementsActivity extends AppCompatActivity {
    private BuildingElement.Type selectedType = BuildingElement.Type.DOOR;
    private FloorPlanView planView;
    private TextView categoryLabel, elementsList, hint;
    private EditText inputWidth, inputHeight, inputSill, inputMaterial;
    private EditText inputSteps, inputStepDepth, inputStepRise;
    private View labelSill, stairRow;
    private MaterialButton btnDoor, btnWindow, btnStair, btnFlooring;
    private boolean waitingForTap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elements);

        planView = findViewById(R.id.elementsPlanView);
        categoryLabel = findViewById(R.id.categoryLabel);
        elementsList = findViewById(R.id.elementsList);
        hint = findViewById(R.id.elementsHint);
        inputWidth = findViewById(R.id.inputWidth);
        inputHeight = findViewById(R.id.inputHeight);
        inputSill = findViewById(R.id.inputSill);
        inputMaterial = findViewById(R.id.inputMaterial);
        inputSteps = findViewById(R.id.inputSteps);
        inputStepDepth = findViewById(R.id.inputStepDepth);
        inputStepRise = findViewById(R.id.inputStepRise);
        labelSill = findViewById(R.id.labelSill);
        stairRow = findViewById(R.id.stairRow);
        btnDoor = findViewById(R.id.btnTypeDoor);
        btnWindow = findViewById(R.id.btnTypeWindow);
        btnStair = findViewById(R.id.btnTypeStair);
        btnFlooring = findViewById(R.id.btnTypeFlooring);

        List<float[]> verts = PlanStore.get().getFloorVertices();
        boolean closed = verts.size() >= 3;
        planView.setVertices(verts, closed);
        planView.setElements(PlanStore.get().getElements());
        if (!closed) hint.setText(R.string.elements_need_plan);

        btnDoor.setOnClickListener(v -> selectType(BuildingElement.Type.DOOR));
        btnWindow.setOnClickListener(v -> selectType(BuildingElement.Type.WINDOW));
        btnStair.setOnClickListener(v -> selectType(BuildingElement.Type.STAIR));
        btnFlooring.setOnClickListener(v -> selectType(BuildingElement.Type.FLOORING));
        findViewById(R.id.btnPlaceElement).setOnClickListener(v -> startPlacement());
        findViewById(R.id.btnDeleteLast).setOnClickListener(v -> deleteLast());

        planView.setPlacementMode(false, null);
        selectType(BuildingElement.Type.DOOR);
        refreshList();
    }

    private void selectType(BuildingElement.Type type) {
        selectedType = type;
        BuildingElement def = new BuildingElement(type);
        inputWidth.setText(fmt(def.width));
        inputHeight.setText(fmt(def.height));
        inputSill.setText(fmt(def.sillHeight));
        inputMaterial.setText(def.material);
        inputSteps.setText(String.valueOf(def.stepCount));
        inputStepDepth.setText(fmt(def.stepDepth));
        inputStepRise.setText(fmt(def.stepRise));

        boolean opening = type == BuildingElement.Type.DOOR || type == BuildingElement.Type.WINDOW;
        boolean window = type == BuildingElement.Type.WINDOW;
        boolean stair = type == BuildingElement.Type.STAIR;
        boolean flooring = type == BuildingElement.Type.FLOORING;

        inputWidth.setEnabled(!flooring);
        inputHeight.setEnabled(opening);
        labelSill.setVisibility(window ? View.VISIBLE : View.GONE);
        inputSill.setVisibility(window ? View.VISIBLE : View.GONE);
        stairRow.setVisibility(stair ? View.VISIBLE : View.GONE);

        categoryLabel.setText(opening
                ? getString(R.string.elem_category_openings) + " → " + def.typeLabelHu()
                : def.typeLabelHu());

        btnDoor.setAlpha(type == BuildingElement.Type.DOOR ? 1f : 0.55f);
        btnWindow.setAlpha(type == BuildingElement.Type.WINDOW ? 1f : 0.55f);
        btnStair.setAlpha(type == BuildingElement.Type.STAIR ? 1f : 0.55f);
        btnFlooring.setAlpha(type == BuildingElement.Type.FLOORING ? 1f : 0.55f);
        waitingForTap = false;
        planView.setPlacementMode(false, null);
    }

    private void startPlacement() {
        if (PlanStore.get().getFloorVertices().size() < 3) {
            Toast.makeText(this, R.string.elements_need_plan, Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedType == BuildingElement.Type.FLOORING) {
            BuildingElement e = buildFromForm();
            e.x = 0; e.y = 0; e.wallIndex = -1;
            PlanStore.get().addElement(e);
            planView.setElements(PlanStore.get().getElements());
            refreshList();
            Toast.makeText(this, R.string.elem_flooring_added, Toast.LENGTH_SHORT).show();
            return;
        }
        waitingForTap = true;
        Toast.makeText(this, R.string.elem_tap_plan, Toast.LENGTH_SHORT).show();
        planView.setPlacementMode(true, (mx, mz, wallIndex, tAlong) -> {
            if (!waitingForTap) return;
            BuildingElement e = buildFromForm();
            e.x = mx;
            e.y = mz;
            if (e.isOpening()) {
                if (wallIndex < 0) {
                    Toast.makeText(this, R.string.elem_need_wall, Toast.LENGTH_SHORT).show();
                    return;
                }
                e.wallIndex = wallIndex;
                e.tAlongWall = tAlong;
            } else {
                e.wallIndex = -1;
            }
            PlanStore.get().addElement(e);
            planView.setElements(PlanStore.get().getElements());
            waitingForTap = false;
            planView.setPlacementMode(false, null);
            refreshList();
            Toast.makeText(this, e.typeLabelHu() + " elhelyezve", Toast.LENGTH_SHORT).show();
        });
    }

    private BuildingElement buildFromForm() {
        BuildingElement e = new BuildingElement(selectedType);
        try {
            e.width = parse(inputWidth, e.width);
            e.height = parse(inputHeight, e.height);
            e.sillHeight = parse(inputSill, e.sillHeight);
            e.stepDepth = parse(inputStepDepth, e.stepDepth);
            e.stepRise = parse(inputStepRise, e.stepRise);
            String steps = inputSteps.getText().toString().trim();
            if (!steps.isEmpty()) e.stepCount = Math.max(1, Integer.parseInt(steps));
        } catch (Exception ignored) {}
        String mat = inputMaterial.getText().toString().trim();
        if (!mat.isEmpty()) e.material = mat;
        return e;
    }

    private void deleteLast() {
        List<BuildingElement> list = PlanStore.get().getElements();
        if (list.isEmpty()) {
            Toast.makeText(this, R.string.elem_none, Toast.LENGTH_SHORT).show();
            return;
        }
        BuildingElement last = list.get(list.size() - 1);
        PlanStore.get().removeElement(last.id);
        planView.setElements(PlanStore.get().getElements());
        refreshList();
    }

    private void refreshList() {
        List<BuildingElement> list = PlanStore.get().getElements();
        if (list.isEmpty()) {
            elementsList.setText(R.string.elem_list_empty);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (BuildingElement e : list) {
            sb.append(i++).append(". ").append(e.summaryLine());
            if (e.isOpening() && e.wallIndex >= 0) sb.append(" • fal #").append(e.wallIndex + 1);
            sb.append('\n');
        }
        elementsList.setText(sb.toString().trim());
    }

    private static float parse(EditText et, float fallback) {
        try {
            String s = et.getText().toString().replace(',', '.').trim();
            if (s.isEmpty()) return fallback;
            return Float.parseFloat(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String fmt(float v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
