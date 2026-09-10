# LazyMouse

Use an Android phone as a wireless trackpad for a Linux PC, over LAN.

- **server/** is a Python asyncio WebSocket server. It injects a virtual mouse via
  `/dev/uinput` (evdev) and types via `wtype`. Wayland and Hyprland native. It advertises
  itself on the LAN over mDNS (`_lazymouse._tcp`).
- **android/** is a Kotlin + Compose app in the 98k style: black, JetBrains Mono,
  terminal-flavoured, purple used only as an accent. Trackpad surface with move, tap,
  two-finger right click, two-finger scroll and long-press drag-lock, plus Left/Mid/Right
  hold buttons, an on-demand keyboard, a settings sheet, and mDNS auto-detect of nearby
  servers.

Pairing is a 6-character key, shown by the server as a QR code. One client at a time.

See `docs/SETUP.md`.

Built by the 98k Team.
