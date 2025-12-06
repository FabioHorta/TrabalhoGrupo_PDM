package pt.ubi.pdm.ecotrack.api;

import java.util.List;

import pt.ubi.pdm.ecotrack.models.AssistenciasSyncResult;
import pt.ubi.pdm.ecotrack.models.DicasResponse;
import pt.ubi.pdm.ecotrack.models.GoogleLoginRequest;
import pt.ubi.pdm.ecotrack.models.MensagemChatSync;
import pt.ubi.pdm.ecotrack.models.MensagemSuporteSync;
import pt.ubi.pdm.ecotrack.models.RegisterRequest;
import pt.ubi.pdm.ecotrack.models.Tecnico;
import pt.ubi.pdm.ecotrack.models.UpdatePrecoRequest;
import pt.ubi.pdm.ecotrack.models.UploadLeituraImagemRequest;
import pt.ubi.pdm.ecotrack.models.UserResponse;
import pt.ubi.pdm.ecotrack.models.LoginRequest;
import pt.ubi.pdm.ecotrack.models.LeituraSync;
import pt.ubi.pdm.ecotrack.models.AssistenciaSync;
import pt.ubi.pdm.ecotrack.models.CasaSync;
import pt.ubi.pdm.ecotrack.models.ApplianceSync;
import pt.ubi.pdm.ecotrack.models.RelatorioCreateRequest;
import pt.ubi.pdm.ecotrack.models.RelatorioResponse;


import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/register")
    Call<UserResponse> register(@Body RegisterRequest request);

    @POST("/login")
    Call<UserResponse> login(@Body LoginRequest request);

    @POST("/oauth/google")
    Call<UserResponse> loginWithGoogle(@Body GoogleLoginRequest body);

    @GET("/casas/by-user")
    Call<List<CasaSync>> getCasasByUser(@Query("email") String email);

    @GET("/appliances/by-user")
    Call<List<ApplianceSync>> getAppliancesByUser(@Query("email") String email);

    @GET("/leituras/by-user")
    Call<List<LeituraSync>> getLeiturasByUser(@Query("email") String email);

    @GET("/assistencias/all")
    Call<List<AssistenciaSync>> getAssistencias(@Query("tecnico_email") String tecnicoEmail);

    @POST("/leituras/sync")
    Call<Void> syncLeituras(@Body List<LeituraSync> leituras);

    @POST("/assistencias/sync")
    Call<AssistenciasSyncResult> syncAssistencias(@Body List<AssistenciaSync> assistencias);

    @POST("/casas/sync")
    Call<Void> syncCasas(@Body List<CasaSync> casas);

    @POST("/appliances/sync")
    Call<Void> syncAppliances(@Body List<ApplianceSync> appliances);

    @POST("/leituras/upload-bitmap")
    Call<Void> uploadLeituraBitmap(@Body UploadLeituraImagemRequest body);

    @POST("/mensagens_suporte/sync")
    Call<Void> syncMensagensSuporte(@Body List<MensagemSuporteSync> mensagens);

    @POST("/mensagens_chat/sync")
    Call<Void> syncMensagensChat(@Body List<MensagemChatSync> mensagens);

    @GET("/mensagens_chat/by-email")
    Call<List<MensagemChatSync>> getMensagensChatByEmail(@Query("email") String email);

    @GET("/alertas/dicas")
    Call<DicasResponse> getDicas(@Query("tipo") String tipo);
    @GET("/tecnicos/all")
    Call<List<Tecnico>> getTecnicos();
    @POST("/relatorios/create")
    Call<RelatorioResponse> criarRelatorio(@Body RelatorioCreateRequest body);

    // Lista de relatórios de um cliente
    @GET("relatorios/by-cliente")
    Call<List<RelatorioResponse>> getRelatoriosByCliente(@Query("email") String email);

    // Obter um relatório específico com o PDF em Base64
    @GET("relatorios/{id}/base64")
    Call<RelatorioResponse> getRelatorioBase64(@Path("id") long id);

    @POST("/users/update-preco")
    Call<UserResponse> updatePrecoKwh(@Body UpdatePrecoRequest body);


}
