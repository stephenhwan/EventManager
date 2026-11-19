package com.example.eventmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setTitle(R.string.title_main)

        db = EventDbHelper(this)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = EventAdapter(emptyList()) { event ->
            startActivity(
                Intent(this, AddEditEventActivity::class.java).putExtra("event_id", event.id)
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
        val list = db.getAll()
        adapter.submit(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_upload -> {
            uploadToCloud()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun uploadToCloud() {
        // 1) Kiểm tra mạng trước
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_LONG).show()
            return
        }
        val events = db.getAll()
        if (events.isEmpty()) {
            Toast.makeText(this, R.string.no_events_to_upload, Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, R.string.uploading, Toast.LENGTH_SHORT).show()

        // 2) Gửi ở luồng nền, xong thì quay về UI thread để báo kết quả
        Thread {
            val result = CloudSync.uploadAll(events)
            runOnUiThread {
                val msg = result.fold(
                    onSuccess = { getString(R.string.upload_success, events.size) },
                    onFailure = { getString(R.string.upload_failed, it.message ?: "unknown error") }
                )
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }.start()
    }
}