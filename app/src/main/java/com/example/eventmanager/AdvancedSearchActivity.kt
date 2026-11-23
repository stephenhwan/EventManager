package com.example.eventmanager

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdvancedSearchActivity : AppCompatActivity() {

    private lateinit var db: EventDbHelper
    private lateinit var adapter: EventAdapter
    private lateinit var tilOrganiser: TextInputLayout
    private lateinit var edtDate: TextInputEditText
    private lateinit var edtOrganiser: TextInputEditText
    private lateinit var tvInfo: TextView
    private lateinit var tvEmpty: TextView

    private var hasSearched = false
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_search)

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.title_advanced_search)

        db = EventDbHelper(this)
        tilOrganiser = findViewById(R.id.tilSearchOrganiser)
        edtDate = findViewById(R.id.edtSearchDate)
        edtOrganiser = findViewById(R.id.edtSearchOrganiser)
        tvInfo = findViewById(R.id.tvSearchInfo)
        tvEmpty = findViewById(R.id.tvSearchEmpty)

        // Bấm vào một kết quả => mở màn hình chi tiết đầy đủ của event
        adapter = EventAdapter(emptyList()) { event ->
            startActivity(
                Intent(this, EventDetailActivity::class.java).putExtra("event_id", event.id)
            )
        }
        val rv = findViewById<RecyclerView>(R.id.rvSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        edtDate.setOnClickListener { showDatePicker() }
        edtDate.doAfterTextChanged { tilOrganiser.error = null }
        edtOrganiser.doAfterTextChanged { tilOrganiser.error = null }
        edtOrganiser.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch()
                true
            } else false
        }
        findViewById<Button>(R.id.btnRunSearch).setOnClickListener { runSearch() }
        findViewById<Button>(R.id.btnClearSearch).setOnClickListener { clearSearch() }
    }

    // Quay lại từ màn hình chi tiết (event có thể đã sửa hoặc xoá) => chạy lại tìm kiếm để kết quả đúng
    override fun onResume() {
        super.onResume()
        if (hasSearched) runSearch()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        runCatching { dateFormat.parse(edtDate.text.toString()) }.getOrNull()?.let { cal.time = it }
        DatePickerDialog(
            this,
            { _, y, m, d -> edtDate.setText(String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y)) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun runSearch() {
        val date = edtDate.text.toString().trim()
        val organiser = edtOrganiser.text.toString().trim()

        // Phải có ít nhất một tiêu chí
        if (date.isEmpty() && organiser.isEmpty()) {
            tilOrganiser.error = getString(R.string.error_enter_criteria)
            return
        }

        hasSearched = true
        val results = db.advancedSearch(date, organiser)
        adapter.submit(results, db.getSessionCounts())
        tvInfo.text = getString(R.string.search_results_count, results.size)
        tvEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun clearSearch() {
        hasSearched = false
        edtDate.setText("")
        edtOrganiser.setText("")
        tilOrganiser.error = null
        adapter.submit(emptyList(), emptyMap())
        tvInfo.text = ""
        tvEmpty.visibility = View.GONE
    }
}