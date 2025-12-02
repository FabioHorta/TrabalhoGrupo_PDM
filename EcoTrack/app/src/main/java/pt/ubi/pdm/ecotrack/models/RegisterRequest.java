package pt.ubi.pdm.ecotrack.models;

public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    private String nif;
    private Double preco_kwh;
    private String tipo;

    public RegisterRequest(String email, String password, String name, String nif, Double preco_kwh, String tipo) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nif = nif;
        this.preco_kwh = preco_kwh;
        this.tipo = tipo;
    }

}
