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

import androidx.annotation.Nullable;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.MensagemChatSync;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lista de clientes com quem o técnico tem conversa.
 * Lê as mensagens da BD local, mas antes faz sync com o servidor
 * para garantir que as conversas online aparecem também offline.
 */
public class ListaChatsTecnicoActivity extends BaseActivityTecnico {

    private ListView listClientes;
    private DBHelper db;
    private String emailTecnico;
    private List<String> listaEmailsClientes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_chats_tecnico);

        db = new DBHelper(this);

        // 1) Obter email do técnico das SharedPreferences
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailTecnico = sp.getString("user_email", null);

        // 2) Se vier null, tentar ir buscar um técnico da BD local (fallback)
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

        // Sync geral (inclui envio de mensagens locais para o servidor)
        SyncUtils.syncTudoAsync(getApplicationContext());

        // puxar mensagens do servidor para o SQLite deste técnico
        sincronizarMensagensDoServidor();

        // Carregar lista de clientes a partir da BD local
        carregarClientes();

        // Ao clicar num cliente, abre o chat com esse email
        listClientes.setOnItemClickListener((parent, view, position, id) -> {
            String emailCliente = listaEmailsClientes.get(position);
            Intent i = new Intent(ListaChatsTecnicoActivity.this, ChatTecnicoActivity.class);
            i.putExtra("cliente_email", emailCliente);
            startActivity(i);
        });

        // Bottom navigation do técnico
        setupBottomNavTecnico(R.id.menu_mensagens_tecnico);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Sempre que regressas, tenta sincronizar outra vez
        SyncUtils.syncTudoAsync(getApplicationContext());
        sincronizarMensagensDoServidor();
        carregarClientes();

        if (bottomNavTecnico != null) {
            bottomNavTecnico.setSelectedItemId(R.id.menu_mensagens_tecnico);
        }
    }

    /**
     * Faz pedido ao servidor para obter todas as mensagens
     * em que o técnico participa e grava-as na BD local,
     * evitando duplicados.
     */
    private void sincronizarMensagensDoServidor() {
        ApiService api = ApiClient.getRetrofit(this).create(ApiService.class);

        api.getMensagensChatByEmail(emailTecnico).enqueue(new Callback<List<MensagemChatSync>>() {
            @Override
            public void onResponse(Call<List<MensagemChatSync>> call,
                                   Response<List<MensagemChatSync>> response) {

                if (!response.isSuccessful() || response.body() == null) return;

                List<MensagemChatSync> lista = response.body();
                DBHelper dbLocal = new DBHelper(ListaChatsTecnicoActivity.this);

                for (MensagemChatSync m : lista) {

                    String remetente = m.remetenteEmail;
                    String destinatario = m.destinatarioEmail;
                    String texto = m.texto;
                    long ts = m.timestamp;

                    // já existe? -> ignorar
                    if (dbLocal.existeMensagemChatComTs(remetente, destinatario, ts)) {
                        continue;
                    }

                    // inserir no SQLite
                    dbLocal.inserirMensagemChatComTs(remetente, destinatario, texto, ts);
                }

                carregarClientes();
            }

            @Override
            public void onFailure(Call<List<MensagemChatSync>> call, Throwable t) {
                // fica só com o local
            }
        });
    }

    /**
     * Lê da BD local todos os clientes com quem este técnico
     * tem mensagens trocadas e preenche a ListView.
     */
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