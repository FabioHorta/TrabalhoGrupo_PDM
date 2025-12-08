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

import java.util.HashMap;
import java.util.Map;

/**
 * Activity responsável por definir a Classe Energética (A, B, C...)
 * para cada eletrodoméstico que o utilizador adicionou à casa.
 *
 * Gera dinamicamente "Cards" para cada item encontrado na BD.
 */
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
            0xFFDC143C   // G - Vermelho
    };

    private String[] classeLetras = {"A", "B", "C", "D", "E", "F", "G"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classe_consumo);

        try {
            // Receber ID da casa (criada ou editada anteriormente)
            casaId = getIntent().getIntExtra("casa_id", -1);

            if (casaId == -1) {
                Toast.makeText(this, "Erro: Casa não encontrada!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            dbHelper = new DBHelper(this);

            // Views
            containerClasseConsumo = findViewById(R.id.containerClasseConsumo);
            btnProximo = findViewById(R.id.btnProximoClasse);
            btnVoltar = findViewById(R.id.btnVoltarClasse);

            // Carregar a lista
            carregarEletrodomesticosSelecionados();

            // --- BOTÃO PRÓXIMO ---
            btnProximo.setOnClickListener(v -> {
                // 1. Obter email do utilizador (SharedPreferences)
                String userEmail = getSharedPreferences("auth", MODE_PRIVATE)
                        .getString("user_email", "");

                // 2. Buscar nome da casa à BD para atualizar o Singleton
                String nomeCasa = "Casa Nova";
                Cursor c = dbHelper.obterCasaPorId(casaId);
                if (c != null && c.moveToFirst()) {
                    nomeCasa = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_NOME));
                    c.close();
                }

                // 3. ATUALIZAR O SINGLETON GLOBAL
                // Isto garante que o MapaGastos sabe que ESTA é a casa ativa
                CasaSelecionada.getInstance().setSelecionada(casaId, nomeCasa, userEmail);

                Toast.makeText(this, "Classes guardadas!", Toast.LENGTH_SHORT).show();

                SyncUtils.syncTudoAsync(ClasseConsumo.this);

                // 4. Abrir Mapa de Gastos
                Intent intent = new Intent(ClasseConsumo.this, MapaGastos.class);
                // Não é estritamente necessário o putExtra porque usamos o Singleton,
                // mas não faz mal deixar.
                intent.putExtra("casa_id", casaId);
                startActivity(intent);
                finish();
            });

            btnVoltar.setOnClickListener(v -> finish());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    //Itera sobre todos os eletrodomésticos da casa na BD e cria um Card visual para cada.
    private void carregarEletrodomesticosSelecionados() {
        containerClasseConsumo.removeAllViews(); // Limpar antes de adicionar

        try {
            Cursor cursor = dbHelper.obterEletrodomesticosDaCasa(casaId);

            if (cursor == null || cursor.getCount() == 0) {
                TextView tvVazio = new TextView(this);
                tvVazio.setText("Nenhum eletrodoméstico encontrado.");
                tvVazio.setPadding(20, 20, 20, 20);
                containerClasseConsumo.addView(tvVazio);
                if(cursor != null) cursor.close();
                return;
            }

            // Mapa para numerar os aparelhos (ex: TV #1, TV #2)
            Map<String, Integer> contadores = new HashMap<>();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.C_APP_ID));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_NOME));
                String categoria = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_CATEGORIA));
                String classeGuardada = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_APP_CLASSE));

                // Incrementar contador para este tipo de aparelho
                int count = contadores.getOrDefault(nome, 0) + 1;
                contadores.put(nome, count);

                String tituloExibicao = nome + " #" + count;

                // Criar o card
                adicionarCardClasseConsumo(id, tituloExibicao, categoria, classeGuardada);
            }
            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Cria programaticamente o Layout (Card) com RadioButtons A-G para um aparelho.
    private void adicionarCardClasseConsumo(int eletroId, String tituloExibicao, String categoria, String classeGuardada) {
        try {
            // Card Principal
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(32, 24, 32, 24);
            card.setBackgroundColor(0xFFFFFFFF);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 24); // Margem inferior
            card.setLayoutParams(cardParams);

            // Título
            TextView tvTitulo = new TextView(this);
            tvTitulo.setText("📦 " + tituloExibicao);
            tvTitulo.setTextSize(16);
            tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitulo.setTextColor(0xFF1E4D42);
            tvTitulo.setPadding(0, 0, 0, 16);
            card.addView(tvTitulo);

            // RadioGroup para as Classes A-G
            RadioGroup radioGroup = new RadioGroup(this);
            radioGroup.setOrientation(RadioGroup.HORIZONTAL);

            LinearLayout.LayoutParams rgParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            radioGroup.setLayoutParams(rgParams);

            // Criar botões A-G
            for (int i = 0; i < classeLetras.length; i++) {
                RadioButton rb = new RadioButton(this);
                rb.setText(classeLetras[i]);
                rb.setTextSize(14);
                rb.setTextColor(coresClasses[i]);

                // Cor da "bolinha"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rb.setButtonTintList(ColorStateList.valueOf(coresClasses[i]));
                }

                // Layout params para distribuir uniformemente
                LinearLayout.LayoutParams rbParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                );
                rb.setLayoutParams(rbParams);

                radioGroup.addView(rb);

                // Pré-selecionar se já existir na BD
                if (classeGuardada != null && classeGuardada.equals(classeLetras[i])) {
                    rb.setChecked(true);
                }
            }

            // Listener para guardar automaticamente
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                RadioButton rb = card.findViewById(checkedId);
                if (rb != null) {
                    String classeSelecionada = rb.getText().toString();
                    // Atualiza a linha específica na BD
                    dbHelper.atualizarClasseConsumoEletrodomestico(eletroId, classeSelecionada);
                }
            });

            card.addView(radioGroup);
            containerClasseConsumo.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}