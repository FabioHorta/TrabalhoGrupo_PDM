package pt.ubi.pdm.ecotrack.api;

import java.util.List;

import pt.ubi.pdm.ecotrack.models.RegisterRequest;
import pt.ubi.pdm.ecotrack.models.UserResponse;
import pt.ubi.pdm.ecotrack.models.LoginRequest;
import pt.ubi.pdm.ecotrack.models.LeituraSync;
import pt.ubi.pdm.ecotrack.models.AssistenciaSync;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface ApiService {

    @POST("/register")
    Call<UserResponse> register(@Body RegisterRequest request);

    @POST("/login")
    Call<UserResponse> login(@Body LoginRequest request);

    @POST("/leituras/sync")
    Call<Void> syncLeituras(@Body List<LeituraSync> leituras);

    @POST("/assistencias/sync")
    Call<Void> syncAssistencias(@Body List<AssistenciaSync> assistencias);

}
