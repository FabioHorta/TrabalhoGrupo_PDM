package pt.ubi.pdm.ecotrack;

/**
 * Classe singleton para gerir a casa selecionada globalmente
 * Permite que toda a app saiba qual é a casa atual sem ter de passar por extras
 */
public class CasaSelecionada {
    private static CasaSelecionada instancia;
    private int casaId = -1;
    private String casaNome = "";
    private String userEmail = "";

    private CasaSelecionada() {}

    public static CasaSelecionada getInstance() {
        if (instancia == null) {
            instancia = new CasaSelecionada();
        }
        return instancia;
    }

    public void setSelecionada(int id, String nome, String email) {
        this.casaId = id;
        this.casaNome = nome;
        this.userEmail = email;
    }

    public void limpar() {
        this.casaId = -1;
        this.casaNome = "";
        this.userEmail = "";
    }

    public int getCasaId() {
        return casaId;
    }

    public String getCasaNome() {
        return casaNome;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public boolean temCasaSelecionada() {
        return casaId > 0;
    }
}