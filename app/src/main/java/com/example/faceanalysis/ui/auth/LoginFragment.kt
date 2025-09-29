package com.example.faceanalysis.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.faceanalysis.R
import com.example.faceanalysis.ui.home.HomeActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_login)

        // TODO: Adicionar lógica de login
        // Exemplo simples de navegação:
        // startActivity(Intent(this, HomeActivity::class.java))
        // finish()
    }
}
