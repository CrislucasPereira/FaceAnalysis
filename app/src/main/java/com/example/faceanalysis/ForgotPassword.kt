package com.example.faceanalysis

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private val TAG = "ForgotPasswordActivity"
    private lateinit var auth: FirebaseAuth

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val btnSendReset = findViewById<Button>(R.id.btnSendReset)
        val txtBackToLogin = findViewById<TextView>(R.id.txtBackToLogin)

        // Efeito no botão
        btnSendReset.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    v.performClick()
                }
            }
            false
        }

        btnSendReset.setOnClickListener {
            val email = edtEmail.text.toString()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Email enviado para $email")
                            Toast.makeText(this, "Link de redefinição enviado!", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        } else {
                            Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                edtEmail.error = "Digite seu e-mail"
                Toast.makeText(this, "Por favor, digite seu e-mail.", Toast.LENGTH_SHORT).show()
            }
        }

        // Efeito e ação do texto “Voltar ao login”
        txtBackToLogin.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().alpha(0.85f).scaleX(0.98f).scaleY(0.98f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
                    v.performClick()
                }
            }
            false
        }

        txtBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
