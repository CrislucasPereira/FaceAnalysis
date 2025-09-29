package com.example.faceanalysis.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.faceanalysis.R
import com.example.faceanalysis.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_splash)

        // Aqui você pode colocar lógica de delay ou checar se o usuário está logado
        // Por enquanto, vamos só redirecionar para o Login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
