package com.nagyattila.epitoaitervezo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

/** Munkafázisok. Copyright © Nagy Attila */
public class WorkPhasesActivity extends AppCompatActivity {
    private static final String[] PHASES = {
            "Helyszíni felmérés", "Alaprajz / terv jóváhagyás", "Anyagbeszerzés",
            "Felület-előkészítés", "Szigetelés felhelyezése", "Vakolás / burkolás",
            "Nyílászárók", "Festés / befejezés", "Műszaki átadás", "Utókövetés / garancia"
    };
    private static final String PREFS = "work_phases";

    public static int completedCount(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        int n = 0;
        for (int i = 0; i < PHASES.length; i++) {
            if (sp.getBoolean("p" + i, false)) n++;
        }
        return n;
    }

    public static int totalCount() { return PHASES.length; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_phases);
        TextView summary = findViewById(R.id.phasesSummary);
        ListView list = findViewById(R.id.phasesList);
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, PHASES);
        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for (int i = 0; i < PHASES.length; i++) {
            list.setItemChecked(i, sp.getBoolean("p" + i, false));
        }

        Runnable refresh = () -> summary.setText(String.format(Locale.forLanguageTag("hu-HU"),
                "Kész: %d / %d", completedCount(this), totalCount()));
        refresh.run();

        list.setOnItemClickListener((parent, view, position, id) -> {
            sp.edit().putBoolean("p" + position, list.isItemChecked(position)).apply();
            refresh.run();
        });
    }
}
