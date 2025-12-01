package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class MenuPrincipal extends AppCompatActivity {

    private MaterialCardView btnMelhorTipo, btnAlertas, btnCalculadoraCusto, btnEstimativa, btnLeituras, btnApoioaoCliente;
    private MaterialCardView cardPerfilTopo;
    private ImageView imgPerfilTopo;
    private TextView tvNomeUtilizador;

    private FirebaseAuth mAuth;
    private DBHelper dbHelper; // Precisamos disto para aceder à BD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_menu);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this); // Inicializar

        initViews();
        verificarAutenticacaoEAtualizar();
    }

    private void initViews(){
        // Botões do Grid
        btnMelhorTipo = findViewById(R.id.btnMelhorTipo);
        btnAlertas = findViewById(R.id.btnAlertas);
        btnCalculadoraCusto = findViewById(R.id.btnCalculadora);
        btnEstimativa = findViewById(R.id.btnEstimativa);
        btnLeituras = findViewById(R.id.btnLeituras);
        btnApoioaoCliente = findViewById(R.id.btnApoioaoCliente);

        // Cabeçalho
        cardPerfilTopo = findViewById(R.id.cardPerfilTopo);
        imgPerfilTopo = findViewById(R.id.imgPerfilTopo);
        tvNomeUtilizador = findViewById(R.id.tvNomeUtilizador);

        // --- NAVEGAÇÃO ---
        cardPerfilTopo.setOnClickListener(v -> {
            Intent intent = new Intent(MenuPrincipal.this, PerfilUtilizador.class);
            startActivity(intent);
        });

        btnAlertas.setOnClickListener(v -> startActivity(new Intent(this, AlertasConsumo.class)));
        btnMelhorTipo.setOnClickListener(v -> startActivity(new Intent(this, TipoEnergia.class)));
        btnEstimativa.setOnClickListener(v -> startActivity(new Intent(this, EstimativaConsumo.class)));
        btnLeituras.setOnClickListener(v -> startActivity(new Intent(this, LeiturasMensais.class)));
        btnApoioaoCliente.setOnClickListener(v -> startActivity(new Intent(this, ApoioCliente.class)));
        btnCalculadoraCusto.setOnClickListener(v -> startActivity(new Intent(this, CalculadoraCustos.class)));
    }

    private void verificarAutenticacaoEAtualizar() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // 1. Tentar obter nome do Firebase
            String nomeExibicao = user.getDisplayName();

            // 2. Se o Firebase não tiver nome, VAMOS BUSCAR À BD LOCAL (SQLite)
            if (nomeExibicao == null || nomeExibicao.isEmpty()) {
                nomeExibicao = buscarNomeLocalmente(user.getEmail());
            }

            // 3. Se mesmo assim for nulo, usa o email
            if (nomeExibicao == null || nomeExibicao.isEmpty()) {
                nomeExibicao = user.getEmail();
            }

            // 4. Define o texto (Limita o tamanho para não estragar o layout)
            if (nomeExibicao != null && nomeExibicao.length() > 15) {
                // Pega só o primeiro nome se for muito grande
                nomeExibicao = nomeExibicao.split(" ")[0];
            }

            tvNomeUtilizador.setText(nomeExibicao);

            // Carregar Foto
            carregarFotoPerfil();

        } else {
            Toast.makeText(this, "Sessão inválida.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    // --- TRUQUE: Ler a BD diretamente aqui sem alterar o DBHelper ---
    private String buscarNomeLocalmente(String email) {
        String nomeEncontrado = "";
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Tenta encontrar pelo email do Firebase
            String query = "SELECT " + DBHelper.C_USER_NAME + " FROM " + DBHelper.T_USERS +
                    " WHERE " + DBHelper.C_USER_EMAIL + " = ?";

            cursor = db.rawQuery(query, new String[]{email});

            if (cursor != null && cursor.moveToFirst()) {
                nomeEncontrado = cursor.getString(0);
            } else {
                // Se falhar pelo email, tenta pegar o último utilizador registado (fallback)
                cursor = db.rawQuery("SELECT " + DBHelper.C_USER_NAME + " FROM " + DBHelper.T_USERS + " ORDER BY " + DBHelper.C_USER_ID + " DESC LIMIT 1", null);
                if (cursor != null && cursor.moveToFirst()) {
                    nomeEncontrado = cursor.getString(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return nomeEncontrado;
    }

    private void carregarFotoPerfil() {
        File imgFile = new File(getFilesDir(), "profile_pic.png");
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imgPerfilTopo.setImageBitmap(myBitmap);
        } else {
            imgPerfilTopo.setImageResource(R.drawable.ecotrack_logo);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarAutenticacaoEAtualizar();
    }
}