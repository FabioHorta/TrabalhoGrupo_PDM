package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        // Inicialmente esconder elementos que só aparecem com dados
        layoutSugestoes.setVisibility(View.GONE);
        tvTituloSugestoes.setVisibility(View.GONE);
        btnAgendar.setVisibility(View.GONE);

        btnAgendar.setOnClickListener(v -> {
            Intent intent = new Intent(AlertasConsumo.this, ApoioCliente.class);
            startActivity(intent);
        });

        btnVoltar.setOnClickListener(v -> finish());
    }

    private void analisarConsumoManual() {
        android.database.Cursor cursor = null;
        try {
            cursor = dbHelper.obterHistoricoConsumosAnalisados(null);
            if (cursor == null || !cursor.moveToFirst()) {
                configurarAlerta(
                        "Sem Dados",
                        "Ainda não existem análises de consumo. Insira leituras em 'Leituras Mensais'.",
                        Color.GRAY,
                        android.R.drawable.ic_menu_info_details
                );
                layoutSugestoes.setVisibility(View.GONE);
                tvTituloSugestoes.setVisibility(View.GONE);
                btnAgendar.setVisibility(View.GONE);
                return;
            }

            int idxStatus = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_STATUS);
            int idxPercentagem = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_PERCENTAGEM);
            int idxConsumo = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_VALOR);
            int idxMediaRef = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_MEDIA_REF);
            int idxDataLeitura = cursor.getColumnIndex("data_leitura");
            int idxValorLeitura = cursor.getColumnIndex("valor_leitura");

            if (idxStatus == -1 || idxPercentagem == -1 || idxConsumo == -1 || idxMediaRef == -1) {
                configurarAlerta(
                        "Erro",
                        "Dados incompletos na análise.",
                        Color.RED,
                        android.R.drawable.ic_dialog_alert
                );
                layoutSugestoes.setVisibility(View.GONE);
                tvTituloSugestoes.setVisibility(View.GONE);
                btnAgendar.setVisibility(View.GONE);
                return;
            }

            String status = cursor.getString(idxStatus);
            double percentagem = cursor.getDouble(idxPercentagem);
            double consumoAtual = cursor.getDouble(idxConsumo);
            double mediaRef = cursor.getDouble(idxMediaRef);
            String dataLeitura = idxDataLeitura != -1 ? cursor.getString(idxDataLeitura) : "N/A";
            double valorLeitura = idxValorLeitura != -1 ? cursor.getDouble(idxValorLeitura) : 0;

            // --- Decidir alerta com base no status ---
            if ("ALTO".equals(status)) {
                configurarAlerta(
                        "🚨 ALERTA CRÍTICO!",
                        String.format("Aumento de %.0f%%! Verifique possíveis fugas ou avarias.\n\n📅 Leitura: %s\n🔢 Valor: %.1f kWh",
                                percentagem, dataLeitura, valorLeitura),
                        Color.parseColor("#D32F2F"),
                        android.R.drawable.ic_dialog_alert
                );
                mostrarDicas(
                        "Faça um teste de fuga no quadro elétrico.",
                        "Verifique cabos e ligações junto ao contador.",
                        "Verifique se algum eletrodoméstico está a aquecer ou fazer ruído estranho."
                );
                btnAgendar.setVisibility(View.VISIBLE);

            } else if ("BAIXO".equals(status)) {
                configurarAlerta(
                        "✅ Excelente!",
                        String.format("Poupança de %.1f%% em relação à média!\n\n📅 Leitura: %s\n🔢 Valor: %.1f kWh",
                                Math.abs(percentagem), dataLeitura, valorLeitura),
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

            } else { // NORMAL
                if (percentagem >= 20.0) {
                    configurarAlerta(
                            "⚠️ Consumo Elevado",
                            String.format("Gastou cerca de %.0f%% a mais que o habitual.\n\n📅 Leitura: %s\n🔢 Valor: %.1f kWh",
                                    percentagem, dataLeitura, valorLeitura),
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
                            String.format("O consumo subiu %.1f%% face à média.\n\n📅 Leitura: %s\n🔢 Valor: %.1f kWh",
                                    percentagem, dataLeitura, valorLeitura),
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
                            String.format("Poupança de %.1f%% em relação à média!\n\n📅 Leitura: %s\n🔢 Valor: %.1f kWh",
                                    Math.abs(percentagem), dataLeitura, valorLeitura),
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

        } catch (android.database.sqlite.SQLiteException e) {
            Toast.makeText(this, "Erro ao aceder à base de dados.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Erro ao processar os dados.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    // Define a estética dos alertas
    private void configurarAlerta(String titulo, String msg, int cor, int iconRes) {
        tvTituloAlerta.setText(titulo);
        tvTituloAlerta.setTextColor(cor);
        tvMensagemAlerta.setText(msg);
        ivIconeAlerta.setImageResource(iconRes);
        ivIconeAlerta.setColorFilter(cor);
        tvTituloAlerta.setVisibility(View.VISIBLE);
        tvMensagemAlerta.setVisibility(View.VISIBLE);
        ivIconeAlerta.setVisibility(View.VISIBLE);
    }

    // Coloca as sugestões visíveis e define o texto das dicas
    private void mostrarDicas(String d1, String d2, String d3) {
        layoutSugestoes.setVisibility(View.VISIBLE);
        tvTituloSugestoes.setVisibility(View.VISIBLE);
        tvDica1.setText(d1);
        tvDica2.setText(d2);
        tvDica3.setText(d3);
    }
}