package pt.ubi.pdm.ecotrack;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;


/**
 * Activity que determina o melhor tipo de energia renovável (Solar vs Eólica).
 *
 * Processo:
 * 1. Pede permissão e obtém a localização GPS atual.
 * 2. Consulta a API Open-Meteo para dados meteorológicos (vento, sol, nuvens).
 * 3. Aplica um algoritmo de pontuação para sugerir a melhor tecnologia.
 */
public class TipoEnergia extends AppCompatActivity {

    private Button btnVoltarMenu, btnObterLocalizacao;
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;
    private AlertDialog loadingDialog;
    private static final String TAG = "TipoEnergia";

    // Launcher para pedir permissões de localização
    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    obterLocalizacao();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    obterLocalizacao();
                } else {
                    Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tipo_energia);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executorService = Executors.newSingleThreadExecutor();

        initViews();
    }

    private void initViews(){
        btnVoltarMenu = findViewById(R.id.btnVoltarMenu);
        btnObterLocalizacao = findViewById(R.id.btnObterLocalizacao);

        btnObterLocalizacao.setOnClickListener(v -> verificarPermissaoEObterLocalizacao());
        btnVoltarMenu.setOnClickListener(v -> finish());
    }

    /**
     * Verifica permissões antes de tentar obter a localização.
     */
    private void verificarPermissaoEObterLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacao();
        } else {
            locationPermissionRequest.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    //Tenta obter a última localização conhecida ou a localização atual.
    private void obterLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mostrarLoadingDialog();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        processarLocalizacao(location);
                    } else {
                        obterLocalizacaoAtual();// Se lastLocation for null, força update
                    }
                })
                .addOnFailureListener(this, e -> {
                    esconderLoadingDialog();
                    mostrarDialogErro("Erro ao obter localização: " + e.getMessage());
                });
    }

    private void obterLocalizacaoAtual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
        ).addOnSuccessListener(this, location -> {
            if (location != null) {
                processarLocalizacao(location);
            } else {
                esconderLoadingDialog();
                mostrarDialogErro("Não foi possível obter localização atual");
            }
        });
    }

    private void processarLocalizacao(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Chama a API com as coordenadas
        buscarDadosMeteorologicos(latitude, longitude);
    }

    //Faz o pedido HTTP à API Open-Meteo numa thread separada.
    private void buscarDadosMeteorologicos(double latitude, double longitude) {
        executorService.execute(() -> {
            try {
                String urlString = String.format(
                        Locale.US,
                        "https://api.open-meteo.com/v1/forecast" +
                                "?latitude=%.6f&longitude=%.6f" +
                                "&current=temperature_2m,wind_speed_10m,cloud_cover" +
                                "&daily=sunrise,sunset,daylight_duration,sunshine_duration,wind_speed_10m_max" +
                                "&timezone=Europe/Lisbon",
                        latitude, longitude 
                );

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    analisarDadosEnergeticos(response.toString(), latitude, longitude);

                } else {
                    runOnUiThread(() -> {
                        esconderLoadingDialog();
                        mostrarDialogErro("Erro ao obter dados: " + responseCode);
                    });
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar dados meteorológicos", e);
                runOnUiThread(() -> {
                    esconderLoadingDialog();
                    mostrarDialogErro("Erro de conexão: " + e.getMessage());
                });
            }
        });
    }

    //Faz o parse do JSON e calcula as variáveis meteorológicas chave.
    private void analisarDadosEnergeticos(String jsonResponse, double latitude, double longitude) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject current = json.getJSONObject("current");
            JSONObject daily = json.getJSONObject("daily");

            // Extrair valores atuais e diários
            double windSpeed = current.getDouble("wind_speed_10m");
            double cloudCover = current.getDouble("cloud_cover");
            double temperature = current.getDouble("temperature_2m");

            JSONArray sunshineDuration = daily.getJSONArray("sunshine_duration");
            JSONArray daylightDuration = daily.getJSONArray("daylight_duration");
            JSONArray windSpeedMax = daily.getJSONArray("wind_speed_10m_max");

            // Converter segundos para horas
            double sunshineHours = sunshineDuration.getDouble(0) / 3600.0;
            double daylightHours = daylightDuration.getDouble(0) / 3600.0;
            double maxWindSpeed = windSpeedMax.getDouble(0);

            // Calcular percentagem de sol efetivo
            double sunshinePercentage = (sunshineHours / daylightHours) * 100;

            DadosEnergia dados = new DadosEnergia(
                    latitude, longitude, temperature, windSpeed, maxWindSpeed,
                    sunshinePercentage, sunshineHours, cloudCover
            );

            // Determinar a recomendação final
            String recomendacao = determinarEnergiaComDadosReais(
                    windSpeed, maxWindSpeed, sunshinePercentage, cloudCover,
                    latitude, longitude
            );

            // Atualizar UI na Thread Principal
            runOnUiThread(() -> {
                esconderLoadingDialog();
                mostrarDialogResultado(dados, recomendacao);
            });

        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar JSON", e);
            runOnUiThread(() -> {
                esconderLoadingDialog();
                mostrarDialogErro("Erro ao processar dados");
            });
        }
    }

    /**
     * Algoritmo de pontuação para decidir entre Solar, Eólica ou Híbrida.
     * Baseia-se em vento e insolação.
     */
    private String determinarEnergiaComDadosReais(
            double windSpeed, double maxWindSpeed, double sunshinePercentage,
            double cloudCover, double latitude, double longitude) {

        final double WIND_THRESHOLD = 15.0; // km/h
        final double SUN_THRESHOLD = 60.0; // %

        int solarScore = 0;
        int windScore = 0;

        // Pontuação Solar
        if (sunshinePercentage > 70) solarScore += 3;
        else if (sunshinePercentage > SUN_THRESHOLD) solarScore += 2;
        else if (sunshinePercentage > 40) solarScore += 1;

        if (cloudCover < 30) solarScore += 2;
        else if (cloudCover < 50) solarScore += 1;

        // Ajuste por latitude (Sul de Portugal tem mais sol)
        if (latitude < 38.5) solarScore += 2;
        else if (latitude < 40) solarScore += 1;

        // Pontuação Eólica
        if (maxWindSpeed > 25) windScore += 3;
        else if (maxWindSpeed > WIND_THRESHOLD) windScore += 2;
        else if (maxWindSpeed > 10) windScore += 1;

        if (windSpeed > 20) windScore += 2;
        else if (windSpeed > WIND_THRESHOLD) windScore += 1;

        // Ajuste geográfico (Norte/Litoral costuma ter mais vento)
        if (latitude > 41) windScore += 2;
        else if (latitude > 39.5) windScore += 1;

        // Ajuste para litoral
        if (longitude > -9.0) windScore += 1;


        // Decisão final
        String tipoEnergia;
        String descricao;

        if (solarScore > windScore + 2) {
            tipoEnergia = "☀️ ENERGIA SOLAR";
            descricao = "Excelentes condições de insolação e baixa cobertura de nuvens. " +
                    "Instale painéis fotovoltaicos para máxima eficiência.";
        } else if (windScore > solarScore + 2) {
            tipoEnergia = "💨 ENERGIA EÓLICA";
            descricao = "Ventos consistentes e fortes com bom potencial eólico. " +
                    "Considere mini-turbinas eólicas.";
        } else if (solarScore > windScore) {
            tipoEnergia = "☀️ ENERGIA SOLAR";
            descricao = "Boas condições solares com vento moderado. " +
                    "Painéis solares são recomendados.";
        } else if (windScore > solarScore) {
            tipoEnergia = "💨 ENERGIA EÓLICA";
            descricao = "Boas condições de vento com sol moderado. " +
                    "Turbinas eólicas são recomendadas.";
        } else {
            tipoEnergia = "🔋 SISTEMA HÍBRIDO";
            descricao = "Potencial equilibrado entre solar e eólica. " +
                    "Combine ambas as tecnologias para máxima eficiência!";
        }

        return tipoEnergia + "\n\n" + descricao;
    }

    // ========== DIALOGS ==========
    private void mostrarLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.obter_informacoes_energia, null);
        builder.setView(view);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        loadingDialog.show();
    }

    private void esconderLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void mostrarDialogResultado(DadosEnergia dados, String recomendacao) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.resultados_informacao_energia, null);

        TextView tvLocalizacao = view.findViewById(R.id.tvLocalizacao);
        TextView tvTemperatura = view.findViewById(R.id.tvTemperatura);
        TextView tvVento = view.findViewById(R.id.tvVento);
        TextView tvSol = view.findViewById(R.id.tvSol);
        TextView tvNuvens = view.findViewById(R.id.tvNuvens);
        TextView tvRecomendacao = view.findViewById(R.id.tvRecomendacao);
        Button btnFechar = view.findViewById(R.id.btnFechar);

        tvLocalizacao.setText(String.format("📍 %.4f, %.4f", dados.latitude, dados.longitude));
        tvTemperatura.setText(String.format("🌡️ %.1f°C", dados.temperatura));
        tvVento.setText(String.format("💨 %.1f km/h (máx: %.1f km/h)", dados.ventoAtual, dados.ventoMax));
        tvSol.setText(String.format("☀️ %.1f%% (%.1fh de sol)", dados.solPercentagem, dados.solHoras));
        tvNuvens.setText(String.format("☁️ %.0f%% de cobertura", dados.nuvens));
        tvRecomendacao.setText(recomendacao);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnFechar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void mostrarDialogErro(String mensagem) {
        new AlertDialog.Builder(this)
                .setTitle("❌ Erro")
                .setMessage(mensagem)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    // Classe auxiliar para dados
    private static class DadosEnergia {
        double latitude, longitude, temperatura, ventoAtual, ventoMax;
        double solPercentagem, solHoras, nuvens;

        DadosEnergia(double lat, double lon, double temp, double vento, double ventoMax,
                     double sol, double solHoras, double nuvens) {
            this.latitude = lat;
            this.longitude = lon;
            this.temperatura = temp;
            this.ventoAtual = vento;
            this.ventoMax = ventoMax;
            this.solPercentagem = sol;
            this.solHoras = solHoras;
            this.nuvens = nuvens;
        }
    }
}
