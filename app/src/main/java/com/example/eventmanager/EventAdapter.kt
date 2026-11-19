package com.example.eventmanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private var items: List<Event>,
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvEventName)
        val tvDate: TextView = v.findViewById(R.id.tvEventDate)
        val tvLocation: TextView = v.findViewById(R.id.tvEventLocation)
        val tvOrganiser: TextView = v.findViewById(R.id.tvEventOrganiser)
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
        holder.tvType.text = e.type
        holder.tvStatus.text = e.status
        holder.tvStatus.setTextColor(
            Color.parseColor(
                when (e.status) {
                    "Upcoming" -> "#1976D2"
                    "Completed" -> "#388E3C"
                    else -> "#D32F2F"
                }
            )
        )
        holder.itemView.setOnClickListener { onClick(e) }
    }

    fun submit(list: List<Event>) {
        items = list
        notifyDataSetChanged()
    }
}