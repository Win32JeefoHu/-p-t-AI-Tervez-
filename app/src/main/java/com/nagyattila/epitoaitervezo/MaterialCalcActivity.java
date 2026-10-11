package com.nagyattila.epitoaitervezo;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

/** Teljes anyagkalkuláció: szigetelés, festés, vakolat, padló, csempe, ragasztó, nyílászárók, lépcső. */
public class MaterialCalcActivity extends AppCompatActivity {
    private static final String[] CATEGORIES = {
            "Szigetelés", "Festés", "Vakolat / glett", "Padlóburkolat",
            "Csempe / burkolat", "Ragasztó / fugázó", "Nyílászárók", "Lépcső"
    };
    private static final String[] INSUL_TYPES = {
            "EPS grafit", "EPS fehér", "XPS", "Kőzetgyapot", "Üveggyapot", "PUR/PIR"
    };
    private static final float[] PACK_M2_AT_10CM = {5.0f, 5.0f, 3.0f, 4.5f, 6.0f, 4.0f};

    private LinearLayout panelInsulation, panelPaint, panelPlaster, panelFlooring,
            panelTile, panelAdhesive, panelOpenings, panelStairs;
    private EditText areaInput, wasteInput, thickInput, paintCoatsInput, paintCoverageInput,
            plasterMmInput, plasterBagInput, wallFactorInput, wallAreaInput,
            adhesiveKgInput, groutKgInput;
    private Spinner typeSpin, categorySpin;
    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materials);

        categorySpin = findViewById(R.id.spinnerCategory);
        areaInput = findViewById(R.id.inputArea);
        wasteInput = findViewById(R.id.inputWaste);
        thickInput = findViewById(R.id.inputThickness);
        typeSpin = findViewById(R.id.spinnerType);
        paintCoatsInput = findViewById(R.id.inputPaintCoats);
        paintCoverageInput = findViewById(R.id.inputPaintCoverage);
        plasterMmInput = findViewById(R.id.inputPlasterMm);
        plasterBagInput = findViewById(R.id.inputPlasterBagKg);
        wallFactorInput = findViewById(R.id.inputWallFactor);
        wallAreaInput = findViewById(R.id.inputWallArea);
        adhesiveKgInput = findViewById(R.id.inputAdhesiveKg);
        groutKgInput = findViewById(R.id.inputGroutKg);
        result = findViewById(R.id.calcResult);
        Button calc = findViewById(R.id.btnCalc);

        panelInsulation = findViewById(R.id.panelInsulation);
        panelPaint = findViewById(R.id.panelPaint);
        panelPlaster = findViewById(R.id.panelPlaster);
        panelFlooring = findViewById(R.id.panelFlooring);
        panelTile = findViewById(R.id.panelTile);
        panelAdhesive = findViewById(R.id.panelAdhesive);
        panelOpenings = findViewById(R.id.panelOpenings);
        panelStairs = findViewById(R.id.panelStairs);

        categorySpin.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        typeSpin.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, INSUL_TYPES));

        Locale us = Locale.US;
        PlanStore ps = PlanStore.get();
        float best = ps.getBestAreaM2();
        if (best < 0.01f) best = 25f;
        areaInput.setText(String.format(us, "%.2f", best));
        wasteInput.setText(String.format(us, "%.0f", (ps.getInsulationWasteFactor() - 1f) * 100f));
        thickInput.setText(String.format(us, "%.0f", ps.getInsulationThicknessCm()));
        paintCoatsInput.setText(String.format(us, "%.0f", ps.getPaintCoats()));
        paintCoverageInput.setText(String.format(us, "%.2f", ps.getPaintCoverageLPerM2()));
        plasterMmInput.setText(String.format(us, "%.0f", ps.getPlasterMm()));
        plasterBagInput.setText("25");
        wallFactorInput.setText(String.format(us, "%.1f", ps.getTileWallFactor()));
        wallAreaInput.setText("0");
        adhesiveKgInput.setText(String.format(us, "%.1f", ps.getAdhesiveKgPerM2()));
        groutKgInput.setText(String.format(us, "%.2f", ps.getGroutKgPerM2()));

        String saved = ps.getInsulationType();
        for (int i = 0; i < INSUL_TYPES.length; i++) {
            if (INSUL_TYPES[i].equals(saved)) { typeSpin.setSelection(i); break; }
        }

        categorySpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                showPanel(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        showPanel(0);

        calc.setOnClickListener(v -> runCalc());
        calc.performClick();
    }

    private void showPanel(int pos) {
        panelInsulation.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
        panelPaint.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
        panelPlaster.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
        panelFlooring.setVisibility(pos == 3 ? View.VISIBLE : View.GONE);
        panelTile.setVisibility(pos == 4 ? View.VISIBLE : View.GONE);
        panelAdhesive.setVisibility(pos == 5 ? View.VISIBLE : View.GONE);
        panelOpenings.setVisibility(pos == 6 ? View.VISIBLE : View.GONE);
        panelStairs.setVisibility(pos == 7 ? View.VISIBLE : View.GONE);
    }

    private float parse(EditText et, float fallback) {
        try {
            String s = et.getText().toString().replace(',', '.').trim();
            if (s.isEmpty()) return fallback;
            return Float.parseFloat(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void runCalc() {
        try {
            PlanStore ps = PlanStore.get();
            float area = parse(areaInput, 25f);
            float wastePct = Math.max(0f, parse(wasteInput, 10f));
            float factor = 1f + wastePct / 100f;
            float thick = parse(thickInput, 10f);
            int ti = Math.max(0, typeSpin.getSelectedItemPosition());
            float coats = parse(paintCoatsInput, 3f);
            float cov = parse(paintCoverageInput, 0.13f);
            float plasterMm = parse(plasterMmInput, 10f);
            float bagKg = Math.max(1f, parse(plasterBagInput, 25f));
            float wallFactor = parse(wallFactorInput, 2.5f);
            float wallOverride = parse(wallAreaInput, 0f);
            float adhesivePerM2 = parse(adhesiveKgInput, 3f);
            float groutPerM2 = parse(groutKgInput, 0.5f);

            ps.setInsulationThicknessCm(thick);
            ps.setInsulationType(INSUL_TYPES[ti]);
            ps.setInsulationWasteFactor(factor);
            ps.setPaintCoats(coats);
            ps.setPaintCoverageLPerM2(cov);
            ps.setPlasterMm(plasterMm);
            ps.setTileWallFactor(wallFactor);
            ps.setAdhesiveKgPerM2(adhesivePerM2);
            ps.setGroutKgPerM2(groutPerM2);

            float netArea = area * factor;
            float volumeM3 = netArea * (thick / 100f);
            float packCoverage = PACK_M2_AT_10CM[ti] * (10f / Math.max(thick, 1f));
            int packs = (int) Math.ceil(netArea / Math.max(packCoverage, 0.1f));

            float openingDeduct = 0f;
            String flooringMat = "—";
            for (BuildingElement e : ps.getElements()) {
                if (e.isOpening()) openingDeduct += Math.max(0f, e.width) * Math.max(0f, e.height);
                if (e.type == BuildingElement.Type.FLOORING && e.material != null && !e.material.isEmpty()) {
                    flooringMat = e.material;
                }
            }
            float adjustedInsul = Math.max(0f, netArea - openingDeduct * 0.15f);

            // Festés: fal+mennyezet ≈ area * 2.5 (fal) + area (mennyezet) vagy area*3.5; egyszerűen area*3
            float paintSurface = area * 3.5f;
            float paintLiters = paintSurface * coats * cov * factor;

            // Vakolat: ~1.5 kg / m² / mm
            float plasterKg = paintSurface * plasterMm * 1.5f * factor;
            int plasterBags = (int) Math.ceil(plasterKg / bagKg);

            float floorBase = ps.flooringAreaM2();
            if (floorBase < 0.01f) floorBase = area;
            float floorNet = floorBase * factor;

            float wallArea = wallOverride > 0.01f ? wallOverride : (area * wallFactor);
            float tileNet = wallArea * factor;

            float adhesiveBase = Math.max(tileNet, floorNet);
            float adhesiveKg = adhesiveBase * adhesivePerM2;
            float groutKg = adhesiveBase * groutPerM2;

            int doors = ps.countByType(BuildingElement.Type.DOOR);
            int windows = ps.countByType(BuildingElement.Type.WINDOW);
            int stairs = ps.countByType(BuildingElement.Type.STAIR);
            float stairM = ps.stairRunLengthM();

            Locale hu = Locale.forLanguageTag("hu-HU");
            String summary = String.format(hu,
                    "══ ANYAGKALKULÁCIÓ ══\n" +
                    "Alapterület: %.2f m² • veszteség: %.0f%%\n" +
                    "─────────────────\n" +
                    "SZIGETELÉS (%s)\n" +
                    "  Nettó: %.2f m² • korrigált: %.2f m²\n" +
                    "  Vastagság: %.0f cm → %.3f m³\n" +
                    "  Csomag / tábla: %d db (≈ %.2f m²/db)\n" +
                    "─────────────────\n" +
                    "FESTÉS (fal+mennyezet ≈ %.1f m²)\n" +
                    "  Rétegek: %.0f • %.2f L/m²/réteg\n" +
                    "  Festék: %.1f liter\n" +
                    "─────────────────\n" +
                    "VAKOLAT / GLETT (%.0f mm)\n" +
                    "  Anyag: %.0f kg ≈ %d zsák (%.0f kg/zsák)\n" +
                    "─────────────────\n" +
                    "PADLÓBURKOLAT\n" +
                    "  %.2f m² • anyag: %s\n" +
                    "─────────────────\n" +
                    "CSEMPE / FALBURKOLAT\n" +
                    "  Fal: %.2f m² (tényező %.1f)\n" +
                    "─────────────────\n" +
                    "RAGASZTÓ / FUGÁZÓ\n" +
                    "  Ragasztó: %.1f kg • Fugázó: %.1f kg\n" +
                    "─────────────────\n" +
                    "NYÍLÁSZÁRÓK\n" +
                    "  Ajtó: %d db • Ablak: %d db\n" +
                    "─────────────────\n" +
                    "LÉPCSŐ\n" +
                    "  %d db • futóhossz: %.2f m\n" +
                    "─────────────────\n" +
                    "Összegzés rögzítve a költségmodulhoz.",
                    area, wastePct,
                    INSUL_TYPES[ti], netArea, adjustedInsul, thick, volumeM3, packs, packCoverage,
                    paintSurface, coats, cov, paintLiters,
                    plasterMm, plasterKg, plasterBags, bagKg,
                    floorNet, flooringMat,
                    tileNet, wallFactor,
                    adhesiveKg, groutKg,
                    doors, windows,
                    stairs, stairM);

            ps.setLastMaterialsSummary(summary);
            ps.setLastPaintLiters(paintLiters);
            ps.setLastPlasterKg(plasterKg);
            result.setText(summary);
        } catch (Exception e) {
            Toast.makeText(this, R.string.materials_check_numbers, Toast.LENGTH_SHORT).show();
        }
    }
}
