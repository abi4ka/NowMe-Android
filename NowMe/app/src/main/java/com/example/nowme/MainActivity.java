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

    public static final String EXTRA_PROFILE_USER_ID = "profileUserId";

    BottomNavigationView bottomNavigationView;
    private NavController navController;

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

        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeFragment) {
                openHome(navController);
                return true;
            } else if (id == R.id.cameraFragment) {
                startActivity(new Intent(this, CameraActivity.class));
                return true;
            } else if (id == R.id.profileFragment) {
                openMyProfile(navController);
                return true;
            } else {
                return NavigationUI.onNavDestinationSelected(item, navController);
            }
        });

        bottomNavigationView.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.homeFragment) {
                openHome(navController);
            } else if (item.getItemId() == R.id.profileFragment) {
                openMyProfile(navController);
            }
        });

        openProfileFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openProfileFromIntent(intent);
    }

    private void openHome(NavController navController) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.homeFragment) {
            return;
        }

        if (!navController.popBackStack(R.id.homeFragment, false)) {
            navController.navigate(R.id.homeFragment);
        }
    }

    private void openMyProfile(NavController navController) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.profileFragment
                && (navController.getCurrentBackStackEntry() == null
                || navController.getCurrentBackStackEntry().getArguments() == null
                || !navController.getCurrentBackStackEntry().getArguments().containsKey("userId"))) {
            return;
        }

        navController.popBackStack(R.id.profileFragment, true);
        navController.navigate(R.id.profileFragment);
    }

    private void openProfileFromIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_PROFILE_USER_ID) || navController == null) {
            return;
        }

        long userId = intent.getLongExtra(EXTRA_PROFILE_USER_ID, -1L);
        intent.removeExtra(EXTRA_PROFILE_USER_ID);
        if (userId <= 0L) {
            return;
        }

        Bundle args = new Bundle();
        args.putLong("userId", userId);
        navController.popBackStack(R.id.profileFragment, true);
        navController.navigate(R.id.profileFragment, args);
    }
}
