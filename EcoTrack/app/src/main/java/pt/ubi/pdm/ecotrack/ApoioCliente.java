package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ApoioCliente extends AppCompatActivity {
    private Button btnAgendarAssistencia, btnEnviarMensagem, btnConsultarRelatorios;
    private ImageView btnVoltar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apoio_cliente);

        btnAgendarAssistencia = findViewById(R.id.btnAgendarAssistencia);
        btnEnviarMensagem = findViewById(R.id.btnEnviarMensagem);
        btnConsultarRelatorios = findViewById(R.id.btnConsultarRelatorios);
        btnVoltar = findViewById(R.id.voltar);


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

        // Lógica de retorno ao Menu Principal
        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(ApoioCliente.this, MenuPrincipal.class);

            // FLAG_ACTIVITY_CLEAR_TOP: Remove todas as activities acima da activity alvo na pilha.
            // FLAG_ACTIVITY_SINGLE_TOP: Reutiliza a instância existente se ela já estiver no topo, evitando recriação desnecessária.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
            // Encerra a activity atual (ApoioCliente) para libertar recursos e removê-la da pilha
            finish();
        });
    }
}