"""Single-user, authenticated snapshot store for Chi tieu Thanh Phat.

Bind only to loopback. Caddy provides HTTPS. The server treats snapshots as opaque
bytes; the Android client validates their schema before restoring anything.
"""

import hashlib
import hmac
import json
import os
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


DATA_DIR = Path(os.environ.get("BACKUP_DATA_DIR", "/var/lib/chi-tieu-backup"))
TOKEN_FILE = Path(os.environ.get("BACKUP_TOKEN_FILE", "/etc/chi-tieu-backup/token"))
MAX_BYTES = 20 * 1024 * 1024
KEEP = 10
WRITE_LOCK = threading.Lock()


class Handler(BaseHTTPRequestHandler):
    server_version = "ChiTieuBackup/1"

    def _authorized(self):
        expected = TOKEN_FILE.read_text(encoding="utf-8").strip().encode()
        supplied = self.headers.get("Authorization", "")
        if not supplied.startswith("Bearer "):
            return False
        return hmac.compare_digest(supplied[7:].encode(), expected)

    def _reply(self, code, body=b"", content_type="application/json"):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def _check(self):
        if self.path != "/v1/backup" or not self._authorized():
            self._reply(404 if self.path != "/v1/backup" else 401)
            return False
        return True

    def do_GET(self):
        if not self._check():
            return
        latest = DATA_DIR / "latest.json"
        if not latest.exists():
            self._reply(404, b'{"error":"no_backup"}')
            return
        self._reply(200, latest.read_bytes())

    def do_PUT(self):
        if not self._check():
            return
        try:
            length = int(self.headers.get("Content-Length", "-1"))
        except ValueError:
            length = -1
        if length < 2 or length > MAX_BYTES:
            self._reply(413)
            return
        body = self.rfile.read(length)
        if len(body) != length:
            self._reply(400)
            return
        try:
            payload = json.loads(body)
            if payload.get("format") != "chi-tieu-snapshot" or payload.get("version") != 1 or not isinstance(payload.get("tables"), dict):
                raise ValueError("format")
        except (UnicodeDecodeError, ValueError, AttributeError):
            self._reply(400, b'{"error":"invalid_snapshot"}')
            return
        DATA_DIR.mkdir(mode=0o700, parents=True, exist_ok=True)
        stamp = time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
        digest = hashlib.sha256(body).hexdigest()[:12]
        name = f"{stamp}-{digest}.json"
        with WRITE_LOCK:
            fd, tmp = tempfile.mkstemp(prefix=".incoming-", dir=DATA_DIR)
            try:
                with os.fdopen(fd, "wb") as out:
                    out.write(body)
                    out.flush()
                    os.fsync(out.fileno())
                os.chmod(tmp, 0o600)
                os.replace(tmp, DATA_DIR / name)
                fd, latest_tmp = tempfile.mkstemp(prefix=".latest-", dir=DATA_DIR)
                with os.fdopen(fd, "wb") as out:
                    out.write(body)
                    out.flush()
                    os.fsync(out.fileno())
                os.chmod(latest_tmp, 0o600)
                os.replace(latest_tmp, DATA_DIR / "latest.json")
                for old in sorted(DATA_DIR.glob("20*.json"))[:-KEEP]:
                    old.unlink()
            finally:
                if os.path.exists(tmp):
                    os.unlink(tmp)
        self._reply(201, json.dumps({"saved": name, "sha256": hashlib.sha256(body).hexdigest()}).encode())


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 8766), Handler).serve_forever()
