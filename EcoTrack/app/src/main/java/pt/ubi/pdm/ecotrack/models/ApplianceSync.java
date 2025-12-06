package pt.ubi.pdm.ecotrack.models;

public class ApplianceSync {
    public int casa_id;
    public String nome;
    public String categoria;
    public String classe;

    public ApplianceSync(int casa_id, String nome, String categoria, String classe) {
        this.casa_id = casa_id;
        this.nome = nome;
        this.categoria = categoria;
        this.classe = classe;
    }
}
