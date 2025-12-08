package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

// dependências para a API
import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.DicasResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertasConsumo extends BaseActivity {

    // declaração de views
    private ImageView ivIconeAlerta;
    private TextView tvNomeCasaAlertas, tvTituloAlerta, tvMensagemAlerta, tvDica1, tvDica2, tvDica3;
    private Button btnAgendarAssistencia;

    // variáveis de estado e helpers
    private DBHelper dbHelper;
    private int casaIdAtual;
    private String casaNomeAtual;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas_consumo);

        // esconde a action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DBHelper(this);
        // inicializa o retrofit
        api = ApiClient.getRetrofit(this).create(ApiService.class);

        // obtém o contexto da casa
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        ligarViews();
        tvNomeCasaAlertas.setText(casaNomeAtual);

        // inicia a lógica principal
        preencherAnalise();
        configurarClicks();
        setupBottomNav(R.id.nav_alertas);
    }

    // liga as variáveis aos IDs do layout
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

    // configura o clique do botão
    private void configurarClicks() {
        // abre a activity de agendamento
        btnAgendarAssistencia.setOnClickListener(
                v -> startActivity(new Intent(AlertasConsumo.this, AgendarAssistencia.class))
        );
    }

    // verifica a conectividade
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

    // realiza a análise e carrega as dicas (cache e/ou API)
    private void preencherAnalise() {

        // calcula o consumo do último período
        double consumoUltimo = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);

        // calcula a média de referência dos 6 períodos anteriores
        double media6 = (dbHelper.calcularMediaConsumos(7)) * ((double) 7 / 6) - (consumoUltimo / 6);

        String tipo;

        // classifica o tipo de consumo (alto, baixo, normal, inicio)
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

        // tenta carregar da cache primeiro
        DicasResponse cache = dbHelper.obterDicasCache(tipo);
        if (cache != null) {
            aplicarDicasNaUI(tipo, cache);
        } else {
            // fallback visual enquanto carrega
            tvTituloAlerta.setText("a carregar análise...");
            tvMensagemAlerta.setText("a obter recomendações de consumo.");
            tvDica1.setText("");
            tvDica2.setText("");
            tvDica3.setText("");
            ivIconeAlerta.setColorFilter(0xFF1976D2, PorterDuff.Mode.SRC_IN);
        }

        // se houver internet, busca dados atualizados no servidor
        if (temInternet()) {
            carregarDicasServidor(tipo);
        }
    }

    // chamada assíncrona à API
    private void carregarDicasServidor(String tipo) {
        api.getDicas(tipo).enqueue(new Callback<DicasResponse>() {
            @Override
            public void onResponse(Call<DicasResponse> call, Response<DicasResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                DicasResponse d = response.body();

                // aplica na UI e guarda na cache
                aplicarDicasNaUI(tipo, d);
                dbHelper.guardarDicasCache(tipo, d);
            }

            @Override
            public void onFailure(Call<DicasResponse> call, Throwable t) {
                // ignora, fica com o que está em cache
            }
        });
    }

    // preenche os campos com os dados e define a cor do ícone
    private void aplicarDicasNaUI(String tipo, DicasResponse d) {
        tvTituloAlerta.setText(d.titulo);
        tvMensagemAlerta.setText(d.mensagem);
        tvDica1.setText(d.dica1);
        tvDica2.setText(d.dica2);
        tvDica3.setText(d.dica3);

        // define a cor do ícone
        switch (tipo) {
            case "alto":
                ivIconeAlerta.setColorFilter(0xFFD32F2F, PorterDuff.Mode.SRC_IN); // vermelho
                break;
            case "baixo":
                ivIconeAlerta.setColorFilter(0xFF388E3C, PorterDuff.Mode.SRC_IN); // verde
                break;
            case "normal":
                ivIconeAlerta.setColorFilter(0xFFFFA000, PorterDuff.Mode.SRC_IN); // amarelo
                break;
            default: // inicio
                ivIconeAlerta.setColorFilter(0xFF1976D2, PorterDuff.Mode.SRC_IN); // azul
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // atualiza o contexto da casa
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        if (tvNomeCasaAlertas != null) {
            tvNomeCasaAlertas.setText(casaNomeAtual);
        }

        // recarrega a análise
        preencherAnalise();

        // seleciona o item na navegação
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_alertas);
        }
    }
}