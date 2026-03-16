package com.example.nowme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.UserDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    TextView tvSignup;
    boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> {
            if (registerMode) {
                register();
            } else {
                login();
            }
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

    private void register() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        UserDto userDto = new UserDto(username, password);

        Call<String> call = RetrofitClient.getApi().register(userDto);

        auth(call);
    }

    private void login() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        UserDto userDto = new UserDto(username, password);

        Call<String> call = RetrofitClient.getApi().login(userDto);

        auth(call);
    }

    private void auth(Call<String> call) {

        call.enqueue(new retrofit2.Callback<String>() {

            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                if (response.isSuccessful()) {

                    saveSessionToken(response.body());

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void saveSessionToken(String token) {
        getSharedPreferences("session", MODE_PRIVATE)
                .edit()
                .putString("sessionToken", token)
                .apply();
    }
}