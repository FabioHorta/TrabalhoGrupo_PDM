package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.DicasResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertasConsumo extends BaseActivity {

    private ImageView ivIconeAlerta;
    private TextView tvNomeCasaAlertas, tvTituloAlerta, tvMensagemAlerta, tvDica1, tvDica2, tvDica3;
    private Button btnAgendarAssistencia;

    private DBHelper dbHelper;
    private int casaIdAtual;
    private String casaNomeAtual;

    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas_consumo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DBHelper(this);
        api = ApiClient.getRetrofit().create(ApiService.class);

        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        ligarViews();
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
        btnAgendarAssistencia.setOnClickListener(
                v -> startActivity(new Intent(AlertasConsumo.this, AgendarAssistencia.class))
        );
    }

    // ---------------------------------------------------------
    //  Verifica se há internet (mesmo estilo do SyncUtils)
    // ---------------------------------------------------------
    private boolean temInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    private void preencherAnalise() {

        double consumoUltimo = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);
        double media6 = (dbHelper.calcularMediaConsumos(7)) * ((double) 7 / 6) - (consumoUltimo / 6);

        String tipo;

        if (consumoUltimo <= 0 || media6 <= 0) {
            tipo = "inicio";
        } else {
            double diffPercent = ((consumoUltimo - media6) / media6) * 100.0;

            if (diffPercent > DBHelper.LIMITE_PERCENTUAL_SUP) {
                tipo = "alto";
            } else if (diffPercent < DBHelper.LIMITE_PERCENTUAL_INF) {
                tipo = "baixo";
            } else {
                tipo = "normal";
            }
        }

        // 1) Tentar mostrar logo o que estiver em cache
        DicasResponse cache = dbHelper.obterDicasCache(tipo);
        if (cache != null) {
            aplicarDicasNaUI(tipo, cache);
        } else {
            // fallback visual mínimo
            tvTituloAlerta.setText("A carregar análise...");
            tvMensagemAlerta.setText("A obter recomendações de consumo.");
            tvDica1.setText("");
            tvDica2.setText("");
            tvDica3.setText("");
            ivIconeAlerta.setColorFilter(0xFF1976D2, PorterDuff.Mode.SRC_IN);
        }

        // 2) Se houver internet, ir ao servidor e atualizar cache + UI
        if (temInternet()) {
            carregarDicasServidor(tipo);
        }
    }

    private void carregarDicasServidor(String tipo) {
        api.getDicas(tipo).enqueue(new Callback<DicasResponse>() {
            @Override
            public void onResponse(Call<DicasResponse> call, Response<DicasResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                DicasResponse d = response.body();

                // atualiza UI
                aplicarDicasNaUI(tipo, d);
                // guarda na cache offline
                dbHelper.guardarDicasCache(tipo, d);
            }

            @Override
            public void onFailure(Call<DicasResponse> call, Throwable t) {
                // se falhar, ficas só com o que estiver em cache
            }
        });
    }

    private void aplicarDicasNaUI(String tipo, DicasResponse d) {
        tvTituloAlerta.setText(d.titulo);
        tvMensagemAlerta.setText(d.mensagem);
        tvDica1.setText(d.dica1);
        tvDica2.setText(d.dica2);
        tvDica3.setText(d.dica3);

        switch (tipo) {
            case "alto":
                ivIconeAlerta.setColorFilter(0xFFD32F2F, PorterDuff.Mode.SRC_IN);
                break;
            case "baixo":
                ivIconeAlerta.setColorFilter(0xFF388E3C, PorterDuff.Mode.SRC_IN);
                break;
            case "normal":
                ivIconeAlerta.setColorFilter(0xFFFFA000, PorterDuff.Mode.SRC_IN);
                break;
            default: // "inicio" ou outros
                ivIconeAlerta.setColorFilter(0xFF1976D2, PorterDuff.Mode.SRC_IN);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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
