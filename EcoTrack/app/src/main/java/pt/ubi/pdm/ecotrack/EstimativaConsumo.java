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

    // Define o preço de referência por kWh para os cálculos de custo (€0.40).
    private static final double PRECO_KWH = 0.40;

    // Declaração dos componentes da interface (Views)
    private TextView tvNomeCasaEstimativa;
    private TextView tvAlerta;
    private TextView tvSugestao;
    private TextView tvConsumoAtual;
    private TextView tvCustoAtual;
    private TextView tvResultado;
    private EditText etReducao; // Campo para a percentagem de redução inserida pelo utilizador
    private Button btnCalcular;
    private Button btnVoltar;

    // Variáveis auxiliares e de contexto
    private DBHelper dbHelper; // Interface para a base de dados
    private int casaIdAtual;
    private String casaNomeAtual;
    private double consumoBaseKwh = 0.0; // Valor médio de consumo usado como base para as estimativas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Expande o conteúdo por baixo das barras do sistema
        setContentView(R.layout.activity_estimativa_consumo);

        dbHelper = new DBHelper(this); // Inicializa a conexão à base de dados

        // Obtém o ID e o nome da casa selecionada globalmente
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        ligarViews(); // Associa as variáveis aos elementos do layout
        carregarDadosBase(); // Calcula e exibe o consumo médio atual e alertas
        configurarBotoes(); // Define os listeners de clique para os botões
    }

    // Associa as variáveis de classe aos IDs das Views no layout
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

        // Define o nome da casa no cabeçalho do ecrã
        tvNomeCasaEstimativa.setText(casaNomeAtual);
    }

    // Carrega o consumo médio e alertas da base de dados e atualiza a interface
    private void carregarDadosBase() {
        Cursor cursor = null;
        double media6 = 0.0; // Média de consumo dos últimos 6 períodos
        double consumoAtualPeriodo = 0.0; // Valor da última leitura registrada
        double percentagem = 0.0;
        String status = null;

        try {
            // Cálculos para obter a média dos 6 períodos anteriores
            double consumoUltimoPeriodo = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
            double mediaGeral7 = dbHelper.calcularMediaConsumosPorCasa(7, casaIdAtual);
            media6 = (mediaGeral7 * 7.0 / 6.0) - (consumoUltimoPeriodo / 6.0);

            // Tenta obter o último alerta e status de análise da base de dados
            cursor = dbHelper.obterHistoricoConsumosAnalisados(null);
            if (cursor != null && cursor.moveToFirst()) {
                // Obtém os índices das colunas para extração de dados
                int idxConsumo = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_VALOR);
                int idxMediaRef = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_MEDIA_REF);
                int idxPercent = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_PERCENTAGEM);
                int idxStatus = cursor.getColumnIndex(DBHelper.C_CONSUMO_ANALISADO_STATUS);

                if (idxConsumo != -1) {
                    consumoAtualPeriodo = cursor.getDouble(idxConsumo);
                }
                // Usa a referência da BD se a média de 6 períodos calculada for zero
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
            // Garante que o Cursor é sempre fechado, mesmo que ocorram erros
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        // Define o consumo base (referência) para a estimativa
        consumoBaseKwh = media6 > 0 ? media6 : consumoAtualPeriodo;

        // Trata o caso de não haver dados suficientes na BD
        if (consumoBaseKwh <= 0) {
            tvAlerta.setText("Ainda não existem dados suficientes para análise de consumo.");
            tvSugestao.setText("Sugestão: regista pelo menos duas leituras em \"Leituras Mensais\" para ver alertas e estimativas.");
            tvConsumoAtual.setText("Consumo médio atual: -");
            tvCustoAtual.setText("Custo médio mensal: -");
            btnCalcular.setEnabled(false); // Desativa o botão de cálculo
            return;
        }

        // Calcula e exibe o consumo e custo médio atual
        double custoMedio = consumoBaseKwh * PRECO_KWH;
        tvConsumoAtual.setText(String.format("Consumo médio atual: %.1f kWh", consumoBaseKwh));
        tvCustoAtual.setText(String.format("Custo médio mensal: €%.2f", custoMedio));

        // Define a mensagem de alerta e sugestão com base no status da última análise
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

        } else { // Status Normal
            tvAlerta.setText("O consumo recente está próximo da média habitual.");
            tvSugestao.setText("Sugestão: pequenas reduções em aquecimento, iluminação e standby podem gerar poupanças visíveis no próximo mês.");
        }
    }

    // Configura os listeners dos botões
    private void configurarBotoes() {
        btnCalcular.setOnClickListener(v -> calcularEstimativa()); // Chama a função que processa a estimativa
        btnVoltar.setOnClickListener(v -> finish()); // Fecha a Activity atual
    }

    // Processa a percentagem de redução inserida pelo utilizador e calcula a poupança
    private void calcularEstimativa() {
        String valorTxt = etReducao.getText().toString().trim();
        // Valida se o campo de input está vazio
        if (valorTxt.isEmpty()) {
            Toast.makeText(this, "Insere a percentagem de redução pretendida.", Toast.LENGTH_SHORT).show();
            return;
        }

        double reducao;
        try {
            // Converte o input de texto para número
            reducao = Double.parseDouble(valorTxt);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor inválido. Usa apenas números.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Valida se a percentagem está dentro do intervalo [0, 100]
        if (reducao < 0 || reducao > 100) {
            Toast.makeText(this, "Introduz uma percentagem entre 0 e 100.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Valida se a base de consumo existe
        if (consumoBaseKwh <= 0) {
            Toast.makeText(this, "Ainda não existem dados suficientes para calcular a estimativa.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cálculo da redução e do novo consumo/custo
        double fator = (100.0 - reducao) / 100.0;
        double consumoEstimado = consumoBaseKwh * fator;

        double custoAtual = consumoBaseKwh * PRECO_KWH;
        double custoEstimado = consumoEstimado * PRECO_KWH;
        double poupanca = custoAtual - custoEstimado;

        // Formata o resultado para apresentação
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

        // Atualiza a informação da casa selecionada (após voltar a este ecrã)
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        tvNomeCasaEstimativa.setText(casaNomeAtual);
    }
}