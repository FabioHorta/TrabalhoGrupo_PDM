package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// IMPORTANTE:
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaracterizacaoCasa extends AppCompatActivity {

    private EditText etNomeCasa;
    private MaterialAutoCompleteTextView acDistrito, acCidade, acFreguesia;
    private EditText etMorada, etCodPostal;

    // Layouts clicáveis (Custom Buttons)
    private LinearLayout btnApt, btnMoradia;
    private ImageView iconApt, iconMoradia;
    private TextView txtApt, txtMoradia;

    // Material Buttons (Estilo Branco/Verde)
    private MaterialButton btnPerm, btnSazonal;
    private Button btnGuardar;
    private MaterialButton[] btnPessoas = new MaterialButton[5];
    private MaterialButton[] btnAnos = new MaterialButton[4];

    private String selTipo = "", selUso = "", selAno = "";
    private int selPessoas = 0;

    private List<String> listaDistritos = new ArrayList<>();
    private Map<String, List<String>> mapDistritoConcelhos = new HashMap<>();
    private Map<String, List<String>> mapConcelhoFreguesias = new HashMap<>();

    DBHelper dbHelper;
    String userEmail;
    int casaId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caracterizacao_casa);

        dbHelper = new DBHelper(this);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }
        casaId = getIntent().getIntExtra("casa_id", -1);

        carregarDadosPortugal();
        initViews();
        configurarDropdowns();

        if (casaId != -1) carregarDadosExistentes(casaId);
    }

    private void initViews() {
        etNomeCasa = findViewById(R.id.etNomeCasa);
        acDistrito = findViewById(R.id.autoCompleteDistrito);
        acCidade = findViewById(R.id.autoCompleteCidade);
        acFreguesia = findViewById(R.id.autoCompleteFreguesia);
        etMorada = findViewById(R.id.etMorada);
        etCodPostal = findViewById(R.id.etCodPostal);

        btnApt = findViewById(R.id.btnApartamento);
        btnMoradia = findViewById(R.id.btnMoradia);
        iconApt = findViewById(R.id.iconApt); iconMoradia = findViewById(R.id.iconMoradia);
        txtApt = findViewById(R.id.txtApt); txtMoradia = findViewById(R.id.txtMoradia);

        btnApt.setOnClickListener(v -> selectCustomTipo("Apartamento"));
        btnMoradia.setOnClickListener(v -> selectCustomTipo("Moradia"));

        // Botões de Uso
        btnPerm = findViewById(R.id.btnPermanente);
        btnSazonal = findViewById(R.id.btnSazonal);
        btnPerm.setOnClickListener(v -> selectUso("Permanente", btnPerm, btnSazonal));
        btnSazonal.setOnClickListener(v -> selectUso("Sazonal", btnSazonal, btnPerm));

        // Botões de Pessoas
        int[] idsPessoas = {R.id.btnP1, R.id.btnP2, R.id.btnP3, R.id.btnP4, R.id.btnP5};
        for (int i = 0; i < 5; i++) {
            btnPessoas[i] = findViewById(idsPessoas[i]);
            int q=i+1;
            btnPessoas[i].setOnClickListener(v -> selectPessoas(q));
        }

        // Botões de Ano
        int[] idsAnos = {R.id.btnAno1, R.id.btnAno2, R.id.btnAno3, R.id.btnAno4};
        for (int i = 0; i < 4; i++) {
            btnAnos[i] = findViewById(idsAnos[i]);
            btnAnos[i].setOnClickListener(v -> selectAno((MaterialButton) v));
        }

        btnGuardar = findViewById(R.id.btnGuardar);
        btnGuardar.setOnClickListener(v -> guardarEAvancar());
    }

    // --- VISUAL SELEÇÃO ---
    private void selectCustomTipo(String tipo) {
        selTipo = tipo;
        boolean isApt = tipo.equals("Apartamento");

        btnApt.setBackgroundResource(isApt ? R.drawable.bg_option_selected : R.drawable.bg_option_unselected);
        iconApt.setColorFilter(isApt ? Color.parseColor("#4CAF50") : Color.parseColor("#555555"));
        txtApt.setTextColor(isApt ? Color.parseColor("#4CAF50") : Color.parseColor("#555555"));

        btnMoradia.setBackgroundResource(!isApt ? R.drawable.bg_option_selected : R.drawable.bg_option_unselected);
        iconMoradia.setColorFilter(!isApt ? Color.parseColor("#4CAF50") : Color.parseColor("#555555"));
        txtMoradia.setTextColor(!isApt ? Color.parseColor("#4CAF50") : Color.parseColor("#555555"));
    }

    private void selectUso(String v, MaterialButton s, MaterialButton o) { selUso = v; highlight(s); reset(o); }
    private void selectPessoas(int q) { selPessoas = q; for(MaterialButton b:btnPessoas) reset(b); highlight(btnPessoas[q-1]); }
    private void selectAno(MaterialButton s) { selAno = s.getText().toString(); for(MaterialButton b:btnAnos) reset(b); highlight(s); }

    // --- MUDAR A COR DO BOTÃO ---
    private void highlight(MaterialButton b) {
        b.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Verde
        b.setStrokeWidth(6);
        b.setTextColor(Color.parseColor("#4CAF50"));
    }

    private void reset(MaterialButton b) {
        b.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#CCCCCC"))); // Cinza
        b.setStrokeWidth(2);
        b.setTextColor(Color.parseColor("#1E4D42"));
    }

    private void guardarEAvancar() {
        String nome = etNomeCasa.getText().toString();
        if (nome.isEmpty() || selTipo.isEmpty() || selUso.isEmpty() || selPessoas == 0) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show(); return;
        }

        int novoId = dbHelper.guardarCasaCompleta(casaId, userEmail, nome, selTipo, selUso, selPessoas, selAno, etMorada.getText().toString(), acDistrito.getText().toString(), acCidade.getText().toString(), acFreguesia.getText().toString(), etCodPostal.getText().toString());

        if (novoId != -1) {
            Intent i = new Intent(CaracterizacaoCasa.this, Eletrodomesticos.class);
            i.putExtra("casa_id", novoId);
            startActivity(i);
            finish();
        } else {
            Toast.makeText(this, "Erro ao guardar.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- JSON ---
    private void carregarDadosPortugal() { try { InputStream is = getAssets().open("portugal_db.json"); int size = is.available(); byte[] buffer = new byte[size]; is.read(buffer); is.close(); String json = new String(buffer, "UTF-8"); JSONArray jsonArray = new JSONArray(json); String distritoAtual = "", concelhoAtual = ""; for (int i = 0; i < jsonArray.length(); i++) { JSONObject obj = jsonArray.getJSONObject(i); int level = obj.optInt("level", 0); String nome = obj.getString("name"); if (level == 1) { distritoAtual = nome; listaDistritos.add(nome); mapDistritoConcelhos.put(distritoAtual, new ArrayList<>()); } else if (level == 2) { concelhoAtual = nome; if (!distritoAtual.isEmpty()) mapDistritoConcelhos.get(distritoAtual).add(nome); mapConcelhoFreguesias.put(distritoAtual + "_" + concelhoAtual, new ArrayList<>()); } else if (level == 3) { if (!distritoAtual.isEmpty() && !concelhoAtual.isEmpty()) mapConcelhoFreguesias.get(distritoAtual + "_" + concelhoAtual).add(nome); } } Collections.sort(listaDistritos); } catch (Exception e) {} }

    private void configurarDropdowns() {
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, listaDistritos);
        acDistrito.setAdapter(adp);
        acDistrito.setOnItemClickListener((p, v, pos, id) -> { String d = adp.getItem(pos); acCidade.setText("", false); acFreguesia.setText("", false); List<String> l = mapDistritoConcelhos.get(d); if(l!=null){Collections.sort(l); acCidade.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, l));} });
        acCidade.setOnItemClickListener((p, v, pos, id) -> { String d = acDistrito.getText().toString(); String c = p.getItemAtPosition(pos).toString(); acFreguesia.setText("", false); List<String> l = mapConcelhoFreguesias.get(d+"_"+c); if(l!=null){Collections.sort(l); acFreguesia.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, l));} });
    }

    private void carregarDadosExistentes(int id) {
        Cursor c = dbHelper.obterCasaPorId(id);
        if (c != null && c.moveToFirst()) {
            etNomeCasa.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_NOME)));
            etMorada.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_MORADA)));
            etCodPostal.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_COD_POSTAL)));

            acDistrito.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_DISTRITO)), false);
            acCidade.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_CONCELHO)), false);
            acFreguesia.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_FREGUESIA)), false);

            String tipo = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_TIPO));
            if ("Apartamento".equals(tipo)) selectCustomTipo("Apartamento"); else selectCustomTipo("Moradia");

            String uso = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_USO));
            if ("Permanente".equals(uso)) btnPerm.performClick(); else btnSazonal.performClick();

            int p = c.getInt(c.getColumnIndexOrThrow(DBHelper.C_CASA_PESSOAS));
            if (p >= 1 && p <= 5) selectPessoas(p);

            String a = c.getString(c.getColumnIndexOrThrow(DBHelper.C_CASA_ANO));
            selAno = a; for(MaterialButton b : btnAnos) if(b.getText().toString().equals(a)) highlight(b);
        }
        if (c != null) c.close();
    }
}