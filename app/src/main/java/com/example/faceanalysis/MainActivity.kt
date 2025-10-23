package com.example.faceanalysis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivityHome"
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val txtUserEmail: TextView = findViewById(R.id.txtUserEmail)
        val btnStartAnalysis: Button = findViewById(R.id.btnStartAnalysis)
        val btnViewProfile: Button = findViewById(R.id.btnViewProfile)
        val btnDailyReport: Button = findViewById(R.id.btnDailyReport)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        // Exibe o email do usuário logado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            txtUserEmail.text = currentUser.email
            Log.d(TAG, "Usuário logado: ${currentUser.email}")
        } else {
            Log.w(TAG, "Nenhum usuário logado. Redirecionando para Login.")
            redirectToLogin()
        }

        // ⚡ Animação padrão (escala suave)
        fun applyButtonEffect(button: Button, action: () -> Unit) {
            button.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                        v.performClick()
                    }
                }
                false
            }
            button.setOnClickListener {
                action()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        // 🚀 Navegação com efeito
        applyButtonEffect(btnStartAnalysis) {
            startActivity(Intent(this, AnalysisActivity::class.java))
        }

        applyButtonEffect(btnViewProfile) {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        applyButtonEffect(btnDailyReport) {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        applyButtonEffect(btnLogout) {
            auth.signOut()
            Toast.makeText(this, "Você saiu da conta.", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }

        // Botão voltar → minimiza app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
                Log.d(TAG, "Botão voltar: minimizando o app.")
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToLogin()
        } else {
            findViewById<TextView>(R.id.txtUserEmail).text = currentUser.email
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
