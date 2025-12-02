package pt.ubi.pdm.ecotrack;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SyncUtils.syncTudoAsync(this);
    }

    /**
     * Chamas isto em cada Activity filha DEPOIS do setContentView()
     * passando o item atual da bottom bar (ex: R.id.nav_leituras).
     */
    protected void setupBottomNav(int selectedItemId) {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView == null) return;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == selectedItemId) {
                // já estamos neste ecrã
                return true;
            }

            Intent intent = null;

            if (id == R.id.nav_home) {
                intent = new Intent(this, MenuPrincipal.class);
            } else if (id == R.id.nav_leituras) {
                intent = new Intent(this, LeiturasMensais.class);
            } else if (id == R.id.nav_simulador) {
                intent = new Intent(this, CalculadoraCustos.class);
            } else if (id == R.id.nav_alertas) {
                intent = new Intent(this, AlertasConsumo.class);
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

        bottomNavigationView.setSelectedItemId(selectedItemId);
    }

}
