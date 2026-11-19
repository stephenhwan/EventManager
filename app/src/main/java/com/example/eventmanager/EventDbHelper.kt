package com.example.eventmanager

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EventDbHelper(context: Context) :
    SQLiteOpenHelper(context, "events.db", null, 1) {

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
                status TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS events")
        onCreate(db)
    }

    private fun toValues(e: Event) = ContentValues().apply {
        put("name", e.name)
        put("date", e.date)
        put("location", e.location)
        put("organiser", e.organiser)
        put("type", e.type)
        put("status", e.status)
    }

    private fun Cursor.toEvent() = Event(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        date = getString(getColumnIndexOrThrow("date")),
        location = getString(getColumnIndexOrThrow("location")),
        organiser = getString(getColumnIndexOrThrow("organiser")),
        type = getString(getColumnIndexOrThrow("type")),
        status = getString(getColumnIndexOrThrow("status"))
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
}