package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.RegisterRequest;
import pt.ubi.pdm.ecotrack.models.UserResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etNomeCompleto, etEmail, etNif, etPassword, etConfirmacaoPassword;
    private Button btnCriarConta, btnJaTenhoConta;

    private DBHelper dbHelper;
    private ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);   // mantém o teu XML aqui

        // BD local
        dbHelper = new DBHelper(this);

        // API remota (MariaDB)
        apiService = ApiClient.getRetrofit(this).create(ApiService.class);


        // Ligação às views
        etNomeCompleto = findViewById(R.id.etNomeCompleto);
        etEmail        = findViewById(R.id.etNomedeUtilizador);
        etNif          = findViewById(R.id.etNIF);
        etPassword     = findViewById(R.id.etPassword);
        etConfirmacaoPassword = findViewById(R.id.etConfirmacaoPassword);

        btnCriarConta   = findViewById(R.id.botaodelogin);
        btnJaTenhoConta = findViewById(R.id.signUpPrompt);

        // Botão "Criar Conta"
        btnCriarConta.setOnClickListener(v -> tentarRegistar());

        // Botão "Já tenho conta"
        btnJaTenhoConta.setOnClickListener(v -> {
            // volta ao ecrã de login
            finish();
            // ou se tiveres um LoginActivity específico:
            // startActivity(new Intent(this, LoginActivity.class));
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

        btnCriarConta.setEnabled(false);

        // objeto de pedido para a API
        RegisterRequest request = new RegisterRequest(
                email,
                pass,
                nome,
                nif,
                0.20,
                "cliente"
        );

        apiService.register(request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                btnCriarConta.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();


                    // criar hash offline para login sem internet
                    String offlineHash = org.mindrot.jbcrypt.BCrypt.hashpw(
                            pass,
                            org.mindrot.jbcrypt.BCrypt.gensalt()
                    );

                // guardar utilizador na BD local já sincronizado com o servidor
                    dbHelper.saveOrUpdateUser(
                            String.valueOf(user.getId()),
                            user.getEmail(),
                            user.getName(),
                            user.getPreco_kwh(),
                            user.getTipo(),
                            offlineHash
                    );

                    Toast.makeText(RegisterActivity.this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();

                    // ir para o ecrã principal (ou login, como preferires)
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Erro ao criar conta (email já registado ou dados inválidos).",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                btnCriarConta.setEnabled(true);
                Toast.makeText(RegisterActivity.this,
                        "Erro de ligação ao servidor: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
