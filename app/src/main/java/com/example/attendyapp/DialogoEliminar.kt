package com.example.attendyapp

import android.content.Context
import androidx.appcompat.app.AlertDialog

object DialogUtils {

    fun mostrarConfirmacion(
        context: Context,
        titulo: String,
        mensaje: String,
        onAceptar: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar") { dialogo, _ ->
                onAceptar()
                dialogo.dismiss()
            }
            .setNegativeButton("Cancelar") { dialogo, _ ->
                dialogo.dismiss()
            }
            .create()
            .show()
    }
}