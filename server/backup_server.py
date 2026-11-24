"""Single-user, authenticated snapshot store for Chi tieu Thanh Phat.

Bind only to loopback. Caddy provides HTTPS. The server treats snapshots as opaque
bytes; the Android client validates their schema before restoring anything.
"""

import calendar
import datetime
import hashlib
import hmac
import json
import os
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

try:
    from zoneinfo import ZoneInfo
    VN_TZ = ZoneInfo("Asia/Ho_Chi_Minh")
except Exception:
    VN_TZ = datetime.timezone(datetime.timedelta(hours=7))


DATA_DIR = Path(os.environ.get("BACKUP_DATA_DIR", "/var/lib/chi-tieu-backup"))
TOKEN_FILE = Path(os.environ.get("BACKUP_TOKEN_FILE", "/etc/chi-tieu-backup/token"))
SECRETARY_KEY_FILE = Path(os.environ.get("SECRETARY_KEY_FILE", "/etc/chi-tieu-backup/secretary.key"))
MAX_BYTES = 20 * 1024 * 1024
KEEP = 10
WRITE_LOCK = threading.Lock()


def compute_installment_next_due(first_due_ms, paid_months, month_count):
    """Calculates next due date for a card installment in Asia/Ho_Chi_Minh.

    Matches CardInstallment.nextDueDate() in com/example/data/model/CardInstallment.kt:
    If paidMonths >= monthCount, returns None (all paid).
    Otherwise rolls firstDueDate forward by paidMonths, clamping the day of month
    to the target month's maximum days.
    """
    if paid_months >= month_count or month_count <= 0 or not first_due_ms:
        return None
    try:
        base_dt = datetime.datetime.fromtimestamp(first_due_ms / 1000.0, tz=VN_TZ)
    except (ValueError, OSError, OverflowError):
        return None
    day = base_dt.day
    total_months = (base_dt.month - 1) + paid_months
    target_year = base_dt.year + total_months // 12
    target_month = (total_months % 12) + 1
    max_day = calendar.monthrange(target_year, target_month)[1]
    target_day = min(day, max_day)
    return f"{target_year:04d}-{target_month:02d}-{target_day:02d}"


def compute_bill_next_due(next_due_ms, day_of_month=None, now_dt=None):
    """Calculates/formats next due date for recurring bills in Asia/Ho_Chi_Minh.

    In Room DB v9 (recurring_bills), nextDueDate is stored as epoch ms.
    When paid in Kotlin (FinanceRepository.payBill / nextDueDateAfter),
    it is advanced to the same day next month.
    """
    if next_due_ms and next_due_ms > 0:
        try:
            dt = datetime.datetime.fromtimestamp(next_due_ms / 1000.0, tz=VN_TZ)
            return dt.strftime("%Y-%m-%d")
        except (ValueError, OSError, OverflowError):
            pass
    if day_of_month is not None and 1 <= day_of_month <= 31:
        # Fallback using FinanceViewModel.addBill & nextDueDateAfter logic
        now = now_dt or datetime.datetime.now(VN_TZ)
        cur_max = calendar.monthrange(now.year, now.month)[1]
        clamped_day = min(day_of_month, cur_max)
        candidate = datetime.date(now.year, now.month, clamped_day)
        if candidate < now.date():
            total_months = (now.month - 1) + 1
            y = now.year + total_months // 12
            m = (total_months % 12) + 1
            max_d = calendar.monthrange(y, m)[1]
            candidate = datetime.date(y, m, min(day_of_month, max_d))
        return candidate.strftime("%Y-%m-%d")
    return None


def format_epoch_ms_to_date(epoch_ms):
    """Formats epoch ms as YYYY-MM-DD in Asia/Ho_Chi_Minh."""
    if not epoch_ms or epoch_ms <= 0:
        return None
    try:
        dt = datetime.datetime.fromtimestamp(epoch_ms / 1000.0, tz=VN_TZ)
        return dt.strftime("%Y-%m-%d")
    except (ValueError, OSError, OverflowError):
        return None


def format_timestamp_iso(ts):
    """Formats timestamp (epoch ms or seconds) as ISO 8601 with offset."""
    if ts is None:
        return None
    if isinstance(ts, (int, float)):
        try:
            sec = ts / 1000.0 if ts > 1e11 else float(ts)
            return datetime.datetime.fromtimestamp(sec, tz=VN_TZ).isoformat()
        except (ValueError, OSError, OverflowError):
            return None
    if isinstance(ts, str):
        return ts
    return None


def extract_secretary_finance_items(backup_data, now_dt=None):
    """Builds the response payload for GET /v1/secretary/finance from backup data."""
    tables = backup_data.get("tables", {})
    if not isinstance(tables, dict):
        tables = {}

    items = []

    # 1. Recurring bills: active only (isActive == 1)
    for row in tables.get("recurring_bills", []):
        if not isinstance(row, dict):
            continue
        is_active = row.get("isActive")
        if is_active is False or is_active == 0:
            continue
        title = str(row.get("name") or row.get("title") or "").strip()
        due_date = compute_bill_next_due(row.get("nextDueDate"), row.get("dayOfMonth"), now_dt)
        if not due_date:
            continue
        items.append({
            "kind": "bill",
            "title": title,
            "due_date": due_date,
            "status": "pending",
        })

    # 2. Card installments: upcoming unpaid periods (paidMonths < monthCount)
    for row in tables.get("card_installments", []):
        if not isinstance(row, dict):
            continue
        try:
            month_count = int(row.get("monthCount", 0))
            paid_months = int(row.get("paidMonths", 0))
        except (ValueError, TypeError):
            continue
        due_date = compute_installment_next_due(row.get("firstDueDate"), paid_months, month_count)
        if not due_date:
            continue
        title = str(row.get("name") or row.get("title") or "").strip()
        items.append({
            "kind": "installment",
            "title": title,
            "due_date": due_date,
            "status": "pending",
        })

    # 3. Debts: unsettled debts with due date
    for row in tables.get("debts", []):
        if not isinstance(row, dict):
            continue
        is_settled = row.get("isSettled")
        if is_settled is True or is_settled == 1:
            continue
        try:
            remaining = float(row.get("remainingAmount", 0.0))
        except (ValueError, TypeError):
            remaining = 0.0
        if remaining <= 0.001:
            continue
        due_val = row.get("dueDate")
        if not due_val:
            continue
        due_date = format_epoch_ms_to_date(due_val)
        if not due_date:
            continue
        title = str(row.get("personName") or row.get("name") or row.get("title") or "").strip()
        items.append({
            "kind": "debt",
            "title": title,
            "due_date": due_date,
            "status": "pending",
        })

    # Sort items by due_date ascending, then kind, then title
    items.sort(key=lambda x: (x["due_date"], x["kind"], x["title"]))

    truncated = False
    if len(items) > 2000:
        items = items[:2000]
        truncated = True

    backup_at = format_timestamp_iso(backup_data.get("createdAt"))
    generated_at = (now_dt or datetime.datetime.now(VN_TZ)).isoformat()

    result = {
        "generated_at": generated_at,
        "backup_at": backup_at,
        "items": items,
    }
    if truncated:
        result["truncated"] = True
    return result


class Handler(BaseHTTPRequestHandler):
    server_version = "ChiTieuBackup/1"

    def _authorized(self):
        expected = Path(TOKEN_FILE).read_text(encoding="utf-8").strip().encode()
        supplied = self.headers.get("Authorization", "")
        if not supplied.startswith("Bearer "):
            return False
        return hmac.compare_digest(supplied[7:].encode(), expected)

    def _reply(self, code, body=b"", content_type="application/json", headers=None):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        if headers:
            for k, v in headers.items():
                self.send_header(k, v)
        self.end_headers()
        self.wfile.write(body)

    def _clean_path(self):
        return self.path.split("?")[0].rstrip("/")

    def _secretary_authorized(self):
        """Validates X-Secretary-Key header against SECRETARY_KEY_FILE.
        Returns: (authorized: bool, status_code: int)
        - If file is missing or empty: (False, 503)
        - If key is missing or wrong: (False, 401)
        - If valid: (True, 200)
        """
        key_file = Path(SECRETARY_KEY_FILE)
        try:
            if not key_file.is_file():
                return False, 503
            expected = key_file.read_text(encoding="utf-8").strip().encode("utf-8")
            if not expected:
                return False, 503
        except OSError:
            return False, 503

        supplied = self.headers.get("X-Secretary-Key", "").strip().encode("utf-8")
        if not supplied or not hmac.compare_digest(supplied, expected):
            return False, 401
        return True, 200

    def _handle_secretary_finance_get(self):
        authorized, code = self._secretary_authorized()
        if not authorized:
            self._reply(code, b'{"error":"service_unavailable"}' if code == 503 else b'{"error":"unauthorized"}')
            return

        latest = Path(DATA_DIR) / "latest.json"
        if not latest.is_file():
            # Chưa có bản sao lưu: trả rỗng để bot cảnh báo thay vì dừng cả bản tin.
            empty = {"generated_at": datetime.datetime.now(VN_TZ).isoformat(), "backup_at": None, "items": []}
            self._reply(200, json.dumps(empty).encode("utf-8"))
            return

        try:
            raw = latest.read_bytes()
            data = json.loads(raw.decode("utf-8"))
            payload = extract_secretary_finance_items(data)
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self._reply(200, body, content_type="application/json; charset=utf-8")
        except Exception:
            self._reply(500, b'{"error":"internal_error"}')

    def _check(self):
        if self.path != "/v1/backup" or not self._authorized():
            self._reply(404 if self.path != "/v1/backup" else 401)
            return False
        return True

    def do_GET(self):
        if self._clean_path() == "/v1/secretary/finance":
            self._handle_secretary_finance_get()
            return
        if not self._check():
            return
        latest = Path(DATA_DIR) / "latest.json"
        if not latest.exists():
            self._reply(404, b'{"error":"no_backup"}')
            return
        self._reply(200, latest.read_bytes())

    def do_PUT(self):
        if self._clean_path() == "/v1/secretary/finance":
            self._reply(405, b'{"error":"method_not_allowed"}', headers={"Allow": "GET"})
            return
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
        data_dir = Path(DATA_DIR)
        data_dir.mkdir(mode=0o700, parents=True, exist_ok=True)
        stamp = time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
        digest = hashlib.sha256(body).hexdigest()[:12]
        name = f"{stamp}-{digest}.json"
        with WRITE_LOCK:
            fd, tmp = tempfile.mkstemp(prefix=".incoming-", dir=data_dir)
            try:
                with os.fdopen(fd, "wb") as out:
                    out.write(body)
                    out.flush()
                    os.fsync(out.fileno())
                os.chmod(tmp, 0o600)
                os.replace(tmp, data_dir / name)
                fd, latest_tmp = tempfile.mkstemp(prefix=".latest-", dir=data_dir)
                with os.fdopen(fd, "wb") as out:
                    out.write(body)
                    out.flush()
                    os.fsync(out.fileno())
                os.chmod(latest_tmp, 0o600)
                os.replace(latest_tmp, data_dir / "latest.json")
                for old in sorted(data_dir.glob("20*.json"))[:-KEEP]:
                    old.unlink()
            finally:
                if os.path.exists(tmp):
                    os.unlink(tmp)
        self._reply(201, json.dumps({"saved": name, "sha256": hashlib.sha256(body).hexdigest()}).encode())

    def do_POST(self):
        if self._clean_path() == "/v1/secretary/finance":
            self._reply(405, b'{"error":"method_not_allowed"}', headers={"Allow": "GET"})
            return
        self._reply(404)

    def do_DELETE(self):
        if self._clean_path() == "/v1/secretary/finance":
            self._reply(405, b'{"error":"method_not_allowed"}', headers={"Allow": "GET"})
            return
        self._reply(404)

    def _secretary_other_method(self):
        # Hợp đồng: mọi phương thức ngoài GET trên đường thư ký -> 405. Đường khác giữ như cũ (501).
        if self._clean_path() == "/v1/secretary/finance":
            body = b"" if self.command == "HEAD" else b'{"error":"method_not_allowed"}'
            self._reply(405, body, headers={"Allow": "GET"})
            return
        self.send_error(501, f"Unsupported method ({self.command!r})")

    do_HEAD = do_OPTIONS = do_PATCH = do_TRACE = _secretary_other_method


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 8766), Handler).serve_forever()
