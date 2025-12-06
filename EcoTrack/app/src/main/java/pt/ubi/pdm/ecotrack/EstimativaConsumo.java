package pt.ubi.pdm.ecotrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class EstimativaConsumo extends AppCompatActivity {

    private static final double PRECO_KWH = 0.40;

    private TextView tvNomeCasaEstimativa;
    private TextView tvAlerta;
    private TextView tvSugestao;
    private TextView tvConsumoAtual;
    private TextView tvCustoAtual;
    private TextView tvResultado;
    private EditText etReducao;
    private Button btnCalcular;
    private Button btnVoltar;

    private DBHelper dbHelper;
    private int casaIdAtual;
    private String casaNomeAtual;
    private double consumoBaseKwh = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_estimativa_consumo);

        dbHelper = new DBHelper(this);

        // Multi-casa
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        ligarViews();
        carregarDadosBase();
        configurarBotoes();
    }

    private void ligarViews() {
        tvNomeCasaEstimativa = findViewById(R.id.tvNomeCasaEstimativa);
        tvAlerta = findViewById(R.id.tvAlerta);
        tvSugestao = findViewById(R.id.tvSugestao);
        tvConsumoAtual = findViewById(R.id.tvConsumoAtual);
        tvCustoAtual = findViewById(R.id.tvCustoAtual);
        tvResultado = findViewById(R.id.tvResultado);
        etReducao = findViewById(R.id.etReducao);
        btnCalcular = findViewById(R.id.btnCalcular);
        btnVoltar = findViewById(R.id.btnVoltar);

        // Mostrar nome da casa
        tvNomeCasaEstimativa.setText(casaNomeAtual);
    }

    private void carregarDadosBase() {
        Cursor cursor = null;
        double media6 = 0.0;
        double consumoAtualPeriodo = 0.0;
        double percentagem = 0.0;
        String status = null;

        try {
            double consumoUltimoPeriodo = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
            double mediaGeral7 = dbHelper.calcularMediaConsumosPorCasa(7, casaIdAtual);
            media6 = (mediaGeral7 * 7.0 / 6.0) - (consumoUltimoPeriodo / 6.0);

            cursor = dbHelper.obterHistoricoConsumosAnalisados(null);
            if (cursor != null && cursor.moveToFirst()) {
                int idxConsumo = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_VALOR);
                int idxMediaRef = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_MEDIA_REF);
                int idxPercent = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_PERCENTAGEM);
                int idxStatus = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_STATUS);

                if (idxConsumo != -1) {
                    consumoAtualPeriodo = cursor.getDouble(idxConsumo);
                }
                if (idxMediaRef != -1 && media6 <= 0) {
                    media6 = cursor.getDouble(idxMediaRef);
                }
                if (idxPercent != -1) {
                    percentagem = cursor.getDouble(idxPercent);
                }
                if (idxStatus != -1) {
                    status = cursor.getString(idxStatus);
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        consumoBaseKwh = media6 > 0 ? media6 : consumoAtualPeriodo;

        if (consumoBaseKwh <= 0) {
            tvAlerta.setText("Ainda não existem dados suficientes para análise de consumo.");
            tvSugestao.setText("Sugestão: regista pelo menos duas leituras em \"Leituras Mensais\" para ver alertas e estimativas.");
            tvConsumoAtual.setText("Consumo médio atual: -");
            tvCustoAtual.setText("Custo médio mensal: -");
            btnCalcular.setEnabled(false);
            return;
        }

        double custoMedio = consumoBaseKwh * PRECO_KWH;
        tvConsumoAtual.setText(String.format("Consumo médio atual: %.1f kWh", consumoBaseKwh));
        tvCustoAtual.setText(String.format("Custo médio mensal: €%.2f", custoMedio));

        if ("ALTO".equals(status)) {
            tvAlerta.setText(String.format(
                    "O consumo do último período está %.1f%% acima da média. Consumo médio: %.1f kWh.",
                    percentagem, consumoBaseKwh));

            double poupancaPotencial = Math.max(0, consumoAtualPeriodo - consumoBaseKwh) * PRECO_KWH;
            tvSugestao.setText(String.format(
                    "Sugestão: reduz o uso de aquecimento e grandes eletrodomésticos em cerca de 15%% para poupares aproximadamente €%.2f/mês.",
                    poupancaPotencial));

        } else if ("BAIXO".equals(status)) {
            tvAlerta.setText(String.format(
                    "Bom trabalho! O consumo do último período está %.1f%% abaixo da média.",
                    Math.abs(percentagem)));
            tvSugestao.setText("Sugestão: mantém estes hábitos eficientes. Verifica se há mais aparelhos em standby que possas desligar.");

        } else {
            tvAlerta.setText("O consumo recente está próximo da média habitual.");
            tvSugestao.setText("Sugestão: pequenas reduções em aquecimento, iluminação e standby podem gerar poupanças visíveis no próximo mês.");
        }
    }

    private void configurarBotoes() {
        btnCalcular.setOnClickListener(v -> calcularEstimativa());
        btnVoltar.setOnClickListener(v -> finish());
    }

    private void calcularEstimativa() {
        String valorTxt = etReducao.getText().toString().trim();
        if (valorTxt.isEmpty()) {
            Toast.makeText(this, "Insere a percentagem de redução pretendida.", Toast.LENGTH_SHORT).show();
            return;
        }

        double reducao;
        try {
            reducao = Double.parseDouble(valorTxt);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor inválido. Usa apenas números.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reducao < 0 || reducao > 100) {
            Toast.makeText(this, "Introduz uma percentagem entre 0 e 100.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (consumoBaseKwh <= 0) {
            Toast.makeText(this, "Ainda não existem dados suficientes para calcular a estimativa.", Toast.LENGTH_SHORT).show();
            return;
        }

        double fator = (100.0 - reducao) / 100.0;
        double consumoEstimado = consumoBaseKwh * fator;

        double custoAtual = consumoBaseKwh * PRECO_KWH;
        double custoEstimado = consumoEstimado * PRECO_KWH;
        double poupanca = custoAtual - custoEstimado;

        String resultado = String.format(
                "Com uma redução de %.1f%%:\n\n" +
                        "• Consumo estimado: %.1f kWh\n" +
                        "• Custo estimado: €%.2f\n" +
                        "• Poupança aproximada: €%.2f/mês",
                reducao, consumoEstimado, custoEstimado, poupanca
        );

        tvResultado.setText(resultado);
    }

    @Override
    protected void onResume() {
        super.onResume();

        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        tvNomeCasaEstimativa.setText(casaNomeAtual);
    }
}