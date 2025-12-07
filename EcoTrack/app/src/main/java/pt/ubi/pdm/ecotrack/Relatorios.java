package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.RelatorioResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Relatorios extends AppCompatActivity {

    private ListView listRelatorios;
    private ArrayAdapter<String> adapter;

    // Lista de strings para mostrar na ListView
    private final ArrayList<String> relatoriosList = new ArrayList<>();
    // Lista com os objetos completos (id, titulo, etc.)
    private final List<RelatorioResponse> listaRelatorios = new ArrayList<>();

    private ApiService api;
    private String emailCliente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorios);

        // 1) Ler email do cliente da sessão local
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailCliente = sp.getString("user_email", null);

        if (emailCliente == null) {
            Toast.makeText(this, "Sessão expirada. Faz login novamente.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        api = ApiClient.getRetrofit(this).create(ApiService.class);


        listRelatorios = findViewById(R.id.listRelatorios);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, relatoriosList);
        listRelatorios.setAdapter(adapter);

        // Carregar lista de relatórios do servidor
        carregarRelatoriosDoServidor();

        // Ao clicar num relatório → buscar PDF em Base64 e abrir
        listRelatorios.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= listaRelatorios.size()) return;
            RelatorioResponse r = listaRelatorios.get(position);
            obterEPedirAberturaRelatorio(r);
        });
    }

    // ------------------------------------------------------------------------
    // 1) Obter lista de relatórios do cliente (online)
    // ------------------------------------------------------------------------
    private void carregarRelatoriosDoServidor() {
        api.getRelatoriosByCliente(emailCliente).enqueue(new Callback<List<RelatorioResponse>>() {
            @Override
            public void onResponse(Call<List<RelatorioResponse>> call,
                                   Response<List<RelatorioResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(Relatorios.this,
                            "Erro ao carregar relatórios (" + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                listaRelatorios.clear();
                listaRelatorios.addAll(response.body());

                relatoriosList.clear();
                for (RelatorioResponse r : listaRelatorios) {
                    String linha =
                            "Data: " + (r.created_at != null ? r.created_at : "-") +
                                    "\nTítulo: " + (r.titulo != null ? r.titulo : "(sem título)") +
                                    "\nTécnico: " + (r.tecnico_email != null ? r.tecnico_email : "-");
                    relatoriosList.add(linha);
                }

                adapter.notifyDataSetChanged();

                if (listaRelatorios.isEmpty()) {
                    Toast.makeText(Relatorios.this,
                            "Ainda não tens relatórios disponíveis.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RelatorioResponse>> call, Throwable t) {
                Toast.makeText(Relatorios.this,
                        "Falha na ligação ao servidor ao carregar relatórios.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ------------------------------------------------------------------------
    // 2) Obter Base64 do relatório e abrir como PDF
    // ------------------------------------------------------------------------
    private void obterEPedirAberturaRelatorio(RelatorioResponse relatorio) {
        // Se o endpoint /relatorios/by-cliente já devolver o base64, podes usar diretamente.
        // Aqui assumo que /by-cliente NÃO traz base64, e que temos um endpoint próprio.
        api.getRelatorioBase64(relatorio.id).enqueue(new Callback<RelatorioResponse>() {
            @Override
            public void onResponse(Call<RelatorioResponse> call, Response<RelatorioResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(Relatorios.this,
                            "Erro ao obter o PDF (" + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String base64 = response.body().base64;
                if (base64 == null || base64.isEmpty()) {
                    Toast.makeText(Relatorios.this,
                            "Relatório sem conteúdo PDF.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                File pdfFile = guardarPdfEmCache(relatorio.id, base64);
                if (pdfFile != null) {
                    abrirPdf(pdfFile);
                } else {
                    Toast.makeText(Relatorios.this,
                            "Erro ao guardar ficheiro PDF.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RelatorioResponse> call, Throwable t) {
                Toast.makeText(Relatorios.this,
                        "Falha na ligação ao servidor ao obter o PDF.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ------------------------------------------------------------------------
    // 3) Guardar PDF em cache interna (Base64 → ficheiro)
    // ------------------------------------------------------------------------
    private File guardarPdfEmCache(long relatorioId, String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            File pdfFile = new File(getCacheDir(), "relatorio_" + relatorioId + ".pdf");

            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                fos.write(bytes);
            }

            return pdfFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // 4) Abrir PDF com FileProvider
    // ------------------------------------------------------------------------
    private void abrirPdf(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Se não houver app de PDF, pode falhar
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this,
                        "Não foi encontrada nenhuma aplicação para abrir PDF.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Erro ao abrir o PDF.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
