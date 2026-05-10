package com.example.nowme.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.nowme.R;
import com.example.nowme.ui.camera.CameraActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_PROFILE_USER_ID = "profileUserId";

    BottomNavigationView bottomNavigationView;
    private NavController navController;
    private boolean syncingBottomNavigation = false;

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
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                syncBottomNavigationSelection()
        );

        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (syncingBottomNavigation) {
                return true;
            }

            int id = item.getItemId();

            if (id == R.id.homeFragment) {
                openHome(navController);
                return true;
            } else if (id == R.id.cameraFragment) {
                openCamera();
                syncBottomNavigationSelection();
                return false;
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

    private void openCamera() {
        startActivity(new Intent(this, CameraActivity.class));
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncBottomNavigationSelection();
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
            navController.navigate(R.id.homeFragment, null, buildTabNavOptions());
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

        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.profileFragment) {
            navController.popBackStack(R.id.profileFragment, true);
        }
        navController.navigate(R.id.profileFragment, null, buildTabNavOptions());
    }

    private NavOptions buildTabNavOptions() {
        return new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .build();
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

    public void openUserProfile(long userId) {
        if (navController == null || userId <= 0L) {
            return;
        }

        Bundle args = new Bundle();
        args.putLong("userId", userId);
        navController.popBackStack(R.id.profileFragment, true);
        navController.navigate(R.id.profileFragment, args);
    }

    private void syncBottomNavigationSelection() {
        if (bottomNavigationView == null
                || navController == null
                || navController.getCurrentDestination() == null) {
            return;
        }

        int destinationId = navController.getCurrentDestination().getId();
        if (destinationId == R.id.homeFragment || destinationId == R.id.profileFragment) {
            if (bottomNavigationView.getSelectedItemId() == destinationId) {
                return;
            }

            syncingBottomNavigation = true;
            bottomNavigationView.setSelectedItemId(destinationId);
            syncingBottomNavigation = false;
        }
    }
}
