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

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResumoLeituras extends AppCompatActivity {

    private DBHelper dbHelper;
    private TextView tvConsumoAtual, tvDataAtual;
    private TableLayout tabelaHistorico;
    private Button btnVoltar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_leituras);

        dbHelper = new DBHelper(this);

        tvConsumoAtual = findViewById(R.id.tvConsumoResumo);
        tvDataAtual = findViewById(R.id.tvDataResumo);
        tabelaHistorico = findViewById(R.id.tabelaHistorico);
        btnVoltar = findViewById(R.id.btnVoltarResumo);

        carregarEcraCompleto();

        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(ResumoLeituras.this, LeiturasMensais.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarEcraCompleto(); // atualiza sempre que volta para esta tela
    }

    private void carregarEcraCompleto() {
        carregarDadosUltimaLeitura();
        carregarHistoricoComImagens();
    }

    private void carregarDadosUltimaLeitura() {
        double ultima = dbHelper.obterUltimaLeituraOuDefault(0);
        if (ultima > 0) {
            tvConsumoAtual.setText(String.format("%.1f kWh", ultima));
            tvDataAtual.setText("Última leitura registada");
        } else {
            tvConsumoAtual.setText("---");
            tvDataAtual.setText("Sem dados");
        }
    }

    private void carregarHistoricoComImagens() {
        tabelaHistorico.removeAllViews();

        try (Cursor cursor = dbHelper.obterLeituras()) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_ID));
                    String data = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_DATA));
                    double valor = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_VALOR));
                    String caminhoImagem = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.C_LEITURA_IMAGEM_PATH));

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

    private void adicionarLinhaVazia(String mensagem) {
        TableRow row = new TableRow(this);
        TextView tv = new TextView(this);
        tv.setText(mensagem);
        tv.setPadding(16, 16, 16, 16);
        row.addView(tv);
        tabelaHistorico.addView(row);
    }

    private void criarLinhaTabela(long id, String data, double valor, String pathImagem) {
        TableRow row = new TableRow(this);
        row.setPadding(0, 20, 0, 20);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(android.R.drawable.list_selector_background);

        // Coluna Data
        TextView tvData = new TextView(this);
        tvData.setText(data);
        tvData.setTextSize(14);
        tvData.setTextColor(Color.DKGRAY);
        tvData.setPadding(8, 0, 8, 0);
        tvData.setWidth(250);

        // Coluna Imagem
        ImageView imgView = new ImageView(this);
        TableRow.LayoutParams imgParams = new TableRow.LayoutParams(120, 120);
        imgView.setLayoutParams(imgParams);
        imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgView.setContentDescription("Imagem da leitura de " + data);

        Bitmap bmp = carregarBitmapAPartirDoPath(pathImagem);
        if (bmp != null) {
            imgView.setImageBitmap(bmp);
        } else {
            imgView.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // Coluna Valor
        TextView tvValor = new TextView(this);
        tvValor.setText(String.format("%.1f kWh", valor));
        tvValor.setTextSize(14);
        tvValor.setTextColor(Color.BLACK);
        tvValor.setGravity(Gravity.CENTER);
        tvValor.setWidth(150);

        // Botão Apagar
        ImageButton btnApagar = new ImageButton(this);
        btnApagar.setImageResource(android.R.drawable.ic_menu_delete);
        btnApagar.setBackgroundColor(Color.TRANSPARENT);
        btnApagar.setColorFilter(Color.RED);
        btnApagar.setPadding(8, 8, 8, 8);
        btnApagar.setContentDescription("Eliminar leitura de " + data);

        btnApagar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Apagar Leitura")
                    .setMessage("Tem a certeza que quer eliminar este registo?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        dbHelper.apagarLeitura(id);
                        if (pathImagem != null && !pathImagem.isEmpty()) {
                            try {
                                File f = new File(pathImagem);
                                if (!f.exists()) {
                                    f = new File(getFilesDir(), pathImagem);
                                }
                                if (f.exists()) f.delete();
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(this, "Registo apagado.", Toast.LENGTH_SHORT).show();
                        carregarEcraCompleto(); // atualiza a tabela
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });

        row.addView(tvData);
        row.addView(imgView);
        row.addView(tvValor);
        row.addView(btnApagar);

        tabelaHistorico.addView(row);
    }

    // === Métodos auxiliares para carregar imagens ===

    private Bitmap carregarBitmapAPartirDoPath(String path) {
        if (path == null || path.isEmpty()) return null;

        try {
            // 1) Se for content:// ou file:// -> usar ContentResolver
            if (path.startsWith("content://") || path.startsWith("file://")) {
                try (InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(path))) {
                    if (is == null) return null;
                    return decodeSampledBitmapFromStream(is, 120, 120);
                }
            }

            // 2) Tentar como caminho absoluto
            File f = new File(path);
            if (f.exists()) {
                return decodeSampledBitmapFromFile(f.getAbsolutePath(), 120, 120);
            }

            // 3) Tentar no internal storage (nome relativo)
            File f2 = new File(getFilesDir(), path);
            if (f2.exists()) {
                return decodeSampledBitmapFromFile(f2.getAbsolutePath(), 120, 120);
            }

            // 4) Tentar interpretar path como URI (por precaução)
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

    private Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private Bitmap decodeSampledBitmapFromStream(InputStream is, int reqWidth, int reqHeight) throws IOException {
        byte[] data = readAllBytes(is);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

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