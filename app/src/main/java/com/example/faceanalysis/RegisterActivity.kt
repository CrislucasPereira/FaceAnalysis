package com.example.faceanalysis

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import android.text.method.PasswordTransformationMethod
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Fluxo de cadastro de usuarios (Firebase Auth).
 *
 * - Validacoes de senha e aceite de termos.
 * - Feedback de forca da senha (texto + barra).
 * - Encaminha para login apos cadastro.
 */
class RegisterActivity : AppCompatActivity() {

    private val TAG = "RegisterActivity"
    private lateinit var auth: FirebaseAuth

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnToggle = findViewById<ImageButton>(R.id.btnTogglePasswordRegister)
        // Força sempre oculto por padrão
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
        val tvPasswordStrength = findViewById<TextView>(R.id.tvPasswordStrength)
        val progressPasswordStrength = findViewById<ProgressBar>(R.id.progressPasswordStrength)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtBackToLogin = findViewById<TextView>(R.id.txtBackToLogin)
        val switchTerms = findViewById<Switch>(R.id.switchTerms)
        val switchNotify = findViewById<Switch>(R.id.switchNotify)
        val tvTerms = findViewById<TextView>(R.id.tvTerms)

        // Termos de uso
        tvTerms.text = Html.fromHtml(
            "Concordo com os <b><u>Termos de Uso</u></b> e Politica de Privacidade.",
            Html.FROM_HTML_MODE_LEGACY
        )

        tvTerms.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().alpha(0.85f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().alpha(1f).setDuration(100).start()
                    v.performClick()
                }
            }
            false
        }

        tvTerms.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Feedback da senha
        edtPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val feedback = getPasswordFeedback(password)

                tvPasswordStrength.text = feedback.text
                tvPasswordStrength.setTextColor(Color.parseColor(feedback.color))
                progressPasswordStrength.progress = feedback.level
                progressPasswordStrength.progressTintList = ColorStateList.valueOf(Color.parseColor(feedback.color))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Botao Cadastrar
        btnRegister.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    v.performClick()
                }
            }
            false
        }

        btnRegister.setOnClickListener {
            val name = edtName.text.toString()
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!switchTerms.isChecked) {
                Toast.makeText(this, "Aceite os Termos de Uso e Politica de Privacidade.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isPasswordValid(password)) {
                Toast.makeText(this, "Senha fraca! Use letra maiuscula, minuscula, numero e simbolo.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Cadastro realizado com sucesso!")
                        Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    } else {
                        Toast.makeText(this, "Falha: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Voltar ao Login
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

    private fun isPasswordValid(password: String): Boolean {
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        val hasLength = password.length >= 8
        return hasUpper && hasLower && hasDigit && hasSymbol && hasLength
    }

    private fun getPasswordFeedback(password: String): PasswordFeedback {
        if (password.isEmpty()) return PasswordFeedback("A senha deve conter: minimo 8 caracteres, letra maiuscula, minuscula, numero e simbolo.", "#B71C1C", 0)
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when (score) {
            5 -> PasswordFeedback("Senha forte", "#2E7D32", 5)
            4 -> PasswordFeedback("Senha media", "#F9A825", 4)
            3 -> PasswordFeedback("Senha fraca", "#FB8C00", 3)
            else -> PasswordFeedback("Senha muito fraca", "#B71C1C", 1)
        }
    }

    data class PasswordFeedback(val text: String, val color: String, val level: Int)
}
