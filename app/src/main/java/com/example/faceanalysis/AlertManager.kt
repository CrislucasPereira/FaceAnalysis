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

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar sons: ${e.message}")
        }
    }

    fun handleEvent(event: String) {
        when (event) {
            "Microsleep" -> {
                soundPool?.play(soundMicrosleep, 1f, 1f, 1, 0, 1f)
                Log.d(TAG, "🔊 Som Microsleep ativado")
            }
            "Bocejo" -> {
                soundPool?.play(soundBocejo, 1f, 1f, 1, 0, 1f)
                Log.d(TAG, "🔊 Som Bocejo ativado")
            }
            "SemRosto" -> {
                soundPool?.play(soundNoFace, 1f, 1f, 1, 0, 1f)
                Log.d(TAG, "🔊 Som Falta de Rosto ativado")
            }
            else -> {
                // Evento ignorado sem log
            }
        }
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
