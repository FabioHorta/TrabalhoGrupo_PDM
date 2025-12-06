package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

// Lista de clientes com quem o técnico tem conversa
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

        // 1) Obter email do técnico das SharedPreferences
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailTecnico = sp.getString("user_email", null);

        // 2) Se vier null, tentar ir buscar um técnico da BD local
        if (emailTecnico == null) {
            Cursor cTec = db.listarTecnicos();  // SELECT email FROM tecnicos
            if (cTec != null && cTec.moveToFirst()) {
                int idxEmail = cTec.getColumnIndexOrThrow(DBHelper.C_USER_EMAIL);
                emailTecnico = cTec.getString(idxEmail);
            }
            if (cTec != null) cTec.close();
        }

        // 3) Se mesmo assim não tivermos técnico, não faz sentido estar nesta Activity
        if (emailTecnico == null) {
            Toast.makeText(this, "Técnico não autenticado ou não configurado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // (opcional) confirmar que este email é mesmo técnico na tabela users
        String tipo = db.obterTipoUtilizadorPorEmail(emailTecnico);
        if (!"tecnico".equalsIgnoreCase(tipo)) {
            Toast.makeText(this, "Esta área é apenas para técnicos.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listClientes = findViewById(R.id.listClientesChat);

        // Tenta sincronizar tudo (inclui mensagens de chat) logo ao abrir
        SyncUtils.syncTudoAsync(getApplicationContext());

        carregarClientes();

        listClientes.setOnItemClickListener((parent, view, position, id) -> {
            String emailCliente = listaEmailsClientes.get(position);
            Intent i = new Intent(ListaChatsTecnicoActivity.this, ChatTecnicoActivity.class);
            i.putExtra("cliente_email", emailCliente);
            startActivity(i);
        });

        // bottom nav do técnico
        setupBottomNavTecnico(R.id.menu_mensagens_tecnico);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Volta a sincronizar sempre que regressas a esta activity
        SyncUtils.syncTudoAsync(getApplicationContext());

        carregarClientes();

        if (bottomNavTecnico != null) {
            bottomNavTecnico.setSelectedItemId(R.id.menu_mensagens_tecnico);
        }
    }

    private void carregarClientes() {
        listaEmailsClientes = new ArrayList<>();

        Cursor c = db.listarClientesDoTecnicoNoChat(emailTecnico);
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
