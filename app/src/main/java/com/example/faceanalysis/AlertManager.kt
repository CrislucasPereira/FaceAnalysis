package com.example.faceanalysis

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.faceanalysis.R

class AlertManager(private val context: Context) {

    private var mpMicrosleep: MediaPlayer? = null
    private var mpBocejo: MediaPlayer? = null
    private var mpDesatencao: MediaPlayer? = null
    private var mpSemRosto: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())

    private var isMicrosleepPlaying = false
    private var isDesattentionActive = false
    private var isBocejoPlaying = false

    companion object {
        private const val TAG = "AlertManager"
    }

    init {
        try {
            // Sons curtos (bipes) e longos devem estar em /res/raw/
            mpMicrosleep = MediaPlayer.create(context, R.raw.alerta_microsleep) // som contínuo
            mpBocejo = MediaPlayer.create(context, R.raw.alerta_bocejo)         // som curto de bip
            mpDesatencao = MediaPlayer.create(context, R.raw.alerta_bip)        // som simples de bip
            mpSemRosto = MediaPlayer.create(context, R.raw.alerta_falta_rosto)  // bip duplo ou diferente
        } catch (e: Exception) {
            Log.e(TAG, "Erro carregando sons: ${e.message}")
        }
    }

    // --- MICROSLEEP: toca contínuo até parar ---
    fun playMicrosleep() {
        if (isMicrosleepPlaying) return
        try {
            isMicrosleepPlaying = true
            mpMicrosleep?.isLooping = true
            mpMicrosleep?.setVolume(1f, 1f)
            mpMicrosleep?.start()
            Log.d(TAG, "🔊 Som Microsleep contínuo iniciado")
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
            Log.d(TAG, "🔇 Som Microsleep parado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro parando som Microsleep: ${e.message}")
        }
    }

    // --- DESATENÇÃO: 3 bipes -> pausa -> repete enquanto ativo ---
    fun startDesattentionLoop() {
        if (isDesattentionActive) return
        isDesattentionActive = true
        loopDesattention()
    }

    private fun loopDesattention() {
        if (!isDesattentionActive) return

        // toca 3 bipes seguidos
        repeat(3) { i ->
            handler.postDelayed({
                mpDesatencao?.seekTo(0)
                mpDesatencao?.start()
            }, (i * 400).toLong()) // intervalo entre bipes
        }

        // repete após 2s + tempo dos 3 bipes (~3.2s total)
        handler.postDelayed({
            loopDesattention()
        }, 3200)
    }

    fun stopDesattentionLoop() {
        isDesattentionActive = false
        handler.removeCallbacksAndMessages(null)
    }

    // --- BOCEJO: toca bipes até o usuário clicar em OK ---
    fun startBocejoLoop() {
        if (isBocejoPlaying) return
        isBocejoPlaying = true
        loopBocejo()
    }

    private fun loopBocejo() {
        if (!isBocejoPlaying) return
        mpBocejo?.seekTo(0)
        mpBocejo?.start()
        handler.postDelayed({ loopBocejo() }, 600) // 600 ms entre bipes
    }

    fun stopBocejoLoop() {
        isBocejoPlaying = false
        handler.removeCallbacksAndMessages(null)
    }

    // --- SEM ROSTO: apenas uma vez ---
    fun playSemRosto() {
        try {
            mpSemRosto?.seekTo(0)
            mpSemRosto?.start()
            Log.d(TAG, "🔊 Som 'Sem Rosto' tocado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro tocando som Sem Rosto: ${e.message}")
        }
    }

    // --- Libera tudo ---
    fun release() {
        try {
            stopMicrosleep()
            stopBocejoLoop()
            stopDesattentionLoop()
            mpMicrosleep?.release()
            mpBocejo?.release()
            mpDesatencao?.release()
            mpSemRosto?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar sons: ${e.message}")
        }
    }
}
