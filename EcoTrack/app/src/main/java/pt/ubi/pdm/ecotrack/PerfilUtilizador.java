package pt.ubi.pdm.ecotrack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

public class PerfilUtilizador extends AppCompatActivity {

    private TextView tvNome, tvEmail, tvConsumoStat, tvPoupancaStat;
    private ImageView imgPerfil;
    private com.google.android.material.card.MaterialCardView btnEditarFoto;

    private EditText etPrecoKwh;
    private Button btnGuardarPreco;
    private Button btnLogout, btnEliminar, btnVoltar;

    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    //imagem que aparece no topo
    private final ActivityResultLauncher<String> escolherImagemLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        imgPerfil.setImageBitmap(bitmap);
                        guardarFotoPerfil(bitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "Erro ao carregar imagem.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_utilizador);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);
        sharedPreferences = getSharedPreferences("EcoTrackPrefs", Context.MODE_PRIVATE);

        initViews();

        // Carregar dados de Perfil (Usando Firebase pois o DBHelper original nao tem metodo get publico)
        carregarDadosUtilizador();
        carregarFotoExistente();
        carregarPrecoAtual();
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

        btnLogout = findViewById(R.id.btnLogout);
        btnEliminar = findViewById(R.id.btnEliminarConta);
        btnVoltar = findViewById(R.id.btnVoltar);
    }

    private void carregarDadosUtilizador() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String nome = user.getDisplayName();
            String email = user.getEmail();
            tvEmail.setText(email != null ? email : "Sem email");
            tvNome.setText(nome != null && !nome.isEmpty() ? nome : "Utilizador");
        }
    }

    private void carregarFotoExistente() {
        File imgFile = new File(getFilesDir(), "profile_pic.png");
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imgPerfil.setImageBitmap(myBitmap);
        }
    }

    private void carregarPrecoAtual() {
        float precoGuardado = sharedPreferences.getFloat("preco_kwh", 0.20f);
        etPrecoKwh.setText(String.valueOf(precoGuardado));
    }

    private void guardarNovoPreco() {
        String texto = etPrecoKwh.getText().toString().replace(",", ".");
        if (!texto.isEmpty()) {
            try {
                float novoPreco = Float.parseFloat(texto);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("preco_kwh", novoPreco);
                editor.apply();

                Toast.makeText(this, "Preço atualizado!", Toast.LENGTH_SHORT).show();
                calcularConsumoECusto(); // Atualiza logo os valores no ecrã
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Valor inválido.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- CÁLCULO USANDO O DBHelper ORIGINAL ---
    private void calcularConsumoECusto() {
        // 1. Obter Preço definido pelo user
        double precoKwh = sharedPreferences.getFloat("preco_kwh", 0.20f);

        // 2. Obter Último Consumo Real
        // TRUQUE: calcularMediaConsumos(1) devolve a média de 1 período,
        // ou seja, o próprio valor do último consumo registado!
        double consumoKwh = dbHelper.calcularMediaConsumos(1);

        if (consumoKwh > 0) {
            // Mostra o consumo
            tvConsumoStat.setText(String.format("%.0f kWh", consumoKwh));

            // Calcula o custo (Consumo * Preço)
            double custoTotal = consumoKwh * precoKwh;
            tvPoupancaStat.setText(String.format("€ %.2f", custoTotal));
        } else {
            tvConsumoStat.setText("-- kWh");
            tvPoupancaStat.setText("€ --");
        }
    }

    private void configurarBotoes() {
        btnGuardarPreco.setOnClickListener(v -> guardarNovoPreco());
        btnEditarFoto.setOnClickListener(v -> escolherImagemLauncher.launch("image/*"));
        btnVoltar.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            try { mAuth.signOut(); } catch (Exception e) {}
            Toast.makeText(this, "Saiu da conta.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(PerfilUtilizador.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar Conta")
                    .setMessage("Tem a certeza?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        try { if (mAuth.getCurrentUser() != null) mAuth.getCurrentUser().delete(); } catch (Exception e) {}
                        Toast.makeText(this, "Conta eliminada.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(PerfilUtilizador.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void guardarFotoPerfil(Bitmap bitmap) {
        try {
            FileOutputStream fos = openFileOutput("profile_pic.png", MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "Foto atualizada!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}