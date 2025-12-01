package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
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

    // Cabeçalho
    private MaterialCardView cardPerfilTopo;
    private ImageView imgPerfilTopo;
    private TextView tvNomeUtilizador;

    private FirebaseAuth mAuth;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_menu);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        initViews();
        atualizarCabecalho(); // Carrega os dados mal abre
    }

    private void initViews(){
        // Botões
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

    private void atualizarCabecalho() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String email = user.getEmail();

            // 1. BUSCAR NOME À BASE DE DADOS LOCAL
            // Usamos o método que criámos no DBHelper para o perfil
            Cursor c = dbHelper.obterDadosUtilizadorPorEmail(email);
            String nomeExibicao = "Utilizador";

            if (c != null && c.moveToFirst()) {
                // Tenta pegar o nome guardado na coluna 'name'
                String nomeBd = c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_NAME));
                if (nomeBd != null && !nomeBd.isEmpty()) {
                    nomeExibicao = nomeBd;
                }
                c.close();
            }

            tvNomeUtilizador.setText(nomeExibicao);

            // 2. BUSCAR FOTO ESPECÍFICA DESTE USER
            // O nome do ficheiro tem de ser IGUAL ao que guardámos no PerfilUtilizadorActivity
            String nomeFicheiro = "profile_" + email + ".png";

            File imgFile = new File(getFilesDir(), nomeFicheiro);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imgPerfilTopo.setImageBitmap(myBitmap);
            } else {
                // Se não existir foto para este email, mete o logótipo
                imgPerfilTopo.setImageResource(R.drawable.ecotrack_logo);
            }

        } else {
            // Se não houver login, manda para o início
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Isto é fundamental!
        // Quando voltas do Perfil para o Menu, este código corre e atualiza a foto/nome
        atualizarCabecalho();
    }
}