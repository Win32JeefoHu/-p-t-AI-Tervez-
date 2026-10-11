package com.nagyattila.epitoaitervezo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class CostEstimateActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost);

        EditText areaIn = findViewById(R.id.costArea);
        EditText matPrice = findViewById(R.id.costMatPrice);
        EditText laborPrice = findViewById(R.id.costLaborPrice);
        EditText extra = findViewById(R.id.costExtra);
        EditText doorPrice = findViewById(R.id.costDoorPrice);
        EditText windowPrice = findViewById(R.id.costWindowPrice);
        EditText stairPrice = findViewById(R.id.costStairPrice);
        EditText flooringPrice = findViewById(R.id.costFlooringPrice);
        EditText paintPrice = findViewById(R.id.costPaintPrice);
        EditText plasterPrice = findViewById(R.id.costPlasterPrice);
        TextView out = findViewById(R.id.costResult);
        Button calc = findViewById(R.id.btnCostCalc);

        float area = PlanStore.get().getBestAreaM2();
        if (area < 0.01f) area = 25f;
        Locale us = Locale.US;
        areaIn.setText(String.format(us, "%.2f", area * PlanStore.get().getInsulationWasteFactor()));
        matPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPriceInsulationHuf()));
        laborPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPriceLaborHuf()));
        extra.setText("50000");
        doorPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPriceDoorHuf()));
        windowPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPriceWindowHuf()));
        stairPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPriceStairPerMHuf()));
        flooringPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPriceFlooringPerM2Huf()));
        paintPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPricePaintHufPerL()));
        plasterPrice.setText(String.format(us, "%.0f", PlanStore.get().getUnitPricePlasterHufPerKg()));

        calc.setOnClickListener(v -> {
            try {
                float a = Float.parseFloat(areaIn.getText().toString().replace(',', '.'));
                float mp = Float.parseFloat(matPrice.getText().toString().replace(',', '.'));
                float lp = Float.parseFloat(laborPrice.getText().toString().replace(',', '.'));
                float ex = Float.parseFloat(extra.getText().toString().replace(',', '.'));
                float dp = Float.parseFloat(doorPrice.getText().toString().replace(',', '.'));
                float wp = Float.parseFloat(windowPrice.getText().toString().replace(',', '.'));
                float sp = Float.parseFloat(stairPrice.getText().toString().replace(',', '.'));
                float fp = Float.parseFloat(flooringPrice.getText().toString().replace(',', '.'));
                float ptp = Float.parseFloat(paintPrice.getText().toString().replace(',', '.'));
                float plp = Float.parseFloat(plasterPrice.getText().toString().replace(',', '.'));
                PlanStore ps = PlanStore.get();
                ps.setUnitPriceInsulationHuf(mp);
                ps.setUnitPriceLaborHuf(lp);
                ps.setUnitPriceDoorHuf(dp);
                ps.setUnitPriceWindowHuf(wp);
                ps.setUnitPriceStairPerMHuf(sp);
                ps.setUnitPriceFlooringPerM2Huf(fp);
                ps.setUnitPricePaintHufPerL(ptp);
                ps.setUnitPricePlasterHufPerKg(plp);

                float matTotal = a * mp;
                float laborTotal = a * lp;
                int doorN = ps.countByType(BuildingElement.Type.DOOR);
                int winN = ps.countByType(BuildingElement.Type.WINDOW);
                float stairM = ps.stairRunLengthM();
                float floorM2 = ps.flooringAreaM2();
                float doorTotal = doorN * dp;
                float winTotal = winN * wp;
                float stairTotal = stairM * sp;
                float floorTotal = floorM2 * fp;

                float paintL = ps.getLastPaintLiters();
                if (paintL < 0.01f) {
                    float base = ps.getBestAreaM2();
                    if (base < 0.01f) base = a / Math.max(ps.getInsulationWasteFactor(), 1f);
                    paintL = base * 3.5f * ps.getPaintCoats() * ps.getPaintCoverageLPerM2()
                            * ps.getInsulationWasteFactor();
                }
                float plasterKg = ps.getLastPlasterKg();
                if (plasterKg < 0.01f) {
                    float base = ps.getBestAreaM2();
                    if (base < 0.01f) base = a / Math.max(ps.getInsulationWasteFactor(), 1f);
                    plasterKg = base * 3.5f * ps.getPlasterMm() * 1.5f * ps.getInsulationWasteFactor();
                }
                float paintTotal = paintL * ptp;
                float plasterTotal = plasterKg * plp;

                float elementsTotal = doorTotal + winTotal + stairTotal + floorTotal + paintTotal + plasterTotal;
                int phasesDone = WorkPhasesActivity.completedCount(this);
                int phasesTotal = WorkPhasesActivity.totalCount();
                float progress = phasesTotal == 0 ? 0f : (float) phasesDone / phasesTotal;
                float contingency = (matTotal + laborTotal + elementsTotal) * 0.08f;
                float grand = matTotal + laborTotal + ex + elementsTotal + contingency;

                out.setText(String.format(Locale.forLanguageTag("hu-HU"),
                        "Felület (veszteséggel): %.2f m²\nSzigetelés anyag (%s): %s Ft\nMunkadíj: %s Ft\n─────────────────\nFestés (%.1f L × %s): %s Ft\nVakolat (%.0f kg × %s): %s Ft\n─────────────────\nÉpítőelemek:\n  Ajtó (%d × %s): %s Ft\n  Ablak (%d × %s): %s Ft\n  Lépcső (%.2f m × %s): %s Ft\n  Padló (%.2f m² × %s): %s Ft\n  Elemek+festék+vakolat: %s Ft\n─────────────────\nEgyéb: %s Ft\nTartalék (8%%): %s Ft\n─────────────────\nÖSSZESEN: %s Ft\nMunkafázis: %d/%d (%.0f%%)\nArányos költség: %s Ft",
                        a, ps.getInsulationType(), huf(matTotal), huf(laborTotal),
                        paintL, huf(ptp), huf(paintTotal),
                        plasterKg, huf(plp), huf(plasterTotal),
                        doorN, huf(dp), huf(doorTotal), winN, huf(wp), huf(winTotal),
                        stairM, huf(sp), huf(stairTotal), floorM2, huf(fp), huf(floorTotal),
                        huf(elementsTotal), huf(ex), huf(contingency), huf(grand),
                        phasesDone, phasesTotal, progress * 100f, huf(grand * progress)));
            } catch (Exception ex) {
                Toast.makeText(this, "Ellenőrizd a számokat", Toast.LENGTH_SHORT).show();
            }
        });
        calc.performClick();
    }

    private static String huf(float v) {
        return String.format(Locale.forLanguageTag("hu-HU"), "%,.0f", v).replace('\u00a0', ' ');
    }
}
