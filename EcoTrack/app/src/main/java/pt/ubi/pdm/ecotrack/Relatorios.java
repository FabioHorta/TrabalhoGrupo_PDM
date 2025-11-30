package pt.ubi.pdm.ecotrack;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Relatorios extends AppCompatActivity {

    ListView listRelatorios;
    DBHelper db;
    ArrayList<String> relatoriosList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorios);

        db = new DBHelper(this);

        listRelatorios = findViewById(R.id.listRelatorios);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, relatoriosList);
        listRelatorios.setAdapter(adapter);

        carregarRelatorios();
    }

    void carregarRelatorios() {
        Cursor c = db.listarAssistencias();
        relatoriosList.clear();

        while (c.moveToNext()) {
            String linha =
                    "Data: " + c.getString(1) +
                            "  Hora: " + c.getString(2) +
                            "\nProblema: " + c.getString(3) +
                            "\nFeedback: " + c.getString(4);

            relatoriosList.add(linha);
        }
        adapter.notifyDataSetChanged();
    }
}
