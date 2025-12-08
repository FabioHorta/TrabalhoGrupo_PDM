package pt.ubi.pdm.ecotrack;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EnviarMensagem extends AppCompatActivity {

    EditText etAssunto, etMensagem;
    Button btnEnviar;
    ListView listMensagens;

    DBHelper db;

    ArrayList<String> mensagensList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enviar_mensagem);

        db = new DBHelper(this);
        etAssunto = findViewById(R.id.etAssunto);
        etMensagem = findViewById(R.id.etMensagem);
        btnEnviar = findViewById(R.id.btnEnviar);
        listMensagens = findViewById(R.id.listMensagens);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mensagensList);
        listMensagens.setAdapter(adapter);

        carregarMensagens();

        btnEnviar.setOnClickListener(v -> {
            // Obtém o texto escrito nos campos
            String assunto = etAssunto.getText().toString();
            String mensagem = etMensagem.getText().toString();

            // Gera a data e hora atual formatada
            String data = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

            // Validação: verifica se algum campo está vazio
            if (assunto.isEmpty() || mensagem.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
                return; // Para a execução aqui se estiver vazio
            }

            // Tenta inserir na base de dados
            if (db.inserirMensagem(assunto, mensagem, data)) {
                // Se inserir com sucesso: mostra aviso, limpa os campos e atualiza a lista
                Toast.makeText(this, "Mensagem enviada!", Toast.LENGTH_SHORT).show();
                etAssunto.setText("");
                etMensagem.setText("");
                carregarMensagens();
            }
        });
    }

    void carregarMensagens() {
        // Limpa a lista atual para não duplicar dados
        mensagensList.clear();

        // Pede ao DBHelper todas as mensagens (retorna um Cursor)
        Cursor c = db.listarMensagens();

        // Loop: enquanto houver uma próxima linha na base de dados
        while (c.moveToNext()) {
            // Monta uma string com: Data • Assunto (quebra de linha) Mensagem
            // Nota: assume-se que os índices das colunas são 3, 1 e 2
            String linha = c.getString(3) + " • " + c.getString(1) + "\n" + c.getString(2);

            mensagensList.add(linha);
        }

        // Avisa o adaptador que os dados mudaram para ele atualizar o ecrã
        adapter.notifyDataSetChanged();
    }
}