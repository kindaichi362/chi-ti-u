package com.example.ui.components

import com.example.data.model.Category
import java.text.Normalizer
import java.util.Locale

private fun searchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .replace('đ', 'd').replace('Đ', 'D')
    .lowercase(Locale.ROOT)

fun Category.matchesSearch(query: String): Boolean {
    val words = searchKey(query).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val target = searchKey("$name ${groupName.orEmpty()}")
    return words.all { it in target }
}
