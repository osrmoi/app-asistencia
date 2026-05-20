package com.example.attendyapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Estado de asistencia de un alumno en la lista
enum class EstadoAsistencia {
    SIN_ESTADO, PRESENTE, AUSENTE, RETARDO, JUSTIFICADO
}

// Modelo que representa una fila en la lista de asistencia
data class AlumnoAsistencia(
    val idAlumno: Int,
    val matricula: String,
    val nombreCompleto: String,
    var estado: EstadoAsistencia = EstadoAsistencia.SIN_ESTADO
)

class AsistenciaAdapter(
    private val alumnos: List<AlumnoAsistencia>,
    private val onEstadoCambiado: (AlumnoAsistencia) -> Unit
) : RecyclerView.Adapter<AsistenciaAdapter.AsistenciaViewHolder>() {

    // Colores por estado
    private val colorSinEstado   = Color.parseColor("#BDBDBD")
    private val colorPresente    = Color.parseColor("#2E7D32")
    private val colorAusente     = Color.parseColor("#C62828")
    private val colorRetardo     = Color.parseColor("#E65100")
    private val colorJustificado = Color.parseColor("#1565C0")
    private val colorInactivo    = Color.parseColor("#EEEEEE")
    private val colorTextoInactivo = Color.parseColor("#BDBDBD")

    inner class AsistenciaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEstadoCirculo: TextView  = itemView.findViewById(R.id.tvEstadoCirculo)
        val tvNombre: TextView         = itemView.findViewById(R.id.tvNombreAlumnoLista)
        val tvMatricula: TextView      = itemView.findViewById(R.id.tvMatriculaAlumnoLista)
        val btnPresente: TextView      = itemView.findViewById(R.id.btnPresente)
        val btnAusente: TextView       = itemView.findViewById(R.id.btnAusente)
        val btnRetardo: TextView       = itemView.findViewById(R.id.btnRetardo)
        val btnJustificado: TextView   = itemView.findViewById(R.id.btnJustificado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsistenciaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asistencia_alumno, parent, false)
        return AsistenciaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AsistenciaViewHolder, position: Int) {
        val alumno = alumnos[position]

        holder.tvNombre.text   = alumno.nombreCompleto
        holder.tvMatricula.text = alumno.matricula

        // Actualiza el círculo grande de estado y los 4 botones
        actualizarVisuales(holder, alumno.estado)

        // Clic en cada botón — cambia el estado y notifica
        holder.btnPresente.setOnClickListener {
            alumno.estado = EstadoAsistencia.PRESENTE
            actualizarVisuales(holder, alumno.estado)
            onEstadoCambiado(alumno)
        }
        holder.btnAusente.setOnClickListener {
            alumno.estado = EstadoAsistencia.AUSENTE
            actualizarVisuales(holder, alumno.estado)
            onEstadoCambiado(alumno)
        }
        holder.btnRetardo.setOnClickListener {
            alumno.estado = EstadoAsistencia.RETARDO
            actualizarVisuales(holder, alumno.estado)
            onEstadoCambiado(alumno)
        }
        holder.btnJustificado.setOnClickListener {
            alumno.estado = EstadoAsistencia.JUSTIFICADO
            actualizarVisuales(holder, alumno.estado)
            onEstadoCambiado(alumno)
        }
    }

    override fun getItemCount(): Int = alumnos.size

    // Actualiza el círculo grande y los 4 botones según el estado actual
    private fun actualizarVisuales(holder: AsistenciaViewHolder, estado: EstadoAsistencia) {

        // Círculo grande (izquierda)
        val (letraCirculo, colorCirculo) = when (estado) {
            EstadoAsistencia.SIN_ESTADO   -> "-" to colorSinEstado
            EstadoAsistencia.PRESENTE     -> "P" to colorPresente
            EstadoAsistencia.AUSENTE      -> "A" to colorAusente
            EstadoAsistencia.RETARDO      -> "R" to colorRetardo
            EstadoAsistencia.JUSTIFICADO  -> "J" to colorJustificado
        }
        holder.tvEstadoCirculo.text = letraCirculo
        holder.tvEstadoCirculo.backgroundTintList = ColorStateList.valueOf(colorCirculo)

        // Botones de estado: activo = color propio, inactivo = gris
        pintarBoton(holder.btnPresente,    estado == EstadoAsistencia.PRESENTE,    colorPresente)
        pintarBoton(holder.btnAusente,     estado == EstadoAsistencia.AUSENTE,     colorAusente)
        pintarBoton(holder.btnRetardo,     estado == EstadoAsistencia.RETARDO,     colorRetardo)
        pintarBoton(holder.btnJustificado, estado == EstadoAsistencia.JUSTIFICADO, colorJustificado)
    }

    private fun pintarBoton(boton: TextView, activo: Boolean, colorActivo: Int) {
        if (activo) {
            boton.backgroundTintList = ColorStateList.valueOf(colorActivo)
            boton.setTextColor(Color.WHITE)
        } else {
            boton.backgroundTintList = ColorStateList.valueOf(colorInactivo)
            boton.setTextColor(colorTextoInactivo)
        }
    }
}