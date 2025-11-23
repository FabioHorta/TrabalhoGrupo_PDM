package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;

    private FirebaseAuth mAuth;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // SQLite
        dbHelper = new DBHelper(this);

        // Ligar às views do XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Clique em "Entrar"
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preenche o email e a palavra-passe.", Toast.LENGTH_SHORT).show();
                return;
            }

            fazerLoginFirebase(email, password);
        });

        // Clique em "Criar conta" (depois criamos uma RegisterActivity)
        btnRegister.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(i);
        });

    }

    private void fazerLoginFirebase(String email, String password) {
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // guardar no SQLite
                            String uid = user.getUid();
                            String mail = user.getEmail() != null ? user.getEmail() : email;
                            String nome = user.getDisplayName(); // pode vir null, não faz mal

                            dbHelper.saveOrUpdateUser(uid, mail, nome);

                            Toast.makeText(this, "Login feito com sucesso!", Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(MainActivity.this, MenuPrincipal.class);
                            startActivity(i);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Falha no login: " +
                                        (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
