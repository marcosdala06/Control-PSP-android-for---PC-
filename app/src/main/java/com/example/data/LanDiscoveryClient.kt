package com.example.data

import com.example.model.DiscoveredServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class LanDiscoveryClient {

    suspend fun discoverServers(port: Int = 8000): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, DiscoveredServer>()

        // 1. Fast UDP broadcast discovery (works with padphone_v3.py on port 8001)
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 700
                val payload = "PADPHONE_DISCOVER".toByteArray(Charsets.UTF_8)
                val targets = buildSet {
                    add(InetAddress.getByName("255.255.255.255"))
                    localBroadcastAddresses().forEach { add(it) }
                }
                for (addr in targets) {
                    runCatching {
                        socket.send(DatagramPacket(payload, payload.size, addr, 8001))
                    }
                }
                val deadline = System.currentTimeMillis() + 900L
                val buf = ByteArray(512)
                while (System.currentTimeMillis() < deadline) {
                    val pkt = DatagramPacket(buf, buf.size)
                    try {
                        socket.receive(pkt)
                        val msg = String(pkt.data, 0, pkt.length, Charsets.UTF_8).trim()
                        if (msg.startsWith("PADPHONE_HERE|")) {
                            val parts = msg.split("|")
                            val ip = pkt.address.hostAddress ?: continue
                            val p = parts.getOrNull(2)?.toIntOrNull() ?: port
                            val hostName = parts.getOrNull(1).orEmpty().ifBlank { "PC Linux" }
                            val backend = parts.getOrNull(3).orEmpty().ifBlank { "PadPhone v3" }
                            found["$ip:$p"] = DiscoveredServer(
                                host = ip,
                                port = p,
                                name = hostName,
                                backendInfo = backend,
                            )
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }

        // 2. Fallback TCP/port probe on the local /24 subnet if UDP found nothing (supports padphone.py v2!)
        if (found.isEmpty()) {
            val prefixes = localIpv4Addresses().mapNotNull { ip ->
                val parts = ip.split(".")
                if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" to ip else null
            }
            coroutineScope {
                for ((prefix, selfIp) in prefixes) {
                    val jobs = (1..254).map { lastOctet ->
                        val candidate = "$prefix.$lastOctet"
                        async {
                            if (candidate == selfIp) return@async null
                            val open = isTcpPortOpen(candidate, port, timeoutMs = 180)
                            if (open) {
                                DiscoveredServer(
                                    host = candidate,
                                    port = port,
                                    name = "Servidor PadPhone",
                                    backendInfo = "Porta $port aberta",
                                )
                            } else {
                                null
                            }
                        }
                    }
                    jobs.awaitAll().filterNotNull().forEach { srv ->
                        found["${srv.host}:${srv.port}"] = srv
                    }
                }
            }
        }

        found.values.toList()
    }

    private fun isTcpPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun localIpv4Addresses(): List<String> {
        val result = mutableListOf<String>()
        runCatching {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            for (iface in ifaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addr.hostAddress?.let { result.add(it) }
                    }
                }
            }
        }
        return result
    }

    private fun localBroadcastAddresses(): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        runCatching {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            for (iface in ifaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (ifAddr in iface.interfaceAddresses) {
                    val bcast = ifAddr.broadcast
                    if (bcast is Inet4Address) {
                        result.add(bcast)
                    }
                }
            }
        }
        return result
    }
}
