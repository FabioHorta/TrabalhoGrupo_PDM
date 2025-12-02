package pt.ubi.pdm.ecotrack.models;

public class AssistenciaSync {
    public long idLocal;
    public String data;
    public String hora;
    public String descricao;
    public String feedback;
    public String tecnico_email;

    public AssistenciaSync(long idLocal,
                           String data,
                           String hora,
                           String descricao,
                           String feedback,
                           String tecnico_email) {
        this.idLocal = idLocal;
        this.data = data;
        this.hora = hora;
        this.descricao = descricao;
        this.feedback = feedback;
        this.tecnico_email = tecnico_email;
    }
}
