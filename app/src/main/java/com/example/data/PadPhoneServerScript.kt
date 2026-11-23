package com.example.data

object PadPhoneServerScript {

    const val FILENAME = "padphone_v3.py"

    val scriptText: String = """
#!/usr/bin/env python3
\"\"\"
PadPhone v3 - Comando PSP (PPSSPP no Linux) 100% funcional.

CORREÇÕES FACE À v2 (porque falhava o L/R - correr rápido e trocar jogador):
  1. Padrão --lr agora é "ambos" (envia LB/RB + gatilhos LT/RT ao mesmo tempo).
  2. Novo modo --lr hibrido: além de Xbox LB/RB + LT/RT, também pulsa as
     teclas padrão do PPSSPP (L=Q, R=W) para funcionar SEMPRE sem remapeamento!
  3. Descoberta automática na rede Wi-Fi (UDP 8001) para a app Android encontrar
     o PC com 1 toque sem precisar de escrever o IP.
  4. Ping com medição de latência real (pong,<ts>) e TCP_NODELAY ativo.

Uso:
    python3 padphone_v3.py                  # Xbox 360 + L/R ambos (recomendado)
    python3 padphone_v3.py --lr hibrido     # Xbox 360 + teclas Q/W no L e R
    python3 padphone_v3.py --teclado        # só teclado virtual (Z/X/A/S/Q/W)
    python3 padphone_v3.py --simular        # diagnóstico sem uinput
    python3 padphone_v3.py --teste-lr       # pulsa L e R para testar no PPSSPP
\"\"\"
import argparse
import base64
import hashlib
import socket
import struct
import sys
import threading
import time
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler

try:
    from evdev import UInput, AbsInfo, ecodes as e
except ImportError:
    UInput = None
    AbsInfo = None
    e = None

PORT = 8000
DISCOVERY_PORT = 8001
GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
LOCK = threading.RLock()
pad = None

LR_MODES = ("ambos", "hibrido", "botoes", "gatilhos")


def build_maps():
    if e is None:
        return {}, {}
    pad_buttons = {
        "cross": e.BTN_A, "circle": e.BTN_B, "square": e.BTN_X, "triangle": e.BTN_Y,
        "l": e.BTN_TL, "r": e.BTN_TR, "select": e.BTN_SELECT, "start": e.BTN_START,
        "home": e.BTN_MODE,
    }
    kbd_keys = {
        "cross": e.KEY_Z, "circle": e.KEY_X, "square": e.KEY_A, "triangle": e.KEY_S,
        "l": e.KEY_Q, "r": e.KEY_W, "start": e.KEY_SPACE, "select": e.KEY_ENTER,
        "home": e.KEY_ESC,
    }
    return pad_buttons, kbd_keys


PAD_BUTTONS, KBD_KEYS = build_maps()


def nome(code):
    try:
        n = e.bytype[e.EV_KEY][code]
        return n[0] if isinstance(n, tuple) else str(n)
    except Exception:
        return str(code)


class GamepadBackend:
    kind = "Xbox 360 + L/R"

    def __init__(self, lr="ambos"):
        if UInput is None:
            raise RuntimeError("Falta python3-evdev (sudo apt install python3-evdev)")
        self.lr = lr if lr in LR_MODES else "ambos"
        stick = AbsInfo(0, -32768, 32767, 16, 128, 0)
        trigger = AbsInfo(0, 0, 255, 0, 0, 0)
        hat = AbsInfo(0, -1, 1, 0, 0, 0)
        cap = {
            e.EV_KEY: list(PAD_BUTTONS.values()) + [e.BTN_THUMBL, e.BTN_THUMBR],
            e.EV_ABS: [
                (e.ABS_X, stick), (e.ABS_Y, stick),
                (e.ABS_Z, trigger),
                (e.ABS_RX, stick), (e.ABS_RY, stick),
                (e.ABS_RZ, trigger),
                (e.ABS_HAT0X, hat), (e.ABS_HAT0Y, hat),
            ],
        }
        self.ui = UInput(cap, name="Microsoft X-Box 360 pad", vendor=0x045e,
                         product=0x028e, version=0x110, bustype=e.BUS_USB)
        # Teclado auxiliar apenas para Q (L) e W (R) no modo híbrido
        self.kbd_lr = UInput({e.EV_KEY: [e.KEY_Q, e.KEY_W]}, name="PadPhone LR Keys")

    def set_lr(self, modo):
        if modo in LR_MODES:
            self._release_lr()
            self.lr = modo
            return True
        return False

    def _release_lr(self):
        for btn, axis, key in (("l", e.ABS_Z, e.KEY_Q), ("r", e.ABS_RZ, e.KEY_W)):
            self.ui.write(e.EV_KEY, PAD_BUTTONS[btn], 0)
            self.ui.write(e.EV_ABS, axis, 0)
            self.kbd_lr.write(e.EV_KEY, key, 0)
        self.ui.syn()
        self.kbd_lr.syn()

    def button(self, name, down):
        val = 1 if down else 0
        if name in ("l", "r"):
            if self.lr in ("botoes", "ambos", "hibrido"):
                self.ui.write(e.EV_KEY, PAD_BUTTONS[name], val)
            if self.lr in ("gatilhos", "ambos", "hibrido"):
                self.ui.write(e.EV_ABS, e.ABS_Z if name == "l" else e.ABS_RZ, 255 if down else 0)
            self.ui.syn()

            if self.lr == "hibrido":
                key = e.KEY_Q if name == "l" else e.KEY_W
                self.kbd_lr.write(e.EV_KEY, key, val)
                self.kbd_lr.syn()

            if self.lr == "gatilhos":
                return f"{name.upper()} -> {'ABS_Z' if name == 'l' else 'ABS_RZ'}"
            if self.lr == "ambos":
                return f"{name.upper()} -> {nome(PAD_BUTTONS[name])} + gatilho"
            if self.lr == "hibrido":
                return f"{name.upper()} -> LB/LT + {'Q' if name == 'l' else 'W'}"
            return f"{name.upper()} -> {nome(PAD_BUTTONS[name])}"

        code = PAD_BUTTONS.get(name)
        if code:
            self.ui.write(e.EV_KEY, code, val)
            self.ui.syn()
            return f"{name.upper()} -> {nome(code)}"
        return None

    def hat(self, x, y):
        self.ui.write(e.EV_ABS, e.ABS_HAT0X, x)
        self.ui.write(e.EV_ABS, e.ABS_HAT0Y, y)
        self.ui.syn()

    def stick(self, x, y):
        self.ui.write(e.EV_ABS, e.ABS_X, int(x * 32767))
        self.ui.write(e.EV_ABS, e.ABS_Y, int(y * 32767))
        self.ui.syn()

    def release_all(self):
        for code in PAD_BUTTONS.values():
            self.ui.write(e.EV_KEY, code, 0)
        for ax in (e.ABS_X, e.ABS_Y, e.ABS_HAT0X, e.ABS_HAT0Y, e.ABS_Z, e.ABS_RZ):
            self.ui.write(e.EV_ABS, ax, 0)
        self.ui.syn()
        self.kbd_lr.write(e.EV_KEY, e.KEY_Q, 0)
        self.kbd_lr.write(e.EV_KEY, e.KEY_W, 0)
        self.kbd_lr.syn()

    def close(self):
        self.release_all()
        self.ui.close()
        self.kbd_lr.close()


class KeyboardBackend:
    kind = "teclado (Q/W)"
    lr = "teclas"

    def __init__(self, lr="ambos"):
        if UInput is None:
            raise RuntimeError("Falta python3-evdev (sudo apt install python3-evdev)")
        keys = set(KBD_KEYS.values()) | {
            e.KEY_UP, e.KEY_DOWN, e.KEY_LEFT, e.KEY_RIGHT,
            e.KEY_I, e.KEY_J, e.KEY_K, e.KEY_L,
        }
        self.ui = UInput({e.EV_KEY: sorted(keys)}, name="PadPhone Keyboard")
        self.state = {}

    def set_lr(self, modo):
        return False

    def _set(self, code, down):
        down = 1 if down else 0
        if self.state.get(code, 0) != down:
            self.state[code] = down
            self.ui.write(e.EV_KEY, code, down)
            self.ui.syn()

    def button(self, name, down):
        code = KBD_KEYS.get(name)
        if code:
            self._set(code, down)
            return f"{name.upper()} -> tecla {nome(code)}"
        return None

    def hat(self, x, y):
        self._set(e.KEY_LEFT, x < 0)
        self._set(e.KEY_RIGHT, x > 0)
        self._set(e.KEY_UP, y < 0)
        self._set(e.KEY_DOWN, y > 0)

    def stick(self, x, y):
        t = 0.45
        self._set(e.KEY_J, x < -t)
        self._set(e.KEY_L, x > t)
        self._set(e.KEY_I, y < -t)
        self._set(e.KEY_K, y > t)

    def release_all(self):
        for code in list(self.state):
            self._set(code, False)

    def close(self):
        self.ui.close()


class SimBackend:
    kind = "simulação"

    def __init__(self, lr="ambos"):
        self.lr = lr

    def set_lr(self, modo):
        if modo in LR_MODES:
            self.lr = modo
            return True
        return False

    def button(self, name, down):
        extra = f" [modo L/R: {self.lr}]" if name in ("l", "r") else ""
        return f"{name.upper()} {'▼' if down else '▲'}{extra}"

    def hat(self, x, y):
        print(f"  D-pad {x},{y}", flush=True)

    def stick(self, x, y):
        pass

    def release_all(self):
        print("  tudo solto", flush=True)

    def close(self):
        pass


def clamp(v):
    return max(-1.0, min(1.0, v))


def process(msg):
    p = msg.strip().split(",")
    try:
        if p[0] == "p":
            return f"pong,{p[1]}" if len(p) > 1 else None
        with LOCK:
            if p[0] == "k":
                down = p[2] == "1"
                info = pad.button(p[1], down)
                seta = "▼" if down else "▲"
                print(f"{p[1].upper()} {seta}  {info or ''}", flush=True)
                return f"{p[1].upper()} {seta}  {info or '?'}"
            if p[0] == "h":
                pad.hat(int(p[1]), int(p[2]))
            elif p[0] == "a":
                pad.stick(clamp(float(p[1])), clamp(float(p[2])))
            elif p[0] == "m":
                ok = pad.set_lr(p[1])
                print(f"modo L/R -> {p[1]} {'ok' if ok else '(ignorado)'}", flush=True)
                return f"L/R: {p[1]}" if ok else "modo L/R não suportado"
            elif p[0] == "x":
                pad.release_all()
                return "tudo solto"
    except (IndexError, ValueError, AttributeError):
        pass
    return None


class Server(ThreadingHTTPServer):
    daemon_threads = True


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def _send(self, code, body=b"", ctype="text/plain"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if body:
            self.wfile.write(body)

    def _read(self, n):
        data = self.rfile.read(n)
        if data is None or len(data) < n:
            raise EOFError
        return data

    def _text(self, txt):
        b = txt.encode("utf-8")[:125]
        self.wfile.write(bytes([0x81, len(b)]) + b)
        self.wfile.flush()

    def do_GET(self):
        if self.path.startswith("/ws"):
            return self.websocket()
        if self.path == "/ping":
            return self._send(200, b"padphone-v3")
        self._send(200, b"PadPhone v3 ativo. Ligue pela app Android PadPhone.")

    def websocket(self):
        key = self.headers.get("Sec-WebSocket-Key")
        if not key:
            return self._send(400, b"WebSocket necessario")
        accept = base64.b64encode(
            hashlib.sha1((key + GUID).encode()).digest()).decode()
        self.send_response(101, "Switching Protocols")
        self.send_header("Upgrade", "websocket")
        self.send_header("Connection", "Upgrade")
        self.send_header("Sec-WebSocket-Accept", accept)
        self.end_headers()
        self.connection.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        try:
            self._text(f"ligado ({pad.kind} | L/R={pad.lr})")
            while True:
                b0, b1 = self._read(2)
                op = b0 & 0x0F
                ln = b1 & 0x7F
                if ln == 126:
                    ln = struct.unpack(">H", self._read(2))[0]
                elif ln == 127:
                    ln = struct.unpack(">Q", self._read(8))[0]
                masked = b1 & 0x80
                mask = self._read(4) if masked else b""
                payload = bytearray(self._read(ln)) if ln else bytearray()
                if masked:
                    for i in range(len(payload)):
                        payload[i] ^= mask[i % 4]
                if op == 8:
                    break
                if op == 9 and len(payload) < 126:
                    self.wfile.write(bytes([0x8A, len(payload)]) + bytes(payload))
                    self.wfile.flush()
                elif op == 1:
                    ack = process(payload.decode("utf-8", "ignore"))
                    if ack:
                        self._text(ack)
        except (EOFError, OSError):
            pass
        finally:
            with LOCK:
                pad.release_all()
            self.close_connection = True


def start_discovery_responder(http_port):
    hostname = socket.gethostname()
    def loop():
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind(("0.0.0.0", DISCOVERY_PORT))
        except OSError:
            return
        while True:
            try:
                data, addr = sock.recvfrom(256)
                if data.strip() == b"PADPHONE_DISCOVER":
                    reply = f"PADPHONE_HERE|{hostname}|{http_port}|{pad.kind} ({pad.lr})".encode("utf-8")
                    sock.sendto(reply, addr)
            except OSError:
                break
    t = threading.Thread(target=loop, daemon=True)
    t.start()


def local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("10.255.255.255", 1))
        return s.getsockname()[0]
    except OSError:
        return "IP_DO_PC"
    finally:
        s.close()


def main():
    global pad
    ap = argparse.ArgumentParser(description="PadPhone v3 - Servidor Linux")
    ap.add_argument("--teclado", action="store_true")
    ap.add_argument("--simular", action="store_true")
    ap.add_argument("--lr", choices=LR_MODES, default="ambos")
    ap.add_argument("--teste-lr", action="store_true")
    ap.add_argument("--porta", type=int, default=PORT)
    args = ap.parse_args()

    try:
        if args.simular:
            pad = SimBackend(args.lr)
        elif args.teclado:
            pad = KeyboardBackend(args.lr)
        else:
            pad = GamepadBackend(args.lr)
    except Exception as err:
        sys.exit(f"Erro ao criar comando virtual ({err}).\n"
                 "Execute:\n  sudo modprobe uinput\n  sudo chmod 666 /dev/uinput")

    print(f"PadPhone v3 ativo ({pad.kind}, L/R = {pad.lr}).")
    if args.teste_lr:
        try:
            while True:
                for b in ("l", "r"):
                    with LOCK:
                        print("Pulso:", pad.button(b, True), flush=True)
                    time.sleep(0.5)
                    with LOCK:
                        pad.button(b, False)
                    time.sleep(1.2)
        except KeyboardInterrupt:
            pass
        finally:
            pad.close()
        return

    start_discovery_responder(args.porta)
    print(f"IP para ligar na app Android: {local_ip()}:{args.porta}")
    server = Server(("0.0.0.0", args.porta), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        pad.close()


if __name__ == "__main__":
    main()
""".trimIndent().replace("\\\"\\\"\\\"", "\"\"\"")
}
