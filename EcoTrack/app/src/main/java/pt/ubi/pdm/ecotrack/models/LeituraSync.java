package pt.ubi.pdm.ecotrack.models;

public class LeituraSync {
    public long id;
    public Integer casa_id;
    public String data;
    public double valor_kwh;
    public String imagem_path;
    public String imagem_base64;   // <== NOVO
    public Long prev_leitura_id;
    public Double consumo_periodo;
    public Long created_at_ts;

    public LeituraSync(long id,
                       Integer casa_id,
                       String data,
                       double valor_kwh,
                       String imagem_path,
                       Long prev_leitura_id,
                       Double consumo_periodo,
                       Long created_at_ts) {
        this(id, casa_id, data, valor_kwh, imagem_path, null,
                prev_leitura_id, consumo_periodo, created_at_ts);
    }

    public LeituraSync(long id,
                       Integer casa_id,
                       String data,
                       double valor_kwh,
                       String imagem_path,
                       String imagem_base64,
                       Long prev_leitura_id,
                       Double consumo_periodo,
                       Long created_at_ts) {
        this.id = id;
        this.casa_id = casa_id;
        this.data = data;
        this.valor_kwh = valor_kwh;
        this.imagem_path = imagem_path;
        this.imagem_base64 = imagem_base64;
        this.prev_leitura_id = prev_leitura_id;
        this.consumo_periodo = consumo_periodo;
        this.created_at_ts = created_at_ts;
    }
}


