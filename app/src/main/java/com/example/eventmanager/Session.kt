package com.example.eventmanager

data class Session(
    val id: Long = 0,            // ID tự sinh bởi SQLite
    val eventId: Long,           // session này thuộc event nào
    val name: String,
    val time: String,            // dạng "HH:mm", ví dụ "09:30"
    val speaker: String = "",
    val room: String = "",
    val type: String,
    val description: String = ""
)