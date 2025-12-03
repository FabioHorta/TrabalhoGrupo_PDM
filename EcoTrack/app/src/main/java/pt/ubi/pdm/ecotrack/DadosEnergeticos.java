package pt.ubi.pdm.ecotrack;

public class DadosEnergeticos {

    // Preço médio do kWh em Portugal (podes ajustar se quiseres ser mais preciso ou usar o do user)
    public static final double PRECO_KWH = 0.22;

    /**
     * Retorna o consumo MENSAL estimado (kWh) para cada tipo e classe.
     * Dados baseados em perfis médios (lógica EPREL simplificada).
     */
    public static double getConsumoMensal(String tipo, String classe) {
        // Se a classe vier nula ou vazia, assumimos "F" (pior cenário comum) por segurança
        if (classe == null || classe.isEmpty()) classe = "F";

        // Normalizar para maiúsculas para o switch funcionar (ex: "a" -> "A")
        String c = classe.toUpperCase();

        // COZINHA
        if (match(tipo, "Combinado")) return getFrigorificoCombinado(c);
        if (match(tipo, "Frigorífico") || match(tipo, "Frigorifico")) return getFrigorifico(c);
        if (match(tipo, "Forno")) return getForno(c);
        if (match(tipo, "Arca") || match(tipo, "Congelador")) return getArca(c);
        if (match(tipo, "Placa") || match(tipo, "Fogão")) return getPlaca(c);
        if (match(tipo, "Microondas")) return getMicroondas(c);
        if (match(tipo, "Ferro")) return getFerro(c);
        if (match(tipo, "Robot")) return getRobot(c);
        if (match(tipo, "Café") || match(tipo, "Cafeteira")) return getCafeteira(c);
        if (match(tipo, "Chaleira")) return getChaleira(c);

        // CLIMATIZAÇÃO
        if (match(tipo, "Ar Condic")) return getArCondicionado(c);
        if (match(tipo, "Radiador")) return getRadiador(c);
        if (match(tipo, "Aquecedor")) return getAquecedor(c);
        if (match(tipo, "Caldeira")) return getCaldeira(c);
        if (match(tipo, "Bomba Calor")) return getBombaCalor(c);
        if (match(tipo, "Desumidif")) return getDesumidificador(c);
        if (match(tipo, "Lareira")) return getLareira(c);
        if (match(tipo, "Piso")) return getPisoAquecido(c);

        // LAVAGENS
        if (match(tipo, "Loiça") || match(tipo, "Louça")) return getLouca(c);
        if (match(tipo, "Roupa") || match(tipo, "Lavar")) return getMaquinaLavar(c);
        if (match(tipo, "Secar")) return getMaquinaSecar(c);

        // ENTRETENIMENTO / ELETRÓNICA
        if (match(tipo, "Televisão") || match(tipo, "TV")) return getTelevisao(c);
        if (match(tipo, "Consola") || match(tipo, "PlayStation") || match(tipo, "Xbox")) return getConsola(c);
        if (match(tipo, "Computador") || match(tipo, "PC")) return getComputador(c);
        if (match(tipo, "Router") || match(tipo, "Internet")) return getRouter(c);

        // OUTROS
        if (match(tipo, "Paineis") || match(tipo, "Solar")) return getSolar(c);
        if (match(tipo, "Piscina")) return getPiscina(c);
        if (match(tipo, "Rega") || match(tipo, "Bomba")) return getBombaRega(c);
        if (match(tipo, "Carro") || match(tipo, "Elétrico")) return getCarroEletrico(c);

        return 0.0; // Se não encontrarmos o aparelho
    }

    // Método auxiliar para comparar strings sem problemas de maiúsculas/minúsculas
    private static boolean match(String input, String termo) {
        return input != null && input.toLowerCase().contains(termo.toLowerCase());
    }

    // =========================================================================
    // VALORES MENSAIS (kWh) - Extraídos do teu CSV
    // =========================================================================

    private static double getFrigorifico(String c) {
        switch (c) { case "A": return 15.0; case "B": return 22.0; case "C": return 28.0; case "D": return 35.0; case "E": return 42.0; case "F": return 50.0; case "G": return 65.0; default: return 35.0; }
    }
    private static double getFrigorificoCombinado(String c) {
        switch (c) { case "A": return 22.0; case "B": return 32.0; case "C": return 42.0; case "D": return 52.0; case "E": return 65.0; case "F": return 80.0; case "G": return 100.0; default: return 52.0; }
    }
    private static double getForno(String c) {
        switch (c) { case "A": return 35.0; case "B": return 42.0; case "C": return 50.0; case "D": return 60.0; case "E": return 75.0; case "F": return 90.0; case "G": return 110.0; default: return 60.0; }
    }
    private static double getArca(String c) {
        switch (c) { case "A": return 18.0; case "B": return 26.0; case "C": return 35.0; case "D": return 45.0; case "E": return 58.0; case "F": return 72.0; case "G": return 90.0; default: return 45.0; }
    }
    private static double getPlaca(String c) {
        switch (c) { case "A": return 8.0; case "B": return 12.0; case "C": return 16.0; case "D": return 20.0; case "E": return 26.0; case "F": return 32.0; case "G": return 40.0; default: return 20.0; }
    }
    private static double getMicroondas(String c) {
        switch (c) { case "A": return 3.0; case "B": return 4.0; case "C": return 5.0; case "D": return 6.0; case "E": return 7.0; case "F": return 8.0; case "G": return 10.0; default: return 6.0; }
    }
    private static double getFerro(String c) {
        switch (c) { case "A": return 5.0; case "B": return 7.0; case "C": return 9.0; case "D": return 12.0; case "E": return 15.0; case "F": return 18.0; case "G": return 22.0; default: return 12.0; }
    }
    private static double getRobot(String c) {
        switch (c) { case "A": return 2.0; case "B": return 3.0; case "C": return 4.0; case "D": return 5.0; case "E": return 6.5; case "F": return 8.0; case "G": return 10.0; default: return 5.0; }
    }
    private static double getCafeteira(String c) {
        switch (c) { case "A": return 8.0; case "B": return 12.0; case "C": return 16.0; case "D": return 20.0; case "E": return 26.0; case "F": return 32.0; case "G": return 40.0; default: return 20.0; }
    }
    private static double getChaleira(String c) {
        switch (c) { case "A": return 3.0; case "B": return 5.0; case "C": return 7.0; case "D": return 9.0; case "E": return 12.0; case "F": return 15.0; case "G": return 18.0; default: return 9.0; }
    }
    private static double getArCondicionado(String c) {
        switch (c) { case "A": return 80.0; case "B": return 110.0; case "C": return 140.0; case "D": return 170.0; case "E": return 210.0; case "F": return 250.0; case "G": return 300.0; default: return 170.0; }
    }
    private static double getRadiador(String c) {
        switch (c) { case "A": return 35.0; case "B": return 50.0; case "C": return 65.0; case "D": return 85.0; case "E": return 110.0; case "F": return 140.0; case "G": return 180.0; default: return 85.0; }
    }
    private static double getAquecedor(String c) {
        switch (c) { case "A": return 25.0; case "B": return 35.0; case "C": return 48.0; case "D": return 65.0; case "E": return 85.0; case "F": return 110.0; case "G": return 140.0; default: return 65.0; }
    }
    private static double getCaldeira(String c) {
        switch (c) { case "A": return 50.0; case "B": return 70.0; case "C": return 95.0; case "D": return 120.0; case "E": return 150.0; case "F": return 190.0; case "G": return 240.0; default: return 120.0; }
    }
    private static double getBombaCalor(String c) {
        switch (c) { case "A": return 120.0; case "B": return 160.0; case "C": return 210.0; case "D": return 270.0; case "E": return 340.0; case "F": return 420.0; case "G": return 520.0; default: return 270.0; }
    }
    private static double getDesumidificador(String c) {
        switch (c) { case "A": return 12.0; case "B": return 18.0; case "C": return 24.0; case "D": return 32.0; case "E": return 42.0; case "F": return 52.0; case "G": return 65.0; default: return 32.0; }
    }
    private static double getLareira(String c) {
        switch (c) { case "A": return 40.0; case "B": return 55.0; case "C": return 75.0; case "D": return 95.0; case "E": return 120.0; case "F": return 150.0; case "G": return 190.0; default: return 95.0; }
    }
    private static double getPisoAquecido(String c) {
        switch (c) { case "A": return 60.0; case "B": return 85.0; case "C": return 115.0; case "D": return 150.0; case "E": return 190.0; case "F": return 240.0; case "G": return 300.0; default: return 150.0; }
    }
    private static double getLouca(String c) {
        switch (c) { case "A": return 10.0; case "B": return 14.0; case "C": return 18.0; case "D": return 22.0; case "E": return 28.0; case "F": return 35.0; case "G": return 45.0; default: return 22.0; }
    }
    private static double getMaquinaLavar(String c) {
        switch (c) { case "A": return 6.0; case "B": return 8.0; case "C": return 10.0; case "D": return 12.0; case "E": return 15.0; case "F": return 18.0; case "G": return 22.0; default: return 12.0; }
    }
    private static double getMaquinaSecar(String c) {
        switch (c) { case "A": return 50.0; case "B": return 65.0; case "C": return 80.0; case "D": return 95.0; case "E": return 110.0; case "F": return 130.0; case "G": return 160.0; default: return 95.0; }
    }
    private static double getTelevisao(String c) {
        switch (c) { case "A": return 8.0; case "B": return 12.0; case "C": return 16.0; case "D": return 20.0; case "E": return 26.0; case "F": return 32.0; case "G": return 40.0; default: return 20.0; }
    }
    private static double getConsola(String c) {
        switch (c) { case "A": return 12.0; case "B": return 16.0; case "C": return 22.0; case "D": return 28.0; case "E": return 36.0; case "F": return 45.0; case "G": return 56.0; default: return 28.0; }
    }
    private static double getComputador(String c) {
        switch (c) { case "A": return 12.0; case "B": return 16.0; case "C": return 22.0; case "D": return 28.0; case "E": return 36.0; case "F": return 45.0; case "G": return 56.0; default: return 28.0; }
    }
    private static double getSolar(String c) {
        switch (c) { case "A": return -150.0; case "B": return -120.0; case "C": return -100.0; case "D": return -80.0; case "E": return -60.0; case "F": return -40.0; case "G": return -20.0; default: return -80.0; }
    }
    private static double getCarroEletrico(String c) {
        switch (c) { case "A": return 120.0; case "B": return 160.0; case "C": return 210.0; case "D": return 270.0; case "E": return 340.0; case "F": return 420.0; case "G": return 520.0; default: return 270.0; }
    }
    private static double getPiscina(String c) {
        switch (c) { case "A": return 180.0; case "B": return 240.0; case "C": return 320.0; case "D": return 420.0; case "E": return 540.0; case "F": return 680.0; case "G": return 860.0; default: return 420.0; }
    }
    private static double getBombaRega(String c) {
        switch (c) { case "A": return 8.0; case "B": return 12.0; case "C": return 16.0; case "D": return 22.0; case "E": return 28.0; case "F": return 35.0; case "G": return 45.0; default: return 22.0; }
    }
    private static double getRouter(String c) {
        switch (c) { case "A": return 2.0; case "B": return 3.0; case "C": return 4.0; case "D": return 5.0; case "E": return 6.5; case "F": return 8.0; case "G": return 10.0; default: return 5.0; }
    }
}