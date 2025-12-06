package pt.ubi.pdm.ecotrack.api;

import java.util.List;

import pt.ubi.pdm.ecotrack.models.GoogleLoginRequest;
import pt.ubi.pdm.ecotrack.models.RegisterRequest;
import pt.ubi.pdm.ecotrack.models.UserResponse;
import pt.ubi.pdm.ecotrack.models.LoginRequest;
import pt.ubi.pdm.ecotrack.models.LeituraSync;
import pt.ubi.pdm.ecotrack.models.AssistenciaSync;
import pt.ubi.pdm.ecotrack.models.CasaSync;
import pt.ubi.pdm.ecotrack.models.ApplianceSync;
import retrofit2.http.GET;
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
    Call<Void> syncAssistencias(@Body List<AssistenciaSync> assistencias);

    @POST("/casas/sync")
    Call<Void> syncCasas(@Body List<CasaSync> casas);

    @POST("/appliances/sync")
    Call<Void> syncAppliances(@Body List<ApplianceSync> appliances);
}
