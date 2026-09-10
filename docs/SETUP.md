# LazyMouse setup

## PC server

```bash
cd ~/dv/lazymouse/server
./run.sh
```

It prints an IP, a 6-char token, and a QR code. Leave it running (or install the
user service below). No sudo needed — `/dev/uinput` already grants this user access.

Autostart:

```bash
systemctl --user enable --now ~/dv/lazymouse/server/systemd/lazymouse.service
```

Requirements (all already present on this box): python `evdev`, `websockets`,
`aiohttp`, `qrcode`; the `wtype` and `qrencode` binaries.

## Phone app

Build + install over USB (debugging already authorised on RZCW9224NWA):

```bash
cd ~/dv/lazymouse/android
./gw :app:assembleDebug
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`./gw` is a shim — the stock `gradlew` script is broken on this system
(`DEFAULT_JVM_OPTS` mangling), so it calls the wrapper jar directly.

Cable-free installs afterwards:

```bash
adb tcpip 5555
adb connect <phone-wifi-ip>:5555
```

## Using it

1. Phone and PC on the same Wi-Fi / LAN.
2. Open LazyMouse, scan the QR (or type `IP : 8098` + token), Connect.
3. Drag = move. Tap = left click. Two-finger tap = right click.
   Two-finger drag = scroll. Long-press = drag-lock (tap to drop).
   Bottom bar = hold-to-hold Left / Mid / Right. "Keys" = pop the keyboard.
