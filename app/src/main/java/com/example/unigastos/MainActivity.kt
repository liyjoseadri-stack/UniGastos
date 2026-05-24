package com.example.unigastos

import android.app.DatePickerDialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                    composable("register") { RegisterScreen(navController, db) }
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
    var loginUsuario by remember { mutableStateOf("") }
    var loginContrasena by remember { mutableStateOf("") }
    var mensajeDialogo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val activo = db.obtenerUsuarioActivo()
        if (activo.isNotEmpty()) {
            navController.navigate("home/$activo")
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
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
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(TarjetaInicio, TarjetaFin)), RoundedCornerShape(35.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Iniciar Sesión", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = loginUsuario,
                    onValueChange = { loginUsuario = it },
                    placeholder = { Text("Usuario", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = loginContrasena,
                    onValueChange = { loginContrasena = it },
                    placeholder = { Text("Contraseña", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Button(
                    onClick = {
                        val u = loginUsuario.trim()
                        val p = loginContrasena.trim()
                        if (u.isEmpty() || p.isEmpty()) {
                            mensajeDialogo = "Ambos campos son obligatorios para iniciar sesión."
                        } else if (db.validarCredenciales(u, p)) {
                            db.iniciarSesion(u)
                            navController.navigate("home/$u")
                        } else {
                            mensajeDialogo = "Credenciales incorrectas. Verifica usuario y contraseña."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Entrar", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¿No tienes cuenta?", color = Color.White.copy(0.8f))
                    TextButton(onClick = { navController.navigate("register") }) {
                        Text("Regístrate", color = Color.Cyan)
                    }
                }
            }
        }

        mensajeDialogo?.let { msg ->
            AlertDialog(
                onDismissRequest = { mensajeDialogo = null },
                title = { Text("Información") },
                text = { Text(msg) },
                confirmButton = {
                    Button(onClick = { mensajeDialogo = null }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}

@Composable
fun RegisterScreen(navController: NavHostController, db: SQLiteManager) {
    var registroUsuario by remember { mutableStateOf("") }
    var registroContrasena by remember { mutableStateOf("") }
    var mensajeDialogo by remember { mutableStateOf<String?>(null) }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(110.dp),
                tint = Color.White
            )
            Text("Crear cuenta", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(TarjetaInicio, TarjetaFin)), RoundedCornerShape(35.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Registro", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = registroUsuario,
                    onValueChange = { registroUsuario = it },
                    placeholder = { Text("Crea un usuario", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = registroContrasena,
                    onValueChange = { registroContrasena = it },
                    placeholder = { Text("Crea una contraseña", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Button(
                    onClick = {
                        val u = registroUsuario.trim()
                        val p = registroContrasena.trim()
                        when {
                            u.isEmpty() || p.isEmpty() -> {
                                mensajeDialogo = "Debes ingresar usuario y contraseña para registrarte."
                            }
                            db.usuarioExiste(u) -> {
                                mensajeDialogo = "El usuario ya existe. Intenta con otro nombre."
                            }
                            else -> {
                                val ok = db.registrarUsuario(u, p)
                                if (ok) {
                                    db.iniciarSesion(u)
                                    navController.navigate("home/$u")
                                } else {
                                    mensajeDialogo = "No se pudo registrar la cuenta. Intenta nuevamente."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Crear Cuenta", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Ya tienes cuenta?", color = Color.White.copy(0.8f))
                    TextButton(onClick = { navController.navigate("login") }) {
                        Text("Inicia sesión", color = Color.Cyan)
                    }
                }
            }
        }

        mensajeDialogo?.let { msg ->
            AlertDialog(
                onDismissRequest = { mensajeDialogo = null },
                title = { Text("Información") },
                text = { Text(msg) },
                confirmButton = {
                    Button(onClick = { mensajeDialogo = null }) {
                        Text("Aceptar")
                    }
                }
            )
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
                Text("$${String.format("%.2f", balance)}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
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
    var itemAEditar by remember { mutableStateOf<Transaccion?>(null) }
    var itemAEliminar by remember { mutableStateOf<Transaccion?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var filtroFecha by remember { mutableStateOf("Todos") }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val listaOriginal: List<Transaccion> = if (esIngreso) ingresos else gastos
    val listaFiltrada = listaOriginal.filter { 
        (busqueda.isEmpty() || it.concepto.contains(busqueda, ignoreCase = true)) &&
        (filtroFecha == "Todos" || filtrarPorTiempo(it.fecha, filtroFecha))
    }

    val total = listaFiltrada.sumOf { it.cantidad }

    Column(modifier = Modifier.fillMaxSize().padding(15.dp)) {
        Text(if (esIngreso) "Ingreso Total: $${String.format("%.2f", total)}" else "Gasto Total: $${String.format("%.2f", total)}",
            fontSize = 24.sp, color = Color.White, modifier = Modifier.padding(bottom = 5.dp))

        // Buscador
        TextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Buscar por concepto...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        // Filtro de fecha
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterButton("Todos", filtroFecha == "Todos") { filtroFecha = "Todos" }
            FilterButton("Día", filtroFecha == "Día") { filtroFecha = "Día" }
            FilterButton("Semana", filtroFecha == "Semana") { filtroFecha = "Semana" }
            FilterButton("Mes", filtroFecha == "Mes") { filtroFecha = "Mes" }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(listaFiltrada.reversed()) { item ->
                val cantidadMostrada = if (esIngreso) item.cantidad else -item.cantidad
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { itemAEditar = item },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.concepto, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(sdf.format(item.fecha), color = Color.White.copy(0.6f), fontSize = 12.sp)
                        }
                        Text("$${String.format("%.2f", cantidadMostrada)}", color = if (cantidadMostrada > 0) Color.Green else Color.Red, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { itemAEliminar = item }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(0.7f))
                        }
                    }
                }
            }
        }

        Button(
            onClick = { mostrarForm = true },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Añadir ${if (esIngreso) "Ingreso" else "Gasto"}")
        }
    }

    if (mostrarForm) {
        FormularioDialog(esIngreso, onDismiss = { mostrarForm = false }) { concepto, monto, fecha ->
            if (esIngreso) db.insertarIngreso(concepto, monto, fecha)
            else db.insertarGasto(concepto, monto, fecha)
            onUpdate()
            mostrarForm = false
        }
    }

    itemAEditar?.let { item ->
        FormularioDialog(
            esIngreso = esIngreso,
            onDismiss = { itemAEditar = null },
            conceptoIni = item.concepto,
            montoIni = item.cantidad.toString(),
            fechaIni = item.fecha,
            esEdicion = true
        ) { concepto, monto, fecha ->
            if (esIngreso) db.actualizarIngreso(item.id, concepto, monto, fecha)
            else db.actualizarGasto(item.id, concepto, monto, fecha)
            onUpdate()
            itemAEditar = null
        }
    }

    itemAEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { itemAEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro?") },
            confirmButton = {
                TextButton(onClick = {
                    if (esIngreso) db.eliminarIngreso(item.id)
                    else db.eliminarGasto(item.id)
                    onUpdate()
                    itemAEliminar = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { itemAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun FilterButton(text: String, activo: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (activo) Color(0xFF4A4ED4) else Color.Gray.copy(0.3f)),
        modifier = Modifier.padding(2.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) { Text(text, fontSize = 12.sp, color = Color.White) }
}

@Composable
fun FormularioDialog(
    esIngreso: Boolean,
    onDismiss: () -> Unit,
    conceptoIni: String = "",
    montoIni: String = "",
    fechaIni: Date = Date(),
    esEdicion: Boolean = false,
    onSave: (String, Double, Date) -> Unit
) {
    var concepto by remember { mutableStateOf(conceptoIni) }
    var monto by remember { mutableStateOf(montoIni) }
    var fecha by remember { mutableStateOf(fechaIni) }
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = TarjetaInicio) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (esEdicion) "Editar ${if (esIngreso) "Ingreso" else "Gasto"}" 
                           else "Nuevo ${if (esIngreso) "Ingreso" else "Gasto"}",
                    fontSize = 20.sp, 
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(15.dp))
                
                TextField(
                    value = concepto, 
                    onValueChange = { concepto = it }, 
                    label = { Text("Concepto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                TextField(
                    value = monto,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            monto = input
                        }
                    },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { time = fecha }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCalendar = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                fecha = newCalendar.time
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Fecha: ${sdf.format(fecha)}", color = Color.White)
                }

                Spacer(modifier = Modifier.height(15.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(0.7f)) }
                    Button(onClick = {
                        val m = monto.toDoubleOrNull() ?: 0.0
                        onSave(concepto.ifEmpty { if (esIngreso) "Ingreso" else "Gasto" }, m, fecha)
                    }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

fun filtrarPorTiempo(fecha: Date, periodo: String): Boolean {
    val cal = Calendar.getInstance()
    val itemCal = Calendar.getInstance().apply { time = fecha }
    return when (periodo) {
        "Día" -> cal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) &&
                 cal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
        "Semana" -> cal.get(Calendar.WEEK_OF_YEAR) == itemCal.get(Calendar.WEEK_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
        "Mes" -> cal.get(Calendar.MONTH) == itemCal.get(Calendar.MONTH) &&
                 cal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
        else -> true
    }
}
