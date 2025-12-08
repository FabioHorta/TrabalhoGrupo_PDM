package pt.ubi.pdm.ecotrack;

import android.content.ActivityNotFoundException;
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

    private final ArrayList<String> relatoriosList = new ArrayList<>();
    private final List<RelatorioResponse> listaRelatorios = new ArrayList<>();

    private ApiService api;
    private String emailCliente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorios);

        // Verificar se o utilizador está logado lendo as SharedPreferences
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailCliente = sp.getString("user_email", null);

        if (emailCliente == null) {
            Toast.makeText(this, "Sessão expirada. Faz login novamente.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Inicializar o Retrofit
        api = ApiClient.getRetrofit(this).create(ApiService.class);

        listRelatorios = findViewById(R.id.listRelatorios);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, relatoriosList);
        listRelatorios.setAdapter(adapter);

        // Carregar a lista inicial
        carregarRelatoriosDoServidor();

        // Configurar o clique na lista
        listRelatorios.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= listaRelatorios.size()) return;

            RelatorioResponse r = listaRelatorios.get(position);
            obterEPedirAberturaRelatorio(r);
        });
    }

    // Obter lista de relatórios (apenas metadados: título, data, id)
    private void carregarRelatoriosDoServidor() {
        api.getRelatoriosByCliente(emailCliente).enqueue(new Callback<List<RelatorioResponse>>() {
            @Override
            public void onResponse(Call<List<RelatorioResponse>> call, Response<List<RelatorioResponse>> response) {
                if (isFinishing()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(Relatorios.this, "Erro ao carregar relatórios (" + response.code() + ")", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(Relatorios.this, "Ainda não tens relatórios disponíveis.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RelatorioResponse>> call, Throwable t) {
                if (isFinishing()) return;
                Toast.makeText(Relatorios.this, "Falha na ligação ao servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Obter o Base64 (PDF) de um relatório específico
    private void obterEPedirAberturaRelatorio(RelatorioResponse relatorio) {
        Toast.makeText(this, "A descarregar PDF...", Toast.LENGTH_SHORT).show();

        api.getRelatorioBase64(relatorio.id).enqueue(new Callback<RelatorioResponse>() {
            @Override
            public void onResponse(Call<RelatorioResponse> call, Response<RelatorioResponse> response) {
                if (isFinishing()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(Relatorios.this, "Erro ao obter o PDF.", Toast.LENGTH_LONG).show();
                    return;
                }

                String base64 = response.body().base64;
                if (base64 == null || base64.isEmpty()) {
                    Toast.makeText(Relatorios.this, "Este relatório não tem conteúdo PDF.", Toast.LENGTH_LONG).show();
                    return;
                }

                File pdfFile = guardarPdfEmCache(relatorio.id, base64);

                if (pdfFile != null) {
                    abrirPdf(pdfFile);
                } else {
                    Toast.makeText(Relatorios.this, "Erro ao guardar ficheiro no telemóvel.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RelatorioResponse> call, Throwable t) {
                if (isFinishing()) return;
                Toast.makeText(Relatorios.this, "Falha de rede ao descarregar PDF.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Converter Base64 para Ficheiro físico na Cache
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

    // Abrir o PDF usando uma App externa (FileProvider)
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

            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Não tens nenhuma app para abrir PDFs instalada.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao tentar abrir o PDF.", Toast.LENGTH_LONG).show();
        }
    }
}