package pt.ubi.pdm.ecotrack.models;

import com.google.gson.annotations.SerializedName;

public class MensagemSuporteSync {

    @SerializedName("id")
    public long id;

    @SerializedName("assunto")
    public String assunto;

    @SerializedName("mensagem")
    public String mensagem;

    @SerializedName("data")
    public String data;

    // Construtor usado em SyncUtils
    public MensagemSuporteSync(long id, String assunto, String mensagem, String data) {
        this.id = id;
        this.assunto = assunto;
        this.mensagem = mensagem;
        this.data = data;
    }

    // Construtor vazio (obrigatório para Gson)
    public MensagemSuporteSync() {
    }
}
