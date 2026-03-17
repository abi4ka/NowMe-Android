package com.example.nowme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.AuthDto;
import com.example.nowme.network.dto.UserDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvSignup;
    private boolean registerMode = false;

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

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> {
            auth();
        });

        tvSignup.setOnClickListener(v -> {
            if (!registerMode) {
                //register
                btnLogin.setText("Register");
                tvSignup.setText("Back");
                registerMode = true;
            } else {
                //login
                btnLogin.setText("Login");
                tvSignup.setText("Sign up");
                registerMode = false;
            }
        });
    }

    private void auth() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        UserDto userDto = new UserDto(username, password);

        Call<AuthDto> call = (registerMode)
                ? RetrofitClient.getApi().register(userDto)
                : RetrofitClient.getApi().login(userDto);

        call.enqueue(new Callback<AuthDto>() {

            @Override
            public void onResponse(Call<AuthDto> call, Response<AuthDto> response) {

                if (response.isSuccessful()) {

                    saveSessionToken(response.body().token);

                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<AuthDto> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private boolean hasSession() {
        String token = getSharedPreferences("session", MODE_PRIVATE)
                .getString("sessionToken", null);
        return token != null && !token.isEmpty();
    }

    private void saveSessionToken(String token) {
        getSharedPreferences("session", MODE_PRIVATE)
                .edit()
                .putString("sessionToken", token)
                .apply();
    }
}