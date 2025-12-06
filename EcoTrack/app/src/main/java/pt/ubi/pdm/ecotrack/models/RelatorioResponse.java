package pt.ubi.pdm.ecotrack.models;

public class RelatorioResponse {
    public long id;
    public Long assistencia_id;
    public String tecnico_email;
    public String cliente_email;
    public String titulo;
    public String resumo;
    public String created_at;
    public String base64;   // PDF em Base64 vindo do servidor
}
