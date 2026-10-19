package com.ambiorix.pay

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ambiorix.pay.data.Payment
import com.ambiorix.pay.data.SettingsRepository
import com.ambiorix.pay.notification.NotificationPaymentEvents
import com.ambiorix.pay.notification.UiPaymentEvent
import com.ambiorix.pay.sound.SoundAnnouncer
import com.ambiorix.pay.ui.components.PinDialog
import com.ambiorix.pay.ui.screens.KioskMainScreen
import com.ambiorix.pay.ui.screens.PaymentSuccessScreen
import com.ambiorix.pay.ui.theme.AmbiorixPayTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var soundAnnouncer: SoundAnnouncer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository.getInstance(applicationContext)
        soundAnnouncer = SoundAnnouncer(this)

        configureKioskWindowFlags()
        lockBackButton()

        setContent {
            AmbiorixPayTheme {
                val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle()
                var currentPayment by remember { mutableStateOf<Payment?>(null) }
                var showPinDialog by remember { mutableStateOf(false) }
                var isNotificationAccessGranted by remember { mutableStateOf(checkNotificationAccess()) }

                // Monitorear pagos entrantes (reales o simulados)
                LaunchedEffect(Unit) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        NotificationPaymentEvents.paymentEvents.collect { event ->
                            val payment = when (event) {
                                is UiPaymentEvent.RealPayment -> event.payment
                                is UiPaymentEvent.SimulatedTestPayment -> event.payment
                            }
                            // Reproducir aviso sonoro y síntesis de voz en español
                            soundAnnouncer.announce(
                                amount = payment.amount,
                                playSound = settings.soundEnabled,
                                speakVoice = settings.ttsEnabled,
                                includeThankYou = settings.includeThankYou
                            )
                            // Mostrar pantalla verde
                            currentPayment = payment
                        }
                    }
                }

                // Actualizar verificación de permisos al volver a primer plano
                DisposableEffect(Unit) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hideSystemBars()
                            isNotificationAccessGranted = checkNotificationAccess()
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Pantalla Principal Kiosco con códigos QR
                    KioskMainScreen(
                        settings = settings,
                        isNotificationAccessGranted = isNotificationAccessGranted,
                        onOpenSettingsClick = { showPinDialog = true },
                        onRequestNotificationAccess = { openNotificationSettings() }
                    )

                    // Pantalla de Confirmación Verde con animación de entrada
                    AnimatedVisibility(
                        visible = currentPayment != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        currentPayment?.let { payment ->
                            PaymentSuccessScreen(
                                payment = payment,
                                durationSeconds = settings.confirmationDurationSeconds,
                                onTimeout = {
                                    currentPayment = null
                                }
                            )
                        }
                    }

                    // Modal de PIN para ingresar a la configuración
                    if (showPinDialog) {
                        PinDialog(
                            correctPin = settings.pinCode,
                            onDismissRequest = { showPinDialog = false },
                            onPinSuccess = {
                                showPinDialog = false
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }

    private fun configureKioskWindowFlags() {
        // Mantener la pantalla permanentemente encendida en el local comercial
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Encender y mostrar incluso si el dispositivo se llega a bloquear
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun lockBackButton() {
        // En modo kiosco bloqueamos el botón Atrás para que clientes o toques accidentales
        // no cierren la aplicación
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacer nada deliberadamente (bloqueado para modo kiosco)
            }
        })
    }

    private fun checkNotificationAccess(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledPackages.contains(packageName)
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundAnnouncer.release()
    }
}
