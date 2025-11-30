package pt.ubi.pdm.ecotrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

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

        // ---- PICKER DA DATA ----
        etData.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            int ano = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH);
            int dia = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    AgendarAssistencia.this,
                    (view, year, month, dayOfMonth) -> {
                        // month + 1 porque Janeiro = 0
                        String dataSelecionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etData.setText(dataSelecionada);
                    },
                    ano, mes, dia
            );

            dialog.show();
        });

        // ---- PICKER DA HORA ----
        etHora.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            int hora = calendar.get(Calendar.HOUR_OF_DAY);
            int minuto = calendar.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(
                    AgendarAssistencia.this,
                    (view, hourOfDay, minute) -> {
                        String horaSelecionada = String.format("%02d:%02d", hourOfDay, minute);
                        etHora.setText(horaSelecionada);
                    },
                    hora, minuto,
                    true // formato 24h
            );

            dialog.show();
        });

        // ---- BOTÃO AGENDAR (mantive tudo igual) ----
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
