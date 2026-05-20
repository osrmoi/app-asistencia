package com.example.attendyapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportadorCSV {

    /**
     * Genera y guarda el CSV de asistencia de un grupo.
     *
     * @param context     Contexto de la Activity o Fragment
     * @param grupo       Datos del grupo (nombre y descripción)
     * @param dbHelper    Instancia del DBHelper para consultar la BD
     * @return            Uri del archivo guardado, o null si falló
     */
    fun exportarAsistenciaGrupo(
        context: Context,
        grupo: Grupo,
        dbHelper: DBHelper
    ): Uri? {

        // Obtener todos los alumnos del grupo
        val alumnos = mutableListOf<Alumno>()
        val cursorAlumnos = dbHelper.obtenerAlumnosPorGrupo(grupo.id)
        while (cursorAlumnos.moveToNext()) {
            alumnos.add(
                Alumno(
                    id             = cursorAlumnos.getInt(cursorAlumnos.getColumnIndexOrThrow("idAlumno")),
                    matricula      = cursorAlumnos.getString(cursorAlumnos.getColumnIndexOrThrow("matricula")),
                    nombreCompleto = cursorAlumnos.getString(cursorAlumnos.getColumnIndexOrThrow("nombreCompleto"))
                )
            )
        }
        cursorAlumnos.close()

        if (alumnos.isEmpty()) return null

        // Obtener todas las fechas con registros para este grupo
        val fechas = mutableListOf<Long>()
        val cursorFechas = dbHelper.obtenerFechasConRegistro(grupo.id)
        while (cursorFechas.moveToNext()) {
            fechas.add(cursorFechas.getLong(cursorFechas.getColumnIndexOrThrow("fecha")))
        }
        cursorFechas.close()

        // Construir mapa de asistencia: [idAlumno][fecha] = estado
        // Estructura: Map<idAlumno, Map<fecha, estado>>
        val mapaAsistencia = mutableMapOf<Int, MutableMap<Long, String>>()
        alumnos.forEach { mapaAsistencia[it.id] = mutableMapOf() }

        fechas.forEach { fecha ->
            val cursorDia = dbHelper.obtenerAsistenciaPorGrupoYFecha(grupo.id, fecha)
            while (cursorDia.moveToNext()) {
                val matricula = cursorDia.getString(cursorDia.getColumnIndexOrThrow("matricula"))
                val estado    = cursorDia.getString(cursorDia.getColumnIndexOrThrow("estado"))
                val alumno    = alumnos.find { it.matricula == matricula }
                alumno?.let { mapaAsistencia[it.id]?.put(fecha, estado) }
            }
            cursorDia.close()
        }

        // Construir el contenido del CSV
        val sb = StringBuilder()
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))
        val formatoFechaNombre = SimpleDateFormat("ddMMMyyyy", Locale("es", "MX"))

        // Fila 1: Grupo y descripción
        val descripcion = grupo.descripcion ?: "Sin descripción"
        sb.appendLine("Grupo,${escaparCSV(grupo.nombre)},Descripción,${escaparCSV(descripcion)}")

        // Fila 2: vacía de separación
        sb.appendLine()

        // Fila 3: encabezados — Matrícula | Nombre | [fecha1] | [fecha2] | ...
        val encabezados = mutableListOf("Matrícula", "Nombre")
        fechas.forEach { encabezados.add(formatoFecha.format(Date(it))) }
        sb.appendLine(encabezados.joinToString(",") { escaparCSV(it) })

        // Filas de datos — una por alumno
        alumnos.forEach { alumno ->
            val fila = mutableListOf(alumno.matricula, alumno.nombreCompleto)
            fechas.forEach { fecha ->
                fila.add(mapaAsistencia[alumno.id]?.get(fecha) ?: "-")
            }
            sb.appendLine(fila.joinToString(",") { escaparCSV(it) })
        }

        // Guardar el archivo en Descargas
        val nombreArchivo = "Asistencia_${grupo.nombre.replace(" ", "_")}_${formatoFechaNombre.format(Date())}.csv"

        return guardarEnDescargas(context, nombreArchivo, sb.toString())
    }

    // Guarda el String como archivo en la carpeta Descargas del dispositivo
    private fun guardarEnDescargas(context: Context, nombreArchivo: String, contenido: String): Uri? {
        return try {
            val uri: Uri?
            val outputStream: OutputStream?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — MediaStore (sin permisos de almacenamiento requeridos)
                val valores = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores
                )
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                // Android 9 y menor — File I/O directo
                val carpeta = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val archivo = java.io.File(carpeta, nombreArchivo)
                uri = Uri.fromFile(archivo)
                outputStream = java.io.FileOutputStream(archivo)
            }

            outputStream?.use { stream ->
                // BOM UTF-8 para que Excel abra el CSV con acentos correctamente
                stream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                stream.write(contenido.toByteArray(Charsets.UTF_8))
            }

            uri
        } catch (e: Exception) {
            android.util.Log.e("ExportadorCSV", "Error al exportar: ${e::class.simpleName} — ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Escapa un campo CSV: si contiene coma, salto de línea o comilla,
    // lo envuelve en comillas dobles y escapa las comillas internas
    private fun escaparCSV(valor: String): String {
        return if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            "\"${valor.replace("\"", "\"\"")}\""
        } else {
            valor
        }
    }
}