package com.example.attendyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Modelo de datos
data class Grupo(
    val id: Int,
    val nombre: String,
    val descripcion: String?
)
class GruposAdapter (
    private val grupos: List<Grupo>,
    private val onItemClick: (Grupo) -> Unit
) : RecyclerView.Adapter<GruposAdapter.GrupoViewHolder>(){

    inner class GrupoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreGrupo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionGrupo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrupoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grupo, parent, false)
        return GrupoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GrupoViewHolder, position: Int) {
        val grupo = grupos[position]

        holder.tvNombre.text = grupo.nombre

        if (grupo.descripcion.isNullOrBlank()) {
            holder.tvDescripcion.visibility = View.GONE
        } else {
            holder.tvDescripcion.visibility = View.VISIBLE
            holder.tvDescripcion.text = grupo.descripcion
        }

        holder.itemView.setOnClickListener {
            onItemClick(grupo)
        }
    }

    override fun getItemCount(): Int = grupos.size


}