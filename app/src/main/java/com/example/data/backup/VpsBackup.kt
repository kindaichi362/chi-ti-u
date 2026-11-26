package com.example.data.backup

import android.content.ContentValues
import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One-phone snapshots. The server keeps versions; a restore replaces all local tables. */
class VpsBackup(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val prefs = context.getSharedPreferences("vps_backup", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) {
            prefs.edit().putString("token", value.trim()).apply()
            NightlyBackupScheduler.schedule(context)
        }

    suspend fun upload(): String = withContext(Dispatchers.IO) {
        val bytes = createSnapshot()
        val conn = connect("PUT")
        try {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            if (conn.responseCode != 201) error("Máy chủ trả về mã ${conn.responseCode}")
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getString("saved")
        } finally {
            conn.disconnect()
        }
    }

    suspend fun downloadAndRestore(): Unit = withContext(Dispatchers.IO) {
        val conn = connect("GET")
        val bytes = try {
            if (conn.responseCode != 200) error("Không tải được bản sao lưu (mã ${conn.responseCode})")
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
        restoreSnapshot(bytes)
    }

    internal suspend fun restoreSnapshot(bytes: ByteArray) {
        if (bytes.size > MAX_BYTES) error("Bản sao lưu quá lớn")
        val snapshot = JSONObject(String(bytes, Charsets.UTF_8))
        validate(snapshot)
        // A private local recovery copy is made before any existing data is replaced.
        context.openFileOutput("before_vps_restore.json", Context.MODE_PRIVATE).use { it.write(createSnapshot()) }
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            // Older backups have no dragon tables: restoring one replaces the finance data only and
            // leaves the dragon's progress alone.
            val cleared = if (snapshotTables(snapshot).containsAll(DRAGON_TABLES)) TABLES else FINANCE_TABLES
            for (table in cleared.asReversed()) db.delete(table, null, null)
            val tables = snapshot.getJSONObject("tables")
            for (table in snapshotTables(snapshot)) {
                val rows = tables.getJSONArray(table)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val values = ContentValues()
                    for (column in expectedColumns(db, table)) {
                        if (table == "card_installments" && column == "monthlyAmount" && !row.has(column)) {
                            values.putNull(column)
                            continue
                        }
                        if ((table == "transactions" && column == "debtId" || table == "debts" && column == "accountId") && !row.has(column)) {
                            values.putNull(column)
                            continue
                        }
                        when (val value = row.get(column)) {
                            JSONObject.NULL -> values.putNull(column)
                            is Int -> values.put(column, value)
                            is Long -> values.put(column, value)
                            is Double -> values.put(column, value)
                            is String -> values.put(column, value)
                            else -> error("Giá trị không hợp lệ: $table.$column")
                        }
                    }
                    if (db.insert(table, 0, values) == -1L) error("Không khôi phục được $table")
                }
            }
        }
    }

    internal suspend fun createSnapshot(): ByteArray = database.withTransaction {
        val db = database.openHelper.readableDatabase
        val tables = JSONObject()
        for (table in TABLES) {
            val rows = JSONArray()
            db.query("SELECT * FROM $table ORDER BY id").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    for (index in 0 until cursor.columnCount) {
                        val value: Any = when (cursor.getType(index)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                            android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                            else -> error("Dữ liệu không hỗ trợ")
                        }
                        row.put(cursor.getColumnName(index), value)
                    }
                    rows.put(row)
                }
            }
            tables.put(table, rows)
        }
        JSONObject().put("format", "chi-tieu-snapshot").put("version", 1)
            .put("databaseVersion", 10).put("createdAt", System.currentTimeMillis())
            .put("tables", tables).toString().toByteArray(Charsets.UTF_8)
    }

    private fun validate(snapshot: JSONObject) {
        require(snapshot.optString("format") == "chi-tieu-snapshot" && snapshot.optInt("version") == 1) { "Sai định dạng bản sao lưu" }
        val sourceTables = snapshotTables(snapshot)
        val tables = snapshot.getJSONObject("tables")
        require(tables.keys().asSequence().toSet() == sourceTables.toSet()) { "Thiếu bảng dữ liệu" }
        val db = database.openHelper.readableDatabase
        for (table in sourceTables) {
            val columns = expectedColumns(db, table).let { current ->
                when {
                    snapshot.optInt("databaseVersion") < 10 && table == "transactions" -> current - "debtId"
                    snapshot.optInt("databaseVersion") < 10 && table == "debts" -> current - "accountId"
                    snapshot.optInt("databaseVersion") == 7 && table == "card_installments" -> current - "monthlyAmount"
                    else -> current
                }
            }
            val rows = tables.getJSONArray(table)
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                require(row.keys().asSequence().toSet() == columns) { "Sai cột dữ liệu trong $table" }
                for (column in columns) {
                    val value = row.get(column)
                    require(value == JSONObject.NULL || value is Number || value is String) { "Sai kiểu dữ liệu trong $table" }
                }
            }
        }
    }

    private fun snapshotTables(snapshot: JSONObject): List<String> = when (snapshot.optInt("databaseVersion")) {
        6 -> TABLES_V6
        7, 8 -> FINANCE_TABLES
        9, 10 -> TABLES
        else -> throw IllegalArgumentException("Bản sao lưu không cùng phiên bản dữ liệu")
    }

    private fun expectedColumns(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Set<String> {
        val result = linkedSetOf<String>()
        db.query("PRAGMA table_info($table)").use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(1))
        }
        return result
    }

    private fun connect(method: String): HttpURLConnection {
        val key = token
        require(key.isNotEmpty()) { "Hãy nhập mã kết nối VPS trong Cài đặt" }
        return (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Accept", "application/json")
        }
    }

    companion object {
        private const val ENDPOINT = "https://backup.180-93-125-5.sslip.io/v1/backup"
        private const val MAX_BYTES = 20 * 1024 * 1024
        private val TABLES_V6 = listOf("accounts", "categories", "transactions", "budgets", "debts", "goals", "recurring_bills")
        private val FINANCE_TABLES = TABLES_V6 + "card_installments"
        private val DRAGON_TABLES = listOf("dragon_pet", "dragon_ledger")
        private val TABLES = FINANCE_TABLES + DRAGON_TABLES
    }
}
