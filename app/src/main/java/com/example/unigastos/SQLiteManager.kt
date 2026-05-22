package com.example.unigastos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Date

data class Gasto(val id: Int, val fecha: Date, val concepto: String, val cantidad: Double)
data class Ingreso(val id: Int, val fecha: Date, val cantidad: Double)

class SQLiteManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "unigastos.db"
        private const val DATABASE_VERSION = 1
        private var instance: SQLiteManager? = null

        fun getInstance(context: Context): SQLiteManager {
            if (instance == null) {
                instance = SQLiteManager(context.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuarios (nombre TEXT PRIMARY KEY, activo INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE gastos (id INTEGER PRIMARY KEY AUTOINCREMENT, concepto TEXT, cantidad REAL, fecha INTEGER, usuario_nombre TEXT)")
        db.execSQL("CREATE TABLE ingresos (id INTEGER PRIMARY KEY AUTOINCREMENT, cantidad REAL, fecha INTEGER, usuario_nombre TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun crearUsuario(nombre: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
        }
        db.insertWithOnConflict("usuarios", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun iniciarSesion(nombre: String) {
        val db = writableDatabase
        db.execSQL("UPDATE usuarios SET activo = 0")
        db.execSQL("UPDATE usuarios SET activo = 1 WHERE nombre = ?", arrayOf(nombre))
    }

    fun cerrarSesion() {
        val db = writableDatabase
        db.execSQL("UPDATE usuarios SET activo = 0")
    }

    fun obtenerUsuarioActivo(): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT nombre FROM usuarios WHERE activo = 1", null)
        var nombre = ""
        if (cursor.moveToFirst()) {
            nombre = cursor.getString(0)
        }
        cursor.close()
        return nombre
    }

    fun obtenerUsuarios(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT nombre FROM usuarios", null)
        val usuarios = mutableListOf<String>()
        while (cursor.moveToNext()) {
            usuarios.add(cursor.getString(0))
        }
        cursor.close()
        return usuarios
    }

    fun eliminarUsuario(nombre: String) {
        val db = writableDatabase
        db.delete("usuarios", "nombre = ?", arrayOf(nombre))
        db.delete("gastos", "usuario_nombre = ?", arrayOf(nombre))
        db.delete("ingresos", "usuario_nombre = ?", arrayOf(nombre))
    }

    fun editarUsuario(nombreActual: String, nuevoNombre: String) {
        val db = writableDatabase
        val values = ContentValues().apply { put("nombre", nuevoNombre) }
        db.update("usuarios", values, "nombre = ?", arrayOf(nombreActual))
        
        val recordValues = ContentValues().apply { put("usuario_nombre", nuevoNombre) }
        db.update("gastos", recordValues, "usuario_nombre = ?", arrayOf(nombreActual))
        db.update("ingresos", recordValues, "usuario_nombre = ?", arrayOf(nombreActual))
    }

    fun insertarGasto(concepto: String, cantidad: Double, fecha: Date) {
        val usuario = obtenerUsuarioActivo()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
            put("usuario_nombre", usuario)
        }
        db.insert("gastos", null, values)
    }

    fun obtenerGastos(): List<Gasto> {
        val usuario = obtenerUsuarioActivo()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM gastos WHERE usuario_nombre = ?", arrayOf(usuario))
        val gastos = mutableListOf<Gasto>()
        while (cursor.moveToNext()) {
            gastos.add(Gasto(
                cursor.getInt(0),
                Date(cursor.getLong(3)),
                cursor.getString(1),
                cursor.getDouble(2)
            ))
        }
        cursor.close()
        return gastos
    }

    fun eliminarGasto(id: Int) {
        writableDatabase.delete("gastos", "id = ?", arrayOf(id.toString()))
    }

    fun actualizarGasto(id: Int, concepto: String, cantidad: Double) {
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
        }
        writableDatabase.update("gastos", values, "id = ?", arrayOf(id.toString()))
    }

    fun insertarIngreso(cantidad: Double, fecha: Date) {
        val usuario = obtenerUsuarioActivo()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("cantidad", cantidad)
            put("fecha", fecha.time)
            put("usuario_nombre", usuario)
        }
        db.insert("ingresos", null, values)
    }

    fun obtenerIngresos(): List<Ingreso> {
        val usuario = obtenerUsuarioActivo()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ingresos WHERE usuario_nombre = ?", arrayOf(usuario))
        val ingresos = mutableListOf<Ingreso>()
        while (cursor.moveToNext()) {
            ingresos.add(Ingreso(
                cursor.getInt(0),
                Date(cursor.getLong(2)),
                cursor.getDouble(1)
            ))
        }
        cursor.close()
        return ingresos
    }

    fun eliminarIngreso(id: Int) {
        writableDatabase.delete("ingresos", "id = ?", arrayOf(id.toString()))
    }

    fun actualizarIngreso(id: Int, cantidad: Double) {
        val values = ContentValues().apply {
            put("cantidad", cantidad)
        }
        writableDatabase.update("ingresos", values, "id = ?", arrayOf(id.toString()))
    }
}
