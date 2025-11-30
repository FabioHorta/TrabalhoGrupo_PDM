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
            String assunto = etAssunto.getText().toString();
            String mensagem = etMensagem.getText().toString();
            String data = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

            if (assunto.isEmpty() || mensagem.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (db.inserirMensagem(assunto, mensagem, data)) {
                Toast.makeText(this, "Mensagem enviada!", Toast.LENGTH_SHORT).show();
                etAssunto.setText("");
                etMensagem.setText("");
                carregarMensagens();
            }
        });
    }

    void carregarMensagens() {
        mensagensList.clear();
        Cursor c = db.listarMensagens();

        while (c.moveToNext()) {
            String linha = c.getString(3) + " • " + c.getString(1) + "\n" + c.getString(2);
            mensagensList.add(linha);
        }
        adapter.notifyDataSetChanged();
    }
}
