package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.navigation.NavigationBarView;

// Importações do ML Kit para o Reconhecimento Óptico de Caracteres (OCR)
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LeiturasMensais extends BaseActivity {

    // Variáveis para ligar aos elementos do layout (XML)
    private TextView tvNomeCasaLeituras;
    private TextView tvLeituraAnterior, tvResultado;
    private EditText etNovaLeitura;
    private Button btnEscolherImagem, btnCalcular, btnTirarFoto, btnGuardar, btnVerHistorico;
    private ImageView imgContador;

    // Variável para guardar o URI da imagem selecionada (se vier da galeria)
    private Uri imagemSelecionadaUri;

    // Helpers e dados
    private DBHelper dbHelper;
    private Bitmap imagemAtualBitmap; // usamos um Bitmap para trabalhar com a imagem (câmera ou galeria)

    // Multi-casa: informações sobre a casa atualmente selecionada (armazenadas em CasaSelecionada)
    private int casaIdAtual;
    private String casaNomeAtual;

    // Launchers (os novos métodos para iniciar Activities e obter resultados)

    // Launcher para escolher uma imagem da galeria (GetContent)
    private final ActivityResultLauncher<String> escolherImagemLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imagemSelecionadaUri = uri;
                    try {
                        // Carrega a imagem com redução de tamanho para evitar OutOfMemoryError
                        Bitmap bitmap = carregarImagemReduzida(uri);
                        imgContador.setImageBitmap(bitmap);
                        imagemAtualBitmap = bitmap;

                        // tentar ler o número da imagem e preencher a leitura
                        reconhecerTextoDaImagemEPreencher(bitmap);

                    } catch (IOException e) {
                        Toast.makeText(this, "Erro ao carregar imagem.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Launcher para tirar foto com a câmara
    private final ActivityResultLauncher<Intent> tirarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Se a foto foi tirada com sucesso (RESULT_OK) e há dados...
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        // A imagem da câmara costuma vir como "data" no Bundle (thumbnail)
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            imgContador.setImageBitmap(imageBitmap);
                            imagemAtualBitmap = imageBitmap; // armazena o Bitmap para guardar mais tarde

                            // Tenta fazer o OCR para preencher o campo de leitura automaticamente
                            reconhecerTextoDaImagemEPreencher(imageBitmap);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leituras_mensais);
        // Configura a barra de navegação inferior (herdado de BaseActivity)
        setupBottomNav(R.id.nav_leituras);
        dbHelper = new DBHelper(this); // Inicializa a conexão com a base de dados

        // Configurar a label visibility para mostrar todos os labels na Bottom Nav
        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        }
        // Obtém o ID e nome da casa selecionada globalmente
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        ligarViews();
        configurarEventos();
    }

    // Método para fazer o 'findViewById' e inicializar as views
    private void ligarViews() {
        tvNomeCasaLeituras = findViewById(R.id.tvNomeCasaLeituras);
        tvLeituraAnterior = findViewById(R.id.tvLeituraAnterior);
        etNovaLeitura = findViewById(R.id.etNovaLeitura);
        btnEscolherImagem = findViewById(R.id.btnEscolherImagem);
        imgContador = findViewById(R.id.imgContador);
        btnCalcular = findViewById(R.id.btnCalcular);
        tvResultado = findViewById(R.id.tvResultado);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnTirarFoto = findViewById(R.id.btnTirarFoto);
        btnVerHistorico = findViewById(R.id.btnVerHistorico);

        // Mostrar o nome da casa no topo
        tvNomeCasaLeituras.setText(casaNomeAtual);

        // Carrega e exibe a última leitura registrada para esta casa
        double leituraAnterior = dbHelper.obterUltimaLeituraOuDefaultPorCasa(casaIdAtual, 0);
        if (leituraAnterior > 0) {
            tvLeituraAnterior.setText("Leitura anterior do contador: " + leituraAnterior + " kWh");
        } else {
            tvLeituraAnterior.setText("Ainda não existem leituras anteriores.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Atualiza a casa selecionada (caso o utilizador tenha mudado noutra Activity)
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        if (tvNomeCasaLeituras != null) {
            tvNomeCasaLeituras.setText(casaNomeAtual);
        }

        // Atualiza a leitura anterior exibida, se a casa tiver mudado
        if (tvLeituraAnterior != null) {
            double leituraAnterior = dbHelper.obterUltimaLeituraOuDefaultPorCasa(casaIdAtual, 0);
            if (leituraAnterior > 0) {
                tvLeituraAnterior.setText("Leitura anterior do contador: " + leituraAnterior + " kWh");
            } else {
                tvLeituraAnterior.setText("Ainda não existem leituras anteriores.");
            }
        }

        // Garante que a Bottom Nav está configurada corretamente (após um retorno)
        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.setSelectedItemId(R.id.nav_leituras); // Seleciona o item atual
        }
    }

    // Configura os listeners de clique para todos os botões
    private void configurarEventos() {
        // Lança o intent para escolher imagem da galeria
        btnEscolherImagem.setOnClickListener(v -> escolherImagemLauncher.launch("image/*"));
        // Lógica de cálculo do consumo
        btnCalcular.setOnClickListener(v -> calcularConsumo());
        // Lógica para salvar a leitura e a imagem
        btnGuardar.setOnClickListener(v -> guardarLeituraComImagem());
        // Lança a câmara
        btnTirarFoto.setOnClickListener(v -> abrirCamera());
        // Abre o histórico de leituras (nova Activity)
        btnVerHistorico.setOnClickListener(v -> {
            Intent intent = new Intent(LeiturasMensais.this, ResumoLeituras.class);
            startActivity(intent);
        });
    }

    // Lógica principal para calcular o consumo do período e analisar anomalias
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

        // Validações
        if (leituraAtual < 0) {
            Toast.makeText(this, "A leitura não pode ser negativa.", Toast.LENGTH_SHORT).show();
            return;
        }

        double leituraAnterior = dbHelper.obterUltimaLeituraOuDefaultPorCasa(casaIdAtual, 0);
        if (leituraAnterior > 0 && leituraAtual < leituraAnterior) {
            Toast.makeText(this, "A nova leitura não pode ser inferior à leitura anterior do contador.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cálculo base do consumo
        double consumoPeriodo = leituraAnterior > 0 ? leituraAtual - leituraAnterior : 0;
        if (leituraAnterior > 0) {
            tvResultado.setText(String.format("Consumo desde a leitura anterior: %.1f kWh", consumoPeriodo));
        } else {
            tvResultado.setText(String.format("Leitura registada: %.1f kWh. Esta é a primeira leitura.", leituraAtual));
        }

        // Início da Análise de Consumo
        int totalLeituras = dbHelper.contarLeiturasPorCasa(casaIdAtual);
        if (totalLeituras < 2) {
            Toast.makeText(this, "Serão necessárias pelo menos 2 leituras para análise.", Toast.LENGTH_LONG).show();
            return;
        }

        // Calcula a média dos consumos dos últimos 6 períodos para comparação
        double mediaConsumos = dbHelper.calcularMediaConsumosPorCasa(6, casaIdAtual);
        if (mediaConsumos <= 0 || consumoPeriodo <= 0) {
            Toast.makeText(this, "Leitura registada. Serão necessárias mais leituras para analisar anomalias.", Toast.LENGTH_LONG).show();
            return;
        }

        // Cálculo da percentagem de diferença em relação à média
        double percentagemAumento = ((consumoPeriodo - mediaConsumos) / mediaConsumos) * 100.0;

        // Feedback ao utilizador baseado na percentagem de desvio
        if (percentagemAumento > DBHelper.LIMITE_PERCENTUAL_SUP) {
            // Consumo acima do limite (possível anomalia)
            Toast.makeText(this,
                    String.format("Atenção: o consumo deste período (%.1f kWh) está %.1f%% acima da média (%.1f kWh).",
                            consumoPeriodo, percentagemAumento, mediaConsumos),
                    Toast.LENGTH_LONG).show();
        } else if (percentagemAumento < DBHelper.LIMITE_PERCENTUAL_INF) {
            // Consumo abaixo do limite
            Toast.makeText(this,
                    String.format("Bom trabalho! O consumo deste período (%.1f kWh) está %.1f%% abaixo da média (%.1f kWh).",
                            consumoPeriodo, Math.abs(percentagemAumento), mediaConsumos),
                    Toast.LENGTH_LONG).show();
        } else {
            // Consumo normal
            Toast.makeText(this,
                    String.format("Consumo dentro de valores normais. Este período: %.1f kWh, média: %.1f kWh.",
                            consumoPeriodo, mediaConsumos),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Lógica para guardar a leitura e, opcionalmente, a imagem associada
    private void guardarLeituraComImagem() {

        // impedir guardar se não houver casa associada
        if (casaIdAtual <= 0) {
            Toast.makeText(this,
                    "Tem de registar uma casa antes de guardar uma leitura.",
                    Toast.LENGTH_LONG).show();
            return;
        }

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

        double leituraAnterior = dbHelper.obterUltimaLeituraOuDefaultPorCasa(casaIdAtual, 0);
        // Revalidação da regra: nova leitura não pode ser menor que a anterior
        if (leituraAnterior > 0 && leituraValor < leituraAnterior) {
            Toast.makeText(this, "A nova leitura não pode ser inferior à leitura anterior.", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagemPath = null;
        // Se houver um Bitmap capturado/selecionado, guardamos no armazenamento interno
        if (imagemAtualBitmap != null) {
            imagemPath = guardarImagemInterna(imagemAtualBitmap);
        }

        // Pega a data atual no formato YYYY-MM-DD
        String dataHoje = java.time.LocalDate.now().toString();
        // Insere o registro na base de dados, obtendo o ID gerado
        long id = dbHelper.inserirLeituraComFotoPorCasa(casaIdAtual, dataHoje, leituraValor, imagemPath);


        if (id > 0) {
            // Atualiza o texto da leitura anterior no ecrã (refetch da BD)
            double novaLeituraAnterior = dbHelper.obterUltimaLeituraOuDefaultPorCasa(casaIdAtual, 0);
            if (novaLeituraAnterior > 0) {
                tvLeituraAnterior.setText("Leitura anterior do contador: " + novaLeituraAnterior + " kWh");
            } else {
                tvLeituraAnterior.setText("Ainda não existem leituras anteriores.");
            }

            // Após guardar, tenta obter a análise (consumo vs média) diretamente do método da BD
            try (Cursor analise = dbHelper.obterAnaliseConsumoPorCasa(id, casaIdAtual)) {
                if (analise != null && analise.moveToFirst()) {
                    // Extrai os dados de análise do Cursor e mostra um Toast detalhado
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

            // Limpeza da interface e variáveis após sucesso
            etNovaLeitura.setText("");
            tvResultado.setText("");
            imgContador.setImageResource(android.R.drawable.ic_menu_camera); // Reset da imagem
            imagemAtualBitmap = null;
            imagemSelecionadaUri = null;
        } else {
            Toast.makeText(this, "Erro ao guardar leitura com imagem.", Toast.LENGTH_SHORT).show();
        }
    }

    // Salva o Bitmap da imagem no armazenamento interno da aplicação
    private String guardarImagemInterna(Bitmap bitmap) {
        if (bitmap == null) return null;

        // Gera um nome de ficheiro único
        String fileName = "contador_" + System.currentTimeMillis() + ".png";

        try {
            // Abre um FileOutputStream no modo privado (apenas o app tem acesso)
            FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            // Comprime o Bitmap para PNG e escreve no ficheiro
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return fileName; // Retorna o nome do ficheiro (path) para guardar na BD
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao guardar imagem.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Prepara e lança a Intent para abrir a aplicação da câmara
    private void abrirCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Verifica se existe alguma app que consiga lidar com a Intent (para evitar crash)
        if (intent.resolveActivity(getPackageManager()) != null) {
            tirarFotoLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Não foi possível abrir a câmara.", Toast.LENGTH_SHORT).show();
        }
    }


    // Método auxiliar para carregar imagens da galeria de forma eficiente, reduzindo o tamanho
    // para caber na memória sem OutOfMemoryError.
    private Bitmap carregarImagemReduzida(Uri uri) throws IOException {
        java.io.InputStream input = getContentResolver().openInputStream(uri);

        // Apenas ler dimensões (sem carregar a imagem na memória)
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true; // Flag para ler apenas info, não a imagem
        android.graphics.BitmapFactory.decodeStream(input, null, options);
        input.close();

        // Calcular fator de redução (inSampleSize) para caber em ~1024x1024
        int maxSize = 1024;
        int scale = 1;
        // Divide o tamanho por 2 sucessivamente até ficar abaixo do máximo
        while ((options.outWidth / scale) / 2 >= maxSize || (options.outHeight / scale) / 2 >= maxSize) {
            scale *= 2;
        }

        // Carregar imagem real com a redução aplicada
        options.inJustDecodeBounds = false; // Agora queremos ler a imagem
        options.inSampleSize = scale; // Aplica o fator de redução calculado

        input = getContentResolver().openInputStream(uri); // Reabrir stream (o anterior já foi fechado)
        Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input, null, options);
        input.close();

        return bitmap;
    }

    // ocr da imagem para preencher automaticamente a leitura

    // Prepara a imagem e chama o serviço de reconhecimento de texto do ML Kit
    private void reconhecerTextoDaImagemEPreencher(Bitmap bitmap) {
        if (bitmap == null || etNovaLeitura == null) return;

        try {
            // Cria um objeto InputImage a partir do Bitmap (padrão ML Kit)
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // Obtém o cliente de reconhecimento de texto (para latim, que inclui números)
            com.google.mlkit.vision.text.TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            // Processa a imagem de forma assíncrona
            recognizer.process(image)
                    .addOnSuccessListener(this::tratarResultadoOCR) // Chama método se sucesso
                    .addOnFailureListener(e -> {
                        // Se falhar (ex: imagem muito má), ignora e não preenche
                        e.printStackTrace();
                        // Nota: não fazemos Toast aqui para não incomodar o utilizador se for falha de qualidade
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Trata o resultado de OCR: procura o primeiro número com regex e coloca no EditText
    private void tratarResultadoOCR(Text result) {
        if (result == null) return;

        String textoCompleto = result.getText();
        if (textoCompleto == null || textoCompleto.isEmpty()) return;

        // Regex para apanhar um número (inteiro ou decimal com '.' ou ',')
        // \\d+ -> um ou mais dígitos
        // (?:[\\.,]\\d+)? -> grupo opcional: um '.' ou ',' seguido de um ou mais dígitos
        Pattern p = Pattern.compile("\\d+(?:[\\.,]\\d+)?");
        Matcher m = p.matcher(textoCompleto);

        if (m.find()) {
            // Pega o primeiro número que a regex encontra
            String numero = m.group(0);
            // Normaliza vírgula para ponto (se o OCR usou vírgula), pois Double.parseDouble precisa de ponto
            numero = numero.replace(',', '.');

            // Preenche o campo de leitura
            etNovaLeitura.setText(numero);
            etNovaLeitura.setSelection(numero.length()); // Coloca o cursor no fim
            Toast.makeText(this, "Leitura sugerida a partir da imagem.", Toast.LENGTH_SHORT).show();
        }
    }
}