package pt.ubi.pdm.ecotrack;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.pdm.ecotrack.api.ApiClient;
import pt.ubi.pdm.ecotrack.api.ApiService;
import pt.ubi.pdm.ecotrack.models.AssistenciaSync;
import pt.ubi.pdm.ecotrack.models.LeituraSync;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncUtils {

    public static void syncTudoAsync(Context context) {
        if (!temInternet(context)) {
            return;
        }

        DBHelper db = new DBHelper(context.getApplicationContext());
        ApiService api = ApiClient.getRetrofit().create(ApiService.class);

        syncLeituras(context, db, api);
        syncAssistencias(context, db, api);
    }

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

    private static void syncLeituras(Context context, DBHelper db, ApiService api) {
        Cursor c = db.obterLeiturasPorSincronizar();
        if (c == null) return;

        List<LeituraSync> lista = new ArrayList<>();
        List<Long> idsLocais = new ArrayList<>();

        try {
            if (!c.moveToFirst()) return;

            int idxId = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_ID);
            int idxData = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_DATA);
            int idxValor = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_VALOR);
            int idxImagem = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_IMAGEM_PATH);
            int idxPrev = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_PREV_ID);
            int idxConsumo = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_CONSUMO_PERIODO);
            int idxCreated = c.getColumnIndexOrThrow(DBHelper.C_LEITURA_CREATED_AT_TS);

            do {
                long id = c.getLong(idxId);
                String data = c.getString(idxData);
                double valor = c.getDouble(idxValor);
                String imagemPath = c.getString(idxImagem);
                Long prev = c.isNull(idxPrev) ? null : c.getLong(idxPrev);
                Double consumo = c.isNull(idxConsumo) ? null : c.getDouble(idxConsumo);
                Long created = c.getLong(idxCreated);

                lista.add(new LeituraSync(
                        id,
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
                if (response.isSuccessful()) {
                    long[] idsArray = new long[idsLocais.size()];
                    for (int i = 0; i < idsLocais.size(); i++) {
                        idsArray[i] = idsLocais.get(i);
                    }
                    db.marcarLeiturasComoSincronizadas(idsArray);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private static void syncAssistencias(Context context, DBHelper db, ApiService api) {
        Cursor c = db.listarAssistencias();   // já existe no DBHelper
        if (c == null) return;

        List<AssistenciaSync> lista = new ArrayList<>();

        try {
            if (!c.moveToFirst()) return;

            int idxId = c.getColumnIndexOrThrow("id");
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
            public void onResponse(Call<Void> call, Response<Void> response) {
                // aqui não precisamos de marcar nada como sincronizado,
                // porque o servidor tem UNIQUE(data, hora, tecnico_email)
                // e ignora duplicados.
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // falhou → não faz mal, tenta-se outra vez da próxima vez
            }
        });
    }


}
