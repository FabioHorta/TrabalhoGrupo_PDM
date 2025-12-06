package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ChatClienteActivity extends AppCompatActivity {

    private ListView listMensagens;
    private EditText etMensagem;
    private Button btnEnviar;

    private DBHelper db;
    private String emailCliente;

    private final android.os.Handler handler = new android.os.Handler();
    private Runnable autoRefreshTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_cliente);

        db = new DBHelper(this);

        // Obter email do utilizador autenticado das SharedPreferences
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        emailCliente = prefs.getString("user_email", null);

        if (emailCliente == null) {
            Toast.makeText(this, "Sessão expirada. Por favor faz login.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        listMensagens = findViewById(R.id.listMensagensCliente);
        etMensagem = findViewById(R.id.etMensagemCliente);
        btnEnviar = findViewById(R.id.btnEnviarCliente);

        carregarMensagens();

        btnEnviar.setOnClickListener(v -> enviarMensagem());
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
        Cursor c = db.listarMensagensDoUtilizador(emailCliente);
        List<String> linhas = new ArrayList<>();

        if (c != null) {
            int idxRem = c.getColumnIndexOrThrow(DBHelper.C_MSG_REMETENTE);
            int idxTexto = c.getColumnIndexOrThrow(DBHelper.C_MSG_TEXTO);

            while (c.moveToNext()) {
                String rem = c.getString(idxRem);
                String txt = c.getString(idxTexto);

                if (rem.equals(emailCliente)) {
                    linhas.add("Eu: " + txt);
                } else {
                    linhas.add("Técnico: " + txt);
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

        if (!linhas.isEmpty()) {
            listMensagens.setSelection(linhas.size() - 1);
        }
    }

    private void enviarMensagem() {
        String texto = etMensagem.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escreve uma mensagem.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Buscar todos os técnicos
        Cursor cTec = db.listarTecnicos();
        if (cTec == null || !cTec.moveToFirst()) {
            Toast.makeText(this, "Não existem técnicos registados.", Toast.LENGTH_SHORT).show();
            if (cTec != null) cTec.close();
            return;
        }

        int idxEmail = cTec.getColumnIndexOrThrow(DBHelper.C_USER_EMAIL);
        boolean algumInserido = false;

        // Enviar para todos os técnicos (simulação de ticket de suporte)
        do {
            String emailTec = cTec.getString(idxEmail);
            boolean ok = db.inserirMensagemChat(emailCliente, emailTec, texto);
            if (ok) algumInserido = true;
        } while (cTec.moveToNext());
        cTec.close();

        if (algumInserido) {
            etMensagem.setText("");

            // sincroniza imediatamente após enviar
            SyncUtils.syncChatCompleto(this);

            // volta a carregar da BD local (já com possíveis mensagens novas do técnico)
            carregarMensagens();
        } else {
            Toast.makeText(this, "Erro ao enviar mensagem.", Toast.LENGTH_SHORT).show();
        }
    }

}
