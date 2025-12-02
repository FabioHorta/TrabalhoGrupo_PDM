package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ApoioCliente extends AppCompatActivity {

    Button btnAgendarAssistencia, btnEnviarMensagem, btnConsultarRelatorios,btnvoltar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apoio_cliente);

        btnAgendarAssistencia = findViewById(R.id.btnAgendarAssistencia);
        btnEnviarMensagem = findViewById(R.id.btnEnviarMensagem);
        btnConsultarRelatorios = findViewById(R.id.btnConsultarRelatorios);
        btnvoltar = findViewById(R.id.voltar);

        btnAgendarAssistencia.setOnClickListener(v -> {
            Intent intent = new Intent(ApoioCliente.this, AgendarAssistencia.class);
            startActivity(intent);
        });

        btnEnviarMensagem.setOnClickListener(v -> {
            Intent intent = new Intent(ApoioCliente.this, ChatClienteActivity.class);
            startActivity(intent);
        });

        btnConsultarRelatorios.setOnClickListener(v -> {
            Intent intent = new Intent(ApoioCliente.this, Relatorios.class);
            startActivity(intent);
        });
        btnvoltar.setOnClickListener(v -> {
            Intent intent = new Intent(ApoioCliente.this, MenuPrincipal.class);
            startActivity(intent);
        });

    }
}
