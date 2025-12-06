package pt.ubi.pdm.ecotrack;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HomeTecnico extends BaseActivityTecnico {

    private TextView tvProximaTitulo, tvProximaNome, tvProximaDataHora, tvProximaMoradaOuEstado;
    private TextView tvResumoAssistencias, tvResumoMensagens;
    private DBHelper db;

    private String emailTecnico;   // <== agora guardamos aqui

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_tecnico);
        SyncUtils.syncTudoAsync(this);
        db = new DBHelper(this);

        // Cabeçalho
        ImageView imgAvatar = findViewById(R.id.imgAvatarTecnico);
        TextView tvBemVindo = findViewById(R.id.tvBemVindoTecnico);

        // 1) Obter email do técnico autenticado a partir das SharedPreferences
        SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
        emailTecnico = sp.getString("user_email", null);

        if (emailTecnico == null) {
            Toast.makeText(this, "Técnico não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2) Confirmar (se quiseres) que é mesmo técnico na BD local
        String tipo = db.obterTipoUtilizadorPorEmail(emailTecnico);
        if (!"tecnico".equalsIgnoreCase(tipo)) {
            Toast.makeText(this, "Esta área é apenas para técnicos.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3) Mostrar nome ou email no cabeçalho
        String nomeMostrar = emailTecnico;
        Cursor cUser = db.obterDadosUtilizadorPorEmail(emailTecnico);
        if (cUser != null) {
            int idxNome = cUser.getColumnIndex(DBHelper.C_USER_NAME);
            if (idxNome >= 0 && cUser.moveToFirst()) {
                String nome = cUser.getString(idxNome);
                if (nome != null && !nome.isEmpty()) {
                    nomeMostrar = nome;
                }
            }
            cUser.close();
        }
        tvBemVindo.setText("Bem-vindo, " + nomeMostrar);

        // Card "Próxima visita"
        tvProximaTitulo = findViewById(R.id.tvProximaVisitaTitulo);
        tvProximaNome = findViewById(R.id.tvProximaVisitaNome);
        tvProximaDataHora = findViewById(R.id.tvProximaVisitaDataHora);
        tvProximaMoradaOuEstado = findViewById(R.id.tvProximaVisitaMoradaEstado);

        // Resumos
        tvResumoAssistencias = findViewById(R.id.tvResumoAssistencias);
        tvResumoMensagens = findViewById(R.id.tvResumoMensagens);

        // ligar a bottom bar (item atual = início técnico)
        setupBottomNavTecnico(R.id.menu_inicio_tecnico);

        carregarProximaVisita();
        carregarResumo();
    }

    private void carregarProximaVisita() {
        // Se por algum motivo o email não estiver definido, evita NPEs
        if (emailTecnico == null) {
            tvProximaTitulo.setText("Próxima visita");
            tvProximaNome.setText("Sessão não autenticada");
            tvProximaDataHora.setText("");
            tvProximaMoradaOuEstado.setText("");
            return;
        }

        Cursor c = db.listarAssistenciasDoTecnico(emailTecnico);
        if (c == null) {
            tvProximaTitulo.setText("Próxima visita");
            tvProximaNome.setText("Sem visitas agendadas");
            tvProximaDataHora.setText("");
            tvProximaMoradaOuEstado.setText("");
            return;
        }

        // escolher a assistência futura mais próxima de "agora"
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();
        java.time.LocalDateTime melhor = null;

        String melhorData = null;
        String melhorHora = null;
        String melhorDesc = null;
        String melhorFeedback = null;

        java.time.format.DateTimeFormatter fmtData =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.time.format.DateTimeFormatter fmtHora =
                java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        while (c.moveToNext()) {
            String data = c.getString(c.getColumnIndexOrThrow("data"));
            String hora = c.getString(c.getColumnIndexOrThrow("hora"));
            String descricao = c.getString(c.getColumnIndexOrThrow("descricao"));
            String feedback = c.getString(c.getColumnIndexOrThrow("feedback"));

            try {
                java.time.LocalDate d = java.time.LocalDate.parse(data, fmtData);
                java.time.LocalTime t = java.time.LocalTime.parse(hora, fmtHora);
                java.time.LocalDateTime dt = java.time.LocalDateTime.of(d, t);

                // ignorar visitas já no passado
                if (dt.isBefore(agora)) continue;

                if (melhor == null || dt.isBefore(melhor)) {
                    melhor = dt;
                    melhorData = data;
                    melhorHora = hora;
                    melhorDesc = descricao;
                    melhorFeedback = feedback;
                }
            } catch (Exception e) {
                // se houver algum registo mal formatado, é ignorado
            }
        }
        c.close();

        tvProximaTitulo.setText("Próxima visita");

        if (melhor != null) {
            tvProximaNome.setText(melhorDesc != null ? melhorDesc : "Assistência agendada");
            tvProximaDataHora.setText(melhorData + " • " + melhorHora);
            tvProximaMoradaOuEstado.setText(
                    (melhorFeedback != null && !melhorFeedback.isEmpty())
                            ? "Estado: " + melhorFeedback
                            : ""
            );
        } else {
            tvProximaNome.setText("Sem visitas agendadas");
            tvProximaDataHora.setText("");
            tvProximaMoradaOuEstado.setText("");
        }
    }

    private void carregarResumo() {
        if (emailTecnico == null) {
            tvResumoAssistencias.setText("Assistências agendadas: -");
            tvResumoMensagens.setText("Clientes com conversa: -");
            return;
        }

        // Assistências deste técnico
        Cursor cAssist = db.listarAssistenciasDoTecnico(emailTecnico);
        int totalAssist = (cAssist != null) ? cAssist.getCount() : 0;
        if (cAssist != null) cAssist.close();

        // Nº de clientes com quem o técnico tem chat (mesma lógica da lista de clientes)
        Cursor cClientes = db.listarClientesDoTecnicoNoChat(emailTecnico);
        int totalClientes = (cClientes != null) ? cClientes.getCount() : 0;
        if (cClientes != null) cClientes.close();

        tvResumoAssistencias.setText("Assistências agendadas: " + totalAssist);
        tvResumoMensagens.setText("Clientes com conversa: " + totalClientes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarProximaVisita();
        carregarResumo();

        if (bottomNavTecnico != null) {
            bottomNavTecnico.setSelectedItemId(R.id.menu_inicio_tecnico);
        }
    }
}
