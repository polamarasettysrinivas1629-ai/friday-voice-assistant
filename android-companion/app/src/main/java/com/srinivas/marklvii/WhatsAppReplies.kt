package com.srinivas.marklvii

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WhatsAppReplies : NotificationListenerService() {
    data class Chat(val key: String, val id: String?, val title: String, val text: String, val business: Boolean)

    private val autoSeen = linkedMapOf<String, String>()
    private val lastSent = mutableMapOf<String, Long>()

    override fun onListenerConnected() { instance = this }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    private fun extract(sbn: StatusBarNotification): Chat? {
        if (Build.VERSION.SDK_INT < 28 || sbn.packageName !in packages) return null
        val note = sbn.notification
        if (note.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val style = Notification.MessagingStyle.extractMessagingStyleFromNotification(note)
        val message = style?.messages?.lastOrNull()?.text?.toString()
            ?: note.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (message.isNullOrBlank() || action(note) == null) return null
        val title = note.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        if (title.isBlank()) return null
        val identity = if (style != null && !style.isGroupConversation
            && !note.shortcutId.isNullOrBlank()) sbn.packageName + ":" + note.shortcutId else null
        return Chat(sbn.key, identity, title, message, sbn.packageName == "com.whatsapp.w4b")
    }

    private fun action(note: Notification): Notification.Action? =
        note.actions?.firstOrNull { it.actionIntent != null &&
            (it.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY) &&
            it.remoteInputs?.any { input -> input.allowFreeFormInput } == true }

    fun chats(): List<Chat> = runCatching {
        activeNotifications.orEmpty().sortedByDescending { it.postTime }.mapNotNull(::extract)
    }.getOrDefault(emptyList())

    fun reply(chat: Chat, text: String): Boolean {
        if (text.isBlank() || text.length > 1000) return false
        val current = runCatching { activeNotifications.orEmpty().firstOrNull { it.key == chat.key } }.getOrNull()
            ?: return false
        if (extract(current) != chat) return false
        val selected = action(current.notification) ?: return false
        val input = selected.remoteInputs.firstOrNull { it.allowFreeFormInput } ?: return false
        val intent = Intent()
        RemoteInput.addResultsToIntent(arrayOf(input), intent, Bundle().apply { putCharSequence(input.resultKey, text) })
        return try {
            selected.actionIntent.send(this, 0, intent)
            true
        } catch (_: PendingIntent.CanceledException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val chat = extract(sbn) ?: return
        if (!chat.business) return
        val id = chat.id ?: return
        val prefs = getSharedPreferences("whatsapp_replies", MODE_PRIVATE)
        if (id != prefs.getString("business_chat", null)) return
        val response = DairyReply.forMessage(chat.text) ?: return
        val style = Notification.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification) ?: return
        val message = style.messages.lastOrNull() ?: return
        val peer = message.senderPerson ?: return
        val self = style.user
        if (peer == self || (peer.key != null && peer.key == self.key)
            || (peer.uri != null && peer.uri == self.uri)) return
        val now = System.currentTimeMillis()
        if (message.timestamp < now - 5 * 60 * 1000 || message.timestamp > now + 30_000) return
        val event = "${message.timestamp}:${chat.text}"
        if (autoSeen[id] == event || now - (lastSent[id] ?: prefs.getLong("business_last_sent", 0L)) < 60 * 60 * 1000) return
        autoSeen[id] = event
        if (autoSeen.size > 25) autoSeen.remove(autoSeen.keys.first())
        lastSent[id] = now
        prefs.edit().putLong("business_last_sent", now).apply()
        if (lastSent.size > 25) lastSent.remove(lastSent.keys.first())
        reply(chat, response)
    }

    companion object {
        private val packages = setOf("com.whatsapp", "com.whatsapp.w4b")
        @Volatile var instance: WhatsAppReplies? = null
            private set
    }
}
