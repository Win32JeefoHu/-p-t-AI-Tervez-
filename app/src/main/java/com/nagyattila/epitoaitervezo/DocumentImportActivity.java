package com.nagyattila.epitoaitervezo;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Dokumentum import OCR. Copyright © Nagy Attila */
public class DocumentImportActivity extends AppCompatActivity {
    private MaterialButton btnApply, btnOpenFloor, btnOpenInsul;
    private TextView fileNameTv, rawTextTv, statusTv, summaryTv;
    private String lastFileName;
    private BuildingInfoParser.Result lastResult;
    private ProgressBar progress;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> openDoc =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFilePicked);

    private void applyToModules() {
        if (lastResult == null) return;
        BuildingInfoParser.applyToPlanStore(lastResult, lastFileName);
        Toast.makeText(this, R.string.import_applied, Toast.LENGTH_LONG).show();
        btnOpenFloor.setVisibility(View.VISIBLE);
        btnOpenInsul.setVisibility(View.VISIBLE);
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_applied_title)
                .setMessage(R.string.import_applied_msg)
                .setPositiveButton(R.string.import_open_floor, (d, w) -> {
                    startActivity(new Intent(this, FloorPlanActivity.class));
                    finish();
                })
                .setNeutralButton(R.string.import_open_insulation, (d, w) -> {
                    startActivity(new Intent(this, MaterialCalcActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.import_stay, (d, w) -> d.dismiss())
                .show();
    }

    void onFilePicked(Uri uri) {
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
        String name = queryDisplayName(uri);
        String type = getContentResolver().getType(uri);
        if (!DocumentTextExtractor.isSupported(type, name)) {
            Toast.makeText(this, R.string.import_unsupported_format, Toast.LENGTH_LONG).show();
            return;
        }
        lastFileName = name;
        fileNameTv.setText(name);
        rawTextTv.setText("");
        summaryTv.setText("");
        lastResult = null;
        btnApply.setEnabled(false);
        btnOpenFloor.setVisibility(View.GONE);
        btnOpenInsul.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        statusTv.setText(R.string.import_working);
        executor.execute(() -> {
            try {
                String extract = DocumentTextExtractor.extract(this, uri, type, name);
                BuildingInfoParser.Result parsed = BuildingInfoParser.parse(extract);
                runOnUiThread(() -> showResult(extract, parsed));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    statusTv.setText(getString(R.string.import_error,
                            e.getMessage() != null ? e.getMessage() : "ismeretlen"));
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String queryDisplayName(Uri uri) {
        String name = uri.getLastPathSegment();
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        return name != null ? name : "dokumentum";
    }

    private void showResult(String text, BuildingInfoParser.Result result) {
        progress.setVisibility(View.GONE);
        lastResult = result;
        if (text == null) text = "";
        if (text.length() > 12000) text = text.substring(0, 12000) + "\n...";
        rawTextTv.setText(text.isEmpty() ? getString(R.string.import_no_text) : text);
        summaryTv.setText(result.summary);
        statusTv.setText(R.string.import_done_check);
        btnApply.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_import);
        fileNameTv = findViewById(R.id.importFileName);
        rawTextTv = findViewById(R.id.importRawText);
        summaryTv = findViewById(R.id.importSummary);
        statusTv = findViewById(R.id.importStatus);
        progress = findViewById(R.id.importProgress);
        btnApply = findViewById(R.id.btnApplyImport);
        btnOpenFloor = findViewById(R.id.btnOpenFloorPlan);
        btnOpenInsul = findViewById(R.id.btnOpenInsulation);
        btnApply.setEnabled(false);
        btnOpenFloor.setVisibility(View.GONE);
        btnOpenInsul.setVisibility(View.GONE);
        findViewById(R.id.btnPickFile).setOnClickListener(v ->
                openDoc.launch(new String[]{"image/png", "image/jpeg", "application/pdf"}));
        btnApply.setOnClickListener(v -> applyToModules());
        btnOpenFloor.setOnClickListener(v -> {
            startActivity(new Intent(this, FloorPlanActivity.class));
            finish();
        });
        btnOpenInsul.setOnClickListener(v -> {
            startActivity(new Intent(this, MaterialCalcActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
