package com.example.attendyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Alumno(
    val id: Int,
    val matricula: String,
    val nombreCompleto: String
)

class AlumnosAdapter(
    private val alumnos: List<Alumno>,
    private val onItemClick: (Alumno) -> Unit
) : RecyclerView.Adapter<AlumnosAdapter.AlumnoViewHolder>() {

    inner class AlumnoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreAlumno)
        val tvMatricula: TextView = itemView.findViewById(R.id.tvMatriculaAlumno)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alumno, parent, false)
        return AlumnoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlumnoViewHolder, position: Int) {
        val alumno = alumnos[position]

        holder.tvNombre.text = alumno.nombreCompleto
        holder.tvMatricula.text = alumno.matricula

        holder.itemView.setOnClickListener {
            onItemClick(alumno)
        }
    }

    override fun getItemCount(): Int = alumnos.size
}