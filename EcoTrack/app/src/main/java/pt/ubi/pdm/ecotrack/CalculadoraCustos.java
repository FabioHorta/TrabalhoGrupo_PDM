package pt.ubi.pdm.ecotrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CalculadoraCustos extends BaseActivity {

    private TextView tvConsumoAtual, tvResultadoEuros, tvNomeCasaCalculadora;
    private EditText etPrecoKwh;
    private Button btnCalcular;
    private LinearLayout layoutResultado;

    private DBHelper dbHelper;
    private int casaIdAtual;
    private String casaNomeAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculadora_custos);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DBHelper(this);

        // Multi-casa
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        tvNomeCasaCalculadora = findViewById(R.id.tvNomeCasaCalculadora);
        tvConsumoAtual = findViewById(R.id.tvConsumoAtual);
        tvResultadoEuros = findViewById(R.id.tvResultadoEuros);
        etPrecoKwh = findViewById(R.id.etPrecoKwh);
        btnCalcular = findViewById(R.id.btnCalcular);
        layoutResultado = findViewById(R.id.layoutResultado);

        // Mostrar nome da casa
        tvNomeCasaCalculadora.setText(casaNomeAtual);

        carregarConsumoAtual();

        btnCalcular.setOnClickListener(v -> {
            String precoStr = etPrecoKwh.getText().toString().trim();
            if (precoStr.isEmpty()) {
                etPrecoKwh.setError("Indica o preço por kWh");
                return;
            }

            double preco;
            try {
                preco = Double.parseDouble(precoStr);
            } catch (NumberFormatException e) {
                etPrecoKwh.setError("Valor inválido");
                return;
            }

            double consumoMes = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
            if (consumoMes <= 0) {
                tvResultadoEuros.setText("€ 0,00");
                layoutResultado.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Sem consumos registados para esta casa.", Toast.LENGTH_SHORT).show();
                return;
            }

            double estimativa = consumoMes * preco;
            tvResultadoEuros.setText(String.format("€ %.2f", estimativa));
            layoutResultado.setVisibility(View.VISIBLE);
        });

        // marcar este separador na bottom bar
        setupBottomNav(R.id.nav_simulador);
    }

    private void carregarConsumoAtual() {
        double consumoMes = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
        if (consumoMes > 0) {
            tvConsumoAtual.setText(String.format("%.1f", consumoMes));
        } else {
            tvConsumoAtual.setText("-- kWh");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Atualizar casa selecionada
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        if (tvNomeCasaCalculadora != null) {
            tvNomeCasaCalculadora.setText(casaNomeAtual);
        }

        carregarConsumoAtual();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_simulador);
        }
    }
}