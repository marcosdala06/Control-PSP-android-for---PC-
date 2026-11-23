package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PadPhoneServerScript
import com.example.model.ConnectionState
import com.example.model.DiagnosticEvent
import com.example.model.LrMode
import com.example.ui.GamepadUiState
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeckBezel
import com.example.ui.theme.DeckObsidian
import com.example.ui.theme.DeckSurface
import com.example.ui.theme.DeckSurfaceElevated
import com.example.ui.theme.DeckWell
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ConnectionAndScriptDialog(
    uiState: GamepadUiState,
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onDiscover: (Int) -> Unit,
    onSelectLrMode: (LrMode) -> Unit,
    onRunLrTest: () -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetThumbAssist: (Boolean) -> Unit,
    onSetShowSprintLock: (Boolean) -> Unit,
    onSetDeadzone: (Float) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = DeckSurface,
            border = BorderStroke(1.dp, DeckBezel),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "PadPhone • Configuração",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Ligação sem fios, L/R e servidor Linux v3",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Fechar",
                            tint = TextPrimary,
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DeckSurfaceElevated,
                    contentColor = CyanPulse,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Ligação & L/R") },
                        icon = { Icon(Icons.Rounded.Computer, contentDescription = null) },
                        modifier = Modifier.testTag("tab_connection"),
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Controlos") },
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                        modifier = Modifier.testTag("tab_controls"),
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Script PC v3") },
                        icon = { Icon(Icons.Rounded.Code, contentDescription = null) },
                        modifier = Modifier.testTag("tab_script"),
                    )
                }

                when (selectedTab) {
                    0 -> ConnectionTabContent(
                        uiState = uiState,
                        onConnect = onConnect,
                        onDisconnect = onDisconnect,
                        onDiscover = onDiscover,
                        onSelectLrMode = onSelectLrMode,
                        onRunLrTest = onRunLrTest,
                    )
                    1 -> ControlsTabContent(
                        uiState = uiState,
                        onSetHaptics = onSetHaptics,
                        onSetThumbAssist = onSetThumbAssist,
                        onSetShowSprintLock = onSetShowSprintLock,
                        onSetDeadzone = onSetDeadzone,
                    )
                    else -> ScriptTabContent()
                }
            }
        }
    }
}

@Composable
private fun ConnectionTabContent(
    uiState: GamepadUiState,
    onConnect: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onDiscover: (Int) -> Unit,
    onSelectLrMode: (LrMode) -> Unit,
    onRunLrTest: () -> Unit,
) {
    var hostInput by rememberSaveable(uiState.settings.host) {
        mutableStateOf(uiState.settings.host)
    }
    var portInput by rememberSaveable(uiState.settings.port) {
        mutableStateOf(uiState.settings.port.toString())
    }
    val parsedPort = portInput.toIntOrNull()?.coerceIn(1, 65535) ?: 8000
    val isConnectedOrActive = uiState.connectionState is ConnectionState.Connected ||
        uiState.connectionState is ConnectionState.Connecting ||
        uiState.connectionState is ConnectionState.Reconnecting

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ConnectionStatusBanner(uiState = uiState)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("IP do PC Linux") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ip_input"),
                )
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Porta") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(104.dp)
                        .testTag("port_input"),
                )
            }
        }

        if (uiState.settings.savedHosts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.settings.savedHosts.forEach { saved ->
                        Surface(
                            onClick = { hostInput = saved },
                            shape = RoundedCornerShape(50),
                            color = DeckSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (saved == hostInput) CyanPulse else DeckBezel,
                            ),
                        ) {
                            Text(
                                text = saved,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (saved == hostInput) CyanPulse else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onConnect(hostInput, parsedPort) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPulse,
                        contentColor = DeckObsidian,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("connect_button"),
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isConnectedOrActive) "Religar" else "Ligar ao PC")
                }

                if (isConnectedOrActive) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        border = BorderStroke(1.dp, CrimsonAlert),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("disconnect_button"),
                    ) {
                        Icon(Icons.Rounded.WifiOff, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Desligar")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { onDiscover(parsedPort) },
                enabled = !uiState.isDiscovering,
                border = BorderStroke(1.dp, DeckBezel),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("discover_button"),
            ) {
                if (uiState.isDiscovering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CyanPulse,
                    )
                } else {
                    Icon(Icons.Rounded.Radar, contentDescription = null, tint = CyanPulse)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (uiState.isDiscovering) "A procurar na rede Wi-Fi…" else "Procurar PC Automaticamente (Wi-Fi)",
                    color = TextPrimary,
                )
            }
            uiState.discoveryMessage?.let { msg ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        items(uiState.discoveredServers) { srv ->
            Surface(
                onClick = {
                    hostInput = srv.host
                    portInput = srv.port.toString()
                    onConnect(srv.host, srv.port)
                },
                shape = RoundedCornerShape(14.dp),
                color = DeckSurfaceElevated,
                border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "${srv.name} (${srv.host}:${srv.port})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = srv.backendInfo,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldLive,
                        )
                    }
                    Text(
                        text = "Ligar",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanPulse,
                    )
                }
            }
        }

        item {
            HorizontalDivider(color = DeckBezel.copy(alpha = 0.6f))
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Modo de Envio L / R (Trocar Jogador & Correr Rápido)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Se o L ou R não responder no PPSSPP, use 'Ambos' ou 'Híbrido (+Q/W)' com o Script v3.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        items(LrMode.entries) { mode ->
            val selected = mode == uiState.settings.lrMode
            Surface(
                onClick = { onSelectLrMode(mode) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) CyanPulse.copy(alpha = 0.14f) else DeckSurfaceElevated,
                border = BorderStroke(1.2.dp, if (selected) CyanPulse else DeckBezel),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lr_mode_${mode.wireValue}"),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (selected) CyanPulse else TextMuted.copy(alpha = 0.35f),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.shortLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) CyanPulse else TextPrimary,
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = DeckBezel.copy(alpha = 0.6f))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Diagnóstico L / R",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Pulsa L e R e mostra a resposta real do PC.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Button(
                    onClick = onRunLrTest,
                    enabled = !uiState.isTestingLr,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldLive,
                        contentColor = DeckObsidian,
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("test_lr_button"),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (uiState.isTestingLr) "A testar…" else "Testar L/R")
                }
            }

            if (uiState.diagnosticEvents.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeckWell)
                        .border(1.dp, DeckBezel, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    uiState.diagnosticEvents.take(8).forEach { ev ->
                        val prefix = when (ev.direction) {
                            DiagnosticEvent.Direction.OUT -> "APP ->"
                            DiagnosticEvent.Direction.IN -> "PC  <-"
                            DiagnosticEvent.Direction.SYS -> "SYS ::"
                        }
                        val color = when (ev.direction) {
                            DiagnosticEvent.Direction.OUT -> CyanPulse
                            DiagnosticEvent.Direction.IN -> EmeraldLive
                            DiagnosticEvent.Direction.SYS -> TextSecondary
                        }
                        Text(
                            text = "$prefix ${ev.text}",
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBanner(uiState: GamepadUiState) {
    val (color, title, sub) = when (val st = uiState.connectionState) {
        is ConnectionState.Connected -> Triple(
            EmeraldLive,
            "Ligado a ${st.target}",
            listOfNotNull(
                st.serverBanner,
                uiState.latencyMs?.let { "Latência: ${it}ms" },
            ).joinToString(" • ").ifBlank { uiState.lastAck },
        )
        is ConnectionState.Connecting -> Triple(
            AmberWarn,
            "A ligar a ${st.target}…",
            "A abrir canal WebSocket de baixa latência",
        )
        is ConnectionState.Reconnecting -> Triple(
            AmberWarn,
            "A religar (${st.attempt})…",
            st.reason,
        )
        is ConnectionState.Error -> Triple(
            CrimsonAlert,
            "Erro de ligação",
            st.message,
        )
        ConnectionState.Disconnected -> Triple(
            TextMuted,
            "Desligado",
            "Inicie o script no PC Linux e toque em Ligar.",
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun ControlsTabContent(
    uiState: GamepadUiState,
    onSetHaptics: (Boolean) -> Unit,
    onSetThumbAssist: (Boolean) -> Unit,
    onSetShowSprintLock: (Boolean) -> Unit,
    onSetDeadzone: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingToggleRow(
            title = "Botão Trava R (Correr Rápido)",
            subtitle = "Mostra um interruptor 'Travar R' no painel central para manter o R sempre carregado sem precisar de segurar no topo.",
            checked = uiState.settings.showSprintLock,
            onCheckedChange = onSetShowSprintLock,
            testTag = "switch_sprint_lock",
        )
        SettingToggleRow(
            title = "L / R para Polegares (Modo 2 Dedos)",
            subtitle = "Adiciona botões extra L (Trocar) e R (Correr) perto dos polegares, acima do D-Pad e dos botões de ação.",
            checked = uiState.settings.thumbAssistLr,
            onCheckedChange = onSetThumbAssist,
            testTag = "switch_thumb_assist",
        )
        SettingToggleRow(
            title = "Vibração Tátil",
            subtitle = "Pulso curto ao premir botões, ombros L/R e direções do D-Pad.",
            checked = uiState.settings.hapticsEnabled,
            onCheckedChange = onSetHaptics,
            testTag = "switch_haptics",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DeckSurfaceElevated)
                .border(1.dp, DeckBezel, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Zona Morta do Analógico",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "${(uiState.settings.analogDeadzone * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanPulse,
                )
            }
            Slider(
                value = uiState.settings.analogDeadzone,
                onValueChange = onSetDeadzone,
                valueRange = 0f..0.30f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanPulse,
                    activeTrackColor = CyanPulse,
                ),
            )
            Text(
                text = "Valores menores respondem mais depressa a pequenos movimentos do polegar.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DeckSurfaceElevated)
            .border(1.dp, DeckBezel, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun ScriptTabContent() {
    val context = LocalContext.current
    var copiedFeedback by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CyanPulse.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, CyanPulse.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Como pôr o L e R a 100% no PPSSPP (Linux)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanPulse,
                )
                Text(
                    text = "1. Copie ou partilhe este ficheiro como padphone_v3.py para o seu PC.\n" +
                        "2. Execute no terminal: python3 padphone_v3.py --lr hibrido\n" +
                        "3. O modo 'hibrido' envia Xbox LB/RB + LT/RT + teclas Q/W ao mesmo tempo — funciona mesmo sem remapear no PPSSPP!\n" +
                        "4. Se preferir mapear manualmente: PPSSPP > Definições > Controlos > Mapear controlos > toque em L e carregue em L na app (igual para R).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(
                        ClipData.newPlainText("padphone_v3.py", PadPhoneServerScript.scriptText),
                    )
                    copiedFeedback = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPulse,
                    contentColor = DeckObsidian,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("copy_script_button"),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (copiedFeedback) "Copiado!" else "Copiar Script")
            }

            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, PadPhoneServerScript.FILENAME)
                        putExtra(Intent.EXTRA_TEXT, PadPhoneServerScript.scriptText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Enviar padphone_v3.py"))
                },
                border = BorderStroke(1.dp, DeckBezel),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("share_script_button"),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = TextPrimary)
                Spacer(Modifier.width(6.dp))
                Text("Partilhar", color = TextPrimary)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DeckWell)
                .border(1.dp, DeckBezel, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text(
                text = PadPhoneServerScript.scriptText,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}
