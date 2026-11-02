package com.splitsmart.app.ui

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
