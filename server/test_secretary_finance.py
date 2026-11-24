"""Tests for GET /v1/secretary/finance endpoint in backup_server.py.

Can be run with:
    py -3 -I -m unittest D:\\chi-ti-u\\server\\test_secretary_finance.py
or from the repository directory:
    python -m unittest server/test_secretary_finance.py
"""

import datetime
import json
import os
import re
import shutil
import sys
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path

# Add server directory to sys.path so backup_server can be imported
SERVER_DIR = Path(__file__).resolve().parent
if str(SERVER_DIR) not in sys.path:
    sys.path.insert(0, str(SERVER_DIR))

import backup_server


class TestSecretaryFinanceEndpoint(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.mkdtemp(prefix="chi-tieu-test-")
        cls.data_dir = Path(cls.temp_dir) / "data"
        cls.data_dir.mkdir(parents=True, exist_ok=True)

        cls.token_file = Path(cls.temp_dir) / "token"
        cls.token_content = "test-backup-token-secret-12345"
        cls.token_file.write_text(cls.token_content, encoding="utf-8")

        cls.key_file = Path(cls.temp_dir) / "secretary.key"
        cls.key_content = "test-secretary-key-abcde-67890"
        cls.key_file.write_text(cls.key_content, encoding="utf-8")

        # Sample snapshot structure matching Room DB v9 & VpsBackup.kt
        # 1732123456000 = 2024-11-20T17:24:16+00:00 = 2024-11-21T00:24:16+07:00
        cls.backup_created_at = 1732123456000
        cls.sample_backup = {
            "format": "chi-tieu-snapshot",
            "version": 1,
            "databaseVersion": 9,
            "createdAt": cls.backup_created_at,
            "tables": {
                "recurring_bills": [
                    {
                        "id": 1,
                        "name": "Tiền điện sinh hoạt",
                        "amount": 750000.0,
                        "accountId": 1,
                        "categoryId": 10,
                        "dayOfMonth": 15,
                        # 1732500000000 = 2024-11-25 09:00:00+07:00
                        "nextDueDate": 1732500000000,
                        "note": "Hóa đơn EVN tháng 11",
                        "isActive": 1,
                    },
                    {
                        "id": 2,
                        "name": "Gói Internet cũ",
                        "amount": 220000.0,
                        "accountId": 1,
                        "categoryId": 11,
                        "dayOfMonth": 20,
                        "nextDueDate": 1732500000000,
                        "note": "Đã cắt mạng",
                        "isActive": 0,  # Inactive bill -> MUST be excluded
                    },
                ],
                "card_installments": [
                    {
                        "id": 1,
                        "cardId": 2,
                        "name": "MacBook Air M2",
                        "totalAmount": 24000000.0,
                        "monthlyAmount": 2000000.0,
                        "monthCount": 12,
                        "paidMonths": 4,  # Unfinished -> MUST be included
                        # 1705294800000 = 2024-01-15 12:00:00+07:00
                        # 4 months added -> 2024-05-15
                        "firstDueDate": 1705294800000,
                        "createdAt": 1705000000000,
                    },
                    {
                        "id": 2,
                        "cardId": 2,
                        "name": "Tủ lạnh Panasonic",
                        "totalAmount": 12000000.0,
                        "monthlyAmount": 2000000.0,
                        "monthCount": 6,
                        "paidMonths": 6,  # Fully paid (paidMonths >= monthCount) -> MUST be excluded
                        "firstDueDate": 1705294800000,
                        "createdAt": 1705000000000,
                    },
                ],
                "debts": [
                    {
                        "id": 1,
                        "type": "I_OWE",
                        "personName": "Anh Minh",
                        "phone": "0912345678",
                        "initialAmount": 10000000.0,
                        "remainingAmount": 5000000.0,
                        # 1732950000000 = 2024-11-30 14:00:00+07:00
                        "dueDate": 1732950000000,
                        "note": "Vay mua đồ gia dụng",
                        "isSettled": 0,  # Unsettled with dueDate -> MUST be included
                        "createdAt": 1730000000000,
                    },
                    {
                        "id": 2,
                        "type": "OWED_TO_ME",
                        "personName": "Bạn Hùng",
                        "phone": "0987654321",
                        "initialAmount": 3000000.0,
                        "remainingAmount": 0.0,
                        "dueDate": 1732950000000,
                        "note": "Đã trả xong",
                        "isSettled": 1,  # Settled debt -> MUST be excluded
                        "createdAt": 1730000000000,
                    },
                    {
                        "id": 3,
                        "type": "I_OWE",
                        "personName": "Chị Lan",
                        "phone": "0933333333",
                        "initialAmount": 2000000.0,
                        "remainingAmount": 2000000.0,
                        "dueDate": None,  # No due date -> MUST be excluded
                        "note": "Mượn không hẹn ngày",
                        "isSettled": 0,
                        "createdAt": 1730000000000,
                    },
                ],
            },
        }

        cls.latest_file = cls.data_dir / "latest.json"
        cls.latest_file.write_text(json.dumps(cls.sample_backup), encoding="utf-8")

        # Patch module-level paths
        backup_server.DATA_DIR = cls.data_dir
        backup_server.TOKEN_FILE = cls.token_file
        backup_server.SECRETARY_KEY_FILE = cls.key_file

        # Start live HTTP server on ephemeral port (0 = OS chooses free port)
        cls.httpd = ThreadingHTTPServer(("127.0.0.1", 0), backup_server.Handler)
        cls.server_port = cls.httpd.server_address[1]
        cls.server_thread = threading.Thread(target=cls.httpd.serve_forever, daemon=True)
        cls.server_thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()
        cls.httpd.server_close()
        cls.server_thread.join(timeout=3.0)
        shutil.rmtree(cls.temp_dir, ignore_errors=True)

    def setUp(self):
        # Reset SECRETARY_KEY_FILE to the valid test file before each test
        backup_server.SECRETARY_KEY_FILE = self.key_file
        self.key_file.write_text(self.key_content, encoding="utf-8")

    def _request(self, method, path, headers=None, body=None):
        url = f"http://127.0.0.1:{self.server_port}{path}"
        req = urllib.request.Request(url, data=body, method=method)
        if headers:
            for k, v in headers.items():
                req.add_header(k, v)
        try:
            with urllib.request.urlopen(req) as resp:
                return resp.status, dict(resp.headers), resp.read()
        except urllib.error.HTTPError as e:
            return e.code, dict(e.headers), e.read()

    def test_503_when_key_file_missing(self):
        """When SECRETARY_KEY_FILE does not exist, endpoint must return 503."""
        missing_file = Path(self.temp_dir) / "nonexistent.key"
        backup_server.SECRETARY_KEY_FILE = missing_file

        status, headers, body = self._request("GET", "/v1/secretary/finance", {"X-Secretary-Key": self.key_content})
        self.assertEqual(status, 503)
        self.assertEqual(headers.get("Cache-Control"), "no-store")

    def test_503_when_key_file_empty(self):
        """When SECRETARY_KEY_FILE exists but is empty/whitespace, endpoint must return 503."""
        empty_file = Path(self.temp_dir) / "empty.key"
        empty_file.write_text("   \n  ", encoding="utf-8")
        backup_server.SECRETARY_KEY_FILE = empty_file

        status, headers, body = self._request("GET", "/v1/secretary/finance", {"X-Secretary-Key": self.key_content})
        self.assertEqual(status, 503)
        self.assertEqual(headers.get("Cache-Control"), "no-store")

    def test_401_when_key_missing_or_wrong(self):
        """Missing or wrong X-Secretary-Key header must return 401 without detail."""
        # 1. Missing header
        status, headers, _ = self._request("GET", "/v1/secretary/finance")
        self.assertEqual(status, 401)
        self.assertEqual(headers.get("Cache-Control"), "no-store")

        # 2. Empty header
        status, _, _ = self._request("GET", "/v1/secretary/finance", {"X-Secretary-Key": ""})
        self.assertEqual(status, 401)

        # 3. Wrong key
        status, _, _ = self._request("GET", "/v1/secretary/finance", {"X-Secretary-Key": "wrong-key-value"})
        self.assertEqual(status, 401)

    def test_405_when_method_not_allowed(self):
        """Methods other than GET on /v1/secretary/finance must return 405."""
        headers = {"X-Secretary-Key": self.key_content}

        for method in ("POST", "PUT", "DELETE"):
            status, resp_headers, _ = self._request(method, "/v1/secretary/finance", headers=headers, body=b"{}")
            self.assertEqual(status, 405, f"Expected 405 for {method}, got {status}")
            self.assertEqual(resp_headers.get("Allow"), "GET")
            self.assertEqual(resp_headers.get("Cache-Control"), "no-store")
        for method in ("HEAD", "OPTIONS", "PATCH"):
            status, resp_headers, _ = self._request(method, "/v1/secretary/finance", headers=headers)
            self.assertEqual(status, 405, f"Expected 405 for {method}, got {status}")
            self.assertEqual(resp_headers.get("Allow"), "GET")

    def test_200_success_contract_and_no_money_fields(self):
        """GET /v1/secretary/finance returns 200, strictly adheres to contract, leaks no money fields."""
        status, headers, body = self._request("GET", "/v1/secretary/finance", {"X-Secretary-Key": self.key_content})
        self.assertEqual(status, 200)
        self.assertEqual(headers.get("Cache-Control"), "no-store")
        self.assertIn("application/json", headers.get("Content-Type", ""))

        data = json.loads(body.decode("utf-8"))

        # Root fields
        self.assertIn("generated_at", data)
        self.assertIn("backup_at", data)
        self.assertIn("items", data)

        # ISO 8601 with offset regex (+07:00 or Z)
        iso_pattern = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(\+07:00|[+-]\d{2}:\d{2})$")
        self.assertTrue(iso_pattern.match(data["generated_at"]), f"generated_at not ISO with offset: {data['generated_at']}")
        self.assertTrue(iso_pattern.match(data["backup_at"]), f"backup_at not ISO with offset: {data['backup_at']}")

        items = data["items"]
        # Exactly 3 items expected: active bill, unfinished installment, unsettled debt with dueDate
        self.assertEqual(len(items), 3, f"Expected 3 items, got: {items}")

        # Strict whitelist of allowed fields per item
        allowed_item_keys = {"kind", "title", "due_date", "status"}
        forbidden_keys = {
            "amount", "totalAmount", "monthlyAmount", "initialAmount", "remainingAmount",
            "phone", "note", "accountId", "categoryId", "cardId", "isSettled",
            "paidMonths", "monthCount", "firstDueDate", "nextDueDate", "dayOfMonth",
        }

        date_pattern = re.compile(r"^\d{4}-\d{2}-\d{2}$")

        for item in items:
            item_keys = set(item.keys())
            self.assertEqual(item_keys, allowed_item_keys, f"Item keys mismatch: {item_keys}")
            self.assertTrue(forbidden_keys.isdisjoint(item_keys), f"Forbidden fields found in item: {item}")
            self.assertIn(item["kind"], {"bill", "installment", "debt"})
            self.assertEqual(item["status"], "pending")
            self.assertTrue(date_pattern.match(item["due_date"]), f"due_date not YYYY-MM-DD: {item['due_date']}")

        # Validate each expected item
        by_kind = {it["kind"]: it for it in items}

        # 1. Bill
        bill = by_kind["bill"]
        self.assertEqual(bill["title"], "Tiền điện sinh hoạt")
        self.assertEqual(bill["due_date"], "2024-11-25")

        # 2. Installment (1705294800000 is 2024-01-15 + 4 months = 2024-05-15)
        installment = by_kind["installment"]
        self.assertEqual(installment["title"], "MacBook Air M2")
        self.assertEqual(installment["due_date"], "2024-05-15")

        # 3. Debt (1732950000000 is 2024-11-30 in Asia/Ho_Chi_Minh)
        debt = by_kind["debt"]
        self.assertEqual(debt["title"], "Anh Minh")
        self.assertEqual(debt["due_date"], "2024-11-30")

    def test_settled_items_excluded(self):
        """Verify that settled debts, completed installments, and inactive bills are excluded."""
        status, _, body = self._request("GET", "/v1/secretary/finance", {"X-Secretary-Key": self.key_content})
        self.assertEqual(status, 200)
        data = json.loads(body.decode("utf-8"))
        titles = [it["title"] for it in data["items"]]

        self.assertNotIn("Gói Internet cũ", titles, "Inactive bill must be excluded")
        self.assertNotIn("Tủ lạnh Panasonic", titles, "Completed installment must be excluded")
        self.assertNotIn("Bạn Hùng", titles, "Settled debt must be excluded")
        self.assertNotIn("Chị Lan", titles, "Debt without dueDate must be excluded")

    def test_installment_date_calculation_corner_cases(self):
        """Corner cases for compute_installment_next_due matching CardInstallment.nextDueDate()."""
        # Leap year: Jan 31 2024 + 1 month -> Feb 29 2024
        # 2024-01-31 12:00:00+07:00 = 1706677200000
        jan_31_2024 = 1706677200000
        feb_29 = backup_server.compute_installment_next_due(jan_31_2024, paid_months=1, month_count=12)
        self.assertEqual(feb_29, "2024-02-29")

        # Non-leap year: Jan 31 2023 + 1 month -> Feb 28 2023
        # 2023-01-31 12:00:00+07:00 = 1675141200000
        jan_31_2023 = 1675141200000
        feb_28 = backup_server.compute_installment_next_due(jan_31_2023, paid_months=1, month_count=12)
        self.assertEqual(feb_28, "2023-02-28")

        # Clamping to 30 days: March 31 2024 + 1 month -> April 30 2024
        # 2024-03-31 12:00:00+07:00 = 1711861200000
        mar_31_2024 = 1711861200000
        apr_30 = backup_server.compute_installment_next_due(mar_31_2024, paid_months=1, month_count=12)
        self.assertEqual(apr_30, "2024-04-30")

        # Completed installment (paidMonths == monthCount) -> None
        self.assertIsNone(backup_server.compute_installment_next_due(mar_31_2024, paid_months=12, month_count=12))

    def test_truncation_when_exceeding_2000_items(self):
        """Verify that items list is capped at 2000 and truncated=True is added."""
        fake_backup = {
            "createdAt": 1732123456000,
            "tables": {
                "recurring_bills": [
                    {
                        "name": f"Hóa đơn {i}",
                        "isActive": 1,
                        "nextDueDate": 1732500000000,
                    }
                    for i in range(2500)
                ]
            },
        }
        res = backup_server.extract_secretary_finance_items(fake_backup)
        self.assertEqual(len(res["items"]), 2000)
        self.assertTrue(res.get("truncated"))

    def test_existing_backup_endpoint_behavior_intact(self):
        """Existing /v1/backup GET and PUT endpoints continue to work as expected."""
        # 1. GET /v1/backup with valid Bearer token -> 200 and raw backup bytes
        auth_header = {"Authorization": f"Bearer {self.token_content}"}
        status, _, body = self._request("GET", "/v1/backup", headers=auth_header)
        self.assertEqual(status, 200)
        self.assertEqual(body, self.latest_file.read_bytes())

        # 2. GET /v1/backup without Bearer token -> 401
        status, _, _ = self._request("GET", "/v1/backup")
        self.assertEqual(status, 401)

        # 3. GET /v1/backup with secretary key -> 401 (Secretary key has NO access to full backup)
        status, _, _ = self._request("GET", "/v1/backup", headers={"X-Secretary-Key": self.key_content})
        self.assertEqual(status, 401)


if __name__ == "__main__":
    unittest.main()
