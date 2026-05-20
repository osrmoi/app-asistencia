package com.example.attendyapp

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TomarListaFragment : Fragment() {

    private var grupoId: Int = -1
    private var grupoNombre: String = ""

    private val calendar = Calendar.getInstance()

    private lateinit var dbHelper: DBHelper
    private lateinit var asistenciaAdapter: AsistenciaAdapter
    private var listaAlumnos = mutableListOf<AlumnoAsistencia>()

    companion object {
        private const val ARG_ID     = "grupo_id"
        private const val ARG_NOMBRE = "grupo_nombre"

        fun newInstance(id: Int, nombre: String): TomarListaFragment {
            val fragment = TomarListaFragment()
            val args = Bundle()
            args.putInt(ARG_ID, id)
            args.putString(ARG_NOMBRE, nombre)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        grupoId     = arguments?.getInt(ARG_ID) ?: -1
        grupoNombre = arguments?.getString(ARG_NOMBRE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tomar_lista, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        view.findViewById<TextView>(R.id.tvNombreGrupoLista).text = grupoNombre

        view.findViewById<ImageButton>(R.id.btnRegresarLista).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val tvFecha = view.findViewById<TextView>(R.id.tvFechaSeleccionada)
        tvFecha.text = formatearFecha(calendar)

        view.findViewById<ImageButton>(R.id.btnExportarLista).setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                requireContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    101
                )
            } else {
                exportar()
            }
        }

        view.findViewById<TextView>(R.id.tvComoTomoLista).setOnClickListener {
            val vistaDialog = layoutInflater.inflate(R.layout.dialog_instrucciones, null)
            android.app.AlertDialog.Builder(requireContext())
                .setView(vistaDialog)
                .create()
                .apply {
                    window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                    vistaDialog.findViewById<Button>(R.id.btnEntendido).setOnClickListener {
                        dismiss()
                    }
                }
                .show()
        }

        view.findViewById<LinearLayout>(R.id.cardFecha).setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, anio, mes, dia ->
                    calendar.set(anio, mes, dia)
                    tvFecha.text = formatearFecha(calendar)
                    // Al cambiar la fecha recargamos para mostrar asistencia
                    // guardada previamente ese día (si existe)
                    cargarAlumnos()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        view.findViewById<Button>(R.id.btnMarcarTodosPresentes).setOnClickListener {
            listaAlumnos.forEach { it.estado = EstadoAsistencia.PRESENTE }
            asistenciaAdapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "Todos marcados como presentes", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnGuardarAsistencia).setOnClickListener {
            guardarAsistencia()
            Toast.makeText(requireContext(), "Asistencia guardada", Toast.LENGTH_SHORT).show()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAsistencia)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        asistenciaAdapter = AsistenciaAdapter(listaAlumnos) { alumnoModificado ->
            // Solo actualizamos la UI
        }
        recyclerView.adapter = asistenciaAdapter

        cargarAlumnos()
    }

    private fun exportar() {
        val grupo = dbHelper.obtenerGrupoPorId(grupoId) ?: return
        val uri = ExportadorCSV.exportarAsistenciaGrupo(requireContext(), grupo, dbHelper)
        if (uri != null) {
            Toast.makeText(requireContext(), "Guardado en Descargas", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "Error al exportar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            exportar()
        } else {
            Toast.makeText(requireContext(), "Permiso necesario para exportar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarAlumnos() {
        listaAlumnos.clear()

        val cursor = dbHelper.obtenerAlumnosPorGrupo(grupoId)
        while (cursor.moveToNext()) {
            val idAlumno       = cursor.getInt(cursor.getColumnIndexOrThrow("idAlumno"))
            val matricula      = cursor.getString(cursor.getColumnIndexOrThrow("matricula"))
            val nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow("nombreCompleto"))
            listaAlumnos.add(AlumnoAsistencia(idAlumno, matricula, nombreCompleto))
        }
        cursor.close()

        val fechaTimestamp = obtenerTimestampDelDia(calendar)
        val cursorAsistencia = dbHelper.obtenerAsistenciaPorGrupoYFecha(grupoId, fechaTimestamp)
        while (cursorAsistencia.moveToNext()) {
            val matricula = cursorAsistencia.getString(cursorAsistencia.getColumnIndexOrThrow("matricula"))
            val estadoStr = cursorAsistencia.getString(cursorAsistencia.getColumnIndexOrThrow("estado"))
            listaAlumnos.find { it.matricula == matricula }?.estado = estadoDesdeString(estadoStr)
        }
        cursorAsistencia.close()

        asistenciaAdapter.notifyDataSetChanged()
    }

    private fun guardarAsistencia() {
        val fechaTimestamp = obtenerTimestampDelDia(calendar)

        val mapaAsistencia = listaAlumnos
            .filter { it.estado != EstadoAsistencia.SIN_ESTADO }
            .associate { it.idAlumno to estadoAString(it.estado) }

        if (mapaAsistencia.isNotEmpty()) {
            dbHelper.registrarAsistenciaMasiva(grupoId, fechaTimestamp, mapaAsistencia)
        }
    }

    private fun obtenerTimestampDelDia(cal: Calendar): Long {
        val copia = cal.clone() as Calendar
        copia.set(Calendar.HOUR_OF_DAY, 0)
        copia.set(Calendar.MINUTE, 0)
        copia.set(Calendar.SECOND, 0)
        copia.set(Calendar.MILLISECOND, 0)
        return copia.timeInMillis
    }

    private fun formatearFecha(cal: Calendar): String {
        val formato = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "MX"))
        return formato.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    private fun estadoAString(estado: EstadoAsistencia): String = when (estado) {
        EstadoAsistencia.PRESENTE     -> "PRESENTE"
        EstadoAsistencia.AUSENTE      -> "AUSENTE"
        EstadoAsistencia.RETARDO      -> "RETARDO"
        EstadoAsistencia.JUSTIFICADO  -> "JUSTIFICADO"
        EstadoAsistencia.SIN_ESTADO   -> ""
    }

    private fun estadoDesdeString(str: String): EstadoAsistencia = when (str) {
        "PRESENTE"    -> EstadoAsistencia.PRESENTE
        "AUSENTE"     -> EstadoAsistencia.AUSENTE
        "RETARDO"     -> EstadoAsistencia.RETARDO
        "JUSTIFICADO" -> EstadoAsistencia.JUSTIFICADO
        else          -> EstadoAsistencia.SIN_ESTADO
    }
}