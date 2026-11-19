package com.example.eventmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditEventActivity : AppCompatActivity() {

    private val types = listOf("Conference", "Workshop", "Seminar", "Social", "Other")
    private val statuses = listOf("Upcoming", "Completed", "Cancelled")

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
    private val datePattern = Regex("""\d{2}/\d{2}/\d{4}""")

    private lateinit var db: EventDbHelper
    private var eventId = -1L

    private lateinit var tilName: TextInputLayout
    private lateinit var tilDate: TextInputLayout
    private lateinit var tilLocation: TextInputLayout
    private lateinit var tilOrganiser: TextInputLayout
    private lateinit var edtName: TextInputEditText
    private lateinit var edtDate: TextInputEditText
    private lateinit var edtLocation: TextInputEditText
    private lateinit var edtOrganiser: TextInputEditText
    private lateinit var spType: Spinner
    private lateinit var spStatus: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_event)

        db = EventDbHelper(this)

        tilName = findViewById(R.id.tilName)
        tilDate = findViewById(R.id.tilDate)
        tilLocation = findViewById(R.id.tilLocation)
        tilOrganiser = findViewById(R.id.tilOrganiser)
        edtName = findViewById(R.id.edtName)
        edtDate = findViewById(R.id.edtDate)
        edtLocation = findViewById(R.id.edtLocation)
        edtOrganiser = findViewById(R.id.edtOrganiser)
        spType = findViewById(R.id.spType)
        spStatus = findViewById(R.id.spStatus)
        val tvFormTitle = findViewById<TextView>(R.id.tvFormTitle)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)

        // Có event_id => chế độ sửa
        eventId = intent.getLongExtra("event_id", -1L)
        if (eventId != -1L) {
            tvFormTitle.text = "Edit event"
            btnDelete.visibility = View.VISIBLE
            db.getById(eventId)?.let { e ->
                edtName.setText(e.name)
                edtDate.setText(e.date)
                edtLocation.setText(e.location)
                edtOrganiser.setText(e.organiser)
                spType.setSelection(types.indexOf(e.type).coerceAtLeast(0))
                spStatus.setSelection(statuses.indexOf(e.status).coerceAtLeast(0))
            }
        }

        edtDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        runCatching { dateFormat.parse(edtDate.text.toString()) }.getOrNull()?.let { cal.time = it }

        DatePickerDialog(
            this,
            { _, y, m, d ->
                edtDate.setText(String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y))
                tilDate.error = null
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun isValidDate(s: String): Boolean =
        datePattern.matches(s) && runCatching { dateFormat.parse(s) }.isSuccess

    private fun save() {
        val name = edtName.text.toString().trim()
        val date = edtDate.text.toString().trim()
        val location = edtLocation.text.toString().trim()
        val organiser = edtOrganiser.text.toString().trim()

        tilName.error = if (name.isEmpty()) "Event name is required" else null
        tilDate.error = when {
            date.isEmpty() -> "Event date is required"
            !isValidDate(date) -> "Invalid date, use dd/MM/yyyy"
            else -> null
        }
        tilLocation.error = if (location.isEmpty()) "Event location is required" else null
        tilOrganiser.error = if (organiser.isEmpty()) "Event organiser is required" else null

        if (listOf(tilName, tilDate, tilLocation, tilOrganiser).any { it.error != null }) return

        val event = Event(
            id = if (eventId == -1L) 0 else eventId,
            name = name,
            date = date,
            location = location,
            organiser = organiser,
            type = spType.selectedItem as String,
            status = spStatus.selectedItem as String
        )

        if (eventId == -1L) {
            db.insert(event)
            Toast.makeText(this, "Event added", Toast.LENGTH_SHORT).show()
        } else {
            db.update(event)
            Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ ->
                db.delete(eventId)
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}