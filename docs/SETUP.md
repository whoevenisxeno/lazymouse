# LazyMouse setup

## PC server

```bash
cd ~/dv/lazymouse/server
./run.sh
```

It prints an IP, a 6-character key, and a QR code. Leave it running (or install the
user service below). No sudo needed. `/dev/uinput` already grants this user access
on this box.

Autostart:

```bash
systemctl --user enable --now ~/dv/lazymouse/server/systemd/lazymouse.service
```

Requirements (all already present on this box): python `evdev`, `websockets`,
`aiohttp`, `qrcode`; the `wtype`, `qrencode` and `avahi-publish-service` binaries.

## Phone app

Build and install over USB (debugging already authorised on RZCW9224NWA):

```bash
cd ~/dv/lazymouse/android
./gw :app:assembleDebug
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`./gw` is a shim. The stock `gradlew` script is broken on this system
(`DEFAULT_JVM_OPTS` mangling), so it calls the wrapper jar directly.

Cable-free installs afterwards:

```bash
adb tcpip 5555
adb connect <phone-wifi-ip>:5555
```

## Using it

1. Phone and PC on the same Wi-Fi / LAN.
2. Open LazyMouse. It scans the local network and lists any servers it finds
   ("detected on lan"); tap one to fill the host and port. Or type `IP : 8098`,
   or scan the QR. Enter the key, connect.
3. Drag to move. Tap to left click. Two-finger tap for right click.
   Two-finger drag to scroll. Long-press for drag-lock (tap to drop).
   Bottom bar is hold-to-hold Left / Mid / Right. KEYS opens the keyboard.
   CFG opens settings (sensitivity, acceleration, scroll speed, gesture toggles).
