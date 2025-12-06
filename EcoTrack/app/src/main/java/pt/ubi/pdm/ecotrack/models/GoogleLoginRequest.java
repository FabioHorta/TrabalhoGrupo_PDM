package pt.ubi.pdm.ecotrack.models;

public class GoogleLoginRequest {
    private String id_token;

    public GoogleLoginRequest(String id_token) {
        this.id_token = id_token;
    }
}
