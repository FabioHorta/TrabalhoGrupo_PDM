package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuPrincipal extends BaseActivity {

    // Cabeçalho
    private MaterialCardView cardPerfilTopo;
    private ImageView imgPerfilTopo;
    private TextView tvNomeUtilizador;

    // Seletor de casas
    private Spinner spinnerCasas;
    private TextView tvSemCasas;

    // Resumo
    private TextView tvConsumoResumo, tvCustoResumo, tvComparacaoResumo;
    private PieChart pieResumo;

    // Atalhos rápidos (Nota: cardLeituras foi removido do XML)
    private MaterialCardView cardMelhorEnergia, cardEstimativas, cardMapaGastos;
    private MaterialCardView cardAlertas, cardApoio;

    // Dados
    private DBHelper dbHelper;
    private double precoKwhAtual = 0.20;
    private List<CasaItem> casas = new ArrayList<>();
    private ArrayAdapter<CasaItem> adapterCasas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_principal_menu);

        dbHelper = new DBHelper(this);

        initViews();
        atualizarCabecalho();
        carregarListaCasas();
        configurarClicks();

        // Bottom Nav (item atual: home)
        setupBottomNav(R.id.nav_home);
    }

    private void initViews() {
        // Cabeçalho
        cardPerfilTopo = findViewById(R.id.cardPerfilTopo);
        imgPerfilTopo = findViewById(R.id.imgPerfilTopo);
        tvNomeUtilizador = findViewById(R.id.tvNomeUtilizador);

        // Seletor de casas
        spinnerCasas = findViewById(R.id.spinnerCasas);
        tvSemCasas = findViewById(R.id.tvSemCasas);

        // Resumo
        tvConsumoResumo = findViewById(R.id.tvConsumoResumo);
        tvCustoResumo = findViewById(R.id.tvCustoResumo);
        tvComparacaoResumo = findViewById(R.id.tvComparacaoResumo);
        pieResumo = findViewById(R.id.pieResumo);

        // Cards rápidos (Removido cardLeituras pois não existe no XML)
        cardMelhorEnergia = findViewById(R.id.cardMelhorEnergia);
        cardEstimativas = findViewById(R.id.cardEstimativas);
        cardMapaGastos = findViewById(R.id.cardMapaGastos);

        // Secção inferior
        cardAlertas = findViewById(R.id.cardAlertas);
        cardApoio = findViewById(R.id.cardApoio);
    }

    private String obterEmailSessao() {
        return getSharedPreferences("auth", MODE_PRIVATE)
                .getString("user_email", null);
    }

    private void carregarListaCasas() {
        String email = obterEmailSessao();
        if (email == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        casas.clear();

        Cursor c = dbHelper.listarCasasDoUtilizador(email);
        if (c != null && c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndexOrThrow(DBHelper.C_CASA_ID));
                String nome = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_NOME));
                casas.add(new CasaItem(id, nome));
            } while (c.moveToNext());
            c.close();
        }

        if (casas.isEmpty()) {
            // Sem casas
            spinnerCasas.setVisibility(View.GONE);
            tvSemCasas.setVisibility(View.VISIBLE);
            tvSemCasas.setText("Nenhuma casa registada. Cria uma primeira!");

            // Esconder secções de dados
            tvConsumoResumo.setText("Sem casa selecionada");
            tvCustoResumo.setText("Cria uma casa para ver dados");
            tvComparacaoResumo.setText("");
            pieResumo.setVisibility(View.GONE);

            // Desativar cards que dependem da casa
            cardMelhorEnergia.setEnabled(false);
            cardEstimativas.setEnabled(false);
            cardMapaGastos.setEnabled(false);
            cardAlertas.setEnabled(false);
        } else {
            // Há casas
            spinnerCasas.setVisibility(View.VISIBLE);
            tvSemCasas.setVisibility(View.GONE);

            adapterCasas = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, casas);
            spinnerCasas.setAdapter(adapterCasas);

            final String emailFinal = email;

            spinnerCasas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    CasaItem casaSelecionada = casas.get(position);

                    // Atualizar Singleton Global
                    CasaSelecionada.getInstance().setSelecionada(
                            casaSelecionada.getId(),
                            casaSelecionada.getNome(),
                            emailFinal
                    );

                    // Atualizar Gráficos para ESTA casa
                    carregarResumoConsumo(casaSelecionada.getId());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // Tentar selecionar a casa que já estava no Singleton (se houver)
            int casaAtualId = CasaSelecionada.getInstance().getCasaId();
            if (casaAtualId != -1) {
                for (int i = 0; i < casas.size(); i++) {
                    if (casas.get(i).getId() == casaAtualId) {
                        spinnerCasas.setSelection(i);
                        break;
                    }
                }
            } else if (!casas.isEmpty()) {
                spinnerCasas.setSelection(0);
            }

            // Reativar cards
            cardMelhorEnergia.setEnabled(true);
            cardEstimativas.setEnabled(true);
            cardMapaGastos.setEnabled(true);
            cardAlertas.setEnabled(true);
        }
    }

    private void configurarClicks() {
        cardPerfilTopo.setOnClickListener(v ->
                startActivity(new Intent(MenuPrincipal.this, PerfilUtilizador.class)));

        cardMelhorEnergia.setOnClickListener(v -> {
             startActivity(new Intent(this, TipoEnergia.class));
        });

        // Nota: As leituras agora são acessíveis pela BottomBar, não pelo card

        cardEstimativas.setOnClickListener(v -> {
            if (verificarCasaSelecionada()) startActivity(new Intent(this, EstimativaConsumo.class));
        });

        cardMapaGastos.setOnClickListener(v -> {
            if (verificarCasaSelecionada()) startActivity(new Intent(this, MapaGastos.class));
        });

        cardAlertas.setOnClickListener(v -> {
            if (verificarCasaSelecionada()) startActivity(new Intent(this, AlertasConsumo.class));
        });

        cardApoio.setOnClickListener(v ->
                startActivity(new Intent(this, ApoioCliente.class)));
    }

    private boolean verificarCasaSelecionada() {
        if (!CasaSelecionada.getInstance().temCasaSelecionada()) {
            Toast.makeText(this, "Por favor, seleciona uma casa.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void atualizarCabecalho() {
        String email = obterEmailSessao();

        if (email == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String nomeExibicao = "Utilizador";

        Cursor c = dbHelper.obterDadosUtilizadorPorEmail(email);
        if (c != null && c.moveToFirst()) {
            String nomeBd = c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_NAME));
            if (nomeBd != null && !nomeBd.isEmpty()) {
                nomeExibicao = nomeBd;
            }

            int idxPreco = c.getColumnIndex(DBHelper.C_USER_PRECO_KWH);
            if (idxPreco != -1) {
                double preco = c.getDouble(idxPreco);
                if (preco > 0) {
                    precoKwhAtual = preco;
                }
            }
            c.close();
        }

        tvNomeUtilizador.setText(nomeExibicao);

        // Foto de perfil
        String nomeFicheiro = "profile_" + email + ".png";
        File imgFile = new File(getFilesDir(), nomeFicheiro);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imgPerfilTopo.setImageBitmap(myBitmap);
        } else {
            imgPerfilTopo.setImageResource(R.drawable.ecotrack_logo);
        }
    }

    private void carregarResumoConsumo(int casaId) {
        pieResumo.setVisibility(View.VISIBLE);

        // 1. Calcular consumo do último período para ESTA CASA
        double consumoUltimoPeriodo = dbHelper.calcularMediaConsumosPorCasa(1, casaId);

        // 2. Calcular a média histórica (últimos 6 períodos) para ESTA CASA
        // Nota: Assumindo que tens este método no DBHelper. Se não, usa o genérico filtrando por casaId
        double mediaGeral7 = dbHelper.calcularMediaConsumosPorCasa(7, casaId);

        // Matemática para isolar os últimos 6 períodos excluindo o atual (aproximação)
        double media6 = (mediaGeral7 * 7.0 / 6.0) - (consumoUltimoPeriodo / 6.0);
        if (media6 < 0) media6 = 0; // Proteção contra valores negativos

        if (consumoUltimoPeriodo > 0) {
            tvConsumoResumo.setText(String.format(Locale.getDefault(),
                    "%.1f kWh (último período)", consumoUltimoPeriodo));
            double custoEstimado = consumoUltimoPeriodo * precoKwhAtual;
            tvCustoResumo.setText(String.format(Locale.getDefault(),
                    "≈ € %.2f neste período", custoEstimado));
        } else {
            tvConsumoResumo.setText("Sem leituras suficientes");
            tvCustoResumo.setText("Adiciona leituras para ver dados");
        }

        if (consumoUltimoPeriodo > 0 && media6 > 0) {
            double diffPercent = ((consumoUltimoPeriodo - media6) / media6) * 100.0;
            String texto;
            if (diffPercent > 0) {
                texto = String.format(Locale.getDefault(),
                        "↑ %.1f%% acima da média", diffPercent);
                tvComparacaoResumo.setTextColor(Color.parseColor("#D32F2F")); // Vermelho
            } else if (diffPercent < 0) {
                texto = String.format(Locale.getDefault(),
                        "↓ %.1f%% abaixo da média", Math.abs(diffPercent));
                tvComparacaoResumo.setTextColor(Color.parseColor("#388E3C")); // Verde
            } else {
                texto = "Em linha com a média";
                tvComparacaoResumo.setTextColor(Color.parseColor("#78909C")); // Cinza
            }
            tvComparacaoResumo.setText(texto);
        } else {
            tvComparacaoResumo.setText("Ainda sem histórico suficiente.");
        }

        configurarGraficoResumo(consumoUltimoPeriodo, media6);
    }

    private void configurarGraficoResumo(double consumoPeriodo, double media6) {
        ArrayList<PieEntry> entradas = new ArrayList<>();
        if (consumoPeriodo <= 0 && media6 <= 0) {
            entradas.add(new PieEntry(1f, "Sem dados"));
        } else {
            if (consumoPeriodo > 0) {
                entradas.add(new PieEntry((float) consumoPeriodo, "Atual"));
            }
            if (media6 > 0) {
                entradas.add(new PieEntry((float) media6, "Média"));
            }
        }

        PieDataSet dataSet = new PieDataSet(entradas, "");
        ArrayList<Integer> cores = new ArrayList<>();
        cores.add(Color.parseColor("#4CAF50")); // verde
        cores.add(Color.parseColor("#B2DFDB")); // verde muito claro
        dataSet.setColors(cores);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        // Remove descrição de cada fatia se forem "Sem dados"
        if (consumoPeriodo <= 0 && media6 <= 0) {
            dataSet.setDrawValues(false);
        }

        PieData data = new PieData(dataSet);
        pieResumo.setData(data);
        pieResumo.getDescription().setEnabled(false);
        pieResumo.setCenterText("kWh");
        pieResumo.setCenterTextSize(14f);
        pieResumo.setHoleRadius(50f);
        pieResumo.setTransparentCircleRadius(55f);
        pieResumo.getLegend().setEnabled(false); // Esconde a legenda para ficar mais limpo
        pieResumo.setEntryLabelColor(Color.WHITE);
        pieResumo.animateY(800);
        pieResumo.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarCabecalho();
        carregarListaCasas(); // Recarrega casas caso tenhas adicionado uma nova

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    // Classe interna para o Spinner
    static class CasaItem {
        private final int id;
        private final String nome;

        CasaItem(int id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        int getId() { return id; }
        String getNome() { return nome; }

        @Override
        public String toString() { return nome; }
    }
}