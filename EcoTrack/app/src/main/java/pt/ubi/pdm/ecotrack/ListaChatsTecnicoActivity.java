package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

// Se tiveres BaseActivityTecnico, podes estender essa em vez de AppCompatActivity
public class ListaChatsTecnicoActivity extends BaseActivityTecnico {

    private ListView listClientes;
    private DBHelper db;
    private String emailTecnico;
    private List<String> listaEmailsClientes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_chats_tecnico);

        db = new DBHelper(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Técnico não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        emailTecnico = user.getEmail();

        listClientes = findViewById(R.id.listClientesChat);

        carregarClientes();

        listClientes.setOnItemClickListener((parent, view, position, id) -> {
            String emailCliente = listaEmailsClientes.get(position);
            Intent i = new Intent(ListaChatsTecnicoActivity.this, ChatTecnicoActivity.class);
            i.putExtra("cliente_email", emailCliente);
            startActivity(i);
        });

        // se tiveres bottom nav:
        setupBottomNavTecnico(R.id.menu_mensagens_tecnico); // ou o id que tiveres
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarClientes();
        if (bottomNavTecnico != null) {
            bottomNavTecnico.setSelectedItemId(R.id.menu_mensagens_tecnico);
        }
    }

    private void carregarClientes() {
        Cursor c = db.listarClientesDoTecnicoNoChat(emailTecnico);
        listaEmailsClientes = new ArrayList<>();
        if (c != null) {
            int idx = c.getColumnIndexOrThrow("cliente_email");
            while (c.moveToNext()) {
                String mail = c.getString(idx);
                listaEmailsClientes.add(mail);
            }
            c.close();
        }

        ArrayAdapter<String> adp = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                listaEmailsClientes
        );
        listClientes.setAdapter(adp);
    }
}
