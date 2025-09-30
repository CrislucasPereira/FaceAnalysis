package com.example.faceanalysis

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val btnSendReset = findViewById<Button>(R.id.btnSendReset)
        val txtBackToLogin = findViewById<TextView>(R.id.txtBackToLogin)

        btnSendReset.setOnClickListener {
            val email = edtEmail.text.toString()
            if(email.isNotEmpty()) {
                // Aqui você pode enviar link de recuperação com Firebase ou backend
                edtEmail.error = null
            } else {
                edtEmail.error = "Digite seu email"
            }
        }

        txtBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
