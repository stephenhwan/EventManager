package com.example.eventmanager

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CloudSync {
    private const val BASE_URL =
        "https://fir-car-e6945-default-rtdb.asia-southeast1.firebasedatabase.app"

    /** Gửi toàn bộ event lên /eventmanager/events. Phải gọi ở luồng nền (không phải UI thread). */
    fun uploadAll(events: List<Event>): Result<Unit> = runCatching {
        val root = JSONObject()
        for (e in events) {
            // Dùng "event_<id>" thay vì "<id>" thuần số: Firebase hay tự đổi key số
            // liên tiếp thành mảng khi đọc lại
            root.put("event_${e.id}", JSONObject().apply {
                put("name", e.name)
                put("date", e.date)
                put("location", e.location)
                put("organiser", e.organiser)
                put("type", e.type)
                put("status", e.status)
                put("description", e.description)
                put("contactEmail", e.contactEmail)
                put("contactPhone", e.contactPhone)
            })
        }

        val conn = URL("$BASE_URL/eventmanager/events.json").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "PUT"          // PUT = thay thế cả node bằng dữ liệu mới
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("HTTP $code $detail")
            }
        } finally {
            conn.disconnect()
        }
    }
}