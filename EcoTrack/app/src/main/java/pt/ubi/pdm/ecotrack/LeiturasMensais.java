package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileOutputStream;
import java.io.IOException;

public class LeiturasMensais extends BaseActivity {


    private TextView tvLeituraAnterior, tvResultado;
    private EditText etNovaLeitura;
    private Button btnEscolherImagem, btnCalcular, btnTirarFoto, btnGuardar, btnVoltar, btnVerHistorico;
    private ImageView imgContador;

    private Uri imagemSelecionadaUri;

    private DBHelper dbHelper;
    private Bitmap imagemAtualBitmap;

    // Launchers
    private final ActivityResultLauncher<String> escolherImagemLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imagemSelecionadaUri = uri;
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        imgContador.setImageBitmap(bitmap);
                        imagemAtualBitmap = bitmap;
                    } catch (IOException e) {
                        Toast.makeText(this, "Erro ao carregar imagem.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> tirarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            imgContador.setImageBitmap(imageBitmap);
                            imagemAtualBitmap = imageBitmap;
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leituras_mensais);
        setupBottomNav(R.id.nav_leituras);
        dbHelper = new DBHelper(this);

        ligarViews();
        configurarEventos();
    }

    private void ligarViews() {
        tvLeituraAnterior = findViewById(R.id.tvLeituraAnterior);
        etNovaLeitura = findViewById(R.id.etNovaLeitura);
        btnEscolherImagem = findViewById(R.id.btnEscolherImagem);
        imgContador = findViewById(R.id.imgContador);
        btnCalcular = findViewById(R.id.btnCalcular);
        tvResultado = findViewById(R.id.tvResultado);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnVoltar = findViewById(R.id.btnVoltar);
        btnTirarFoto = findViewById(R.id.btnTirarFoto);
        btnVerHistorico = findViewById(R.id.btnVerHistorico);

        double leituraAnterior = dbHelper.obterUltimaLeituraOuDefault(0);
        if (leituraAnterior > 0) {
            tvLeituraAnterior.setText("Leitura anterior do contador: " + leituraAnterior + " kWh");
        } else {
            tvLeituraAnterior.setText("Ainda não existem leituras anteriores.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvLeituraAnterior != null) {
            double leituraAnterior = dbHelper.obterUltimaLeituraOuDefault(0);
            if (leituraAnterior > 0) {
                tvLeituraAnterior.setText("Leitura anterior do contador: " + leituraAnterior + " kWh");
            } else {
                tvLeituraAnterior.setText("Ainda não existem leituras anteriores.");
            }
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_leituras);
        }
    }

    private void configurarEventos() {
        btnEscolherImagem.setOnClickListener(v -> escolherImagemLauncher.launch("image/*"));
        btnCalcular.setOnClickListener(v -> calcularConsumo());
        btnGuardar.setOnClickListener(v -> guardarLeituraComImagem());
        btnVoltar.setOnClickListener(v -> finish());
        btnTirarFoto.setOnClickListener(v -> abrirCamera());
        btnVerHistorico.setOnClickListener(v -> {
            Intent intent = new Intent(LeiturasMensais.this, ResumoLeituras.class);
            startActivity(intent);
        });
    }

    private void calcularConsumo() {
        String novaLeituraStr = etNovaLeitura.getText().toString().trim();
        if (novaLeituraStr.isEmpty()) {
            Toast.makeText(this, "Insere a leitura atual do contador (kWh).", Toast.LENGTH_SHORT).show();
            return;
        }

        double leituraAtual;
        try {
            leituraAtual = Double.parseDouble(novaLeituraStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor inválido. Usa apenas números.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (leituraAtual < 0) {
            Toast.makeText(this, "A leitura não pode ser negativa.", Toast.LENGTH_SHORT).show();
            return;
        }

        double leituraAnterior = dbHelper.obterUltimaLeituraOuDefault(0);
        if (leituraAnterior > 0 && leituraAtual < leituraAnterior) {
            Toast.makeText(this, "A nova leitura não pode ser inferior à leitura anterior do contador.", Toast.LENGTH_SHORT).show();
            return;
        }

        double consumoPeriodo = leituraAnterior > 0 ? leituraAtual - leituraAnterior : 0;
        if (leituraAnterior > 0) {
            tvResultado.setText(String.format("Consumo desde a leitura anterior: %.1f kWh", consumoPeriodo));
        } else {
            tvResultado.setText(String.format("Leitura registada: %.1f kWh. Esta é a primeira leitura.", leituraAtual));
        }

        int totalLeituras = dbHelper.contarLeituras();
        if (totalLeituras < 2) {
            Toast.makeText(this, "Serão necessárias pelo menos 2 leituras para análise.", Toast.LENGTH_LONG).show();
            return;
        }

        double mediaConsumos = dbHelper.calcularMediaConsumos(6);
        if (mediaConsumos <= 0 || consumoPeriodo <= 0) {
            Toast.makeText(this, "Leitura registada. Serão necessárias mais leituras para analisar anomalias.", Toast.LENGTH_LONG).show();
            return;
        }

        double percentagemAumento = ((consumoPeriodo - mediaConsumos) / mediaConsumos) * 100.0;

        if (percentagemAumento > DBHelper.LIMITE_PERCENTUAL_SUP) {
            Toast.makeText(this,
                    String.format("Atenção: o consumo deste período (%.1f kWh) está %.1f%% acima da média (%.1f kWh).",
                            consumoPeriodo, percentagemAumento, mediaConsumos),
                    Toast.LENGTH_LONG).show();
        } else if (percentagemAumento < DBHelper.LIMITE_PERCENTUAL_INF) {
            Toast.makeText(this,
                    String.format("Bom trabalho! O consumo deste período (%.1f kWh) está %.1f%% abaixo da média (%.1f kWh).",
                            consumoPeriodo, Math.abs(percentagemAumento), mediaConsumos),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    String.format("Consumo dentro de valores normais. Este período: %.1f kWh, média: %.1f kWh.",
                            consumoPeriodo, mediaConsumos),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarLeituraComImagem() {
        String leituraStr = etNovaLeitura.getText().toString().trim();
        if (leituraStr.isEmpty()) {
            Toast.makeText(this, "Insere primeiro a leitura do contador.", Toast.LENGTH_SHORT).show();
            return;
        }

        double leituraValor;
        try {
            leituraValor = Double.parseDouble(leituraStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor de leitura inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        double leituraAnterior = dbHelper.obterUltimaLeituraOuDefault(0);
        if (leituraAnterior > 0 && leituraValor < leituraAnterior) {
            Toast.makeText(this, "A nova leitura não pode ser inferior à leitura anterior.", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagemPath = null;
        if (imagemAtualBitmap != null) {
            imagemPath = guardarImagemInterna(imagemAtualBitmap);
        }

        String dataHoje = java.time.LocalDate.now().toString();
        long id = dbHelper.inserirLeituraComFoto(dataHoje, leituraValor, imagemPath);

        if (id > 0) {
            double novaLeituraAnterior = dbHelper.obterUltimaLeituraOuDefault(0);
            if (novaLeituraAnterior > 0) {
                tvLeituraAnterior.setText("Leitura anterior do contador: " + novaLeituraAnterior + " kWh");
            } else {
                tvLeituraAnterior.setText("Ainda não existem leituras anteriores.");
            }

            try (Cursor analise = dbHelper.obterAnaliseConsumo(id)) {
                if (analise != null && analise.moveToFirst()) {
                    double consumo = analise.getDouble(analise.getColumnIndexOrThrow(DBHelper.C_CONSUMO_ANALISADO_VALOR));
                    double mediaRef = analise.getDouble(analise.getColumnIndexOrThrow(DBHelper.C_CONSUMO_ANALISADO_MEDIA_REF));
                    double percent = analise.getDouble(analise.getColumnIndexOrThrow(DBHelper.C_CONSUMO_ANALISADO_PERCENTAGEM));
                    String status = analise.getString(analise.getColumnIndexOrThrow(DBHelper.C_CONSUMO_ANALISADO_STATUS));
                    String msg = String.format("Análise: %s — consumo %.1f kWh — %.1f%% (média %.1f kWh)",
                            status, consumo, percent, mediaRef);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Leitura guardada. Será necessária mais leitura para análise.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            etNovaLeitura.setText("");
            tvResultado.setText("");
            imgContador.setImageResource(android.R.drawable.ic_menu_camera);
            imagemAtualBitmap = null;
            imagemSelecionadaUri = null;
        } else {
            Toast.makeText(this, "Erro ao guardar leitura com imagem.", Toast.LENGTH_SHORT).show();
        }
    }

    private String guardarImagemInterna(Bitmap bitmap) {
        if (bitmap == null) return null;

        String fileName = "contador_" + System.currentTimeMillis() + ".png";

        try {
            FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao guardar imagem.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void abrirCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            tirarFotoLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Não foi possível abrir a câmara.", Toast.LENGTH_SHORT).show();
        }
    }
}