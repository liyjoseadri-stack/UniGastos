package com.example.unigastos

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreManager private constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private var instance: FirestoreManager? = null

        fun getInstance(): FirestoreManager {
            if (instance == null) {
                instance = FirestoreManager()
            }
            return instance!!
        }
    }

    suspend fun usuarioExiste(nombre: String): Boolean {
        return usuarios().document(nombre).get().esperar().exists()
    }

    suspend fun registrarAlumnoYTutor(
        alumno: String,
        passwordAlumno: String,
        tutor: String,
        passwordTutor: String
    ): Boolean {
        if (usuarioExiste(alumno) || usuarioExiste(tutor)) return false

        val batch = firestore.batch()
        batch.set(
            usuarios().document(alumno),
            mapOf(
                "nombre" to alumno,
                "password" to passwordAlumno,
                "rol" to "USUARIO",
                "vinculadoA" to alumno,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            usuarios().document(tutor),
            mapOf(
                "nombre" to tutor,
                "password" to passwordTutor,
                "rol" to "TUTOR",
                "vinculadoA" to alumno,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().esperar()
        return true
    }

    suspend fun validarCredenciales(nombre: String, password: String): UsuarioInfo? {
        val doc = usuarios().document(nombre).get().esperar()
        if (!doc.exists()) return null
        if ((doc.getString("password") ?: "") != password) return null
        return UsuarioInfo(
            nombre = doc.getString("nombre") ?: nombre,
            rol = doc.getString("rol") ?: "USUARIO",
            vinculadoA = doc.getString("vinculadoA") ?: nombre
        )
    }

    suspend fun guardarIngreso(usuario: String, ingreso: Ingreso) {
        usuarios()
            .document(usuario)
            .collection("ingresos")
            .document(ingreso.id)
            .set(ingreso.toFirestoreMap())
            .esperar()
    }

    suspend fun guardarGasto(usuario: String, gasto: Gasto) {
        usuarios()
            .document(usuario)
            .collection("gastos")
            .document(gasto.id)
            .set(gasto.toFirestoreMap())
            .esperar()
    }

    suspend fun eliminarIngreso(usuario: String, id: String) {
        usuarios().document(usuario).collection("ingresos").document(id).delete().esperar()
    }

    suspend fun eliminarGasto(usuario: String, id: String) {
        usuarios().document(usuario).collection("gastos").document(id).delete().esperar()
    }

    suspend fun actualizarEstadoGasto(usuario: String, id: String, estado: String) {
        usuarios()
            .document(usuario)
            .collection("gastos")
            .document(id)
            .update(
                mapOf(
                    "estado" to estado,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .esperar()
    }

    suspend fun subirDatosLocales(usuario: String, ingresos: List<Ingreso>, gastos: List<Gasto>) {
        val batch = firestore.batch()
        ingresos.forEach { ingreso ->
            batch.set(usuarios().document(usuario).collection("ingresos").document(ingreso.id), ingreso.toFirestoreMap())
        }
        gastos.forEach { gasto ->
            batch.set(usuarios().document(usuario).collection("gastos").document(gasto.id), gasto.toFirestoreMap())
        }
        batch.commit().esperar()
    }

    suspend fun guardarPresupuesto(usuario: String, presupuesto: Double) {
        usuarios()
            .document(usuario)
            .collection("configuracion")
            .document("presupuesto")
            .set(
                mapOf(
                    "monto" to presupuesto,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .esperar()
    }

    fun observarPresupuesto(
        usuario: String,
        onChange: (Double) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return usuarios()
            .document(usuario)
            .collection("configuracion")
            .document("presupuesto")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val monto = snapshot?.getDouble("monto") ?: 0.0
                onChange(monto)
            }
    }

    fun observarIngresos(
        usuario: String,
        onChange: (List<Ingreso>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return usuarios()
            .document(usuario)
            .collection("ingresos")
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents?.map { doc ->
                    Ingreso(
                        id = doc.id,
                        fecha = doc.readDate(),
                        concepto = doc.getString("concepto") ?: "Ingreso",
                        cantidad = doc.getDouble("cantidad") ?: 0.0
                    )
                }.orEmpty())
            }
    }

    fun observarGastos(
        usuario: String,
        onChange: (List<Gasto>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return usuarios()
            .document(usuario)
            .collection("gastos")
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents?.map { doc ->
                    Gasto(
                        id = doc.id,
                        fecha = doc.readDate(),
                        concepto = doc.getString("concepto") ?: "Gasto",
                        cantidad = doc.getDouble("cantidad") ?: 0.0,
                        estado = doc.getString("estado") ?: "APROBADO"
                    )
                }.orEmpty())
            }
    }

    private fun usuarios() = firestore.collection("usuarios")

    private fun Ingreso.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "concepto" to concepto,
            "cantidad" to cantidad,
            "fecha" to Timestamp(fecha),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun Gasto.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "concepto" to concepto,
            "cantidad" to cantidad,
            "fecha" to Timestamp(fecha),
            "estado" to estado,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.readDate(): Date {
        return getTimestamp("fecha")?.toDate() ?: Date(getLong("fecha") ?: System.currentTimeMillis())
    }
}

private suspend fun <T> Task<T>.esperar(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}

fun mensajeFirestore(error: Exception): String {
    val firestoreError = error as? FirebaseFirestoreException
    return when (firestoreError?.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "No se pudo conectar con Firestore. Revisa internet, que Cloud Firestore este activado y vuelve a intentar."
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore rechazo la operacion. Revisa las reglas de seguridad en Firebase Console."
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "No se encontro Cloud Firestore. Crea la base de datos en Firebase Console."
        else ->
            error.message ?: "No se pudo completar la operacion en Firestore."
    }
}
