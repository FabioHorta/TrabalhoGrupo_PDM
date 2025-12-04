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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_cliente);

        db = new DBHelper(this);

        // --- MUDANÇA: USAR MEMÓRIA LOCAL (SharedPreferences) EM VEZ DE FIREBASE ---
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        emailCliente = prefs.getString("user_email", null);

        if (emailCliente == null) {
            Toast.makeText(this, "Sessão expirada. Por favor faz login.", Toast.LENGTH_SHORT).show();
            // Redireciona para o Login se não houver utilizador guardado
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        // --------------------------------------------------------------------------

        listMensagens = findViewById(R.id.listMensagensCliente);
        etMensagem = findViewById(R.id.etMensagemCliente);
        btnEnviar = findViewById(R.id.btnEnviarCliente);

        carregarMensagens();

        btnEnviar.setOnClickListener(v -> enviarMensagem());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarMensagens();
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

        // Rolar para o fim da lista se houver mensagens
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
            carregarMensagens();
        } else {
            Toast.makeText(this, "Erro ao enviar mensagem.", Toast.LENGTH_SHORT).show();
        }
    }
}