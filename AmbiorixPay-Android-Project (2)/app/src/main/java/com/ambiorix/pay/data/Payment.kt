package com.ambiorix.pay.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Estado interno de la app.
 * La app NUNCA afirma que el banco confirmó el pago.
 * El estado interno es PAYMENT_DETECTED y la interfaz dice "PAGO RECIBIDO".
 */
enum class PaymentStatus {
    IDLE,
    PAYMENT_DETECTED
}

/**
 * Representa una transacción detectada a través del NotificationListenerService.
 */
data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val amount: Long,
    val appName: String,
    val packageName: String,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val formattedAmount: String
        get() {
            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            format.maximumFractionDigits = 0
            return format.format(amount)
        }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            return sdf.format(Date(timestampMillis))
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return sdf.format(Date(timestampMillis))
        }
}
