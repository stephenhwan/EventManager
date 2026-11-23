package com.example.eventmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SessionAdapter(
    private var items: List<Session>,
    private val onClick: (Session) -> Unit
) : RecyclerView.Adapter<SessionAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTime: TextView = v.findViewById(R.id.tvSessionTime)
        val tvName: TextView = v.findViewById(R.id.tvSessionName)
        val tvMeta: TextView = v.findViewById(R.id.tvSessionMeta)
        val tvType: TextView = v.findViewById(R.id.tvSessionType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.tvTime.text = s.time
        holder.tvName.text = s.name
        holder.tvType.text = s.type

        // Speaker và room là tuỳ chọn: chỉ hiện dòng này khi có ít nhất một giá trị
        val meta = listOf(s.speaker, s.room).filter { it.isNotEmpty() }.joinToString("  ·  ")
        holder.tvMeta.text = meta
        holder.tvMeta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onClick(s) }
    }

    fun submit(list: List<Session>) {
        items = list
        notifyDataSetChanged()
    }
}