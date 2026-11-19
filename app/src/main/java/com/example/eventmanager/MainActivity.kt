package com.example.eventmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var db: EventDbHelper
    private lateinit var adapter: EventAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
}