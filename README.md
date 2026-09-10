# LazyMouse

Use an Android phone as a wireless trackpad for a Linux PC, over LAN.

- **server/** — Python asyncio WebSocket server, injects a virtual mouse via
  `/dev/uinput` (evdev) and types via `wtype`. Wayland/Hyprland-native.
- **android/** — Kotlin + Compose app. Glassmorphism UI, aurora background.
  Trackpad surface with move / tap / two-finger right-click / two-finger scroll /
  long-press drag-lock, plus Left/Mid/Right hold buttons and an on-demand keyboard.

Pairing is a 6-char token (shown as a QR by the server). One client at a time.

See `docs/SETUP.md`.

Built by the 98k Team.
