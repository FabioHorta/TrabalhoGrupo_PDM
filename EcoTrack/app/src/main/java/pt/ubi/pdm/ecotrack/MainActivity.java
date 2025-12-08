package pt.ubi.pdm.ecotrack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.GoogleLoginRequest;
import pt.ubi.pdm.ecotrack.models.LoginRequest;
import pt.ubi.pdm.ecotrack.models.UserResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // ---------------------------------------------------------
    // Views
    // ---------------------------------------------------------
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;

    // ---------------------------------------------------------
    // Acesso a dados e API
    // ---------------------------------------------------------
    private DBHelper dbHelper;
    private ApiService apiService;

    // ---------------------------------------------------------
    // Google Sign-In
    // ---------------------------------------------------------
    private GoogleSignInClient googleSignInClient;
    private static final int RC_GOOGLE_SIGN_IN = 1001;

    // ---------------------------------------------------------
    // onCreate – inicialização da Activity
    // ---------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instância da BD local
        dbHelper = new DBHelper(this);

        // Instância do serviço Retrofit para falar com o servidor Node/MariaDB
        apiService = ApiClient.getRetrofit(this).create(ApiService.class);

        // Ligação às views do layout
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // -----------------------------------------------------
        // Clique no botão "Login"
        // -----------------------------------------------------
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            // Validação básica de campos
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Preenche o email e a palavra-passe.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Se houver internet tenta login no servidor, senão faz login offline
            if (temInternet()) {
                fazerLoginServidor(email, password);
            } else {
                fazerLoginOffline(email, password);
            }
        });

        // -----------------------------------------------------
        // Clique no botão "Registar"
        // Abre a RegisterActivity para criar nova conta
        // -----------------------------------------------------
        btnRegister.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        // -----------------------------------------------------
        // Configuração do Google Sign-In
        // - pede email
        // - pede ID Token para enviar para o backend
        // -----------------------------------------------------
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        // Cliente Google para iniciar o fluxo de login com Google
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Botão "Entrar com Google"
        MaterialButton btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            // Inicia fluxo de login Google, resultado tratado em onActivityResult
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    // ---------------------------------------------------------
    // Verificar se existe ligação à internet (WiFi / Dados / Ethernet)
    // ---------------------------------------------------------
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

    // ---------------------------------------------------------
    // onActivityResult – resposta do fluxo Google Sign-In
    // ---------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Trata apenas o resultado do login com Google
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Conta Google autenticada
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account.getIdToken();
                if (idToken != null) {
                    // Envia ID Token para o servidor Node para validar e criar/login utilizador
                    enviarTokenParaServidor(idToken);
                } else {
                    Toast.makeText(this, "Erro a obter token Google.", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Login Google falhou: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------------------------------------------------------
    // Enviar o ID Token do Google para o servidor
    // - O servidor valida o token junto da Google
    // - Cria ou obtém o utilizador na MariaDB
    // - Devolve UserResponse (agora com token JWT)
    // ---------------------------------------------------------
    private void enviarTokenParaServidor(String idToken) {
        GoogleLoginRequest body = new GoogleLoginRequest(idToken);

        apiService.loginWithGoogle(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();

                    // guardar token JWT nas SharedPreferences
                    if (user.getToken() != null) {
                        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                        prefs.edit()
                                .putString("auth_token", user.getToken())
                                .apply();
                    }

                    // Para login offline com Google:
                    // é usado um hash da string do email (não há password real)
                    String offlineHash = org.mindrot.jbcrypt.BCrypt.hashpw(
                            user.getEmail(),
                            org.mindrot.jbcrypt.BCrypt.gensalt()
                    );

                    // Guarda/actualiza o utilizador na BD local
                    dbHelper.saveOrUpdateUser(
                            String.valueOf(user.getId()),
                            user.getEmail(),
                            user.getName(),
                            user.getPreco_kwh(),
                            user.getTipo(),
                            offlineHash
                    );

                    // Guarda o email do utilizador nas SharedPreferences (para usar no resto da app)
                    getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .putString("user_email", user.getEmail())
                            .apply();

                    // Restaura dados do servidor para a BD local se esta estiver vazia
                    SyncUtils.restaurarTudoSeNecessario(MainActivity.this);
                    // Escolhe o ecrã seguinte consoante o tipo de utilizador
                    Intent i;

                    if ("tecnico".equalsIgnoreCase(user.getTipo())) {
                        i = new Intent(MainActivity.this, HomeTecnico.class);
                    } else {
                        i = new Intent(MainActivity.this, MenuPrincipal.class);
                    }
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Falha na autenticação Google (servidor).",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Erro de ligação ao servidor.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------------------------------------------------------
    // Login OFFLINE (sem internet)
    // - Só usa a BD local (SQLite)
    // ---------------------------------------------------------
    private void fazerLoginOffline(String email, String password) {
        // Procura o utilizador na BD local pelo email
        Cursor c = dbHelper.obterDadosUtilizadorPorEmail(email);

        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            Toast.makeText(this,
                    "Utilizador não encontrado na BD local (offline).",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Vai buscar o hash da password guardado localmente
        String hashGuardado = c.getString(c.getColumnIndexOrThrow(DBHelper.C_USER_PASSWORD_HASH));

        // Tipo de utilizador (cliente/tecnico) para decidir o menu
        String tipo = null;
        int idxTipo = c.getColumnIndex(DBHelper.C_USER_TIPO);
        if (idxTipo != -1) {
            tipo = c.getString(idxTipo);
        }
        c.close();

        // Se não houver hash, não dá para autenticar offline
        if (hashGuardado == null || hashGuardado.isEmpty()) {
            Toast.makeText(this,
                    "Não há password guardada localmente para este utilizador.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Verifica a password introduzida com o hash guardado
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

        // Guarda o email nas SharedPreferences
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putString("user_email", email)
                .apply();

        // Abre a activity correspondente ao tipo de utilizador
        Intent i;
        if ("tecnico".equalsIgnoreCase(tipo)) {
            i = new Intent(MainActivity.this, HomeTecnico.class);
        } else {
            i = new Intent(MainActivity.this, MenuPrincipal.class);
        }
        startActivity(i);
        finish();
    }

    // ---------------------------------------------------------
    // Login ONLINE (com servidor)
    // - Envia email/password para a API Node
    // - Se OK, guarda utilizador localmente para offline
    // - Se falhar, tenta automaticamente o login offline
    // ---------------------------------------------------------
    private void fazerLoginServidor(String email, String password) {
        // Desactiva o botão para evitar múltiplos cliques
        btnLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                // Volta a activar o botão
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();

                    //  guardar token JWT nas SharedPreferences
                    if (user.getToken() != null) {
                        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                        prefs.edit()
                                .putString("auth_token", user.getToken())
                                .apply();
                    }

                    // Gera hash BCRYPT da password para guardar offline
                    String offlineHash = BCrypt.hashpw(password, BCrypt.gensalt());

                    // Guarda/actualiza o utilizador na BD local
                    dbHelper.saveOrUpdateUser(
                            String.valueOf(user.getId()),
                            user.getEmail(),
                            user.getName(),
                            user.getPreco_kwh(),
                            user.getTipo(),
                            offlineHash
                    );

                    // Guarda email nas SharedPreferences (duas vezes aqui, mas efeito é o mesmo)
                    getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .putString("user_email", user.getEmail())
                            .apply();

                    getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .putString("user_email", user.getEmail())
                            .apply();

                    // Restaura dados do servidor (casas, appliances, assistências, leituras)
                    // se a BD local estiver vazia para esse utilizador
                    SyncUtils.restaurarTudoSeNecessario(MainActivity.this);

                    // Lê o tipo do utilizador da BD local (garante que está alinhado)
                    String tipoLocal = dbHelper.obterTipoUtilizadorPorEmail(user.getEmail());

                    Toast.makeText(MainActivity.this,
                            "Login feito com sucesso!",
                            Toast.LENGTH_SHORT).show();

                    // Abre o ecrã correspondente ao tipo
                    Intent i;
                    if ("tecnico".equalsIgnoreCase(tipoLocal)) {
                        i = new Intent(MainActivity.this, HomeTecnico.class);
                    } else {
                        i = new Intent(MainActivity.this, MenuPrincipal.class);
                    }

                    startActivity(i);
                    finish();

                } else {
                    // Se o servidor respondeu erro (401, 500, etc), cai para modo offline
                    Toast.makeText(MainActivity.this,
                            "Erro no servidor. A tentar login offline...",
                            Toast.LENGTH_SHORT).show();

                    fazerLoginOffline(email, password);
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                // Falha de comunicação com o servidor (sem rede, timeout, etc.)
                btnLogin.setEnabled(true);

                Toast.makeText(MainActivity.this,
                        "Não foi possível contactar o servidor. A tentar login offline...",
                        Toast.LENGTH_SHORT).show();

                fazerLoginOffline(email, password);
            }
        });
    }

}
