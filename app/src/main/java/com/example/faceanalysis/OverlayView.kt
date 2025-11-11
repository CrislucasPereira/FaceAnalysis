package com.example.faceanalysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.util.AttributeSet
import android.view.View

/**
 * Camada de desenho para exibir landmarks faciais sobre o preview da camera.
 * - Converte coordenadas normalizadas em pixels de tela, respeitando rotacao e camera frontal.
 * - Redesenha apenas quando necessario (invalidate/postInvalidate).
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var drawLandmarks: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
        strokeWidth = 2f
    }

    private var points: List<Pair<Float, Float>> = emptyList()

    private var isFrontCamera: Boolean = true
    private var rotationDegrees: Int = 0

    fun setPoints(newPoints: List<Pair<Float, Float>>, isFrontCamera: Boolean = true) {
        this.points = newPoints
        this.isFrontCamera = isFrontCamera
        // Garante execucao na UI thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate()
        } else {
            postInvalidate()
        }
    }

    fun setRotationDegrees(degrees: Int) {
        this.rotationDegrees = degrees
        // Garante execucao na UI thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate()
        } else {
            postInvalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!drawLandmarks || points.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        for ((x, y) in points) {
            var drawX = x
            var drawY = y

            when (rotationDegrees) {
                90 -> { drawX = y; drawY = 1f - x }
                180 -> { drawX = 1f - x; drawY = 1f - y }
                270 -> { drawX = 1f - y; drawY = x }
            }

            drawY = 1f - drawY

            val screenX = drawX * w
            val screenY = drawY * h

            canvas.drawCircle(screenX, screenY, 3.5f, paint)
        }
    }
}

