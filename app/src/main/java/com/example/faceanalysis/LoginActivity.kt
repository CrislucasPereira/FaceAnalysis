package com.example.faceanalysis

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import android.text.method.PasswordTransformationMethod
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Tela de autenticacao por e-mail/senha (Firebase Auth).
 *
 * - Valida campos minimos.
 * - Mostra feedback visual em toques.
 * - Redireciona para a tela principal apos sucesso.
 */
class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private lateinit var auth: FirebaseAuth

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnToggle = findViewById<ImageButton>(R.id.btnTogglePasswordLogin)
        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)
        val txtRegister = findViewById<TextView>(R.id.txtRegister)

        // Força sempre oculto por padrão e adiciona toggle de visibilidade
        edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        var showing = false
        btnToggle?.setOnClickListener {
            showing = !showing
            if (showing) {
                edtPassword.transformationMethod = null
                btnToggle.setImageResource(R.drawable.ic_visibility)
            } else {
                edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnToggle.setImageResource(R.drawable.ic_visibility_off)
            }
            edtPassword.setSelection(edtPassword.text?.length ?: 0)
        }

        // Icone de voltar
        btnBack?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().alpha(0.7f).scaleX(0.9f).scaleY(0.9f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
                    v.performClick()
                }
            }
            false
        }

        btnBack?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Efeito no botao Login
        btnLogin.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    v.performClick()
                }
            }
            false
        }

        // Acao de login
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "signInWithEmailAndPassword:success")
                            Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        } else {
                            Toast.makeText(this, "Falha no login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                edtEmail.error = if (email.isEmpty()) "Digite seu e-mail" else null
                edtPassword.error = if (password.isEmpty()) "Digite sua senha" else null
                Toast.makeText(this, "Preencha e-mail e senha!", Toast.LENGTH_SHORT).show()
            }
        }

        // Link "Esqueci a senha"
        txtForgotPassword.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().alpha(0.85f).scaleX(0.98f).scaleY(0.98f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
                    v.performClick()
                }
            }
            false
        }

        txtForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        // Link "Criar conta"
        txtRegister.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().alpha(0.85f).scaleX(0.98f).scaleY(0.98f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
                    v.performClick()
                }
            }
            false
        }

        txtRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
