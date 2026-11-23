package com.example.eventmanager

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object CloudSync {
    private const val BASE_URL =
        "https://fir-car-e6945-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val PREFS = "cloud_sync"
    private const val KEY_DIRTY = "dirty"

    // Một luồng duy nhất => các lần gửi chạy lần lượt, không chồng lên nhau
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = AtomicInteger(0) // số lần gửi đang chờ

    class OfflineException : Exception("No internet connection")

    /** Còn thay đổi nào trong máy chưa đưa lên cloud không? */
    fun isDirty(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DIRTY, false)

    private fun setDirty(context: Context, dirty: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DIRTY, dirty).apply()
    }

    /**
     * Đồng bộ toàn bộ dữ liệu trong máy lên cloud (chạy nền, gọi từ UI thread được).
     * onDone (tuỳ chọn) được gọi lại trên UI thread với kết quả: Pair(số event, số session).
     */
    fun sync(context: Context, onDone: ((Result<Pair<Int, Int>>) -> Unit)? = null) {
        val app = context.applicationContext
        setDirty(app, true)          // đánh dấu trước, chỉ xoá cờ khi gửi thành công
        pending.incrementAndGet()
        executor.execute {
            val result: Result<Pair<Int, Int>> =
                if (!NetworkUtils.isOnline(app)) {
                    Result.failure(OfflineException())
                } else {
                    val db = EventDbHelper(app)
                    try {
                        push(db)
                    } finally {
                        db.close()
                    }
                }
            val remaining = pending.decrementAndGet()
            if (result.isSuccess) {
                if (remaining == 0) setDirty(app, false) // còn lần gửi chờ thì giữ cờ
            } else {
                setDirty(app, true)
            }
            if (onDone != null) mainHandler.post { onDone(result) }
        }
    }

    private fun push(db: EventDbHelper): Result<Pair<Int, Int>> = runCatching {
        val events = db.getAll()
        var sessionTotal = 0
        val root = JSONObject()

        for (e in events) {
            val sessions = db.getSessionsByEvent(e.id)
            sessionTotal += sessions.size

            val eventJson = JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("date", e.date)
                put("location", e.location)
                put("organiser", e.organiser)
                put("type", e.type)
                put("status", e.status)
                put("description", e.description)
                put("contactEmail", e.contactEmail)
                put("contactPhone", e.contactPhone)
                put("sessionCount", sessions.size)
            }
            if (sessions.isNotEmpty()) {
                val sessionsJson = JSONObject()
                for (s in sessions) {
                    sessionsJson.put("session_${s.id}", JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("time", s.time)
                        put("speaker", s.speaker)
                        put("room", s.room)
                        put("type", s.type)
                        put("description", s.description)
                    })
                }
                eventJson.put("sessions", sessionsJson)
            }
            // "event_<id>" thay vì số thuần: Firebase hay biến key số liên tiếp thành mảng
            root.put("event_${e.id}", eventJson)
        }

        httpPut("$BASE_URL/eventmanager/events.json", root.toString())
        Pair(events.size, sessionTotal)
    }

    // PUT = thay thế cả node bằng dữ liệu mới (root rỗng "{}" => node bị xoá, đúng khi máy hết event)
    private fun httpPut(url: String, body: String) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "PUT"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

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