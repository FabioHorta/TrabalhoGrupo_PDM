package pt.ubi.pdm.ecotrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;

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

        SyncUtils.restaurarTudoSeNecessario(getApplicationContext());

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
            int minuto = 0; // começamos logo em :00

            TimePickerDialog dialog = new TimePickerDialog(
                    AgendarAssistencia.this,
                    (view, hourOfDay, minute) -> {
                        String horaSelecionada = String.format("%02d:%02d", hourOfDay, minute);
                        etHora.setText(horaSelecionada);
                    },
                    hora, minuto,
                    true
            );

            dialog.show();
        });

        // ---- BOTÃO AGENDAR ----
        btnAgendar.setOnClickListener(v -> {
            String data = etData.getText().toString().trim();
            String hora = etHora.getText().toString().trim();
            String desc = etDescricao.getText().toString().trim();

            if (data.isEmpty() || hora.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            // validar hora: formato HH:MM
            if (!hora.matches("\\d{2}:\\d{2}")) {
                Toast.makeText(this, "Hora inválida. Usa o formato HH:MM.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] partes = hora.split(":");
            int h = Integer.parseInt(partes[0]);
            int m = Integer.parseInt(partes[1]);

            // apenas horas certas entre 09:00 e 17:00
            if (m != 0 || h < 9 || h > 17) {
                Toast.makeText(this,
                        "Escolhe um horário de 1 em 1 hora entre as 09:00 e as 17:00 (ex: 09:00, 10:00...).",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // ir buscar técnicos à BD
            Cursor cTec = db.listarTecnicos();
            if (cTec == null || !cTec.moveToFirst()) {
                Toast.makeText(this,
                        "Não existem técnicos configurados na aplicação.",
                        Toast.LENGTH_LONG).show();
                if (cTec != null) cTec.close();
                return;
            }

            String tecnicoEscolhido = null;

            do {
                String emailTec = cTec.getString(cTec.getColumnIndexOrThrow(DBHelper.C_USER_EMAIL));

                // vê se ESTE técnico está livre para esse dia/hora
                if (!db.existeAssistenciaNoSlot(data, hora, emailTec)) {
                    tecnicoEscolhido = emailTec;
                    break;
                }
            } while (cTec.moveToNext());

            cTec.close();

            if (tecnicoEscolhido == null) {
                Toast.makeText(this,
                        "Nenhum técnico disponível nesse horário. Tente outro horário.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // gravar assistência com técnico associado (LOCAL)
            boolean ok = db.inserirAssistencia(data, hora, desc, tecnicoEscolhido);

            if (ok) {
                Toast.makeText(this,
                        "Assistência agendada com o técnico: " + tecnicoEscolhido,
                        Toast.LENGTH_SHORT).show();
                // assim o técnico noutro dispositivo consegue recebê-la
                SyncUtils.syncTudoAsync(getApplicationContext());

                finish();
            } else {
                Toast.makeText(this,
                        "Erro ao agendar assistência.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
