package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

//Activity de catálogo onde o utilizador seleciona QUE aparelhos tem em casa.

public class Eletrodomesticos extends AppCompatActivity {

    private int casaId;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eletrodomesticos);

        try {
            // 1. Receber ID da Casa
            casaId = getIntent().getIntExtra("casa_id", -1);

            if (casaId == -1) {
                Toast.makeText(this, "Erro: Casa não encontrada!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            dbHelper = new DBHelper(this);

            // 2. Configurar Cards (Inicializar UI com dados da BD)

            // ===== COZINHA =====
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

            // ===== CLIMATIZAÇÃO =====
            setupCard(R.id.cardAC, "❄️💨", "Ar Condic.", "Climatização");
            setupCard(R.id.cardRadiador, "🌡️", "Radiador", "Climatização");
            setupCard(R.id.cardAquecedor, "🔥", "Aquecedor", "Climatização");
            setupCard(R.id.cardCaldeira, "🚿", "Caldeira", "Climatização");
            setupCard(R.id.cardBombaCalor, "⚙️", "Bomba Calor", "Climatização");
            setupCard(R.id.cardDesumidificador, "💧", "Desumidif.", "Climatização");
            setupCard(R.id.cardLareira, "🪵", "Lareira", "Climatização");
            setupCard(R.id.cardPiso, "👣", "Piso Aque.", "Climatização");

            // ===== LAVAGENS =====
            setupCard(R.id.cardMaqLoica, "🍽️", "Máq. Loiça", "Lavagens");
            setupCard(R.id.cardMaqRoupa, "🧺", "Máq. Roupa", "Lavagens");
            setupCard(R.id.cardSecar, "🌀", "Máq. Secar", "Lavagens");

            // ===== ENTRETENIMENTO =====
            setupCard(R.id.cardTV, "📺", "Televisão", "Entretenimento");
            setupCard(R.id.cardConsola, "🎮", "Consola", "Entretenimento");
            setupCard(R.id.cardPC, "💻", "Computador", "Entretenimento");

            // ===== OUTROS =====
            setupCard(R.id.cardSolar, "☀️", "Paineis", "Outros");
            setupCard(R.id.cardPiscina, "🏊", "Piscina", "Outros");
            setupCard(R.id.cardRega, "⛲", "Bomba Rega", "Outros");
            setupCard(R.id.cardRouter, "🖲️", "Router", "Outros");

            // 3. Botão Concluir
            Button btnConcluir = findViewById(R.id.btnConcluir);
            btnConcluir.setOnClickListener(v -> {
                if (verificarSeTemEletros()) {
                    Toast.makeText(this, "Guardado! A avançar...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Eletrodomesticos.this, ClasseConsumo.class);
                    intent.putExtra("casa_id", casaId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Seleciona pelo menos um eletrodoméstico!", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ocorreu um erro ao iniciar.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupCard(int cardId, String emoji, String nome, String categoria) {
        try {
            MaterialCardView card = findViewById(cardId);
            if (card == null) return;

            TextView tvEmoji = card.findViewById(R.id.tvEmoji);
            TextView tvNome = card.findViewById(R.id.tvNome);
            TextView tvQtd = card.findViewById(R.id.tvQtd);
            ImageButton btnMinus = card.findViewById(R.id.btnMinus);
            ImageButton btnPlus = card.findViewById(R.id.btnPlus);

            tvEmoji.setText(emoji);
            tvNome.setText(nome);

            // Ler quantidade atual da BD
            int qtdAtual = dbHelper.contarEletrodomesticosEspecificos(casaId, nome);

            tvQtd.setText(String.valueOf(qtdAtual));
            atualizarVisualCard(card, qtdAtual);

            // Botão Menos (-)
            btnMinus.setOnClickListener(v -> {
                int q = dbHelper.contarEletrodomesticosEspecificos(casaId, nome);
                if (q > 0) {
                    dbHelper.removerUmEletrodomestico(casaId, nome);
                    int novaQ = q - 1;
                    tvQtd.setText(String.valueOf(novaQ));
                    atualizarVisualCard(card, novaQ);
                }
            });

            // Botão Mais (+)
            btnPlus.setOnClickListener(v -> {
                int q = dbHelper.contarEletrodomesticosEspecificos(casaId, nome);
                if (q < 20) {
                    dbHelper.adicionarUmEletrodomestico(casaId, nome, categoria);
                    int novaQ = q + 1;
                    tvQtd.setText(String.valueOf(novaQ));
                    atualizarVisualCard(card, novaQ);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Altera a cor do Card para Verde se a quantidade for > 0.
    private void atualizarVisualCard(MaterialCardView card, int qtd) {
        if (qtd > 0) {
            card.setStrokeColor(Color.parseColor("#4CAF50"));
            card.setStrokeWidth(6);
            card.setCardBackgroundColor(Color.parseColor("#F1F8E9"));
        } else {
            card.setStrokeColor(Color.parseColor("#E0E0E0"));
            card.setStrokeWidth(2);
            card.setCardBackgroundColor(Color.WHITE);
        }
    }

    private boolean verificarSeTemEletros() {
        Cursor c = null;
        try {
            c = dbHelper.obterEletrodomesticosDaCasa(casaId);
            if (c != null && c.getCount() > 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (c != null) c.close();
        }
    }
}