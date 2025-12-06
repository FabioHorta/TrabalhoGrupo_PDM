package pt.ubi.pdm.ecotrack.models;

public class UpdatePrecoRequest {
    public String email;
    public double preco_kwh;

    public UpdatePrecoRequest(String email, double preco_kwh) {
        this.email = email;
        this.preco_kwh = preco_kwh;
    }
}
