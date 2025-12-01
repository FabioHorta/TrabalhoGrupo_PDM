package pt.ubi.pdm.ecotrack;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private FirebaseAuth mAuth;
    private String userEmailAtual = "";
    private double precoKwhAtual = 0.20;

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

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);
        if (mAuth.getCurrentUser() != null) userEmailAtual = mAuth.getCurrentUser().getEmail();

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
        if (c != null && c.moveToFirst()) {
            tvNome.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_NAME)));
            tvEmail.setText(userEmailAtual);
            double preco = c.getDouble(c.getColumnIndexOrThrow(DBHelper.C_USER_PRECO_KWH));
            precoKwhAtual = preco > 0 ? preco : 0.20;
            etPrecoKwh.setText(String.valueOf(precoKwhAtual));
        }
        if (c != null) c.close();
    }

    private void calcularConsumoECusto() {
        double consumoKwh = dbHelper.calcularMediaConsumos(1);
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
                precoKwhAtual = Double.parseDouble(etPrecoKwh.getText().toString().replace(",", "."));
                dbHelper.atualizarPrecoUtilizador(userEmailAtual, precoKwhAtual);
                Toast.makeText(this, "Preço atualizado!", Toast.LENGTH_SHORT).show();
                calcularConsumoECusto();
            } catch (Exception e) {}
        });

        btnGerirCasas.setOnClickListener(v -> startActivity(new Intent(this, ListarCasas.class)));
        btnEditarFoto.setOnClickListener(v -> escolherImagemLauncher.launch("image/*"));
        btnVoltar.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            try { mAuth.signOut(); } catch (Exception e) {}
            startActivity(new Intent(this, MainActivity.class)); finish();
        });

        // ... (Eliminar igual) ...
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