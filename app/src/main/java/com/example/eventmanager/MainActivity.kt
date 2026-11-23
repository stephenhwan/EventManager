package com.example.eventmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var db: EventDbHelper
    private lateinit var adapter: EventAdapter
    private lateinit var tvEmpty: TextView
    private var currentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setTitle(R.string.title_main)

        db = EventDbHelper(this)
        tvEmpty = findViewById(R.id.tvEmpty)

        // Bấm 1 event => mở màn hình chi tiết (xem thông tin + danh sách session của event)
        adapter = EventAdapter(emptyList()) { event ->
            startActivity(
                Intent(this, EventDetailActivity::class.java).putExtra("event_id", event.id)
            )
        }
        val rv = findViewById<RecyclerView>(R.id.rvEvents)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddEditEventActivity::class.java))
        }
    }

    // Quay lại màn hình này thì load lại dữ liệu từ database
    override fun onResume() {
        super.onResume()
        loadEvents()
        if (CloudSync.isDirty(this)) CloudSync.sync(this)   // MỚI
    }

    // Chưa gõ gì => hiện tất cả; có gõ => hiện kết quả tìm
    private fun loadEvents() {
        val list = if (currentQuery.isEmpty()) db.getAll() else db.searchEvents(currentQuery)
        adapter.submit(list, db.getSessionCounts())
        tvEmpty.text = getString(
            if (currentQuery.isEmpty()) R.string.empty_events else R.string.no_search_results
        )
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_events_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false // false: để SearchView tự ẩn bàn phím
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty().trim()
                loadEvents() // cập nhật danh sách ngay khi gõ từng chữ
                return true
            }
        })
        // Đóng ô tìm kiếm thì trả danh sách về đầy đủ
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                currentQuery = ""
                loadEvents()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_upload -> {
            uploadToCloud()
            true
        }
        R.id.action_advanced_search -> {
            startActivity(Intent(this, AdvancedSearchActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun uploadToCloud() {
        // 1) Kiểm tra mạng trước khi gửi
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_LONG).show()
            return
        }
        if (db.getAll().isEmpty()) {
            Toast.makeText(this, R.string.no_events_to_upload, Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, R.string.uploading, Toast.LENGTH_SHORT).show()

        // 2) Gửi toàn bộ event + session; kết quả trả về trên UI thread
        CloudSync.sync(this) { result ->
            val msg = result.fold(
                onSuccess = { getString(R.string.upload_success, it.first, it.second) },
                onFailure = { getString(R.string.upload_failed, it.message ?: "unknown error") }
            )
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        }
    }
}