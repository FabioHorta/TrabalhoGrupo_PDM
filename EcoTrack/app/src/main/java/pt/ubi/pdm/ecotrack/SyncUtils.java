package pt.ubi.pdm.ecotrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.ApplianceSync;
import pt.ubi.pdm.ecotrack.models.AssistenciaSync;
import pt.ubi.pdm.ecotrack.models.CasaSync;
import pt.ubi.pdm.ecotrack.models.LeituraSync;
import pt.ubi.pdm.ecotrack.models.MensagemChatSync;
import pt.ubi.pdm.ecotrack.models.MensagemSuporteSync;
import pt.ubi.pdm.ecotrack.models.Tecnico;
import pt.ubi.pdm.ecotrack.models.UploadLeituraImagemRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Classe utilitária para sincronização e restauro:
 *
 * - LOCAL -> SERVIDOR:
 *      leituras, assistências, casas, appliances, mensagens de suporte, mensagens de chat
 *
 * - SERVIDOR -> LOCAL:
 *      casas, appliances, assistências, leituras, técnicos, mensagens de chat
 */
public class SyncUtils {

    // =========================================================
    //  VERIFICAÇÃO DE INTERNET
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
    //  MÉTODO PRINCIPAL – SYNC LOCAL -> SERVIDOR
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

        // Enviar dados locais para o servidor
        syncLeituras(context, db, api);
        syncAssistencias(context, db, api);
        syncCasas(context, db, api);
        syncAppliances(context, db, api);
        syncMensagensSuporte(context, db, api);
        syncMensagensChat(context, db, api);
    }

    /**
     * Sincronização específica do CHAT para o utilizador autenticado:
     *  - envia mensagens locais deste utilizador para o servidor
     *  - vai buscar do servidor todo o histórico deste utilizador
     *    e faz merge na BD local (sem duplicar).
     */
    public static void syncChatCompleto(Context context) {
        if (!temInternet(context)) return;

        SharedPreferences sp =
                context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String email = sp.getString("user_email", null);
        if (email == null) return;

        DBHelper db = new DBHelper(context.getApplicationContext());
        ApiService api = ApiClient.getRetrofit().create(ApiService.class);

        // 1) Enviar tudo o que temos localmente para o servidor
        syncMensagensChat(context, db, api);

        // 2) Buscar do servidor e fazer merge na BD local
        restaurarMensagensChatMerge(context, db, api, email);
    }



    // =========================================================
    //  SYNC LEITURAS (LOCAL -> SERVIDOR)
    // =========================================================

    /**
     * Envia para o servidor todas as leituras com sync_status = 0.
     * Depois de aceites pelo servidor, marca como sincronizadas.
     * Para cada leitura com imagem, faz um segundo POST só com o bitmap em Base64.
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
                if (!response.isSuccessful()) return;

                // Marca as leituras como sincronizadas
                long[] idsArray = new long[idsLocais.size()];
                for (int i = 0; i < idsLocais.size(); i++) {
                    idsArray[i] = idsLocais.get(i);
                }
                db.marcarLeiturasComoSincronizadas(idsArray);

                // Para cada leitura com imagem, faz o segundo POST com o bitmap
                for (LeituraSync l : lista) {
                    if (l.imagem_path != null && !l.imagem_path.isEmpty()) {
                        enviarImagemLeitura(
                                context.getApplicationContext(),
                                api,
                                l.casa_id,
                                l.data,
                                l.valor_kwh,
                                l.imagem_path
                        );
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Se falhar, volta a tentar na próxima sincronização
            }
        });
    }

    // =========================================================
    //  SYNC ASSISTÊNCIAS (LOCAL -> SERVIDOR)
    // =========================================================

    /**
     * Envia todas as assistências da BD local para o servidor.
     * (Aqui não há controlo de "já sincronizado", envia tudo).
     */
    private static void syncAssistencias(Context context, DBHelper db, ApiService api) {
        Cursor c = db.listarAssistencias();
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
    //  SYNC CASAS (LOCAL -> SERVIDOR)
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
    //  SYNC APPLIANCES (LOCAL -> SERVIDOR)
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
    //  SYNC MENSAGENS DE SUPORTE (LOCAL -> SERVIDOR)
    // =========================================================

    /**
     * Envia todas as mensagens de suporte da BD local para o servidor.
     * (Não há ainda restauro do histórico de suporte do servidor -> local.)
     */
    private static void syncMensagensSuporte(Context context, DBHelper db, ApiService api) {
        Cursor c = db.listarMensagens();
        if (c == null) return;

        List<MensagemSuporteSync> lista = new ArrayList<>();

        try {
            if (!c.moveToFirst()) return;

            int idxId   = c.getColumnIndexOrThrow("id");
            int idxAss  = c.getColumnIndexOrThrow("assunto");
            int idxMsg  = c.getColumnIndexOrThrow("mensagem");
            int idxData = c.getColumnIndexOrThrow("data");

            do {
                long id = c.getLong(idxId);
                String assunto = c.getString(idxAss);
                String mensagem = c.getString(idxMsg);
                String data = c.getString(idxData);

                lista.add(new MensagemSuporteSync(id, assunto, mensagem, data));
            } while (c.moveToNext());
        } finally {
            c.close();
        }

        if (lista.isEmpty()) return;

        api.syncMensagensSuporte(lista).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { }

            @Override
            public void onFailure(Call<Void> call, Throwable t) { }
        });
    }

    // =========================================================
    //  SYNC MENSAGENS DE CHAT (LOCAL -> SERVIDOR)
    // =========================================================

    /**
     * Envia para o servidor as mensagens de chat em que
     * o utilizador autenticado seja remetente ou destinatário,
     * MAS apenas as que ainda não foram sincronizadas
     * (sync_status = 0 na BD local).
     *
     * Assim evitas duplicar no MariaDB e resolves o quadriplicar
     * que estavas a ver.
     */
    private static void syncMensagensChat(Context context, DBHelper db, ApiService api) {
        SharedPreferences sp = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String email = sp.getString("user_email", null);
        if (email == null) return;

        // *** ALTERAÇÃO IMPORTANTE ***
        // Em vez de listar TODAS as mensagens do utilizador,
        // só listamos as que estão por sincronizar.
        Cursor c = db.listarMensagensPorSincronizar(email);
        if (c == null) return;

        List<MensagemChatSync> lista = new ArrayList<>();
        List<Long> idsLocais = new ArrayList<>();

        try {
            if (!c.moveToFirst()) return;

            int idxId   = c.getColumnIndexOrThrow(DBHelper.C_MSG_ID);
            int idxRem  = c.getColumnIndexOrThrow(DBHelper.C_MSG_REMETENTE);
            int idxDest = c.getColumnIndexOrThrow(DBHelper.C_MSG_DESTINATARIO);
            int idxTxt  = c.getColumnIndexOrThrow(DBHelper.C_MSG_TEXTO);
            int idxTs   = c.getColumnIndexOrThrow(DBHelper.C_MSG_TS);

            do {
                long id = c.getLong(idxId);
                String rem = c.getString(idxRem);
                String dest = c.getString(idxDest);
                String txt = c.getString(idxTxt);
                long ts = c.getLong(idxTs);

                lista.add(new MensagemChatSync(id, rem, dest, txt, ts));
                idsLocais.add(id);
            } while (c.moveToNext());
        } finally {
            c.close();
        }

        if (lista.isEmpty()) return;

        api.syncMensagensChat(lista).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) return;

                // Depois de o servidor aceitar, marcamos como sincronizadas
                long[] idsArray = new long[idsLocais.size()];
                for (int i = 0; i < idsLocais.size(); i++) {
                    idsArray[i] = idsLocais.get(i);
                }
                db.marcarMensagensComoSincronizadas(idsArray);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Se falhar, mantêm-se com sync_status = 0 e tenta de novo no próximo sync
            }
        });
    }

    // =========================================================
    //  RESTAURO COMPLETO (SERVIDOR -> LOCAL) APÓS LOGIN
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
        restaurarTecnicosSeNecessario(context, db, api);
    }

    // =========================================================
    //  RESTAURO DE LEITURAS (SERVIDOR -> LOCAL)
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

        boolean temLocais = db.existemLeiturasLocais();
        if (temLocais) return;   // já há leituras -> não repõe

        api.getLeiturasByUser(email).enqueue(new Callback<List<LeituraSync>>() {
            @Override
            public void onResponse(Call<List<LeituraSync>> call,
                                   Response<List<LeituraSync>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                for (LeituraSync l : response.body()) {

                    // Se vier imagem em Base64, recria o ficheiro local
                    String imagemPathLocal;
                    if (l.imagem_base64 != null && !l.imagem_base64.isEmpty()) {
                        imagemPathLocal = guardarImagemRestaurada(context, l.imagem_base64);
                    } else {
                        // fallback: se usares ainda o path textual do servidor
                        imagemPathLocal = l.imagem_path;
                    }

                    db.inserirLeituraRestaurada(
                            l.casa_id,
                            l.data,
                            l.valor_kwh,
                            imagemPathLocal,
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
    //  RESTAURO DE CASAS (SERVIDOR -> LOCAL)
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
    //  RESTAURO DE APPLIANCES (SERVIDOR -> LOCAL)
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
    //  RESTAURO DE ASSISTÊNCIAS (SERVIDOR -> LOCAL)
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

    // =========================================================
    //  RESTAURO DE MENSAGENS DE CHAT (SERVIDOR -> LOCAL)
    // =========================================================
    /**
     * Vai SEMPRE ao servidor buscar todas as mensagens deste utilizador
     * e faz merge na BD local:
     *  - se a mensagem ainda não existir localmente (mesmo rem/dest/ts),
     *    é inserida;
     *  - se já existir, é ignorada.
     */
    private static void restaurarMensagensChatMerge(Context context,
                                                    DBHelper db,
                                                    ApiService api,
                                                    String email) {

        api.getMensagensChatByEmail(email).enqueue(new Callback<List<MensagemChatSync>>() {
            @Override
            public void onResponse(Call<List<MensagemChatSync>> call,
                                   Response<List<MensagemChatSync>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                for (MensagemChatSync m : response.body()) {
                    // Só insere se ainda não existir esta mensagem localmente
                    if (!db.existeMensagemChatComTs(
                            m.remetenteEmail,
                            m.destinatarioEmail,
                            m.timestamp
                    )) {
                        db.inserirMensagemChatComTs(
                                m.remetenteEmail,
                                m.destinatarioEmail,
                                m.texto,
                                m.timestamp
                        );
                    }
                }
            }

            @Override
            public void onFailure(Call<List<MensagemChatSync>> call, Throwable t) { }
        });
    }


    // =========================================================
    //  RESTAURO DE TÉCNICOS (SERVIDOR -> LOCAL)
    // =========================================================

    /**
     * Se não houver técnicos na BD local, vai buscá-los ao servidor
     * e insere-os na tabela de técnicos.
     */
    private static void restaurarTecnicosSeNecessario(Context context,
                                                      DBHelper db,
                                                      ApiService api) {

        // Se já houver técnicos locais, não é preciso repor
        if (db.existemTecnicosLocais()) return;

        api.getTecnicos().enqueue(new Callback<List<Tecnico>>() {
            @Override
            public void onResponse(Call<List<Tecnico>> call,
                                   Response<List<Tecnico>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                for (Tecnico t : response.body()) {
                    db.inserirTecnico(t.email, t.name);
                }
            }

            @Override
            public void onFailure(Call<List<Tecnico>> call, Throwable t) { }
        });
    }

    // =========================================================
    //  ENVIAR IMAGEM DE UMA LEITURA (SEGUNDO POST)
    // =========================================================

    /**
     * Lê o ficheiro de imagem guardado em internal storage e envia-o
     * em Base64 para o endpoint /leituras/upload-bitmap.
     */
    private static void enviarImagemLeitura(Context context,
                                            ApiService api,
                                            Integer casaId,
                                            String data,
                                            double valorKwh,
                                            String imagemPath) {

        if (imagemPath == null || casaId == null) return;

        FileInputStream fis = null;
        try {
            fis = context.openFileInput(imagemPath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }

            String imagemBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            UploadLeituraImagemRequest body =
                    new UploadLeituraImagemRequest(casaId, data, valorKwh, imagemBase64);

            api.uploadLeituraBitmap(body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    // opcional: logs
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    t.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException ignored) {}
            }
        }
    }

    // =========================================================
    //  GUARDAR IMAGENS RECEBIDAS DA API EM FICHEIROS LOCAIS
    // =========================================================

    /**
     * Guarda um PNG vindo da API (Base64) num ficheiro local
     * com prefixo "contador_srv_".
     */
    private static String guardarImagemDaApi(Context context, String base64) {
        if (base64 == null || base64.isEmpty()) return null;

        byte[] dados;
        try {
            dados = Base64.decode(base64, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String fileName = "contador_srv_" + System.currentTimeMillis() + ".png";

        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(dados);
            fos.flush();
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Guarda imagem restaurada (Base64) em ficheiro local
     * com prefixo "contador_restaurado_".
     */
    private static String guardarImagemRestaurada(Context context, String base64) {
        if (base64 == null || base64.isEmpty()) return null;

        try {
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);

            String fileName = "contador_restaurado_" + System.currentTimeMillis() + ".png";

            try (java.io.FileOutputStream fos =
                         context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                fos.write(bytes);
            }

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
