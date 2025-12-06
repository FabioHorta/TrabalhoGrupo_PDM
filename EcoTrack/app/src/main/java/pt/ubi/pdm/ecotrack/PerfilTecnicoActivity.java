package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PerfilTecnicoActivity extends BaseActivityTecnico {

    private TextView tvEmail;
    private EditText etNome;
    private Button btnGuardar;

    private DBHelper db;
    private String emailTecnico;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_tecnico);

        db = new DBHelper(this);

        // 1) obter email do técnico a partir das SharedPreferences
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailTecnico = sp.getString("user_email", null);

        if (emailTecnico == null) {
            Toast.makeText(this, "Sessão expirada. Faz login novamente.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 2) confirmar que é mesmo técnico
        String tipo = db.obterTipoUtilizadorPorEmail(emailTecnico);
        if (!"tecnico".equalsIgnoreCase(tipo)) {
            Toast.makeText(this, "Este perfil é apenas para técnicos.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEmail = findViewById(R.id.tvEmailTecnico);
        etNome  = findViewById(R.id.etNomeTecnico);
        btnGuardar = findViewById(R.id.btnGuardarPerfilTecnico);

        tvEmail.setText(emailTecnico);

        // 3) carregar dados do técnico da BD local
        carregarDadosTecnico();

        btnGuardar.setOnClickListener(v -> guardarAlteracoes());

        // bottom nav
        setupBottomNavTecnico(R.id.menu_perfil_tecnico);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavTecnico != null) {
            bottomNavTecnico.setSelectedItemId(R.id.menu_perfil_tecnico);
        }
    }

    private void carregarDadosTecnico() {
        Cursor c = db.obterDadosUtilizadorPorEmail(emailTecnico);
        if (c != null) {
            if (c.moveToFirst()) {
                int idxNome = c.getColumnIndexOrThrow(DBHelper.C_USER_NAME);
                String nome = c.getString(idxNome);
                etNome.setText(nome != null ? nome : "");
            }
            c.close();
        }
    }

    private void guardarAlteracoes() {
        String novoNome = etNome.getText().toString().trim();
        if (novoNome.isEmpty()) {
            Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show();
            return;
        }

        int linhas = db.atualizarNomeUtilizador(emailTecnico, novoNome);
        if (linhas > 0) {
            Toast.makeText(this, "Perfil atualizado.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Não foi possível atualizar o perfil.", Toast.LENGTH_SHORT).show();
        }
    }
}
