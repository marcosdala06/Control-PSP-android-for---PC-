package com.example.data

import com.example.model.LrMode
import com.example.model.PadButton
import java.util.Locale
import kotlin.math.hypot

object PadProtocol {

    fun buttonCommand(button: PadButton, pressed: Boolean): String =
        "k,${button.wireKey},${if (pressed) 1 else 0}"

    fun dpadCommand(x: Int, y: Int): String =
        "h,${x.coerceIn(-1, 1)},${y.coerceIn(-1, 1)}"

    fun stickCommand(x: Float, y: Float): String {
        val cx = x.coerceIn(-1f, 1f)
        val cy = y.coerceIn(-1f, 1f)
        return String.format(Locale.US, "a,%.3f,%.3f", cx, cy)
    }

    fun lrModeCommand(mode: LrMode): String = "m,${mode.wireValue}"

    fun releaseAllCommand(): String = "x"

    fun pingCommand(timestampMs: Long? = null): String =
        if (timestampMs != null) "p,$timestampMs" else "p"

    fun normalizeStick(
        dx: Float,
        dy: Float,
        radius: Float,
        deadzone: Float,
    ): Pair<Float, Float> {
        if (radius <= 0f) return 0f to 0f
        var nx = dx / radius
        var ny = dy / radius
        val mag = hypot(nx, ny)
        if (mag < deadzone) return 0f to 0f
        if (mag > 1f) {
            nx /= mag
            ny /= mag
            return nx to ny
        }
        // Rescale smoothly outside deadzone so full range 0..1 is reachable
        val scaled = ((mag - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
        val factor = scaled / mag
        return (nx * factor) to (ny * factor)
    }

    fun computeDpadAxes(
        dx: Float,
        dy: Float,
        halfWidth: Float,
        threshold: Float = 0.26f,
    ): Pair<Int, Int> {
        if (halfWidth <= 0f) return 0 to 0
        val nx = dx / halfWidth
        val ny = dy / halfWidth
        val hx = when {
            nx < -threshold -> -1
            nx > threshold -> 1
            else -> 0
        }
        val hy = when {
            ny < -threshold -> -1
            ny > threshold -> 1
            else -> 0
        }
        return hx to hy
    }

    fun buildWebSocketUrl(host: String, port: Int): String {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("ws://")
            .removePrefix("wss://")
            .substringBefore("/")
        return "ws://$cleanHost:$port/ws"
    }
}
