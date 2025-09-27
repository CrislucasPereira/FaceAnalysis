package com.example.faceanalysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
        strokeWidth = 6f
    }

    private var points: List<Pair<Float, Float>> = emptyList()
    private var isFrontCamera: Boolean = true
    private var rotationDegrees: Int = 0

    fun setPoints(
        newPoints: List<Pair<Float, Float>>,
        isFrontCamera: Boolean = true,
        rotationDegrees: Int = 0
    ) {
        this.points = newPoints
        this.isFrontCamera = isFrontCamera
        this.rotationDegrees = rotationDegrees
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        for ((x, y) in points) {
            var drawX = x
            var drawY = y

            when (rotationDegrees) {
                90 -> {
                    drawX = y
                    drawY = 1f - x
                }
                180 -> {
                    drawX = 1f - x
                    drawY = 1f - y
                }
                270 -> {
                    drawX = 1f - y
                    drawY = x
                }
            }

            if (isFrontCamera) {
                drawX = 1f - drawX
            }

            val screenX = drawX * w
            val screenY = drawY * h

            canvas.drawCircle(screenX, screenY, 10f, paint)
        }
    }
}
