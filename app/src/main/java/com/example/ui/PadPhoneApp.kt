package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.ConnectionState
import com.example.model.LrMode
import com.example.model.PadButton
import com.example.ui.components.AnalogStickControl
import com.example.ui.components.CenterHudCluster
import com.example.ui.components.ConnectionAndScriptDialog
import com.example.ui.components.ConsolePillButton
import com.example.ui.components.DpadControl
import com.example.ui.components.FaceButtonsDiamond
import com.example.ui.components.ShoulderTriggerButton
import com.example.ui.components.ThumbAssistPill
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

@Composable
fun PadPhoneApp(
    viewModel: GamepadViewModel,
    onLandscapeLockChanged: (Boolean) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
        viewModel.releaseAllInputs()
    }

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            DeckSurfaceElevated.copy(alpha = 0.72f),
            DeckObsidian,
        ),
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val isLandscape = maxWidth > maxHeight
        if (isLandscape) {
            LandscapeGamepadDeck(
                uiState = uiState,
                viewModel = viewModel,
                onOpenSettings = { showSettingsDialog = true },
                onToggleLandscapeLock = {
                    viewModel.toggleLandscapeLock()
                    onLandscapeLockChanged(!uiState.landscapeLocked)
                },
            )
        } else {
            PortraitDashboardScreen(
                uiState = uiState,
                viewModel = viewModel,
                onOpenSettings = { showSettingsDialog = true },
                onEnterLandscapeMode = {
                    if (!uiState.landscapeLocked) {
                        viewModel.toggleLandscapeLock()
                    }
                    onLandscapeLockChanged(true)
                },
            )
        }
    }

    if (showSettingsDialog) {
        ConnectionAndScriptDialog(
            uiState = uiState,
            onDismiss = { showSettingsDialog = false },
            onConnect = viewModel::connect,
            onDisconnect = viewModel::disconnect,
            onDiscover = viewModel::discoverServers,
            onSelectLrMode = viewModel::setLrMode,
            onRunLrTest = viewModel::runLrDiagnosticTest,
            onSetHaptics = viewModel::setHapticsEnabled,
            onSetThumbAssist = viewModel::setThumbAssistLr,
            onSetShowSprintLock = viewModel::setShowSprintLock,
            onSetDeadzone = viewModel::setAnalogDeadzone,
        )
    }
}

@Composable
private fun LandscapeGamepadDeck(
    uiState: GamepadUiState,
    viewModel: GamepadViewModel,
    onOpenSettings: () -> Unit,
    onToggleLandscapeLock: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val deckHeight = maxHeight
        val deckWidth = maxWidth
        val dpadSize = min(deckHeight * 0.44f, deckWidth * 0.22f).coerceIn(112.dp, 168.dp)
        val stickSize = min(deckHeight * 0.36f, deckWidth * 0.18f).coerceIn(92.dp, 144.dp)
        val faceSize = min(deckHeight * 0.56f, deckWidth * 0.27f).coerceIn(142.dp, 212.dp)
        val triggerWidth = (deckWidth * 0.21f).coerceIn(124.dp, 210.dp)
        val triggerHeight = (deckHeight * 0.17f).coerceIn(50.dp, 72.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: L Trigger + Center Telemetry HUD + R Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                ShoulderTriggerButton(
                    button = PadButton.L,
                    subtitle = "TROCAR",
                    isPressed = PadButton.L in uiState.activeButtons,
                    accentColor = CyanPulse,
                    contentDesc = stringResource(R.string.cd_btn_l),
                    testTag = "btn_l_shoulder",
                    onPressChanged = { viewModel.setButtonPressed(PadButton.L, it) },
                    modifier = Modifier
                        .width(triggerWidth)
                        .height(triggerHeight),
                )

                CenterHudCluster(
                    connectionState = uiState.connectionState,
                    latencyMs = uiState.latencyMs,
                    lastAck = uiState.lastAck,
                    lrMode = uiState.settings.lrMode,
                    sprintLocked = uiState.sprintLocked,
                    showSprintLock = uiState.settings.showSprintLock,
                    landscapeLocked = uiState.landscapeLocked,
                    onOpenConnection = onOpenSettings,
                    onCycleLrMode = viewModel::cycleLrMode,
                    onToggleSprintLock = viewModel::toggleSprintLock,
                    onToggleLandscapeLock = onToggleLandscapeLock,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 400.dp)
                        .padding(horizontal = 12.dp),
                )

                ShoulderTriggerButton(
                    button = PadButton.R,
                    subtitle = "CORRER",
                    isPressed = PadButton.R in uiState.activeButtons,
                    isLocked = uiState.sprintLocked,
                    accentColor = EmeraldLive,
                    contentDesc = stringResource(R.string.cd_btn_r),
                    testTag = "btn_r_shoulder",
                    onPressChanged = { viewModel.setButtonPressed(PadButton.R, it) },
                    modifier = Modifier
                        .width(triggerWidth)
                        .height(triggerHeight),
                )
            }

            // Bottom/Main Wing Row: Left (D-pad + Stick), Center (Select/PS/Start), Right (Face Diamond)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                // Left Wing: Optional Thumb L + D-Pad + Analog Stick side-by-side or staggered
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (uiState.settings.thumbAssistLr) {
                        ThumbAssistPill(
                            button = PadButton.L,
                            subtitle = "Trocar",
                            isPressed = PadButton.L in uiState.activeButtons,
                            accentColor = CyanPulse,
                            onPressChanged = { viewModel.setButtonPressed(PadButton.L, it) },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DpadControl(
                            dpadX = uiState.dpadX,
                            dpadY = uiState.dpadY,
                            onDpadChanged = viewModel::setDpad,
                            size = dpadSize,
                        )
                        AnalogStickControl(
                            stickX = uiState.stickX,
                            stickY = uiState.stickY,
                            deadzone = uiState.settings.analogDeadzone,
                            onStickChanged = viewModel::setStick,
                            size = stickSize,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }

                // Center Menu Cluster (SELECT / PS / START)
                Row(
                    modifier = Modifier.padding(bottom = 6.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ConsolePillButton(
                        button = PadButton.SELECT,
                        isPressed = PadButton.SELECT in uiState.activeButtons,
                        contentDesc = stringResource(R.string.cd_btn_select),
                        testTag = "btn_select",
                        onPressChanged = { viewModel.setButtonPressed(PadButton.SELECT, it) },
                    )
                    ConsolePillButton(
                        button = PadButton.HOME,
                        isPressed = PadButton.HOME in uiState.activeButtons,
                        contentDesc = stringResource(R.string.cd_btn_home),
                        testTag = "btn_home",
                        onPressChanged = { viewModel.setButtonPressed(PadButton.HOME, it) },
                    )
                    ConsolePillButton(
                        button = PadButton.START,
                        isPressed = PadButton.START in uiState.activeButtons,
                        contentDesc = stringResource(R.string.cd_btn_start),
                        testTag = "btn_start",
                        onPressChanged = { viewModel.setButtonPressed(PadButton.START, it) },
                    )
                }

                // Right Wing: Optional Thumb R + Face Buttons Diamond
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End,
                ) {
                    if (uiState.settings.thumbAssistLr) {
                        ThumbAssistPill(
                            button = PadButton.R,
                            subtitle = "Correr",
                            isPressed = PadButton.R in uiState.activeButtons,
                            accentColor = EmeraldLive,
                            onPressChanged = { viewModel.setButtonPressed(PadButton.R, it) },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    FaceButtonsDiamond(
                        pressedButtons = uiState.activeButtons,
                        onButtonChanged = viewModel::setButtonPressed,
                        size = faceSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitDashboardScreen(
    uiState: GamepadUiState,
    viewModel: GamepadViewModel,
    onOpenSettings: () -> Unit,
    onEnterLandscapeMode: () -> Unit,
) {
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val statusColor = when (uiState.connectionState) {
        is ConnectionState.Connected -> EmeraldLive
        is ConnectionState.Connecting, is ConnectionState.Reconnecting -> AmberWarn
        is ConnectionState.Error -> CrimsonAlert
        ConnectionState.Disconnected -> TextMuted
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top App Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyanPulse.copy(alpha = 0.16f))
                            .border(1.dp, CyanPulse.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = CyanPulse,
                        )
                    }
                    Column {
                        Text(
                            text = "PadPhone",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Comando PSP / PPSSPP sem fios",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }

                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(14.dp),
                    color = DeckSurfaceElevated,
                    border = BorderStroke(1.dp, DeckBezel),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = TextPrimary,
                        )
                    }
                }
            }

            // Primary Launch Landscape Deck CTA
            Button(
                onClick = onEnterLandscapeMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPulse,
                    contentColor = DeckObsidian,
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("enter_landscape_button"),
            ) {
                Icon(Icons.Rounded.ScreenRotation, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Abrir Comando PSP (Paisagem)",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Connection Status Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DeckSurface,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = statusColor,
                            )
                            Text(
                                text = when (val st = uiState.connectionState) {
                                    is ConnectionState.Connected -> "Ligado a ${st.target}"
                                    is ConnectionState.Connecting -> "A ligar a ${st.target}…"
                                    is ConnectionState.Reconnecting -> "A religar (${st.attempt})…"
                                    is ConnectionState.Error -> st.message
                                    ConnectionState.Disconnected -> "Desligado"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = statusColor,
                            )
                        }
                        uiState.latencyMs?.let { ms ->
                            Text(
                                text = "${ms}ms",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldLive,
                            )
                        }
                    }

                    Text(
                        text = "Destino: ${uiState.settings.host}:${uiState.settings.port} • L/R: ${uiState.settings.lrMode.shortLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                if (isConnected) {
                                    viewModel.disconnect()
                                } else {
                                    viewModel.connect(uiState.settings.host, uiState.settings.port)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) CrimsonAlert else EmeraldLive,
                                contentColor = DeckObsidian,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("portrait_connect_toggle"),
                        ) {
                            Text(if (isConnected) "Desligar" else "Ligar Agora")
                        }

                        OutlinedButton(
                            onClick = { viewModel.discoverServers(uiState.settings.port) },
                            border = BorderStroke(1.dp, DeckBezel),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("portrait_discover_button"),
                        ) {
                            if (uiState.isDiscovering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanPulse,
                                )
                            } else {
                                Icon(Icons.Rounded.Radar, contentDescription = null, tint = CyanPulse)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Procurar PC", color = TextPrimary)
                        }
                    }

                    if (uiState.discoveredServers.isNotEmpty()) {
                        uiState.discoveredServers.forEach { srv ->
                            Surface(
                                onClick = { viewModel.connect(srv.host, srv.port) },
                                shape = RoundedCornerShape(12.dp),
                                color = DeckSurfaceElevated,
                                border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = "${srv.name} (${srv.host}:${srv.port})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary,
                                        )
                                        Text(
                                            text = srv.backendInfo,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldLive,
                                        )
                                    }
                                    Text("Ligar", color = CyanPulse, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            // Quick L / R Verification & Mode Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DeckSurface,
                border = BorderStroke(1.dp, DeckBezel),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Teste Rápido L / R (Trocar & Correr)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Teste os ombros L e R antes de rodar o telemóvel e confirme a resposta do Linux abaixo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ShoulderTriggerButton(
                            button = PadButton.L,
                            subtitle = "TROCAR",
                            isPressed = PadButton.L in uiState.activeButtons,
                            accentColor = CyanPulse,
                            contentDesc = stringResource(R.string.cd_btn_l),
                            testTag = "portrait_btn_l",
                            onPressChanged = { viewModel.setButtonPressed(PadButton.L, it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                        )
                        ShoulderTriggerButton(
                            button = PadButton.R,
                            subtitle = "CORRER",
                            isPressed = PadButton.R in uiState.activeButtons,
                            isLocked = uiState.sprintLocked,
                            accentColor = EmeraldLive,
                            contentDesc = stringResource(R.string.cd_btn_r),
                            testTag = "portrait_btn_r",
                            onPressChanged = { viewModel.setButtonPressed(PadButton.R, it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = viewModel::cycleLrMode,
                            border = BorderStroke(1.dp, CyanPulse.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Text(
                                text = "Modo: ${uiState.settings.lrMode.shortLabel}",
                                color = CyanPulse,
                            )
                        }

                        Button(
                            onClick = viewModel::runLrDiagnosticTest,
                            enabled = !uiState.isTestingLr,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeckSurfaceElevated,
                                contentColor = EmeraldLive,
                            ),
                            border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.5f)),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Pulsar L/R")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeckWell)
                            .border(1.dp, DeckBezel, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "PC Ack: ${uiState.lastAck}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // Quick open Config / Script PC v3
            OutlinedButton(
                onClick = onOpenSettings,
                border = BorderStroke(1.dp, DeckBezel),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("open_config_and_script_button"),
            ) {
                Icon(Icons.Rounded.Code, contentDescription = null, tint = CyanPulse)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Configurar IP, Controlos & Copiar Script PC (v3)",
                    color = TextPrimary,
                )
            }

            // Mini mode info strip
            Text(
                text = "Dica: No script PC v3, execute 'python3 padphone_v3.py --lr hibrido' para o L e R funcionarem mesmo sem remapear no PPSSPP.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}
