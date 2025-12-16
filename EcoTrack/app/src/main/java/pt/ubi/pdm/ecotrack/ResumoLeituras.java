package pt.ubi.pdm.ecotrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

// Esta Activity mostra o histórico de leituras e permite a eliminação.
public class ResumoLeituras extends BaseActivity {

    private DBHelper dbHelper;
    private TextView tvNomeCasaResumo, tvConsumoAtual, tvDataAtual;
    private TableLayout tabelaHistorico; // Onde as leituras serão listadas
    private Button btnVoltar;

    // Multi-casa: ID e nome da casa atualmente selecionada
    private int casaIdAtual;
    private String casaNomeAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_leituras);

        // Configura a barra de navegação inferior
        setupBottomNav(R.id.nav_leituras);

        // Garante que todas as etiquetas da barra inferior estão visíveis
        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        }

        dbHelper = new DBHelper(this);

        // Obtém o contexto da casa selecionada
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        // Associa as variáveis às Views do layout
        tvNomeCasaResumo = findViewById(R.id.tvNomeCasaResumo);
        tvConsumoAtual = findViewById(R.id.tvConsumoResumo);
        tvDataAtual = findViewById(R.id.tvDataResumo);
        tabelaHistorico = findViewById(R.id.tabelaHistorico);
        btnVoltar = findViewById(R.id.btnVoltarResumo);

        // Define o nome da casa no cabeçalho
        tvNomeCasaResumo.setText(casaNomeAtual);

        carregarEcraCompleto();

        // Configura o botão Voltar
        btnVoltar.setOnClickListener(v -> {
            // Volta para LeiturasMensais e limpa a pilha de Activities
            Intent intent = new Intent(ResumoLeituras.this, LeiturasMensais.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Atualiza o ID e nome da casa, caso tenha havido mudança
        casaIdAtual = CasaSelecionada.getInstance().getCasaId();
        casaNomeAtual = CasaSelecionada.getInstance().getCasaNome();

        if (tvNomeCasaResumo != null) {
            tvNomeCasaResumo.setText(casaNomeAtual);
        }

        // Garante que o item "Leituras" continua selecionado na barra inferior
        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.setSelectedItemId(R.id.nav_leituras);
        }

        carregarEcraCompleto(); // Recarrega os dados do histórico
    }

    // Função que combina o carregamento da última leitura e do histórico completo
    private void carregarEcraCompleto() {
        carregarDadosUltimaLeitura();
        carregarHistoricoComImagens();
    }

    // Obtém e exibe o valor da última leitura registada
    private void carregarDadosUltimaLeitura() {
        double ultima = dbHelper.obterUltimaLeituraOuDefaultPorCasa(casaIdAtual, 0);
        if (ultima > 0) {
            tvConsumoAtual.setText(String.format("%.1f kWh", ultima));
            tvDataAtual.setText("Última leitura registada");
        } else {
            tvConsumoAtual.setText("---");
            tvDataAtual.setText("Sem dados");
        }
    }

    // Carrega todas as leituras da base de dados e gera as linhas na TableLayout
    private void carregarHistoricoComImagens() {
        tabelaHistorico.removeAllViews(); // Limpa todas as linhas existentes

        try (Cursor cursor = dbHelper.obterLeiturasPorCasa(casaIdAtual)) {
            // Verifica se há registos
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Extrai os dados de cada coluna do registo atual
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_ID));
                    String data = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_DATA));
                    double valor = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_VALOR));
                    String caminhoImagem = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_IMAGEM_PATH));

                    // Cria e adiciona a linha à tabela
                    criarLinhaTabela(id, data, valor, caminhoImagem);
                } while (cursor.moveToNext());
            } else {
                adicionarLinhaVazia("Histórico vazio.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            adicionarLinhaVazia("Erro ao carregar histórico.");
        }
    }

    // Adiciona uma linha simples à tabela com uma mensagem de status
    private void adicionarLinhaVazia(String mensagem) {
        TableRow row = new TableRow(this);
        TextView tv = new TextView(this);
        tv.setText(mensagem);
        tv.setPadding(16, 16, 16, 16);
        row.addView(tv);
        tabelaHistorico.addView(row);
    }

    // Cria uma nova linha (TableRow) com a data, imagem, valor e botão de apagar
    private void criarLinhaTabela(long id, String data, double valor, String pathImagem) {
        TableRow row = new TableRow(this);
        row.setPadding(0, 20, 0, 20);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(android.R.drawable.list_selector_background);

        // Coluna 1: Data
        TextView tvData = new TextView(this);
        tvData.setText(data);
        tvData.setTextSize(14);
        tvData.setTextColor(Color.DKGRAY);
        tvData.setPadding(8, 0, 8, 0);
        tvData.setWidth(250);

        // Coluna 2: Imagem
        ImageView imgView = new ImageView(this);
        TableRow.LayoutParams imgParams = new TableRow.LayoutParams(120, 120);
        imgView.setLayoutParams(imgParams);
        imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Tenta carregar a imagem a partir do caminho guardado
        Bitmap bmp = carregarBitmapAPartirDoPath(pathImagem);
        if (bmp != null) {
            imgView.setImageBitmap(bmp);
        } else {
            // Se falhar, mostra um ícone genérico de câmara
            imgView.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // Coluna 3: Valor da Leitura
        TextView tvValor = new TextView(this);
        tvValor.setText(String.format("%.1f kWh", valor));
        tvValor.setTextSize(14);
        tvValor.setTextColor(Color.BLACK);
        tvValor.setGravity(Gravity.CENTER);
        tvValor.setWidth(150);

        // Coluna 4: Botão Apagar
        ImageButton btnApagar = new ImageButton(this);
        btnApagar.setImageResource(android.R.drawable.ic_menu_delete);
        btnApagar.setBackgroundColor(Color.TRANSPARENT);
        btnApagar.setColorFilter(Color.RED);
        btnApagar.setPadding(8, 8, 8, 8);

        // Lógica de Confirmação e Eliminação
        btnApagar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Apagar Leitura")
                    .setMessage("Tem a certeza que quer eliminar este registo?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        // Apaga o registo da base de dados
                        dbHelper.apagarLeituraPorCasa(id, casaIdAtual);

                        // Tenta apagar o ficheiro da imagem do armazenamento interno
                        if (pathImagem != null && !pathImagem.isEmpty()) {
                            try {
                                File f = new File(pathImagem);
                                if (!f.exists()) {
                                    // Tenta encontrar o ficheiro no diretório interno, caso seja um nome relativo
                                    f = new File(getFilesDir(), pathImagem);
                                }
                                if (f.exists()) f.delete(); // Apaga o ficheiro
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(this, "Registo apagado.", Toast.LENGTH_SHORT).show();
                        carregarEcraCompleto(); // Atualiza a tabela após eliminação
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });

        // Adiciona as colunas à linha
        row.addView(tvData);
        row.addView(imgView);
        row.addView(tvValor);
        row.addView(btnApagar);

        // Adiciona a linha à tabela principal
        tabelaHistorico.addView(row);
    }

    // Métodos auxiliares para carregar imagens e evitar OutOfMemoryError

    // Tenta carregar um Bitmap (imagem) a partir de um caminho/URI, verificando múltiplos locais
    private Bitmap carregarBitmapAPartirDoPath(String path) {
        if (path == null || path.isEmpty()) return null;

        try {
            // Se o caminho for uma Content URI ou File URI
            if (path.startsWith("content://") || path.startsWith("file://")) {
                try (InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(path))) {
                    if (is == null) return null;
                    return decodeSampledBitmapFromStream(is, 120, 120);
                }
            }

            // Tentar como caminho absoluto
            File f = new File(path);
            if (f.exists()) {
                return decodeSampledBitmapFromFile(f.getAbsolutePath(), 120, 120);
            }

            // Tentar no armazenamento interno (usando o nome do ficheiro)
            File f2 = new File(getFilesDir(), path);
            if (f2.exists()) {
                return decodeSampledBitmapFromFile(f2.getAbsolutePath(), 120, 120);
            }

            // Tentar interpretar o path como URI (última tentativa)
            try {
                android.net.Uri uri = android.net.Uri.parse(path);
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is == null) return null;
                    return decodeSampledBitmapFromStream(is, 120, 120);
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            Log.e("ResumoLeituras", "Erro ao carregar imagem: " + path, e);
        }
        return null;
    }

    // Decodifica o Bitmap a partir de um ficheiro, aplicando redução de qualidade (sampling)
    private Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // Lê apenas as dimensões
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false; // Decodifica o bitmap com a redução
        return BitmapFactory.decodeFile(filePath, options);
    }

    // Decodifica o Bitmap a partir de um Stream (ex: ContentResolver), aplicando redução
    private Bitmap decodeSampledBitmapFromStream(InputStream is, int reqWidth, int reqHeight) throws IOException {
        // Primeiro, lemos o stream para um array de bytes
        byte[] data = readAllBytes(is);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    // Calcula o fator de redução (inSampleSize) necessário para redimensionar a imagem
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calcula o maior inSampleSize que é potência de 2 e mantém as dimensões maiores que as requeridas
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // Função utilitária para ler todo o conteúdo de um InputStream para um array de bytes
    private byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}