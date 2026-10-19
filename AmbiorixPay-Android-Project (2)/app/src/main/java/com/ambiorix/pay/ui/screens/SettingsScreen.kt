package com.ambiorix.pay.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ambiorix.pay.data.AppSettings
import com.ambiorix.pay.data.ImageStore
import com.ambiorix.pay.data.KnownApps
import com.ambiorix.pay.sound.SpanishNumbers
import com.ambiorix.pay.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen(
    settings: AppSettings,
    isSpanishVoiceAvailable: Boolean = true,
    onSettingsChanged: ((AppSettings) -> AppSettings) -> Unit,
    onBackClick: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenScreenPinningSettings: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    onOpenHistoryClick: () -> Unit,
    onTestPaymentClick: () -> Unit,
    onTestVoiceClick: () -> Unit
) {
    val context = LocalContext.current

    // Launchers para seleccionar imágenes desde la galería y guardarlas localmente
    val pickQr1Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStore.saveImageToInternalStorage(context, uri, "qr1.png")
            if (savedPath != null) {
                onSettingsChanged { it.copy(qr1Path = savedPath) }
            }
        }
    }

    val pickQr2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStore.saveImageToInternalStorage(context, uri, "qr2.png")
            if (savedPath != null) {
                onSettingsChanged { it.copy(qr2Path = savedPath) }
            }
        }
    }

    val pickLogoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStore.saveImageToInternalStorage(context, uri, "logo.png")
            if (savedPath != null) {
                onSettingsChanged { it.copy(logoPath = savedPath) }
            }
        }
    }

    // Estados para campos de texto
    var businessName by remember(settings.businessName) { mutableStateOf(settings.businessName) }
    var scanToPayText by remember(settings.scanToPayText) { mutableStateOf(settings.scanToPayText) }
    var phoneText by remember(settings.phoneText) { mutableStateOf(settings.phoneText) }
    var additionalText by remember(settings.additionalText) { mutableStateOf(settings.additionalText) }
    var customPackageInput by remember { mutableStateOf("") }

    // Diálogo para cambiar PIN
    var showPinChangeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuración",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // BOTÓN PROBAR PAGO ($35.000)
            Button(
                onClick = onTestPaymentClick,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Probar",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Probar pago ($35.000)",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ACCESO A NOTIFICACIONES DEL SISTEMA
            SettingsCard(title = "Permisos del Sistema") {
                Button(
                    onClick = onOpenNotificationSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = DarkBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Abrir Acceso a Notificaciones",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Obligatorio para que Ambiorix Pay pueda leer los pagos entrantes de las apps bancarias sin conectarse a internet.",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

            // CÓDIGOS QR Y LOGO
            SettingsCard(title = "Imágenes y Códigos QR") {
                // QR 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Código QR 1 (Principal)", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (settings.qr1Path != null) "Cargado en almacenamiento privado" else "No cargado",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { pickQr1Launcher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Subir QR 1", color = GoldPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Activar QR 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Habilitar segundo QR (QR 2)", color = TextWhite)
                    Switch(
                        checked = settings.qr2Enabled,
                        onCheckedChange = { isEnabled ->
                            onSettingsChanged { it.copy(qr2Enabled = isEnabled) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                    )
                }

                if (settings.qr2Enabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Código QR 2", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (settings.qr2Path != null) "Cargado" else "No cargado",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = { pickQr2Launcher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Subir QR 2", color = GoldPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Logo del negocio (opcional)", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (settings.logoPath != null) "Logo personalizado activo" else "Se muestra 'AMBIORIX'",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { pickLogoLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Subir Logo", color = GoldPrimary)
                    }
                }
            }

            // DATOS DEL NEGOCIO Y TEXTOS
            SettingsCard(title = "Textos de la Pantalla Kiosco") {
                OutlinedTextField(
                    value = businessName,
                    onValueChange = {
                        businessName = it
                        onSettingsChanged { s -> s.copy(businessName = it) }
                    },
                    label = { Text("Nombre del Negocio") },
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = scanToPayText,
                    onValueChange = {
                        scanToPayText = it
                        onSettingsChanged { s -> s.copy(scanToPayText = it) }
                    },
                    label = { Text("Texto superior de acción") },
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = {
                        phoneText = it
                        onSettingsChanged { s -> s.copy(phoneText = it) }
                    },
                    label = { Text("Teléfono de transferencias (opcional)") },
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = additionalText,
                    onValueChange = {
                        additionalText = it
                        onSettingsChanged { s -> s.copy(additionalText = it) }
                    },
                    label = { Text("Texto adicional de ayuda") },
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // APPS DE PAGO A ESCUCHAR (PACKAGE NAMES)
            SettingsCard(title = "Apps de Pago Autorizadas") {
                Text(
                    text = "Solo se procesarán notificaciones de los paquetes marcados. Las demás apps son ignoradas sin leer su contenido.",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                for (app in KnownApps.DEFAULT_APPS) {
                    val isChecked = settings.enabledPackages.contains(app.primaryPackage)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                onSettingsChanged { current ->
                                    val newSet = current.enabledPackages.toMutableSet()
                                    if (checked) {
                                        newSet.add(app.primaryPackage)
                                        newSet.addAll(app.alternativePackages)
                                    } else {
                                        newSet.remove(app.primaryPackage)
                                        newSet.removeAll(app.alternativePackages.toSet())
                                    }
                                    current.copy(enabledPackages = newSet)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = app.name, color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text(text = app.primaryPackage, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                // Paquetes personalizados agregados
                if (settings.customPackages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paquetes personalizados:", color = GoldPrimary, fontSize = 13.sp)
                    for (pkg in settings.customPackages) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = pkg, color = TextWhite, fontSize = 13.sp)
                            IconButton(
                                onClick = {
                                    onSettingsChanged { s ->
                                        s.copy(customPackages = s.customPackages - pkg)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = DangerRed)
                            }
                        }
                    }
                }

                // Agregar nuevo package name
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customPackageInput,
                        onValueChange = { customPackageInput = it },
                        label = { Text("Agregar package name") },
                        placeholder = { Text("ej: com.mibanco.app") },
                        colors = outlinedFieldColors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customPackageInput.isNotBlank()) {
                                val pkgToAdd = customPackageInput.trim()
                                onSettingsChanged { s ->
                                    s.copy(customPackages = s.customPackages + pkgToAdd)
                                }
                                customPackageInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Agregar", color = GoldPrimary)
                    }
                }
            }

            // AUDIO Y VOZ DE CONFIRMACIÓN (DOS OPCIONES INDEPENDIENTES)
            SettingsCard(title = "Sonido y Voz de Confirmación") {
                // Aviso si no hay voz en español instalada
                if (!isSpanishVoiceAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DangerRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Voz en español no disponible en el sistema",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "El motor de voz del teléfono necesita descargar los datos en español para hablar.",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onOpenTtsSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Abrir Ajustes de Voz (TTS) del Sistema", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 1) Tono de confirmación (Chime)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Tono de confirmación (Chime)", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Sonido de timbre instantáneo", color = TextGray, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { isEnabled ->
                            onSettingsChanged { it.copy(soundEnabled = isEnabled) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2) Opción 1: Voz de pago
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Voz de pago", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Dice: \"Pago recibido, <monto en palabras> pesos\"", color = TextGray, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = settings.ttsEnabled,
                        onCheckedChange = { isEnabled ->
                            onSettingsChanged { it.copy(ttsEnabled = isEnabled) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3) Opción 2: Agregar "Gracias por tu compra" (Solo tiene efecto si la voz de pago está activa)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Agregar \"Gracias por tu compra\"",
                            color = if (settings.ttsEnabled) TextWhite else TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (settings.ttsEnabled) "Añade esa frase al final del anuncio de voz" else "Requiere que la voz de pago esté encendida",
                            color = if (settings.ttsEnabled) TextGray else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.includeThankYou,
                        enabled = settings.ttsEnabled,
                        onCheckedChange = { isEnabled ->
                            onSettingsChanged { it.copy(includeThankYou = isEnabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldPrimary,
                            disabledCheckedThumbColor = GoldPrimary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Muestra la frase exacta de previsualización
                val previewPhrase = remember(settings.includeThankYou) {
                    SpanishNumbers.buildPaymentAnnouncementPhrase(35000L, settings.includeThankYou)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Frase hablada: \"$previewPhrase\"",
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón "Probar voz"
                OutlinedButton(
                    onClick = onTestVoiceClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Probar voz ($35.000)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // DURACIÓN DE PANTALLA VERDE
            SettingsCard(title = "Duración de Pantalla de Confirmación") {
                Text(
                    text = "${settings.confirmationDurationSeconds} segundos",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Slider(
                    value = settings.confirmationDurationSeconds.toFloat(),
                    onValueChange = { newValue ->
                        onSettingsChanged { it.copy(confirmationDurationSeconds = newValue.toInt()) }
                    },
                    valueRange = 3f..30f,
                    steps = 27,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary,
                        inactiveTrackColor = DarkSurfaceElevated
                    )
                )
                Text(
                    text = "Rango de 3 a 30 segundos. Tras este lapso regresa a la pantalla de códigos QR.",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

            // FIJAR PANTALLA (SCREEN PINNING)
            SettingsCard(title = "Modo Kiosco y Fijar Pantalla") {
                Text(
                    text = "Para evitar que clientes o empleados salgan de la app, activa 'Fijar pantalla' (Screen Pinning) en los ajustes del sistema Android.",
                    color = TextGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenScreenPinningSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Ajustes de Seguridad", color = GoldPrimary)
                }
            }

            // HISTORIAL Y SEGURIDAD (CAMBIO DE PIN)
            SettingsCard(title = "Historial y PIN") {
                Button(
                    onClick = onOpenHistoryClick,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Historial de Pagos", color = GoldPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showPinChangeDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = TextWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cambiar PIN de Acceso", color = TextWhite)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Diálogo para cambiar PIN
        if (showPinChangeDialog) {
            ChangePinDialog(
                currentPin = settings.pinCode,
                onDismiss = { showPinChangeDialog = false },
                onSavePin = { newPin ->
                    onSettingsChanged { it.copy(pinCode = newPin) }
                    showPinChangeDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = {
                Text(
                    text = title,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                content()
            }
        )
    }
}

@Composable
private fun ChangePinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Cambiar PIN de Seguridad", color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPinInput,
                    onValueChange = { oldPinInput = it },
                    label = { Text("PIN actual") },
                    colors = outlinedFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { newPinInput = it },
                    label = { Text("Nuevo PIN (mínimo 4 dígitos)") },
                    colors = outlinedFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPinInput,
                    onValueChange = { confirmPinInput = it },
                    label = { Text("Confirmar nuevo PIN") },
                    colors = outlinedFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = DangerRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPinInput != currentPin) {
                        errorMessage = "El PIN actual no coincide"
                    } else if (newPinInput.length < 4) {
                        errorMessage = "El nuevo PIN debe tener mínimo 4 dígitos"
                    } else if (newPinInput != confirmPinInput) {
                        errorMessage = "La confirmación no coincide"
                    } else {
                        onSavePin(newPinInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("Guardar PIN", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextWhite)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = GoldPrimary,
    unfocusedLabelColor = TextGray,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    cursorColor = GoldPrimary
)
