package com.example.faceanalysis

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

class AlertManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var soundMicrosleep: Int = 0
    private var soundBocejo: Int = 0
    private var soundNoFace: Int = 0

    // Evita disparar o mesmo som várias vezes seguidas
    private var lastEventPlayed: String? = null
    private var lastPlayTime: Long = 0L
    private val MIN_INTERVAL_MS = 3000L // intervalo mínimo entre sons iguais

    companion object {
        private const val TAG = "AlertManager"
    }

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(3)
                .build()

            soundMicrosleep = soundPool!!.load(context, R.raw.alerta_microsleep, 1)
            soundBocejo = soundPool!!.load(context, R.raw.alerta_bocejo, 1)
            soundNoFace = soundPool!!.load(context, R.raw.alerta_falta_rosto, 1)

            Log.d(TAG, "🎧 Sons carregados com sucesso")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar sons: ${e.message}")
        }
    }

    fun handleEvent(event: String) {
        val now = System.currentTimeMillis()

        // Evita som repetido em menos de 3 segundos
        if (event == lastEventPlayed && (now - lastPlayTime < MIN_INTERVAL_MS)) {
            Log.d(TAG, "🔇 Ignorado som duplicado de $event")
            return
        }

        when (event) {
            "Microsleep" -> {
                soundPool?.play(soundMicrosleep, 1f, 1f, 1, 0, 1f)
                Log.d(TAG, "🔊 Som Microsleep ativado")
            }
            "Bocejo" -> {
                soundPool?.play(soundBocejo, 1f, 1f, 1, 0, 1f)
                Log.d(TAG, "🔊 Som Bocejo ativado")
            }
            "Desatenção" -> {
                soundPool?.play(soundBocejo, 1f, 1f, 1, 0, 1f)
                Log.d(TAG, "🔊 Som Desatenção (usa mesmo áudio do bocejo)")
            }
            "SemRosto" -> {
                soundPool?.play(soundNoFace, 1f, 1f, 1, 0, 1f)
                Log.d(TAG, "🔊 Som Falta de Rosto ativado")
            }
            else -> {
                Log.d(TAG, "⚪ Evento sem som configurado: $event")
            }
        }

        lastEventPlayed = event
        lastPlayTime = now
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            Log.d(TAG, "🔇 Sons pausados/liberados")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar sons: ${e.message}")
        }
    }
}
