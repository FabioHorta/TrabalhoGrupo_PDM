package pt.ubi.pdm.ecotrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ecotrack.db";
    private static final int DB_VERSION = 1;

    public static final String T_USERS = "users";
    public static final String C_USER_ID = "id";
    public static final String C_USER_UID = "firebase_uid";
    public static final String C_USER_EMAIL = "email";
    public static final String C_USER_NAME = "name";


    public static final String T_LEITURAS = "leituras";
    public static final String C_LEITURA_ID = "id";
    public static final String C_LEITURA_DATA = "data";
    public static final String C_LEITURA_VALOR = "valor_kwh";


    // Tabela de leituras com foto
    public static final String T_LEITURAS_FOTO = "leituras_foto";
    public static final String C_LF_ID = "id";
    public static final String C_LF_DATA = "data";
    public static final String C_LF_VALOR = "valor_kwh";
    public static final String C_LF_IMAGEM_PATH = "imagem_path";

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

        db.execSQL(
                "CREATE TABLE " + T_LEITURAS + " (" +
                        C_LEITURA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        C_LEITURA_DATA + " TEXT NOT NULL, " +
                        C_LEITURA_VALOR + " REAL NOT NULL" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE " + T_LEITURAS_FOTO + " (" +
                        C_LF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        C_LF_DATA + " TEXT NOT NULL, " +
                        C_LF_VALOR + " REAL NOT NULL, " +
                        C_LF_IMAGEM_PATH + " TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
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


    // Inserir uma nova leitura
    public long inserirLeitura(String data, double valorKwh) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_LEITURA_DATA, data);
        cv.put(C_LEITURA_VALOR, valorKwh);
        return db.insert(T_LEITURAS, null, cv);
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
    // CORREÇÃO: Agora usa a tabela T_LEITURAS_FOTO para calcular os consumos
    public double calcularMediaConsumos(int numPeriodos) {
        SQLiteDatabase db = getReadableDatabase();

        // Para ter N períodos, precisamos de N+1 leituras
        int limiteLeituras = numPeriodos + 1;


        Cursor c = db.rawQuery(
                "SELECT " + C_LF_VALOR +
                        " FROM " + T_LEITURAS_FOTO +
                        " ORDER BY " + C_LF_ID + " DESC " +
                        " LIMIT ?",
                new String[]{String.valueOf(limiteLeituras)}
        );

        if (!c.moveToFirst()) {
            c.close();
            return 0;
        }

        // Guardar as leituras num array
        double[] leituras = new double[c.getCount()];
        int idx = 0;
        do {
            leituras[idx++] = c.getDouble(0);
        } while (c.moveToNext());
        c.close();

        if (leituras.length < 2) {
            return 0;
        }

        double somaConsumos = 0;
        int contPeriodos = 0;

        // Calcular as diferenças (Consumo = Leitura Atual - Leitura Anterior)
        for (int i = 0; i < leituras.length - 1; i++) {
            double atual = leituras[i];
            double anterior = leituras[i + 1];

            if (atual >= anterior) {
                double consumo = atual - anterior;
                somaConsumos += consumo;
                contPeriodos++;
            }
        }

        if (contPeriodos == 0) return 0;

        // Se pedimos 1 período (para a calculadora), devolve o consumo exato desse mês
        if (numPeriodos == 1) {
            return somaConsumos;
        }

        // Senão, devolve a média
        return somaConsumos / contPeriodos;
    }

    // Obter a última leitura (para mostrar como leitura anterior)
    // CORREÇÃO: Agora vai buscar à tabela T_LEITURAS_FOTO
    public double obterUltimaLeituraOuDefault(double defaultValor) {
        SQLiteDatabase db = getReadableDatabase();

        // Seleciona o valor da tabela de fotos, ordenado pelo ID decrescente (o último inserido)
        Cursor c = db.rawQuery(
                "SELECT " + C_LF_VALOR +
                        " FROM " + T_LEITURAS_FOTO +
                        " ORDER BY " + C_LF_ID + " DESC " +
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
        cv.put(C_LF_DATA, data);
        cv.put(C_LF_VALOR, valorKwh);
        cv.put(C_LF_IMAGEM_PATH, imagemPath);
        return db.insert(T_LEITURAS_FOTO, null, cv);
    }

    // apaga todos (por exemplo, num logout)
    public void clearUsers() {
        getWritableDatabase().delete(T_USERS, null, null);
    }

    // Método para obter todas as leituras (para o histórico)
    // Adiciona isto no final da classe DBHelper
    public android.database.Cursor obterLeituras() {
        android.database.sqlite.SQLiteDatabase db = getReadableDatabase();
        // Seleciona tudo da tabela de fotos, ordenado pela data (mais recente primeiro)
        return db.query(
                T_LEITURAS_FOTO,
                null,
                null,
                null,
                null,
                null,
                C_LF_ID + " DESC"
        );
    }

    // Método para apagar uma leitura específica pelo ID
    public void apagarLeitura(long id) {
        android.database.sqlite.SQLiteDatabase db = getWritableDatabase();
        // Apaga da tabela de fotos onde o ID for igual ao passado
        db.delete(T_LEITURAS_FOTO, C_LF_ID + "=?", new String[]{String.valueOf(id)});

        // (Opcional) Se quiseres apagar também da tabela simples de leituras, descomenta:
        // db.delete(T_LEITURAS, C_LEITURA_ID + "=?", new String[]{String.valueOf(id)});
    }
}


