package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class MenuPrincipal extends BaseActivity {

    // Cabeçalho
    private MaterialCardView cardPerfilTopo;
    private ImageView imgPerfilTopo;
    private TextView tvNomeUtilizador;

    // Resumo
    private TextView tvConsumoResumo, tvCustoResumo, tvComparacaoResumo;
    private PieChart pieResumo;

    // Atalhos rápidos
    private MaterialCardView cardMelhorEnergia, cardLeituras, cardEstimativas, cardMapaGastos;
    private MaterialCardView cardAlertas, cardApoio;

    private FirebaseAuth mAuth;
    private DBHelper dbHelper;

    private double precoKwhAtual = 0.20; // valor por defeito se não estiver na BD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // esconder a barra do topo para ficar igual às outras
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_principal_menu);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        initViews();
        atualizarCabecalho();
        carregarResumoConsumo();
        configurarClicks();

        // ligar a bottom bar (item atual: home)
        setupBottomNav(R.id.nav_home);
    }

    private void initViews() {
        // Cabeçalho
        cardPerfilTopo = findViewById(R.id.cardPerfilTopo);
        imgPerfilTopo = findViewById(R.id.imgPerfilTopo);
        tvNomeUtilizador = findViewById(R.id.tvNomeUtilizador);

        // Resumo + gráfico
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

    private void configurarClicks() {
        cardPerfilTopo.setOnClickListener(v ->
                startActivity(new Intent(MenuPrincipal.this, PerfilUtilizador.class)));

        cardMelhorEnergia.setOnClickListener(v ->
                startActivity(new Intent(this, TipoEnergia.class)));

        cardLeituras.setOnClickListener(v ->
                startActivity(new Intent(this, LeiturasMensais.class)));

        cardEstimativas.setOnClickListener(v ->
                startActivity(new Intent(this, EstimativaConsumo.class)));

        cardMapaGastos.setOnClickListener(v ->
                startActivity(new Intent(this, MapaGastos.class)));

        cardAlertas.setOnClickListener(v ->
                startActivity(new Intent(this, AlertasConsumo.class)));

        cardApoio.setOnClickListener(v ->
                startActivity(new Intent(this, ApoioCliente.class)));
    }

    private void atualizarCabecalho() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String email = user.getEmail();
            String nomeExibicao = "Utilizador";

            Cursor c = dbHelper.obterDadosUtilizadorPorEmail(email);
            if (c != null && c.moveToFirst()) {
                // Nome
                String nomeBd = c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_NAME));
                if (nomeBd != null && !nomeBd.isEmpty()) {
                    nomeExibicao = nomeBd;
                }

                // Preço kWh
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

        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void carregarResumoConsumo() {
        // Último período (diferença entre últimas 2 leituras)
        double consumoUltimoPeriodo = dbHelper.calcularMediaConsumos(1);
        // Média de 6 períodos para comparação
        double media6 = dbHelper.calcularMediaConsumos(6);

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
            if (diffPercent > 5) {
                texto = String.format(Locale.getDefault(),
                        "↑ %.1f%% acima da média dos últimos 6 períodos", diffPercent);
            } else if (diffPercent < -5) {
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
        cores.add(Color.parseColor("#4CAF50"));  // verde
        cores.add(Color.parseColor("#80CBC4"));  // verde claro
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
        carregarResumoConsumo();

        // garantir que o item "Início" fica selecionado
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }
}
