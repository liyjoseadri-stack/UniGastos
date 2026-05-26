package com.example.unigastos

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unigastos.ui.theme.UniGastosTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniGastosTheme {
                val navController = rememberNavController()
                val db = SQLiteManager.getInstance(LocalContext.current)
                val firestore = remember { FirestoreManager.getInstance() }

                NavHost(navController = navController, startDestination = "login") {
                    composable("login") { LoginScreen(navController, db, firestore) }
                    composable("register") { RegisterScreen(navController, db, firestore) }
                    composable(
                        "home/{usuario}/{soloLectura}",
                        arguments = listOf(
                            navArgument("usuario") { type = NavType.StringType },
                            navArgument("soloLectura") { type = NavType.BoolType }
                        )
                    ) { backStackEntry ->
                        val user = Uri.decode(backStackEntry.arguments?.getString("usuario") ?: "")
                        val readOnly = backStackEntry.arguments?.getBoolean("soloLectura") ?: false
                        HomeScreen(user, navController, db, firestore, readOnly)
                    }
                }
            }
        }
    }
}

val LISTA_CATEGORIAS_GASTOS = listOf("Comida", "Transporte", "Renta", "Material escolar", "Servicios", "Salud", "Entretenimiento", "Otros")
val LISTA_CATEGORIAS_INGRESOS = listOf("Sueldo", "Beca", "Apoyo", "Otros")

val FondoInicio = Color(0xFF1C133F)
val FondoFin = Color(0xFF65909D)
val TarjetaInicio = Color(0xFF282A5C)
val TarjetaFin = Color(0xFF3A4178)
val HeaderColor = Color(0xFF1E0F48)

fun getIconForCategory(categoria: String, esIngreso: Boolean): ImageVector {
    return if (esIngreso) {
        when (categoria) {
            "Sueldo" -> Icons.Rounded.Payments
            "Beca" -> Icons.Rounded.School
            "Apoyo" -> Icons.Rounded.VolunteerActivism
            else -> Icons.Rounded.AddCard
        }
    } else {
        when (categoria) {
            "Comida" -> Icons.Rounded.Restaurant
            "Transporte" -> Icons.Rounded.DirectionsCar
            "Renta" -> Icons.Rounded.Home
            "Material escolar" -> Icons.Rounded.MenuBook
            "Servicios" -> Icons.Rounded.Bolt
            "Salud" -> Icons.Rounded.MedicalServices
            "Entretenimiento" -> Icons.Rounded.SportsEsports
            else -> Icons.Rounded.Category
        }
    }
}

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

@Composable
fun LoginScreen(navController: NavHostController, db: SQLiteManager, firestore: FirestoreManager) {
    var loginUsuario by remember { mutableStateOf("") }
    var loginContrasena by remember { mutableStateOf("") }
    var mostrarLoginContrasena by remember { mutableStateOf(false) }
    var mensajeDialogo by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val info = db.obtenerUsuarioActivoInfo()
        if (info != null) {
            val isReadOnly = info.rol == "TUTOR"
            navController.navigate("home/${Uri.encode(info.nombre)}/$isReadOnly")
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
            Text(
                "UniGastos",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(listOf(Color.White, Color.Cyan))
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(TarjetaInicio, TarjetaFin)), RoundedCornerShape(35.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Iniciar Sesion", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                    placeholder = { Text("Contrasena", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (mostrarLoginContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { mostrarLoginContrasena = !mostrarLoginContrasena }) {
                            Icon(
                                imageVector = if (mostrarLoginContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (mostrarLoginContrasena) "Ocultar contrasena" else "Ver contrasena"
                            )
                        }
                    },
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
                            mensajeDialogo = "Ambos campos son obligatorios para iniciar sesion."
                        } else {
                            cargando = true
                            scope.launch {
                                try {
                                    val info = firestore.validarCredenciales(u, p)
                                    if (info != null) {
                                        db.guardarUsuario(info.nombre, p, info.rol, info.vinculadoA)
                                        db.iniciarSesion(info.nombre)
                                        if (info.rol != "TUTOR") {
                                            firestore.subirDatosLocales(
                                                info.nombre,
                                                db.obtenerIngresosDeUsuario(info.nombre),
                                                db.obtenerGastosDeUsuario(info.nombre)
                                            )
                                        }
                                        val isReadOnly = info.rol == "TUTOR"
                                        navController.navigate("home/${Uri.encode(info.nombre)}/$isReadOnly")
                                    } else {
                                        mensajeDialogo = "Credenciales incorrectas. Verifica usuario y contrasena."
                                    }
                                } catch (_: Exception) {
                                    val infoLocal = db.obtenerInfoUsuario(u, p)
                                    if (infoLocal != null) {
                                        db.iniciarSesion(u)
                                        val isReadOnly = infoLocal.rol == "TUTOR"
                                        navController.navigate("home/${Uri.encode(infoLocal.nombre)}/$isReadOnly")
                                    } else {
                                        mensajeDialogo = "No se pudo conectar con Firestore y no hay una sesion local valida."
                                    }
                                } finally {
                                    cargando = false
                                }
                            }
                        }
                    },
                    enabled = !cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Entrar", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("No tienes cuenta?", color = Color.White.copy(0.8f))
                    TextButton(onClick = { navController.navigate("register") }) {
                        Text("Registrate", color = Color.Cyan)
                    }
                }
            }
        }

        mensajeDialogo?.let { msg ->
            AlertDialog(
                onDismissRequest = { mensajeDialogo = null },
                title = { Text("Informacion") },
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
fun RegisterScreen(navController: NavHostController, db: SQLiteManager, firestore: FirestoreManager) {
    var registroUsuario by remember { mutableStateOf("") }
    var registroContrasena by remember { mutableStateOf("") }
    var nombreTutor by remember { mutableStateOf("") }
    var contrasenaTutor by remember { mutableStateOf("") }
    var mostrarRegistroContrasena by remember { mutableStateOf(false) }
    var mostrarTutorContrasena by remember { mutableStateOf(false) }
    var mensajeDialogo by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    GradientBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
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
                    Text("Tus Datos", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = registroUsuario,
                        onValueChange = { registroUsuario = it.trim() },
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
                        value = registroContrasena,
                        onValueChange = { registroContrasena = it },
                        placeholder = { Text("Contrasena", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (mostrarRegistroContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { mostrarRegistroContrasena = !mostrarRegistroContrasena }) {
                                Icon(
                                    imageVector = if (mostrarRegistroContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (mostrarRegistroContrasena) "Ocultar contrasena" else "Ver contrasena"
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Datos del Tutor (Revisor)", fontSize = 22.sp, color = Color.Cyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = nombreTutor,
                        onValueChange = { nombreTutor = it.trim() },
                        placeholder = { Text("Nombre del Tutor", color = Color.Gray) },
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
                        value = contrasenaTutor,
                        onValueChange = { contrasenaTutor = it },
                        placeholder = { Text("Contrasena del Tutor", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (mostrarTutorContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { mostrarTutorContrasena = !mostrarTutorContrasena }) {
                                Icon(
                                    imageVector = if (mostrarTutorContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (mostrarTutorContrasena) "Ocultar contrasena" else "Ver contrasena"
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Button(
                        onClick = {
                            val u = registroUsuario.trim()
                            val p = registroContrasena.trim()
                            val tU = nombreTutor.trim()
                            val tP = contrasenaTutor.trim()

                            when {
                                u.isEmpty() || p.isEmpty() || tU.isEmpty() || tP.isEmpty() -> {
                                    mensajeDialogo = "Todos los campos son obligatorios."
                                }
                                u == tU -> {
                                    mensajeDialogo = "El alumno y el tutor deben tener usuarios diferentes."
                                }
                                else -> {
                                    cargando = true
                                    scope.launch {
                                        try {
                                            val ok = firestore.registrarAlumnoYTutor(u, p, tU, tP)
                                            if (ok) {
                                                db.guardarUsuario(u, p, "USUARIO", u)
                                                db.guardarUsuario(tU, tP, "TUTOR", u)
                                                db.iniciarSesion(u)
                                                navController.navigate("home/${Uri.encode(u)}/false")
                                            } else {
                                                mensajeDialogo = "El usuario del alumno o tutor ya existe."
                                            }
                                        } catch (e: Exception) {
                                            mensajeDialogo = mensajeFirestore(e)
                                        } finally {
                                            cargando = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Registrar Ambos", fontSize = 18.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { navController.navigate("login") }) {
                        Text("Volver al Login", color = Color.Cyan)
                    }
                }
            }
        }

        mensajeDialogo?.let { msg ->
            AlertDialog(
                onDismissRequest = { mensajeDialogo = null },
                title = { Text("Informacion") },
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
fun HomeScreen(nombre: String, navController: NavHostController, db: SQLiteManager, firestore: FirestoreManager, soloLectura: Boolean) {
    val info = remember(nombre) { db.obtenerInfoUsuario(nombre) }
    val usuarioDatos = info?.vinculadoA ?: nombre
    var vistaActiva by remember { mutableStateOf("Resumen") }
    var ingresos by remember(usuarioDatos) { mutableStateOf(db.obtenerIngresosDeUsuario(usuarioDatos)) }
    var gastos by remember(usuarioDatos) { mutableStateOf(db.obtenerGastosDeUsuario(usuarioDatos)) }
    var mensajeDialogo by remember { mutableStateOf<String?>(null) }

    BackHandler {
        db.cerrarSesion()
        navController.popBackStack()
    }

    DisposableEffect(usuarioDatos) {
        val ingresosListener = firestore.observarIngresos(
            usuario = usuarioDatos,
            onChange = {
                ingresos = it
                db.guardarIngresosDesdeServidor(usuarioDatos, it)
            },
            onError = { mensajeDialogo = mensajeFirestore(it) }
        )
        val gastosListener = firestore.observarGastos(
            usuario = usuarioDatos,
            onChange = {
                gastos = it
                db.guardarGastosDesdeServidor(usuarioDatos, it)
            },
            onError = { mensajeDialogo = mensajeFirestore(it) }
        )
        onDispose {
            ingresosListener.remove()
            gastosListener.remove()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FondoInicio, FondoFin)))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(HeaderColor).padding(20.dp).padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Hola, $nombre", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (soloLectura) {
                    Text("Modo: Tutor (Solo Lectura)", color = Color.Cyan, fontSize = 14.sp)
                    Text("Viendo a: $usuarioDatos", color = Color.White.copy(0.75f), fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                db.cerrarSesion()
                navController.popBackStack()
            }) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF221852)).padding(10.dp)) {
            TabItem("Ingresos", vistaActiva == "Ingresos") { vistaActiva = "Ingresos" }
            TabItem("Resumen", vistaActiva == "Resumen") { vistaActiva = "Resumen" }
            TabItem("Gastos", vistaActiva == "Gastos") { vistaActiva = "Gastos" }
        }

        AnimatedContent(
            targetState = vistaActiva,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith
                        fadeOut(animationSpec = tween(90))
            },
            label = "TabTransition",
            modifier = Modifier.weight(1f)
        ) { targetVista ->
            when (targetVista) {
                "Resumen" -> ResumenView(usuarioDatos, ingresos, gastos, soloLectura)
                "Ingresos" -> TransaccionesView(true, ingresos, emptyList(), db, firestore, usuarioDatos, soloLectura) {
                    ingresos = db.obtenerIngresosDeUsuario(usuarioDatos)
                }
                "Gastos" -> TransaccionesView(false, emptyList(), gastos, db, firestore, usuarioDatos, soloLectura) {
                    gastos = db.obtenerGastosDeUsuario(usuarioDatos)
                }
            }
        }
    }

    mensajeDialogo?.let { msg ->
        AlertDialog(
            onDismissRequest = { mensajeDialogo = null },
            title = { Text("Firestore") },
            text = { Text(msg) },
            confirmButton = { Button(onClick = { mensajeDialogo = null }) { Text("Aceptar") } }
        )
    }
}

@Composable
fun RowScope.TabItem(text: String, activo: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (activo) Color.White else Color.White.copy(0.5f))
    val ancho by animateDpAsState(if (activo) 40.dp else 0.dp)

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            color = color,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(ancho)
                .background(Color.Cyan, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun ResumenView(usuario: String, ingresos: List<Ingreso>, gastos: List<Gasto>, soloLectura: Boolean) {
    var filtro by remember { mutableStateOf("Mes") }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("UniGastosPrefs", android.content.Context.MODE_PRIVATE) }
    val budgetKey = "presupuesto_$usuario"
    var presupuestoInput by remember { mutableStateOf(prefs.getString(budgetKey, "") ?: "") }
    val presupuesto = presupuestoInput.toDoubleOrNull() ?: 0.0

    val ingresosF = ingresos.filter { filtrarPorTiempo(it.fecha, filtro) }
    val gastosF = gastos.filter { filtrarPorTiempo(it.fecha, filtro) }
    val totalI = ingresosF.sumOf { it.cantidad }
    val totalG = gastosF.sumOf { it.cantidad }
    val balance = totalI - totalG
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tarjeta de Saldo con Animacion
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(1000)) + expandVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1254)),
                shape = RoundedCornerShape(35.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(25.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saldo Actual:", color = Color.White.copy(0.7f), fontSize = 16.sp)
                    Text(
                        "\$${String.format("%.2f", balance)}",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Presupuesto
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SettingsSuggest, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Presupuesto Mensual", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White.copy(0.4f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = presupuestoInput,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                presupuestoInput = it
                                prefs.edit().putString(budgetKey, it).apply()
                            }
                        },
                        enabled = !soloLectura,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Definir meta de ahorro", color = Color.Gray, fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedIndicatorColor = Color.Cyan.copy(0.5f)
                        )
                    )
                }

                if (presupuesto > 0) {
                    val progreso = (totalG / presupuesto).coerceIn(0.0, 1.0)
                    Column {
                        LinearProgressIndicator(
                            progress = { progreso.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = if (totalG > presupuesto) Color(0xFFFF5252) else Color(0xFF4CAF50),
                            trackColor = Color.White.copy(0.1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (totalG > presupuesto) "Excedido" else "Progreso: ${(progreso * 100).toInt()}%",
                                color = Color.White.copy(0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                "\$${String.format("%.0f", totalG)} / \$${String.format("%.0f", presupuesto)}",
                                color = Color.White.copy(0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (totalG > presupuesto) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFFCDD2), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Has excedido tu presupuesto por \$${String.format("%.2f", totalG - presupuesto)}",
                                    color = Color(0xFFFFCDD2),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Donut Chart mejorado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF34396E), Color(0xFF425375))), RoundedCornerShape(35.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (totalI + totalG > 0) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val sweepI = (totalI / (totalI + totalG) * 360).toFloat()
                    val strokeWidth = 35.dp.toPx()
                    // Donut background
                    drawCircle(color = Color.White.copy(0.05f), radius = size.minDimension / 2, style = Stroke(strokeWidth))
                    // Slices
                    drawArc(Color(0xFF4D86FF), -90f, sweepI, false, style = Stroke(strokeWidth))
                    drawArc(Color(0xFFE91E63), -90f + sweepI, 360f - sweepI, false, style = Stroke(strokeWidth))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gastos vs", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    Text("Ingresos", color = Color.White.copy(0.6f), fontSize = 12.sp)
                }
            } else {
                Text("Sin movimientos", color = Color.White.copy(0.4f))
            }
        }

        Row(modifier = Modifier.padding(top = 20.dp)) {
            FilterButton("Dia", filtro == "Dia") { filtro = "Dia" }
            FilterButton("Semana", filtro == "Semana") { filtro = "Semana" }
            FilterButton("Mes", filtro == "Mes") { filtro = "Mes" }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            "Gastos por Categoria",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 5.dp)
        )
        Text(
            "Periodo seleccionado: $filtro",
            color = Color.Cyan.copy(0.7f),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 5.dp, bottom = 12.dp)
        )

        val gastosAgrupados = gastosF.groupBy { it.concepto }
        if (gastosAgrupados.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Inbox, contentDescription = null, tint = Color.White.copy(0.2f), modifier = Modifier.size(64.dp))
                Text("No hay registros en este periodo", color = Color.White.copy(0.4f), fontSize = 14.sp)
            }
        } else {
            gastosAgrupados.forEach { (categoria, lista) ->
                val suma = lista.sumOf { it.cantidad }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(Color.White.copy(0.06f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color.White.copy(0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(getIconForCategory(categoria, false), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(categoria, color = Color.White.copy(0.9f), fontWeight = FontWeight.Medium)
                    }
                    Text("\$${String.format("%.2f", suma)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TransaccionesView(
    esIngreso: Boolean,
    ingresos: List<Ingreso>,
    gastos: List<Gasto>,
    db: SQLiteManager,
    firestore: FirestoreManager,
    usuarioDatos: String,
    soloLectura: Boolean,
    onUpdate: () -> Unit
) {
    var mostrarForm by remember { mutableStateOf(false) }
    var itemAEditar by remember { mutableStateOf<Transaccion?>(null) }
    var itemAEliminar by remember { mutableStateOf<Transaccion?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var filtroFecha by remember { mutableStateOf("Todos") }
    var filtroCat by remember { mutableStateOf("Todas") }
    var mensajeDialogo by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val listaOriginal: List<Transaccion> = if (esIngreso) ingresos else gastos
    val categoriasDisponibles = listOf("Todas") + (if (esIngreso) LISTA_CATEGORIAS_INGRESOS else LISTA_CATEGORIAS_GASTOS)
    val listaFiltrada = listaOriginal.filter {
        (busqueda.isEmpty() || it.concepto.contains(busqueda, ignoreCase = true)) &&
            (filtroFecha == "Todos" || filtrarPorTiempo(it.fecha, filtroFecha)) &&
            (filtroCat == "Todas" || it.concepto == filtroCat)
    }
    val total = listaFiltrada.sumOf { it.cantidad }

    Column(modifier = Modifier.fillMaxSize().padding(15.dp)) {
        Text(
            if (esIngreso) "Ingreso Total: \$${String.format("%.2f", total)}" else "Gasto Total: \$${String.format("%.2f", total)}",
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 5.dp)
        )

        if (soloLectura) {
            Text("Modo tutor: solo lectura", color = Color.White.copy(0.75f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        TextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Buscar...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterButton("Todos", filtroFecha == "Todos") { filtroFecha = "Todos" }
            FilterButton("Dia", filtroFecha == "Dia") { filtroFecha = "Dia" }
            FilterButton("Semana", filtroFecha == "Semana") { filtroFecha = "Semana" }
            FilterButton("Mes", filtroFecha == "Mes") { filtroFecha = "Mes" }
        }

        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            items(categoriasDisponibles) { cat ->
                FilterButton(cat, filtroCat == cat) { filtroCat = cat }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(listaFiltrada.reversed()) { item ->
                val cantidadMostrada = if (esIngreso) item.cantidad else -item.cantidad

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(enabled = !soloLectura) { itemAEditar = item },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).background(Color.White.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getIconForCategory(item.concepto, esIngreso),
                                    contentDescription = null,
                                    tint = if (esIngreso) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.concepto, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(sdf.format(item.fecha), color = Color.White.copy(0.5f), fontSize = 12.sp)
                            }
                            Text(
                                "\$${String.format("%.2f", cantidadMostrada)}",
                                color = if (cantidadMostrada > 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )

                            if (!soloLectura) {
                                IconButton(onClick = { itemAEliminar = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(0.5f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!soloLectura) {
            Button(
                onClick = { mostrarForm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Anadir ${if (esIngreso) "Ingreso" else "Gasto"}")
            }
        }
    }

    if (mostrarForm) {
        FormularioDialog(esIngreso, onDismiss = { mostrarForm = false }) { concepto, monto, fecha ->
            if (esIngreso) {
                val id = db.insertarIngreso(concepto, monto, fecha)
                onUpdate()
                if (id.isNotEmpty()) {
                    scope.launch {
                        try {
                            firestore.guardarIngreso(usuarioDatos, Ingreso(id, fecha, concepto, monto))
                        } catch (e: Exception) {
                            mensajeDialogo = mensajeFirestore(e)
                        }
                    }
                }
            } else {
                val id = db.insertarGasto(concepto, monto, fecha)
                onUpdate()
                if (id.isNotEmpty()) {
                    scope.launch {
                        try {
                            firestore.guardarGasto(usuarioDatos, Gasto(id, fecha, concepto, monto))
                        } catch (e: Exception) {
                            mensajeDialogo = mensajeFirestore(e)
                        }
                    }
                }
            }
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
            if (esIngreso) {
                db.actualizarIngreso(item.id, concepto, monto, fecha)
                onUpdate()
                scope.launch {
                    try {
                        firestore.guardarIngreso(usuarioDatos, Ingreso(item.id, fecha, concepto, monto))
                    } catch (e: Exception) {
                        mensajeDialogo = mensajeFirestore(e)
                    }
                }
            } else {
                db.actualizarGasto(item.id, concepto, monto, fecha)
                onUpdate()
                scope.launch {
                    try {
                        firestore.guardarGasto(usuarioDatos, Gasto(item.id, fecha, concepto, monto))
                    } catch (e: Exception) {
                        mensajeDialogo = mensajeFirestore(e)
                    }
                }
            }
            itemAEditar = null
        }
    }

    itemAEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { itemAEliminar = null },
            title = { Text("Confirmar eliminacion") },
            text = { Text("Seguro que deseas eliminar este registro?") },
            confirmButton = {
                TextButton(onClick = {
                    if (esIngreso) {
                        db.eliminarIngreso(item.id)
                        onUpdate()
                        scope.launch {
                            try {
                                firestore.eliminarIngreso(usuarioDatos, item.id)
                            } catch (e: Exception) {
                                mensajeDialogo = mensajeFirestore(e)
                            }
                        }
                    } else {
                        db.eliminarGasto(item.id)
                        onUpdate()
                        scope.launch {
                            try {
                                firestore.eliminarGasto(usuarioDatos, item.id)
                            } catch (e: Exception) {
                                mensajeDialogo = mensajeFirestore(e)
                            }
                        }
                    }
                    itemAEliminar = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { itemAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    mensajeDialogo?.let { msg ->
        AlertDialog(
            onDismissRequest = { mensajeDialogo = null },
            title = { Text("Firestore") },
            text = { Text(msg) },
            confirmButton = { Button(onClick = { mensajeDialogo = null }) { Text("Aceptar") } }
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
    var expanded by remember { mutableStateOf(false) }
    val categorias = if (esIngreso) LISTA_CATEGORIAS_INGRESOS else LISTA_CATEGORIAS_GASTOS

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

                Box {
                    OutlinedTextField(
                        value = concepto,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Categoria", color = Color.Cyan) },
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Cyan)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Cyan,
                            unfocusedBorderColor = Color.White.copy(0.5f)
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f).background(TarjetaFin)
                    ) {
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    concepto = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

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
        "Dia" -> cal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) &&
            cal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
        "Semana" -> cal.get(Calendar.WEEK_OF_YEAR) == itemCal.get(Calendar.WEEK_OF_YEAR) &&
            cal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
        "Mes" -> cal.get(Calendar.MONTH) == itemCal.get(Calendar.MONTH) &&
            cal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
        else -> true
    }
}
