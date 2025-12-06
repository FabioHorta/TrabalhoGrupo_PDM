package pt.ubi.pdm.ecotrack.models;

public class CasaSync {

    public long id;              // id (PRIMARY KEY na tabela casas)
    public String user_email;    // user_email
    public String nome_casa;     // nome_casa
    public String tipo;          // tipo
    public String uso;           // uso
    public int pessoas;          // pessoas
    public String ano;           // ano
    public String morada;        // morada
    public String distrito;      // distrito
    public String concelho;      // concelho
    public String freguesia;     // freguesia
    public String cod_postal;    // cod_postal

    public CasaSync(long id,
                    String user_email,
                    String nome_casa,
                    String tipo,
                    String uso,
                    int pessoas,
                    String ano,
                    String morada,
                    String distrito,
                    String concelho,
                    String freguesia,
                    String cod_postal) {

        this.id = id;
        this.user_email = user_email;
        this.nome_casa = nome_casa;
        this.tipo = tipo;
        this.uso = uso;
        this.pessoas = pessoas;
        this.ano = ano;
        this.morada = morada;
        this.distrito = distrito;
        this.concelho = concelho;
        this.freguesia = freguesia;
        this.cod_postal = cod_postal;
    }
}
