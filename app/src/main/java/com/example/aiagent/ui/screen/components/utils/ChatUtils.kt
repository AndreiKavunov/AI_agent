// ui/screen/components/utils/ChatUtils.kt
package com.example.aiagent.ui.screen.components.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("agent_message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Сообщение скопировано", Toast.LENGTH_SHORT).show()
}

fun formatTime(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${String.format("%.1f", ms / 1000.0)}с"
        else -> "${String.format("%.1f", ms / 60000.0)}мин"
    }
}
