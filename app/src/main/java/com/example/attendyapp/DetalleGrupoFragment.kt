package com.example.attendyapp

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DetalleGrupoFragment : Fragment() {

    // ── Argumentos que recibe desde GruposFragment ───────────────────────────
    private var grupoId: Int = -1
    private var grupoNombre: String = ""

    companion object {
        private const val ARG_ID = "grupo_id"
        private const val ARG_NOMBRE = "grupo_nombre"

        fun newInstance(id: Int, nombre: String): DetalleGrupoFragment {
            val fragment = DetalleGrupoFragment()
            val args = Bundle()
            args.putInt(ARG_ID, id)
            args.putString(ARG_NOMBRE, nombre)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerView: RecyclerView
    private var listaAlumnos = mutableListOf<Alumno>()
    private lateinit var alumnosAdapter: AlumnosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        grupoId = arguments?.getInt(ARG_ID) ?: -1
        grupoNombre = arguments?.getString(ARG_NOMBRE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_detalle_grupo, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        view.findViewById<TextView>(R.id.tvNombreGrupoDetalle).text = grupoNombre
        view.findViewById<ImageButton>(R.id.btnRegresar).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<ImageButton>(R.id.btnEditarGrupo).setOnClickListener {
            val grupo = dbHelper.obtenerGrupoPorId(grupoId)
            mostrarDialogoEditarGrupo(grupo)
        }

        view.findViewById<Button>(R.id.btnAgregarAlumno).setOnClickListener {
            mostrarDialogoAgregarAlumno()
        }

        view.findViewById<ImageButton>(R.id.btnExportarGrupo).setOnClickListener {
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

        recyclerView = view.findViewById(R.id.recyclerViewAlumnos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        alumnosAdapter = AlumnosAdapter(listaAlumnos) { alumnoClickeado ->
            mostrarDialogoEditarAlumno(alumnoClickeado)
        }
        recyclerView.adapter = alumnosAdapter

        cargarAlumnos()

        // Swipe to delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val alumno = listaAlumnos[position]

                DialogUtils.mostrarConfirmacion(
                    context   = requireContext(),
                    titulo    = "Quitar alumno",
                    mensaje   = "¿Quitar a \"${alumno.nombreCompleto}\" de este grupo?",
                    onAceptar = {
                        dbHelper.darDeBajaAlumnoDeGrupo(grupoId, alumno.id)
                        cargarAlumnos()
                    }
                )
                alumnosAdapter.notifyItemChanged(position)
            }

            override fun onChildDraw(
                c: android.graphics.Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = android.graphics.Paint()

                paint.color = Color.parseColor("#F44336")
                val fondoRojo = android.graphics.RectF(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRect(fondoRojo, paint)

                paint.color = android.graphics.Color.WHITE
                paint.textSize = 42f
                paint.isFakeBoldText = true
                paint.textAlign = android.graphics.Paint.Align.CENTER
                val centroY = (itemView.top + itemView.bottom) / 2f - (paint.descent() + paint.ascent()) / 2f
                c.drawText("Quitar", itemView.right - 100f, centroY, paint)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun exportar() {
        val grupo = dbHelper.obtenerGrupoPorId(grupoId) ?: return
        val uri = ExportadorCSV.exportarAsistenciaGrupo(requireContext(), grupo, dbHelper)
        if (uri != null) {
            Toast.makeText(requireContext(), "Guardado en Descargas ✅", Toast.LENGTH_LONG).show()
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

    // Lee los alumnos del grupo desde la BD
    private fun cargarAlumnos() {
        listaAlumnos.clear()

        val cursor = dbHelper.obtenerAlumnosPorGrupo(grupoId)
        while (cursor.moveToNext()) {
            val idAlumno       = cursor.getInt(cursor.getColumnIndexOrThrow("idAlumno"))
            val matricula      = cursor.getString(cursor.getColumnIndexOrThrow("matricula"))
            val nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow("nombreCompleto"))
            listaAlumnos.add(Alumno(idAlumno, matricula, nombreCompleto))
        }
        cursor.close()

        alumnosAdapter.notifyDataSetChanged()
    }

    // Dialog de edición
    private fun mostrarDialogoEditarGrupo(grupo: Grupo?) {
        if (grupo == null) return

        val vistaDialogo  = layoutInflater.inflate(R.layout.dialog_grupo, null)
        val etNombre      = vistaDialogo.findViewById<android.widget.EditText>(R.id.etNombre)
        val etDescripcion = vistaDialogo.findViewById<android.widget.EditText>(R.id.etDescripcion)

        // Cambiamos el título y el texto del botón del XML reutilizado
        vistaDialogo.findViewById<android.widget.TextView>(R.id.tvTituloDialogo).text = "Editar Grupo"
        val btnGuardar = vistaDialogo.findViewById<android.widget.Button>(R.id.btnGuardar).apply {
            text = "Guardar"
        }

        etNombre.setText(grupo.nombre)
        etDescripcion.setText(grupo.descripcion)

        android.app.AlertDialog.Builder(requireContext())
            .setView(vistaDialogo)
            .create()
            .apply {
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

                vistaDialogo.findViewById<android.widget.Button>(R.id.btnCancelar).setOnClickListener {
                    dismiss()
                }
                btnGuardar.setOnClickListener {
                    val nombre      = etNombre.text.toString().trim()
                    val descripcion = etDescripcion.text.toString().trim().ifEmpty { null }

                    if (nombre.isEmpty()) {
                        etNombre.error = "El nombre es obligatorio"
                        return@setOnClickListener
                    }

                    val exito = dbHelper.actualizarGrupo(grupo.id, nombre, descripcion)
                    if (exito) {
                        grupoNombre = nombre
                        view?.findViewById<TextView>(R.id.tvNombreGrupoDetalle)?.text = nombre
                    } else {
                        Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }

                    dismiss()
                }
            }
            .show()
    }

    private fun mostrarDialogoEditarAlumno(alumno: Alumno) {
        val vistaDialogo = layoutInflater.inflate(R.layout.dialog_alumno, null)
        val etNombre     = vistaDialogo.findViewById<android.widget.EditText>(R.id.etNombreAlumno)
        val etMatricula  = vistaDialogo.findViewById<android.widget.EditText>(R.id.etMatriculaAlumno)

        // Cambiamos el título y el texto del botón del XML reutilizado
        vistaDialogo.findViewById<android.widget.TextView>(R.id.tvTituloDialogoAlumno).text = "Editar Alumno"
        val btnGuardar = vistaDialogo.findViewById<android.widget.Button>(R.id.btnAgregarAlumno).apply {
            text = "Guardar"
        }

        etNombre.setText(alumno.nombreCompleto)
        etMatricula.setText(alumno.matricula)

        android.app.AlertDialog.Builder(requireContext())
            .setView(vistaDialogo)
            .create()
            .apply {
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

                vistaDialogo.findViewById<android.widget.Button>(R.id.btnCancelarAlumno).setOnClickListener {
                    dismiss()
                }

                btnGuardar.setOnClickListener {
                    val nombre    = etNombre.text.toString().trim()
                    val matricula = etMatricula.text.toString().trim()

                    if (nombre.isEmpty()) {
                        etNombre.error = "El nombre es obligatorio"
                        return@setOnClickListener
                    }
                    if (matricula.isEmpty()) {
                        etMatricula.error = "La matrícula es obligatoria"
                        return@setOnClickListener
                    }

                    val exito = dbHelper.actualizarAlumno(alumno.id, matricula, nombre)
                    if (!exito) {
                        Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }

                    cargarAlumnos()
                    dismiss()
                }
            }
            .show()
    }

    private fun mostrarDialogoAgregarAlumno() {
        val vistaDialogo = layoutInflater.inflate(R.layout.dialog_alumno, null)
        val etNombre     = vistaDialogo.findViewById<android.widget.EditText>(R.id.etNombreAlumno)
        val etMatricula  = vistaDialogo.findViewById<android.widget.EditText>(R.id.etMatriculaAlumno)

        // Ajustar el texto del título en el XML (Útil si reutilizas el XML para editar)
        vistaDialogo.findViewById<TextView>(R.id.tvTituloDialogoAlumno).text = "Agregar Alumno"

        android.app.AlertDialog.Builder(requireContext())
            .setView(vistaDialogo)
            .create()
            .apply {
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

                vistaDialogo.findViewById<Button>(R.id.btnCancelarAlumno).setOnClickListener {
                    dismiss()
                }

                vistaDialogo.findViewById<Button>(R.id.btnAgregarAlumno).setOnClickListener {
                    val nombre    = etNombre.text.toString().trim()
                    val matricula = etMatricula.text.toString().trim()

                    if (nombre.isEmpty()) {
                        etNombre.error = "El nombre es obligatorio"
                        return@setOnClickListener
                    }
                    if (matricula.isEmpty()) {
                        etMatricula.error = "La matrícula es obligatoria"
                        return@setOnClickListener
                    }

                    val insertado = dbHelper.insertarAlumno(matricula, nombre)

                    if (insertado) {
                        val alumno = dbHelper.obtenerAlumnoPorMatricula(matricula)
                        if (alumno != null) {
                            dbHelper.inscribirAlumnoEnGrupo(grupoId, alumno.id)
                        }
                    } else {
                        val alumno = dbHelper.obtenerAlumnoPorMatricula(matricula)
                        if (alumno != null) {
                            val inscrito = dbHelper.inscribirAlumnoEnGrupo(grupoId, alumno.id)
                            if (!inscrito) {
                                Toast.makeText(
                                    requireContext(),
                                    "Este alumno ya está en el grupo",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }
                        } else {
                            Toast.makeText(
                                requireContext(), "Error al agregar alumno", Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }
                    }

                    cargarAlumnos()
                    dismiss()
                }
            }
            .show()
    }
}