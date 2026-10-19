package com.ambiorix.pay

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambiorix.pay.data.PaymentRepository
import com.ambiorix.pay.data.SettingsRepository
import com.ambiorix.pay.notification.NotificationPaymentEvents
import com.ambiorix.pay.sound.SoundAnnouncer
import com.ambiorix.pay.ui.screens.HistoryScreen
import com.ambiorix.pay.ui.screens.SettingsScreen
import com.ambiorix.pay.ui.theme.AmbiorixPayTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var soundAnnouncer: SoundAnnouncer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository.getInstance(applicationContext)
        paymentRepository = PaymentRepository.getInstance(applicationContext)
        soundAnnouncer = SoundAnnouncer(this)

        setContent {
            AmbiorixPayTheme {
                val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle()
                val payments by paymentRepository.paymentsFlow.collectAsStateWithLifecycle()
                val isSpanishSupported by soundAnnouncer.isSpanishSupported.collectAsStateWithLifecycle()

                var currentSubScreen by remember { mutableStateOf("settings") }

                if (currentSubScreen == "history") {
                    HistoryScreen(
                        payments = payments,
                        onBackClick = { currentSubScreen = "settings" },
                        onClearHistory = { paymentRepository.clearHistory() }
                    )
                } else {
                    SettingsScreen(
                        settings = settings,
                        isSpanishVoiceAvailable = isSpanishSupported,
                        onSettingsChanged = { transform ->
                            settingsRepository.updateSettings(transform)
                        },
                        onBackClick = { finish() },
                        onOpenNotificationSettings = {
                            openNotificationAccessSettings()
                        },
                        onOpenScreenPinningSettings = {
                            openSecuritySettings()
                        },
                        onOpenTtsSettings = {
                            soundAnnouncer.openTtsSettings()
                        },
                        onOpenHistoryClick = {
                            currentSubScreen = "history"
                        },
                        onTestPaymentClick = {
                            // Emitir pago de prueba sin agregarlo al historial
                            NotificationPaymentEvents.emitTestPayment(35000L)
                            // Regresar a la pantalla principal para que se vea la pantalla verde inmediatamente
                            finish()
                        },
                        onTestVoiceClick = {
                            // Si la voz está apagada, la encendemos para la prueba o la probamos directamente
                            soundAnnouncer.speakTest(35000L, settings.includeThankYou)
                        }
                    )
                }
            }
        }
    }

    private fun openNotificationAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun openSecuritySettings() {
        try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundAnnouncer.release()
    }
}
