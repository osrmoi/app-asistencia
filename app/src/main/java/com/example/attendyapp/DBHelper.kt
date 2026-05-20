package com.example.attendyapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "ControlAsistencia", null, 4) {

    // Habilitar llaves foráneas.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createGrupo = """
            CREATE TABLE Grupo (
                idGrupo INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                descripcion TEXT,
                activo INTEGER DEFAULT 1
            )
        """.trimIndent()

        val createAlumno = """
            CREATE TABLE Alumno (
                idAlumno INTEGER PRIMARY KEY AUTOINCREMENT,
                matricula TEXT UNIQUE NOT NULL,
                nombreCompleto TEXT NOT NULL,
                activo INTEGER DEFAULT 1
            )
        """.trimIndent()

        val createGrupoAlumnoRef = """
            CREATE TABLE Grupo_Alumno_Ref (
                idGrupo INTEGER,
                idAlumno INTEGER,
                activo INTEGER DEFAULT 1,
                PRIMARY KEY (idGrupo, idAlumno),
                FOREIGN KEY (idGrupo) REFERENCES Grupo(idGrupo),
                FOREIGN KEY (idAlumno) REFERENCES Alumno(idAlumno)
            )
        """.trimIndent()

        val createAsistencia = """
            CREATE TABLE Asistencia (
                idAsistencia INTEGER PRIMARY KEY AUTOINCREMENT,
                idGrupo INTEGER NOT NULL,
                idAlumno INTEGER NOT NULL,
                fecha INTEGER NOT NULL, 
                estado TEXT NOT NULL,   
                FOREIGN KEY (idGrupo) REFERENCES Grupo(idGrupo),
                FOREIGN KEY (idAlumno) REFERENCES Alumno(idAlumno),
                UNIQUE (idGrupo, idAlumno, fecha) 
            )
        """.trimIndent()

        db.execSQL(createGrupo)
        db.execSQL(createAlumno)
        db.execSQL(createGrupoAlumnoRef)
        db.execSQL(createAsistencia)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Asistencia")
        db.execSQL("DROP TABLE IF EXISTS Grupo_Alumno_Ref")
        db.execSQL("DROP TABLE IF EXISTS Alumno")
        db.execSQL("DROP TABLE IF EXISTS Grupo")
        onCreate(db)
    }

    // MÉTODOS PARA GRUPOS

    fun insertarGrupo(nombre: String, descripcion: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
            put("activo", 1)
        }
        return db.insert("Grupo", null, values) != -1L
    }

    fun obtenerGruposActivos(): Cursor {
        return readableDatabase.rawQuery("SELECT * FROM Grupo WHERE activo = 1", null)
    }

    fun actualizarGrupo(idGrupo: Int, nombre: String, descripcion: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
        }
        return db.update("Grupo", values, "idGrupo=?", arrayOf(idGrupo.toString())) > 0
    }

    fun obtenerGrupoPorId(id: Int): Grupo? {
        val cursor = readableDatabase.query(
            "Grupo",
            null,
            "idGrupo = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            val grupo = Grupo(
                id          = cursor.getInt(cursor.getColumnIndexOrThrow("idGrupo")),
                nombre      = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
            )
            cursor.close()
            grupo
        } else {
            cursor.close()
            null
        }
    }

    /**
     * Realiza una baja lógica de la clase. No la borra físicamente.
     */
    fun eliminarGrupo(idGrupo: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("activo", 0) }
        return db.update("Grupo", values, "idGrupo=?", arrayOf(idGrupo.toString())) > 0
    }

    // MÉTODOS PARA ALUMNO

    /**
     * Inserta un alumno nuevo o reactiva uno que había sido eliminado.
     */
    fun insertarAlumno(matricula: String, nombreCompleto: String): Boolean {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT idAlumno, activo FROM Alumno WHERE matricula = ?", arrayOf(matricula))

        if (cursor.moveToFirst()) {
            val idAlumno = cursor.getInt(cursor.getColumnIndexOrThrow("idAlumno"))
            val activo = cursor.getInt(cursor.getColumnIndexOrThrow("activo"))
            cursor.close()

            if (activo == 0) {
                // El alumno existe pero estaba eliminado. Se reactiva.
                val values = ContentValues().apply {
                    put("nombreCompleto", nombreCompleto)
                    put("activo", 1)
                }
                return db.update("Alumno", values, "idAlumno=?", arrayOf(idAlumno.toString())) > 0
            } else {
                // El alumno ya existe y está activo. Retorna false para mostrar error de duplicado.
                return false
            }
        } else {
            cursor.close()
            // El alumno no existe, es un registro 100% nuevo.
            val values = ContentValues().apply {
                put("matricula", matricula)
                put("nombreCompleto", nombreCompleto)
                put("activo", 1)
            }
            return db.insert("Alumno", null, values) != -1L
        }
    }

    /**
     * Obtiene todos los alumnos vigentes en la escuela/sistema.
     * ACTUALMENTE, NO SE USA, PERO POR SI QUEDAN ALUMNOS QUE NO PERTENECEN A NINGÚN GRUPO ACTIVO, PERO SÍ A LA ESCUELA.
     */
    fun obtenerAlumnosActivos(): Cursor {
        return readableDatabase.rawQuery("SELECT * FROM Alumno WHERE activo = 1", null)
    }

    fun actualizarAlumno(idAlumno: Int, matricula: String, nombreCompleto: String): Boolean {
        val values = ContentValues().apply {
            put("matricula", matricula)
            put("nombreCompleto", nombreCompleto)
        }
        return writableDatabase.update(
            "Alumno", values, "idAlumno=?", arrayOf(idAlumno.toString())
        ) > 0
    }

    fun obtenerAlumnoPorMatricula(matricula: String): Alumno? {
        val cursor = readableDatabase.query(
            "Alumno",
            null,
            "matricula = ? AND activo = 1",
            arrayOf(matricula),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            val alumno = Alumno(
                id             = cursor.getInt(cursor.getColumnIndexOrThrow("idAlumno")),
                matricula      = cursor.getString(cursor.getColumnIndexOrThrow("matricula")),
                nombreCompleto = cursor.getString(cursor.getColumnIndexOrThrow("nombreCompleto"))
            )
            cursor.close()
            alumno
        } else {
            cursor.close()
            null
        }
    }

    // MÉTODOS PARA CLASE_ALUMNO_REF (INSCRIPCIONES A GRUPOS)

    /**
     * Asigna un alumno a una clase específica. Si ya estaba pero se dio de baja, lo reactiva.
     */
    fun inscribirAlumnoEnGrupo(idGrupo: Int, idAlumno: Int): Boolean {
        val db = writableDatabase

        val cursor = db.rawQuery("SELECT activo FROM Grupo_Alumno_Ref WHERE idGrupo = ? AND idAlumno = ?", arrayOf(idGrupo.toString(), idAlumno.toString()))

        if (cursor.moveToFirst()) {
            val activo = cursor.getInt(cursor.getColumnIndexOrThrow("activo"))
            cursor.close()

            if (activo == 0) {
                // Reactivar la inscripción
                val values = ContentValues().apply { put("activo", 1) }
                return db.update("Grupo_Alumno_Ref", values, "idGrupo=? AND idAlumno=?", arrayOf(idGrupo.toString(), idAlumno.toString())) > 0
            }
            return false // Ya está inscrito y activo
        } else {
            cursor.close()
            // Nueva inscripción
            val values = ContentValues().apply {
                put("idGrupo", idGrupo)
                put("idAlumno", idAlumno)
                put("activo", 1)
            }
            return db.insert("Grupo_Alumno_Ref", null, values) != -1L
        }
    }

    /**
     * Lista los alumnos que pertenecen a una materia en particular.
     * Filtra para que solo salgan alumnos activos en la escuela y activos en esa materia.
     */
    fun obtenerAlumnosPorGrupo(idGrupo: Int): Cursor {
        val query = """
            SELECT a.idAlumno, a.matricula, a.nombreCompleto 
            FROM Alumno a
            INNER JOIN Grupo_Alumno_Ref gar ON a.idAlumno = gar.idAlumno
            WHERE gar.idGrupo = ? AND a.activo = 1 AND gar.activo = 1
        """.trimIndent()
        return readableDatabase.rawQuery(query, arrayOf(idGrupo.toString()))
    }

    /**
     * Quita a un alumno de una materia específica sin borrarlo de la escuela.
     */
    fun darDeBajaAlumnoDeGrupo(idGrupo: Int, idAlumno: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("activo", 0) }
        return db.update("Grupo_Alumno_Ref", values, "idGrupo=? AND idAlumno=?", arrayOf(idGrupo.toString(), idAlumno.toString())) > 0
    }

    // MÉTODOS PARA ASISTENCIA

    /**
     * Consulta qué pasó en una clase en un día determinado (el historial de pases de lista).
     * Sigue funcionando incluso si el alumno ya fue dado de baja (porque consulta el histórico).
     */
    fun obtenerAsistenciaPorGrupoYFecha(idGrupo: Int, fecha: Long): Cursor {
        val query = """
            SELECT a.matricula, a.nombreCompleto, ast.estado
            FROM Asistencia ast
            INNER JOIN Alumno a ON ast.idAlumno = a.idAlumno
            WHERE ast.idGrupo = ? AND ast.fecha = ?
        """.trimIndent()
        return readableDatabase.rawQuery(query, arrayOf(idGrupo.toString(), fecha.toString()))
    }

    /**
     * Guarda la asistencia de todo el grupo en una sola operación de base de datos.
     * @param asistencias Un mapa (diccionario) donde la clave (Int) es el idAlumno y el valor (String) es el estado.
     */
    fun registrarAsistenciaMasiva(idGrupo: Int, fecha: Long, asistencias: Map<Int, String>): Boolean {
        val db = writableDatabase
        var exito = true
        // Iniciamos la transacción masiva
        db.beginTransaction()
        try {
            for ((idAlumno, estado) in asistencias) {
                val values = ContentValues().apply {
                    put("idGrupo", idGrupo)
                    put("idAlumno", idAlumno)
                    put("fecha", fecha)
                    put("estado", estado)
                }
                val result = db.insertWithOnConflict("Asistencia", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                if (result == -1L) {
                    exito = false // Si falla uno, marcamos error
                }
            }
            // Si todo salió bien, confirmamos los cambios en la base de datos
            if (exito) {
                db.setTransactionSuccessful()
            }
        } catch (e: Exception) {
            exito = false
        } finally {
            // Cerramos la transacción
            db.endTransaction()
        }
        return exito
    }

    /**
     * Obtiene todas las fechas distintas en que se registró asistencia
     * para un grupo, ordenadas de más antigua a más reciente.
     * Uso: Para construir las columnas del CSV exportado.
     */
    fun obtenerFechasConRegistro(idGrupo: Int): Cursor {
        return readableDatabase.rawQuery(
            "SELECT DISTINCT fecha FROM Asistencia WHERE idGrupo = ? ORDER BY fecha ASC",
            arrayOf(idGrupo.toString())
        )
    }
}