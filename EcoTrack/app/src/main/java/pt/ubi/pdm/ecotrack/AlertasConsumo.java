package pt.ubi.pdm.ecotrack;

import android.content.Intent;
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
        analisarConsumoManual();
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

        btnAgendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlertasConsumo.this, ApoioCliente.class);
                startActivity(intent);
            }
        });

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void analisarConsumoManual() {
        // 1. Vai buscar os dados das leituras à BD
        android.database.Cursor cursor = dbHelper.obterLeituras();
        List<Double> leituras = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            int colIndex = cursor.getColumnIndex(DBHelper.C_LEITURA_VALOR);
            do {
                if (colIndex != -1) {
                    leituras.add(cursor.getDouble(colIndex));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // --- VERIFICAÇÕES DE SEGURANÇA ---

        // Nenhuma leitura
        if (leituras.isEmpty()) {
            configurarAlerta(
                    "Sem Dados",
                    "Ainda não inseriu leituras. Vá a 'Leituras Mensais'.",
                    Color.GRAY,
                    android.R.drawable.ic_menu_info_details
            );
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        // Apenas 1 leitura
        if (leituras.size() < 2) {
            configurarAlerta(
                    "Dados Insuficientes",
                    "Precisa de pelo menos 2 leituras para calcular o consumo.",
                    Color.GRAY,
                    android.R.drawable.ic_menu_info_details
            );
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        // 2. Obter valores - lista está ordenada da mais recente para a mais antiga
        double leituraMaisRecente = leituras.get(0);
        double leituraPenultima = leituras.get(1);

        // Verificação de erro: leitura atual não pode ser menor que a anterior
        if (leituraMaisRecente < leituraPenultima) {
            configurarAlerta(
                    "❌ Erro na Leitura",
                    String.format(
                            "A leitura atual (%.0f) é inferior à anterior (%.0f). Verifique se se enganou a escrever.",
                            leituraMaisRecente, leituraPenultima
                    ),
                    Color.RED,
                    android.R.drawable.ic_delete
            );
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        // 3. Consumo atual (mês)
        double consumoAtual = leituraMaisRecente - leituraPenultima;

        // Se só existem 2 leituras, ainda não há histórico suficiente para média
        if (leituras.size() < 3) {
            configurarAlerta(
                    "Primeiro Registo",
                    String.format("Consumo registado: %.1f kWh. Insira mais dados no próximo mês.", consumoAtual),
                    Color.parseColor("#1976D2"),
                    android.R.drawable.ic_menu_info_details
            );
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        // 4. Calcular média histórica dos consumos (máx. 6 períodos anteriores)
        double somaConsumosAntigos = 0;
        int contadorPeriodos = 0;

        // Começamos no índice 1 porque o par (0,1) já foi usado para o consumoAtual
        for (int i = 1; i < leituras.size() - 1; i++) {
            double recente = leituras.get(i);
            double antiga = leituras.get(i + 1);

            if (recente >= antiga) {
                somaConsumosAntigos += (recente - antiga);
                contadorPeriodos++;
            }
            if (contadorPeriodos >= 6) break; // limita a 6 períodos
        }

        // Sem períodos válidos → tratar como sem histórico suficiente
        if (contadorPeriodos == 0) {
            configurarAlerta(
                    "A recolher histórico",
                    String.format("Consumo atual: %.1f kWh.", consumoAtual),
                    Color.BLUE,
                    android.R.drawable.ic_menu_info_details
            );
            layoutSugestoes.setVisibility(View.GONE);
            btnAgendar.setVisibility(View.GONE);
            return;
        }

        double mediaHistorica = somaConsumosAntigos / contadorPeriodos;
        double diferenca = consumoAtual - mediaHistorica;
        double percentagem = (mediaHistorica > 0)
                ? (diferenca / mediaHistorica) * 100.0
                : 0;

        // 5. Gerar alertas consoante a percentagem
        if (percentagem >= 40.0) {
            configurarAlerta(
                    "🚨 ALERTA CRÍTICO!",
                    String.format("Aumento de %.0f%%! Verifique possíveis fugas ou avarias.", percentagem),
                    Color.parseColor("#D32F2F"),
                    android.R.drawable.ic_dialog_alert
            );
            mostrarDicas(
                    "Faça um teste de fuga no quadro elétrico.",
                    "Verifique cabos e ligações junto ao contador.",
                    "Verifique se algum eletrodoméstico está a aquecer ou fazer ruído estranho."
            );
            btnAgendar.setVisibility(View.VISIBLE);

        } else if (percentagem >= 20.0) {
            configurarAlerta(
                    "⚠️ Consumo Elevado",
                    String.format("Gastou cerca de %.0f%% a mais que o habitual.", percentagem),
                    Color.parseColor("#FF9800"),
                    android.R.drawable.stat_notify_error
            );
            mostrarDicas(
                    "Reduza o uso de aquecedores e ar condicionado.",
                    "Evite deixar equipamentos em standby.",
                    "Use máquinas de lavar nas horas de vazio."
            );
            btnAgendar.setVisibility(View.GONE);

        } else if (percentagem > 0) {
            configurarAlerta(
                    "📈 Ligeiro Aumento",
                    String.format("O consumo subiu %.1f%% face à média.", percentagem),
                    Color.parseColor("#FBC02D"),
                    android.R.drawable.ic_menu_sort_by_size
            );
            mostrarDicas(
                    "Substitua lâmpadas por LEDs.",
                    "Tape as panelas ao cozinhar.",
                    "Desligue carregadores da tomada."
            );
            btnAgendar.setVisibility(View.GONE);

        } else {
            configurarAlerta(
                    "✅ Bom Trabalho!",
                    String.format("Poupança de %.1f%% em relação à média!", Math.abs(percentagem)),
                    Color.parseColor("#388E3C"),
                    android.R.drawable.ic_input_add
            );
            mostrarDicas(
                    "Continue a monitorizar o seu consumo.",
                    "Isole portas e janelas para manter o calor.",
                    "Partilhe estas boas práticas com a família."
            );
            tvTituloSugestoes.setText("Dicas para manter:");
            btnAgendar.setVisibility(View.GONE);
        }
    }

    // Define a estética dos alertas
    private void configurarAlerta(String titulo, String msg, int cor, int iconRes) {
        tvTituloAlerta.setText(titulo);
        tvTituloAlerta.setTextColor(cor);
        tvMensagemAlerta.setText(msg);
        ivIconeAlerta.setImageResource(iconRes);
        ivIconeAlerta.setColorFilter(cor);
    }

    // Coloca as sugestões visíveis e define o texto das dicas
    private void mostrarDicas(String d1, String d2, String d3) {
        layoutSugestoes.setVisibility(View.VISIBLE);
        tvDica1.setText(d1);
        tvDica2.setText(d2);
        tvDica3.setText(d3);
    }
}