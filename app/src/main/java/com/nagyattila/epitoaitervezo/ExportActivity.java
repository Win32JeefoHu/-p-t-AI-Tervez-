package com.nagyattila.epitoaitervezo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportActivity extends AppCompatActivity {
    private TextView status;
    private File lastPdf;
    private File lastPng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        status = findViewById(R.id.exportStatus);
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportPdf());
        findViewById(R.id.btnExportPng).setOnClickListener(v -> exportPng());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareLast());
        status.setText("Export a Dokumentumok / Képek mappába (app saját tárhely).");
    }

    private File docsDir() {
        File d = new File(getExternalFilesDir(null), "Documents");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private File picsDir() {
        File d = new File(getExternalFilesDir(null), "Pictures");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private String stamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    private void exportPdf() {
        try {
            PdfDocument doc = new PdfDocument();
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = doc.startPage(info);
            Canvas c = page.getCanvas();
            Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
            title.setColor(Color.parseColor("#18C7D9"));
            title.setTextSize(22f);
            title.setFakeBoldText(true);
            Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
            body.setColor(Color.parseColor("#222222"));
            body.setTextSize(12f);

            float y = 48f;
            c.drawText("Építő AI Tervező — Összefoglaló", 40, y, title);
            y += 28;
            c.drawText("Copyright © Nagy Attila", 40, y, body);
            y += 24;

            PlanStore ps = PlanStore.get();
            y = line(c, body, y, String.format(Locale.forLanguageTag("hu-HU"),
                    "Alapterület: %.2f m²", ps.getBestAreaM2()));
            y = line(c, body, y, "Szigetelés: " + ps.getInsulationType()
                    + String.format(Locale.forLanguageTag("hu-HU"), " • %.0f cm • ×%.2f",
                    ps.getInsulationThicknessCm(), ps.getInsulationWasteFactor()));
            y = line(c, body, y, String.format(Locale.forLanguageTag("hu-HU"),
                    "Elemek: ajtó %d, ablak %d, lépcső %d, padló %.1f m²",
                    ps.countByType(BuildingElement.Type.DOOR),
                    ps.countByType(BuildingElement.Type.WINDOW),
                    ps.countByType(BuildingElement.Type.STAIR),
                    ps.flooringAreaM2()));
            for (BuildingElement e : ps.getElements()) {
                y = line(c, body, y, "  • " + e.summaryLine());
                if (y > 780) break;
            }
            List<float[]> verts = ps.getFloorVertices();
            if (verts.size() >= 3) {
                y += 12;
                y = line(c, body, y, "Alaprajz vázlat:");
                drawSketch(c, verts, ps.getElements(), 40, y + 8, 500, 220);
            }
            doc.finishPage(page);
            lastPdf = new File(docsDir(), "EpitoAI_" + stamp() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(lastPdf)) {
                doc.writeTo(fos);
            }
            doc.close();
            status.setText("PDF kész: " + lastPdf.getAbsolutePath());
            Toast.makeText(this, "PDF exportálva", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            status.setText("PDF hiba: " + e.getMessage());
        }
    }

    private float line(Canvas c, Paint p, float y, String text) {
        c.drawText(text, 40, y, p);
        return y + 18;
    }

    private void drawSketch(Canvas c, List<float[]> verts, List<BuildingElement> els,
                            float left, float top, float w, float h) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (float[] v : verts) {
            minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
            minZ = Math.min(minZ, v[1]); maxZ = Math.max(maxZ, v[1]);
        }
        float span = Math.max(Math.max(maxX - minX, maxZ - minZ), 1f);
        float scale = Math.min(w, h) * 0.85f / span;
        float cx = left + w / 2f, cy = top + h / 2f;
        float ox = (minX + maxX) / 2f, oz = (minZ + maxZ) / 2f;
        Paint wall = new Paint(Paint.ANTI_ALIAS_FLAG);
        wall.setStyle(Paint.Style.STROKE);
        wall.setStrokeWidth(3f);
        wall.setColor(Color.parseColor("#18C7D9"));
        Path path = new Path();
        for (int i = 0; i < verts.size(); i++) {
            float[] v = verts.get(i);
            float sx = cx + (v[0] - ox) * scale;
            float sy = cy + (v[1] - oz) * scale;
            if (i == 0) path.moveTo(sx, sy); else path.lineTo(sx, sy);
        }
        path.close();
        c.drawPath(path, wall);
        Paint ep = new Paint(Paint.ANTI_ALIAS_FLAG);
        ep.setColor(Color.parseColor("#FF9F43"));
        for (BuildingElement e : els) {
            if (e.type == BuildingElement.Type.FLOORING) continue;
            float[] pos = e.resolvePosition(verts);
            c.drawCircle(cx + (pos[0] - ox) * scale, cy + (pos[1] - oz) * scale, 5f, ep);
        }
    }

    private void exportPng() {
        try {
            List<float[]> verts = PlanStore.get().getFloorVertices();
            int W = 1080, H = 1080;
            Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(Color.parseColor("#0B1824"));
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.WHITE);
            text.setTextSize(36f);
            c.drawText("Építő AI Tervező", 40, 80, text);
            if (verts.size() >= 3) {
                drawSketch(c, verts, PlanStore.get().getElements(), 80, 160, W - 160, H - 320);
            } else {
                c.drawText("Nincs mentett alaprajz", 40, H / 2f, text);
            }
            lastPng = new File(picsDir(), "EpitoAI_" + stamp() + ".png");
            try (FileOutputStream fos = new FileOutputStream(lastPng)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            status.setText("PNG kész: " + lastPng.getAbsolutePath());
            Toast.makeText(this, "PNG exportálva", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            status.setText("PNG hiba: " + e.getMessage());
        }
    }

    private void shareLast() {
        File f = lastPdf != null ? lastPdf : lastPng;
        if (f == null || !f.exists()) {
            Toast.makeText(this, "Előbb exportálj PDF-et vagy PNG-t", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(f.getName().endsWith(".pdf") ? "application/pdf" : "image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Megosztás"));
    }
}
