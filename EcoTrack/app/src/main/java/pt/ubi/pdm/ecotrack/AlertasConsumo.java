package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AlertasConsumo extends BaseActivity {

    private ImageView ivIconeAlerta;
    private TextView tvNomeCasaAlertas, tvTituloAlerta, tvMensagemAlerta, tvDica1, tvDica2, tvDica3;
    private Button btnAgendarAssistencia, btnVoltarMenu;
    private DBHelper dbHelper;
    private int casaIdAtual;
    private String casaNomeAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas_consumo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DBHelper(this);

        // Multi-casa
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        ligarViews();

        // Mostrar nome da casa
        tvNomeCasaAlertas.setText(casaNomeAtual);

        preencherAnalise();
        configurarClicks();
        setupBottomNav(R.id.nav_alertas);
    }

    private void ligarViews() {
        tvNomeCasaAlertas = findViewById(R.id.tvNomeCasaAlertas);
        ivIconeAlerta = findViewById(R.id.ivIconeAlerta);
        tvTituloAlerta = findViewById(R.id.tvTituloAlerta);
        tvMensagemAlerta = findViewById(R.id.tvMensagemAlerta);
        tvDica1 = findViewById(R.id.tvDica1);
        tvDica2 = findViewById(R.id.tvDica2);
        tvDica3 = findViewById(R.id.tvDica3);
        btnAgendarAssistencia = findViewById(R.id.btnAgendarAssistencia);
    }

    private void configurarClicks() {
        btnAgendarAssistencia.setOnClickListener(v ->
                startActivity(new Intent(AlertasConsumo.this, AgendarAssistencia.class)));
    }

    private void preencherAnalise() {
        // ✅ CORRIGIDO: Usar dados ESPECÍFICOS DA CASA SELECIONADA
        double consumoUltimo = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
        double media6 = (dbHelper.calcularMediaConsumos(7)) * ((double) 7 /6) - (consumoUltimo/6);

        if (consumoUltimo <= 0 || media6 <= 0) {
            tvTituloAlerta.setText("Ainda sem dados suficientes");
            tvMensagemAlerta.setText("Regista leituras para ativar a análise inteligente.");
            ivIconeAlerta.setColorFilter(0xFF1976D2, PorterDuff.Mode.SRC_IN);
            tvDica1.setText("Garante que registas pelo menos 2 leituras em momentos diferentes.");
            tvDica2.setText("Tira foto do contador sempre com boa iluminação.");
            tvDica3.setText("Assim que houver histórico suficiente, iremos alertar sobre consumos anormais.");
            return;
        }

        double diffPercent = ((consumoUltimo - media6) / media6) * 100.0;

        if (diffPercent > DBHelper.LIMITE_PERCENTUAL_SUP) {
            tvTituloAlerta.setText("Consumo acima do normal");
            tvMensagemAlerta.setText(String.format(
                    "O último período está cerca de %.1f%% acima da média dos últimos meses.",
                    diffPercent
            ));
            ivIconeAlerta.setColorFilter(0xFFD32F2F, PorterDuff.Mode.SRC_IN);
            tvDica1.setText("Verifica se algum equipamento ficou ligado mais tempo do que o habitual.");
            tvDica2.setText("Confirma se não há avarias em aquecedores, ar condicionado ou termoacumulador.");
            tvDica3.setText("Se a situação se mantiver, considera agendar uma assistência técnica.");

        } else if (diffPercent < DBHelper.LIMITE_PERCENTUAL_INF) {
            tvTituloAlerta.setText("Boa eficiência energética");
            tvMensagemAlerta.setText(String.format(
                    "O último período está cerca de %.1f%% abaixo da média. Continua assim!",
                    Math.abs(diffPercent)
            ));
            ivIconeAlerta.setColorFilter(0xFF388E3C, PorterDuff.Mode.SRC_IN);
            tvDica1.setText("Mantém os hábitos que ajudaram a reduzir o consumo.");
            tvDica2.setText("Podes comparar os períodos no histórico de leituras.");
            tvDica3.setText("Explora o simulador para ver quanto podes poupar a longo prazo.");

        } else {
            tvTituloAlerta.setText("Consumo estável");
            tvMensagemAlerta.setText("O último período está dentro da normalidade face à média.");
            ivIconeAlerta.setColorFilter(0xFFFFA000, PorterDuff.Mode.SRC_IN);
            tvDica1.setText("Continua a registar leituras regularmente para manter o controlo.");
            tvDica2.setText("Analisa os períodos com maior consumo e tenta evitar picos.");
            tvDica3.setText("Se notar alterações inesperadas, volta a consultar esta análise.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Atualizar casa selecionada
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        if (tvNomeCasaAlertas != null) {
            tvNomeCasaAlertas.setText(casaNomeAtual);
        }

        preencherAnalise();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_alertas);
        }
    }
}