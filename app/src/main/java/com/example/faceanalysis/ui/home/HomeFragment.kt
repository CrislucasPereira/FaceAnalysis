package com.example.faceanalysis.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.faceanalysis.R
import com.example.faceanalysis.MainActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_home)

        // TODO: Aqui você coloca botões para navegar
        // Exemplo: abrir a câmera (MainActivity)
        // startActivity(Intent(this, MainActivity::class.java))
    }
}
