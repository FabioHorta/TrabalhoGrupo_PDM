package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.UpdatePrecoRequest;
import pt.ubi.pdm.ecotrack.models.UserResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ecrã de perfil do utilizador:
 *  - mostra nome, email, consumo e custo estimado
 *  - permite alterar o preço por kWh (localmente + servidor)
 *  - permite gerir casas, alterar foto de perfil, logout, etc.
 */
public class PerfilUtilizador extends AppCompatActivity {

    // Views
    private TextView tvNome, tvEmail, tvConsumoStat, tvPoupancaStat;
    private ImageView imgPerfil;
    private com.google.android.material.card.MaterialCardView btnEditarFoto;
    private EditText etPrecoKwh;
    private Button btnGuardarPreco, btnGerirCasas, btnLogout, btnEliminar, btnVoltar;

    // Dados / helpers
    private DBHelper dbHelper;
    private ApiService api;              // ligação à API (Retrofit)
    private String userEmailAtual = "";
    private double precoKwhAtual;
    private int casaIdAtual;

    /**
     * Launcher para escolher imagem da galeria e atualizar foto de perfil.
     */
    private final ActivityResultLauncher<String> escolherImagemLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = reduzirImagem(uri);
                        imgPerfil.setImageBitmap(bitmap);
                        guardarFotoPerfil(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    // =========================================================
    // CICLO DE VIDA
    // =========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_utilizador);

        dbHelper = new DBHelper(this);
        api = ApiClient.getRetrofit().create(ApiService.class);

        // 1) Recuperar email da sessão (guardado no login)
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userEmailAtual = prefs.getString("user_email", null);

        // Sem email → sessão inválida → voltar ao login
        if (userEmailAtual == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 2) Casa actualmente seleccionada (para cálculo de consumo)
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();

        // 3) Inicializar UI e dados
        initViews();
        carregarDadosDaBD();
        carregarFotoExistente();
        calcularConsumoECusto();
        configurarBotoes();
    }

    // =========================================================
    // INICIALIZAÇÃO DE VIEWS
    // =========================================================
    private void initViews() {
        tvNome = findViewById(R.id.tvNomeCompleto);
        tvEmail = findViewById(R.id.tvEmail);
        tvConsumoStat = findViewById(R.id.tvConsumoStat);
        tvPoupancaStat = findViewById(R.id.tvPoupancaStat);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnEditarFoto = findViewById(R.id.btnEditarFoto);
        etPrecoKwh = findViewById(R.id.etPrecoKwhPerfil);
        btnGuardarPreco = findViewById(R.id.btnGuardarPreco);
        btnGerirCasas = findViewById(R.id.btnGerirCasas);
        btnLogout = findViewById(R.id.btnLogout);
        btnEliminar = findViewById(R.id.btnEliminarConta);
        btnVoltar = findViewById(R.id.btnVoltar);
    }

    // =========================================================
    // CARREGAR DADOS DO UTILIZADOR (BD LOCAL)
    // =========================================================
    private void carregarDadosDaBD() {
        if (userEmailAtual.isEmpty()) return;

        Cursor c = dbHelper.obterDadosUtilizadorPorEmail(userEmailAtual);

        // Se o utilizador ainda não existir localmente, cria um registo mínimo
        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();

            String uidLocal = "local_" + System.currentTimeMillis();
            dbHelper.saveOrUpdateUser(uidLocal, userEmailAtual, "Utilizador",
                    null, "cliente", "");

            c = dbHelper.obterDadosUtilizadorPorEmail(userEmailAtual);
            if (c != null) c.moveToFirst();
        }

        if (c != null && !c.isClosed() && c.getCount() > 0) {
            String nomeBD = c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_NAME));

            tvNome.setText(nomeBD != null && !nomeBD.isEmpty() ? nomeBD : "Utilizador");
            tvEmail.setText(userEmailAtual);

            double preco = c.getDouble(c.getColumnIndexOrThrow(DBHelper.C_USER_PRECO_KWH));
            precoKwhAtual = preco > 0 ? preco : 0.20;
            etPrecoKwh.setText(String.valueOf(precoKwhAtual));
        }

        if (c != null) c.close();
    }

    // =========================================================
    // CONSUMO E CUSTO ESTIMADO
    // =========================================================
    private void calcularConsumoECusto() {
        // Consumo médio do último período para a casa seleccionada
        double consumoKwh = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);

        if (consumoKwh > 0) {
            tvConsumoStat.setText(String.format("%.0f kWh", consumoKwh));
            tvPoupancaStat.setText(String.format("€ %.2f", consumoKwh * precoKwhAtual));
        } else {
            tvConsumoStat.setText("-- kWh");
            tvPoupancaStat.setText("€ --");
        }
    }

    // =========================================================
    // BOTÕES / ACÇÕES
    // =========================================================
    private void configurarBotoes() {

        // Guardar preço kWh (local + servidor)
        btnGuardarPreco.setOnClickListener(v -> {
            try {
                String valorTexto = etPrecoKwh.getText().toString().replace(",", ".");
                if (valorTexto.isEmpty()) return;

                double novoPreco = Double.parseDouble(valorTexto);

                // 1) Actualizar na BD LOCAL (SQLite)
                int linhas = dbHelper.atualizarPrecoUtilizador(userEmailAtual, novoPreco);

                if (linhas > 0) {
                    precoKwhAtual = novoPreco;
                    calcularConsumoECusto();
                    Toast.makeText(this, "Preço atualizado localmente.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao atualizar BD local.", Toast.LENGTH_SHORT).show();
                }

                // 2) Actualizar também no SERVIDOR (MariaDB)
                UpdatePrecoRequest body = new UpdatePrecoRequest(userEmailAtual, novoPreco);
                api.updatePrecoKwh(body).enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(Call<UserResponse> call,
                                           Response<UserResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(PerfilUtilizador.this,
                                    "Falha ao atualizar preço no servidor (" + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Se quisermos, podemos sincronizar novamente o valor local
                        // precoKwhAtual = response.body().preco_kwh;
                        Toast.makeText(PerfilUtilizador.this,
                                "Preço atualizado no servidor.",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<UserResponse> call, Throwable t) {
                        Toast.makeText(PerfilUtilizador.this,
                                "Não foi possível contactar o servidor.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Valor de preço inválido.", Toast.LENGTH_SHORT).show();
            }
        });

        // Gerir casas do utilizador
        btnGerirCasas.setOnClickListener(
                v -> startActivity(new Intent(this, ListarCasas.class))
        );

        // Alterar foto de perfil
        btnEditarFoto.setOnClickListener(
                v -> escolherImagemLauncher.launch("image/*")
        );

        // Voltar para o ecrã anterior
        btnVoltar.setOnClickListener(v -> finish());

        // Logout: limpar sessão local e voltar ao login
        btnLogout.setOnClickListener(v -> {
            getSharedPreferences("auth", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            CasaSelecionada.getInstance().limpar();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Eliminar conta (por agora só faz logout; apagar no servidor fica para depois)
        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar Conta")
                    .setMessage("Tem a certeza? Isto apagará todos os dados locais.")
                    .setPositiveButton("Sim", (dialog, which) -> btnLogout.performClick())
                    .setNegativeButton("Não", null)
                    .show();
        });
    }

    // =========================================================
    // FOTO DE PERFIL
    // =========================================================

    /**
     * Reduz a imagem escolhida para um tamanho máximo, para evitar bitmaps gigantes.
     */
    private Bitmap reduzirImagem(Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);

        // 1) ler apenas dimensões
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        int maxSize = 1024;
        int scale = 1;

        while ((options.outWidth / scale) / 2 >= maxSize &&
                (options.outHeight / scale) / 2 >= maxSize) {
            scale *= 2;
        }

        // 2) ler imagem já com downscale
        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;

        input = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        return bitmap;
    }

    /**
     * Guarda a foto de perfil em storage interno, associada ao email do utilizador.
     */
    private void guardarFotoPerfil(Bitmap bitmap) {
        try {
            FileOutputStream fos = openFileOutput("profile_" + userEmailAtual + ".png", MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Se já existir uma foto gravada para este utilizador, carrega-a para o ImageView.
     */
    private void carregarFotoExistente() {
        File imgFile = new File(getFilesDir(), "profile_" + userEmailAtual + ".png");
        if (imgFile.exists()) {
            imgPerfil.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
        }
    }
}
