package pt.ubi.pdm.ecotrack.models;

public class LeituraSync {
    public long idLocal;
    public String data;
    public double valor_kwh;
    public String imagem_path;
    public Long prev_leitura_id;
    public Double consumo_periodo;
    public Long created_at_ts;

    public LeituraSync(long idLocal,
                       String data,
                       double valor_kwh,
                       String imagem_path,
                       Long prev_leitura_id,
                       Double consumo_periodo,
                       Long created_at_ts) {

        this.idLocal = idLocal;
        this.data = data;
        this.valor_kwh = valor_kwh;
        this.imagem_path = imagem_path;
        this.prev_leitura_id = prev_leitura_id;
        this.consumo_periodo = consumo_periodo;
        this.created_at_ts = created_at_ts;
    }
}
