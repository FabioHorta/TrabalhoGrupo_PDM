package pt.ubi.pdm.ecotrack;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class CalculadoraCustos extends BaseActivity {

    private TextView tvConsumoAtual, tvResultadoEuros, tvNomeCasaCalculadora;
    private EditText etPrecoKwh;
    private Button btnCalcular;
    private LinearLayout layoutResultado;

    private DBHelper dbHelper;
    private int casaIdAtual;
    private String casaNomeAtual;

    // para ir buscar/guardar o preço associado ao utilizador
    private String emailUtilizador;

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

        // Obter email do utilizador autenticado (para ir buscar preco_kwh à BD local)
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailUtilizador = sp.getString("user_email", null);

        // Carregar preço/kWh guardado localmente (cache offline na tabela users)
        carregarPrecoKwhGuardado();

        // Mostrar consumo actual (média/consumo do último período da casa)
        carregarConsumoAtual();

        btnCalcular.setOnClickListener(v -> calcularEstimativa());

        // marcar este separador na bottom bar
        setupBottomNav(R.id.nav_simulador);
    }

    private void carregarPrecoKwhGuardado() {
        if (emailUtilizador == null) {
            return;
        }

        Cursor c = dbHelper.obterDadosUtilizadorPorEmail(emailUtilizador);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int idxPreco = c.getColumnIndex(DBHelper.C_USER_PRECO_KWH);
                    if (idxPreco >= 0) {
                        double preco = c.getDouble(idxPreco);
                        if (preco > 0) {
                            // Preenche o campo com o preço gravado (cache offline)
                            etPrecoKwh.setText(String.format(Locale.getDefault(), "%.4f", preco));
                        }
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    /**
     * Realiza o cálculo: (Total kWh da Casa) * (Preço inserido).
     * Guarda o novo preço na BD para uso futuro.
     */
    private void calcularEstimativa() {
        String precoStr = etPrecoKwh.getText().toString().trim();
        if (precoStr.isEmpty()) {
            etPrecoKwh.setError("Indica o preço por kWh");
            return;
        }

        double preco;
        try {
            preco = Double.parseDouble(precoStr.replace(",", "."));
        } catch (NumberFormatException e) {
            etPrecoKwh.setError("Valor inválido");
            return;
        }

        // Consumo “mensal”/último período da casa actual
        double consumoMes = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
        if (consumoMes <= 0) {
            tvResultadoEuros.setText("€ 0,00");
            layoutResultado.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Sem consumos registados para esta casa.", Toast.LENGTH_SHORT).show();
            return;
        }

        double estimativa = consumoMes * preco;
        tvResultadoEuros.setText(String.format(Locale.getDefault(), "€ %.2f", estimativa));
        layoutResultado.setVisibility(View.VISIBLE);

        // Guardar/preferir este preço na BD local como cache offline
        if (emailUtilizador != null) {
            dbHelper.atualizarPrecoUtilizador(emailUtilizador, preco);

            // se quiseres reforçar a cache também em SharedPreferences:
            SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
            sp.edit().putFloat("preco_kwh_cache", (float) preco).apply();
        }
    }

    private void carregarConsumoAtual() {
        double consumoMes = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
        if (consumoMes > 0) {
            tvConsumoAtual.setText(String.format(Locale.getDefault(), "%.1f kWh", consumoMes));
        } else {
            tvConsumoAtual.setText("-- kWh");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Atualizar casa selecionada (se o utilizador mudou de casa entretanto)
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
