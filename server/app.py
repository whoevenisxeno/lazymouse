import queue
import re
import shutil
import signal
import socket
import subprocess
import sys
import threading
import tkinter as tk
from pathlib import Path

import pair




HERE = Path(__file__).resolve().parent
BG = "#0A0910"
INK = "#F1EEF7"
DIM = "#7B7489"
VIOLET = "#A78BFA"
GO = "#3EE0D2"
DANGER = "#FF6B8B"
MONO = "JetBrains Mono"

TOKEN = pair.load_or_create_token()
IP = pair.lan_ip()
QR_PNG = HERE / ".qr.png"
CLOUDFLARED = shutil.which("cloudflared") or str(Path.home() / ".local/bin/cloudflared")


def make_qr(uri: str):
    subprocess.run(
        ["qrencode", "-t", "PNG", "-o", str(QR_PNG), "-s", "6", "-m", "2",
         "--foreground", "0A0910", "--background", "F1EEF7", uri],
        check=True,
    )


def port_open(p: int) -> bool:
    with socket.socket() as s:
        s.settimeout(0.3)
        return s.connect_ex(("127.0.0.1", p)) == 0


class Server:
    def __init__(self):
        self.proc = None
        self.owns = False
        self._q = queue.Queue()

    def start(self):
        if port_open(8098):
            return
        self.owns = True
        self.proc = subprocess.Popen(
            [sys.executable, "-u", str(HERE / "server.py")],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
        )
        threading.Thread(target=self._pump, daemon=True).start()

    def _pump(self):
        for line in self.proc.stdout:
            self._q.put(line.rstrip())

    def lines(self):
        while True:
            try:
                yield self._q.get_nowait()
            except queue.Empty:
                return

    def dead(self):
        return self.owns and self.proc and self.proc.poll() is not None

    def stop(self):
        if self.proc and self.proc.poll() is None:
            self.proc.send_signal(signal.SIGINT)
            try:
                self.proc.wait(timeout=2)
            except subprocess.TimeoutExpired:
                self.proc.kill()


class Tunnel:
    def __init__(self):
        self.proc = None
        self.url = None
        self._q = queue.Queue()

    def start(self):
        if self.proc:
            return
        self.proc = subprocess.Popen(
            [CLOUDFLARED, "tunnel", "--url", "http://localhost:8098", "--no-autoupdate"],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
        )
        threading.Thread(target=self._pump, daemon=True).start()

    def _pump(self):
        for line in self.proc.stdout:
            m = re.search(r"https://[a-z0-9-]+\.trycloudflare\.com", line)
            if m and not self.url:
                self._q.put(m.group(0))

    def host(self):
        try:
            self.url = self._q.get_nowait().removeprefix("https://")
        except queue.Empty:
            pass
        return self.url

    def stop(self):
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()


class App:
    def __init__(self):
        self.r = tk.Tk()
        self.r.title("LazyMouse")
        self.r.configure(bg=BG)
        self.r.resizable(False, False)
        try:
            self._ic = tk.PhotoImage(file=str(HERE / "assets" / "icon-128.png"))
            self.r.iconphoto(True, self._ic)
        except tk.TclError:
            pass

        self.mode = "lan"
        self.server = Server()
        self.tunnel = Tunnel()
        self.server.start()

        tk.Label(self.r, text="lazymouse", bg=BG, fg=INK,
                 font=(MONO, 22, "bold")).pack(pady=(24, 0))
        tk.Label(self.r, text="98k  ·  trackpad server", bg=BG, fg=DIM,
                 font=(MONO, 10)).pack(pady=(2, 16))

        tabs = tk.Frame(self.r, bg=BG)
        tabs.pack()
        self.tab_lan = self._tab(tabs, "same wifi", lambda: self.switch("lan"))
        self.tab_rem = self._tab(tabs, "anywhere", lambda: self.switch("remote"))

        self.card = tk.Frame(self.r, bg="#F1EEF7")
        self.card.pack(padx=28, pady=(14, 0))
        self.qr_label = tk.Label(self.card, bg="#F1EEF7", bd=0)
        self.qr_label.pack(padx=10, pady=10)

        self.addr = tk.Label(self.r, text="", bg=BG, fg=INK, font=(MONO, 13))
        self.addr.pack(pady=(16, 3))
        self.key = tk.Label(self.r, text=f"key  {TOKEN}", bg=BG, fg=VIOLET,
                            font=(MONO, 12), cursor="hand2")
        self.key.pack()
        self.key.bind("<Button-1>", self.copy_key)
        self.status = tk.Label(self.r, text="  starting", bg=BG, fg=DIM, font=(MONO, 10))
        self.status.pack(pady=(16, 22))

        self.r.protocol("WM_DELETE_WINDOW", self.quit)
        self.switch("remote" if "remote" in sys.argv else "lan")
        self.r.after(200, self.tick)

    def _tab(self, parent, text, cmd):
        b = tk.Label(parent, text=text, bg=BG, fg=DIM, font=(MONO, 11),
                     cursor="hand2", padx=14, pady=6)
        b.pack(side="left", padx=4)
        b.bind("<Button-1>", lambda _: cmd())
        return b

    def show_qr(self, uri, addr_text):
        make_qr(uri)
        self._qr = tk.PhotoImage(file=str(QR_PNG))
        self.qr_label.config(image=self._qr)
        self.addr.config(text=addr_text)

    def switch(self, mode):
        self.mode = mode
        self.tab_lan.config(fg=INK if mode == "lan" else DIM)
        self.tab_rem.config(fg=INK if mode == "remote" else DIM)
        if mode == "lan":
            self.show_qr(pair.pair_uri(IP, 8098, TOKEN), f"{IP} : 8098")
            self.set_status("waiting for a phone on this wifi", DIM)
        else:
            host = self.tunnel.host()
            if host:
                self.show_qr(f"lazymouse://{host}:443/{TOKEN}", host)
                self.set_status("reachable from anywhere", GO)
            else:
                self.tunnel.start()
                self.addr.config(text="opening secure tunnel...")
                self.set_status("this takes a few seconds", DIM)

    def set_status(self, text, color):
        try:
            self.status.config(text="  " + text, fg=color)
        except tk.TclError:
            pass

    def copy_key(self, _=None):
        self.r.clipboard_clear()
        self.r.clipboard_append(TOKEN)
        self.key.config(text="copied")
        self.r.after(900, lambda: self.key.config(text=f"key  {TOKEN}"))

    def tick(self):
        if getattr(self, "_quitting", False):
            return
        for line in self.server.lines():
            if "client connected" in line:
                self.set_status("phone linked", GO)
            elif "client disconnected" in line:
                self.set_status("phone disconnected", DIM)
        if self.server.dead():
            self.set_status("server stopped, reopen the app", DANGER)
            return
        if self.mode == "remote" and not self.tunnel.url:
            host = self.tunnel.host()
            if host:
                self.show_qr(f"lazymouse://{host}:443/{TOKEN}", host)
                self.set_status("reachable from anywhere", GO)
        self.r.after(200, self.tick)

    def quit(self):
        self._quitting = True
        self.tunnel.stop()
        self.server.stop()
        try:
            self.r.destroy()
        except tk.TclError:
            pass

    def run(self):
        self.r.mainloop()


if __name__ == "__main__":
    app = App()
    for sig in (signal.SIGTERM, signal.SIGINT):
        signal.signal(sig, lambda *_: app.quit())
    app.run()
