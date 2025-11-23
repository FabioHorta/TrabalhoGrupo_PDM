package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class MapaGastos extends AppCompatActivity {

    private PieChart pieChart;
    private Button btnSimular, btnVoltarMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_gastos);

        initViews();
        configurarGrafico();
        configurarBotoes();
    }

    private void initViews() {
        pieChart = findViewById(R.id.pieChart);
        btnSimular = findViewById(R.id.btnSimular);
        btnVoltarMenu = findViewById(R.id.btnVoltarMenu);
    }

    private void configurarBotoes() {
        // 1. Botão Voltar ao Menu
        btnVoltarMenu.setOnClickListener(v -> {
            finish(); // Apenas fecha esta atividade e volta à anterior (Menu)
        });

        // 2. Botão Simular Redução
        btnSimular.setOnClickListener(v -> {
            // Redireciona para a Activity de Estimativa
            Intent intent = new Intent(MapaGastos.this, EstimativaConsumo.class);
            startActivity(intent);

            // Opcional: Mostrar um aviso pequeno
            Toast.makeText(this, "A abrir simulador...", Toast.LENGTH_SHORT).show();
        });
    }

    private void configurarGrafico() {
        // Criar a lista de dados (percentagens que tens no layout)
        ArrayList<PieEntry> entradas = new ArrayList<>();
        entradas.add(new PieEntry(35f, "Aquecimento"));
        entradas.add(new PieEntry(20f, "Iluminação"));
        entradas.add(new PieEntry(45f, "Eletrodomésticos"));

        // Configurar as cores
        ArrayList<Integer> cores = new ArrayList<>();
        cores.add(Color.parseColor("#1565C0")); // Azul (Aquecimento)
        cores.add(Color.parseColor("#E65100")); // Laranja (Iluminação)
        cores.add(Color.parseColor("#2E7D32")); // Verde (Eletrodomésticos)

        // Criar o DataSet
        PieDataSet dataSet = new PieDataSet(entradas, "Categorias");
        dataSet.setColors(cores);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        // Juntar tudo no Data
        PieData data = new PieData(dataSet);

        // Aplicar ao gráfico
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false); // Remove a descrição "Description Label"
        pieChart.setCenterText("Gastos");
        pieChart.setCenterTextSize(16f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.animateY(1000); // Animação bonita ao abrir
        pieChart.invalidate(); // Atualizar visualização
    }
}