package com.example.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v6: moves an existing database onto the cleaned-up category tree of [InitialCategories] without
 * losing any history, and normalises account bank ids so the bank logos resolve.
 *
 * 1. Every old default name is renamed to its new name, or — when the new name already exists —
 *    its transactions / budgets / bills / sub-categories are re-pointed there and the old row goes.
 * 2. Old "categories" that were really transfers or debts are deleted when unused, otherwise kept
 *    under the "Khác" group so history stays readable.
 * 3. Defaults are refreshed (group, colour, emoji, order) and any missing default is added.
 * User-created categories are never touched.
 */
object CategoryCleanupMigration : Migration(5, 6) {

    private val legacyBankIds = mapOf(
        "vietinbank" to "vtb", "ctg" to "vtb", "vpbank" to "vpb", "agribank" to "vba",
        "tpbank" to "tpb", "sacombank" to "stb", "techcombank" to "tcb", "vietcombank" to "vcb",
        "credit" to null
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        for ((oldName, newName) in InitialCategories.legacyRenames) {
            for ((oldId, type) in idsByName(db, oldName)) {
                val targetId = idsByName(db, newName).firstOrNull { it.second == type }?.first
                if (targetId == null) {
                    db.update("categories", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply { put("name", newName) }, "id = ?", arrayOf(oldId))
                } else {
                    repoint(db, oldId, targetId)
                    db.execSQL("DELETE FROM categories WHERE id = ?", arrayOf(oldId))
                }
            }
        }

        for (name in InitialCategories.legacyNonSpending) {
            for ((id, _) in idsByName(db, name)) {
                if (isUsed(db, id)) {
                    db.execSQL("UPDATE categories SET groupName = 'Khác', isDefault = 0 WHERE id = ?", arrayOf(id))
                } else {
                    db.execSQL("UPDATE categories SET parentId = NULL WHERE parentId = ?", arrayOf(id))
                    db.execSQL("DELETE FROM categories WHERE id = ?", arrayOf(id))
                }
            }
        }

        for (cat in InitialCategories.getDefaultCategories()) {
            val existing = idsByName(db, cat.name).firstOrNull { it.second == cat.type.name }
            val values = ContentValues().apply {
                put("groupName", cat.groupName)
                put("colorHex", cat.colorHex)
                put("emoji", cat.emoji)
                put("iconName", cat.iconName)
                put("displayOrder", cat.displayOrder)
                put("isDefault", 1)
            }
            if (existing != null) {
                db.update("categories", SQLiteDatabase.CONFLICT_NONE, values, "id = ?", arrayOf(existing.first))
            } else {
                values.put("name", cat.name)
                values.put("type", cat.type.name)
                values.put("isPriority", if (cat.isPriority) 1 else 0)
                db.insert("categories", SQLiteDatabase.CONFLICT_NONE, values)
            }
        }

        for ((old, new) in legacyBankIds) {
            if (new == null) db.execSQL("UPDATE accounts SET bankId = NULL WHERE bankId = ?", arrayOf(old))
            else db.execSQL("UPDATE accounts SET bankId = ? WHERE bankId = ?", arrayOf(new, old))
        }
    }

    private fun idsByName(db: SupportSQLiteDatabase, name: String): List<Pair<Long, String>> =
        db.query("SELECT id, type FROM categories WHERE name = ? ORDER BY id", arrayOf(name)).use { c ->
            buildList { while (c.moveToNext()) add(c.getLong(0) to c.getString(1)) }
        }

    private fun repoint(db: SupportSQLiteDatabase, fromId: Long, toId: Long) {
        db.execSQL("UPDATE transactions SET categoryId = ? WHERE categoryId = ?", arrayOf(toId, fromId))
        db.execSQL("UPDATE recurring_bills SET categoryId = ? WHERE categoryId = ?", arrayOf(toId, fromId))
        db.execSQL("UPDATE categories SET parentId = ? WHERE parentId = ?", arrayOf(toId, fromId))
        // Two budgets for the merged pair in the same month: add the limits together.
        db.execSQL(
            "UPDATE budgets SET amountLimit = amountLimit + (SELECT b2.amountLimit FROM budgets b2 WHERE b2.categoryId = ? AND b2.monthYear = budgets.monthYear) " +
                "WHERE categoryId = ? AND monthYear IN (SELECT monthYear FROM budgets WHERE categoryId = ?)",
            arrayOf(fromId, toId, fromId)
        )
        db.execSQL("DELETE FROM budgets WHERE categoryId = ? AND monthYear IN (SELECT monthYear FROM budgets WHERE categoryId = ?)", arrayOf(fromId, toId))
        db.execSQL("UPDATE budgets SET categoryId = ? WHERE categoryId = ?", arrayOf(toId, fromId))
    }

    private fun isUsed(db: SupportSQLiteDatabase, id: Long): Boolean =
        db.query(
            "SELECT (SELECT COUNT(*) FROM transactions WHERE categoryId = ?) + (SELECT COUNT(*) FROM budgets WHERE categoryId = ?) + (SELECT COUNT(*) FROM recurring_bills WHERE categoryId = ?)",
            arrayOf(id, id, id)
        ).use { it.moveToFirst() && it.getLong(0) > 0 }
}
