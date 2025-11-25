package pt.ubi.pdm.ecotrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ecotrack.db";
    private static final int DB_VERSION = 1;

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

        // Tabela única de leituras (com foto opcional)
        db.execSQL(
                "CREATE TABLE " + T_LEITURAS + " (" +
                        C_LEITURA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        C_LEITURA_DATA + " TEXT NOT NULL, " +
                        C_LEITURA_VALOR + " REAL NOT NULL, " +
                        C_LEITURA_IMAGEM_PATH + " TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + T_LEITURAS);
        onCreate(db);
    }

    // ---------- USERS ----------

    // insere ou actualiza utilizador vindo do Firebase
    public long saveOrUpdateUser(String firebaseUid, String email, String name) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(C_USER_UID, firebaseUid);
        cv.put(C_USER_EMAIL, email);
        cv.put(C_USER_NAME, name);

        // já existe?
        long existingId = getUserIdByUid(firebaseUid);
        if (existingId > 0) {
            db.update(T_USERS, cv, C_USER_UID + "=?", new String[]{firebaseUid});
            return existingId;
        } else {
            return db.insert(T_USERS, null, cv);
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

    // exemplo simples para saber se temos algum utilizador guardado
    public boolean hasAnyUser() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + T_USERS, null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0) > 0;
            }
            return false;
        } finally {
            c.close();
        }
    }


    // Calcular média dos últimos N registos (por ordem de data)
    public double calcularMediaUltimasLeituras(int limite) {
        SQLiteDatabase db = getReadableDatabase();

        // Pega nos últimos N registos por ordem de inserção (id decrescente)
        Cursor c = db.rawQuery(
                "SELECT AVG(" + C_LEITURA_VALOR + ") FROM (" +
                        "SELECT " + C_LEITURA_VALOR +
                        " FROM " + T_LEITURAS +
                        " ORDER BY " + C_LEITURA_ID + " DESC " +
                        " LIMIT ?" +
                        ")",
                new String[]{String.valueOf(limite)}
        );

        double media = 0;
        if (c.moveToFirst()) {
            media = c.getDouble(0);
        }
        c.close();
        return media;
    }

    // Calcular média dos consumos (diferenças) dos últimos N períodos
// Ex.: se N = 6, usa as últimas 7 leituras para obter 6 diferenças.
    public double calcularMediaConsumos(int numPeriodos) {
        SQLiteDatabase db = getReadableDatabase();

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
            return somaConsumos;   // consumo do último período
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

    public long inserirLeituraComFoto(String data, double valorKwh, String imagemPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_LEITURA_DATA, data);
        cv.put(C_LEITURA_VALOR, valorKwh);
        cv.put(C_LEITURA_IMAGEM_PATH, imagemPath);
        return db.insert(T_LEITURAS, null, cv);
    }

    // apaga todos (por exemplo, num logout)
    public void clearUsers() {
        getWritableDatabase().delete(T_USERS, null, null);
    }

    // Método para obter todas as leituras (para o histórico)
    // Adiciona isto no final da classe DBHelper
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

    // Método para apagar uma leitura específica pelo ID
    public void apagarLeitura(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_LEITURAS, C_LEITURA_ID + "=?", new String[]{String.valueOf(id)});
    }
}


