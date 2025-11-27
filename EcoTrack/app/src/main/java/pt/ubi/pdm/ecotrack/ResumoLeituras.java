package pt.ubi.pdm.ecotrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
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

        Cursor cursor = dbHelper.obterLeituras();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Ajustado para a nova tabela "leituras" (constantes do DBHelper)
                int indexId = cursor.getColumnIndex(DBHelper.C_LEITURA_ID);
                int indexData = cursor.getColumnIndex(DBHelper.C_LEITURA_DATA);
                int indexValor = cursor.getColumnIndex(DBHelper.C_LEITURA_VALOR);
                int indexPath = cursor.getColumnIndex(DBHelper.C_LEITURA_IMAGEM_PATH);

                if (indexId != -1 && indexData != -1 && indexValor != -1) {
                    long id = cursor.getLong(indexId);
                    String data = cursor.getString(indexData);
                    double valor = cursor.getDouble(indexValor);
                    String caminhoImagem = (indexPath != -1) ? cursor.getString(indexPath) : "";

                    criarLinhaTabela(id, data, valor, caminhoImagem);
                }

            } while (cursor.moveToNext());
            cursor.close();
        } else {
            TableRow row = new TableRow(this);
            TextView tvVazio = new TextView(this);
            tvVazio.setText("Histórico vazio.");
            tvVazio.setPadding(16, 16, 16, 16);
            row.addView(tvVazio);
            tabelaHistorico.addView(row);
        }
    }

    private void criarLinhaTabela(long id, String data, double valor, String pathImagem) {
        TableRow row = new TableRow(this);
        row.setPadding(0, 20, 0, 20);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(android.R.drawable.list_selector_background);

        TextView tvData = new TextView(this);
        tvData.setText(data);
        tvData.setTextSize(14);
        tvData.setTextColor(Color.DKGRAY);
        tvData.setPadding(8, 0, 0, 0);

        ImageView imgView = new ImageView(this);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(120, 120);
        imgView.setLayoutParams(layoutParams);
        imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (pathImagem != null && !pathImagem.isEmpty()) {
            File imgFile = new File(getFilesDir(), pathImagem);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imgView.setImageBitmap(myBitmap);
            } else {
                imgView.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            imgView.setImageResource(android.R.drawable.ic_menu_camera);
        }

        TextView tvValor = new TextView(this);
        tvValor.setText(String.format("%.1f kWh", valor));
        tvValor.setTextSize(14);
        tvValor.setTextColor(Color.BLACK);
        tvValor.setGravity(Gravity.CENTER);

        ImageButton btnApagar = new ImageButton(this);
        btnApagar.setImageResource(android.R.drawable.ic_menu_delete);
        btnApagar.setBackgroundColor(Color.TRANSPARENT);
        btnApagar.setColorFilter(Color.RED);
        btnApagar.setPadding(8, 8, 8, 8);

        btnApagar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Apagar Leitura")
                    .setMessage("Tem a certeza que quer eliminar este registo?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        dbHelper.apagarLeitura(id);
                        if (pathImagem != null && !pathImagem.isEmpty()) {
                            try {
                                File f = new File(getFilesDir(), pathImagem);
                                if (f.exists()) f.delete();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(this, "Registo apagado.", Toast.LENGTH_SHORT).show();
                        carregarEcraCompleto();
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
}