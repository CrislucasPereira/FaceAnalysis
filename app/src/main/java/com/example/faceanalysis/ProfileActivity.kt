package com.example.faceanalysis

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        val imgProfileLogo: ImageView = findViewById(R.id.imgProfileLogo)
        val txtProfileTitle: TextView = findViewById(R.id.txtProfileTitle)
        val txtProfileEmail: TextView = findViewById(R.id.txtProfileEmail)
        val txtProfileUid: TextView = findViewById(R.id.txtProfileUid)
        val txtProfileStatus: TextView = findViewById(R.id.txtProfileStatus)
        val btnBackToMain: Button = findViewById(R.id.btnBackToMain)

        // 🔐 Obtém usuário logado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            txtProfileEmail.text = "📧 ${currentUser.email}"
            txtProfileUid.text = "🆔 UID: ${currentUser.uid}"

            txtProfileStatus.text = if (currentUser.isEmailVerified) {
                "✅ E-mail verificado"
            } else {
                "⚠️ E-mail ainda não verificado"
            }

        } else {
            Toast.makeText(this, "Nenhum usuário logado.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 💫 Efeito suave no botão
        btnBackToMain.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    v.performClick()
                }
            }
            false
        }

        // 🔙 Voltar à tela principal
        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
