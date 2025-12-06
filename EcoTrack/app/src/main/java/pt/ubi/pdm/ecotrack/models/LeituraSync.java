package pt.ubi.pdm.ecotrack.models;

public class LeituraSync {
    public long id;
    public Integer casa_id;          // <- NOVO
    public String data;
    public double valor_kwh;
    public String imagem_path;
    public Long prev_leitura_id;
    public Double consumo_periodo;
    public Long created_at_ts;

    public LeituraSync() {}

    public LeituraSync(long id,
                       Integer casa_id,
                       String data,
                       double valor_kwh,
                       String imagem_path,
                       Long prev_leitura_id,
                       Double consumo_periodo,
                       Long created_at_ts) {

        this.id = id;
        this.casa_id = casa_id;
        this.data = data;
        this.valor_kwh = valor_kwh;
        this.imagem_path = imagem_path;
        this.prev_leitura_id = prev_leitura_id;
        this.consumo_periodo = consumo_periodo;
        this.created_at_ts = created_at_ts;
    }
}
