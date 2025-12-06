package pt.ubi.pdm.ecotrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.AssistenciaSync;
import pt.ubi.pdm.ecotrack.models.LeituraSync;
import pt.ubi.pdm.ecotrack.models.CasaSync;
import pt.ubi.pdm.ecotrack.models.ApplianceSync;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Classe utilitária para:
 *  - Sincronizar dados locais -> servidor (leituras, assistências, casas, appliances)
 *  - Restaurar dados do servidor -> base de dados local quando esta está vazia
 */
public class SyncUtils {

    // =========================================================
    // MÉTODO PRINCIPAL DE SINCRONIZAÇÃO (LOCAL -> SERVIDOR)
    // =========================================================
    /**
     * Chama todos os processos de sync para enviar dados locais
     * para o servidor, se houver ligação à internet.
     */
    public static void syncTudoAsync(Context context) {
        if (!temInternet(context)) {
            return;
        }

        DBHelper db = new DBHelper(context.getApplicationContext());
        ApiService api = ApiClient.getRetrofit().create(ApiService.class);

        // Enviar para o servidor:
        syncLeituras(context, db, api);
        syncAssistencias(context, db, api);
        syncCasas(context, db, api);
        syncAppliances(context, db, api);
    }

    // =========================================================
    // VERIFICAR INTERNET
    // =========================================================
    /**
     * Verifica se há ligação à internet (Wi-Fi, dados móveis ou ethernet).
     */
    private static boolean temInternet(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    // =========================================================
    // SYNC LEITURAS (LOCAL -> SERVIDOR)
    // =========================================================
    /**
     * Lê da BD local todas as leituras com sync_status=0 e envia-as
     * para o servidor. Se o servidor aceitar, marca essas leituras
     * como sincronizadas (sync_status=1).
     */
    private static void syncLeituras(Context context, DBHelper db, ApiService api) {
        Cursor c = db.obterLeiturasPorSincronizar();
        if (c == null) return;

        List<LeituraSync> lista = new ArrayList<>();
        List<Long> idsLocais = new ArrayList<>();

        try {
            if (!c.moveToFirst()) return;

            int idxId      = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_ID);
            int idxCasaId  = c.getColumnIndexOrThrow("casa_id");
            int idxData    = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_DATA);
            int idxValor   = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_VALOR);
            int idxImagem  = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_IMAGEM_PATH);
            int idxPrev    = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_PREV_ID);
            int idxConsumo = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_CONSUMO_PERIODO);
            int idxCreated = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_CREATED_AT_TS);

            do {
                long id = c.getLong(idxId);
                Integer casaId = c.isNull(idxCasaId) ? null : c.getInt(idxCasaId);
                String data = c.getString(idxData);
                double valor = c.getDouble(idxValor);
                String imagemPath = c.getString(idxImagem);
                Long prev = c.isNull(idxPrev) ? null : c.getLong(idxPrev);
                Double consumo = c.isNull(idxConsumo) ? null : c.getDouble(idxConsumo);
                Long created = c.getLong(idxCreated);

                lista.add(new LeituraSync(
                        id,
                        casaId,
                        data,
                        valor,
                        imagemPath,
                        prev,
                        consumo,
                        created
                ));

                idsLocais.add(id);

            } while (c.moveToNext());

        } finally {
            c.close();
        }

        if (lista.isEmpty()) return;

        api.syncLeituras(lista).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Se o servidor recebeu bem, marcamos as leituras como sincronizadas
                if (response.isSuccessful()) {
                    long[] idsArray = new long[idsLocais.size()];
                    for (int i = 0; i < idsLocais.size(); i++) {
                        idsArray[i] = idsLocais.get(i);
                    }
                    db.marcarLeiturasComoSincronizadas(idsArray);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Se falhar, não fazemos nada: tenta na próxima sync
            }
        });
    }

    // =========================================================
    // SYNC ASSISTÊNCIAS (LOCAL -> SERVIDOR)
    // =========================================================
    /**
     * Envia todas as assistências da BD local para o servidor.
     * (Aqui não há controlo de "já sincronizado", envia tudo).
     */
    private static void syncAssistencias(Context context, DBHelper db, ApiService api) {
        Cursor c = db.listarAssistencias();   // todas as assistências
        if (c == null) return;

        List<AssistenciaSync> lista = new ArrayList<>();

        try {
            if (!c.moveToFirst()) return;

            int idxId   = c.getColumnIndexOrThrow("id");
            int idxData = c.getColumnIndexOrThrow("data");
            int idxHora = c.getColumnIndexOrThrow("hora");
            int idxDesc = c.getColumnIndexOrThrow("descricao");
            int idxFeed = c.getColumnIndexOrThrow("feedback");
            int idxTec  = c.getColumnIndexOrThrow("tecnico_email");

            do {
                long id = c.getLong(idxId);
                String data = c.getString(idxData);
                String hora = c.getString(idxHora);
                String desc = c.getString(idxDesc);
                String feedback = c.getString(idxFeed);
                String tecnicoEmail = c.getString(idxTec);

                lista.add(new AssistenciaSync(
                        id,
                        data,
                        hora,
                        desc,
                        feedback,
                        tecnicoEmail
                ));
            } while (c.moveToNext());
        } finally {
            c.close();
        }

        if (lista.isEmpty()) return;

        api.syncAssistencias(lista).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { }

            @Override
            public void onFailure(Call<Void> call, Throwable t) { }
        });
    }

    // =========================================================
    // SYNC CASAS (LOCAL -> SERVIDOR)
    // =========================================================
    /**
     * Lê todas as casas do utilizador autenticado na BD local
     * e envia-as para o servidor (/casas/sync).
     */
    private static void syncCasas(Context context, DBHelper db, ApiService api) {
        SharedPreferences sp = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String email = sp.getString("user_email", null);
        if (email == null) return;

        Cursor c = db.listarCasasDoUtilizador(email);
        if (c == null) return;

        List<CasaSync> lista = new ArrayList<>();

        try {
            if (!c.moveToFirst()) return;

            int idxId        = c.getColumnIndexOrThrow(DBHelper.C_CASA_ID);
            int idxEmail     = c.getColumnIndexOrThrow(DBHelper.C_CASA_USER_EMAIL);
            int idxNome      = c.getColumnIndexOrThrow(DBHelper.C_CASA_NOME);
            int idxTipo      = c.getColumnIndexOrThrow(DBHelper.C_CASA_TIPO);
            int idxUso       = c.getColumnIndexOrThrow(DBHelper.C_CASA_USO);
            int idxPessoas   = c.getColumnIndexOrThrow(DBHelper.C_CASA_PESSOAS);
            int idxAno       = c.getColumnIndexOrThrow(DBHelper.C_CASA_ANO);
            int idxMorada    = c.getColumnIndexOrThrow(DBHelper.C_CASA_MORADA);
            int idxDistrito  = c.getColumnIndexOrThrow(DBHelper.C_CASA_DISTRITO);
            int idxConcelho  = c.getColumnIndexOrThrow(DBHelper.C_CASA_CONCELHO);
            int idxFreguesia = c.getColumnIndexOrThrow(DBHelper.C_CASA_FREGUESIA);
            int idxCP        = c.getColumnIndexOrThrow(DBHelper.C_CASA_COD_POSTAL);

            do {
                CasaSync casa = new CasaSync(
                        c.getInt(idxId),
                        c.getString(idxEmail),
                        c.getString(idxNome),
                        c.getString(idxTipo),
                        c.getString(idxUso),
                        c.getInt(idxPessoas),
                        c.getString(idxAno),
                        c.getString(idxMorada),
                        c.getString(idxDistrito),
                        c.getString(idxConcelho),
                        c.getString(idxFreguesia),
                        c.getString(idxCP)
                );
                lista.add(casa);
            } while (c.moveToNext());
        } finally {
            c.close();
        }

        if (lista.isEmpty()) return;

        api.syncCasas(lista).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { }

            @Override
            public void onFailure(Call<Void> call, Throwable t) { }
        });
    }

    // =========================================================
    // SYNC APPLIANCES (LOCAL -> SERVIDOR)
    // =========================================================
    /**
     * Para cada casa do utilizador, lê os eletrodomésticos da BD local
     * e envia-os para o servidor.
     */
    private static void syncAppliances(Context context, DBHelper db, ApiService api) {
        SharedPreferences sp = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String email = sp.getString("user_email", null);
        if (email == null) return;

        Cursor cCasas = db.listarCasasDoUtilizador(email);
        if (cCasas == null) return;

        List<ApplianceSync> lista = new ArrayList<>();

        try {
            if (!cCasas.moveToFirst()) return;

            int idxCasaId = cCasas.getColumnIndexOrThrow(DBHelper.C_CASA_ID);

            do {
                int casaId = cCasas.getInt(idxCasaId);

                Cursor cApps = db.obterEletrodomesticosDaCasa(casaId);
                if (cApps == null) continue;

                try {
                    if (!cApps.moveToFirst()) continue;

                    int idxNome      = cApps.getColumnIndexOrThrow(DBHelper.C_APP_NOME);
                    int idxCategoria = cApps.getColumnIndexOrThrow(DBHelper.C_APP_CATEGORIA);
                    int idxClasse    = cApps.getColumnIndexOrThrow(DBHelper.C_APP_CLASSE);

                    do {
                        ApplianceSync appSync = new ApplianceSync(
                                casaId,
                                cApps.getString(idxNome),
                                cApps.getString(idxCategoria),
                                cApps.getString(idxClasse)
                        );
                        lista.add(appSync);
                    } while (cApps.moveToNext());
                } finally {
                    cApps.close();
                }

            } while (cCasas.moveToNext());
        } finally {
            cCasas.close();
        }

        if (lista.isEmpty()) return;

        api.syncAppliances(lista).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { }

            @Override
            public void onFailure(Call<Void> call, Throwable t) { }
        });
    }

    // =========================================================
    // RESTAURO COMPLETO (SERVIDOR -> LOCAL) APÓS LOGIN
    // =========================================================
    /**
     * Chamado depois do login com sucesso:
     *  - se a BD local estiver vazia para certos tipos de dados,
     *    vai buscar tudo ao servidor e repõe.
     */
    public static void restaurarTudoSeNecessario(Context context) {
        if (!temInternet(context)) return;

        SharedPreferences sp =
                context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String email = sp.getString("user_email", null);
        if (email == null) return;

        DBHelper db = new DBHelper(context.getApplicationContext());
        ApiService api = ApiClient.getRetrofit().create(ApiService.class);

        restaurarCasasSeNecessario(context, db, api, email);
        restaurarAppliancesSeNecessario(context, db, api, email);
        restaurarAssistenciasSeNecessario(context, db, api);
        restaurarLeiturasSeNecessario(context, db, api, email);
    }

    // =========================================================
    // RESTAURO DE LEITURAS (SERVIDOR -> LOCAL)
    // =========================================================
    /**
     * Se a BD local não tiver nenhuma leitura, vai buscar
     * todas as leituras do utilizador ao servidor e insere-as
     * na BD local, marcadas como já sincronizadas.
     */
    private static void restaurarLeiturasSeNecessario(Context context,
                                                      DBHelper db,
                                                      ApiService api,
                                                      String email) {

        // Ver se já há ALGUMA leitura na BD local
        boolean temLocais = db.existemLeiturasLocais();
        if (temLocais) return;   // já há leituras -> não repõe

        api.getLeiturasByUser(email).enqueue(new Callback<List<LeituraSync>>() {
            @Override
            public void onResponse(Call<List<LeituraSync>> call,
                                   Response<List<LeituraSync>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                for (LeituraSync l : response.body()) {
                    db.inserirLeituraRestaurada(
                            l.casa_id,
                            l.data,
                            l.valor_kwh,
                            l.imagem_path,
                            l.prev_leitura_id,
                            l.consumo_periodo,
                            l.created_at_ts
                    );
                }
            }

            @Override
            public void onFailure(Call<List<LeituraSync>> call, Throwable t) { }
        });
    }




    // =========================================================
    // RESTAURO DE CASAS (SERVIDOR -> LOCAL)
    // =========================================================
    /**
     * Se o utilizador não tiver casas na BD local, vai buscar
     * todas as casas dele ao servidor e insere-as na BD local.
     */
    private static void restaurarCasasSeNecessario(Context context,
                                                   DBHelper db,
                                                   ApiService api,
                                                   String email) {

        Cursor c = db.listarCasasDoUtilizador(email);
        boolean temLocais = false;
        if (c != null) {
            temLocais = c.moveToFirst();
            c.close();
        }
        if (temLocais) return; // já há casas locais, não repõe

        api.getCasasByUser(email).enqueue(new Callback<List<CasaSync>>() {
            @Override
            public void onResponse(Call<List<CasaSync>> call,
                                   Response<List<CasaSync>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                for (CasaSync casa : response.body()) {
                    db.guardarCasaCompleta(
                            -1, // gera novo id local
                            casa.user_email,
                            casa.nome_casa,
                            casa.tipo,
                            casa.uso,
                            casa.pessoas,
                            casa.ano,
                            casa.morada,
                            casa.distrito,
                            casa.concelho,
                            casa.freguesia,
                            casa.cod_postal
                    );
                }
            }

            @Override
            public void onFailure(Call<List<CasaSync>> call, Throwable t) { }
        });
    }

    // =========================================================
    // RESTAURO DE APPLIANCES (SERVIDOR -> LOCAL)
    // =========================================================
    /**
     * Se para as casas do utilizador ainda não houver appliances
     * na BD local, vai buscar os appliances ao servidor.
     */
    private static void restaurarAppliancesSeNecessario(Context context,
                                                        DBHelper db,
                                                        ApiService api,
                                                        String email) {

        // Ver se já há appliances locais em alguma casa
        Cursor cCasas = db.listarCasasDoUtilizador(email);
        boolean temApps = false;
        if (cCasas != null && cCasas.moveToFirst()) {
            int idxCasaId = cCasas.getColumnIndexOrThrow(DBHelper.C_CASA_ID);
            do {
                int casaId = cCasas.getInt(idxCasaId);
                Cursor cApps = db.obterEletrodomesticosDaCasa(casaId);
                if (cApps != null) {
                    if (cApps.moveToFirst()) {
                        temApps = true;
                        cApps.close();
                        break;
                    }
                    cApps.close();
                }
            } while (cCasas.moveToNext());
            cCasas.close();
        }
        if (temApps) return; // já há appliances locais

        api.getAppliancesByUser(email).enqueue(new Callback<List<ApplianceSync>>() {
            @Override
            public void onResponse(Call<List<ApplianceSync>> call,
                                   Response<List<ApplianceSync>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                for (ApplianceSync app : response.body()) {
                    if (!db.existeEletrodomestico(app.casa_id, app.nome)) {
                        db.adicionarUmEletrodomestico(
                                app.casa_id,
                                app.nome,
                                app.categoria
                        );
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ApplianceSync>> call, Throwable t) { }
        });
    }

    // =========================================================
    // RESTAURO DE ASSISTÊNCIAS (SERVIDOR -> LOCAL)
    // =========================================================
    /**
     * Se não houver assistências na BD local, vai buscar todas
     * as assistências ao servidor e insere-as.
     */
    private static void restaurarAssistenciasSeNecessario(Context context,
                                                          DBHelper db,
                                                          ApiService api) {

        Cursor c = db.listarAssistencias();
        boolean temLocais = false;
        if (c != null) {
            temLocais = c.moveToFirst();
            c.close();
        }
        if (temLocais) return; // já há assistências locais

        api.getAssistencias(null).enqueue(new Callback<List<AssistenciaSync>>() {
            @Override
            public void onResponse(Call<List<AssistenciaSync>> call,
                                   Response<List<AssistenciaSync>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                for (AssistenciaSync a : response.body()) {
                    db.inserirAssistencia(
                            a.data,
                            a.hora,
                            a.descricao,
                            a.tecnico_email
                    );
                    // feedback local fica "Pendente"
                }
            }

            @Override
            public void onFailure(Call<List<AssistenciaSync>> call, Throwable t) { }
        });
    }

}
