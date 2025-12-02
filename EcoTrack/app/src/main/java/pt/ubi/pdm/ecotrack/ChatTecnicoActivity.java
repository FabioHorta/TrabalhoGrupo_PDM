package pt.ubi.pdm.ecotrack;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

// Também pode estender BaseActivityTecnico se quiseres ter bottom nav
public class ChatTecnicoActivity extends BaseActivityTecnico {

    private ListView listMensagens;
    private EditText etMensagem;
    private Button btnEnviar;

    private DBHelper db;
    private String emailTecnico;
    private String emailCliente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_tecnico);

        db = new DBHelper(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Técnico não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        emailTecnico = user.getEmail();

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

        setupBottomNavTecnico(R.id.menu_mensagens_tecnico);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarMensagens();
        if (bottomNavTecnico != null) {
            bottomNavTecnico.setSelectedItemId(R.id.menu_mensagens_tecnico);
        }
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
            carregarMensagens();
        } else {
            Toast.makeText(this, "Erro ao enviar mensagem.", Toast.LENGTH_SHORT).show();
        }
    }
}
