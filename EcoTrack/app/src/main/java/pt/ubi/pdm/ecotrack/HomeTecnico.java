package pt.ubi.pdm.ecotrack;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeTecnico extends BaseActivityTecnico {

    private TextView tvProximaTitulo, tvProximaNome, tvProximaDataHora, tvProximaMoradaOuEstado;
    private TextView tvResumoAssistencias, tvResumoMensagens;
    private DBHelper db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_tecnico);

        db = new DBHelper(this);
        mAuth = FirebaseAuth.getInstance();

        // Cabeçalho
        ImageView imgAvatar = findViewById(R.id.imgAvatarTecnico);
        TextView tvBemVindo = findViewById(R.id.tvBemVindoTecnico);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            tvBemVindo.setText("Bem-vindo, " + user.getEmail());
        }

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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            tvProximaTitulo.setText("Próxima visita");
            tvProximaNome.setText("Sessão não autenticada");
            tvProximaDataHora.setText("");
            tvProximaMoradaOuEstado.setText("");
            return;
        }

        String tecnicoEmail = user.getEmail();

        Cursor c = db.listarAssistenciasDoTecnico(tecnicoEmail);
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            tvResumoAssistencias.setText("Assistências agendadas: -");
            tvResumoMensagens.setText("Mensagens recebidas: -");
            return;
        }

        String tecnicoEmail = user.getEmail();

        Cursor cAssist = db.listarAssistenciasDoTecnico(tecnicoEmail);
        int totalAssist = (cAssist != null) ? cAssist.getCount() : 0;
        if (cAssist != null) cAssist.close();

        // Para já, mensagens continuam globais (todas)
        Cursor cMsg = db.listarMensagens();
        int totalMsg = (cMsg != null) ? cMsg.getCount() : 0;
        if (cMsg != null) cMsg.close();

        tvResumoAssistencias.setText("Assistências agendadas: " + totalAssist);
        tvResumoMensagens.setText("Mensagens recebidas: " + totalMsg);
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
