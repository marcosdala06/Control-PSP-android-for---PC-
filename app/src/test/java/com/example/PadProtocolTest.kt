package com.example

import com.example.data.PadPhoneServerScript
import com.example.data.PadProtocol
import com.example.model.LrMode
import com.example.model.PadButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PadProtocolTest {

    @Test
    fun `button commands match padphone wire format`() {
        assertEquals("k,r,1", PadProtocol.buttonCommand(PadButton.R, true))
        assertEquals("k,r,0", PadProtocol.buttonCommand(PadButton.R, false))
        assertEquals("k,l,1", PadProtocol.buttonCommand(PadButton.L, true))
        assertEquals("k,cross,1", PadProtocol.buttonCommand(PadButton.CROSS, true))
        assertEquals("m,ambos", PadProtocol.lrModeCommand(LrMode.AMBOS))
        assertEquals("m,hibrido", PadProtocol.lrModeCommand(LrMode.HIBRIDO))
        assertEquals("x", PadProtocol.releaseAllCommand())
    }

    @Test
    fun `analog stick deadzone and clamping work properly`() {
        val (dx0, dy0) = PadProtocol.normalizeStick(3f, 4f, radius = 100f, deadzone = 0.08f)
        assertEquals(0f, dx0, 0.0001f)
        assertEquals(0f, dy0, 0.0001f)

        val (dxMax, dyMax) = PadProtocol.normalizeStick(200f, 0f, radius = 100f, deadzone = 0.08f)
        assertEquals(1f, dxMax, 0.0001f)
        assertEquals(0f, dyMax, 0.0001f)
    }

    @Test
    fun `dpad supports 8 way diagonals`() {
        val (hx, hy) = PadProtocol.computeDpadAxes(60f, -60f, halfWidth = 100f)
        assertEquals(1, hx)
        assertEquals(-1, hy)
    }

    @Test
    fun `embedded python v3 script has valid docstring and defaults`() {
        val script = PadPhoneServerScript.scriptText
        assertTrue(script.startsWith("#!/usr/bin/env python3"))
        assertTrue(script.contains("\"\"\""))
        assertFalse(script.contains("\\\"\\\"\\\""))
        assertTrue(script.contains("default=\"ambos\""))
        assertTrue(script.contains("PADPHONE_DISCOVER"))
    }
}
