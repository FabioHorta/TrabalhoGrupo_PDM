package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNomeCompleto, etEmail, etNif, etPassword, etConfirmacaoPassword;
    private Button btnCriarConta, btnJaTenhoConta;

    private FirebaseAuth mAuth;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);   // nome do teu XML

        // Firebase + SQLite
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        // Ligação às views
        etNomeCompleto = findViewById(R.id.NomeCompleto);
        etEmail        = findViewById(R.id.NomedeUtilizador);
        etNif          = findViewById(R.id.NIF);
        etPassword     = findViewById(R.id.Password);
        etConfirmacaoPassword = findViewById(R.id.ConfirmacaoPassword);

        btnCriarConta   = findViewById(R.id.botaodelogin);
        btnJaTenhoConta = findViewById(R.id.signUpPrompt);

        // Botão "Criar Conta"
        btnCriarConta.setOnClickListener(v -> tentarRegistar());

        // Botão "Já tenho conta? Fazer login"
        btnJaTenhoConta.setOnClickListener(v -> {
            // Volta ao ecrã de login
            finish();   // se vieres do MainActivity, isto chega
            // ou, se quiseres garantir:
            // startActivity(new Intent(this, MainActivity.class));
        });
    }

    private void tentarRegistar() {
        String nome   = etNomeCompleto.getText().toString().trim();
        String email  = etEmail.getText().toString().trim();
        String nif    = etNif.getText().toString().trim();
        String pass   = etPassword.getText().toString();
        String pass2  = etConfirmacaoPassword.getText().toString();

        // validações básicas
        if (TextUtils.isEmpty(nome) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(nif) ||
                TextUtils.isEmpty(pass) ||
                TextUtils.isEmpty(pass2)) {

            Toast.makeText(this, "Preenche todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nif.length() != 9 || !TextUtils.isDigitsOnly(nif)) {
            Toast.makeText(this, "NIF inválido (tem de ter 9 dígitos).", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(this, "A password deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(pass2)) {
            Toast.makeText(this, "As passwords não coincidem.", Toast.LENGTH_SHORT).show();
            return;
        }

        // criar conta no Firebase
        btnCriarConta.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    btnCriarConta.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            String mail = user.getEmail() != null ? user.getEmail() : email;

                            // guarda utilizador no SQLite (nome + email + uid)
                            dbHelper.saveOrUpdateUser(uid, mail, nome);

                            Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();

                            // Vai para o login ou para o ecrã principal
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this,
                                "Erro ao criar conta: " +
                                        (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
