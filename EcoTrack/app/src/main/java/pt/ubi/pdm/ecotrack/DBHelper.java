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
    private static final int DB_VERSION = 3;

    // Thresholds percentuais
    public static final double LIMITE_PERCENTUAL_SUP = 40.0;
    public static final double LIMITE_PERCENTUAL_INF = -40.0;

    // Tabelas e colunas
    public static final String T_USERS = "users";
    public static final String C_USER_ID = "id";
    public static final String C_USER_UID = "firebase_uid";
    public static final String C_USER_EMAIL = "email";
    public static final String C_USER_NAME = "name";

    public static final String T_LEITURAS = "leituras";
    public static final String C_LEITURA_ID = "id";
    public static final String C_LEITURA_DATA = "data";
    public static final String C_LEITURA_VALOR = "valor_kwh";
    public static final String C_LEITURA_IMAGEM_PATH = "imagem_path";
    public static final String C_LEITURA_PREV_ID = "prev_leitura_id";
    public static final String C_LEITURA_CONSUMO_PERIODO = "consumo_periodo";
    public static final String C_LEITURA_CREATED_AT_TS = "created_at_ts";

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

    public DBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USERS + " (" +
                C_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_USER_UID + " TEXT UNIQUE, " +
                C_USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
                C_USER_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + T_LEITURAS + " (" +
                C_LEITURA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_LEITURA_DATA + " TEXT NOT NULL, " +
                C_LEITURA_VALOR + " REAL NOT NULL, " +
                C_LEITURA_IMAGEM_PATH + " TEXT, " +
                C_LEITURA_PREV_ID + " INTEGER, " +
                C_LEITURA_CONSUMO_PERIODO + " REAL, " +
                C_LEITURA_CREATED_AT_TS + " INTEGER, " +
                "FOREIGN KEY (" + C_LEITURA_PREV_ID + ") REFERENCES " + T_LEITURAS + "(" + C_LEITURA_ID + ") ON DELETE SET NULL" +
                ")");
        db.execSQL("CREATE TABLE " + T_MEDIA_CONSUMOS + " (" +
                C_MEDIA_NPERIODOS + " INTEGER PRIMARY KEY, " +
                C_MEDIA_VALOR + " REAL NOT NULL, " +
                C_MEDIA_ATUALIZADA_EM + " TEXT)");
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
                "FOREIGN KEY (" + C_CONSUMO_ANALISADO_LEITURA_ID + ") REFERENCES " + T_LEITURAS + "(" + C_LEITURA_ID + ") ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_leituras_prev ON " + T_LEITURAS + "(" + C_LEITURA_PREV_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_leituras_created_ts ON " + T_LEITURAS + "(" + C_LEITURA_CREATED_AT_TS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_consumos_analisados_leitura ON " + T_CONSUMOS_ANALISADOS + "(" + C_CONSUMO_ANALISADO_LEITURA_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_consumos_analisados_created ON " + T_CONSUMOS_ANALISADOS + "(" + C_CONSUMO_ANALISADO_CREATED_AT_TS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_consumos_analisados_status ON " + T_CONSUMOS_ANALISADOS + "(" + C_CONSUMO_ANALISADO_STATUS + ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS mensagens_suporte (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assunto TEXT," +
                "mensagem TEXT," +
                "data TEXT)");
        db.execSQL("CREATE TABLE assistencias (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "data TEXT, " +
                "hora TEXT, " +
                "descricao TEXT, " +
                "feedback TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + T_LEITURAS);
        db.execSQL("DROP TABLE IF EXISTS " + T_MEDIA_CONSUMOS);
        db.execSQL("DROP TABLE IF EXISTS " + T_CONSUMOS_ANALISADOS);
        db.execSQL("DROP TABLE IF EXISTS mensagens_suporte");
        db.execSQL("DROP TABLE IF EXISTS assistencias");
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ---------- UTILIZADORES ----------
    public long saveOrUpdateUser(String firebaseUid, String email, String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USER_UID, firebaseUid);
        cv.put(C_USER_EMAIL, email);
        cv.put(C_USER_NAME, name);
        long existingId = getUserIdByUid(firebaseUid);
        if (existingId > 0) {
            db.update(T_USERS, cv, C_USER_UID + "=?", new String[]{firebaseUid});
            return existingId;
        } else {
            return db.insert(T_USERS, null, cv);
        }
    }

    public long getUserIdByUid(String uid) {
        Cursor c = getReadableDatabase().query(T_USERS, new String[]{C_USER_ID},
                C_USER_UID + "=?", new String[]{uid}, null, null, null);
        try {
            if (c.moveToFirst()) return c.getLong(0);
            return -1;
        } finally {
            c.close();
        }
    }

    // ---------- LEITURAS ----------
    public long inserirLeituraComFoto(String data, double valorKwh, String imagemPath) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            double mediaAntes = calcularMediaConsumosInterno(db, 6);

            long prevId = -1;
            double prevValor = -1;
            Cursor c = db.rawQuery("SELECT " + C_LEITURA_ID + ", " + C_LEITURA_VALOR +
                    " FROM " + T_LEITURAS + " ORDER BY " + C_LEITURA_ID + " DESC LIMIT 1", null);
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
            if (prevId != -1) cv.put(C_LEITURA_PREV_ID, prevId);
            if (consumoPeriodo != null) cv.put(C_LEITURA_CONSUMO_PERIODO, consumoPeriodo);
            cv.put(C_LEITURA_CREATED_AT_TS, System.currentTimeMillis());

            long rowId = db.insert(T_LEITURAS, null, cv);

            if (rowId != -1 && consumoPeriodo != null) {
                criarRegistroConsumoAnalisado(db, rowId, consumoPeriodo, 6, mediaAntes);
                recalcularEMedia(db, new int[]{1, 3, 6});
            }

            db.setTransactionSuccessful();
            return rowId;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor obterLeituras() {
        return getReadableDatabase().query(T_LEITURAS, null, null, null, null, null,
                C_LEITURA_ID + " DESC");
    }

    public void apagarLeitura(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(T_LEITURAS, C_LEITURA_ID + "=?", new String[]{String.valueOf(id)});
            recalcularEMedia(db, new int[]{1, 3, 6});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int contarLeituras() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + T_LEITURAS, null);
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

    private double calcularMediaConsumosInterno(SQLiteDatabase db, int numPeriodos) {
        if (numPeriodos <= 0) return 0.0;

        Cursor c = null;
        try {
            // Pegar as últimas (numPeriodos + 1) leituras cumulativas (id DESC para consistência com UI)
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

            // Ler valores das leituras (mais recente primeiro)
            double[] leituras = new double[n];
            int idx = 0;
            do {
                leituras[idx++] = c.getDouble(0);
            } while (c.moveToNext());
            c.close();
            c = null;

            // Calcular consumos (diferenças) até numPeriodos
            double somaConsumos = 0.0;
            int contPeriodos = 0;
            for (int i = 0; i < leituras.length - 1 && contPeriodos < numPeriodos; i++) {
                double atual = leituras[i];
                double anterior = leituras[i + 1];
                double consumo = atual - anterior;
                if (consumo > 0) { // ignora zeros e negativos
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
            db.insertWithOnConflict(T_MEDIA_CONSUMOS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public Map<Integer, Double> obterMediasArmazenadas() {
        Map<Integer, Double> medias = new HashMap<>();
        Cursor c = getReadableDatabase().query(T_MEDIA_CONSUMOS,
                new String[]{C_MEDIA_NPERIODOS, C_MEDIA_VALOR}, null, null, null, null, null);
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
        Cursor c = db.query(T_MEDIA_CONSUMOS,
                new String[]{C_MEDIA_NPERIODOS, C_MEDIA_VALOR}, null, null, null, null, null);
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

    private void criarRegistroConsumoAnalisado(SQLiteDatabase db, long leituraId,
                                               double consumoValor, int numPeriodos, Double mediaRefParam) {
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

    public Cursor obterAnaliseConsumo(long leituraId) {
        return getReadableDatabase().query(T_CONSUMOS_ANALISADOS, null,
                C_CONSUMO_ANALISADO_LEITURA_ID + "=?",
                new String[]{String.valueOf(leituraId)}, null, null, null);
    }

    public Cursor obterHistoricoConsumosAnalisados(String status) {
        String selection = status != null ? C_CONSUMO_ANALISADO_STATUS + "=?" : null;
        String[] selectionArgs = status != null ? new String[]{status} : null;

        return getReadableDatabase().query(
                T_CONSUMOS_ANALISADOS + " ca JOIN " + T_LEITURAS + " l ON ca." +
                        C_CONSUMO_ANALISADO_LEITURA_ID + " = l." + C_LEITURA_ID,
                new String[]{"ca.*", "l." + C_LEITURA_DATA + " as data_leitura",
                        "l." + C_LEITURA_VALOR + " as valor_leitura"},
                selection, selectionArgs, null, null,
                "ca." + C_CONSUMO_ANALISADO_CREATED_AT_TS + " DESC");
    }

    public double obterUltimaLeituraOuDefault(double defaultValor) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + C_LEITURA_VALOR +
                " FROM " + T_LEITURAS + " ORDER BY " + C_LEITURA_ID + " DESC LIMIT 1", null);
        double valor = defaultValor;
        if (c.moveToFirst()) {
            valor = c.getDouble(0);
        }
        c.close();
        return valor;
    }
    // Inserir mensagem
    public boolean inserirMensagem(String assunto, String mensagem, String data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("assunto", assunto);
        cv.put("mensagem", mensagem);
        cv.put("data", data);
        long r = db.insert("mensagens_suporte", null, cv);
        return r != -1;
    }

    // Listar mensagens
    public Cursor listarMensagens() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM mensagens_suporte ORDER BY id DESC", null);
    }

    // Inserir assistência técnica
    public boolean inserirAssistencia(String data, String hora, String descricao) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("data", data);
        cv.put("hora", hora);
        cv.put("descricao", descricao);
        cv.put("feedback", "Pendente");
        long r = db.insert("assistencias", null, cv);
        return r != -1;
    }

    // Listar assistências + feedback
    public Cursor listarAssistencias() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM assistencias ORDER BY id DESC", null);
    }

}