package com.example.data

import com.example.model.ConnectionState
import com.example.model.LrMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PadSocketClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _ackMessages = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val ackMessages: SharedFlow<String> = _ackMessages.asSharedFlow()

    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var currentHost: String = ""

    @Volatile
    private var currentPort: Int = 8000

    @Volatile
    private var desiredLrMode: LrMode = LrMode.AMBOS

    private val manualDisconnect = AtomicBoolean(true)
    private var reconnectJob: Job? = null
    private var keepAliveJob: Job? = null
    private var lastPingSentAtMs: Long = 0L

    fun connect(host: String, port: Int, initialLrMode: LrMode) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) {
            _connectionState.value = ConnectionState.Error("Informe o IP do PC")
            return
        }
        manualDisconnect.set(false)
        currentHost = cleanHost
        currentPort = port
        desiredLrMode = initialLrMode
        reconnectJob?.cancel()
        openSocket(attempt = 0)
    }

    fun updateDesiredLrMode(mode: LrMode) {
        desiredLrMode = mode
        send(PadProtocol.lrModeCommand(mode))
    }

    fun disconnect() {
        manualDisconnect.set(true)
        reconnectJob?.cancel()
        keepAliveJob?.cancel()
        runCatching { webSocket?.send(PadProtocol.releaseAllCommand()) }
        runCatching { webSocket?.close(1000, "User disconnected") }
        webSocket = null
        _latencyMs.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun send(message: String): Boolean {
        val ws = webSocket ?: return false
        return ws.send(message)
    }

    private fun openSocket(attempt: Int) {
        runCatching { webSocket?.cancel() }
        webSocket = null

        val target = "$currentHost:$currentPort"
        _connectionState.value = if (attempt == 0) {
            ConnectionState.Connecting(target)
        } else {
            ConnectionState.Reconnecting(target, attempt, "A restabelecer ligação…")
        }

        val url = PadProtocol.buildWebSocketUrl(currentHost, currentPort)
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    if (webSocket !== ws) return
                    _connectionState.value = ConnectionState.Connected(target)
                    // Immediately sync preferred L/R mode so v2 or v3 servers stop defaulting to broken mode
                    ws.send(PadProtocol.lrModeCommand(desiredLrMode))
                    startKeepAlive(ws)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    if (webSocket !== ws) return
                    if (text.startsWith("pong,")) {
                        val sentTs = text.removePrefix("pong,").toLongOrNull()
                        if (sentTs != null) {
                            _latencyMs.value = (System.currentTimeMillis() - sentTs).coerceAtLeast(1L)
                        }
                        return
                    }
                    if (lastPingSentAtMs > 0L && _latencyMs.value == null) {
                        _latencyMs.value = (System.currentTimeMillis() - lastPingSentAtMs).coerceAtLeast(1L)
                    }
                    if (text.startsWith("ligado")) {
                        val cur = _connectionState.value
                        if (cur is ConnectionState.Connected) {
                            _connectionState.value = cur.copy(serverBanner = text)
                        }
                    }
                    _ackMessages.tryEmit(text)
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    ws.close(1000, null)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    if (webSocket !== ws) return
                    handleSocketDown(attempt, reason.ifBlank { "Ligação encerrada" })
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    if (webSocket !== ws) return
                    handleSocketDown(attempt, t.localizedMessage ?: "Falha de rede")
                }
            },
        )
    }

    private fun startKeepAlive(ws: WebSocket) {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && webSocket === ws && !manualDisconnect.get()) {
                val now = System.currentTimeMillis()
                lastPingSentAtMs = now
                ws.send(PadProtocol.pingCommand(now))
                delay(2000L)
            }
        }
    }

    private fun handleSocketDown(attempt: Int, reason: String) {
        keepAliveJob?.cancel()
        webSocket = null
        _latencyMs.value = null
        if (manualDisconnect.get()) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        val nextAttempt = attempt + 1
        val target = "$currentHost:$currentPort"
        _connectionState.value = ConnectionState.Reconnecting(target, nextAttempt, reason)
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoff = (700L * nextAttempt.coerceAtMost(4))
            delay(backoff)
            if (!manualDisconnect.get()) {
                openSocket(nextAttempt)
            }
        }
    }
}
