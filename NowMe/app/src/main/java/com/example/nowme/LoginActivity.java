package com.example.nowme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
                // Cambiar a modo registro
                btnLogin.setText("Register");
                tvSignup.setText("Back");
                registerMode = true;
            } else {
                // Volver a login
                btnLogin.setText("Login");
                tvSignup.setText("Sign up");
                registerMode = false;
            }
        });
    }

    private void register() {
        //TODO: register
    }

    private void login() {

        String user = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        //TODO: connect backend
        if (user.equals("test") && password.equals("1234")) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}