package pt.ubi.pdm.ecotrack;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.Map;

public class Eletrodomesticos extends AppCompatActivity {

    private int casaId;
    private DBHelper dbHelper;
    private Map<String, Integer> quantidades = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eletrodomesticos);

        casaId = getIntent().getIntExtra("casa_id", -1);
        dbHelper = new DBHelper(this);

        carregarDadosExistentes();

        // COZINHA
        setupCard(R.id.cardFrigo, "❄️", "Frigorífico", "Cozinha");
        setupCard(R.id.cardCombinado, "❄️🧊", "Frigorífico Combinado", "Cozinha");
        setupCard(R.id.cardForno, "🥘", "Forno", "Cozinha");
        setupCard(R.id.cardArca, "🧊", "Arca", "Cozinha");
        setupCard(R.id.cardPlaca, "🍳", "Placa", "Cozinha");
        setupCard(R.id.cardMicro, "🍿", "Microondas", "Cozinha");
        setupCard(R.id.cardFerro, "👕", "Ferro", "Cozinha");
        setupCard(R.id.cardRobot, "🤖", "Robot", "Cozinha");
        setupCard(R.id.cardCafe, "☕", "Máq. Café", "Cozinha");
        setupCard(R.id.cardChaleira, "🫖", "Chaleira", "Cozinha");

        // CLIMATIZAÇÃO
        setupCard(R.id.cardAC, "❄️💨", "Ar Condic.", "Climatização");
        setupCard(R.id.cardRadiador, "🌡️", "Radiador", "Climatização");
        setupCard(R.id.cardAquecedor, "🔥", "Aquecedor", "Climatização");
        setupCard(R.id.cardCaldeira, "🚿", "Caldeira", "Climatização");
        setupCard(R.id.cardBombaCalor, "⚙️", "Bomba Calor", "Climatização");
        setupCard(R.id.cardDesumidificador, "💧", "Desumidif.", "Climatização");
        setupCard(R.id.cardLareira, "🪵", "Lareira", "Climatização");
        setupCard(R.id.cardPiso, "👣", "Piso Aque.", "Climatização");

        // LAVAGENS
        setupCard(R.id.cardMaqLoica, "🍽️", "Máq. Loiça", "Lavagens");
        setupCard(R.id.cardMaqRoupa, "🧺", "Máq. Roupa", "Lavagens");
        setupCard(R.id.cardSecar, "🌀", "Máq. Secar", "Lavagens");

        // ENTRETENIMENTO
        setupCard(R.id.cardTV, "📺", "Televisão", "Entretenimento");
        setupCard(R.id.cardConsola, "🎮", "Consola", "Entretenimento");
        setupCard(R.id.cardPC, "💻", "Computador", "Entretenimento");

        // OUTROS
        setupCard(R.id.cardSolar, "☀️", "Paineis", "Outros");
        setupCard(R.id.cardEV, "🚗", "Carro Elét.", "Outros");
        setupCard(R.id.cardPiscina, "🏊", "Piscina", "Outros");
        setupCard(R.id.cardRega, "⛲", "Bomba Rega", "Outros");

        findViewById(R.id.btnConcluir).setOnClickListener(v -> {
            Toast.makeText(this, "Guardado!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupCard(int cardId, String emoji, String nome, String categoria) {
        MaterialCardView card = findViewById(cardId); // Agora usamos MaterialCardView
        TextView tvEmoji = card.findViewById(R.id.tvEmoji);
        TextView tvNome = card.findViewById(R.id.tvNome);
        TextView tvQtd = card.findViewById(R.id.tvQtd);
        ImageButton btnMinus = card.findViewById(R.id.btnMinus);
        ImageButton btnPlus = card.findViewById(R.id.btnPlus);

        tvEmoji.setText(emoji);
        tvNome.setText(nome);

        if (!quantidades.containsKey(nome)) quantidades.put(nome, 0);
        int valorInicial = quantidades.get(nome);

        tvQtd.setText(String.valueOf(valorInicial));
        atualizarVisualCard(card, valorInicial); // Define a cor inicial

        btnMinus.setOnClickListener(v -> {
            int qtd = quantidades.get(nome);
            if (qtd > 0) {
                qtd--;
                quantidades.put(nome, qtd);
                tvQtd.setText(String.valueOf(qtd));
                atualizarVisualCard(card, qtd); // Muda cor
                guardarNaBD(nome, categoria, qtd);
            }
        });

        btnPlus.setOnClickListener(v -> {
            int qtd = quantidades.get(nome);
            if (qtd < 50) {
                qtd++;
                quantidades.put(nome, qtd);
                tvQtd.setText(String.valueOf(qtd));
                atualizarVisualCard(card, qtd); // Muda cor
                guardarNaBD(nome, categoria, qtd);
            }
        });
    }

    // --- MAGIA VISUAL: Muda a cor da borda ---
    private void atualizarVisualCard(MaterialCardView card, int qtd) {
        if (qtd > 0) {
            // Selecionado: Borda Verde Grossa e Fundo ligeiramente verde
            card.setStrokeColor(Color.parseColor("#4CAF50"));
            card.setStrokeWidth(6); // Borda grossa
            card.setCardBackgroundColor(Color.parseColor("#F1F8E9")); // Fundo verde muito claro
        } else {
            // Vazio: Borda Cinza Fina e Fundo Branco
            card.setStrokeColor(Color.parseColor("#E0E0E0"));
            card.setStrokeWidth(2);
            card.setCardBackgroundColor(Color.WHITE);
        }
    }

    private void carregarDadosExistentes() {
        if (casaId == -1) return;
        Cursor c = dbHelper.obterEletrodomesticosDaCasa(casaId);
        if (c != null) {
            while (c.moveToNext()) {
                String nome = c.getString(c.getColumnIndexOrThrow(DBHelper.C_APP_NOME));
                int qtd = c.getInt(c.getColumnIndexOrThrow(DBHelper.C_APP_QUANTIDADE));
                quantidades.put(nome, qtd);
            }
            c.close();
        }
    }

    private void guardarNaBD(String nome, String categoria, int qtd) {
        if (casaId != -1) {
            dbHelper.atualizarEletrodomestico(casaId, nome, categoria, qtd);
        }
    }
}