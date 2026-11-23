package com.example.eventmanager

import java.text.SimpleDateFormat
import java.util.Locale

object Validators {
    // to check field date of user input
    private val DATE_PATTERN = Regex("""\d{2}/\d{2}/\d{4}""")
    // to check field email of user input
    private val EMAIL_PATTERN =
        Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9\-]+(\.[A-Za-z0-9\-]+)*\.[A-Za-z]{2,}""")
    // to check field phone of user input
    private val PHONE_SEPARATORS = Regex("""[\s.\-()]""")
    private val PHONE_PATTERN = Regex("""\+?\d{10}""")
    // to check field time (HH:mm, 24h) of session
    private val TIME_PATTERN = Regex("""([01]\d|2[0-3]):[0-5]\d""")

    fun isValidDate(s: String): Boolean {
        if (!DATE_PATTERN.matches(s)) return false
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
        return runCatching { format.parse(s) }.isSuccess
    }

    fun isValidEmail(s: String): Boolean = EMAIL_PATTERN.matches(s)

    fun isValidPhone(s: String): Boolean =
        PHONE_PATTERN.matches(s.replace(PHONE_SEPARATORS, ""))

    fun isValidTime(s: String): Boolean = TIME_PATTERN.matches(s)
}
