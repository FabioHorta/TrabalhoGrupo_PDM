package pt.ubi.pdm.ecotrack.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;

    private static final String BASE_URL = "https://extraterritorial-pseudoaristocratic-janiece.ngrok-free.dev/";

    public static Retrofit getRetrofit(Context context) {

        if (retrofit == null) {

            // Interceptor para adicionar o token JWT automaticamente
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();

                        SharedPreferences prefs =
                                context.getSharedPreferences("auth", Context.MODE_PRIVATE);

                        String token = prefs.getString("auth_token", null);

                        Request.Builder builder = original.newBuilder();

                        if (token != null) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        Request request = builder.build();
                        return chain.proceed(request);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }

        return retrofit;
    }
}
