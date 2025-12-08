package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;


//Activity que mostra uma lista de todas as casas associadas ao user autent.
// Permite clicar numa casa para editar ou criar uma nova.
public class ListarCasas extends AppCompatActivity {

    ListView listCasas;
    Button btnAdicionar, btnVoltar;
    DBHelper dbHelper;
    String userEmail;
    ArrayList<String> nomesCasas = new ArrayList<>();
    ArrayList<Integer> idsCasas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_casas);

        dbHelper = new DBHelper(this);

        userEmail = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("user_email", null);

        if (userEmail == null) {
            Toast.makeText(this, "Sessão expirada. Faz login novamente.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        listCasas = findViewById(R.id.listCasas);
        btnAdicionar = findViewById(R.id.btnAdicionarCasa);
        btnVoltar = findViewById(R.id.btnVoltar);

        carregarLista();

        // Botão Adicionar: Abre CaracterizacaoCasa com ID = -1
        btnAdicionar.setOnClickListener(v -> {
            Intent i = new Intent(ListarCasas.this, CaracterizacaoCasa.class);
            i.putExtra("casa_id", -1);
            startActivity(i);
        });

        btnVoltar.setOnClickListener(v -> finish());

        // Clique na Lista: Abre CaracterizacaoCasa com ID da casa clicada
        listCasas.setOnItemClickListener((parent, view, position, id) -> {
            int idCasaSelecionada = idsCasas.get(position);
            Intent i = new Intent(ListarCasas.this, CaracterizacaoCasa.class);
            i.putExtra("casa_id", idCasaSelecionada);
            startActivity(i);
        });
    }

    private void carregarLista() {
        nomesCasas.clear();
        idsCasas.clear();

        if (userEmail == null) {
            return;
        }

        Cursor c = dbHelper.listarCasasDoUtilizador(userEmail);
        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(DBHelper.C_CASA_ID));
                String nome = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_NOME));
                String tipo = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_TIPO));
                idsCasas.add(id);
                nomesCasas.add(nome + " (" + tipo + ")");
            }
            c.close();
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomesCasas);
        listCasas.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarLista();
    }
}