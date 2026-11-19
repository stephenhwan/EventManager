package com.example.eventmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.google.android.material.appbar.MaterialToolbar
//import android.widget.TextView
import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
//    private val datePattern = Regex("""\d{2}/\d{2}/\d{4}""")

    private lateinit var db: EventDbHelper
    private var eventId = -1L

    private lateinit var tilName: TextInputLayout
    private lateinit var tilDate: TextInputLayout
    private lateinit var tilLocation: TextInputLayout
    private lateinit var tilOrganiser: TextInputLayout
    private lateinit var edtName: TextInputEditText

    private lateinit var tilEmail: TextInputLayout

    private lateinit var tilPhone: TextInputLayout

    private lateinit var edtDescription: TextInputEditText

    private lateinit var edtEmail: TextInputEditText

    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtDate: TextInputEditText
    private lateinit var edtLocation: TextInputEditText
    private lateinit var edtOrganiser: TextInputEditText
    private lateinit var spType: Spinner
    private lateinit var spStatus: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_event)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.title_add_event)

        db = EventDbHelper(this)

        tilName = findViewById(R.id.tilName)
        tilDate = findViewById(R.id.tilDate)
        tilLocation = findViewById(R.id.tilLocation)
        tilOrganiser = findViewById(R.id.tilOrganiser)
        edtName = findViewById(R.id.edtName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPhone = findViewById(R.id.tilPhone)
        edtDescription = findViewById(R.id.edtDescription)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        edtDate = findViewById(R.id.edtDate)
        edtLocation = findViewById(R.id.edtLocation)
        edtOrganiser = findViewById(R.id.edtOrganiser)
        spType = findViewById(R.id.spType)
        spStatus = findViewById(R.id.spStatus)
//        val tvFormTitle = findViewById<TextView>(R.id.tvFormTitle)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)

        // Có event_id => chế độ sửa
        eventId = intent.getLongExtra("event_id", -1L)
        if (eventId != -1L) {

            supportActionBar?.setTitle(R.string.title_edit_event)
            btnDelete.visibility = View.VISIBLE
            db.getById(eventId)?.let { e ->
                edtName.setText(e.name)
                edtDescription.setText(e.description)
                edtEmail.setText(e.contactEmail)
                edtPhone.setText(e.contactPhone)
                edtDate.setText(e.date)
                edtLocation.setText(e.location)
                edtOrganiser.setText(e.organiser)
                spType.setSelection(types.indexOf(e.type).coerceAtLeast(0))
                spStatus.setSelection(statuses.indexOf(e.status).coerceAtLeast(0))
            }
            setupClearErrorOnTyping()
        }

        edtDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { confirmDelete() }
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    private fun setupClearErrorOnTyping() {
        edtName.doAfterTextChanged { tilName.error = null }
        edtLocation.doAfterTextChanged { tilLocation.error = null }
        edtOrganiser.doAfterTextChanged { tilOrganiser.error = null }
        edtEmail.doAfterTextChanged { tilEmail.error = null }
        edtPhone.doAfterTextChanged { tilPhone.error = null }
        edtDate.doAfterTextChanged {
            tilDate.error = null
            // Đổi ngày có thể giải quyết lỗi "trùng tên + ngày" đang hiện ở ô tên
            if (tilName.error?.toString() == getString(R.string.error_event_duplicate)) {
                tilName.error = null
            }
        }
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

//    private fun isValidDate(s: String): Boolean =
//        datePattern.matches(s) && runCatching { dateFormat.parse(s) }.isSuccess

    private fun save() {
        val name = edtName.text.toString().trim()
        val date = edtDate.text.toString().trim()
        val location = edtLocation.text.toString().trim()
        val organiser = edtOrganiser.text.toString().trim()
        val description = edtDescription.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val phone = edtPhone.text.toString().trim()

        // 1) Trường bắt buộc (required)
        tilName.error = if (name.isEmpty()) getString(R.string.error_name_required) else null
        tilDate.error = when {
            date.isEmpty() -> getString(R.string.error_date_required)
            !Validators.isValidDate(date) -> getString(R.string.error_date_invalid)
            else -> null
        }
        tilLocation.error = if (location.isEmpty()) getString(R.string.error_location_required) else null
        tilOrganiser.error = if (organiser.isEmpty()) getString(R.string.error_organiser_required) else null

        // 2) Trường tuỳ chọn (optional): chỉ kiểm tra định dạng khi người dùng có nhập
        tilEmail.error =
            if (email.isNotEmpty() && !Validators.isValidEmail(email)) getString(R.string.error_email_invalid) else null
        tilPhone.error =
            if (phone.isNotEmpty() && !Validators.isValidPhone(phone)) getString(R.string.error_phone_invalid) else null

        if (listOf(tilName, tilDate, tilLocation, tilOrganiser, tilEmail, tilPhone).any { it.error != null }) return

        // 3) Không cho trùng tên + ngày (bỏ qua chính sự kiện đang sửa)
        if (db.existsByNameAndDate(name, date, eventId)) {
            tilName.error = getString(R.string.error_event_duplicate)
            return
        }

        val event = Event(
            id = if (eventId == -1L) 0 else eventId,
            name = name,
            date = date,
            location = location,
            organiser = organiser,
            type = spType.selectedItem as String,
            status = spStatus.selectedItem as String,
            description = description,
            contactEmail = email,
            contactPhone = phone
        )

        // 4) Hiển thị lại thông tin để người dùng xác nhận (hoặc quay lại sửa)
        showConfirmation(event)
    }
    private fun showConfirmation(event: Event) {
        val blank = getString(R.string.value_not_provided)
        val message = getString(
            R.string.confirm_message,
            event.name, event.date, event.location, event.organiser,
            event.type, event.status,
            event.description.ifEmpty { blank },
            event.contactEmail.ifEmpty { blank },
            event.contactPhone.ifEmpty { blank }
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_title)
            .setMessage(message)
            .setPositiveButton(R.string.confirm) { _, _ -> persist(event) }
            .setNegativeButton(R.string.edit, null) // đóng hộp thoại => quay lại form để sửa
            .show()
    }

    private fun persist(event: Event) {
        if (eventId == -1L) {
            db.insert(event)
            Toast.makeText(this, R.string.toast_event_added, Toast.LENGTH_SHORT).show()
        } else {
            db.update(event)
            Toast.makeText(this, R.string.toast_event_updated, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                db.delete(eventId)
                Toast.makeText(this, R.string.toast_event_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}