package com.splitsmart.app.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.splitsmart.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun AppCompatActivity.launchIo(
    onError: (String) -> Unit = { toast(it) },
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            onError(e.message ?: e.toString())
        }
    }
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.confirm(title: String, message: String, onYes: () -> Unit) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.yes) { _, _ -> onYes() }
        .setNegativeButton(android.R.string.no, null)
        .show()
}

fun todayIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun formatMoney(amount: Double): String = String.format(Locale.US, "$%.2f", amount)

fun initials(name: String): String = name.trim().split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { String(Character.toChars(it.codePointAt(0))) }
    .uppercase(Locale.US)
    .ifBlank { "S" }

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

// Only format enum labels. Member names and email addresses stay exactly as supplied.
fun displayChoice(value: String): String = when (value) {
    "ALL" -> "All"
    "EQUAL" -> "Equal split"
    "EXACT" -> "Exact amounts"
    "PERCENTAGE" -> "Percentage"
    "NEWEST" -> "Newest first"
    "OLDEST" -> "Oldest first"
    else -> if (value.matches(Regex("[A-Z_]+"))) {
        value.lowercase(Locale.US).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.US) }
    } else value
}

fun Context.choiceAdapter(items: List<String>): ArrayAdapter<String> =
    object : ArrayAdapter<String>(this, R.layout.item_spinner, items) {
        init { setDropDownViewResource(R.layout.item_spinner_dropdown) }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getView(position, convertView, parent).also {
                (it as TextView).text = displayChoice(getItem(position).orEmpty())
            }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getDropDownView(position, convertView, parent).also {
                (it as TextView).text = displayChoice(getItem(position).orEmpty())
            }
    }
