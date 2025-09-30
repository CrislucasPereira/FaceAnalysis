package com.example.faceanalysis

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)
        val txtRegister = findViewById<TextView>(R.id.txtRegister)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            // Aqui você pode validar login com Firebase ou seu backend
            if(email.isNotEmpty() && password.isNotEmpty()) {
                // Login bem-sucedido
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                edtEmail.error = if(email.isEmpty()) "Digite seu email" else null
                edtPassword.error = if(password.isEmpty()) "Digite sua senha" else null
            }
        }

      txtForgotPassword.setOnClickListener {
          // Navegar para tela de recuperação de senha
          val intent = Intent(this, ForgotPasswordActivity::class.java)
          startActivity(intent)
      }
//
      txtRegister.setOnClickListener {
          // Navegar para tela de cadastro
          val intent = Intent(this, RegisterActivity::class.java)
          startActivity(intent)
      }
    }
}
