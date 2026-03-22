package com.vrumsync.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vrumsync.app.R
import com.vrumsync.app.model.Car
import com.vrumsync.app.ui.loadUrl

class ItemAdapter(
    private val items: List<Car>,
    private val onItemClick: (Car) -> Unit,
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image)
        val nameTextView: TextView = view.findViewById(R.id.name)
        val yearTextView: TextView = view.findViewById(R.id.year)
        val licenceTextView: TextView = view.findViewById(R.id.licence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val car = items[position]

        holder.nameTextView.text = car.name
        holder.yearTextView.text = "Ano: ${car.year}"
        holder.licenceTextView.text = "Placa: ${car.licence}"

        holder.imageView.loadUrl(car.imageUrl)

        holder.itemView.setOnClickListener {
            onItemClick(car)
        }
    }

    override fun getItemCount(): Int = items.size
}
