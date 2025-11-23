package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class MenuPrincipal extends AppCompatActivity {

    private MaterialCardView btnMelhorTipo, btnAlertas, btnCalculadoraCusto, btnEstimativa, btnLeituras, btnApoioaoCliente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Carrega o layout da Activity
        setContentView(R.layout.activity_principal_menu);

        initViews();
    }

    private void initViews(){
        btnMelhorTipo = findViewById(R.id.btnMelhorTipo);
        btnAlertas = findViewById(R.id.btnAlertas);
        btnCalculadoraCusto = findViewById(R.id.btnCalculadora);
        btnEstimativa = findViewById(R.id.btnEstimativa);
        btnLeituras = findViewById(R.id.btnLeituras);
        btnApoioaoCliente = findViewById(R.id.btnApoioaoCliente);

        btnAlertas.setOnClickListener(v -> {
            // Cria Intent para SleepTipsActivity
            Intent intent = new Intent(MenuPrincipal.this, AlertasConsumo.class);
            // Inicia a Activity
            startActivity(intent);
        });

        btnMelhorTipo.setOnClickListener(v -> {
            // Cria Intent para StressTipsActivity
            Intent intent = new Intent(MenuPrincipal.this, TipoEnergia.class);
            // Inicia a Activity
            startActivity(intent);
        });

        btnEstimativa.setOnClickListener(v -> {
            // Cria Intent para MotivationTipsActivity
            Intent intent = new Intent(MenuPrincipal.this, EstimativaConsumo.class);
            // Inicia a Activity
            startActivity(intent);
        });

        // listener para flashcards
        // Ao clicar em "Flashcards"
        btnLeituras.setOnClickListener(v -> {
            // Cria Intent para FlashcardsActivity
            Intent intent = new Intent(MenuPrincipal.this, LeiturasMensais.class);
            startActivity(intent);
        });

        btnApoioaoCliente.setOnClickListener(v -> {
            Intent intent = new Intent(MenuPrincipal.this, ApoioCliente.class);
            startActivity(intent);
        });
        btnCalculadoraCusto.setOnClickListener(v -> {

            Intent intent = new Intent(MenuPrincipal.this, CalculadoraCustos.class);
            startActivity(intent);
        });
    }

}