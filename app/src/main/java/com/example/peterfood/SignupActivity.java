package com.example.peterfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText etNewUsername, etNewPassword, etSDT;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etNewUsername = findViewById(R.id.etNewUsername);
        etNewPassword = findViewById(R.id.etNewPassword);
        etSDT = findViewById(R.id.etSDT);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        if (etSDT == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy trường số điện thoại", Toast.LENGTH_LONG).show();
        }
    }

    public void onSignUpClick(View view) {
        String username = etNewUsername.getText().toString().trim();
        String password = etNewPassword.getText().toString().trim();
        String SDT = etSDT.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || SDT.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!SDT.matches("\\d{10}")) {
            Toast.makeText(this, "Số điện thoại phải có đúng 10 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putString("sdt", SDT);
        editor.apply();

        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(SignupActivity.this, MainActivity.class));
        finish();
    }

    public void goBackToLogin(View view) {
        startActivity(new Intent(SignupActivity.this, MainActivity.class));
        finish();
    }
}