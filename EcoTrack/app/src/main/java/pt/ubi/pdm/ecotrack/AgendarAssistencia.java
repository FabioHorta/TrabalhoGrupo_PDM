package pt.ubi.pdm.ecotrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AgendarAssistencia extends AppCompatActivity {

    EditText etData, etHora, etDescricao;
    Button btnAgendar;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agendar_assistencia);

        db = new DBHelper(this);

        etData = findViewById(R.id.etData);
        etHora = findViewById(R.id.etHora);
        etDescricao = findViewById(R.id.etDescricao);
        btnAgendar = findViewById(R.id.btnAgendar);

        btnAgendar.setOnClickListener(v -> {
            String data = etData.getText().toString();
            String hora = etHora.getText().toString();
            String desc = etDescricao.getText().toString();

            if (data.isEmpty() || hora.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (db.inserirAssistencia(data, hora, desc)) {
                Toast.makeText(this, "Assistência agendada!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
