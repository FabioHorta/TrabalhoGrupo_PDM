package pt.ubi.pdm.ecotrack.models;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    private int id;
    private String email;
    private String name;
    private Double preco_kwh;
    private String tipo;

    private String token;

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Double getPreco_kwh() { return preco_kwh; }
    public String getTipo() { return tipo; }
    public String getToken() { return token; } 
}

