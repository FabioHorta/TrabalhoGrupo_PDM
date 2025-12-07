package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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

public class MapaGastos extends AppCompatActivity {

    private PieChart pieChart;
    private Button btnSimular, btnVoltarMenu;
    private ImageView btnBackArrow;
    private LinearLayout layoutListaCategorias;
    private TextView tvSugestao, tvNomeCasaMapa;

    private DBHelper dbHelper;
    private int casaId;
    private String casaNome;
    private double precoKwhUsuario = 0.20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_gastos);

        dbHelper = new DBHelper(this);

        casaId = CasaSelecionada.getInstance().getCasaId();
        casaNome = CasaSelecionada.getInstance().getCasaNome();

        if (casaId == -1) {
            Toast.makeText(this, "Erro: Nenhuma casa selecionada!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        tvNomeCasaMapa.setText(casaNome);

        configurarBotoes();
        carregarDadosUsuario();
        carregarDadosReais();
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

    @Override
    protected void onResume() {
        super.onResume();
        casaId = CasaSelecionada.getInstance().getCasaId();
        casaNome = CasaSelecionada.getInstance().getCasaNome();
        if (tvNomeCasaMapa != null) {
            tvNomeCasaMapa.setText(casaNome);
        }
        carregarDadosReais();
    }

    private void configurarBotoes() {
        btnBackArrow.setOnClickListener(v -> finish());

        btnVoltarMenu.setOnClickListener(v -> {
            Intent intent = new Intent(MapaGastos.this, MenuPrincipal.class);
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
        String email = CasaSelecionada.getInstance().getUserEmail();
        if (email == null || email.isEmpty()) return;

        try {
            Cursor cUser = dbHelper.obterDadosUtilizadorPorEmail(email);
            if (cUser != null && cUser.moveToFirst()) {
                double preco = cUser.getDouble(cUser.getColumnIndexOrThrow(DBHelper.C_USER_PRECO_KWH));
                if (preco > 0) precoKwhUsuario = preco;
                cUser.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarDadosReais() {
        Cursor cursor = dbHelper.obterEletrodomesticosDaCasa(casaId);

        if (cursor == null || cursor.getCount() == 0) {
            pieChart.clear();
            pieChart.setNoDataText("Sem eletrodomésticos.");
            pieChart.setNoDataTextColor(Color.WHITE);
            layoutListaCategorias.removeAllViews();
            tvSugestao.setText("Adicione eletrodomésticos.");
            return;
        }

        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<Double> valores = new ArrayList<>();
        double consumoTotalTotal = 0.0;

        while (cursor.moveToNext()) {
            String nome = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_NOME));
            String classe = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_CLASSE));

            double consumoMensal = DadosEnergeticos.getConsumoMensal(nome, classe);

            int index = nomes.indexOf(nome);
            if (index >= 0) {
                valores.set(index, valores.get(index) + consumoMensal);
            } else {
                nomes.add(nome);
                valores.add(consumoMensal);
            }
            consumoTotalTotal += consumoMensal;
        }
        cursor.close();

        if (consumoTotalTotal > 0) {
            // USAR AS CORES NEON AQUI
            ArrayList<Integer> cores = gerarCores();
            atualizarGrafico(nomes, valores, consumoTotalTotal, cores);
            atualizarListaCustos(nomes, valores, cores);
        } else {
            pieChart.clear();
            tvSugestao.setText("Consumo zero.");
        }
    }

    // === 30 CORES MISTURADAS (Alto contraste entre vizinhos) ===
    private ArrayList<Integer> gerarCores() {
        ArrayList<Integer> cores = new ArrayList<>();

        // 1. Azul Forte (Frio)
        cores.add(Color.parseColor("#1976D2"));
        // 2. Laranja Vivo (Quente)
        cores.add(Color.parseColor("#F57C00"));
        // 3. Verde Floresta (Frio)
        cores.add(Color.parseColor("#388E3C"));
        // 4. Vermelho (Quente)
        cores.add(Color.parseColor("#D32F2F"));
        // 5. Ciano / Turquesa (Frio)
        cores.add(Color.parseColor("#0097A7"));
        // 6. Amarelo Torrado (Quente)
        cores.add(Color.parseColor("#FBC02D"));
        // 7. Roxo (Frio)
        cores.add(Color.parseColor("#7B1FA2"));
        // 8. Rosa Choque (Quente)
        cores.add(Color.parseColor("#C2185B"));
        // 9. Verde Lima (Frio/Claro)
        cores.add(Color.parseColor("#AFB42B"));
        // 10. Castanho (Neutro/Quente)
        cores.add(Color.parseColor("#795548"));

        // -- Reinicia ciclo com tons diferentes --

        // 11. Azul Claro (Frio)
        cores.add(Color.parseColor("#64B5F6"));
        // 12. Laranja avermelhado (Quente)
        cores.add(Color.parseColor("#E64A19"));
        // 13. Verde Azulado / Teal (Frio)
        cores.add(Color.parseColor("#00796B"));
        // 14. Vermelho Suave (Quente)
        cores.add(Color.parseColor("#E57373"));
        // 15. Indigo (Frio)
        cores.add(Color.parseColor("#303F9F"));
        // 16. Âmbar (Quente)
        cores.add(Color.parseColor("#FFA000"));
        // 17. Verde Água (Frio)
        cores.add(Color.parseColor("#4DB6AC"));
        // 18. Roxo Claro (Quente/Suave)
        cores.add(Color.parseColor("#BA68C8"));
        // 19. Azul Petróleo Escuro (Frio)
        cores.add(Color.parseColor("#006064"));
        // 20. Coral (Quente)
        cores.add(Color.parseColor("#FF8A65"));

        // -- Últimos 10 tons de contraste --

        // 21. Cinza Azulado (Neutro Frio)
        cores.add(Color.parseColor("#607D8B"));
        // 22. Ouro (Quente)
        cores.add(Color.parseColor("#FFD700"));
        // 23. Verde Esmeralda (Frio)
        cores.add(Color.parseColor("#2E7D32"));
        // 24. Bordo / Vinho (Quente)
        cores.add(Color.parseColor("#880E4F"));
        // 25. Azul Marinho (Frio)
        cores.add(Color.parseColor("#1565C0"));
        // 26. Areia / Bege Escuro (Neutro)
        cores.add(Color.parseColor("#A1887F"));
        // 27. Roxo Profundo (Frio)
        cores.add(Color.parseColor("#512DA8"));
        // 28. Laranja Claro (Quente)
        cores.add(Color.parseColor("#FFB74D"));
        // 29. Cinza Escuro (Neutro)
        cores.add(Color.parseColor("#455A64"));
        // 30. Amarelo Claro (Claro)
        cores.add(Color.parseColor("#FFF176"));

        return cores;
    }

    private void atualizarGrafico(ArrayList<String> nomes, ArrayList<Double> valores, double total, ArrayList<Integer> cores) {
        ArrayList<PieEntry> entradas = new ArrayList<>();

        for (int i = 0; i < nomes.size(); i++) {
            if (valores.get(i) > 0) {
                float percentagem = (float) ((valores.get(i) / total) * 100);
                entradas.add(new PieEntry(percentagem, nomes.get(i)));
            }
        }

        PieDataSet dataSet = new PieDataSet(entradas, "");
        dataSet.setColors(cores);
        dataSet.setSliceSpace(2f); // Linha branca a separar
        dataSet.setSelectionShift(5f);
        dataSet.setDrawValues(false); // Sem números no gráfico

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Sem nomes no gráfico
        pieChart.setDrawEntryLabels(false);
        // Sem legenda (usamos a lista)
        pieChart.getLegend().setEnabled(false);

        pieChart.getDescription().setEnabled(false);

        // Texto do centro
        pieChart.setCenterText("Total\n" + String.format("%.0f kWh", total));
        pieChart.setCenterTextColor(Color.WHITE); // Texto branco para ver bem
        pieChart.setCenterTextSize(16f);

        // Configuração do "Buraco"
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT); // Fundo transparente para ver o degradê
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setTransparentCircleColor(Color.parseColor("#22FFFFFF"));

        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void atualizarListaCustos(ArrayList<String> nomes, ArrayList<Double> valores, ArrayList<Integer> cores) {
        layoutListaCategorias.removeAllViews();

        String maiorGastoNome = "";
        double maxVal = 0;

        for (int i = 0; i < nomes.size(); i++) {
            String nome = nomes.get(i);
            double kwh = valores.get(i);
            int corAtual = cores.get(i % cores.size());

            if (kwh > 0) {
                double custo = kwh * precoKwhUsuario;

                if (kwh > maxVal) {
                    maxVal = kwh;
                    maiorGastoNome = nome;
                }

                criarItemLista(nome, custo, kwh, corAtual);
            }
        }

        if (!maiorGastoNome.isEmpty()) {
            tvSugestao.setText("💡 Dica: O maior consumidor é " + maiorGastoNome + ".");
        }
    }

    private void criarItemLista(String nome, double custo, double kwh, int cor) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(30, 30, 30, 30);
        item.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        item.setLayoutParams(params);
        item.setGravity(Gravity.CENTER_VERTICAL);

        // ÍCONE colorido com a cor NEON
        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_info_details);
        icon.setColorFilter(cor);
        icon.setLayoutParams(new LinearLayout.LayoutParams(60, 60));

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