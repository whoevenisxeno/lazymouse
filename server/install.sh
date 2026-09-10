#!/usr/bin/env bash
# Install LazyMouse as a launchable desktop app for the current user. No sudo.
set -e
cd "$(dirname "$0")"

APPS="$HOME/.local/share/applications"
ICONS="$HOME/.local/share/icons/hicolor"
mkdir -p "$APPS"

for s in 48 64 128 256; do
    f="assets/icon.png"
    [ "$s" != 256 ] && f="assets/icon-$s.png"
    d="$ICONS/${s}x${s}/apps"
    mkdir -p "$d"
    cp "$f" "$d/lazymouse.png"
done

sed "s|%HOME%|$HOME|g" lazymouse.desktop > "$APPS/lazymouse.desktop"
chmod +x "$APPS/lazymouse.desktop"

command -v update-desktop-database >/dev/null && update-desktop-database "$APPS" 2>/dev/null || true
command -v gtk-update-icon-cache >/dev/null && gtk-update-icon-cache -f "$ICONS" 2>/dev/null || true

echo "installed. search 'LazyMouse' in your launcher, or run: python3 $PWD/app.py"
echo
read -rp "also start it on login (systemd user service)? [y/N] " a
if [ "$a" = y ] || [ "$a" = Y ]; then
    systemctl --user enable --now "$PWD/systemd/lazymouse.service"
    echo "enabled. it runs headless in the background; open the app window to see the QR."
fi
