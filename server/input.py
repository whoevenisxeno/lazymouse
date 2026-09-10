import shutil
import subprocess

from evdev import UInput, ecodes as e

_BTN = {"l": e.BTN_LEFT, "r": e.BTN_RIGHT, "m": e.BTN_MIDDLE}

_POINTER_CAPS = {
    e.EV_KEY: list(_BTN.values()),
    e.EV_REL: [
        e.REL_X, e.REL_Y,
        e.REL_WHEEL, e.REL_WHEEL_HI_RES,
        e.REL_HWHEEL, e.REL_HWHEEL_HI_RES,
    ],
}

_SPECIAL = {
    "backspace": "BackSpace", "enter": "Return", "tab": "Tab", "escape": "Escape",
    "delete": "Delete", "home": "Home", "end": "End",
    "left": "Left", "right": "Right", "up": "Up", "down": "Down",
    "pageup": "Prior", "pagedown": "Next", "space": "space",
}


class Pointer:
    def __init__(self):
        self._ui = UInput(_POINTER_CAPS, name="LazyMouse Virtual Pointer", version=1)
        self._held = set()

    def move(self, dx: float, dy: float):
        idx, idy = int(round(dx)), int(round(dy))
        if idx:
            self._ui.write(e.EV_REL, e.REL_X, idx)
        if idy:
            self._ui.write(e.EV_REL, e.REL_Y, idy)
        if idx or idy:
            self._ui.syn()

    def button(self, btn: str, down: bool):
        code = _BTN.get(btn)
        if code is None:
            return
        self._ui.write(e.EV_KEY, code, 1 if down else 0)
        self._ui.syn()
        if down:
            self._held.add(code)
        else:
            self._held.discard(code)

    def click(self, btn: str):
        self.button(btn, True)
        self.button(btn, False)

    def scroll(self, dy: float, dx: float = 0.0):
        if dy:
            self._ui.write(e.EV_REL, e.REL_WHEEL_HI_RES, int(round(dy * 120)))
            self._ui.write(e.EV_REL, e.REL_WHEEL, int(round(dy)))
        if dx:
            self._ui.write(e.EV_REL, e.REL_HWHEEL_HI_RES, int(round(dx * 120)))
            self._ui.write(e.EV_REL, e.REL_HWHEEL, int(round(dx)))
        if dy or dx:
            self._ui.syn()

    def release_all(self):
        for code in list(self._held):
            self._ui.write(e.EV_KEY, code, 0)
        if self._held:
            self._ui.syn()
        self._held.clear()

    def close(self):
        self.release_all()
        self._ui.close()


class Keyboard:
    def __init__(self):
        self._wtype = shutil.which("wtype")

    @property
    def available(self) -> bool:
        return self._wtype is not None

    def text(self, s: str):
        if not s or not self._wtype:
            return
        subprocess.run([self._wtype, "--", s], check=False)

    def special(self, name: str, count: int = 1):
        keysym = _SPECIAL.get(name)
        if not keysym or not self._wtype:
            return
        args = [self._wtype]
        for _ in range(max(1, count)):
            args += ["-k", keysym]
        subprocess.run(args, check=False)
