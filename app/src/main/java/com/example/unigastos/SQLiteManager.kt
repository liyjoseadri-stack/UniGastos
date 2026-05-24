package com.example.unigastos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Date

interface Transaccion {
    val id: Int
    val fecha: Date
    val concepto: String
    val cantidad: Double
}

data class Gasto(override val id: Int, override val fecha: Date, override val concepto: String, override val cantidad: Double) : Transaccion
data class Ingreso(override val id: Int, override val fecha: Date, override val concepto: String, override val cantidad: Double) : Transaccion

class SQLiteManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "unigastos.db"
        private const val DATABASE_VERSION = 4
        private var instance: SQLiteManager? = null

        fun getInstance(context: Context): SQLiteManager {
            if (instance == null) {
                instance = SQLiteManager(context.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuarios (nombre TEXT PRIMARY KEY, password TEXT, activo INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE gastos (id INTEGER PRIMARY KEY AUTOINCREMENT, concepto TEXT, cantidad REAL, fecha INTEGER, usuario_nombre TEXT)")
        db.execSQL("CREATE TABLE ingresos (id INTEGER PRIMARY KEY AUTOINCREMENT, concepto TEXT, cantidad REAL, fecha INTEGER, usuario_nombre TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE ingresos ADD COLUMN concepto TEXT DEFAULT 'Ingreso'") } catch (e: Exception) {}
        }
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE gastos ADD COLUMN concepto TEXT DEFAULT 'Gasto'") } catch (e: Exception) {}
        }
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE usuarios ADD COLUMN password TEXT DEFAULT ''") } catch (e: Exception) {}
        }
    }

    // --- Gestión de Usuarios ---
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
        if (cursor.moveToFirst()) { nombre = cursor.getString(0) }
        cursor.close()
        return nombre
    }

    fun usuarioExiste(nombre: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM usuarios WHERE nombre = ? LIMIT 1", arrayOf(nombre))
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe
    }

    fun registrarUsuario(nombre: String, password: String): Boolean {
        if (nombre.isBlank() || password.isBlank() || usuarioExiste(nombre)) return false
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("password", password)
        }
        return writableDatabase.insert("usuarios", null, values) != -1L
    }

    fun validarCredenciales(nombre: String, password: String): Boolean {
        val cursor = readableDatabase.rawQuery("SELECT 1 FROM usuarios WHERE nombre = ? AND password = ? LIMIT 1", arrayOf(nombre, password))
        val valido = cursor.moveToFirst()
        cursor.close()
        return valido
    }

    // --- Gestión de Gastos ---
    fun insertarGasto(concepto: String, cantidad: Double, fecha: Date) {
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
            put("usuario_nombre", obtenerUsuarioActivo())
        }
        writableDatabase.insert("gastos", null, values)
    }

    fun obtenerGastos(): List<Gasto> {
        val cursor = readableDatabase.rawQuery("SELECT * FROM gastos WHERE usuario_nombre = ?", arrayOf(obtenerUsuarioActivo()))
        val lista = mutableListOf<Gasto>()
        while (cursor.moveToNext()) {
            lista.add(Gasto(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                Date(cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))),
                cursor.getString(cursor.getColumnIndexOrThrow("concepto")) ?: "Gasto",
                cursor.getDouble(cursor.getColumnIndexOrThrow("cantidad"))
            ))
        }
        cursor.close()
        return lista
    }

    fun actualizarGasto(id: Int, concepto: String, cantidad: Double, fecha: Date) {
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
        }
        writableDatabase.update("gastos", values, "id = ?", arrayOf(id.toString()))
    }

    fun eliminarGasto(id: Int) {
        writableDatabase.delete("gastos", "id = ?", arrayOf(id.toString()))
    }

    // --- Gestión de Ingresos ---
    fun insertarIngreso(concepto: String, cantidad: Double, fecha: Date) {
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
            put("usuario_nombre", obtenerUsuarioActivo())
        }
        writableDatabase.insert("ingresos", null, values)
    }

    fun obtenerIngresos(): List<Ingreso> {
        val cursor = readableDatabase.rawQuery("SELECT * FROM ingresos WHERE usuario_nombre = ?", arrayOf(obtenerUsuarioActivo()))
        val lista = mutableListOf<Ingreso>()
        while (cursor.moveToNext()) {
            lista.add(Ingreso(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                Date(cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))),
                cursor.getString(cursor.getColumnIndexOrThrow("concepto")) ?: "Ingreso",
                cursor.getDouble(cursor.getColumnIndexOrThrow("cantidad"))
            ))
        }
        cursor.close()
        return lista
    }

    fun actualizarIngreso(id: Int, concepto: String, cantidad: Double, fecha: Date) {
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
        }
        writableDatabase.update("ingresos", values, "id = ?", arrayOf(id.toString()))
    }

    fun eliminarIngreso(id: Int) {
        writableDatabase.delete("ingresos", "id = ?", arrayOf(id.toString()))
    }
}
