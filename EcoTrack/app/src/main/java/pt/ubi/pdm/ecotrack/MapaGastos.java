package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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

    // --- ALTERAÇÃO 1: Variáveis para guardar o estado da casa ---
    private int casaId;
    private String casaNome;
    // -----------------------------------------------------------

    private double precoKwhUsuario = 0.20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_gastos);

        dbHelper = new DBHelper(this);

        // --- ALTERAÇÃO 2: Usar o Singleton em vez do Intent ---
        // Isto garante que apanha a casa que selecionaste no Spinner do Menu
        casaId = CasaSelecionada.getInstance().getCasaId();
        casaNome = CasaSelecionada.getInstance().getCasaNome();

        if (casaId == -1) {
            Toast.makeText(this, "Erro: Nenhuma casa selecionada!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // -----------------------------------------------------

        initViews();

        // Preencher logo o nome
        tvNomeCasaMapa.setText(casaNome);

        carregarDadosUsuario(); // Para saber o preço do kWh
        carregarDadosReais();
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

    // --- ALTERAÇÃO 3: Adicionar onResume ---
    // Se saíres e voltares, garante que os dados estão atualizados
    @Override
    protected void onResume() {
        super.onResume();
        casaId = CasaSelecionada.getInstance().getCasaId();
        casaNome = CasaSelecionada.getInstance().getCasaNome();

        if(tvNomeCasaMapa != null) {
            tvNomeCasaMapa.setText(casaNome);
        }

        // Recarregar dados caso tenhas mudado algo noutro ecrã
        carregarDadosReais();
    }
    // ----------------------------------------

    private void configurarBotoes() {
        btnBackArrow.setOnClickListener(v -> finish()); // Apenas fecha a atividade atual

        btnVoltarMenu.setOnClickListener(v -> {
            Intent intent = new Intent(MapaGastos.this, MenuPrincipal.class);
            // Limpa a pilha para não ficar tudo encavalitado
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnSimular.setOnClickListener(v -> {
            Intent intent = new Intent(MapaGastos.this, EstimativaConsumo.class);
            startActivity(intent);
        });
    }

    private void carregarDadosUsuario() {
        // Tenta buscar o preço do utilizador atual (via Singleton ou DB)
        String email = CasaSelecionada.getInstance().getUserEmail();
        if(email == null || email.isEmpty()) return;

        try {
            Cursor cUser = dbHelper.obterDadosUtilizadorPorEmail(email);
            if (cUser != null && cUser.moveToFirst()) {
                double preco = cUser.getDouble(cUser.getColumnIndexOrThrow(DBHelper.C_USER_PRECO_KWH));
                if (preco > 0) precoKwhUsuario = preco;
                cUser.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void carregarDadosReais() {
        Cursor cursor = dbHelper.obterEletrodomesticosDaCasa(casaId);

        if (cursor == null || cursor.getCount() == 0) {
            pieChart.clear(); // Limpa gráfico antigo
            pieChart.setNoDataText("Sem eletrodomésticos registados.");
            pieChart.setNoDataTextColor(Color.DKGRAY);
            layoutListaCategorias.removeAllViews(); // Limpa a lista
            tvSugestao.setText("Adicione eletrodomésticos em 'Caracterização' para ver a análise.");
            return;
        }

        Map<String, Double> consumoPorAparelho = new HashMap<>();
        double consumoTotalTotal = 0.0;

        while (cursor.moveToNext()) {
            String nome = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_NOME));
            String classe = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_CLASSE));

            // Consumo Mensal estimado
            double consumoMensal = DadosEnergeticos.getConsumoMensal(nome, classe);

            double atual = consumoPorAparelho.getOrDefault(nome, 0.0);
            consumoPorAparelho.put(nome, atual + consumoMensal);

            consumoTotalTotal += consumoMensal;
        }
        cursor.close();

        if (consumoTotalTotal > 0) {
            atualizarGrafico(consumoPorAparelho, consumoTotalTotal);
            atualizarListaCustos(consumoPorAparelho);
        } else {
            pieChart.clear();
            tvSugestao.setText("Consumo calculado é zero. Verifique as classes energéticas.");
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

        ArrayList<Integer> cores = new ArrayList<>();
        // Cores vivas para o gráfico
        cores.add(Color.parseColor("#E53935"));
        cores.add(Color.parseColor("#1E88E5"));
        cores.add(Color.parseColor("#43A047"));
        cores.add(Color.parseColor("#FDD835"));
        cores.add(Color.parseColor("#8E24AA"));
        cores.add(Color.parseColor("#FB8C00"));
        cores.add(Color.parseColor("#00ACC1"));

        dataSet.setColors(cores);
        dataSet.setDrawValues(false); // Sem texto dentro das fatias (mais limpo)

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Total\n" + String.format("%.0f kWh", total));
        pieChart.setCenterTextSize(14f);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);

        // Legenda
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setWordWrapEnabled(true);

        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void atualizarListaCustos(Map<String, Double> dados) {
        layoutListaCategorias.removeAllViews();

        String maiorGastoNome = "";
        double maxVal = 0;

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

        if (!maiorGastoNome.isEmpty()) {
            tvSugestao.setText("💡 Dica: O seu maior consumidor é " + maiorGastoNome + ". Tente otimizar o seu uso.");
        }
    }

    private void criarItemLista(String nome, double custo, double kwh) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(30, 30, 30, 30);
        item.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        item.setLayoutParams(params);
        item.setGravity(Gravity.CENTER_VERTICAL);

        // Ícone simples
        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_info_details);
        icon.setColorFilter(Color.parseColor("#1E4D42"));
        icon.setLayoutParams(new LinearLayout.LayoutParams(60, 60));

        // Texto
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textLayout.setPadding(30, 0, 0, 0);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(nome);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        tvTitulo.setTextColor(Color.BLACK);

        TextView tvKwh = new TextView(this);
        tvKwh.setText(String.format("%.1f kWh/mês", kwh));
        tvKwh.setTextColor(Color.DKGRAY);

        textLayout.addView(tvTitulo);
        textLayout.addView(tvKwh);

        // Preço
        TextView tvPreco = new TextView(this);
        tvPreco.setText(String.format("€ %.2f", custo));
        tvPreco.setTypeface(null, Typeface.BOLD);
        tvPreco.setTextColor(Color.parseColor("#388E3C"));

        item.addView(icon);
        item.addView(textLayout);
        item.addView(tvPreco);

        layoutListaCategorias.addView(item);
    }
}