package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapaGastos extends AppCompatActivity {

    private PieChart pieChart;
    private Button btnSimular, btnVoltarMenu;
    private ImageView btnBackArrow;
    private LinearLayout layoutListaCategorias;
    private TextView tvSugestao, tvNomeCasaMapa;

    private DBHelper dbHelper;
    private int casaId;
    private double precoKwhUsuario = 0.22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_gastos);

        dbHelper = new DBHelper(this);
        casaId = getIntent().getIntExtra("casa_id", -1);

        if (casaId == -1) {
            Toast.makeText(this, "Erro: Casa não identificada!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        carregarNomeDaCasa();
        carregarDadosReais(); // Agora carrega por APARELHO
        configurarBotoes();
    }

    private void initViews() {
        pieChart = findViewById(R.id.pieChart);
        btnSimular = findViewById(R.id.btnSimular);
        btnVoltarMenu = findViewById(R.id.btnVoltarMenu);
        btnBackArrow = findViewById(R.id.btnBackArrow);
        layoutListaCategorias = findViewById(R.id.layoutListaCategorias);
        tvSugestao = findViewById(R.id.tvSugestao);
        tvNomeCasaMapa = findViewById(R.id.tvNomeCasaMapa);
    }

    private void configurarBotoes() {
        View.OnClickListener voltarListener = v -> finish();
        btnVoltarMenu.setOnClickListener(voltarListener);
        btnBackArrow.setOnClickListener(voltarListener);

        btnSimular.setOnClickListener(v -> {
            Intent intent = new Intent(MapaGastos.this, EstimativaConsumo.class);
            intent.putExtra("casa_id", casaId);
            startActivity(intent);
        });
    }

    private void carregarNomeDaCasa() {
        try {
            Cursor c = dbHelper.obterCasaPorId(casaId);
            if (c != null && c.moveToFirst()) {
                String nomeCasa = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_NOME));
                tvNomeCasaMapa.setText(nomeCasa);

                String emailDono = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_USER_EMAIL));
                carregarPrecoUsuario(emailDono);
                c.close();
            }
        } catch (Exception e) {
            Log.e("MAPA", "Erro: " + e.getMessage());
        }
    }

    private void carregarPrecoUsuario(String email) {
        try {
            Cursor cUser = dbHelper.obterDadosUtilizadorPorEmail(email);
            if (cUser != null && cUser.moveToFirst()) {
                double preco = cUser.getDouble(cUser.getColumnIndexOrThrow(DBHelper.C_USER_PRECO_KWH));
                if (preco > 0) precoKwhUsuario = preco;
                cUser.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- LÓGICA PRINCIPAL ALTERADA ---
    private void carregarDadosReais() {
        Cursor cursor = dbHelper.obterEletrodomesticosDaCasa(casaId);

        if (cursor == null || cursor.getCount() == 0) {
            pieChart.setNoDataText("Adicione eletrodomésticos para ver os gastos.");
            pieChart.setNoDataTextColor(Color.WHITE);
            return;
        }

        // Mapa para somar consumo por NOME DO APARELHO (e não categoria)
        Map<String, Double> consumoPorAparelho = new HashMap<>();
        double consumoTotalTotal = 0.0;

        while (cursor.moveToNext()) {
            String nome = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_NOME));
            String classe = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_CLASSE));

            // Consumo Mensal direto do CSV/Classe Auxiliar
            double consumoMensal = DadosEnergeticos.getConsumoMensal(nome, classe);

            // Somar ao mapa usando o NOME como chave (ex: "Frigorífico", "TV")
            double atual = consumoPorAparelho.getOrDefault(nome, 0.0);
            consumoPorAparelho.put(nome, atual + consumoMensal);

            consumoTotalTotal += consumoMensal;
        }
        cursor.close();

        if (consumoTotalTotal > 0) {
            atualizarGrafico(consumoPorAparelho, consumoTotalTotal);
            atualizarListaCustos(consumoPorAparelho);
        }
    }

    private void atualizarGrafico(Map<String, Double> dados, double total) {
        ArrayList<PieEntry> entradas = new ArrayList<>();

        for (Map.Entry<String, Double> entry : dados.entrySet()) {
            if (entry.getValue() > 0) {
                float percentagem = (float) ((entry.getValue() / total) * 100);
                entradas.add(new PieEntry(percentagem, entry.getKey()));
            }
        }

        PieDataSet dataSet = new PieDataSet(entradas, "");

        // --- DEFINIR CORES MANUAIS E BEM DIFERENTES ---
        ArrayList<Integer> cores = new ArrayList<>();

        // Paleta de Alto Contraste (Cores Vivas e Distintas)
        cores.add(Color.parseColor("#E53935")); // Vermelho Vivo
        cores.add(Color.parseColor("#1E88E5")); // Azul Forte
        cores.add(Color.parseColor("#43A047")); // Verde
        cores.add(Color.parseColor("#FDD835")); // Amarelo (Escuro para ver em fundo branco)
        cores.add(Color.parseColor("#8E24AA")); // Roxo
        cores.add(Color.parseColor("#FB8C00")); // Laranja
        cores.add(Color.parseColor("#00ACC1")); // Ciano
        cores.add(Color.parseColor("#D81B60")); // Rosa Choque
        cores.add(Color.parseColor("#3949AB")); // Índigo
        cores.add(Color.parseColor("#6D4C41")); // Castanho
        cores.add(Color.parseColor("#C0CA33")); // Lima
        cores.add(Color.parseColor("#546E7A")); // Cinza Azulado

        dataSet.setColors(cores);
        // --------------------------------------------------

        // Manter o gráfico "limpo" (sem texto dentro)
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Esconder labels de dentro do gráfico
        pieChart.setDrawEntryLabels(false);

        // Estética
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Total\nMensal");
        pieChart.setCenterTextColor(Color.DKGRAY);
        pieChart.setCenterTextSize(14f);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);

        // Legenda (Necessária para saber quem é quem)
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.getLegend().setTextSize(12f);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void atualizarListaCustos(Map<String, Double> dados) {
        layoutListaCategorias.removeAllViews();

        String maiorGastoNome = "";
        double maxVal = 0;

        // Ordenar seria ideal, mas HashMap não garante ordem.
        // Vamos iterar e encontrar o maior para a dica.
        for (Map.Entry<String, Double> entry : dados.entrySet()) {
            String nomeAparelho = entry.getKey();
            double kwhMensal = entry.getValue();
            double custoEstimado = kwhMensal * precoKwhUsuario;

            if (kwhMensal > maxVal) {
                maxVal = kwhMensal;
                maiorGastoNome = nomeAparelho;
            }

            criarItemLista(nomeAparelho, custoEstimado, kwhMensal);
        }

        tvSugestao.setText("💡 Dica: O seu maior gasto mensal é com " + maiorGastoNome + ".");
    }

    private void criarItemLista(String nome, double custo, double kwh) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(30, 30, 30, 30);
        item.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        item.setBackgroundTintList(getColorStateList(R.color.white));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        item.setLayoutParams(params);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setElevation(6f);

        // Ícone Inteligente baseado no nome
        ImageView icon = new ImageView(this);
        int iconRes = android.R.drawable.ic_menu_help;
        int color = Color.GRAY;

        String n = nome.toLowerCase();
        if (n.contains("frig") || n.contains("arca")) {
            iconRes = android.R.drawable.ic_menu_manage; color = Color.parseColor("#00BCD4"); // Ciano
        } else if (n.contains("tv") || n.contains("televisao") || n.contains("pc")) {
            iconRes = android.R.drawable.ic_menu_gallery; color = Color.parseColor("#7E57C2"); // Roxo
        } else if (n.contains("aqueci") || n.contains("ar cond") || n.contains("lareira")) {
            iconRes = android.R.drawable.ic_menu_directions; color = Color.parseColor("#FF7043"); // Laranja
        } else if (n.contains("lavar") || n.contains("loica") || n.contains("roupa")) {
            iconRes = android.R.drawable.ic_menu_compass; color = Color.parseColor("#42A5F5"); // Azul
        } else if (n.contains("solar") || n.contains("paineis")) {
            iconRes = android.R.drawable.ic_menu_day; color = Color.parseColor("#FFCA28"); // Amarelo
        }

        icon.setImageResource(iconRes);
        icon.setColorFilter(color);
        icon.setLayoutParams(new LinearLayout.LayoutParams(80, 80));

        // Texto
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textLayout.setPadding(30, 0, 0, 0);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(nome);
        tvTitulo.setTextSize(16f);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        tvTitulo.setTextColor(Color.BLACK);

        TextView tvKwh = new TextView(this);
        tvKwh.setText(String.format("%.1f kWh/mês", kwh));
        tvKwh.setTextSize(12f);
        tvKwh.setTextColor(Color.DKGRAY);

        textLayout.addView(tvTitulo);
        textLayout.addView(tvKwh);

        // Preço
        TextView tvPreco = new TextView(this);
        tvPreco.setText(String.format("€ %.2f", custo));
        tvPreco.setTextSize(16f);
        tvPreco.setTypeface(null, Typeface.BOLD);
        tvPreco.setTextColor(Color.parseColor("#388E3C")); // Verde Dinheiro

        item.addView(icon);
        item.addView(textLayout);
        item.addView(tvPreco);

        layoutListaCategorias.addView(item);
    }
}