package com.example.faceanalysis

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // ✨ Animação fade geral (arquivo: res/anim/fade_in.xml)
        val root = findViewById<View>(android.R.id.content)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        root.startAnimation(fadeIn)

        // 🎬 Animação suave individual dos elementos
        val center = findViewById<View>(R.id.centerGroup)
        val btnEnter = findViewById<Button>(R.id.btnEnter)

        listOf(center, btnEnter).forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 32f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((180L * index))
                .setDuration(500L)
                .start()
        }

        // ⚡ Efeito de toque (escala suave)
        btnEnter.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .start()

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(80)
                        .start()

                    v.performClick() // mantém a acessibilidade
                }
            }
            false
        }

        btnEnter.setOnClickListener {
            val intent = Intent(this@HomeActivity, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
