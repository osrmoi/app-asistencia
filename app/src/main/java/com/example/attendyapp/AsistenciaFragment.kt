package com.example.attendyapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AsistenciaFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private var listaGrupos = mutableListOf<Grupo>()
    private lateinit var adapter: GruposAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_asistencia, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGruposAsistencia)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Reutilizamndo GruposAdapter porque el item es el mismo visualmente
        adapter = GruposAdapter(listaGrupos) { grupoSeleccionado ->
            val fragment = TomarListaFragment.newInstance(
                grupoSeleccionado.id,
                grupoSeleccionado.nombre
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        cargarGrupos()
    }

    // Se llama también al regresar de TomarListaFragment (onResume)
    // por si se agregó un grupo nuevo mientras tanto
    override fun onResume() {
        super.onResume()
        cargarGrupos()
    }

    private fun cargarGrupos() {
        listaGrupos.clear()
        val cursor = dbHelper.obtenerGruposActivos()
        while (cursor.moveToNext()) {
            val id          = cursor.getInt(cursor.getColumnIndexOrThrow("idGrupo"))
            val nombre      = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
            val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
            listaGrupos.add(Grupo(id, nombre, descripcion))
        }
        cursor.close()
        adapter.notifyDataSetChanged()
    }
}