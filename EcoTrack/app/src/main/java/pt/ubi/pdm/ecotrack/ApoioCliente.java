package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView; // Importante: Adicionado ImageView

import androidx.appcompat.app.AppCompatActivity;

public class ApoioCliente extends AppCompatActivity {

    private Button btnAgendarAssistencia, btnEnviarMensagem, btnConsultarRelatorios;
    private ImageView btnVoltar; // Alterado de Button para ImageView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apoio_cliente);

        // Ligar às Views do XML
        btnAgendarAssistencia = findViewById(R.id.btnAgendarAssistencia);
        btnEnviarMensagem = findViewById(R.id.btnEnviarMensagem);
        btnConsultarRelatorios = findViewById(R.id.btnConsultarRelatorios);

        // O ID no XML é "voltar" e é uma ImageView
        btnVoltar = findViewById(R.id.voltar);

        // --- Configurar Cliques ---

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

        btnVoltar.setOnClickListener(v -> {
            // Volta para o Menu Principal
            Intent intent = new Intent(ApoioCliente.this, MenuPrincipal.class);
            // Flags para evitar empilhar menus uns em cima dos outros
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}