# LazyMouse setup

## PC server

Run the app:

```bash
python3 ~/dv/lazymouse/server/app.py
```

A small window opens with a QR code, the address, and the 6-character key.
It runs the server for you. Close the window to stop it.

Two tabs:

- **same wifi**: QR points at the PC's LAN address (`192.168.1.100:8098`).
  Lowest latency. Phone and PC must be on the same network, and no VPN on the
  phone may be capturing the LAN route (see "VPN" below).
- **anywhere**: opens a Cloudflare quick tunnel and the QR points at a
  `*.trycloudflare.com` address over `wss`. Works from any network, no port
  forwarding, but adds relay latency (feels laggy for fast movement).

### Make it a launchable app

```bash
~/dv/lazymouse/server/install.sh
```

Adds a "LazyMouse" entry with the 98k icon to the app launcher, and optionally
a systemd user service that keeps the server running on login (open the window
any time to see the QR).

Requirements (all present on this box): python `evdev`, `websockets`, `aiohttp`,
`qrcode`, `tkinter`; the `wtype`, `qrencode` and `cloudflared` binaries.

### VPN

If the phone runs a VPN with a kill switch (Surfshark, etc.), it routes LAN
traffic to the PC into the tunnel and the "same wifi" mode cannot connect.
Either disconnect the VPN, add LazyMouse to the VPN's split-tunnel / bypass
list, or use the "anywhere" tab (which the VPN passes fine).

## Phone app

```bash
cd ~/dv/lazymouse/android
./gw :app:assembleDebug
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`./gw` is a shim; the stock `gradlew` is broken on this system.

## Using it

1. Open the server window on the PC, pick a tab.
2. On the phone: scan the QR, or type the address and key by hand.
   A numeric IP connects over plain `ws`; a hostname connects over `wss`.
3. Drag to move. Tap to left click. Two-finger tap for right click.
   Two-finger drag to scroll. Long-press for drag-lock.
   Bottom bar is hold Left / Mid / Right. KEYS opens the keyboard.
   CFG opens settings.
