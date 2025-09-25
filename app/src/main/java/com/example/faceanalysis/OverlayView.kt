package com.example.faceanalysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * OverlayView: recebe pontos normalizados (0..1) e desenha sobre a View.
 * Pode desenhar linhas (mesh) conectando pontos se "connections" for informado.
 */
class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val pointPaint = Paint().apply {
        style = Paint.Style.FILL
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // pontos normalizados (x,y) em 0..1
    private var points: List<Pair<Float, Float>> = emptyList()

    // conexões (pares de índices) para desenhar linhas entre landmarks — opcional
    private var connections: List<Pair<Int, Int>> = emptyList()

    // cor e espelhamento
    private var pointColor: Int = 0xFFFF0000.toInt() // vermelho
    private var lineColor: Int = 0xFF00FF00.toInt() // verde
    private var mirrorX: Boolean = true // normalmente true para câmera frontal

    init {
        pointPaint.color = pointColor
        linePaint.color = lineColor
    }

    /**
     * Atualiza pontos normalizados [0..1]. Se forem coordenadas em pixels, normalize antes.
     * mirror = true espelha em X (útil para câmera frontal).
     */
    fun setPoints(
        newPoints: List<Pair<Float, Float>>,
        mirror: Boolean = true,
        newConnections: List<Pair<Int, Int>> = emptyList()
    ) {
        connections = newConnections
        mirrorX = mirror
        points = newPoints
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // desenha linhas primeiro
        if (connections.isNotEmpty()) {
            for ((a, b) in connections) {
                if (a in points.indices && b in points.indices) {
                    val (ax, ay) = points[a]
                    val (bx, by) = points[b]
                    val pxA = (if (mirrorX) (1f - ax) else ax) * w
                    val pyA = ay * h
                    val pxB = (if (mirrorX) (1f - bx) else bx) * w
                    val pyB = by * h
                    canvas.drawLine(pxA, pyA, pxB, pyB, linePaint)
                }
            }
        }

        // desenha pontos
        for ((nx, ny) in points) {
            val px = (if (mirrorX) (1f - nx) else nx) * w
            val py = ny * h
            canvas.drawCircle(px, py, 6f, pointPaint)
        }
    }
}
