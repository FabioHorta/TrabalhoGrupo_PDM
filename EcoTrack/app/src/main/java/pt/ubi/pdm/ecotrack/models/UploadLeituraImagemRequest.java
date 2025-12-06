package pt.ubi.pdm.ecotrack.models;

public class UploadLeituraImagemRequest {
    private int casa_id;
    private String data;
    private double valor_kwh;
    private String imagem_base64;

    public UploadLeituraImagemRequest(int casa_id, String data, double valor_kwh, String imagem_base64) {
        this.casa_id = casa_id;
        this.data = data;
        this.valor_kwh = valor_kwh;
        this.imagem_base64 = imagem_base64;
    }
}
