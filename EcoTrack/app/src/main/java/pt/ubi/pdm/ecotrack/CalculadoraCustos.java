package pt.ubi.pdm.ecotrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CalculadoraCustos extends BaseActivity {

    private TextView tvConsumoAtual, tvResultadoEuros;
    private EditText etPrecoKwh;
    private Button btnCalcular;
    private LinearLayout layoutResultado;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculadora_custos);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DBHelper(this);

        tvConsumoAtual = findViewById(R.id.tvConsumoAtual);
        tvResultadoEuros = findViewById(R.id.tvResultadoEuros);
        etPrecoKwh = findViewById(R.id.etPrecoKwh);
        btnCalcular = findViewById(R.id.btnCalcular);
        layoutResultado = findViewById(R.id.layoutResultado);

        double consumoMes = dbHelper.calcularMediaConsumos(1); // ou outro método que uses
        if (consumoMes > 0) {
            tvConsumoAtual.setText(String.format("%.1f kWh", consumoMes));
        } else {
            tvConsumoAtual.setText("-- kWh");
        }

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

            if (consumoMes <= 0) {
                tvResultadoEuros.setText("€ 0,00");
                layoutResultado.setVisibility(View.VISIBLE);
                return;
            }

            double estimativa = consumoMes * preco;
            tvResultadoEuros.setText(String.format("€ %.2f", estimativa));
            layoutResultado.setVisibility(View.VISIBLE);
        });

        // marcar este separador na bottom bar
        setupBottomNav(R.id.nav_simulador);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_simulador);
        }
    }
}
