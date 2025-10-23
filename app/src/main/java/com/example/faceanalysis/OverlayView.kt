package com.example.faceanalysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs // Certifique-se de que esta importação existe, se estiver usando abs

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // PROPRIEDADE: Controla se os landmarks devem ser desenhados
    var drawLandmarks: Boolean = true
        set(value) {
            field = value
            invalidate() // Redesenha a view quando o valor muda
        }

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
        strokeWidth = 2f
    }

    private var points: List<Pair<Float, Float>> = emptyList()
    // A propriedade isFrontCamera está aqui, mas se não for usada na lógica de desenho, pode ser removida.
    // Atualmente, ela não impacta o onDraw diretamente.
    private var isFrontCamera: Boolean = true
    private var rotationDegrees: Int = 0

    fun setPoints(newPoints: List<Pair<Float, Float>>, isFrontCamera: Boolean = true) {
        this.points = newPoints
        this.isFrontCamera = isFrontCamera
        invalidate()
    }

    fun setRotationDegrees(degrees: Int) {
        this.rotationDegrees = degrees
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Somente desenha se drawLandmarks for TRUE e houver pontos para desenhar
        if (!drawLandmarks || points.isEmpty()) {
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()

        for ((x, y) in points) {
            var drawX = x
            var drawY = y

            // SUA LÓGICA EXISTENTE DE AJUSTE DE ROTAÇÃO
            when (rotationDegrees) {
                90 -> { drawX = y; drawY = 1f - x }
                180 -> { drawX = 1f - x; drawY = 1f - y }
                270 -> { drawX = 1f - y; drawY = x }
            }

            // SUA LÓGICA EXISTENTE DE INVERSÃO DO EIXO Y
            drawY = 1f - drawY

            val screenX = drawX * w
            val screenY = drawY * h

            // Desenha o ponto menor e delicado
            canvas.drawCircle(screenX, screenY, 3.5f, paint)
        }
    }
}
