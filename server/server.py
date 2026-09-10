import asyncio
import json
import socket

from aiohttp import web
from websockets.asyncio.server import serve

import pair
from input import Keyboard, Pointer

WS_PORT = 8098
HTTP_PORT = 8099

pointer = Pointer()
keyboard = Keyboard()
TOKEN = pair.load_or_create_token()
HOST_IP = pair.lan_ip()
HOSTNAME = socket.gethostname()
URI = pair.pair_uri(HOST_IP, WS_PORT, TOKEN)

_active = {"ws": None}


def _dispatch(msg: dict):
    t = msg.get("t")
    if t == "m":
        pointer.move(float(msg.get("x", 0)), float(msg.get("y", 0)))
    elif t == "b":
        pointer.button(msg.get("btn", "l"), bool(msg.get("down")))
    elif t == "click":
        pointer.click(msg.get("btn", "l"))
    elif t == "scroll":
        pointer.scroll(float(msg.get("y", 0)), float(msg.get("x", 0)))
    elif t == "key":
        if "text" in msg:
            keyboard.text(str(msg["text"]))
        elif "special" in msg:
            keyboard.special(str(msg["special"]), int(msg.get("count", 1)))


async def _handle_ws(ws):
    try:
        raw = await asyncio.wait_for(ws.recv(), timeout=10)
        hello = json.loads(raw)
    except (asyncio.TimeoutError, ValueError):
        await ws.close(code=4000, reason="bad hello")
        return
    if hello.get("t") != "hello" or hello.get("token") != TOKEN:
        await ws.close(code=4001, reason="bad token")
        return

    prev = _active["ws"]
    if prev is not None:
        await prev.close(code=4002, reason="replaced")
    _active["ws"] = ws
    await ws.send(json.dumps({"t": "welcome", "host": HOSTNAME}))
    print(f"[lazymouse] client connected: {ws.remote_address[0]}")

    try:
        async for raw in ws:
            try:
                _dispatch(json.loads(raw))
            except (ValueError, TypeError, KeyError):
                continue
    finally:
        if _active["ws"] is ws:
            _active["ws"] = None
        pointer.release_all()
        print("[lazymouse] client disconnected")


_PAGE = """<!doctype html><meta charset=utf-8>
<meta name=viewport content="width=device-width,initial-scale=1">
<title>LazyMouse pairing</title>
<style>body{{background:#0b0b0f;color:#fff;font:16px system-ui;text-align:center;
padding:8vh 6vw}}code{{background:#1c1c22;padding:2px 8px;border-radius:8px}}
svg{{width:min(70vw,340px);height:auto;background:#fff;padding:12px;border-radius:20px;
margin:24px 0}}</style>
<h2>LazyMouse</h2><p>Scan in the app, or enter manually:</p>
<p><code>{ip}:{port}</code> &nbsp; token <code>{token}</code></p>{qr}
"""


async def _pair_page(_req):
    import subprocess
    try:
        svg = subprocess.run(["qrencode", "-t", "SVG", "-o", "-", URI],
                             capture_output=True, text=True, check=True).stdout
        svg = svg[svg.find("<svg"):]
    except Exception:
        svg = "<p>(install qrencode for a QR)</p>"
    return web.Response(
        text=_PAGE.format(ip=HOST_IP, port=WS_PORT, token=TOKEN, qr=svg),
        content_type="text/html")


async def main():
    app = web.Application()
    app.router.add_get("/", _pair_page)
    runner = web.AppRunner(app)
    await runner.setup()
    await web.TCPSite(runner, "0.0.0.0", HTTP_PORT).start()

    print("=" * 46)
    print("  LazyMouse server")
    print(f"  connect:  {HOST_IP}:{WS_PORT}")
    print(f"  token:    {TOKEN}")
    print(f"  pair web: http://{HOST_IP}:{HTTP_PORT}/")
    print("=" * 46)
    print(pair.ascii_qr(URI))

    async with serve(_handle_ws, "0.0.0.0", WS_PORT, ping_interval=20):
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
    finally:
        pointer.close()
