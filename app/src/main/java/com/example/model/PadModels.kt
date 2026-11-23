package com.example.model

enum class PadButton(val wireKey: String, val label: String) {
    CROSS("cross", "✕"),
    CIRCLE("circle", "○"),
    SQUARE("square", "□"),
    TRIANGLE("triangle", "△"),
    L("l", "L"),
    R("r", "R"),
    SELECT("select", "SELECT"),
    START("start", "START"),
    HOME("home", "PS"),
}

enum class LrMode(
    val wireValue: String,
    val shortLabel: String,
    val description: String,
) {
    AMBOS(
        wireValue = "ambos",
        shortLabel = "Ambos (LB+LT)",
        description = "Envia botão LB/RB + gatilho analógico LT/RT ao mesmo tempo. Melhor compatibilidade.",
    ),
    HIBRIDO(
        wireValue = "hibrido",
        shortLabel = "Híbrido (+Q/W)",
        description = "Requer Script v3: envia LB/RB + LT/RT + teclas Q/W para o L/R funcionar sempre no PPSSPP.",
    ),
    BOTOES(
        wireValue = "botoes",
        shortLabel = "Botões (LB/RB)",
        description = "Envia apenas BTN_TL / BTN_TR (ombros digitais Xbox 360).",
    ),
    GATILHOS(
        wireValue = "gatilhos",
        shortLabel = "Gatilhos (LT/RT)",
        description = "Envia apenas eixos ABS_Z / ABS_RZ (0..255).",
    );

    fun next(): LrMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromWire(value: String): LrMode =
            entries.firstOrNull { it.wireValue == value } ?: AMBOS
    }
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val target: String) : ConnectionState
    data class Connected(val target: String, val serverBanner: String? = null) : ConnectionState
    data class Reconnecting(val target: String, val attempt: Int, val reason: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

data class DiscoveredServer(
    val host: String,
    val port: Int,
    val name: String,
    val backendInfo: String,
)

data class DiagnosticEvent(
    val timestampMs: Long,
    val direction: Direction,
    val text: String,
) {
    enum class Direction { OUT, IN, SYS }
}

data class ControllerSettings(
    val host: String = "192.168.1.100",
    val port: Int = 8000,
    val lrMode: LrMode = LrMode.AMBOS,
    val hapticsEnabled: Boolean = true,
    val thumbAssistLr: Boolean = false,
    val showSprintLock: Boolean = true,
    val analogDeadzone: Float = 0.08f,
    val savedHosts: List<String> = emptyList(),
)
