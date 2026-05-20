package com.example.attendyapp

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GruposFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerView: RecyclerView
    private var listaDeGrupos = mutableListOf<Grupo>()
    private lateinit var adapter: GruposAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_grupos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGrupos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = GruposAdapter(listaDeGrupos) { grupoClickeado ->
            val detalle = DetalleGrupoFragment.newInstance(grupoClickeado.id, grupoClickeado.nombre)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detalle)
                .addToBackStack(null)  // <-- esto permite regresar con el botón atrás
                .commit()
        }
        recyclerView.adapter = adapter

        val btnNuevo = view.findViewById<Button>(R.id.btnNuevoGrupo)
        btnNuevo.setOnClickListener {
            mostrarDialogoGrupo()
        }

        cargarGrupos()

        // Swipe to delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val grupo    = listaDeGrupos[position]

                DialogUtils.mostrarConfirmacion(
                    context  = requireContext(),
                    titulo   = "Eliminar grupo",
                    mensaje  = "¿Estás seguro que deseas eliminar \"${grupo.nombre}\"?",
                    onAceptar = {
                        dbHelper.eliminarGrupo(grupo.id)
                        cargarGrupos()
                    }
                )
                adapter.notifyItemChanged(position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()

                // Fondo rojo
                paint.color = Color.parseColor("#F44336")
                val fondoRojo = RectF(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRect(fondoRojo, paint)

                paint.color = Color.WHITE
                paint.textSize = 42f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.CENTER

                val centroY = (itemView.top + itemView.bottom) / 2f - (paint.descent() + paint.ascent()) / 2f
                val centroX = itemView.right - 100f
                c.drawText("Eliminar", centroX, centroY, paint)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun cargarGrupos() {
        listaDeGrupos.clear()

        val cursor = dbHelper.obtenerGruposActivos()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("idGrupo"))
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
            val descripcion =
                cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
            listaDeGrupos.add(Grupo(id, nombre, descripcion))
        }
        cursor.close()

        adapter.notifyDataSetChanged()
    }

    private fun mostrarDialogoGrupo(grupo: Grupo? = null) {
        val vistaDialogo  = layoutInflater.inflate(R.layout.dialog_grupo, null)
        val etNombre      = vistaDialogo.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = vistaDialogo.findViewById<EditText>(R.id.etDescripcion)

        val tvTitulo = vistaDialogo.findViewById<TextView>(R.id.tvTituloDialogo)
        tvTitulo.text = if (grupo == null) "Nuevo Grupo" else "Editar Grupo"

        grupo?.let {
            etNombre.setText(it.nombre)
            etDescripcion.setText(it.descripcion)
        }

        android.app.AlertDialog.Builder(requireContext())
            .setView(vistaDialogo)
            .create()
            .apply {
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

                vistaDialogo.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
                    dismiss()
                }

                vistaDialogo.findViewById<Button>(R.id.btnGuardar).setOnClickListener {
                    val nombre      = etNombre.text.toString().trim()
                    val descripcion = etDescripcion.text.toString().trim().ifEmpty { null }

                    if (nombre.isEmpty()) {
                        etNombre.error = "El nombre es obligatorio"
                        return@setOnClickListener
                    }

                    if (grupo == null) {
                        val exito = dbHelper.insertarGrupo(nombre, descripcion)
                        if (!exito) Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                    } else {
                        val exito = dbHelper.actualizarGrupo(grupo.id, nombre, descripcion)
                        if (!exito) Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }

                    cargarGrupos()
                    dismiss()
                }
            }
            .show()
    }
}