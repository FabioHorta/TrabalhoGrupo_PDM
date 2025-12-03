package pt.ubi.pdm.ecotrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ecotrack.db";
    private static final int DB_VERSION = 6;

    // Thresholds percentuais
    public static final double LIMITE_PERCENTUAL_SUP = 40.0;
    public static final double LIMITE_PERCENTUAL_INF = -40.0;

    // Tabelas e colunas
    public static final String T_USERS = "users";
    public static final String C_USER_ID = "id";
    public static final String C_USER_UID = "firebase_uid";
    public static final String C_USER_EMAIL = "email";
    public static final String C_USER_NAME = "name";
    public static final String C_USER_PRECO_KWH = "preco_kwh";
    public static final String C_USER_TIPO = "tipo";
    public static final String C_USER_PASSWORD_HASH = "password_hash";

    public static final String T_LEITURAS = "leituras";
    public static final String C_LEITURA_ID = "id";
    public static final String C_LEITURA_DATA = "data";
    public static final String C_LEITURA_VALOR = "valor_kwh";
    public static final String C_LEITURA_IMAGEM_PATH = "imagem_path";
    public static final String C_LEITURA_PREV_ID = "prev_leitura_id";
    public static final String C_LEITURA_CONSUMO_PERIODO = "consumo_periodo";
    public static final String C_LEITURA_CREATED_AT_TS = "created_at_ts";
    public static final String C_LEITURA_SYNC_STATUS = "sync_status";
    // casa_id será uma coluna extra em T_LEITURAS (para leituras por casa)

    public static final String T_MEDIA_CONSUMOS = "media_consumos";
    public static final String C_MEDIA_NPERIODOS = "num_periodos";
    public static final String C_MEDIA_VALOR = "media_valor";
    public static final String C_MEDIA_ATUALIZADA_EM = "atualizada_em";

    public static final String T_CONSUMOS_ANALISADOS = "consumos_analisados";
    public static final String C_CONSUMO_ANALISADO_ID = "id";
    public static final String C_CONSUMO_ANALISADO_LEITURA_ID = "leitura_id";
    public static final String C_CONSUMO_ANALISADO_VALOR = "consumo_valor";
    public static final String C_CONSUMO_ANALISADO_MEDIA_REF = "media_referencia";
    public static final String C_CONSUMO_ANALISADO_NUM_PERIODOS = "num_periodos";
    public static final String C_CONSUMO_ANALISADO_PERCENTAGEM = "percentagem_diferenca";
    public static final String C_CONSUMO_ANALISADO_STATUS = "status";
    public static final String C_CONSUMO_ANALISADO_LIMITE_SUP = "limite_superior";
    public static final String C_CONSUMO_ANALISADO_LIMITE_INF = "limite_inferior";
    public static final String C_CONSUMO_ANALISADO_CREATED_AT_TS = "created_at_ts";

    // --- TABELA CASAS ---
    public static final String T_CASAS = "casas";
    public static final String C_CASA_ID = "id";
    public static final String C_CASA_USER_EMAIL = "user_email";
    public static final String C_CASA_NOME = "nome_casa";
    public static final String C_CASA_TIPO = "tipo";
    public static final String C_CASA_USO = "uso";
    public static final String C_CASA_PESSOAS = "pessoas";
    public static final String C_CASA_ANO = "ano";
    public static final String C_CASA_MORADA = "morada";
    public static final String C_CASA_DISTRITO = "distrito";
    public static final String C_CASA_CONCELHO = "concelho";
    public static final String C_CASA_FREGUESIA = "freguesia";
    public static final String C_CASA_COD_POSTAL = "cod_postal";

    // --- TABELA ELETRODOMÉSTICOS ---

    public static final String T_ELETRODOMESTICOS = "appliances";
    public static final String C_APP_ID = "id";
    public static final String C_APP_CASA_ID = "casa_id";
    public static final String C_APP_NOME = "nome";
    public static final String C_APP_CATEGORIA = "categoria";
    public static final String C_APP_QUANTIDADE = "quantidade";
    public static final String C_APP_TIPO = "tipo";
    public static final String C_APP_CLASSE = "classe";  // Coluna classe

    // --- TABELA MENSAGENS CHAT ---
    public static final String T_MENSAGENS_CHAT = "mensagens_chat";
    public static final String C_MSG_ID = "id";
    public static final String C_MSG_REMETENTE = "remetente_email";
    public static final String C_MSG_DESTINATARIO = "destinatario_email";
    public static final String C_MSG_TEXTO = "texto";
    public static final String C_MSG_TS = "timestamp";

    public DBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // USERS com password_hash e tipo
        db.execSQL("CREATE TABLE " + T_USERS + " (" +
                C_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_USER_UID + " TEXT UNIQUE, " +
                C_USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
                C_USER_NAME + " TEXT, " +
                C_USER_PRECO_KWH + " REAL DEFAULT 0.20, " +
                C_USER_PASSWORD_HASH + " TEXT, " +
                C_USER_TIPO + " TEXT DEFAULT 'cliente'" +
                ")"
        );

        // CASAS
        db.execSQL("CREATE TABLE " + T_CASAS + " (" +
                C_CASA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_CASA_USER_EMAIL + " TEXT NOT NULL, " +
                C_CASA_NOME + " TEXT, " +
                C_CASA_TIPO + " TEXT, " +
                C_CASA_USO + " TEXT, " +
                C_CASA_PESSOAS + " INTEGER, " +
                C_CASA_ANO + " TEXT, " +
                C_CASA_MORADA + " TEXT, " +
                C_CASA_DISTRITO + " TEXT, " +
                C_CASA_CONCELHO + " TEXT, " +
                C_CASA_FREGUESIA + " TEXT, " +
                C_CASA_COD_POSTAL + " TEXT" +
                ")");

        // LEITURAS (com casa_id e sync_status)
        db.execSQL("CREATE TABLE " + T_LEITURAS + " (" +
                C_LEITURA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_LEITURA_DATA + " TEXT NOT NULL, " +
                C_LEITURA_VALOR + " REAL NOT NULL, " +
                C_LEITURA_IMAGEM_PATH + " TEXT, " +
                "casa_id INTEGER, " +
                C_LEITURA_PREV_ID + " INTEGER, " +
                C_LEITURA_CONSUMO_PERIODO + " REAL, " +
                C_LEITURA_CREATED_AT_TS + " INTEGER, " +
                C_LEITURA_SYNC_STATUS + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY (" + C_LEITURA_PREV_ID + ") REFERENCES " + T_LEITURAS + "(" + C_LEITURA_ID + ") ON DELETE SET NULL" +
                ")");

        // MEDIA_CONSUMOS
        db.execSQL("CREATE TABLE " + T_MEDIA_CONSUMOS + " (" +
                C_MEDIA_NPERIODOS + " INTEGER PRIMARY KEY, " +
                C_MEDIA_VALOR + " REAL NOT NULL, " +
                C_MEDIA_ATUALIZADA_EM + " TEXT" +
                ")");

        // CONSUMOS_ANALISADOS
        db.execSQL("CREATE TABLE " + T_CONSUMOS_ANALISADOS + " (" +
                C_CONSUMO_ANALISADO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_CONSUMO_ANALISADO_LEITURA_ID + " INTEGER NOT NULL, " +
                C_CONSUMO_ANALISADO_VALOR + " REAL NOT NULL, " +
                C_CONSUMO_ANALISADO_MEDIA_REF + " REAL NOT NULL, " +
                C_CONSUMO_ANALISADO_NUM_PERIODOS + " INTEGER NOT NULL, " +
                C_CONSUMO_ANALISADO_PERCENTAGEM + " REAL, " +
                C_CONSUMO_ANALISADO_STATUS + " TEXT, " +
                C_CONSUMO_ANALISADO_LIMITE_SUP + " REAL, " +
                C_CONSUMO_ANALISADO_LIMITE_INF + " REAL, " +
                C_CONSUMO_ANALISADO_CREATED_AT_TS + " INTEGER, " +
                "FOREIGN KEY (" + C_CONSUMO_ANALISADO_LEITURA_ID + ") REFERENCES " +
                T_LEITURAS + "(" + C_LEITURA_ID + ") ON DELETE CASCADE" +
                ")");

        // ELETRODOMESTICOS
        db.execSQL("CREATE TABLE " + T_ELETRODOMESTICOS + " (" +
                C_APP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_APP_CASA_ID + " INTEGER NOT NULL, " +
                C_APP_NOME + " TEXT, " +
                C_APP_CATEGORIA + " TEXT, " +
                C_APP_CLASSE + " TEXT, " + // Cada um tem a sua classe
                "FOREIGN KEY (" + C_APP_CASA_ID + ") REFERENCES " +
                T_CASAS + "(" + C_CASA_ID + ") ON DELETE CASCADE" +
                ")");

        // MENSAGENS SUPORTE
        db.execSQL("CREATE TABLE IF NOT EXISTS mensagens_suporte (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assunto TEXT," +
                "mensagem TEXT," +
                "data TEXT" +
                ")");

        // ASSISTENCIAS
        db.execSQL("CREATE TABLE assistencias (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "data TEXT, " +
                "hora TEXT, " +
                "descricao TEXT, " +
                "feedback TEXT, " +
                "tecnico_email TEXT" +
                ")");

        // CHAT
        db.execSQL("CREATE TABLE " + T_MENSAGENS_CHAT + " (" +
                C_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_MSG_REMETENTE + " TEXT NOT NULL, " +
                C_MSG_DESTINATARIO + " TEXT NOT NULL, " +
                C_MSG_TEXTO + " TEXT NOT NULL, " +
                C_MSG_TS + " INTEGER" +
                ")");

        // INDEXES
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_leituras_prev ON " +
                T_LEITURAS + "(" + C_LEITURA_PREV_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_leituras_created_ts ON " +
                T_LEITURAS + "(" + C_LEITURA_CREATED_AT_TS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_consumos_analisados_leitura ON " +
                T_CONSUMOS_ANALISADOS + "(" + C_CONSUMO_ANALISADO_LEITURA_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_consumos_analisados_created ON " +
                T_CONSUMOS_ANALISADOS + "(" + C_CONSUMO_ANALISADO_CREATED_AT_TS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_consumos_analisados_status ON " +
                T_CONSUMOS_ANALISADOS + "(" + C_CONSUMO_ANALISADO_STATUS + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + T_LEITURAS);
        db.execSQL("DROP TABLE IF EXISTS " + T_MEDIA_CONSUMOS);
        db.execSQL("DROP TABLE IF EXISTS " + T_CONSUMOS_ANALISADOS);
        db.execSQL("DROP TABLE IF EXISTS " + T_CASAS);
        db.execSQL("DROP TABLE IF EXISTS " + T_ELETRODOMESTICOS);
        db.execSQL("DROP TABLE IF EXISTS mensagens_suporte");
        db.execSQL("DROP TABLE IF EXISTS assistencias");
        db.execSQL("DROP TABLE IF EXISTS " + T_MENSAGENS_CHAT);

        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ---------- UTILIZADORES ----------

    public void saveOrUpdateUser(String idServidor,
                                 String email,
                                 String name,
                                 Double precoKwh,
                                 String tipo,
                                 String passwordHash) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USER_UID, idServidor);
        cv.put(C_USER_EMAIL, email);
        cv.put(C_USER_NAME, name);
        cv.put(C_USER_PRECO_KWH, precoKwh);
        cv.put(C_USER_TIPO, tipo);
        cv.put(C_USER_PASSWORD_HASH, passwordHash);

        long existingId = -1;
        Cursor c = db.query(
                T_USERS,
                new String[]{C_USER_ID},
                C_USER_EMAIL + " = ?",
                new String[]{email},
                null, null, null
        );
        try {
            if (c.moveToFirst()) {
                existingId = c.getLong(0);
            }
        } finally {
            c.close();
        }

        if (existingId > 0) {
            db.update(T_USERS, cv, C_USER_ID + "=?", new String[]{String.valueOf(existingId)});
        } else {
            db.insert(T_USERS, null, cv);
        }
    }

    public long getUserIdByUid(String uid) {
        Cursor c = getReadableDatabase().query(
                T_USERS,
                new String[]{C_USER_ID},
                C_USER_UID + "=?",
                new String[]{uid},
                null, null, null
        );
        try {
            if (c.moveToFirst()) return c.getLong(0);
            return -1;
        } finally {
            c.close();
        }
    }

    public String obterTipoUtilizadorPorEmail(String email) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + C_USER_TIPO + " FROM " + T_USERS +
                        " WHERE " + C_USER_EMAIL + " = ?",
                new String[]{email}
        );
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            } else {
                return "cliente";
            }
        } finally {
            c.close();
        }
    }

    public void atualizarPrecoUtilizador(String email, double preco) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USER_PRECO_KWH, preco);
        db.update(T_USERS, cv, C_USER_EMAIL + " = ?", new String[]{email});
    }

    public Cursor obterDadosUtilizadorPorEmail(String email) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + T_USERS +
                        " WHERE " + C_USER_EMAIL + " = ?",
                new String[]{email}
        );
    }

    // ---------- LEITURAS ----------



    // Com casa_id
    public long inserirLeituraComFotoPorCasa(int casaId, String data, double valorKwh, String imagemPath) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            double mediaAntes = calcularMediaConsumosInterno(db, 6, casaId);

            long prevId = -1;
            double prevValor = -1;

            Cursor c = db.rawQuery(
                    "SELECT " + C_LEITURA_ID + ", " + C_LEITURA_VALOR +
                            " FROM " + T_LEITURAS +
                            " WHERE casa_id = ? " +
                            " ORDER BY " + C_LEITURA_ID + " DESC LIMIT 1",
                    new String[]{String.valueOf(casaId)}
            );
            if (c.moveToFirst()) {
                prevId = c.getLong(0);
                prevValor = c.getDouble(1);
            }
            c.close();

            Double consumoPeriodo = null;
            if (prevId != -1 && valorKwh >= prevValor) {
                consumoPeriodo = valorKwh - prevValor;
            }

            ContentValues cv = new ContentValues();
            cv.put(C_LEITURA_DATA, data);
            cv.put(C_LEITURA_VALOR, valorKwh);
            cv.put(C_LEITURA_IMAGEM_PATH, imagemPath);
            cv.put("casa_id", casaId);
            if (prevId != -1) cv.put(C_LEITURA_PREV_ID, prevId);
            if (consumoPeriodo != null) cv.put(C_LEITURA_CONSUMO_PERIODO, consumoPeriodo);
            cv.put(C_LEITURA_CREATED_AT_TS, System.currentTimeMillis());
            cv.put(C_LEITURA_SYNC_STATUS, 0);

            long rowId = db.insert(T_LEITURAS, null, cv);

            if (rowId != -1 && consumoPeriodo != null) {
                criarRegistroConsumoAnalisado(db, rowId, consumoPeriodo, 6, mediaAntes);
                recalcularEMediaPorCasa(db, new int[]{1, 3, 6}, casaId);
            }

            db.setTransactionSuccessful();
            return rowId;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor obterLeiturasPorSincronizar() {
        String where = C_LEITURA_SYNC_STATUS + " = ?";
        String[] args = {"0"};
        return getReadableDatabase().query(
                T_LEITURAS,
                null,
                where,
                args,
                null,
                null,
                null
        );
    }


    public Cursor obterLeiturasPorCasa(int casaId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + T_LEITURAS +
                        " WHERE casa_id = ? ORDER BY " + C_LEITURA_ID + " DESC",
                new String[]{String.valueOf(casaId)}
        );
    }


    public void apagarLeituraPorCasa(long leituraId, int casaId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(
                    T_LEITURAS,
                    C_LEITURA_ID + "=? AND casa_id=?",
                    new String[]{String.valueOf(leituraId), String.valueOf(casaId)}
            );
            recalcularEMediaPorCasa(db, new int[]{1, 3, 6}, casaId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    public int contarLeiturasPorCasa(int casaId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + T_LEITURAS + " WHERE casa_id = ?",
                new String[]{String.valueOf(casaId)}
        );
        try {
            if (c.moveToFirst()) return c.getInt(0);
            return 0;
        } finally {
            c.close();
        }
    }

    // ---------- MÉDIAS ----------

    public double calcularMediaConsumos(int numPeriodos) {
        return calcularMediaConsumosInterno(getReadableDatabase(), numPeriodos);
    }

    public double calcularMediaConsumosPorCasa(int numPeriodos, int casaId) {
        return calcularMediaConsumosInterno(getReadableDatabase(), numPeriodos, casaId);
    }

    private double calcularMediaConsumosInterno(SQLiteDatabase db, int numPeriodos) {
        if (numPeriodos <= 0) return 0.0;

        Cursor c = null;
        try {
            String sqlLeituras = "SELECT " + C_LEITURA_VALOR +
                    " FROM " + T_LEITURAS +
                    " ORDER BY " + C_LEITURA_ID + " DESC" +
                    " LIMIT " + (numPeriodos + 1);

            c = db.rawQuery(sqlLeituras, null);
            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                return 0.0;
            }

            int n = c.getCount();
            if (n < 2) {
                c.close();
                return 0.0;
            }

            double[] leituras = new double[n];
            int idx = 0;
            do {
                leituras[idx++] = c.getDouble(0);
            } while (c.moveToNext());
            c.close();
            c = null;

            double somaConsumos = 0.0;
            int contPeriodos = 0;
            for (int i = 0; i < leituras.length - 1 && contPeriodos < numPeriodos; i++) {
                double atual = leituras[i];
                double anterior = leituras[i + 1];
                double consumo = atual - anterior;
                if (consumo > 0) {
                    somaConsumos += consumo;
                    contPeriodos++;
                }
            }

            if (contPeriodos == 0) return 0.0;
            return somaConsumos / contPeriodos;

        } finally {
            if (c != null && !c.isClosed()) c.close();
        }
    }

    private double calcularMediaConsumosInterno(SQLiteDatabase db, int numPeriodos, int casaId) {
        if (numPeriodos <= 0) return 0.0;

        Cursor c = null;
        try {
            String sqlLeituras = "SELECT " + C_LEITURA_VALOR +
                    " FROM " + T_LEITURAS +
                    " WHERE casa_id = ? " +
                    " ORDER BY " + C_LEITURA_ID + " DESC" +
                    " LIMIT " + (numPeriodos + 1);

            c = db.rawQuery(sqlLeituras, new String[]{String.valueOf(casaId)});
            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                return 0.0;
            }

            int n = c.getCount();
            if (n < 2) {
                c.close();
                return 0.0;
            }

            double[] leituras = new double[n];
            int idx = 0;
            do {
                leituras[idx++] = c.getDouble(0);
            } while (c.moveToNext());
            c.close();
            c = null;

            double somaConsumos = 0.0;
            int contPeriodos = 0;
            for (int i = 0; i < leituras.length - 1 && contPeriodos < numPeriodos; i++) {
                double atual = leituras[i];
                double anterior = leituras[i + 1];
                double consumo = atual - anterior;
                if (consumo > 0) {
                    somaConsumos += consumo;
                    contPeriodos++;
                }
            }

            if (contPeriodos == 0) return 0.0;
            return somaConsumos / contPeriodos;

        } finally {
            if (c != null && !c.isClosed()) c.close();
        }
    }

    private void recalcularEMedia(SQLiteDatabase db, int[] periodos) {
        for (int n : periodos) {
            double media = calcularMediaConsumosInterno(db, n);
            ContentValues cv = new ContentValues();
            cv.put(C_MEDIA_NPERIODOS, n);
            cv.put(C_MEDIA_VALOR, media);
            cv.put(C_MEDIA_ATUALIZADA_EM, Instant.now().toString());
            db.insertWithOnConflict(
                    T_MEDIA_CONSUMOS,
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_REPLACE
            );
        }
    }

    private void recalcularEMediaPorCasa(SQLiteDatabase db, int[] periodos, int casaId) {
        for (int n : periodos) {
            double media = calcularMediaConsumosInterno(db, n, casaId);
        }
    }

    public Map<Integer, Double> obterMediasArmazenadas() {
        Map<Integer, Double> medias = new HashMap<>();
        Cursor c = getReadableDatabase().query(
                T_MEDIA_CONSUMOS,
                new String[]{C_MEDIA_NPERIODOS, C_MEDIA_VALOR},
                null, null, null, null, null
        );
        try {
            while (c.moveToNext()) {
                medias.put(c.getInt(0), c.getDouble(1));
            }
        } finally {
            c.close();
        }
        return medias;
    }

    private Map<Integer, Double> obterMediasArmazenadasInterno(SQLiteDatabase db) {
        Map<Integer, Double> medias = new HashMap<>();
        Cursor c = db.query(
                T_MEDIA_CONSUMOS,
                new String[]{C_MEDIA_NPERIODOS, C_MEDIA_VALOR},
                null, null, null, null, null
        );
        try {
            while (c.moveToNext()) {
                medias.put(c.getInt(0), c.getDouble(1));
            }
        } finally {
            c.close();
        }
        return medias;
    }

    // ---------- ANÁLISE DE CONSUMO ----------

    private void criarRegistroConsumoAnalisado(SQLiteDatabase db,
                                               long leituraId,
                                               double consumoValor,
                                               int numPeriodos,
                                               Double mediaRefParam) {
        Double mediaRef = mediaRefParam;

        if (mediaRef == null) {
            Map<Integer, Double> medias = obterMediasArmazenadasInterno(db);
            mediaRef = medias.get(numPeriodos);
            if (mediaRef == null || mediaRef <= 0) {
                mediaRef = calcularMediaConsumosInterno(db, numPeriodos);
            }
        }

        if (mediaRef == null || mediaRef <= 0 || consumoValor <= 0) return;

        double percentagem = ((consumoValor - mediaRef) / mediaRef) * 100.0;
        String status = "NORMAL";
        if (percentagem > LIMITE_PERCENTUAL_SUP) {
            status = "ALTO";
        } else if (percentagem < LIMITE_PERCENTUAL_INF) {
            status = "BAIXO";
        }

        ContentValues cv = new ContentValues();
        cv.put(C_CONSUMO_ANALISADO_LEITURA_ID, leituraId);
        cv.put(C_CONSUMO_ANALISADO_VALOR, consumoValor);
        cv.put(C_CONSUMO_ANALISADO_MEDIA_REF, mediaRef);
        cv.put(C_CONSUMO_ANALISADO_NUM_PERIODOS, numPeriodos);
        cv.put(C_CONSUMO_ANALISADO_PERCENTAGEM, percentagem);
        cv.put(C_CONSUMO_ANALISADO_STATUS, status);
        cv.put(C_CONSUMO_ANALISADO_LIMITE_SUP, LIMITE_PERCENTUAL_SUP);
        cv.put(C_CONSUMO_ANALISADO_LIMITE_INF, LIMITE_PERCENTUAL_INF);
        cv.put(C_CONSUMO_ANALISADO_CREATED_AT_TS, System.currentTimeMillis());

        db.insert(T_CONSUMOS_ANALISADOS, null, cv);
    }


    public Cursor obterAnaliseConsumoPorCasa(long leituraId, int casaId) {
        return getReadableDatabase().rawQuery(
                "SELECT ca.* FROM " + T_CONSUMOS_ANALISADOS + " ca " +
                        "JOIN " + T_LEITURAS + " l ON ca." + C_CONSUMO_ANALISADO_LEITURA_ID +
                        " = l." + C_LEITURA_ID +
                        " WHERE ca." + C_CONSUMO_ANALISADO_LEITURA_ID + " = ? " +
                        " AND l.casa_id = ?",
                new String[]{String.valueOf(leituraId), String.valueOf(casaId)}
        );
    }

    public Cursor obterHistoricoConsumosAnalisados(String status) {
        String selection = status != null ? C_CONSUMO_ANALISADO_STATUS + "=?" : null;
        String[] selectionArgs = status != null ? new String[]{status} : null;

        return getReadableDatabase().query(
                T_CONSUMOS_ANALISADOS + " ca JOIN " + T_LEITURAS + " l ON ca." +
                        C_CONSUMO_ANALISADO_LEITURA_ID + " = l." + C_LEITURA_ID,
                new String[]{
                        "ca.*",
                        "l." + C_LEITURA_DATA + " as data_leitura",
                        "l." + C_LEITURA_VALOR + " as valor_leitura"
                },
                selection,
                selectionArgs,
                null, null,
                "ca." + C_CONSUMO_ANALISADO_CREATED_AT_TS + " DESC"
        );
    }

    public double obterUltimaLeituraOuDefaultPorCasa(int casaId, double defaultValue) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery(
                    "SELECT " + C_LEITURA_VALOR +
                            " FROM " + T_LEITURAS +
                            " WHERE casa_id = ? " +
                            " ORDER BY " + C_LEITURA_DATA + " DESC LIMIT 1",
                    new String[]{String.valueOf(casaId)}
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }
        return defaultValue;
    }

    public double obterUltimaLeituraPorCasa(int casaId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + C_LEITURA_VALOR +
                        " FROM " + T_LEITURAS +
                        " WHERE casa_id = ? " +
                        " ORDER BY " + C_LEITURA_ID + " DESC LIMIT 1",
                new String[]{String.valueOf(casaId)}
        );
        double valor = 0;
        if (c.moveToFirst()) {
            valor = c.getDouble(0);
        }
        c.close();
        return valor;
    }

    // ---------- SUPORTE ----------

    public boolean inserirMensagem(String assunto, String mensagem, String data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("assunto", assunto);
        cv.put("mensagem", mensagem);
        cv.put("data", data);
        long r = db.insert("mensagens_suporte", null, cv);
        return r != -1;
    }

    public Cursor listarMensagens() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM mensagens_suporte ORDER BY id DESC",
                null
        );
    }

    // ---------- ASSISTENCIAS ----------

    public boolean inserirAssistencia(String data, String hora, String descricao, String tecnicoEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("data", data);
        cv.put("hora", hora);
        cv.put("descricao", descricao);
        cv.put("feedback", "Pendente");
        cv.put("tecnico_email", tecnicoEmail);
        long r = db.insert("assistencias", null, cv);
        return r != -1;
    }

    public boolean existeAssistenciaNoSlot(String data, String hora, String tecnicoEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM assistencias WHERE data = ? AND hora = ? AND tecnico_email = ?",
                new String[]{data, hora, tecnicoEmail}
        );
        boolean existe = c.moveToFirst();
        c.close();
        return existe;
    }

    public Cursor listarTecnicos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT " + C_USER_EMAIL +
                        " FROM " + T_USERS +
                        " WHERE " + C_USER_TIPO + " = 'tecnico'",
                null
        );
    }

    public Cursor listarAssistenciasDoTecnico(String tecnicoEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM assistencias WHERE tecnico_email = ? ORDER BY id DESC",
                new String[]{tecnicoEmail}
        );
    }

    public Cursor listarAssistencias() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM assistencias ORDER BY id DESC",
                null
        );
    }

    // ---------- CASAS ----------

    public int guardarCasaCompleta(int id,
                                   String email,
                                   String nome,
                                   String tipo,
                                   String uso,
                                   int pessoas,
                                   String ano,
                                   String morada,
                                   String distrito,
                                   String concelho,
                                   String freguesia,
                                   String codPostal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_CASA_USER_EMAIL, email);
        cv.put(C_CASA_NOME, nome);
        cv.put(C_CASA_TIPO, tipo);
        cv.put(C_CASA_USO, uso);
        cv.put(C_CASA_PESSOAS, pessoas);
        cv.put(C_CASA_ANO, ano);
        cv.put(C_CASA_MORADA, morada);
        cv.put(C_CASA_DISTRITO, distrito);
        cv.put(C_CASA_CONCELHO, concelho);
        cv.put(C_CASA_FREGUESIA, freguesia);
        cv.put(C_CASA_COD_POSTAL, codPostal);

        if (id == -1) {
            return (int) db.insert(T_CASAS, null, cv);
        } else {
            db.update(T_CASAS, cv, C_CASA_ID + "=?", new String[]{String.valueOf(id)});
            return id;
        }
    }

    public Cursor listarCasasDoUtilizador(String email) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + T_CASAS +
                        " WHERE " + C_CASA_USER_EMAIL + " = ?",
                new String[]{email}
        );
    }

    public Cursor obterCasaPorId(int id) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + T_CASAS +
                        " WHERE " + C_CASA_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    // ---------- ELETRODOMÉSTICOS ----------

    public void atualizarEletrodomestico(int casaId,
                                         String nome,
                                         String categoria,
                                         int quantidade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_APP_CASA_ID, casaId);
        cv.put(C_APP_NOME, nome);
        cv.put(C_APP_CATEGORIA, categoria);
        cv.put(C_APP_QUANTIDADE, quantidade);
        db.insertWithOnConflict(
                T_ELETRODOMESTICOS,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public void adicionarUmEletrodomestico(int casaId, String nome, String categoria) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_APP_CASA_ID, casaId);
        cv.put(C_APP_NOME, nome);
        cv.put(C_APP_CATEGORIA, categoria);
        // Não definimos classe ainda (fica NULL)
        db.insert(T_ELETRODOMESTICOS, null, cv);
    }
    public Cursor obterEletrodomesticosDaCasa(int casaId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_CASA_ID + " = ?",
                new String[]{String.valueOf(casaId)}
        );
    }

    // Remove UMA unidade (ex: clicaste no -) -> Apaga o último inserido desse tipo
    public void removerUmEletrodomestico(int casaId, String nome) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Apaga apenas 1 linha desse aparelho nessa casa (o com maior ID, ou seja, o último)
        db.execSQL("DELETE FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_ID + " = (SELECT MAX(" + C_APP_ID + ") FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_CASA_ID + "=? AND " + C_APP_NOME + "=?)",
                new String[]{String.valueOf(casaId), nome});
    }

    // Conta quantos existem (para mostrar no numerozinho do meio)
    public int contarEletrodomesticosEspecificos(int casaId, String nome) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_CASA_ID + "=? AND " + C_APP_NOME + "=?",
                new String[]{String.valueOf(casaId), nome}
        );
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }
    public void eliminarEletrodomestico(int casaId, String nome) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(
                T_ELETRODOMESTICOS,
                C_APP_CASA_ID + " = ? AND " + C_APP_NOME + " = ?",
                new String[]{String.valueOf(casaId), nome}
        );
        db.close();
    }
    public void atualizarClasseConsumoEletrodomestico(int eletroId, String classe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(C_APP_CLASSE, classe);

        db.update(
                T_ELETRODOMESTICOS,
                values,
                C_APP_ID + " = ?",
                new String[]{String.valueOf(eletroId)}
        );

        db.close();
    }


    public void inserirConsumoEstimado(int casaId, double consumoMensal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("casa_id", casaId);
        values.put("consumo_mensal", consumoMensal);
        values.put("consumo_anual", consumoMensal * 12);
        values.put("data_estimativa", System.currentTimeMillis());

        db.insert("CONSUMO_ESTIMADO", null, values);
        db.close();
    }


    // ---------- CHAT UTILIZADOR <-> TÉCNICO ----------

    public boolean inserirMensagemChat(String remetenteEmail,
                                       String destinatarioEmail,
                                       String texto) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_MSG_REMETENTE, remetenteEmail);
        cv.put(C_MSG_DESTINATARIO, destinatarioEmail);
        cv.put(C_MSG_TEXTO, texto);
        cv.put(C_MSG_TS, System.currentTimeMillis());
        long r = db.insert(T_MENSAGENS_CHAT, null, cv);
        return r != -1;
    }

    public Cursor listarMensagensDoUtilizador(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + T_MENSAGENS_CHAT +
                        " WHERE " + C_MSG_REMETENTE + " = ? " +
                        " OR " + C_MSG_DESTINATARIO + " = ? " +
                        " ORDER BY " + C_MSG_TS + " ASC",
                new String[]{email, email}
        );
    }

    public Cursor listarMensagensEntre(String email1, String email2) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + T_MENSAGENS_CHAT +
                        " WHERE (" + C_MSG_REMETENTE + " = ? AND " +
                        C_MSG_DESTINATARIO + " = ?) " +
                        " OR (" + C_MSG_REMETENTE + " = ? AND " +
                        C_MSG_DESTINATARIO + " = ?) " +
                        " ORDER BY " + C_MSG_TS + " ASC",
                new String[]{email1, email2, email2, email1}
        );
    }

    public Cursor listarClientesDoTecnicoNoChat(String tecnicoEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT DISTINCT " +
                        "CASE WHEN " + C_MSG_REMETENTE + " = ? THEN " +
                        C_MSG_DESTINATARIO +
                        " ELSE " + C_MSG_REMETENTE +
                        " END AS cliente_email " +
                        "FROM " + T_MENSAGENS_CHAT +
                        " WHERE " + C_MSG_REMETENTE + " = ? " +
                        " OR " + C_MSG_DESTINATARIO + " = ?",
                new String[]{tecnicoEmail, tecnicoEmail, tecnicoEmail}
        );
    }

    // ---------- SYNC LEITURAS ----------

    public void marcarLeiturasComoSincronizadas(long[] ids) {
        if (ids == null || ids.length == 0) return;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(C_LEITURA_SYNC_STATUS, 1);

            for (long id : ids) {
                db.update(
                        T_LEITURAS,
                        cv,
                        C_LEITURA_ID + " = ?",
                        new String[]{String.valueOf(id)}
                );
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
