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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

    // Atalhos rápidos
    private MaterialCardView cardMelhorEnergia, cardLeituras, cardEstimativas, cardMapaGastos;
    private MaterialCardView cardAlertas, cardApoio;

    // Dados
    private FirebaseAuth mAuth;
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
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        initViews();
        atualizarCabecalho();
        carregarListaCasas();
        configurarClicks();
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

        // Cards rápidos
        cardMelhorEnergia = findViewById(R.id.cardMelhorEnergia);
        cardLeituras = findViewById(R.id.cardLeituras);
        cardEstimativas = findViewById(R.id.cardEstimativas);
        cardMapaGastos = findViewById(R.id.cardMapaGastos);
        cardAlertas = findViewById(R.id.cardAlertas);
        cardApoio = findViewById(R.id.cardApoio);
    }

    private void carregarListaCasas() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String email = user.getEmail();
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
            cardMelhorEnergia.setEnabled(false);
            cardLeituras.setEnabled(false);
            cardEstimativas.setEnabled(false);
            cardMapaGastos.setEnabled(false);
            cardAlertas.setEnabled(false);
        } else {
            // Há casas
            spinnerCasas.setVisibility(View.VISIBLE);
            tvSemCasas.setVisibility(View.GONE);

            adapterCasas = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, casas);
            spinnerCasas.setAdapter(adapterCasas);

            spinnerCasas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    CasaItem casaSelecionada = casas.get(position);
                    CasaSelecionada.getInstance().setSelecionada(
                            casaSelecionada.getId(),
                            casaSelecionada.getNome(),
                            mAuth.getCurrentUser().getEmail()
                    );
                    carregarResumoConsumo(casaSelecionada.getId());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Selecionar primeira casa por padrão
            if (!casas.isEmpty()) {
                spinnerCasas.setSelection(0);
            }
        }
    }

    private void configurarClicks() {
        cardPerfilTopo.setOnClickListener(v ->
                startActivity(new Intent(MenuPrincipal.this, PerfilUtilizador.class)));

        cardMelhorEnergia.setOnClickListener(v -> {
            if (!verificarCasaSelecionada()) return;
            startActivity(new Intent(this, TipoEnergia.class));
        });

        cardLeituras.setOnClickListener(v -> {
            if (!verificarCasaSelecionada()) return;
            startActivity(new Intent(this, LeiturasMensais.class));
        });

        cardEstimativas.setOnClickListener(v -> {
            if (!verificarCasaSelecionada()) return;
            startActivity(new Intent(this, EstimativaConsumo.class));
        });

        cardMapaGastos.setOnClickListener(v -> {
            if (!verificarCasaSelecionada()) return;
            startActivity(new Intent(this, MapaGastos.class));
        });

        cardAlertas.setOnClickListener(v -> {
            if (!verificarCasaSelecionada()) return;
            startActivity(new Intent(this, AlertasConsumo.class));
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
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
    }

    private void carregarResumoConsumo(int casaId) {
        // Usar dados ESPECÍFICOS DA CASA SELECIONADA
        double consumoUltimoPeriodo = dbHelper.calcularMediaConsumosPorCasa(1, casaId);
        double media6 = dbHelper.calcularMediaConsumosPorCasa(6, casaId);

        if (consumoUltimoPeriodo > 0) {
            tvConsumoResumo.setText(String.format(Locale.getDefault(),
                    "%.1f kWh (último período)", consumoUltimoPeriodo));
            double custoEstimado = consumoUltimoPeriodo * precoKwhAtual;
            tvCustoResumo.setText(String.format(Locale.getDefault(),
                    "≈ € %.2f neste período", custoEstimado));
        } else {
            tvConsumoResumo.setText("Sem leituras suficientes");
            tvCustoResumo.setText("Adiciona leituras em \"Leituras\"");
        }

        if (consumoUltimoPeriodo > 0 && media6 > 0) {
            double diffPercent = ((consumoUltimoPeriodo - media6) / media6) * 100.0;
            String texto;
            if (diffPercent > 0) {
                texto = String.format(Locale.getDefault(),
                        "↑ %.1f%% acima da média dos últimos 6 períodos", diffPercent);
            } else if (diffPercent < 0) {
                texto = String.format(Locale.getDefault(),
                        "↓ %.1f%% abaixo da média dos últimos 6 períodos", Math.abs(diffPercent));
            } else {
                texto = "Em linha com a média dos últimos 6 períodos";
            }
            tvComparacaoResumo.setText(texto);
        } else {
            tvComparacaoResumo.setText("Ainda sem média histórica suficiente.");
        }

        configurarGraficoResumo(consumoUltimoPeriodo, media6);
    }

    private void configurarGraficoResumo(double consumoPeriodo, double media6) {
        ArrayList<PieEntry> entradas = new ArrayList<>();
        if (consumoPeriodo <= 0 && media6 <= 0) {
            entradas.add(new PieEntry(1f, "Sem dados"));
        } else {
            if (consumoPeriodo > 0) {
                entradas.add(new PieEntry((float) consumoPeriodo, "Último período"));
            }
            if (media6 > 0) {
                entradas.add(new PieEntry((float) media6, "Média 6 períodos"));
            }
        }

        PieDataSet dataSet = new PieDataSet(entradas, "");
        ArrayList<Integer> cores = new ArrayList<>();
        cores.add(Color.parseColor("#4CAF50")); // verde
        cores.add(Color.parseColor("#80CBC4")); // verde claro
        dataSet.setColors(cores);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieResumo.setData(data);
        pieResumo.getDescription().setEnabled(false);
        pieResumo.setCenterText("Consumo");
        pieResumo.setCenterTextSize(14f);
        pieResumo.setHoleRadius(60f);
        pieResumo.setTransparentCircleRadius(65f);
        pieResumo.getLegend().setEnabled(true);
        pieResumo.setEntryLabelColor(Color.WHITE);
        pieResumo.animateY(800);
        pieResumo.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarCabecalho();
        carregarListaCasas();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    // Classe interna para representar uma casa no spinner
    static class CasaItem {
        private int id;
        private String nome;

        CasaItem(int id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        int getId() {
            return id;
        }

        String getNome() {
            return nome;
        }

        @Override
        public String toString() {
            return nome;
        }
    }
}