package com.example.faceanalysis

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtBackToLogin = findViewById<TextView>(R.id.txtBackToLogin)

        btnRegister.setOnClickListener {
            val name = edtName.text.toString()
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if(name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                // Aqui você pode cadastrar o usuário no Firebase ou backend
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                edtName.error = if(name.isEmpty()) "Digite seu nome" else null
                edtEmail.error = if(email.isEmpty()) "Digite seu email" else null
                edtPassword.error = if(password.isEmpty()) "Digite sua senha" else null
            }
        }

        txtBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
