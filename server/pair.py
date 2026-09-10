import io
import secrets
import socket
import string
from pathlib import Path

import qrcode

_ALPHABET = string.ascii_uppercase + string.digits
_TOKEN_PATH = Path.home() / ".config" / "lazymouse" / "token"


def load_or_create_token() -> str:
    if _TOKEN_PATH.exists():
        tok = _TOKEN_PATH.read_text().strip()
        if tok:
            return tok
    tok = "".join(secrets.choice(_ALPHABET) for _ in range(6))
    _TOKEN_PATH.parent.mkdir(parents=True, exist_ok=True)
    _TOKEN_PATH.write_text(tok + "\n")
    _TOKEN_PATH.chmod(0o600)
    return tok


def lan_ip() -> str:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("192.168.1.1", 80))
        return s.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        s.close()


def pair_uri(host: str, port: int, token: str) -> str:
    return f"lazymouse://{host}:{port}/{token}"


def ascii_qr(data: str) -> str:
    q = qrcode.QRCode(border=1)
    q.add_data(data)
    q.make(fit=True)
    buf = io.StringIO()
    q.print_ascii(out=buf, invert=True)
    return buf.getvalue()
