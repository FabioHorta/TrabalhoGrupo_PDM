package pt.ubi.pdm.ecotrack;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.Nullable;
import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.RelatorioCreateRequest;
import pt.ubi.pdm.ecotrack.models.RelatorioResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ecrã de criação de relatórios por parte do técnico.
 * - Lista assistências do técnico (a partir da BD local).
 * - Permite associar opcionalmente um relatório a uma assistência.
 * - Envia o relatório para o servidor via /relatorios/create.
 */
public class RelatoriosTecnicoActivity extends BaseActivityTecnico {

    private DBHelper db;
    private ApiService api;

    private String emailTecnico;

    private Spinner spinnerAssistencias;
    private TextInputEditText etClienteEmail, etTitulo, etResumo, etDetalhes;
    private View btnEnviar;
    private ProgressBar progressBar;

    /**
     * Para cada posição do spinner, guarda o ID da assistência NO SERVIDOR
     * (server_id na tabela local). Posição 0 = null (sem assistência associada).
     */
    private final List<Long> listaIdsAssistencias = new ArrayList<>();

    // =========================================================
    // CICLO DE VIDA
    // =========================================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorios_tecnico);

        db = new DBHelper(this);
        api = ApiClient.getRetrofit().create(ApiService.class);

        // 1) Obter email do técnico guardado em SharedPreferences
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailTecnico = sp.getString("user_email", null);

        if (emailTecnico == null) {
            Toast.makeText(this, "Técnico não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2) Confirmar que o utilizador é mesmo técnico
        String tipo = db.obterTipoUtilizadorPorEmail(emailTecnico);
        if (!"tecnico".equalsIgnoreCase(tipo)) {
            Toast.makeText(this, "Esta área é apenas para técnicos.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ligarViews();
        carregarAssistenciasDoTecnico();

        btnEnviar.setOnClickListener(v -> enviarRelatorio());

        // Marcar aba correta na bottom navigation
        setupBottomNavTecnico(R.id.menu_relatorios_tecnico);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavTecnico != null) {
            bottomNavTecnico.setSelectedItemId(R.id.menu_relatorios_tecnico);
        }
    }

    // =========================================================
    // INICIALIZAÇÃO DE VIEWS
    // =========================================================
    private void ligarViews() {
        spinnerAssistencias = findViewById(R.id.spinnerAssistencias);
        etClienteEmail = findViewById(R.id.etClienteEmail);
        etTitulo = findViewById(R.id.etTituloRelatorio);
        etResumo = findViewById(R.id.etResumoRelatorio);
        etDetalhes = findViewById(R.id.etDetalhesRelatorio);
        btnEnviar = findViewById(R.id.btnGerarEnviarRelatorio);
        progressBar = findViewById(R.id.progressRelatorio);
    }

    // =========================================================
    // CARREGAR ASSISTÊNCIAS DO TÉCNICO
    // =========================================================

    /**
     * Lê as assistências do técnico da BD local.
     * Usa SEMPRE o server_id (ID no servidor) para enviar mais tarde
     * no corpo do pedido do relatório.
     * Se uma assistência ainda não tiver server_id, fica marcada
     * como "não sincronizada" e o valor guardado será null.
     */
    private void carregarAssistenciasDoTecnico() {
        listaIdsAssistencias.clear();
        List<String> descricoes = new ArrayList<>();

        // Posição 0 do spinner = opção "sem assistência associada"
        descricoes.add("Sem assistência associada");
        listaIdsAssistencias.add(null);

        Cursor c = db.listarAssistenciasDoTecnico(emailTecnico);
        if (c != null) {
            int idxIdLocal  = c.getColumnIndexOrThrow("id");
            int idxData     = c.getColumnIndexOrThrow("data");
            int idxHora     = c.getColumnIndexOrThrow("hora");
            int idxDesc     = c.getColumnIndexOrThrow("descricao");
            int idxServerId = c.getColumnIndexOrThrow("server_id");

            while (c.moveToNext()) {
                long idLocal = c.getLong(idxIdLocal);
                String data  = c.getString(idxData);
                String hora  = c.getString(idxHora);
                String desc  = c.getString(idxDesc);

                Long serverId = null;
                if (!c.isNull(idxServerId)) {
                    serverId = c.getLong(idxServerId);
                }

                // Texto apresentado no spinner
                String linha;
                if (serverId != null) {
                    linha = "#" + serverId + " - " + data + " " + hora + " • " + desc;
                } else {
                    linha = "#" + idLocal + " (não sincronizada) - " + data + " " + hora + " • " + desc;
                }

                descricoes.add(linha);

                // Guardamos o ID do servidor (pode ser null se ainda não sincronizou)
                listaIdsAssistencias.add(serverId);
            }
            c.close();
        }

        ArrayAdapter<String> adp = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                descricoes
        );
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssistencias.setAdapter(adp);
    }

    // =========================================================
    // ENVIO DE RELATÓRIO
    // =========================================================

    /**
     * Valida os campos, constrói o pedido e chama a API /relatorios/create.
     * Se uma assistência estiver selecionada e tiver server_id, esse ID
     * é enviado em assistencia_id. Caso contrário, vai null (relatório sem
     * assistência associada).
     */
    private void enviarRelatorio() {
        String clienteEmail = textOf(etClienteEmail);
        String titulo = textOf(etTitulo);
        String resumo = textOf(etResumo);
        String detalhes = textOf(etDetalhes);

        // Validação simples dos campos obrigatórios
        if (clienteEmail.isEmpty()) {
            etClienteEmail.setError("Indica o email do cliente");
            return;
        }
        if (titulo.isEmpty()) {
            etTitulo.setError("Indica um título");
            return;
        }

        // Assistência associada (opcional)
        int pos = spinnerAssistencias.getSelectedItemPosition();
        Long assistenciaId = null;
        if (pos >= 0 && pos < listaIdsAssistencias.size()) {
            assistenciaId = listaIdsAssistencias.get(pos);
        }

        // Construir body do pedido
        RelatorioCreateRequest body = new RelatorioCreateRequest(
                assistenciaId,               // pode ser null
                emailTecnico,                // técnico autenticado
                clienteEmail,
                titulo,
                resumo.isEmpty() ? null : resumo,
                detalhes.isEmpty() ? null : detalhes
        );

        setLoading(true);

        api.criarRelatorio(body).enqueue(new Callback<RelatorioResponse>() {
            @Override
            public void onResponse(Call<RelatorioResponse> call, Response<RelatorioResponse> response) {
                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(RelatoriosTecnicoActivity.this,
                            "Erro ao enviar relatório (" + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                RelatorioResponse r = response.body();
                Toast.makeText(RelatoriosTecnicoActivity.this,
                        "Relatório enviado (ID: " + r.id + ")",
                        Toast.LENGTH_SHORT).show();

                // Limpar campos de texto (mantém email do cliente e assistência selecionada)
                etTitulo.setText("");
                etResumo.setText("");
                etDetalhes.setText("");
            }

            @Override
            public void onFailure(Call<RelatorioResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RelatoriosTecnicoActivity.this,
                        "Falha na ligação ao servidor.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // =========================================================
    // UTILITÁRIOS
    // =========================================================

    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            btnEnviar.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnEnviar.setEnabled(true);
        }
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
