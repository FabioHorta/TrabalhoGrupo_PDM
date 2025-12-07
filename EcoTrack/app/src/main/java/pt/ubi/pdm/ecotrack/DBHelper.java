package pt.ubi.pdm.ecotrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import pt.ubi.pdm.ecotrack.models.DicasResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {

    // =========================================================
    // CONFIGURAÇÃO DA BASE DE DADOS
    // =========================================================
    private static final String DB_NAME = "ecotrack.db";
    // ++ NOVO: subir versão porque alterámos a tabela de chat
    private static final int DB_VERSION = 7;

    // Limites para análise de consumo (% acima/abaixo da média)
    public static final double LIMITE_PERCENTUAL_SUP = 40.0;
    public static final double LIMITE_PERCENTUAL_INF = -40.0;

    // =========================================================
    // TABELA USERS
    // =========================================================
    public static final String T_USERS = "users";
    public static final String C_USER_ID = "id";
    public static final String C_USER_UID = "firebase_uid";   // aqui guardas o idServidor
    public static final String C_USER_EMAIL = "email";
    public static final String C_USER_NAME = "name";
    public static final String C_USER_PRECO_KWH = "preco_kwh";
    public static final String C_USER_TIPO = "tipo";          // cliente / tecnico
    public static final String C_USER_PASSWORD_HASH = "password_hash"; // hash para login offline

    // =========================================================
    // TABELA LEITURAS
    // =========================================================
    public static final String T_LEITURAS = "leituras";
    public static final String C_LEITURA_ID = "id";
    public static final String C_LEITURA_DATA = "data";
    public static final String C_LEITURA_VALOR = "valor_kwh";
    public static final String C_LEITURA_IMAGEM_PATH = "imagem_path";
    public static final String C_LEITURA_PREV_ID = "prev_leitura_id";
    public static final String C_LEITURA_CONSUMO_PERIODO = "consumo_periodo";
    public static final String C_LEITURA_CREATED_AT_TS = "created_at_ts";
    public static final String C_LEITURA_SYNC_STATUS = "sync_status";  // 0 = por sincronizar, 1 = já enviado
    // casa_id é coluna adicional nesta tabela (leituras associadas a casa)

    // =========================================================
    // TABELA MÉDIA DE CONSUMOS
    // =========================================================
    public static final String T_MEDIA_CONSUMOS = "media_consumos";
    public static final String C_MEDIA_NPERIODOS = "num_periodos";
    public static final String C_MEDIA_VALOR = "media_valor";
    public static final String C_MEDIA_ATUALIZADA_EM = "atualizada_em";

    // =========================================================
    // TABELA CONSUMOS ANALISADOS
    // =========================================================
    public static final String T_CONSUMOS_ANALISADOS = "consumos_analisados";
    public static final String C_CONSUMO_ANALISADO_ID = "id";
    public static final String C_CONSUMO_ANALISADO_LEITURA_ID = "leitura_id";
    public static final String C_CONSUMO_ANALISADO_VALOR = "consumo_valor";
    public static final String C_CONSUMO_ANALISADO_MEDIA_REF = "media_referencia";
    public static final String C_CONSUMO_ANALISADO_NUM_PERIODOS = "num_periodos";
    public static final String C_CONSUMO_ANALISADO_PERCENTAGEM = "percentagem_diferenca";
    public static final String C_CONSUMO_ANALISADO_STATUS = "status"; // NORMAL/ALTO/BAIXO
    public static final String C_CONSUMO_ANALISADO_LIMITE_SUP = "limite_superior";
    public static final String C_CONSUMO_ANALISADO_LIMITE_INF = "limite_inferior";
    public static final String C_CONSUMO_ANALISADO_CREATED_AT_TS = "created_at_ts";

    // =========================================================
    // TABELA CASAS
    // =========================================================
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

    // =========================================================
    // TABELA ELETRODOMÉSTICOS (APPLIANCES)
    // =========================================================
    public static final String T_ELETRODOMESTICOS = "appliances";
    public static final String C_APP_ID = "id";
    public static final String C_APP_CASA_ID = "casa_id";
    public static final String C_APP_NOME = "nome";
    public static final String C_APP_CATEGORIA = "categoria";
    public static final String C_APP_QUANTIDADE = "quantidade";
    public static final String C_APP_TIPO = "tipo";
    public static final String C_APP_CLASSE = "classe";

    // =========================================================
    // TABELA MENSAGENS CHAT (UTILIZADOR <-> TÉCNICO)
    // =========================================================
    public static final String T_MENSAGENS_CHAT = "mensagens_chat";
    public static final String C_MSG_ID = "id";
    public static final String C_MSG_REMETENTE = "remetente_email";
    public static final String C_MSG_DESTINATARIO = "destinatario_email";
    public static final String C_MSG_TEXTO = "texto";
    public static final String C_MSG_TS = "timestamp";
    // ++ NOVO:
    public static final String C_MSG_SYNC_STATUS = "sync_status"; // 0 = local, 1 = já no servidor

    // =========================================================
    // TABELA TÉCNICOS
    // =========================================================
    public static final String T_TECNICOS = "tecnicos";
    public static final String C_TEC_ID = "id";
    public static final String C_TEC_EMAIL = "email";
    public static final String C_TEC_NOME = "nome";

    // =========================================================
    // TABELA CACHE DICAS DE ALERTAS
    // =========================================================
    public static final String T_ALERTAS_DICAS_CACHE = "alertas_dicas_cache";
    public static final String C_ALERTA_TIPO = "tipo";
    public static final String C_ALERTA_TITULO = "titulo";
    public static final String C_ALERTA_MENSAGEM = "mensagem";
    public static final String C_ALERTA_DICA1 = "dica1";
    public static final String C_ALERTA_DICA2 = "dica2";
    public static final String C_ALERTA_DICA3 = "dica3";

    // =========================================================
    // CONSTRUTOR
    // =========================================================
    public DBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    // =========================================================
    // onCreate
    // =========================================================
    @Override
    public void onCreate(SQLiteDatabase db) {

        // ---------- USERS ----------
        db.execSQL("CREATE TABLE " + T_USERS + " (" +
                C_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_USER_UID + " TEXT UNIQUE, " +
                C_USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
                C_USER_NAME + " TEXT, " +
                C_USER_PRECO_KWH + " REAL , " +
                C_USER_PASSWORD_HASH + " TEXT, " +
                C_USER_TIPO + " TEXT DEFAULT 'cliente'" +
                ")"
        );

        // ---------- CASAS ----------
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

        // ---------- LEITURAS ----------
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

        // ---------- MÉDIA DE CONSUMOS ----------
        db.execSQL("CREATE TABLE " + T_MEDIA_CONSUMOS + " (" +
                C_MEDIA_NPERIODOS + " INTEGER PRIMARY KEY, " +
                C_MEDIA_VALOR + " REAL NOT NULL, " +
                C_MEDIA_ATUALIZADA_EM + " TEXT" +
                ")");

        // ---------- CONSUMOS ANALISADOS ----------
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

        // ---------- ELETRODOMÉSTICOS ----------
        db.execSQL("CREATE TABLE " + T_ELETRODOMESTICOS + " (" +
                C_APP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_APP_CASA_ID + " INTEGER NOT NULL, " +
                C_APP_NOME + " TEXT, " +
                C_APP_CATEGORIA + " TEXT, " +
                C_APP_CLASSE + " TEXT, " +
                "FOREIGN KEY (" + C_APP_CASA_ID + ") REFERENCES " +
                T_CASAS + "(" + C_CASA_ID + ") ON DELETE CASCADE" +
                ")");

        // ---------- MENSAGENS DE SUPORTE ----------
        db.execSQL("CREATE TABLE IF NOT EXISTS mensagens_suporte (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assunto TEXT," +
                "mensagem TEXT," +
                "data TEXT" +
                ")");

        // ---------- ASSISTÊNCIAS ----------
        db.execSQL("CREATE TABLE assistencias (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "data TEXT, " +
                "hora TEXT, " +
                "descricao TEXT, " +
                "feedback TEXT, " +
                "tecnico_email TEXT, " +
                "server_id INTEGER" +
                ")");


        // ---------- CHAT ----------
        db.execSQL("CREATE TABLE " + T_MENSAGENS_CHAT + " (" +
                C_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_MSG_REMETENTE + " TEXT NOT NULL, " +
                C_MSG_DESTINATARIO + " TEXT NOT NULL, " +
                C_MSG_TEXTO + " TEXT NOT NULL, " +
                C_MSG_TS + " INTEGER, " +
                C_MSG_SYNC_STATUS + " INTEGER DEFAULT 0" +
                ")");

        // ---------- TÉCNICOS ----------
        db.execSQL("CREATE TABLE " + T_TECNICOS + " (" +
                C_TEC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_TEC_EMAIL + " TEXT NOT NULL, " +
                C_TEC_NOME + " TEXT" +
                ")");

        // ---------- CACHE DICAS ALERTAS ----------  // NOVO
        db.execSQL("CREATE TABLE " + T_ALERTAS_DICAS_CACHE + " (" +
                C_ALERTA_TIPO + " TEXT PRIMARY KEY, " +
                C_ALERTA_TITULO + " TEXT, " +
                C_ALERTA_MENSAGEM + " TEXT, " +
                C_ALERTA_DICA1 + " TEXT, " +
                C_ALERTA_DICA2 + " TEXT, " +
                C_ALERTA_DICA3 + " TEXT" +
                ")");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS dicas_cache (" +
                        "tipo TEXT PRIMARY KEY," +
                        "titulo TEXT," +
                        "mensagem TEXT," +
                        "dica1 TEXT," +
                        "dica2 TEXT," +
                        "dica3 TEXT" +
                        ")"
        );

        inserirDicaDefault(db,
                "alto",
                "Consumo elevado",
                "O seu consumo recente está bastante acima da média.",
                "Verifique se deixou algum equipamento ligado mais tempo.",
                "Considere reduzir o uso de aquecimento/ar condicionado.",
                "Veja no mapa de gastos quais os dias com maior consumo."
        );

        inserirDicaDefault(db,
                "baixo",
                "Bom desempenho energético",
                "O seu consumo recente está abaixo da média. Excelente!",
                "Mantenha estes hábitos de poupança.",
                "Veja se consegue manter o mesmo padrão nos próximos meses.",
                "Compare o consumo com outras casas no histórico."
        );

        inserirDicaDefault(db,
                "normal",
                "Consumo estável",
                "O seu consumo está dentro da média.",
                "Mantenha um uso responsável dos equipamentos.",
                "Veja que eletrodomésticos consomem mais.",
                "Considere trocar equipamentos antigos por outros mais eficientes."
        );

        inserirDicaDefault(db,
                "inicio",
                "Ainda a reunir dados",
                "Precisamos de mais leituras para analisar o seu consumo.",
                "Introduza leituras regulares do contador.",
                "Tente registar pelo menos uma leitura por mês.",
                "Volte a este ecrã depois de ter mais leituras."
        );

        // ---------- INDEXES ----------
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

    // =========================================================
    // onUpgrade
    // =========================================================
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
        db.execSQL("DROP TABLE IF EXISTS " + T_TECNICOS);
        db.execSQL("DROP TABLE IF EXISTS " + T_ALERTAS_DICAS_CACHE);
        onCreate(db);
    }

    // =========================================================
    // FOREIGN KEYS
    // =========================================================
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // =========================================================
    // SECÇÃO: TÉCNICOS
    // =========================================================

    public void inserirTecnico(String email, String nome) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_TEC_EMAIL, email);
        cv.put(C_TEC_NOME, nome);
        db.insert(T_TECNICOS, null, cv);
    }

    public boolean existemTecnicosLocais() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + T_TECNICOS,
                null
        );
        try {
            if (c.moveToFirst()) {
                return c.getInt(0) > 0;
            }
            return false;
        } finally {
            c.close();
        }
    }

    // =========================================================
    // SECÇÃO: UTILIZADORES
    // =========================================================

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
            double precoInicial = (precoKwh != null && precoKwh > 0) ? precoKwh : 0.20;
            cv.put(C_USER_PRECO_KWH, precoInicial);
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

    public int atualizarPrecoUtilizador(String email, double preco) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USER_PRECO_KWH, preco);
        return db.update(T_USERS, cv, C_USER_EMAIL + " = ?", new String[]{email});
    }

    public Cursor obterDadosUtilizadorPorEmail(String email) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + T_USERS +
                        " WHERE " + C_USER_EMAIL + " = ?",
                new String[]{email}
        );
    }

    // =========================================================
    // SECÇÃO: LEITURAS (por casa)
    // =========================================================

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
            cv.put(C_LEITURA_SYNC_STATUS, 0); // por sincronizar

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

    // =========================================================
    // SECÇÃO: MÉDIAS DE CONSUMO
    // =========================================================

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

    /**
     * Actualiza o nome de um utilizador (técnico ou cliente) pelo email.
     */
    public int atualizarNomeUtilizador(String email, String novoNome) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USER_NAME, novoNome);
        return db.update(
                T_USERS,
                cv,
                C_USER_EMAIL + " = ?",
                new String[]{email}
        );
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
            // neste momento só calculas, não guardas por casa
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

    // =========================================================
    // SECÇÃO: ANÁLISE DE CONSUMO
    // =========================================================

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
                            " ORDER BY " + C_LEITURA_ID + " DESC LIMIT 1",
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

    // =========================================================
    // SECÇÃO: SUPORTE
    // =========================================================

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

    // =========================================================
    // SECÇÃO: ASSISTÊNCIAS
    // =========================================================

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
                "SELECT " + C_TEC_EMAIL + " AS " + C_USER_EMAIL +
                        " FROM " + T_TECNICOS,
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

    // =========================================================
    // SECÇÃO: CASAS
    // =========================================================

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
            int existingId = -1;
            Cursor c = db.rawQuery(
                    "SELECT " + C_CASA_ID +
                            " FROM " + T_CASAS +
                            " WHERE " + C_CASA_USER_EMAIL + " = ? AND " + C_CASA_NOME + " = ?",
                    new String[]{email, nome}
            );
            try {
                if (c.moveToFirst()) {
                    existingId = c.getInt(0);
                }
            } finally {
                c.close();
            }

            if (existingId != -1) {
                db.update(T_CASAS, cv, C_CASA_ID + "=?", new String[]{String.valueOf(existingId)});
                return existingId;
            } else {
                return (int) db.insert(T_CASAS, null, cv);
            }

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

    // =========================================================
    // SECÇÃO: ELETRODOMÉSTICOS
    // =========================================================

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
        db.insert(T_ELETRODOMESTICOS, null, cv);
    }

    public Cursor obterEletrodomesticosDaCasa(int casaId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_CASA_ID + " = ?",
                new String[]{String.valueOf(casaId)}
        );
    }

    public void removerUmEletrodomestico(int casaId, String nome) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_ID + " = (SELECT MAX(" + C_APP_ID + ") FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_CASA_ID + "=? AND " + C_APP_NOME + "=?)",
                new String[]{String.valueOf(casaId), nome});
    }

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

    public boolean existeEletrodomestico(int casaId, String nome) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM " + T_ELETRODOMESTICOS +
                        " WHERE " + C_APP_CASA_ID + " = ? AND " + C_APP_NOME + " = ? LIMIT 1",
                new String[]{String.valueOf(casaId), nome}
        );
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    // =========================================================
    // SECÇÃO: CHAT UTILIZADOR <-> TÉCNICO
    // =========================================================

    public boolean inserirMensagemChat(String remetenteEmail,
                                       String destinatarioEmail,
                                       String texto) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_MSG_REMETENTE, remetenteEmail);
        cv.put(C_MSG_DESTINATARIO, destinatarioEmail);
        cv.put(C_MSG_TEXTO, texto);
        cv.put(C_MSG_TS, System.currentTimeMillis());
        cv.put(C_MSG_SYNC_STATUS, 0); // mensagem criada localmente, por sincronizar
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

    /**
     * Inserir mensagem que vem do servidor (já com timestamp).
     * Fica marcada como sincronizada (sync_status = 1).
     */
    public boolean inserirMensagemChatComTs(String remetenteEmail,
                                            String destinatarioEmail,
                                            String texto,
                                            long ts) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_MSG_REMETENTE, remetenteEmail);
        cv.put(C_MSG_DESTINATARIO, destinatarioEmail);
        cv.put(C_MSG_TEXTO, texto);
        cv.put(C_MSG_TS, ts);
        cv.put(C_MSG_SYNC_STATUS, 1); // já vem do servidor
        long r = db.insert(T_MENSAGENS_CHAT, null, cv);
        return r != -1;
    }

    /**
     * Mensagens deste utilizador ainda por sincronizar com o servidor.
     */
    public Cursor listarMensagensPorSincronizar(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + T_MENSAGENS_CHAT +
                        " WHERE (" + C_MSG_REMETENTE + " = ? OR " + C_MSG_DESTINATARIO + " = ?) " +
                        " AND " + C_MSG_SYNC_STATUS + " = 0 " +
                        " ORDER BY " + C_MSG_TS + " ASC",
                new String[]{email, email}
        );
    }

    /**
     * Marca mensagens como sincronizadas (sync_status = 1).
     */
    public void marcarMensagensComoSincronizadas(long[] ids) {
        if (ids == null || ids.length == 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(C_MSG_SYNC_STATUS, 1);
            for (long id : ids) {
                db.update(
                        T_MENSAGENS_CHAT,
                        cv,
                        C_MSG_ID + " = ?",
                        new String[]{String.valueOf(id)}
                );
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Maior timestamp de mensagens deste utilizador (para pulls incrementais).
     */
    public long obterMaxTimestampMensagens(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT MAX(" + C_MSG_TS + ") AS max_ts FROM " + T_MENSAGENS_CHAT +
                        " WHERE " + C_MSG_REMETENTE + " = ? OR " + C_MSG_DESTINATARIO + " = ?",
                new String[]{email, email}
        );
        long max = 0L;
        if (c != null) {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndexOrThrow("max_ts");
                if (!c.isNull(idx)) {
                    max = c.getLong(idx);
                }
            }
            c.close();
        }
        return max;
    }


    /**
     * Verifica se já existe uma mensagem com o mesmo remetente,
     * destinatário e timestamp (para evitar duplicados ao sincronizar).
     */
    public boolean existeMensagemChatComTs(String remetenteEmail,
                                           String destinatarioEmail,
                                           long ts) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + T_MENSAGENS_CHAT +
                        " WHERE " + C_MSG_REMETENTE + " = ? " +
                        "   AND " + C_MSG_DESTINATARIO + " = ? " +
                        "   AND " + C_MSG_TS + " = ? " +
                        " LIMIT 1",
                new String[]{
                        remetenteEmail,
                        destinatarioEmail,
                        String.valueOf(ts)
                }
        );
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }


    // =========================================================
    // SECÇÃO: SYNC LEITURAS (LOCAL -> SERVIDOR)
    // =========================================================

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

    public long inserirLeituraRestaurada(int casaId,
                                         String data,
                                         double valorKwh,
                                         String imagemPath,
                                         Long prevLeituraId,
                                         Double consumoPeriodo,
                                         Long createdAtTs) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_LEITURA_DATA, data);
        cv.put(C_LEITURA_VALOR, valorKwh);
        cv.put(C_LEITURA_IMAGEM_PATH, imagemPath);
        cv.put("casa_id", casaId);

        if (prevLeituraId != null) {
            cv.put(C_LEITURA_PREV_ID, prevLeituraId);
        }
        if (consumoPeriodo != null) {
            cv.put(C_LEITURA_CONSUMO_PERIODO, consumoPeriodo);
        }
        if (createdAtTs != null) {
            cv.put(C_LEITURA_CREATED_AT_TS, createdAtTs);
        }

        cv.put(C_LEITURA_SYNC_STATUS, 1);

        return db.insert(T_LEITURAS, null, cv);
    }

    public boolean existemLeiturasLocais() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + T_LEITURAS,
                null
        );
        try {
            if (c.moveToFirst()) {
                return c.getInt(0) > 0;
            }
            return false;
        } finally {
            c.close();
        }
    }

    // =========================================================
    // SECÇÃO: CACHE OFFLINE DE DICAS DE ALERTAS
    // =========================================================

    public void guardarDicasCache(String tipo, DicasResponse d) {
        if (tipo == null || d == null) return;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_ALERTA_TIPO, tipo);
        cv.put(C_ALERTA_TITULO, d.titulo);
        cv.put(C_ALERTA_MENSAGEM, d.mensagem);
        cv.put(C_ALERTA_DICA1, d.dica1);
        cv.put(C_ALERTA_DICA2, d.dica2);
        cv.put(C_ALERTA_DICA3, d.dica3);

        db.insertWithOnConflict(
                T_ALERTAS_DICAS_CACHE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public DicasResponse obterDicasCache(String tipo) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT titulo, mensagem, dica1, dica2, dica3 " +
                        "FROM dicas_cache WHERE tipo = ?",
                new String[]{ tipo }
        );

        if (c != null && c.moveToFirst()) {
            DicasResponse d = new DicasResponse();
            d.titulo   = c.getString(0);
            d.mensagem = c.getString(1);
            d.dica1    = c.getString(2);
            d.dica2    = c.getString(3);
            d.dica3    = c.getString(4);
            c.close();
            return d;
        }

        if (c != null) c.close();
        return null;
    }

    public void atualizarAssistenciaServerId(long idLocal, long serverId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("server_id", serverId);
        db.update(
                "assistencias",
                cv,
                "id = ?",
                new String[]{ String.valueOf(idLocal) }
        );
    }

    public long obterServerIdDaAssistencia(long idLocal) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT server_id FROM assistencias WHERE id = ?",
                new String[]{ String.valueOf(idLocal) }
        );
        try {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndexOrThrow("server_id");
                if (!c.isNull(idx)) {
                    return c.getLong(idx);
                }
            }
            return -1;
        } finally {
            c.close();
        }
    }

    private void inserirDicaDefault(SQLiteDatabase db,
                                    String tipo,
                                    String titulo,
                                    String mensagem,
                                    String dica1,
                                    String dica2,
                                    String dica3) {
        ContentValues cv = new ContentValues();
        cv.put("tipo", tipo);
        cv.put("titulo", titulo);
        cv.put("mensagem", mensagem);
        cv.put("dica1", dica1);
        cv.put("dica2", dica2);
        cv.put("dica3", dica3);

        db.insertWithOnConflict("dicas_cache", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

}
