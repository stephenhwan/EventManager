package com.example.eventmanager

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class EventDetailActivity : AppCompatActivity() {

    private lateinit var db: EventDbHelper
    private lateinit var adapter: SessionAdapter
    private var eventId = -1L
    private var currentQuery = ""

    private lateinit var tvName: TextView
    private lateinit var tvBody: TextView
    private lateinit var tvHeader: TextView
    private lateinit var tvNoSessions: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.title_event_detail)

        eventId = intent.getLongExtra("event_id", -1L)
        if (eventId == -1L) {
            finish()
            return
        }

        db = EventDbHelper(this)
        tvName = findViewById(R.id.tvDetailName)
        tvBody = findViewById(R.id.tvDetailBody)
        tvHeader = findViewById(R.id.tvSessionsHeader)
        tvNoSessions = findViewById(R.id.tvNoSessions)

        adapter = SessionAdapter(emptyList()) { session ->
            startActivity(
                Intent(this, AddEditSessionActivity::class.java)
                    .putExtra("event_id", eventId)
                    .putExtra("session_id", session.id)
            )
        }
        val rv = findViewById<RecyclerView>(R.id.rvSessions)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<View>(R.id.fabAddSession).setOnClickListener {
            startActivity(
                Intent(this, AddEditSessionActivity::class.java).putExtra("event_id", eventId)
            )
        }
    }

    // Tải lại mỗi lần màn hình hiện lại (sau khi sửa event hoặc thêm session)
    override fun onResume() {
        super.onResume()
        if (eventId == -1L) return

        // Event có thể vừa bị xoá ở màn hình sửa => không còn gì để hiện, quay về danh sách
        val event = db.getById(eventId)
        if (event == null) {
            finish()
            return
        }

        tvName.text = event.name
        tvBody.text = buildString {
            append("📅  ${event.date}\n")
            append("📍  ${event.location}\n")
            append("👤  ${event.organiser}\n")
            append("🏷  ${event.type}  ·  ${event.status}")
            if (event.contactEmail.isNotEmpty()) append("\n✉️  ${event.contactEmail}")
            if (event.contactPhone.isNotEmpty()) append("\n📞  ${event.contactPhone}")
            if (event.description.isNotEmpty()) append("\n\n${event.description}")
        }

        loadSessions()
    }
    private fun loadSessions() {
        val total = db.countSessions(eventId)
        val sessions = if (currentQuery.isEmpty()) db.getSessionsByEvent(eventId)
        else db.searchSessions(eventId, currentQuery)
        adapter.submit(sessions)
        tvHeader.text = if (currentQuery.isEmpty()) getString(R.string.sessions_header, total)
        else getString(R.string.sessions_found, sessions.size, total)
        tvNoSessions.text = getString(
            if (currentQuery.isEmpty()) R.string.no_sessions else R.string.no_search_results
        )
        tvNoSessions.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event_detail, menu)

        val searchItem = menu.findItem(R.id.action_search_sessions)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_sessions_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty().trim()
                loadSessions()
                return true
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                currentQuery = ""
                loadSessions()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_edit_event -> {
            startActivity(
                Intent(this, AddEditEventActivity::class.java).putExtra("event_id", eventId)
            )
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}