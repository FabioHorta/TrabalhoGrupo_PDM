package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AlertasConsumo extends AppCompatActivity {

    private DBHelper dbHelper;

    // Views
    private TextView tvTituloAlerta, tvMensagemAlerta, tvTituloSugestoes;
    private TextView tvDica1, tvDica2, tvDica3;
    private ImageView ivIconeAlerta;
    private LinearLayout layoutSugestoes;
    private Button btnAgendar, btnVoltar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas_consumo);

        dbHelper = new DBHelper(this);

        initViews();
        analisarConsumoManual(); // <--- Nova função de cálculo
    }

    private void initViews() {
        tvTituloAlerta = findViewById(R.id.tvTituloAlerta);
        tvMensagemAlerta = findViewById(R.id.tvMensagemAlerta);
        ivIconeAlerta = findViewById(R.id.ivIconeAlerta);

        tvTituloSugestoes = findViewById(R.id.tvTituloSugestoes);
        layoutSugestoes = findViewById(R.id.layoutSugestoes);

        tvDica1 = findViewById(R.id.tvDica1);
        tvDica2 = findViewById(R.id.tvDica2);
        tvDica3 = findViewById(R.id.tvDica3);

        btnAgendar = findViewById(R.id.btnAgendarAssistencia);
        btnVoltar = findViewById(R.id.btnVoltarMenu);

        btnAgendar.setOnClickListener(v -> {
            Intent intent = new Intent(AlertasConsumo.this, ApoioCliente.class);
            startActivity(intent);
        });

        btnVoltar.setOnClickListener(v -> finish());
    }

    private void analisarConsumoManual() {
        // 1. Buscar dados à tabela de FOTOS
        android.database.Cursor cursor = dbHelper.obterLeituras();
        java.util.List<Double> leituras = new java.util.ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            int colIndex = cursor.getColumnIndex(DBHelper.C_LF_VALOR);
            do {
                if (colIndex != -1) {
                    leituras.add(cursor.getDouble(colIndex));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // --- VERIFICAÇÕES DE SEGURANÇA ---

        if (leituras.isEmpty()) {
            configurarAlerta("Sem Dados", "Ainda não inseriu leituras. Vá a 'Leituras Mensais'.", Color.GRAY, android.R.drawable.ic_menu_info_details);
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        if (leituras.size() < 2) {
            configurarAlerta("Dados Insuficientes", "Precisa de pelo menos 2 leituras para calcular o consumo.", Color.GRAY, android.R.drawable.ic_menu_info_details);
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        // 2. Obter valores (A lista vem ordenada da mais recente para a mais antiga)
        double leituraMaisRecente = leituras.get(0); // Ex: 469
        double leituraPenultima = leituras.get(1);   // Ex: 789

        // --- CORREÇÃO DO ERRO DE LÓGICA ---
        // Se a leitura atual for MENOR que a antiga, houve um erro (o contador não anda para trás)
        if (leituraMaisRecente < leituraPenultima) {
            configurarAlerta(
                    "❌ Erro na Leitura",
                    String.format("A leitura atual (%.0f) é inferior à anterior (%.0f). Verifique se se enganou a escrever.", leituraMaisRecente, leituraPenultima),
                    Color.RED,
                    android.R.drawable.ic_delete
            );
            // Esconder dicas porque é um erro de input
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return; // Pára o código aqui!
        }

        // Se passou daqui, o cálculo é válido
        double consumoAtual = leituraMaisRecente - leituraPenultima;

        // --- CENÁRIO: Apenas 2 leituras (Sem histórico para média) ---
        if (leituras.size() < 3) {
            configurarAlerta("Primeiro Registo", String.format("Consumo registado: %.1f kWh. Insira mais dados no próximo mês.", consumoAtual), Color.parseColor("#1976D2"), android.R.drawable.ic_menu_info_details);
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        // 3. Calcular Média Histórica
        double somaConsumosAntigos = 0;
        int contadorPeriodos = 0;

        for (int i = 1; i < leituras.size() - 1; i++) {
            double recente = leituras.get(i);
            double antiga = leituras.get(i + 1);

            // Aqui também protegemos contra erros antigos
            if (recente >= antiga) {
                somaConsumosAntigos += (recente - antiga);
                contadorPeriodos++;
            }
            if (contadorPeriodos >= 6) break;
        }

        // Evitar divisão por zero
        if (contadorPeriodos == 0) {
            // Se não conseguiu calcular média (dados inconsistentes), trata como primeiro registo
            configurarAlerta("A recolher histórico", String.format("Consumo atual: %.1f kWh.", consumoAtual), Color.BLUE, android.R.drawable.ic_menu_info_details);
            return;
        }

        double mediaHistorica = somaConsumosAntigos / contadorPeriodos;
        double diferenca = consumoAtual - mediaHistorica;
        double percentagem = (mediaHistorica > 0) ? (diferenca / mediaHistorica) * 100.0 : 0;

        // 4. Gerar Alertas
        if (percentagem >= 40.0) {
            configurarAlerta("🚨 ALERTA CRÍTICO!", String.format("Aumento de %.0f%%! Verifique fugas.", percentagem), Color.parseColor("#D32F2F"), android.R.drawable.ic_dialog_alert);
            mostrarDicas("Teste de Fuga no quadro.", "Verifique cabos no contador.", "Avaria em eletrodomésticos.");
            btnAgendar.setVisibility(View.VISIBLE);
        } else if (percentagem >= 20.0) {
            configurarAlerta("⚠️ Consumo Elevado", String.format("Gastou %.0f%% a mais que o habitual.", percentagem), Color.parseColor("#FF9800"), android.R.drawable.stat_notify_error);
            mostrarDicas("Reduza o aquecimento.", "Evite standby.", "Máquinas à noite.");
            btnAgendar.setVisibility(View.GONE);
        } else if (percentagem > 0) {
            configurarAlerta("📈 Ligeiro Aumento", String.format("Subiu %.1f%%.", percentagem), Color.parseColor("#FBC02D"), android.R.drawable.ic_menu_sort_by_size);
            mostrarDicas("Use LEDs.", "Tape as panelas.", "Desligue carregadores.");
            btnAgendar.setVisibility(View.GONE);
        } else {
            configurarAlerta("✅ Bom Trabalho!", String.format("Poupança de %.1f%%!", Math.abs(percentagem)), Color.parseColor("#388E3C"), android.R.drawable.ic_input_add);
            mostrarDicas("Mantenha a monitorização.", "Isole janelas.", "Partilhe dicas.");
            tvTituloSugestoes.setText("Dicas para manter:");
            btnAgendar.setVisibility(View.GONE);
        }
    }

    private void configurarAlerta(String titulo, String msg, int cor, int iconRes) {
        tvTituloAlerta.setText(titulo);
        tvTituloAlerta.setTextColor(cor);
        tvMensagemAlerta.setText(msg);
        ivIconeAlerta.setImageResource(iconRes);
        ivIconeAlerta.setColorFilter(cor);
    }

    private void mostrarDicas(String d1, String d2, String d3) {
        layoutSugestoes.setVisibility(View.VISIBLE);
        tvDica1.setText(d1);
        tvDica2.setText(d2);
        tvDica3.setText(d3);
    }
}