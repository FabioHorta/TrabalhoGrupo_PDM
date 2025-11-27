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
    private static final int DB_VERSION = 2;

    //Tabela: Utilizadores
    public static final String T_USERS = "users";
    public static final String C_USER_ID = "id";
    public static final String C_USER_UID = "firebase_uid";
    public static final String C_USER_EMAIL = "email";
    public static final String C_USER_NAME = "name";

    // Tabela: Leituras
    public static final String T_LEITURAS = "leituras";
    public static final String C_LEITURA_ID = "id";
    public static final String C_LEITURA_DATA = "data";
    public static final String C_LEITURA_VALOR = "valor_kwh";
    public static final String C_LEITURA_IMAGEM_PATH = "imagem_path";

    // Tabela: Médias de Consumo
    public static final String T_MEDIA_CONSUMOS = "media_consumos";
    public static final String C_MEDIA_NPERIODOS = "num_periodos";
    public static final String C_MEDIA_VALOR = "media_valor";
    public static final String C_MEDIA_ATUALIZADA_EM = "atualizada_em";


    public DBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Criação da tabela dos utilizadores
        db.execSQL("CREATE TABLE " + T_USERS + " (" +
                C_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_USER_UID + " TEXT UNIQUE, " +
                C_USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
                C_USER_NAME + " TEXT)");

        //Criação da tabela das leituras
        db.execSQL(
                "CREATE TABLE " + T_LEITURAS + " (" +
                        C_LEITURA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        C_LEITURA_DATA + " TEXT NOT NULL, " +
                        C_LEITURA_VALOR + " REAL NOT NULL, " +
                        C_LEITURA_IMAGEM_PATH + " TEXT" +
                        ")"
        );

        //Criação da tabela das médias de consumo
        db.execSQL(
                "CREATE TABLE " + T_MEDIA_CONSUMOS + " (" +
                        C_MEDIA_NPERIODOS + " INTEGER PRIMARY KEY, " +
                        C_MEDIA_VALOR + " REAL NOT NULL, " +
                        C_MEDIA_ATUALIZADA_EM + " TEXT" +
                        ")"
        );
    }

    //apaga as tabelas da bd e torna a cria-las
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + T_MEDIA_CONSUMOS + " (" +
                    C_MEDIA_NPERIODOS + " INTEGER PRIMARY KEY, " +
                    C_MEDIA_VALOR + " REAL NOT NULL, " +
                    C_MEDIA_ATUALIZADA_EM + " TEXT)");
        }
        //onCreate(db);
    }

    // ---------- UTILIZADORES ----------

    // insere ou atualiza utilizadores vindos da Firebase
    public long saveOrUpdateUser(String firebaseUid, String email, String name) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(C_USER_UID, firebaseUid);
        cv.put(C_USER_EMAIL, email);
        cv.put(C_USER_NAME, name);

        // verifica se já existe (se existir devolve o id, senão existir devolve -1)
        long existingId = getUserIdByUid(firebaseUid);
        if (existingId > 0) {
            //faz update
            db.update(T_USERS, cv, C_USER_UID + "=?", new String[]{firebaseUid});
            return existingId;
        } else {
            //insere
            return db.insert(T_USERS, null, cv);
        }
    }

    //Procura e devolve o id de um utilizador
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

    // Calcular média dos consumos (diferenças) dos últimos N períodos
    public double calcularMediaConsumos(int numPeriodos) {
        return calcularMediaConsumosInterno(getReadableDatabase(), numPeriodos);
    }

    private double calcularMediaConsumosInterno(SQLiteDatabase db, int numPeriodos) {
        int limiteLeituras = numPeriodos + 1;

        Cursor c = db.rawQuery(
                "SELECT " + C_LEITURA_VALOR +
                        " FROM " + T_LEITURAS +
                        " ORDER BY " + C_LEITURA_ID + " DESC " +
                        " LIMIT ?",
                new String[]{String.valueOf(limiteLeituras)}
        );

        if (!c.moveToFirst()) {
            c.close();
            return 0;
        }

        double[] leituras = new double[c.getCount()];
        int idx = 0;
        do {
            leituras[idx++] = c.getDouble(0);
        } while (c.moveToNext());
        c.close();

        if (leituras.length < 2) return 0;

        double somaConsumos = 0;
        int contPeriodos = 0;

        for (int i = 0; i < leituras.length - 1; i++) {
            double atual = leituras[i];
            double anterior = leituras[i + 1];

            if (atual >= anterior) {
                somaConsumos += (atual - anterior);
                contPeriodos++;
            }
        }

        if (contPeriodos == 0) return 0;

        if (numPeriodos == 1) {
            return somaConsumos;
        }

        return somaConsumos / contPeriodos;
    }

    // Obter a última leitura (para mostrar como leitura anterior)
    public double obterUltimaLeituraOuDefault(double defaultValor) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + C_LEITURA_VALOR +
                        " FROM " + T_LEITURAS +
                        " ORDER BY " + C_LEITURA_ID + " DESC " +
                        " LIMIT 1",
                null
        );

        double valor = defaultValor;
        if (c.moveToFirst()) {
            valor = c.getDouble(0);
        }
        c.close();
        return valor;
    }

    //Inserir uma leitura com fotografia
    public long inserirLeituraComFoto(String data, double valorKwh, String imagemPath) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(C_LEITURA_DATA, data);
            cv.put(C_LEITURA_VALOR, valorKwh);
            cv.put(C_LEITURA_IMAGEM_PATH, imagemPath);
            long rowId = db.insert(T_LEITURAS, null, cv);

            // Recalcula e guarda médias para N = 1, 3, 6
            if (rowId != -1) {
                recalcularEMedia(db, new int[]{1, 3, 6});
            }

            db.setTransactionSuccessful();
            return rowId;
        } finally {
            db.endTransaction();
        }
    }

    // Recalcula e guarda médias para os períodos especificados
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

    // Obter todas as médias armazenadas
    public Map<Integer, Double> obterMediasArmazenadas() {
        Map<Integer, Double> medias = new HashMap<>();
        Cursor c = getReadableDatabase().query(
                T_MEDIA_CONSUMOS,
                new String[]{C_MEDIA_NPERIODOS, C_MEDIA_VALOR},
                null, null, null, null, null
        );
        try {
            while (c.moveToNext()) {
                int n = c.getInt(0);
                double media = c.getDouble(1);
                medias.put(n, media);
            }
        } finally {
            c.close();
        }
        return medias;
    }

    // Obter todas as leituras- Adiciona isto no final da classe DBHelper
    public Cursor obterLeituras() {
        SQLiteDatabase db = getReadableDatabase();
        // Últimas leituras, mais recente primeiro
        return db.query(
                T_LEITURAS,
                null,
                null,
                null,
                null,
                null,
                C_LEITURA_ID + " DESC"
        );
    }

    //Apagar uma leitura especifica a partir do id
    public void apagarLeitura(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(T_LEITURAS, C_LEITURA_ID + "=?", new String[]{String.valueOf(id)});
            // Recalcula médias após apagar
            recalcularEMedia(db, new int[]{1, 3, 6});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // apaga todos (por exemplo, num logout)
    public void clearUsers() {
        getWritableDatabase().delete(T_USERS, null, null);
    }
}