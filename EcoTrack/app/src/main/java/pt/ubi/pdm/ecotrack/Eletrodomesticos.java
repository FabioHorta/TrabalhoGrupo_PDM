package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

        Log.d("ELETRO", "=== onCreate INICIADO ===");

        try {
            // ✅ RECEBER casa_id DO INTENT
            casaId = getIntent().getIntExtra("casa_id", -1);
            Log.d("ELETRO", "casaId recebido: " + casaId);

            if (casaId == -1) {
                Toast.makeText(this, "Erro: Casa não encontrada!", Toast.LENGTH_SHORT).show();
                Log.e("ELETRO", "casaId = -1");
                finish();
                return;
            }

            dbHelper = new DBHelper(this);
            Log.d("ELETRO", "DBHelper criado com sucesso");

            // ✅ CARREGAR DADOS EXISTENTES

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

            Log.d("ELETRO", "✅ Todos os cards setup completo");

            // ✅ BOTÃO CONCLUIR - VAI PARA ClasseConsumo
            Button btnConcluir = findViewById(R.id.btnConcluir);
            btnConcluir.setOnClickListener(v -> {
                Log.d("ELETRO", "Botão CONCLUIR clicado");

                // ✅ VERIFICA SE TEM ELETROS NA BD
                if (verificarSeTemEletros()) {
                    Log.d("ELETRO", "✅ Tem eletros, a ir para ClasseConsumo");
                    Toast.makeText(this, "Guardado! Seguindo para classificação...", Toast.LENGTH_SHORT).show();

                    // ✅ CRIA INTENT PARA ClasseConsumo
                    Intent intent = new Intent(Eletrodomesticos.this, ClasseConsumo.class);
                    intent.putExtra("casa_id", casaId);
                    startActivity(intent);
                    // NÃO faz finish() aqui
                } else {
                    Log.e("ELETRO", "❌ Nenhum eletro selecionado");
                    Toast.makeText(this, "Seleciona pelo menos um eletrodoméstico!", Toast.LENGTH_SHORT).show();
                }
            });

            Log.d("ELETRO", "=== onCreate COMPLETO ===");

        } catch (Exception e) {
            Log.e("ELETRO", "ERRO em onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

            // 1. LER CONTAGEM REAL DA NOVA TABELA
            // Este método conta quantas linhas existem com este nome para esta casa
            int qtdAtual = dbHelper.contarEletrodomesticosEspecificos(casaId, nome);

            tvQtd.setText(String.valueOf(qtdAtual));
            atualizarVisualCard(card, qtdAtual);

            // 2. BOTÃO MENOS (-)
            btnMinus.setOnClickListener(v -> {
                // Verifica a contagem em tempo real
                int q = dbHelper.contarEletrodomesticosEspecificos(casaId, nome);
                if (q > 0) {
                    // Remove a última linha inserida deste tipo
                    dbHelper.removerUmEletrodomestico(casaId, nome);

                    // Atualiza visual
                    int novaQ = q - 1;
                    tvQtd.setText(String.valueOf(novaQ));
                    atualizarVisualCard(card, novaQ);
                }
            });

            // 3. BOTÃO MAIS (+)
            btnPlus.setOnClickListener(v -> {
                int q = dbHelper.contarEletrodomesticosEspecificos(casaId, nome);
                if (q < 20) { // Limite de segurança
                    // Insere uma NOVA LINHA na tabela
                    dbHelper.adicionarUmEletrodomestico(casaId, nome, categoria);

                    // Atualiza visual
                    int novaQ = q + 1;
                    tvQtd.setText(String.valueOf(novaQ));
                    atualizarVisualCard(card, novaQ);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ VERIFICA SE REALMENTE GUARDOU NA BD
     */
    private void verificarSeGuardou(String nome, int qtdEsperada) {
        Log.d("ELETRO", "A verificar se guardou " + nome + "...");

        try {
            Cursor c = dbHelper.obterEletrodomesticosDaCasa(casaId);

            if (c == null) {
                Log.e("ELETRO", "❌ Cursor é null ao verificar!");
                return;
            }

            Log.d("ELETRO", "Cursor tem " + c.getCount() + " linhas");

            while (c.moveToNext()) {
                try {
                    String nomeDb = c.getString(c.getColumnIndexOrThrow(DBHelper.C_APP_NOME));
                    int qtdDb = c.getInt(c.getColumnIndexOrThrow(DBHelper.C_APP_QUANTIDADE));

                    Log.d("ELETRO", "  BD: " + nomeDb + " = " + qtdDb);

                    if (nomeDb.equals(nome)) {
                        if (qtdDb == qtdEsperada) {
                            Log.d("ELETRO", "✅✅ CONFIRMADO: " + nome + " = " + qtdDb);
                        } else {
                            Log.e("ELETRO", "❌ MISMATCH: esperava " + qtdEsperada + " mas tem " + qtdDb);
                        }
                    }
                } catch (Exception e) {
                    Log.e("ELETRO", "Erro ao ler: " + e.getMessage());
                }
            }
            c.close();

        } catch (Exception e) {
            Log.e("ELETRO", "ERRO em verificarSeGuardou: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Altera a cor do cartão (verde se tiver itens, branco se não tiver)
     */
    private void atualizarVisualCard(MaterialCardView card, int qtd) {
        if (qtd > 0) {
            // Fica verde clarinho com borda verde
            card.setStrokeColor(Color.parseColor("#4CAF50"));
            card.setStrokeWidth(6);
            card.setCardBackgroundColor(Color.parseColor("#F1F8E9"));
        } else {
            // Fica branco com borda cinzenta
            card.setStrokeColor(Color.parseColor("#E0E0E0"));
            card.setStrokeWidth(2);
            card.setCardBackgroundColor(Color.WHITE);
        }
    }

    /**
     * Verifica se existe pelo menos 1 eletrodoméstico na BD para esta casa
     */
    private boolean verificarSeTemEletros() {
        Cursor c = null;
        try {
            // Obtém todos os eletros desta casa
            c = dbHelper.obterEletrodomesticosDaCasa(casaId);

            // Se o cursor não for nulo e tiver linhas, retorna true
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