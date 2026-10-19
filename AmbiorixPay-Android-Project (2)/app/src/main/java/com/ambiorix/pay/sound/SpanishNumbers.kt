package com.ambiorix.pay.sound

/**
 * Conversor numérico a palabras en español con gramática, ortografía y fonética precisa:
 * - 35000 -> "treinta y cinco mil pesos"
 * - 21000 -> "veintiún mil pesos"
 * - 100000 -> "cien mil pesos"
 * - 1250 -> "mil doscientos cincuenta pesos"
 * - 1000000 -> "un millón de pesos"
 * - 1500000 -> "un millón quinientos mil pesos"
 *
 * Construye una única frase natural sin dígitos ni símbolos numéricos,
 * utilizando únicamente una coma y un punto como pausa fluida:
 * "Pago recibido, treinta y cinco mil pesos. Gracias por tu compra."
 */
object SpanishNumbers {

    private val UNITS = arrayOf(
        "", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete",
        "dieciocho", "diecinueve", "veinte", "veintiún", "veintidós", "veintitrés",
        "veinticuatro", "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve"
    )

    private val TENS = arrayOf(
        "", "", "", "treinta", "cuarenta", "cincuenta",
        "sesenta", "setenta", "ochenta", "noventa"
    )

    private val HUNDREDS = arrayOf(
        "", "ciento", "doscientos", "trescientos", "cuatrocientos",
        "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"
    )

    /**
     * Convierte un número menor a 1000 en palabras en español.
     */
    fun convertGroupUnderThousand(n: Int): String {
        if (n == 0) return ""
        if (n == 100) return "cien"

        val sb = StringBuilder()

        val h = n / 100
        val rem100 = n % 100

        if (h > 0) {
            sb.append(HUNDREDS[h])
        }

        if (rem100 > 0) {
            if (sb.isNotEmpty()) sb.append(" ")

            if (rem100 < 30) {
                sb.append(UNITS[rem100])
            } else {
                val t = rem100 / 10
                val u = rem100 % 10
                sb.append(TENS[t])
                if (u > 0) {
                    sb.append(" y ")
                    sb.append(UNITS[u])
                }
            }
        }

        return sb.toString().trim()
    }

    /**
     * Convierte cualquier monto entero no negativo en su expresión en palabras puras
     * (sin dígitos, sin puntos de miles).
     */
    fun toWords(amount: Long): String {
        if (amount == 0L) return "cero"
        if (amount < 0) return "menos " + toWords(-amount)

        val millions = (amount / 1_000_000).toInt()
        val thousands = ((amount % 1_000_000) / 1_000).toInt()
        val units = (amount % 1_000).toInt()

        val parts = mutableListOf<String>()

        // Millones
        if (millions > 0) {
            if (millions == 1) {
                parts.add("un millón")
            } else {
                parts.add(convertGroupUnderThousand(millions) + " millones")
            }
        }

        // Miles
        if (thousands > 0) {
            if (thousands == 1) {
                parts.add("mil")
            } else {
                parts.add(convertGroupUnderThousand(thousands) + " mil")
            }
        }

        // Unidades
        if (units > 0) {
            parts.add(convertGroupUnderThousand(units))
        }

        return parts.joinToString(" ").trim()
    }

    /**
     * Construye UNA sola cadena de texto completa para TextToSpeech.
     * Estructura:
     * - Con agradecimiento: "Pago recibido, <monto en palabras> pesos. Gracias por tu compra."
     * - Sin agradecimiento: "Pago recibido, <monto en palabras> pesos."
     *
     * Maneja correctamente "de pesos" para múltiplos exactos de millón (ej: "un millón de pesos").
     */
    fun buildPaymentAnnouncementPhrase(amount: Long, includeThankYou: Boolean = true): String {
        val words = toWords(amount)
        val amountWithCurrency = if (amount == 1L) {
            "un peso"
        } else if (amount % 1_000_000 == 0L && amount > 0L) {
            "$words de pesos"
        } else {
            "$words pesos"
        }

        return if (includeThankYou) {
            "Pago recibido, $amountWithCurrency. Gracias por tu compra."
        } else {
            "Pago recibido, $amountWithCurrency."
        }
    }

    /**
     * Alias compatible con versiones previas.
     */
    fun toPaymentAnnouncementPhrase(amount: Long): String {
        return buildPaymentAnnouncementPhrase(amount, includeThankYou = false)
    }
}
