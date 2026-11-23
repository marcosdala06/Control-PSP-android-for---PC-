package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LanDiscoveryClient
import com.example.data.PadProtocol
import com.example.data.PadSocketClient
import com.example.data.SettingsRepository
import com.example.model.ConnectionState
import com.example.model.ControllerSettings
import com.example.model.DiagnosticEvent
import com.example.model.DiscoveredServer
import com.example.model.LrMode
import com.example.model.PadButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GamepadUiState(
    val settings: ControllerSettings = ControllerSettings(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val latencyMs: Long? = null,
    val lastAck: String = "Pronto para ligar ao PC",
    val activeButtons: Set<PadButton> = emptySet(),
    val sprintLocked: Boolean = false,
    val dpadX: Int = 0,
    val dpadY: Int = 0,
    val stickX: Float = 0f,
    val stickY: Float = 0f,
    val isDiscovering: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val discoveryMessage: String? = null,
    val diagnosticEvents: List<DiagnosticEvent> = emptyList(),
    val isTestingLr: Boolean = false,
    val landscapeLocked: Boolean = false,
)

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val socketClient = PadSocketClient()
    private val discoveryClient = LanDiscoveryClient()
    private val haptics = HapticHelper(application)

    private val physicalButtons = MutableStateFlow<Set<PadButton>>(emptySet())
    private val sprintLocked = MutableStateFlow(false)
    private val dpadState = MutableStateFlow(0 to 0)
    private val stickState = MutableStateFlow(0f to 0f)
    private val lastAck = MutableStateFlow("Pronto para ligar ao PC")
    private val isDiscovering = MutableStateFlow(false)
    private val discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    private val discoveryMessage = MutableStateFlow<String?>(null)
    private val diagnosticEvents = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    private val isTestingLr = MutableStateFlow(false)
    private val landscapeLocked = MutableStateFlow(false)

    private var lastStickSentAtMs = 0L

    private val settingsFlow = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ControllerSettings(),
    )

    val uiState: StateFlow<GamepadUiState> = combine(
        settingsFlow,
        socketClient.connectionState,
        socketClient.latencyMs,
        lastAck,
        physicalButtons,
        sprintLocked,
        dpadState,
        stickState,
        isDiscovering,
        discoveredServers,
        discoveryMessage,
        diagnosticEvents,
        isTestingLr,
        landscapeLocked,
    ) { values ->
        val settings = values[0] as ControllerSettings
        val connState = values[1] as ConnectionState
        val latency = values[2] as Long?
        val ack = values[3] as String
        @Suppress("UNCHECKED_CAST")
        val phys = values[4] as Set<PadButton>
        val locked = values[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val dpad = values[6] as Pair<Int, Int>
        @Suppress("UNCHECKED_CAST")
        val stick = values[7] as Pair<Float, Float>
        val discovering = values[8] as Boolean
        @Suppress("UNCHECKED_CAST")
        val servers = values[9] as List<DiscoveredServer>
        val discMsg = values[10] as String?
        @Suppress("UNCHECKED_CAST")
        val logs = values[11] as List<DiagnosticEvent>
        val testing = values[12] as Boolean
        val landLock = values[13] as Boolean

        val effectiveButtons = if (locked) phys + PadButton.R else phys

        GamepadUiState(
            settings = settings,
            connectionState = connState,
            latencyMs = latency,
            lastAck = ack,
            activeButtons = effectiveButtons,
            sprintLocked = locked,
            dpadX = dpad.first,
            dpadY = dpad.second,
            stickX = stick.first,
            stickY = stick.second,
            isDiscovering = discovering,
            discoveredServers = servers,
            discoveryMessage = discMsg,
            diagnosticEvents = logs,
            isTestingLr = testing,
            landscapeLocked = landLock,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GamepadUiState(),
    )

    init {
        viewModelScope.launch {
            socketClient.ackMessages.collect { ack ->
                lastAck.value = ack
                appendLog(DiagnosticEvent.Direction.IN, ack)
            }
        }
    }

    fun connect(host: String, port: Int) {
        val cleanHost = host.trim()
        val safePort = port.coerceIn(1, 65535)
        viewModelScope.launch {
            settingsRepo.saveConnectionTarget(cleanHost, safePort)
        }
        appendLog(DiagnosticEvent.Direction.SYS, "A ligar a $cleanHost:$safePort…")
        socketClient.connect(cleanHost, safePort, settingsFlow.value.lrMode)
    }

    fun disconnect() {
        releaseAllInputs()
        socketClient.disconnect()
        lastAck.value = "Desligado"
        appendLog(DiagnosticEvent.Direction.SYS, "Ligação encerrada")
    }

    fun discoverServers(port: Int) {
        if (isDiscovering.value) return
        viewModelScope.launch {
            isDiscovering.value = true
            discoveryMessage.value = "A procurar PadPhone na rede Wi-Fi…"
            val servers = discoveryClient.discoverServers(port)
            discoveredServers.value = servers
            discoveryMessage.value = if (servers.isEmpty()) {
                "Nenhum PC encontrado automaticamente. Verifique se o script está a correr e na mesma rede Wi-Fi."
            } else {
                "${servers.size} PC(s) encontrado(s). Toque para ligar."
            }
            isDiscovering.value = false
        }
    }

    fun setButtonPressed(button: PadButton, pressed: Boolean) {
        val currentPhys = physicalButtons.value
        val wasPhysPressed = button in currentPhys
        if (wasPhysPressed == pressed) return

        val wasEffectiveR = PadButton.R in currentPhys || sprintLocked.value

        val updatedPhys = if (pressed) currentPhys + button else currentPhys - button
        physicalButtons.value = updatedPhys

        if (pressed && settingsFlow.value.hapticsEnabled) {
            haptics.pulse(12L)
        }

        if (button == PadButton.R) {
            val isEffectiveR = PadButton.R in updatedPhys || sprintLocked.value
            if (isEffectiveR != wasEffectiveR) {
                sendCommand(PadProtocol.buttonCommand(PadButton.R, isEffectiveR))
            }
        } else {
            sendCommand(PadProtocol.buttonCommand(button, pressed))
        }
    }

    fun toggleSprintLock() {
        val nextLocked = !sprintLocked.value
        val phys = physicalButtons.value
        val wasEffectiveR = PadButton.R in phys || sprintLocked.value
        sprintLocked.value = nextLocked
        val isEffectiveR = PadButton.R in phys || nextLocked
        if (settingsFlow.value.hapticsEnabled) {
            haptics.pulse(if (nextLocked) 22L else 10L)
        }
        if (isEffectiveR != wasEffectiveR) {
            sendCommand(PadProtocol.buttonCommand(PadButton.R, isEffectiveR))
        }
    }

    fun setDpad(x: Int, y: Int) {
        val clamped = x.coerceIn(-1, 1) to y.coerceIn(-1, 1)
        if (dpadState.value == clamped) return
        dpadState.value = clamped
        if ((clamped.first != 0 || clamped.second != 0) && settingsFlow.value.hapticsEnabled) {
            haptics.pulse(8L)
        }
        sendCommand(PadProtocol.dpadCommand(clamped.first, clamped.second), logOutgoing = false)
    }

    fun setStick(x: Float, y: Float) {
        val clamped = x.coerceIn(-1f, 1f) to y.coerceIn(-1f, 1f)
        stickState.value = clamped
        val now = System.currentTimeMillis()
        val isCenter = clamped.first == 0f && clamped.second == 0f
        if (isCenter || now - lastStickSentAtMs >= 12L) {
            lastStickSentAtMs = now
            sendCommand(PadProtocol.stickCommand(clamped.first, clamped.second), logOutgoing = false)
        }
    }

    fun setLrMode(mode: LrMode) {
        viewModelScope.launch {
            settingsRepo.saveLrMode(mode)
        }
        socketClient.updateDesiredLrMode(mode)
        appendLog(DiagnosticEvent.Direction.OUT, PadProtocol.lrModeCommand(mode))
        if (settingsFlow.value.hapticsEnabled) {
            haptics.pulse(12L)
        }
    }

    fun cycleLrMode() {
        setLrMode(settingsFlow.value.lrMode.next())
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setHapticsEnabled(enabled) }
    }

    fun setThumbAssistLr(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setThumbAssistLr(enabled) }
    }

    fun setShowSprintLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setShowSprintLock(enabled) }
    }

    fun setAnalogDeadzone(deadzone: Float) {
        viewModelScope.launch { settingsRepo.setAnalogDeadzone(deadzone) }
    }

    fun toggleLandscapeLock() {
        landscapeLocked.value = !landscapeLocked.value
    }

    fun runLrDiagnosticTest() {
        if (isTestingLr.value) return
        viewModelScope.launch {
            isTestingLr.value = true
            appendLog(DiagnosticEvent.Direction.SYS, "A testar L (Trocar) e R (Correr)…")
            setButtonPressed(PadButton.L, true)
            delay(250L)
            setButtonPressed(PadButton.L, false)
            delay(180L)
            setButtonPressed(PadButton.R, true)
            delay(250L)
            setButtonPressed(PadButton.R, false)
            isTestingLr.value = false
        }
    }

    fun releaseAllInputs() {
        physicalButtons.value = emptySet()
        sprintLocked.value = false
        dpadState.value = 0 to 0
        stickState.value = 0f to 0f
        socketClient.send(PadProtocol.releaseAllCommand())
    }

    private fun sendCommand(cmd: String, logOutgoing: Boolean = true) {
        val sent = socketClient.send(cmd)
        if (logOutgoing && sent) {
            appendLog(DiagnosticEvent.Direction.OUT, cmd)
        }
    }

    private fun appendLog(dir: DiagnosticEvent.Direction, text: String) {
        diagnosticEvents.update { current ->
            (listOf(DiagnosticEvent(System.currentTimeMillis(), dir, text)) + current).take(25)
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketClient.disconnect()
    }
}
