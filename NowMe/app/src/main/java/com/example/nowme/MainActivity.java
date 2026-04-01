package com.example.nowme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        View rootView = findViewById(R.id.mainRoot);
        View topAppBar = findViewById(R.id.topAppBar);

        int appBarPaddingLeft = topAppBar.getPaddingLeft();
        int appBarPaddingTop = topAppBar.getPaddingTop();
        int appBarPaddingRight = topAppBar.getPaddingRight();
        int appBarPaddingBottom = topAppBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            topAppBar.setPadding(
                    appBarPaddingLeft,
                    appBarPaddingTop + statusBars.top,
                    appBarPaddingRight,
                    appBarPaddingBottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.cameraFragment) {
                startActivity(new Intent(this, CameraActivity.class));
                return true;
            } else {
                return NavigationUI.onNavDestinationSelected(item, navController);
            }
        });
    }
}
