package com.example.nowme.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nowme.R;
import com.example.nowme.network.ApiErrors;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.SessionManager;
import com.example.nowme.network.TokenStorage;
import com.example.nowme.network.dto.AuthRequest;
import com.example.nowme.network.dto.AuthResponse;
import com.example.nowme.ui.main.MainActivity;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvSignup;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]{3,20}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 24;

    private boolean registerMode = false;
    private boolean authRequestInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasSession()) {
            Intent intent = new Intent(AuthActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        if (getIntent().getBooleanExtra(SessionManager.EXTRA_SESSION_EXPIRED, false)) {
            Toast.makeText(this, R.string.session_expired, Toast.LENGTH_SHORT).show();
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> auth());

        tvSignup.setOnClickListener(v -> {
            if (!registerMode) {
                btnLogin.setText(R.string.register);
                tvSignup.setText(R.string.back);
                registerMode = true;
            } else {
                btnLogin.setText(R.string.login);
                tvSignup.setText(R.string.sign_up);
                registerMode = false;
            }
        });
    }

    private void auth() {
        if (authRequestInFlight) {
            return;
        }

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.username_password_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Same rules as the backend's RegisterRequest, checked here to give instant feedback
        if (registerMode && !USERNAME_PATTERN.matcher(username).matches()) {
            etUsername.setError(getString(R.string.username_format));
            return;
        }
        if (registerMode && (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH)) {
            etPassword.setError(getString(R.string.password_length));
            return;
        }

        AuthRequest authRequest = new AuthRequest(username, password);
        authRequestInFlight = true;
        btnLogin.setEnabled(false);
        tvSignup.setEnabled(false);

        Call<AuthResponse> call = registerMode
                ? RetrofitClient.getApi().register(authRequest)
                : RetrofitClient.getApi().login(authRequest);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                authRequestInFlight = false;
                btnLogin.setEnabled(true);
                tvSignup.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse dto = response.body();

                    TokenStorage.save(AuthActivity.this, dto);

                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AuthActivity.this,
                            errorMessage(ApiErrors.code(response)),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                authRequestInFlight = false;
                btnLogin.setEnabled(true);
                tvSignup.setEnabled(true);
                Toast.makeText(AuthActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int errorMessage(String errorCode) {
        if (errorCode == null) return R.string.auth_failed;
        switch (errorCode) {
            case "INVALID_CREDENTIALS":
                return R.string.invalid_credentials;
            case "USERNAME_ALREADY_EXISTS":
                return R.string.username_taken;
            case "INVALID_REQUEST":
                return registerMode ? R.string.username_format : R.string.auth_failed;
            default:
                return R.string.auth_failed;
        }
    }

    private boolean hasSession() {
        String access = TokenStorage.getAccess(this);
        String refresh = TokenStorage.getRefresh(this);
        return access != null && !access.isEmpty()
                && refresh != null && !refresh.isEmpty();
    }
}
