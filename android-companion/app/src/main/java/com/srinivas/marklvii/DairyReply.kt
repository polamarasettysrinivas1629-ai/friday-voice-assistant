package com.srinivas.marklvii

import java.util.Locale

internal object DairyReply {
    private val bill = Regex("""\b(bill|balance|invoice|amount due)\b|బిల్లు|బాకీ""")
    private val milk = Regex("""\bmilk\b|పాలు|పాల[ు]""")
    private val request = Regex("""\b(need|want|order|send|deliver)\b|కావాలి|పంపండి|ఆర్డర్""")

    fun forMessage(message: String): String? {
        val plain = message.lowercase(Locale.ROOT)
        val telugu = plain.any { it in '\u0C00'..'\u0C7F' }
        return when {
            bill.containsMatchIn(plain) -> if (telugu)
                "మీ బిల్లు వివరాలు చూసి మా డైరీ బృందం మీకు తెలియజేస్తుంది."
            else
                "Our dairy team will check your bill and get back to you."
            milk.containsMatchIn(plain) && request.containsMatchIn(plain) -> if (telugu)
                "ఎన్ని లీటర్లు కావాలి? ఏ తేదీకి కావాలో చెప్పండి."
            else
                "How many litres of milk do you need, and for which date?"
            else -> null
        }
    }
}
