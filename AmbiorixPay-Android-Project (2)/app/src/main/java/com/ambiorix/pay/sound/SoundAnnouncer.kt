package com.ambiorix.pay.sound

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Anunciador sonoro y por voz de pagos recibidos.
 *
 * Mejoras de robustez para asegurar que SUENE SIEMPRE:
 * 1. Usa AudioManager.STREAM_MUSIC para el tono y USAGE_MEDIA para TextToSpeech.
 *    Esto garantiza que suene por el altavoz multimedia incluso si el teléfono tiene silenciadas
 *    las notificaciones o está en modo vibración.
 * 2. Si TextToSpeech aún se está inicializando cuando llega el pago, se encola la frase
 *    y se reproduce inmediatamente al completar la inicialización (sin descartar el pago).
 * 3. Si es-CO no está disponible, prueba es-US, es-ES, es genérico y finalmente Locale.getDefault().
 * 4. Fija volumen máximo en el generador de tonos y velocidad 0.95f natural.
 */
class SoundAnnouncer(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        const val DEFAULT_SPEECH_RATE = 0.95f
        const val DEFAULT_PITCH = 1.0f
    }

    private var toneGenerator: ToneGenerator? = null
    private var textToSpeech: TextToSpeech? = null

    @Volatile
    private var ttsReady = false

    @Volatile
    private var pendingSpeechPhrase: String? = null

    private val _isSpanishSupported = MutableStateFlow(true)
    val isSpanishSupported: StateFlow<Boolean> = _isSpanishSupported.asStateFlow()

    init {
        // STREAM_MUSIC en lugar de STREAM_NOTIFICATION para sonar fuerte por el altavoz multimedia
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
            } catch (e2: Exception) {
                toneGenerator = null
            }
        }

        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            textToSpeech = null
            _isSpanishSupported.value = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return

            // Configurar salida de audio por canal multimedia
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts.setAudioAttributes(audioAttributes)
            }

            // Buscar el mejor idioma en español disponible
            var langFound = false
            val localesToTry = listOf(
                Locale("es", "CO"),
                Locale("es", "US"),
                Locale("es", "ES"),
                Locale("es"),
                Locale.getDefault()
            )

            for (loc in localesToTry) {
                val res = tts.setLanguage(loc)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    langFound = true
                    break
                }
            }

            tts.setPitch(DEFAULT_PITCH)
            tts.setSpeechRate(DEFAULT_SPEECH_RATE)

            _isSpanishSupported.value = langFound
            ttsReady = true

            // Si había una frase encolada mientras inicializaba, hablarla ahora
            pendingSpeechPhrase?.let { phrase ->
                pendingSpeechPhrase = null
                tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "PAYMENT_PENDING")
            }
        } else {
            _isSpanishSupported.value = false
            ttsReady = false
        }
    }

    /**
     * Emite el tono de confirmación si soundEnabled está activo.
     */
    fun playChime() {
        try {
            // Intentar re-instanciar si se liberó
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
        } catch (e: Exception) {
            // Fallback a beep estándar
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
            } catch (e2: Exception) {
                // Silencioso si falla el hardware
            }
        }
    }

    /**
     * Habla en voz alta una sola frase natural continua usando QUEUE_FLUSH.
     * Ejemplo: "Pago recibido, treinta y cinco mil pesos. Gracias por tu compra."
     */
    fun speakPayment(amount: Long, includeThankYou: Boolean = true) {
        val phrase = SpanishNumbers.buildPaymentAnnouncementPhrase(amount, includeThankYou)

        if (!ttsReady || textToSpeech == null) {
            // Encolar para cuando onInit termine
            pendingSpeechPhrase = phrase
            return
        }

        try {
            textToSpeech?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "PAYMENT_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            // Error de síntesis
        }
    }

    /**
     * Prueba de voz directa para el botón de Configuración.
     */
    fun speakTest(amount: Long = 35000L, includeThankYou: Boolean = true) {
        speakPayment(amount, includeThankYou)
    }

    fun announce(amount: Long, playSound: Boolean, speakVoice: Boolean, includeThankYou: Boolean) {
        if (playSound) {
            playChime()
        }
        if (speakVoice) {
            speakPayment(amount, includeThankYou)
        }
    }

    /**
     * Intenta abrir los ajustes de texto a voz del sistema o el instalador de voces.
     */
    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(installIntent)
            } catch (e2: Exception) {
                val generalIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(generalIntent)
            }
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignorar
        }

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            ttsReady = false
        } catch (e: Exception) {
            // Ignorar
        }
    }
}
