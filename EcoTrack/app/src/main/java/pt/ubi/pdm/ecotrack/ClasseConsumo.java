package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import java.util.HashMap;
import java.util.Map;

public class ClasseConsumo extends AppCompatActivity {

    private int casaId;
    private DBHelper dbHelper;
    private LinearLayout containerClasseConsumo;
    private Button btnProximo, btnVoltar;

    // Array com cores das classes A-G
    private int[] coresClasses = {
            0xFF00A86B,  // A - Verde Vibrante
            0xFF3CB371,  // B - Verde Médio
            0xFFCDDC39,  // C - Amarelo Limão
            0xFFFFD700,  // D - Ouro
            0xFFFFA500,  // E - Laranja
            0xFFFF6347,  // F - Laranja Vermelho
            0xFFDC143C   // G - Vermelho Criado
    };

    private String[] classeLetras = {"A", "B", "C", "D", "E", "F", "G"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classe_consumo);

        Log.d("CLASSCONSUMO", "=== onCreate INICIADO ===");

        try {
            // ✅ RECEBER casa_id DO INTENT
            casaId = getIntent().getIntExtra("casa_id", -1);
            Log.d("CLASSCONSUMO", "casaId recebido: " + casaId);

            if (casaId == -1) {
                Toast.makeText(this, "Erro: Casa não encontrada!", Toast.LENGTH_SHORT).show();
                Log.e("CLASSCONSUMO", "casaId = -1, a terminar");
                finish();
                return;
            }

            dbHelper = new DBHelper(this);
            Log.d("CLASSCONSUMO", "DBHelper criado com sucesso");

            // Encontrar views
            containerClasseConsumo = findViewById(R.id.containerClasseConsumo);
            btnProximo = findViewById(R.id.btnProximoClasse);
            btnVoltar = findViewById(R.id.btnVoltarClasse);

            if (containerClasseConsumo == null) {
                Toast.makeText(this, "Erro: containerClasseConsumo não encontrado!", Toast.LENGTH_SHORT).show();
                Log.e("CLASSCONSUMO", "containerClasseConsumo = null");
                finish();
                return;
            }

            Log.d("CLASSCONSUMO", "Views encontradas com sucesso");

            // ✅ CARREGAR ELETROS SELECIONADOS DA BD
            carregarEletrodomesticosSelecionados();

            // ✅ BOTÃO PRÓXIMO - IR PARA MAPA DE GASTOS
            btnProximo.setOnClickListener(v -> {
                Log.d("CLASSCONSUMO", "Botão PRÓXIMO clicado");
                Toast.makeText(this, "Classes guardadas! Seguindo para Mapa de Gastos...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(ClasseConsumo.this, MapaGastos.class);
                intent.putExtra("casa_id", casaId);
                startActivity(intent);
                finish();
            });

            // ✅ BOTÃO VOLTAR
            btnVoltar.setOnClickListener(v -> {
                Log.d("CLASSCONSUMO", "Botão VOLTAR clicado");
                finish();
            });

            Log.d("CLASSCONSUMO", "=== onCreate COMPLETO ===");

        } catch (Exception e) {
            Log.e("CLASSCONSUMO", "ERRO em onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // Em ClasseConsumo.java

    private void carregarEletrodomesticosSelecionados() {
        containerClasseConsumo.removeAllViews(); // Limpa antes de adicionar

        Cursor cursor = dbHelper.obterEletrodomesticosDaCasa(casaId);

        // --- BLOCO DE DEBUG VISUAL ---
        if (cursor == null || cursor.getCount() == 0) {
            TextView tvVazio = new TextView(this);
            tvVazio.setText("⚠️ Nenhum eletrodoméstico encontrado na BD para a Casa ID: " + casaId + "\n\nVolte atrás e adicione equipamentos.");
            tvVazio.setTextColor(0xFFFF0000); // Vermelho
            tvVazio.setTextSize(18);
            tvVazio.setPadding(20, 20, 20, 20);
            containerClasseConsumo.addView(tvVazio);

            if(cursor != null) cursor.close();
            return;
        }
        // -----------------------------

        Map<String, Integer> contadores = new HashMap<>();

        while (cursor.moveToNext()) {
            // ... (o resto do código que te mandei antes) ...
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.C_APP_ID));
            String nome = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_NOME));
            String categoria = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_CATEGORIA));
            String classe = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_CLASSE));

            int count = contadores.getOrDefault(nome, 0) + 1;
            contadores.put(nome, count);

            adicionarCardClasseConsumo(id, nome + " #" + count, categoria, classe);
        }
        cursor.close();
    }
    private void adicionarCardClasseConsumo(int eletroId, String tituloExibicao, String categoria, String classeGuardada) {
        try {
            // ===== CARD PRINCIPAL =====
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 24, 24, 24);
            card.setBackgroundColor(0xFFFFFFFF);
            // Margem entre cartões
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 24); // Espaço em baixo
            card.setLayoutParams(cardParams);

            // ===== TÍTULO (Ex: 📦 Frigorífico #1) =====
            TextView tvTitulo = new TextView(this);
            tvTitulo.setText("📦 " + tituloExibicao);
            tvTitulo.setTextSize(16);
            tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitulo.setTextColor(0xFF1E4D42);
            tvTitulo.setPadding(0, 0, 0, 16);
            card.addView(tvTitulo);

            // ===== RADIOGROUP COM CLASSES A-G =====
            RadioGroup radioGroup = new RadioGroup(this);
            radioGroup.setOrientation(RadioGroup.HORIZONTAL);

            // Layout params para o grupo
            LinearLayout.LayoutParams rgParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            radioGroup.setLayoutParams(rgParams);

            // ===== CRIAR RADIOBUTTONS A-G =====
            String[] letras = {"A", "B", "C", "D", "E", "F", "G"};
            // Cores correspondentes (Verde -> Vermelho)
            int[] cores = {
                    0xFF00A86B, 0xFF3CB371, 0xFFCDDC39, 0xFFFFD700,
                    0xFFFFA500, 0xFFFF6347, 0xFFDC143C
            };

            for (int i = 0; i < letras.length; i++) {
                RadioButton rb = new RadioButton(this);
                rb.setText(letras[i]);
                rb.setTextSize(14);
                rb.setTextColor(cores[i]);

                // Define a cor da "bolinha" do rádio (API 21+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rb.setButtonTintList(ColorStateList.valueOf(cores[i]));
                }

                // Distribuir espaço igualmente
                LinearLayout.LayoutParams rbParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                );
                rb.setLayoutParams(rbParams);

                radioGroup.addView(rb);

                // Verificar se esta é a classe que já estava guardada na BD
                if (classeGuardada != null && classeGuardada.equals(letras[i])) {
                    rb.setChecked(true);
                }
            }

            // Se não houver classe guardada, podes querer selecionar "F" ou deixar vazio
            if (classeGuardada == null) {
                // Opcional: ((RadioButton) radioGroup.getChildAt(5)).setChecked(true);
            }

            // Listener para guardar logo que o utilizador clica
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                RadioButton rb = findViewById(checkedId);
                if (rb != null) {
                    String classeSelecionada = rb.getText().toString();
                    // Atualiza na BD usando o ID único da linha
                    dbHelper.atualizarClasseConsumoEletrodomestico(eletroId, classeSelecionada);
                }
            });

            card.addView(radioGroup);

            // Adicionar ao layout principal
            containerClasseConsumo.addView(card);

        } catch (Exception e) {
            Log.e("CLASSCONSUMO", "Erro ao criar card: " + e.getMessage());
        }
    }
}