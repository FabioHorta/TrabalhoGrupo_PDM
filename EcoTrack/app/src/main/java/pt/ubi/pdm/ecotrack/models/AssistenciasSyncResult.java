package pt.ubi.pdm.ecotrack.models;

import java.util.List;

public class AssistenciasSyncResult {
    public boolean ok;
    public List<Mapping> mappings;

    public static class Mapping {
        public long idLocal;
        public long serverId;
    }
}
