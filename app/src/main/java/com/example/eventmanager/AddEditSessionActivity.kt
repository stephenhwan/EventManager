package com.example.eventmanager

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class AddEditSessionActivity : AppCompatActivity() {

    private val types =
        listOf("Workshop", "Presentation", "Panel Discussion", "Networking", "Keynote", "Others")

    private lateinit var db: EventDbHelper
    private var eventId = -1L
    private var sessionId = -1L

    private lateinit var tilName: TextInputLayout
    private lateinit var tilTime: TextInputLayout
    private lateinit var edtName: TextInputEditText
    private lateinit var edtTime: TextInputEditText
    private lateinit var edtSpeaker: TextInputEditText
    private lateinit var edtRoom: TextInputEditText
    private lateinit var edtDescription: TextInputEditText
    private lateinit var spType: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_session)

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.title_add_session)

        db = EventDbHelper(this)
        eventId = intent.getLongExtra("event_id", -1L)
        sessionId = intent.getLongExtra("session_id", -1L)

        tilName = findViewById(R.id.tilSessionName)
        tilTime = findViewById(R.id.tilSessionTime)
        edtName = findViewById(R.id.edtSessionName)
        edtTime = findViewById(R.id.edtSessionTime)
        edtSpeaker = findViewById(R.id.edtSpeaker)
        edtRoom = findViewById(R.id.edtRoom)
        edtDescription = findViewById(R.id.edtSessionDescription)
        spType = findViewById(R.id.spSessionType)
        val tvId = findViewById<TextView>(R.id.tvSessionId)
        val btnSave = findViewById<Button>(R.id.btnSaveSession)
        val btnDelete = findViewById<Button>(R.id.btnDeleteSession)

        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        tvId.text = getString(R.string.session_id_auto)

        // Có session_id => chế độ sửa
        if (sessionId != -1L) {
            val s = db.getSessionById(sessionId)
            if (s == null) {
                finish()
                return
            }
            eventId = s.eventId
            supportActionBar?.setTitle(R.string.title_edit_session)
            tvId.text = getString(R.string.session_id_value, s.id)
            btnDelete.visibility = View.VISIBLE
            edtName.setText(s.name)
            edtTime.setText(s.time)
            edtSpeaker.setText(s.speaker)
            edtRoom.setText(s.room)
            edtDescription.setText(s.description)
            spType.setSelection(types.indexOf(s.type).coerceAtLeast(0))
        }

        // Không có event để gắn session vào thì không thể lưu
        if (eventId == -1L) {
            finish()
            return
        }

        edtName.doAfterTextChanged { tilName.error = null }
        edtTime.doAfterTextChanged { tilTime.error = null }
        edtTime.setOnClickListener { showTimePicker() }
        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { confirmDelete() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showTimePicker() {
        var hour = 9
        var minute = 0
        Regex("""(\d{2}):(\d{2})""").matchEntire(edtTime.text.toString())?.let {
            hour = it.groupValues[1].toInt()
            minute = it.groupValues[2].toInt()
        }
        TimePickerDialog(
            this,
            { _, h, m -> edtTime.setText(String.format(Locale.US, "%02d:%02d", h, m)) },
            hour, minute, true // true = định dạng 24 giờ
        ).show()
    }

    private fun save() {
        val name = edtName.text.toString().trim()
        val time = edtTime.text.toString().trim()

        tilName.error = if (name.isEmpty()) getString(R.string.error_session_name_required) else null
        tilTime.error = if (time.isEmpty()) getString(R.string.error_session_time_required) else null
        if (tilName.error != null || tilTime.error != null) return

        val session = Session(
            id = if (sessionId == -1L) 0 else sessionId,
            eventId = eventId,
            name = name,
            time = time,
            speaker = edtSpeaker.text.toString().trim(),
            room = edtRoom.text.toString().trim(),
            type = spType.selectedItem as String,
            description = edtDescription.text.toString().trim()
        )

        if (sessionId == -1L) {
            db.insertSession(session)
            Toast.makeText(this, R.string.toast_session_added, Toast.LENGTH_SHORT).show()
        } else {
            db.updateSession(session)
            Toast.makeText(this, R.string.toast_session_updated, Toast.LENGTH_SHORT).show()
        }
        CloudSync.sync(this)
        finish()
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_session_title)
            .setMessage(R.string.delete_session_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                db.deleteSession(sessionId)
                Toast.makeText(this, R.string.toast_session_deleted, Toast.LENGTH_SHORT).show()
                CloudSync.sync(this)
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}