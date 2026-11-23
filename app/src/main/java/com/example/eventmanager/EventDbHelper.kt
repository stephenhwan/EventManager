package com.example.eventmanager

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils // MỚI
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EventDbHelper(context: Context) :
    SQLiteOpenHelper(context, "events.db", null, 3) { // MỚI: đổi 2 -> 3

    // MỚI: bật khóa ngoại. SQLite mặc định KHÔNG kiểm tra khóa ngoại,
    // thiếu dòng này thì "xoá event thì xoá session" sẽ không chạy.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                date TEXT NOT NULL,
                location TEXT NOT NULL,
                organiser TEXT NOT NULL,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                contact_email TEXT NOT NULL DEFAULT '',
                contact_phone TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        createSessionsTable(db) // MỚI: máy cài mới sẽ có luôn bảng sessions
    }

    // MỚI: hàm tạo bảng sessions (dùng ở onCreate và onUpgrade)
    private fun createSessionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                time TEXT NOT NULL,
                speaker TEXT NOT NULL DEFAULT '',
                room TEXT NOT NULL DEFAULT '',
                type TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    // Không xoá bảng khi nâng cấp để khỏi mất dữ liệu người dùng
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE events ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE events ADD COLUMN contact_email TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE events ADD COLUMN contact_phone TEXT NOT NULL DEFAULT ''")
        }
        // MỚI: máy đang ở version 1 hoặc 2 sẽ được thêm bảng sessions
        if (oldVersion < 3) {
            createSessionsTable(db)
        }
    }

    private fun toValues(e: Event) = ContentValues().apply {
        put("name", e.name)
        put("date", e.date)
        put("location", e.location)
        put("organiser", e.organiser)
        put("type", e.type)
        put("status", e.status)
        put("description", e.description)
        put("contact_email", e.contactEmail)
        put("contact_phone", e.contactPhone)
    }

    private fun Cursor.toEvent() = Event(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        date = getString(getColumnIndexOrThrow("date")),
        location = getString(getColumnIndexOrThrow("location")),
        organiser = getString(getColumnIndexOrThrow("organiser")),
        type = getString(getColumnIndexOrThrow("type")),
        status = getString(getColumnIndexOrThrow("status")),
        description = getString(getColumnIndexOrThrow("description")),
        contactEmail = getString(getColumnIndexOrThrow("contact_email")),
        contactPhone = getString(getColumnIndexOrThrow("contact_phone"))
    )

    fun insert(e: Event): Long = writableDatabase.insert("events", null, toValues(e))

    fun update(e: Event): Int =
        writableDatabase.update("events", toValues(e), "id=?", arrayOf(e.id.toString()))

    fun delete(id: Long): Int =
        writableDatabase.delete("events", "id=?", arrayOf(id.toString()))

    fun getAll(): List<Event> {
        val list = mutableListOf<Event>()
        readableDatabase.query("events", null, null, null, null, null, "id DESC").use { c ->
            while (c.moveToNext()) list.add(c.toEvent())
        }
        return list
    }

    fun getById(id: Long): Event? {
        readableDatabase.query("events", null, "id=?", arrayOf(id.toString()), null, null, null)
            .use { c -> return if (c.moveToFirst()) c.toEvent() else null }
    }

    // Kiểm tra trùng tên + ngày.
    // excludeId là id của event đang sửa, để sửa event không bị báo trùng với chính nó
    fun existsByNameAndDate(name: String, date: String, excludeId: Long = -1L): Boolean {
        readableDatabase.query(
            "events", arrayOf("id"),
            "name = ? COLLATE NOCASE AND date = ? AND id != ?",
            arrayOf(name, date, excludeId.toString()),
            null, null, null, "1"
        ).use { c -> return c.moveToFirst() }
    }

    // ================= MỚI: SESSIONS =================

    private fun toValues(s: Session) = ContentValues().apply {
        put("event_id", s.eventId)
        put("name", s.name)
        put("time", s.time)
        put("speaker", s.speaker)
        put("room", s.room)
        put("type", s.type)
        put("description", s.description)
    }

    private fun Cursor.toSession() = Session(
        id = getLong(getColumnIndexOrThrow("id")),
        eventId = getLong(getColumnIndexOrThrow("event_id")),
        name = getString(getColumnIndexOrThrow("name")),
        time = getString(getColumnIndexOrThrow("time")),
        speaker = getString(getColumnIndexOrThrow("speaker")),
        room = getString(getColumnIndexOrThrow("room")),
        type = getString(getColumnIndexOrThrow("type")),
        description = getString(getColumnIndexOrThrow("description"))
    )

    fun insertSession(s: Session): Long = writableDatabase.insert("sessions", null, toValues(s))

    fun updateSession(s: Session): Int =
        writableDatabase.update("sessions", toValues(s), "id=?", arrayOf(s.id.toString()))

    fun deleteSession(id: Long): Int =
        writableDatabase.delete("sessions", "id=?", arrayOf(id.toString()))

    fun getSessionById(id: Long): Session? {
        readableDatabase.query("sessions", null, "id=?", arrayOf(id.toString()), null, null, null)
            .use { c -> return if (c.moveToFirst()) c.toSession() else null }
    }

    // Các session của một event, xếp theo giờ ("HH:mm" 24 giờ nên xếp theo chữ cũng đúng)
    fun getSessionsByEvent(eventId: Long): List<Session> {
        val list = mutableListOf<Session>()
        readableDatabase.query(
            "sessions", null, "event_id=?", arrayOf(eventId.toString()),
            null, null, "time ASC, id ASC"
        ).use { c ->
            while (c.moveToNext()) list.add(c.toSession())
        }
        return list
    }

    // Tổng số session của MỘT event (dùng ở màn hình chi tiết)
    fun countSessions(eventId: Long): Int =
        DatabaseUtils.queryNumEntries(
            readableDatabase, "sessions", "event_id=?", arrayOf(eventId.toString())
        ).toInt()

    // Số session của MỌI event cùng lúc: map eventId -> số lượng (dùng ở danh sách màn hình chính)
    fun getSessionCounts(): Map<Long, Int> {
        val map = mutableMapOf<Long, Int>()
        readableDatabase.rawQuery(
            "SELECT event_id, COUNT(*) FROM sessions GROUP BY event_id", null
        ).use { c ->
            while (c.moveToNext()) map[c.getLong(0)] = c.getInt(1)
        }
        return map
    }
    // ================= SEARCH =================

    // Chuẩn bị chuỗi cho LIKE: "escape" ký tự \ % _ để chữ người dùng gõ được hiểu đúng nghĩa đen
// (nếu không, gõ "%" sẽ khớp với mọi thứ). prefixOnly = true => khớp đầu chuỗi (abc%)
    private fun likeArg(q: String, prefixOnly: Boolean = false): String {
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return if (prefixOnly) "$escaped%" else "%$escaped%"
    }

    // Tìm event theo tên HOẶC mô tả. Event có tên bắt đầu bằng chữ vừa gõ được xếp lên trên.
    fun searchEvents(query: String): List<Event> {
        val contains = likeArg(query)
        val prefix = likeArg(query, prefixOnly = true)
        val list = mutableListOf<Event>()
        readableDatabase.rawQuery(
            """
        SELECT * FROM events
        WHERE name LIKE ? ESCAPE '\' OR description LIKE ? ESCAPE '\'
        ORDER BY CASE WHEN name LIKE ? ESCAPE '\' THEN 0 ELSE 1 END, id DESC
        """.trimIndent(),
            arrayOf(contains, contains, prefix)
        ).use { c ->
            while (c.moveToNext()) list.add(c.toEvent())
        }
        return list
    }

    // Tìm session trong MỘT event theo tên HOẶC mô tả
    fun searchSessions(eventId: Long, query: String): List<Session> {
        val like = likeArg(query)
        val list = mutableListOf<Session>()
        readableDatabase.query(
            "sessions", null,
            "event_id = ? AND (name LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\')",
            arrayOf(eventId.toString(), like, like),
            null, null, "time ASC, id ASC"
        ).use { c ->
            while (c.moveToNext()) list.add(c.toSession())
        }
        return list
    }

    // Tìm nâng cao: theo ngày (khớp đúng) và/hoặc organiser (khớp chứa). Có cả hai thì phải thoả cả hai (AND).
    fun advancedSearch(date: String, organiser: String): List<Event> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (date.isNotEmpty()) {
            clauses += "date = ?"
            args += date
        }
        if (organiser.isNotEmpty()) {
            clauses += "organiser LIKE ? ESCAPE '\\'"
            args += likeArg(organiser)
        }
        val list = mutableListOf<Event>()
        readableDatabase.query(
            "events", null,
            if (clauses.isEmpty()) null else clauses.joinToString(" AND "),
            if (args.isEmpty()) null else args.toTypedArray(),
            null, null, "id DESC"
        ).use { c ->
            while (c.moveToNext()) list.add(c.toEvent())
        }
        return list
    }
}