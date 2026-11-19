package com.srinivas.marklvii

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.util.Locale

class WhatsAppScreenReader : AccessibilityService() {
    private var voice: TextToSpeech? = null
    private var ready = false
    private val button = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
            readOpenWhatsApp()
        }
    }

    override fun onServiceConnected() {
        if (Build.VERSION.SDK_INT < 26) return
        voice = TextToSpeech(this) { result -> ready = result == TextToSpeech.SUCCESS }
        accessibilityButtonController.registerAccessibilityButtonCallback(button)
    }

    private fun readOpenWhatsApp() {
        val root = rootInActiveWindow
        if (root?.packageName?.toString() !in setOf("com.whatsapp", "com.whatsapp.w4b")) {
            notifyUser("Open the WhatsApp chat first.")
            return
        }
        val lines = linkedSetOf<String>()
        readVisible(root ?: return, lines, 0)
        val text = lines.joinToString("\n").take(3600)
        if (text.isBlank()) {
            notifyUser("This WhatsApp screen exposed no readable text.")
            return
        }
        val engine = voice
        val language = if (text.any { it in '\u0C00'..'\u0C7F' }) Locale("te", "IN") else Locale.ENGLISH
        val localVoice = engine?.voices?.firstOrNull {
            it.locale.language == language.language && !it.isNetworkConnectionRequired
        }
        if (!ready || localVoice == null || engine?.setVoice(localVoice) != TextToSpeech.SUCCESS) {
            notifyUser("Install an offline ${language.displayLanguage} text-to-speech voice.")
            return
        }
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "whatsapp-visible")
    }

    private fun readVisible(node: AccessibilityNodeInfo, text: MutableSet<String>, depth: Int) {
        if (!node.isVisibleToUser || node.isPassword || node.isEditable || depth > 30 || text.size >= 70) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(text::add)
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child -> readVisible(child, text, depth + 1) }
        }
    }

    private fun notifyUser(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { voice?.stop() }
    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 26) accessibilityButtonController.unregisterAccessibilityButtonCallback(button)
        ready = false
        voice?.shutdown()
        super.onDestroy()
    }
}
