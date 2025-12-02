package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivityTecnico extends AppCompatActivity {

    protected BottomNavigationView bottomNavTecnico;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Chamas isto depois do setContentView() em cada Activity de técnico
     * passando o item atualmente selecionado (ex: R.id.menu_inicio_tecnico).
     */
    protected void setupBottomNavTecnico(int selectedItemId) {
        bottomNavTecnico = findViewById(R.id.bottomNavTecnico);
        if (bottomNavTecnico == null) return;

        bottomNavTecnico.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == selectedItemId) {
                // já estamos neste separador
                return true;
            }

            Intent intent = null;

            if (id == R.id.menu_inicio_tecnico) {
                intent = new Intent(this, HomeTecnico.class);
            } else if (id == R.id.menu_mensagens_tecnico) {
                intent = new Intent(this, ListaChatsTecnicoActivity.class);
            } else if (id == R.id.menu_perfil_tecnico) {
                intent = new Intent(this, PerfilUtilizador.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });

        bottomNavTecnico.setSelectedItemId(selectedItemId);
    }
}
