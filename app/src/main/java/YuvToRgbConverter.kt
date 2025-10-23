package com.example.faceanalysis

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.renderscript.*
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Conversor otimizado YUV → RGB.
 * Usa RenderScript (ou Bitmap fallback) para converter sem compressão.
 * Evita lag na análise de frames em tempo real.
 */
class YuvToRgbConverter(private val context: android.content.Context) {

    private var rs: RenderScript? = null
    private var scriptYuvToRgb: ScriptIntrinsicYuvToRGB? = null
    private var yuvType: Type? = null
    private var rgbaType: Type? = null
    private var yuvAllocation: Allocation? = null
    private var rgbaAllocation: Allocation? = null

    init {
        try {
            rs = RenderScript.create(context)
            scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        } catch (e: Exception) {
            Log.w("YuvToRgbConverter", "RenderScript não disponível: ${e.message}")
        }
    }

    /**
     * Converte o frame YUV do ImageProxy em Bitmap RGB.
     */
    fun yuvToRgb(image: ImageProxy, output: Bitmap) {
        try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            // Se RenderScript estiver disponível, usa ele
            if (rs != null && scriptYuvToRgb != null) {
                val yuvBytes = nv21
                if (yuvType == null || yuvType?.x != yuvBytes.size) {
                    val elemYuv = Element.U8(rs)
                    yuvType = Type.Builder(rs, elemYuv).setX(yuvBytes.size).create()
                    yuvAllocation = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT)
                    rgbaType = Type.Builder(rs, Element.RGBA_8888(rs))
                        .setX(output.width)
                        .setY(output.height)
                        .create()
                    rgbaAllocation = Allocation.createTyped(rs, rgbaType, Allocation.USAGE_SCRIPT)
                }

                yuvAllocation?.copyFrom(yuvBytes)
                scriptYuvToRgb?.setInput(yuvAllocation)
                scriptYuvToRgb?.forEach(rgbaAllocation)
                rgbaAllocation?.copyTo(output)
            } else {
                // Fallback simples caso RenderScript não esteja disponível
                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
                val jpegBytes = out.toByteArray()
                val tempBitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                val canvas = android.graphics.Canvas(output)
                canvas.drawBitmap(tempBitmap, 0f, 0f, null)
            }

        } catch (e: Exception) {
            Log.e("YuvToRgbConverter", "Erro convertendo YUV→RGB: ${e.message}")
        }
    }

    fun release() {
        try {
            yuvAllocation?.destroy()
            rgbaAllocation?.destroy()
            scriptYuvToRgb?.destroy()
            rs?.destroy()
        } catch (_: Exception) { }
    }
}
