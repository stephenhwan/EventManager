package com.example.eventmanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat

class EventAdapter(
    private var items: List<Event>,
    private var sessionCounts: Map<Long, Int> = emptyMap(),   // eventId -> số session
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvEventName)
        val tvDate: TextView = v.findViewById(R.id.tvEventDate)
        val tvLocation: TextView = v.findViewById(R.id.tvEventLocation)
        val tvOrganiser: TextView = v.findViewById(R.id.tvEventOrganiser)
        val tvSessions: TextView = v.findViewById(R.id.tvEventSessions)
        val tvType: TextView = v.findViewById(R.id.tvEventType)
        val tvStatus: TextView = v.findViewById(R.id.tvEventStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.tvName.text = e.name
        holder.tvDate.text = "📅  ${e.date}"
        holder.tvLocation.text = "📍  ${e.location}"
        holder.tvOrganiser.text = "👤  ${e.organiser}"
        holder.tvSessions.text = holder.itemView.context.getString(
            R.string.sessions_count, sessionCounts[e.id] ?: 0
        )
        holder.tvType.text = e.type
        holder.tvStatus.text = e.status
        val colorRes = when (e.status) {
            "Upcoming" -> R.color.status_upcoming
            "Completed" -> R.color.status_completed
            else -> R.color.status_cancelled
        }
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, colorRes))
        holder.itemView.setOnClickListener { onClick(e) }
    }

    fun submit(list: List<Event>, counts: Map<Long, Int>) {
        items = list
        sessionCounts = counts
        notifyDataSetChanged()
    }
}