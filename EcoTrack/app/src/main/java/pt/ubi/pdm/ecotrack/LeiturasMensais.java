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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileOutputStream;
import java.io.IOException;

public class LeiturasMensais extends AppCompatActivity {

    private TextView tvLeituraAnterior, tvResultado;
    private EditText etNovaLeitura;
    private Button btnEscolherImagem, btnCalcular, btnTirarFoto, btnGuardar, btnVoltar, btnVerHistorico;
    private ImageView imgContador;

    private Uri imagemSelecionadaUri;

    private DBHelper dbHelper;
    private double leituraAnterior;
    private Bitmap imagemAtualBitmap;

    // Launcher para escolher imagem da galeria
    private final ActivityResultLauncher<String> escolherImagemLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), new androidx.activity.result.ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        imagemSelecionadaUri = uri;
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), uri);
                            imgContador.setImageBitmap(bitmap);
                            imagemAtualBitmap = bitmap;
                        } catch (IOException e) {
                            Toast.makeText(LeiturasMensais.this, "Erro ao carregar imagem.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    // Launcher para tirar foto com a câmara (thumbnail)
    private final ActivityResultLauncher<Intent> tirarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new androidx.activity.result.ActivityResultCallback<androidx.activity.result.ActivityResult>() {
                @Override
                public void onActivityResult(androidx.activity.result.ActivityResult result) {
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
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leituras_mensais);

        dbHelper = new DBHelper(this);
        // Para ir buscar o valor da última leitura
        leituraAnterior = dbHelper.obterUltimaLeituraOuDefault(0);

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

        if (leituraAnterior > 0) {
            tvLeituraAnterior.setText(
                    "Leitura anterior do contador: " + leituraAnterior + " kWh"
            );
        } else {
            tvLeituraAnterior.setText(
                    "Ainda não existem leituras anteriores."
            );
        }
    }

    private void configurarEventos() {
        btnEscolherImagem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                escolherImagemLauncher.launch("image/*");
            }
        });

        btnCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calcularConsumo();
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarLeituraComImagem();
            }
        });

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnTirarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirCamera();
            }
        });

        btnVerHistorico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LeiturasMensais.this, ResumoLeituras.class);
                startActivity(intent);
            }
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
            Toast.makeText(this,
                    "A leitura não pode ser negativa.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Regra: A nova leitura não pode ser INFERIOR à anterior (mas pode ser igual)
        if (leituraAnterior > 0 && leituraAtual < leituraAnterior) {
            Toast.makeText(this,
                    "A nova leitura não pode ser inferior à leitura anterior do contador.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Calcular o consumo do período
        double consumoPeriodo;
        if (leituraAnterior > 0) {
            consumoPeriodo = leituraAtual - leituraAnterior;
            tvResultado.setText(
                    String.format("Consumo desde a leitura anterior: %.1f kWh", consumoPeriodo)
            );
        } else {
            consumoPeriodo = 0;
            tvResultado.setText(
                    String.format("Leitura registada: %.1f kWh. Esta é a primeira leitura.", leituraAtual)
            );
        }

        // 2) Atualizar leituraAnterior (temporariamente, para cálculos)
        leituraAnterior = leituraAtual;
        tvLeituraAnterior.setText(
                "Leitura anterior do contador: " + leituraAnterior + " kWh"
        );

        // 3) Análise de anomalias baseada em CONSUMOS (diferenças), não nas leituras totais
        //    Usamos a média dos consumos dos últimos 6 períodos.
        double mediaConsumos = dbHelper.calcularMediaConsumos(6);

        // Só faz sentido analisar se já tivermos consumos > 0 e média > 0
        if (mediaConsumos <= 0 || consumoPeriodo <= 0) {
            Toast.makeText(this,
                    "Leitura registada. Serão necessárias mais leituras para analisar anomalias.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        double percentagemAumento =
                ((consumoPeriodo - mediaConsumos) / mediaConsumos) * 100.0;

        if (percentagemAumento > 20.0) {
            Toast.makeText(
                    LeiturasMensais.this,
                    String.format(
                            "Atenção: o consumo deste período (%.1f kWh) está %.1f%% acima da média (%.1f kWh).",
                            consumoPeriodo, percentagemAumento, mediaConsumos
                    ),
                    Toast.LENGTH_LONG
            ).show();
        } else if (percentagemAumento < -20.0) {
            Toast.makeText(
                    LeiturasMensais.this,
                    String.format(
                            "Bom trabalho! O consumo deste período (%.1f kWh) está %.1f%% abaixo da média (%.1f kWh).",
                            consumoPeriodo, Math.abs(percentagemAumento), mediaConsumos
                    ),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    LeiturasMensais.this,
                    String.format(
                            "Consumo dentro de valores normais. Este período: %.1f kWh, média: %.1f kWh.",
                            consumoPeriodo, mediaConsumos
                    ),
                    Toast.LENGTH_SHORT
            ).show();
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

        // Guardar a imagem (se houver) em ficheiro interno
        String imagemPath = null;
        if (imagemAtualBitmap != null) {
            imagemPath = guardarImagemInterna(imagemAtualBitmap);
        }

        String dataHoje = java.time.LocalDate.now().toString();

        long id = dbHelper.inserirLeituraComFoto(dataHoje, leituraValor, imagemPath);

        if (id > 0) {
            Toast.makeText(this, "Leitura e imagem guardadas com sucesso.", Toast.LENGTH_SHORT).show();

            // Atualizar a leitura anterior para a próxima vez
            leituraAnterior = leituraValor;
            tvLeituraAnterior.setText("Leitura anterior do contador: " + leituraAnterior + " kWh");

            // Limpar campos
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
            return fileName;  // este nome é o que vamos guardar na BD
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