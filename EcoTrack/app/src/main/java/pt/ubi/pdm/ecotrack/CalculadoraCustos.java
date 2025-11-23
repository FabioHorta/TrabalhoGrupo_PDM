package pt.ubi.pdm.ecotrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CalculadoraCustos extends AppCompatActivity {

    private DBHelper dbHelper;
    private TextView tvConsumoAtual, tvResultadoEuros;
    private EditText etPrecoKwh;
    private Button btnCalcular, btnVoltar;
    private LinearLayout layoutResultado;

    private double consumoKwh = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculadora_custos);

        dbHelper = new DBHelper(this);

        initViews();
        carregarConsumo();
    }

    private void initViews() {
        tvConsumoAtual = findViewById(R.id.tvConsumoAtual);
        tvResultadoEuros = findViewById(R.id.tvResultadoEuros);
        etPrecoKwh = findViewById(R.id.etPrecoKwh);
        btnCalcular = findViewById(R.id.btnCalcular);
        btnVoltar = findViewById(R.id.btnVoltar);
        layoutResultado = findViewById(R.id.layoutResultado);

        btnCalcular.setOnClickListener(v -> calcularPreco());
        btnVoltar.setOnClickListener(v -> finish());
    }

    private void carregarConsumo() {
        // VOLTÁMOS AO MÉTODO CORRETO PARA CONTADORES:
        // Pede "1" período, o que devolve (Última Leitura - Penúltima Leitura)
        consumoKwh = dbHelper.calcularMediaConsumos(1);

        if (consumoKwh > 0) {
            tvConsumoAtual.setText(String.format("%.1f kWh", consumoKwh));
            // Ativar botões
            etPrecoKwh.setEnabled(true);
            btnCalcular.setEnabled(true);
        } else {
            tvConsumoAtual.setText("0 kWh");
            // Se der 0, pode ser porque só tens 1 leitura (precisas de 2 para saber a diferença)
            Toast.makeText(this, "Precisa de pelo menos 2 leituras para calcular a diferença.", Toast.LENGTH_LONG).show();

            // Opcional: podes bloquear ou deixar calcular com 0
            etPrecoKwh.setEnabled(false);
            btnCalcular.setEnabled(false);
        }
    }

    private void calcularPreco() {
        String precoStr = etPrecoKwh.getText().toString().trim();

        if (precoStr.isEmpty()) {
            Toast.makeText(this, "Insira o preço por kWh.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tratamento para quem usa vírgula em vez de ponto
        precoStr = precoStr.replace(",", ".");

        try {
            double precoUnitario = Double.parseDouble(precoStr);
            double total = consumoKwh * precoUnitario;

            tvResultadoEuros.setText(String.format("€ %.2f", total));
            layoutResultado.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor inválido.", Toast.LENGTH_SHORT).show();
        }
    }
}