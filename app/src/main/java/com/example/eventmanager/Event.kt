package com.example.eventmanager

import android.R

data class Event(
    val id: Long = 0,
    val name: String,
    val date: String,
    val location: String,
    val organiser: String,
    val type: String,
    val status: String,
    val description: String = "",
    val contactEmail: String = "",
    val contactPhone: String = ""
)