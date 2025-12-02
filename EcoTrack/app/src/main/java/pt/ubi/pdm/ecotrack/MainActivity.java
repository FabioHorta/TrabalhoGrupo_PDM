package pt.ubi.pdm.ecotrack;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.mindrot.jbcrypt.BCrypt;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.LoginRequest;
import pt.ubi.pdm.ecotrack.models.UserResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;

    private DBHelper dbHelper;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        apiService = ApiClient.getRetrofit().create(ApiService.class);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Preenche o email e a palavra-passe.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (temInternet()) {
                fazerLoginServidor(email, password);
            } else {
                fazerLoginOffline(email, password);
            }
        });

        btnRegister.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(i);
        });
    }

    private boolean temInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    private void fazerLoginOffline(String email, String password) {
        Cursor c = dbHelper.obterDadosUtilizadorPorEmail(email);

        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            Toast.makeText(this,
                    "Utilizador não encontrado na BD local (offline).",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String hashGuardado = c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_PASSWORD_HASH));
        String tipo = null;
        int idxTipo = c.getColumnIndex(DBHelper.C_USER_TIPO);
        if (idxTipo != -1) {
            tipo = c.getString(idxTipo);
        }
        c.close();

        if (hashGuardado == null || hashGuardado.isEmpty()) {
            Toast.makeText(this,
                    "Não há password guardada localmente para este utilizador.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean ok;
        try {
            ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, hashGuardado);
        } catch (Exception e) {
            ok = false;
        }

        if (!ok) {
            Toast.makeText(this,
                    "Credenciais inválidas (modo offline).",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // guardar sessão
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putString("user_email", email)
                .apply();

        // abrir ecrã correto
        Intent i;
        if ("tecnico".equalsIgnoreCase(tipo)) {
            i = new Intent(MainActivity.this, HomeTecnico.class);
        } else {
            i = new Intent(MainActivity.this, MenuPrincipal.class);
        }
        startActivity(i);
        finish();
    }


    private void fazerLoginServidor(String email, String password) {
        btnLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();

                    // 1) criar hash local para uso offline
                    String offlineHash = BCrypt.hashpw(password, BCrypt.gensalt());

                    // 2) guardar/atualizar utilizador na SQLite
                    dbHelper.saveOrUpdateUser(
                            String.valueOf(user.getId()),
                            user.getEmail(),
                            user.getName(),
                            user.getPreco_kwh(),
                            user.getTipo(),
                            offlineHash
                    );
                    // 3) guardar sessão
                    getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .putString("user_email", user.getEmail())
                            .apply();

                    // 4) tipo/local
                    String tipoLocal = dbHelper.obterTipoUtilizadorPorEmail(user.getEmail());

                    Toast.makeText(MainActivity.this,
                            "Login feito com sucesso!",
                            Toast.LENGTH_SHORT).show();

                    Intent i;
                    if ("tecnico".equalsIgnoreCase(tipoLocal)) {
                        i = new Intent(MainActivity.this, HomeTecnico.class);
                    } else {
                        i = new Intent(MainActivity.this, MenuPrincipal.class);
                    }

                    startActivity(i);
                    finish();

                } else {
                    Toast.makeText(MainActivity.this,
                            "Credenciais inválidas ou erro no servidor.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                Toast.makeText(MainActivity.this,
                        "Erro de ligação ao servidor.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }


}
