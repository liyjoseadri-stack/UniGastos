package com.example.unigastos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Date
import java.util.UUID

interface Transaccion {
    val id: String
    val fecha: Date
    val concepto: String
    val cantidad: Double
}

data class Gasto(
    override val id: String,
    override val fecha: Date,
    override val concepto: String,
    override val cantidad: Double,
    val estado: String = "APROBADO"
) : Transaccion

data class Ingreso(
    override val id: String,
    override val fecha: Date,
    override val concepto: String,
    override val cantidad: Double
) : Transaccion

data class UsuarioInfo(val nombre: String, val rol: String, val vinculadoA: String)

class SQLiteManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "unigastos.db"
        private const val DATABASE_VERSION = 6
        private var instance: SQLiteManager? = null

        fun getInstance(context: Context): SQLiteManager {
            if (instance == null) {
                instance = SQLiteManager(context.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuarios (nombre TEXT PRIMARY KEY, password TEXT, activo INTEGER DEFAULT 0, rol TEXT DEFAULT 'USUARIO', vinculado_a TEXT)")
        db.execSQL("CREATE TABLE gastos (id INTEGER PRIMARY KEY AUTOINCREMENT, remote_id TEXT UNIQUE, concepto TEXT, cantidad REAL, fecha INTEGER, usuario_nombre TEXT, estado TEXT DEFAULT 'APROBADO')")
        db.execSQL("CREATE TABLE ingresos (id INTEGER PRIMARY KEY AUTOINCREMENT, remote_id TEXT UNIQUE, concepto TEXT, cantidad REAL, fecha INTEGER, usuario_nombre TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE ingresos ADD COLUMN concepto TEXT DEFAULT 'Ingreso'") } catch (_: Exception) {}
        }
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE gastos ADD COLUMN concepto TEXT DEFAULT 'Gasto'") } catch (_: Exception) {}
        }
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE usuarios ADD COLUMN password TEXT DEFAULT ''") } catch (_: Exception) {}
        }
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE usuarios ADD COLUMN rol TEXT DEFAULT 'USUARIO'") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE usuarios ADD COLUMN vinculado_a TEXT") } catch (_: Exception) {}
        }
        if (oldVersion < 6) {
            try { db.execSQL("ALTER TABLE gastos ADD COLUMN remote_id TEXT") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE ingresos ADD COLUMN remote_id TEXT") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE gastos ADD COLUMN estado TEXT DEFAULT 'APROBADO'") } catch (_: Exception) {}
            db.execSQL("UPDATE gastos SET remote_id = 'local-gasto-' || id WHERE remote_id IS NULL OR remote_id = ''")
            db.execSQL("UPDATE ingresos SET remote_id = 'local-ingreso-' || id WHERE remote_id IS NULL OR remote_id = ''")
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        asegurarColumnas(db)
    }

    private fun asegurarColumnas(db: SQLiteDatabase) {
        if (tablaExiste(db, "usuarios")) {
            agregarColumnaSiFalta(db, "usuarios", "password", "TEXT DEFAULT ''")
            agregarColumnaSiFalta(db, "usuarios", "activo", "INTEGER DEFAULT 0")
            agregarColumnaSiFalta(db, "usuarios", "rol", "TEXT DEFAULT 'USUARIO'")
            agregarColumnaSiFalta(db, "usuarios", "vinculado_a", "TEXT")
            db.execSQL("UPDATE usuarios SET vinculado_a = nombre WHERE vinculado_a IS NULL OR vinculado_a = ''")
        }
        if (tablaExiste(db, "gastos")) {
            agregarColumnaSiFalta(db, "gastos", "remote_id", "TEXT")
            agregarColumnaSiFalta(db, "gastos", "concepto", "TEXT DEFAULT 'Gasto'")
            agregarColumnaSiFalta(db, "gastos", "usuario_nombre", "TEXT")
            agregarColumnaSiFalta(db, "gastos", "estado", "TEXT DEFAULT 'APROBADO'")
            db.execSQL("UPDATE gastos SET remote_id = 'local-gasto-' || id WHERE remote_id IS NULL OR remote_id = ''")
            db.execSQL("UPDATE gastos SET estado = 'APROBADO' WHERE estado IS NULL OR estado = ''")
        }
        if (tablaExiste(db, "ingresos")) {
            agregarColumnaSiFalta(db, "ingresos", "remote_id", "TEXT")
            agregarColumnaSiFalta(db, "ingresos", "concepto", "TEXT DEFAULT 'Ingreso'")
            agregarColumnaSiFalta(db, "ingresos", "usuario_nombre", "TEXT")
            db.execSQL("UPDATE ingresos SET remote_id = 'local-ingreso-' || id WHERE remote_id IS NULL OR remote_id = ''")
        }
    }

    private fun tablaExiste(db: SQLiteDatabase, tabla: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(tabla)
        )
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe
    }

    private fun agregarColumnaSiFalta(db: SQLiteDatabase, tabla: String, columna: String, definicion: String) {
        val cursor = db.rawQuery("PRAGMA table_info($tabla)", null)
        var existe = false
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == columna) {
                existe = true
                break
            }
        }
        cursor.close()
        if (!existe) {
            db.execSQL("ALTER TABLE $tabla ADD COLUMN $columna $definicion")
        }
    }

    fun iniciarSesion(nombre: String) {
        val db = writableDatabase
        db.execSQL("UPDATE usuarios SET activo = 0")
        db.execSQL("UPDATE usuarios SET activo = 1 WHERE nombre = ?", arrayOf(nombre))
    }

    fun cerrarSesion() {
        writableDatabase.execSQL("UPDATE usuarios SET activo = 0")
    }

    fun obtenerUsuarioActivo(): String {
        val cursor = readableDatabase.rawQuery("SELECT nombre FROM usuarios WHERE activo = 1", null)
        val nombre = if (cursor.moveToFirst()) cursor.getString(0) else ""
        cursor.close()
        return nombre
    }

    fun obtenerUsuarioActivoInfo(): UsuarioInfo? {
        val nombreActivo = obtenerUsuarioActivo()
        if (nombreActivo.isEmpty()) return null
        return obtenerInfoUsuario(nombreActivo)
    }

    fun usuarioExiste(nombre: String): Boolean {
        val cursor = readableDatabase.rawQuery("SELECT 1 FROM usuarios WHERE nombre = ? LIMIT 1", arrayOf(nombre))
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe
    }

    fun registrarUsuario(nombre: String, password: String, rol: String = "USUARIO", vinculadoA: String? = null): Boolean {
        if (nombre.isBlank() || password.isBlank() || usuarioExiste(nombre)) return false
        guardarUsuario(nombre, password, rol, vinculadoA ?: nombre)
        return true
    }

    fun guardarUsuario(nombre: String, password: String, rol: String, vinculadoA: String) {
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("password", password)
            put("rol", rol)
            put("vinculado_a", vinculadoA)
        }
        writableDatabase.insertWithOnConflict("usuarios", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun validarCredenciales(nombre: String, password: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT 1 FROM usuarios WHERE nombre = ? AND password = ? LIMIT 1",
            arrayOf(nombre, password)
        )
        val valido = cursor.moveToFirst()
        cursor.close()
        return valido
    }

    fun obtenerInfoUsuario(nombre: String, password: String? = null): UsuarioInfo? {
        val args = if (password == null) arrayOf(nombre) else arrayOf(nombre, password)
        val where = if (password == null) "nombre = ?" else "nombre = ? AND password = ?"
        val cursor = readableDatabase.rawQuery("SELECT nombre, rol, vinculado_a FROM usuarios WHERE $where", args)
        val info = if (cursor.moveToFirst()) {
            UsuarioInfo(
                cursor.getString(0),
                cursor.getString(1) ?: "USUARIO",
                cursor.getString(2) ?: cursor.getString(0)
            )
        } else {
            null
        }
        cursor.close()
        return info
    }

    fun usuarioDatosActual(): String {
        return obtenerUsuarioActivoInfo()?.vinculadoA ?: ""
    }

    fun insertarGasto(concepto: String, cantidad: Double, fecha: Date): String {
        val info = obtenerUsuarioActivoInfo() ?: return ""
        if (info.rol == "TUTOR") return ""
        val remoteId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("remote_id", remoteId)
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
            put("usuario_nombre", info.nombre)
            put("estado", if (cantidad >= LIMITE_GASTO_ALTO) "PENDIENTE" else "APROBADO")
        }
        writableDatabase.insert("gastos", null, values)
        return remoteId
    }

    fun obtenerGastos(): List<Gasto> {
        val info = obtenerUsuarioActivoInfo() ?: return emptyList()
        return obtenerGastosDeUsuario(info.vinculadoA)
    }

    fun obtenerGastosDeUsuario(usuarioNombre: String): List<Gasto> {
        val cursor = readableDatabase.rawQuery("SELECT * FROM gastos WHERE usuario_nombre = ?", arrayOf(usuarioNombre))
        val lista = mutableListOf<Gasto>()
        while (cursor.moveToNext()) {
            lista.add(
                Gasto(
                    cursor.getString(cursor.getColumnIndexOrThrow("remote_id"))
                        ?: cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    Date(cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))),
                    cursor.getString(cursor.getColumnIndexOrThrow("concepto")) ?: "Gasto",
                    cursor.getDouble(cursor.getColumnIndexOrThrow("cantidad")),
                    cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: "APROBADO"
                )
            )
        }
        cursor.close()
        return lista
    }

    fun actualizarGasto(id: String, concepto: String, cantidad: Double, fecha: Date) {
        val info = obtenerUsuarioActivoInfo() ?: return
        if (info.rol == "TUTOR") return
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
        }
        writableDatabase.update("gastos", values, "remote_id = ? OR id = ?", arrayOf(id, id))
    }

    fun eliminarGasto(id: String) {
        val info = obtenerUsuarioActivoInfo() ?: return
        if (info.rol == "TUTOR") return
        writableDatabase.delete("gastos", "remote_id = ? OR id = ?", arrayOf(id, id))
    }

    fun actualizarEstadoGasto(id: String, estado: String) {
        val values = ContentValues().apply {
            put("estado", estado)
        }
        writableDatabase.update("gastos", values, "remote_id = ? OR id = ?", arrayOf(id, id))
    }

    fun insertarIngreso(concepto: String, cantidad: Double, fecha: Date): String {
        val info = obtenerUsuarioActivoInfo() ?: return ""
        if (info.rol == "TUTOR") return ""
        return insertarIngresoParaUsuario(info.nombre, concepto, cantidad, fecha)
    }

    fun insertarIngresoParaUsuario(usuarioNombre: String, concepto: String, cantidad: Double, fecha: Date): String {
        val remoteId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("remote_id", remoteId)
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
            put("usuario_nombre", usuarioNombre)
        }
        writableDatabase.insert("ingresos", null, values)
        return remoteId
    }

    fun obtenerIngresos(): List<Ingreso> {
        val info = obtenerUsuarioActivoInfo() ?: return emptyList()
        return obtenerIngresosDeUsuario(info.vinculadoA)
    }

    fun obtenerIngresosDeUsuario(usuarioNombre: String): List<Ingreso> {
        val cursor = readableDatabase.rawQuery("SELECT * FROM ingresos WHERE usuario_nombre = ?", arrayOf(usuarioNombre))
        val lista = mutableListOf<Ingreso>()
        while (cursor.moveToNext()) {
            lista.add(
                Ingreso(
                    cursor.getString(cursor.getColumnIndexOrThrow("remote_id"))
                        ?: cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    Date(cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))),
                    cursor.getString(cursor.getColumnIndexOrThrow("concepto")) ?: "Ingreso",
                    cursor.getDouble(cursor.getColumnIndexOrThrow("cantidad"))
                )
            )
        }
        cursor.close()
        return lista
    }

    fun actualizarIngreso(id: String, concepto: String, cantidad: Double, fecha: Date) {
        val info = obtenerUsuarioActivoInfo() ?: return
        if (info.rol == "TUTOR") return
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("cantidad", cantidad)
            put("fecha", fecha.time)
        }
        writableDatabase.update("ingresos", values, "remote_id = ? OR id = ?", arrayOf(id, id))
    }

    fun eliminarIngreso(id: String) {
        val info = obtenerUsuarioActivoInfo() ?: return
        if (info.rol == "TUTOR") return
        writableDatabase.delete("ingresos", "remote_id = ? OR id = ?", arrayOf(id, id))
    }

    fun guardarIngresosDesdeServidor(usuarioNombre: String, ingresos: List<Ingreso>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("ingresos", "usuario_nombre = ?", arrayOf(usuarioNombre))
            ingresos.forEach { ingreso ->
                val values = ContentValues().apply {
                    put("remote_id", ingreso.id)
                    put("concepto", ingreso.concepto)
                    put("cantidad", ingreso.cantidad)
                    put("fecha", ingreso.fecha.time)
                    put("usuario_nombre", usuarioNombre)
                }
                db.insertWithOnConflict("ingresos", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun guardarGastosDesdeServidor(usuarioNombre: String, gastos: List<Gasto>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("gastos", "usuario_nombre = ?", arrayOf(usuarioNombre))
            gastos.forEach { gasto ->
                val values = ContentValues().apply {
                    put("remote_id", gasto.id)
                    put("concepto", gasto.concepto)
                    put("cantidad", gasto.cantidad)
                    put("fecha", gasto.fecha.time)
                    put("usuario_nombre", usuarioNombre)
                    put("estado", gasto.estado)
                }
                db.insertWithOnConflict("gastos", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

const val LIMITE_GASTO_ALTO = 5000.0
