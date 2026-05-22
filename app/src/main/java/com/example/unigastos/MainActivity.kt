package com.example.unigastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unigastos.ui.theme.UniGastosTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniGastosTheme {
                val navController = rememberNavController()
                val db = SQLiteManager.getInstance(LocalContext.current)

                NavHost(navController = navController, startDestination = "login") {
                    composable("login") { LoginScreen(navController, db) }
                    composable("home/{usuario}") { backStackEntry ->
                        val user = backStackEntry.arguments?.getString("usuario") ?: ""
                        HomeScreen(user, navController, db)
                    }
                }
            }
        }
    }
}

// --- COLORES ---
val FondoInicio = Color(0xFF1C133F)
val FondoFin = Color(0xFF65909D)
val TarjetaInicio = Color(0xFF282A5C)
val TarjetaFin = Color(0xFF3A4178)
val HeaderColor = Color(0xFF1E0F48)

@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoInicio, FondoFin)))
    ) {
        content()
    }
}

// --- PANTALLA LOGIN ---
@Composable
fun LoginScreen(navController: NavHostController, db: SQLiteManager) {
    var usuario by remember { mutableStateOf("") }
    var usuariosGuardados by remember { mutableStateOf(db.obtenerUsuarios()) }
    var usuarioAEliminar by remember { mutableStateOf<String?>(null) }
    var usuarioAEditar by remember { mutableStateOf<String?>(null) }
    var nuevoNombre by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val activo = db.obtenerUsuarioActivo()
        if (activo.isNotEmpty()) {
            navController.navigate("home/$activo")
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )

            Text("UniGastos", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(TarjetaInicio, TarjetaFin)), RoundedCornerShape(35.dp))
                    .padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Registro", fontSize = 32.sp, color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = usuario,
                    onValueChange = { usuario = it },
                    placeholder = { Text("Ingresa tu usuario", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(35.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Button(
                    onClick = {
                        if (usuario.isNotEmpty()) {
                            db.crearUsuario(usuario)
                            db.iniciarSesion(usuario)
                            navController.navigate("home/$usuario")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Crea Cuenta", fontSize = 22.sp, color = Color.White.copy(0.7f))
                }

                Text("Cuentas guardadas", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))

                LazyColumn(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                    items(usuariosGuardados) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    db.iniciarSesion(user)
                                    navController.navigate("home/$user")
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(user, color = Color.White)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { usuarioAEditar = user; nuevoNombre = user }) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Yellow)
                            }
                            IconButton(onClick = { usuarioAEliminar = user }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        // Diálogos de Eliminar y Editar
        usuarioAEliminar?.let { user ->
            AlertDialog(
                onDismissRequest = { usuarioAEliminar = null },
                title = { Text("Eliminar usuario") },
                text = { Text("¿Seguro que deseas eliminar a $user?") },
                confirmButton = {
                    Button(onClick = {
                        db.eliminarUsuario(user)
                        usuariosGuardados = db.obtenerUsuarios()
                        usuarioAEliminar = null
                    }) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { usuarioAEliminar = null }) { Text("Cancelar") } }
            )
        }

        usuarioAEditar?.let { viejo ->
            Dialog(onDismissRequest = { usuarioAEditar = null }) {
                Surface(shape = RoundedCornerShape(20.dp), color = TarjetaInicio) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Editar Usuario", color = Color.White, fontSize = 20.sp)
                        TextField(value = nuevoNombre, onValueChange = { nuevoNombre = it })
                        Button(onClick = {
                            db.editarUsuario(viejo, nuevoNombre)
                            usuariosGuardados = db.obtenerUsuarios()
                            usuarioAEditar = null
                        }) { Text("Actualizar") }
                    }
                }
            }
        }
    }
}

// --- PANTALLA PRINCIPAL ---
@Composable
fun HomeScreen(nombre: String, navController: NavHostController, db: SQLiteManager) {
    var vistaActiva by remember { mutableStateOf("Resumen") }
    var ingresos by remember { mutableStateOf(db.obtenerIngresos()) }
    var gastos by remember { mutableStateOf(db.obtenerGastos()) }

    BackHandler {
        db.cerrarSesion()
        navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FondoInicio, FondoFin)))) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(HeaderColor).padding(20.dp).padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hola, $nombre", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                db.cerrarSesion()
                navController.popBackStack()
            }) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
            }
        }

        // Tabs
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF221852)).padding(10.dp)) {
            TabItem("Ingresos", vistaActiva == "Ingresos") { vistaActiva = "Ingresos" }
            TabItem("Resumen", vistaActiva == "Resumen") { vistaActiva = "Resumen" }
            TabItem("Gastos", vistaActiva == "Gastos") { vistaActiva = "Gastos" }
        }

        when (vistaActiva) {
            "Resumen" -> ResumenView(ingresos, gastos)
            "Ingresos" -> TransaccionesView(true, ingresos, emptyList(), db) { ingresos = db.obtenerIngresos() }
            "Gastos" -> TransaccionesView(false, emptyList(), gastos, db) { gastos = db.obtenerGastos() }
        }
    }
}

@Composable
fun RowScope.TabItem(text: String, activo: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = if (activo) Color.White else Color.White.copy(0.7f), fontWeight = FontWeight.Bold)
        if (activo) Box(modifier = Modifier.height(3.dp).width(60.dp).background(Color.White))
    }
}

@Composable
fun ResumenView(ingresos: List<Ingreso>, gastos: List<Gasto>) {
    var filtro by remember { mutableStateOf("Mes") }
    val ingresosF = ingresos.filter { filtrarPorTiempo(it.fecha, filtro) }
    val gastosF = gastos.filter { filtrarPorTiempo(it.fecha, filtro) }
    val totalI = ingresosF.sumOf { it.cantidad }
    val totalG = gastosF.sumOf { it.cantidad }
    val balance = totalI - totalG

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1254)),
            shape = RoundedCornerShape(35.dp)
        ) {
            Column(modifier = Modifier.padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Saldo Actual:", color = Color.White, fontSize = 24.sp)
                Text("$${String.format("%.0f", balance)}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Gráfico de Sectores Simple
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF34396E), Color(0xFF526580))), RoundedCornerShape(35.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (totalI + totalG > 0) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val sweepI = (totalI / (totalI + totalG) * 360).toFloat()
                    drawArc(Color.Blue, -90f, sweepI, true)
                    drawArc(Color.Magenta, -90f + sweepI, 360f - sweepI, true)
                }
            } else {
                Text("Sin datos", color = Color.White.copy(0.6f))
            }
        }

        Row(modifier = Modifier.padding(top = 20.dp)) {
            FilterButton("Día", filtro == "Día") { filtro = "Día" }
            FilterButton("Semana", filtro == "Semana") { filtro = "Semana" }
            FilterButton("Mes", filtro == "Mes") { filtro = "Mes" }
        }
    }
}

@Composable
fun TransaccionesView(esIngreso: Boolean, ingresos: List<Ingreso>, gastos: List<Gasto>, db: SQLiteManager, onUpdate: () -> Unit) {
    var mostrarForm by remember { mutableStateOf(false) }
    val total = if (esIngreso) ingresos.sumOf { it.cantidad } else gastos.sumOf { it.cantidad }

    Column(modifier = Modifier.fillMaxSize().padding(15.dp)) {
        Text(if (esIngreso) "Ingreso Total: $$total" else "Gasto Total: $$total",
            fontSize = 28.sp, color = Color.White, modifier = Modifier.padding(bottom = 10.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (esIngreso) {
                items(ingresos.reversed()) { item ->
                    TransactionRow("Ingreso", item.cantidad, item.fecha)
                }
            } else {
                items(gastos.reversed()) { item ->
                    TransactionRow(item.concepto, -item.cantidad, item.fecha)
                }
            }
        }

        Button(
            onClick = { mostrarForm = true },
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Añadir ${if (esIngreso) "Ingreso" else "Gasto"}")
        }
    }

    if (mostrarForm) {
        FormularioDialog(esIngreso, onDismiss = { mostrarForm = false }) { concepto, monto ->
            if (esIngreso) db.insertarIngreso(monto, Date())
            else db.insertarGasto(concepto, monto, Date())
            onUpdate()
            mostrarForm = false
        }
    }
}

@Composable
fun TransactionRow(titulo: String, monto: Double, fecha: Date) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column {
            Text(titulo, color = Color.White, fontWeight = FontWeight.Bold)
            Text(sdf.format(fecha), color = Color.White.copy(0.6f), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("$${String.format("%.0f", monto)}", color = if (monto > 0) Color.Green else Color.Red)
    }
}

@Composable
fun FilterButton(text: String, activo: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (activo) Color.Blue else Color.Gray),
        modifier = Modifier.padding(4.dp)
    ) { Text(text) }
}

@Composable
fun FormularioDialog(esGasto: Boolean, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var concepto by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(30.dp), color = TarjetaInicio) {
            Column(modifier = Modifier.padding(25.dp)) {
                Text(if (esGasto) "Nuevo Gasto" else "Nuevo Ingreso", fontSize = 24.sp, color = Color.White)
                if (esGasto) {
                    TextField(value = concepto, onValueChange = { concepto = it }, label = { Text("Concepto") })
                }
                TextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Button(onClick = {
                    val m = monto.toDoubleOrNull() ?: 0.0
                    onSave(concepto.ifEmpty { "Ingreso" }, m)
                }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text("Guardar")
                }
            }
        }
    }
}

fun filtrarPorTiempo(fecha: Date, periodo: String): Boolean {
    val cal = Calendar.getInstance()
    val itemCal = Calendar.getInstance().apply { time = fecha }
    return when (periodo) {
        "Día" -> cal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
        "Semana" -> cal.get(Calendar.WEEK_OF_YEAR) == itemCal.get(Calendar.WEEK_OF_YEAR)
        "Mes" -> cal.get(Calendar.MONTH) == itemCal.get(Calendar.MONTH)
        else -> true
    }
}