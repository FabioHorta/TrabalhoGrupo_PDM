package pt.ubi.pdm.ecotrack.models;

import com.google.gson.annotations.SerializedName;

public class RelatorioCreateRequest {

    @SerializedName("assistencia_id")
    private Long assistenciaId;

    @SerializedName("tecnico_email")
    private String tecnicoEmail;

    @SerializedName("cliente_email")
    private String clienteEmail;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("resumo")
    private String resumo;

    @SerializedName("detalhes")
    private String detalhes;

    public RelatorioCreateRequest(Long assistenciaId,
                                  String tecnicoEmail,
                                  String clienteEmail,
                                  String titulo,
                                  String resumo,
                                  String detalhes) {
        this.assistenciaId = assistenciaId;
        this.tecnicoEmail = tecnicoEmail;
        this.clienteEmail = clienteEmail;
        this.titulo = titulo;
        this.resumo = resumo;
        this.detalhes = detalhes;
    }

}

