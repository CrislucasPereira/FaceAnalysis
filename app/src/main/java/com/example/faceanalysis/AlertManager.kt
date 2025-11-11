package com.example.faceanalysis

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Gerencia a reproducao de alertas sonoros da aplicacao.
 *
 * Responsabilidades:
 * - Carregar e liberar recursos de audio em res/raw.
 * - Tocar alarmes continuos (microsleep) e bipes em laco (bocejo/desatencao).
 * - Evitar sobreposicao de sons e vazamentos de recursos.
 */
class AlertManager(private val context: Context) {

    private var mpMicrosleep: MediaPlayer? = null
    private var mpBocejo: MediaPlayer? = null
    private var mpSemRosto: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())

    private var isMicrosleepPlaying = false
    private var isBocejoPlaying = false

    companion object {
        private const val TAG = "AlertManager"
    }

    init {
        try {
            // Sons curtos (bipes) e longos devem estar em /res/raw/
            mpMicrosleep = MediaPlayer.create(context, R.raw.alerta_microsleep)
            mpBocejo = MediaPlayer.create(context, R.raw.alerta_bocejo)
            mpSemRosto = MediaPlayer.create(context, R.raw.alerta_falta_rosto)
        } catch (e: Exception) {
            Log.e(TAG, "Erro carregando sons: ${e.message}")
        }
    }

    // MICROSLEEP: toca continuo ate parar
    fun playMicrosleep() {
        if (isMicrosleepPlaying) return
        try {
            isMicrosleepPlaying = true
            mpMicrosleep?.isLooping = true
            mpMicrosleep?.setVolume(1f, 1f)
            mpMicrosleep?.start()
            Log.d(TAG, "Som Microsleep continuo iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro iniciando som Microsleep: ${e.message}")
        }
    }

    fun stopMicrosleep() {
        if (!isMicrosleepPlaying) return
        try {
            mpMicrosleep?.pause()
            mpMicrosleep?.seekTo(0)
            isMicrosleepPlaying = false
            Log.d(TAG, "Som Microsleep parado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro parando som Microsleep: ${e.message}")
        }
    }

    // Removido: alerta de desatenção

    // BOCEJO: toca bipes ate o usuario clicar em OK
    fun startBocejoLoop() {
        if (isBocejoPlaying) return
        isBocejoPlaying = true
        loopBocejo()
    }

    private fun loopBocejo() {
        if (!isBocejoPlaying) return
        mpBocejo?.seekTo(0)
        mpBocejo?.start()
        handler.postDelayed({ loopBocejo() }, 600)
    }

    fun stopBocejoLoop() {
        isBocejoPlaying = false
        handler.removeCallbacksAndMessages(null)
    }

    // SEM ROSTO: apenas uma vez
    fun playSemRosto() {
        try {
            mpSemRosto?.seekTo(0)
            mpSemRosto?.start()
            Log.d(TAG, "Som 'Sem Rosto' tocado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro tocando som Sem Rosto: ${e.message}")
        }
    }

    // Libera recursos
    fun release() {
        try {
            stopMicrosleep()
            stopBocejoLoop()
            mpMicrosleep?.release()
            mpBocejo?.release()
            mpSemRosto?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar sons: ${e.message}")
        }
    }
}
