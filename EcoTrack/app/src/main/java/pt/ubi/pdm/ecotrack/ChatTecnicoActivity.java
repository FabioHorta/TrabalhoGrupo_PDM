package pt.ubi.pdm.ecotrack;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

// Activity de chat do técnico com um cliente
public class ChatTecnicoActivity extends BaseActivityTecnico {

    private ListView listMensagens;
    private EditText etMensagem;
    private Button btnEnviar;

    private DBHelper db;
    private String emailTecnico;
    private String emailCliente;
    private final android.os.Handler handler = new android.os.Handler();
    private Runnable autoRefreshTask;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_tecnico);

        db = new DBHelper(this);

        // 1) Tentar obter email do técnico das SharedPreferences
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailTecnico = sp.getString("user_email", null);

        // 2) Se vier null, tentar buscar o primeiro técnico da BD local
        if (emailTecnico == null) {
            Cursor cTec = db.listarTecnicos();   // esta agora lê da tabela 'tecnicos'
            if (cTec != null && cTec.moveToFirst()) {
                // em listarTecnicos() fizemos SELECT C_TEC_EMAIL AS C_USER_EMAIL
                int idxEmail = cTec.getColumnIndexOrThrow(DBHelper.C_USER_EMAIL);
                emailTecnico = cTec.getString(idxEmail);
            }
            if (cTec != null) cTec.close();
        }

        // 3) Se mesmo assim não tivermos email, não dá para continuar
        if (emailTecnico == null) {
            Toast.makeText(this, "Técnico não autenticado ou não configurado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // (opcional) confirmar que este email está marcado como técnico na tabela users
        String tipo = db.obterTipoUtilizadorPorEmail(emailTecnico);
        if (!"tecnico".equalsIgnoreCase(tipo)) {
            Toast.makeText(this, "Esta área é apenas para técnicos.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 4) Email do cliente vem no Intent
        emailCliente = getIntent().getStringExtra("cliente_email");
        if (emailCliente == null) {
            Toast.makeText(this, "Cliente inválido.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listMensagens = findViewById(R.id.listMensagensTecnico);
        etMensagem = findViewById(R.id.etMensagemTecnico);
        btnEnviar = findViewById(R.id.btnEnviarTecnico);

        carregarMensagens();

        btnEnviar.setOnClickListener(v -> enviarMensagem());

        // bottom nav do técnico
        setupBottomNavTecnico(R.id.menu_mensagens_tecnico);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // sincroniza imediatamente ao entrar no ecrã
        SyncUtils.syncChatCompleto(this);
        carregarMensagens();

        autoRefreshTask = () -> {
            SyncUtils.syncChatCompleto(this);
            carregarMensagens();
            handler.postDelayed(autoRefreshTask, 2000); // repetir a cada 2 segundos
        };
        handler.postDelayed(autoRefreshTask, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(autoRefreshTask);
    }

    private void carregarMensagens() {
        Cursor c = db.listarMensagensEntre(emailCliente, emailTecnico);
        List<String> linhas = new ArrayList<>();

        if (c != null) {
            int idxRem = c.getColumnIndexOrThrow(DBHelper.C_MSG_REMETENTE);
            int idxTexto = c.getColumnIndexOrThrow(DBHelper.C_MSG_TEXTO);

            while (c.moveToNext()) {
                String rem = c.getString(idxRem);
                String txt = c.getString(idxTexto);

                if (rem.equals(emailTecnico)) {
                    linhas.add("Eu: " + txt);
                } else {
                    linhas.add("Cliente: " + txt);
                }
            }
            c.close();
        }

        ArrayAdapter<String> adp = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                linhas
        );
        listMensagens.setAdapter(adp);
        listMensagens.setSelection(linhas.size() - 1);
    }

    private void enviarMensagem() {
        String texto = etMensagem.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escreve uma mensagem.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean ok = db.inserirMensagemChat(emailTecnico, emailCliente, texto);
        if (ok) {
            etMensagem.setText("");

            // sincroniza imediatamente depois de enviar
            SyncUtils.syncChatCompleto(this);

            // recarrega mensagens da BD local
            carregarMensagens();
        } else {
            Toast.makeText(this, "Erro ao enviar mensagem.", Toast.LENGTH_SHORT).show();
        }
    }

}
