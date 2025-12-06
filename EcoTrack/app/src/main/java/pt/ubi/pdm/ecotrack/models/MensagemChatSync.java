package pt.ubi.pdm.ecotrack.models;

import com.google.gson.annotations.SerializedName;

public class MensagemChatSync {

    @SerializedName("id")
    public long id;

    @SerializedName("remetente_email")
    public String remetenteEmail;

    @SerializedName("destinatario_email")
    public String destinatarioEmail;

    @SerializedName("texto")
    public String texto;

    @SerializedName("timestamp")
    public long timestamp;

    // Construtor usado em SyncUtils
    public MensagemChatSync(long id,
                            String remetenteEmail,
                            String destinatarioEmail,
                            String texto,
                            long timestamp) {
        this.id = id;
        this.remetenteEmail = remetenteEmail;
        this.destinatarioEmail = destinatarioEmail;
        this.texto = texto;
        this.timestamp = timestamp;
    }

    public MensagemChatSync() {
    }
}
