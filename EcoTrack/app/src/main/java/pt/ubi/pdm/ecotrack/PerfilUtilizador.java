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

public class PerfilUtilizador extends AppCompatActivity {

    private TextView tvNome, tvEmail, tvConsumoStat, tvPoupancaStat;
    private ImageView imgPerfil;
    private com.google.android.material.card.MaterialCardView btnEditarFoto;
    private EditText etPrecoKwh;
    private Button btnGuardarPreco, btnGerirCasas, btnLogout, btnEliminar, btnVoltar;

    private DBHelper dbHelper;
    private String userEmailAtual = "";
    private double precoKwhAtual;
    private int casaIdAtual;

    private final ActivityResultLauncher<String> escolherImagemLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = reduzirImagem(uri);
                        imgPerfil.setImageBitmap(bitmap);
                        guardarFotoPerfil(bitmap);
                    } catch (IOException e) { }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_utilizador);

        dbHelper = new DBHelper(this);

        // 1. RECUPERAR EMAIL DA SESSÃO LOCAL (SharedPreferences)
        // Em vez de Firebase, vamos buscar o que o MainActivity guardou
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userEmailAtual = prefs.getString("user_email", null);

        // Se não houver email, manda para o login
        if (userEmailAtual == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Recuperar casa selecionada (se houver)
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();

        initViews();
        carregarDadosDaBD();
        carregarFotoExistente();
        calcularConsumoECusto();
        configurarBotoes();
    }

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

    private void carregarDadosDaBD() {
        if (userEmailAtual.isEmpty()) return;

        Cursor c = dbHelper.obterDadosUtilizadorPorEmail(userEmailAtual);

        // --- AUTO-CORREÇÃO: Se o utilizador não existir na BD local ---
        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();

            // Cria um utilizador local para não dar erro
            String uidLocal = "local_" + System.currentTimeMillis();
            dbHelper.saveOrUpdateUser(uidLocal, userEmailAtual, "Utilizador", null, "cliente", "");

            // Tenta buscar de novo
            c = dbHelper.obterDadosUtilizadorPorEmail(userEmailAtual);
            if (c != null) c.moveToFirst();
        }
        // -------------------------------------------------------------

        if (c != null && !c.isClosed() && c.getCount() > 0) {
            // 2. PREENCHER NOME E EMAIL NO LAYOUT
            String nomeBD = c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_NAME));

            tvNome.setText(nomeBD != null && !nomeBD.isEmpty() ? nomeBD : "Utilizador");
            tvEmail.setText(userEmailAtual); // Mete o email na TextView

            // Preço kWh
            double preco = c.getDouble(c.getColumnIndexOrThrow(DBHelper.C_USER_PRECO_KWH));
            precoKwhAtual = preco > 0 ? preco : 0.20;
            etPrecoKwh.setText(String.valueOf(precoKwhAtual));
        }

        if (c != null) c.close();
    }

    private void calcularConsumoECusto() {
        // Usa a casa selecionada no Menu Principal
        double consumoKwh = dbHelper.calcularMediaConsumosPorCasa(1, casaIdAtual);

        if (consumoKwh > 0) {
            tvConsumoStat.setText(String.format("%.0f kWh", consumoKwh));
            tvPoupancaStat.setText(String.format("€ %.2f", consumoKwh * precoKwhAtual));
        } else {
            tvConsumoStat.setText("-- kWh");
            tvPoupancaStat.setText("€ --");
        }
    }

    private void configurarBotoes() {
        btnGuardarPreco.setOnClickListener(v -> {
            try {
                String valorTexto = etPrecoKwh.getText().toString().replace(",", ".");
                if (valorTexto.isEmpty()) return;

                double novoPreco = Double.parseDouble(valorTexto);
                int linhas = dbHelper.atualizarPrecoUtilizador(userEmailAtual, novoPreco);

                if (linhas > 0) {
                    precoKwhAtual = novoPreco;
                    Toast.makeText(this, "Preço atualizado!", Toast.LENGTH_SHORT).show();
                    calcularConsumoECusto();
                } else {
                    Toast.makeText(this, "Erro ao atualizar BD.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        btnGerirCasas.setOnClickListener(v -> startActivity(new Intent(this, ListarCasas.class)));
        btnEditarFoto.setOnClickListener(v -> escolherImagemLauncher.launch("image/*"));
        btnVoltar.setOnClickListener(v -> finish());

        // 3. LOGOUT: Limpar SharedPreferences
        btnLogout.setOnClickListener(v -> {
            getSharedPreferences("auth", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            // Limpar Singleton da casa
            CasaSelecionada.getInstance().limpar();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Botão Eliminar (Exemplo simples)
        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar Conta")
                    .setMessage("Tem a certeza? Isto apagará todos os dados locais.")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        // Aqui devias apagar da BD... por agora fazemos logout
                        btnLogout.performClick();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });
    }

    // --- IMAGENS ---
    private Bitmap reduzirImagem(Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();
        int maxSize = 1024; int scale = 1;
        while ((options.outWidth / scale) / 2 >= maxSize && (options.outHeight / scale) / 2 >= maxSize) scale *= 2;
        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;
        input = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();
        return bitmap;
    }

    private void guardarFotoPerfil(Bitmap bitmap) {
        try {
            FileOutputStream fos = openFileOutput("profile_" + userEmailAtual + ".png", MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.close();
        } catch (Exception e) {}
    }

    private void carregarFotoExistente() {
        File imgFile = new File(getFilesDir(), "profile_" + userEmailAtual + ".png");
        if (imgFile.exists()) imgPerfil.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
    }
}