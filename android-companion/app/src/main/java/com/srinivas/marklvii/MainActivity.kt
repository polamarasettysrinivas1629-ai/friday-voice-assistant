package com.srinivas.marklvii

import android.app.AlertDialog
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class MainActivity : Activity() {
    private var localSpeech: android.speech.tts.TextToSpeech? = null
    private var localSpeechReady = false

    private lateinit var address: EditText
    private lateinit var token: EditText
    private lateinit var message: EditText
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var scroll: ScrollView
    private lateinit var send: Button
    private lateinit var account: Button
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var endpoint: URL? = null
    private var pairingToken = ""
    private var busy = false
    private var visible = false
    private var connectionVersion = 0
    private val poll = object : Runnable {
        override fun run() {
            if (!visible) return
            request()
            handler.postDelayed(this, 4000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localSpeech = android.speech.tts.TextToSpeech(this) {
            localSpeechReady = it == android.speech.tts.TextToSpeech.SUCCESS
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(Color.rgb(11, 17, 29))
            val inset = (16 * resources.displayMetrics.density).toInt()
            setPadding(inset, inset, inset, inset)
        }

        val iconSize = (64 * resources.displayMetrics.density).toInt()
        val header = LinearLayout(this).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.dairy_logo)
                contentDescription = "Sri Srinivasa Dairy Farm logo"
            }, LinearLayout.LayoutParams(iconSize, iconSize))
        }
        header.addView(label("Friday").apply {
            setPadding((12 * resources.displayMetrics.density).toInt(), 0, 0, 0)
            textSize = 32f
            typeface = android.graphics.Typeface.SERIF
            setTextColor(Color.rgb(239, 210, 152))
        })
        root.addView(header)
        address = input("Hosted HTTPS address", InputType.TYPE_TEXT_VARIATION_URI)
        address.setText(getPreferences(MODE_PRIVATE).getString("chat_address", ""))
        root.addView(address)
        account = Button(this).apply {
            text = "Gmail sign-in and plans"
            setOnClickListener { openAccount() }
        }
        root.addView(account)
        root.addView(Button(this).apply {
            text = "WhatsApp replies"
            setOnClickListener { showWhatsAppReplies() }
        })
        root.addView(Button(this).apply {
            text = "Read an open WhatsApp chat"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Read visible WhatsApp text")
                    .setMessage("In Accessibility settings, enable Friday and its accessibility button. Open a WhatsApp chat and tap the accessibility button when you want Friday to read it.")
                    .setPositiveButton("Accessibility settings") { _, _ ->
                        startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }.setNegativeButton("Cancel", null).show()
            }
        })
        token = input("App token from your account", InputType.TYPE_TEXT_VARIATION_PASSWORD)
        root.addView(token)
        root.addView(Button(this).apply {
            text = "Connect"
            setOnClickListener { connect() }
        })
        status = label("Enter the hosted HTTPS address, then sign in to get an app token.")
        status.setTextColor(Color.rgb(239, 210, 152))
        status.setPadding(0, 8, 0, 8)
        root.addView(status)
        transcript = label("")
        transcript.setTextIsSelectable(true)
        transcript.setPadding(16, 16, 16, 16)
        transcript.setTextSize(16f)
        transcript.setLineSpacing(4f, 1f)
        transcript.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(17, 30, 48))
            cornerRadius = 16f * resources.displayMetrics.density
        }
        scroll = ScrollView(this).apply { addView(transcript) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        message = input("Message Friday", InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        message.filters = arrayOf(InputFilter.LengthFilter(4000))
        root.addView(message)
        send = Button(this).apply {
            text = "Send"
            isEnabled = false
            setOnClickListener {
                val draft = message.text.toString().trim()
                if (draft.isNotEmpty()) request(draft)
            }
        }
        root.addView(send)
        setContentView(root)
        val prefs = getPreferences(MODE_PRIVATE)
        val storedAddress = address.text.toString()
        prefs.getString("chat_credential", null)?.let { stored ->
            runCatching { ChatCredential.decrypt(storedAddress, stored) }.onSuccess { secret ->
                token.setText(secret)
                connect()
            }.onFailure {
                prefs.edit().remove("chat_credential").apply()
            }
        }
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        setTextColor(Color.WHITE)
    }

    private fun input(label: String, variation: Int) = EditText(this).apply {
        hint = label
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
        inputType = InputType.TYPE_CLASS_TEXT or variation
        setSingleLine(true)
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            inputType = InputType.TYPE_CLASS_TEXT or variation
            isSaveEnabled = false
        }
    }

    private fun validatedAddress(): URI? {
        val value = address.text.toString().trim()
        val base = runCatching { URI(value) }.getOrNull()
        if (base == null || base.scheme != "https" || base.host == null ||
            base.userInfo != null || base.query != null || base.fragment != null ||
            (base.path.isNotEmpty() && base.path != "/")) {
            status.text = "Enter the hosted HTTPS address without a path."
            return null
        }
        return base
    }

    private fun showWhatsAppReplies() {
        val listener = WhatsAppReplies.instance
        val prefs = getSharedPreferences("whatsapp_replies", MODE_PRIVATE)
        if (listener == null) {
            AlertDialog.Builder(this)
                .setTitle("WhatsApp replies")
                .setMessage("Allow Friday to see incoming WhatsApp notifications. Replies use WhatsApp's notification Reply button.")
                .setPositiveButton("Notification access") { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        val chats = listener.chats()
        val dialog = AlertDialog.Builder(this)
            .setTitle("WhatsApp messages with Reply")
            .setNegativeButton("Close", null)
            .setNeutralButton("Turn off auto-reply") { _, _ ->
                prefs.edit().remove("business_chat").apply()
            }
        if (chats.isEmpty()) dialog.setMessage("Wait for a new WhatsApp notification with a Reply action.")
        else dialog.setItems(chats.map { it.title + if (it.business) " (Business)" else " (Personal)" }.toTypedArray()) { _, index ->
            val chat = chats[index]
            val entry = input("Type your reply", InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            entry.setTextColor(Color.BLACK)
            entry.setHintTextColor(Color.DKGRAY)
            entry.filters = arrayOf(InputFilter.LengthFilter(1000))
            val editor = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(entry)
                addView(Button(this@MainActivity).apply {
                    text = "Read message aloud"
                    setOnClickListener { speakMessage(chat.text) }
                })
                val suggestions = listOf(
                    "English" to "Hello! Thanks for your message. I’ll get back to you soon.",
                    "తెలుగు" to "నమస్కారం! మీ సందేశం అందింది. త్వరలో జవాబు ఇస్తాను."
                )
                suggestions.forEach { (language, draft) ->
                    addView(Button(this@MainActivity).apply {
                        text = language
                        setOnClickListener { entry.setText(draft) }
                    })
                }
            }
            val prompt = AlertDialog.Builder(this)
                .setTitle(chat.title)
                .setMessage(chat.text)
                .setView(editor)
                .setNegativeButton("Close", null)
                .setPositiveButton("Reply in WhatsApp") { _, _ ->
                    status.text = if (listener.reply(chat, entry.text.toString())) {
                        "Reply submitted to WhatsApp."
                    } else {
                        "Reply unavailable. Open the latest WhatsApp notification and try again."
                    }
                }
            if (chat.business && chat.id != null) prompt.setNeutralButton("Business auto-reply") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Enable for " + chat.title + "?")
                    .setMessage("Friday can answer milk requests and acknowledge bill questions for this Business chat. Enabling this replaces the previous selection.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Enable") { _, _ ->
                        prefs.edit().putString("business_chat", chat.id).apply()
                        status.text = "Business auto-reply enabled for " + chat.title
                    }.show()
            }
            prompt.show()
        }
        dialog.show()
    }

    private fun speakMessage(text: String) {
        val engine = localSpeech
        val language = if (text.any { it in '\u0C00'..'\u0C7F' }) "te" else "en"
        val voice = engine?.voices?.firstOrNull {
            it.locale.language == language && !it.isNetworkConnectionRequired
        }
        if (!localSpeechReady || engine == null || voice == null ||
            engine.setVoice(voice) != android.speech.tts.TextToSpeech.SUCCESS) {
            android.widget.Toast.makeText(this, "Install an offline voice for " + language,
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        engine.speak(text.take(3600), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "whatsapp-message")
    }

    private fun chooseFoodService() {
        val services = arrayOf("Swiggy", "Zomato")
        val websites = arrayOf("https://www.swiggy.com/", "https://www.zomato.com/")
        AlertDialog.Builder(this)
            .setTitle("Open a food delivery service")
            .setItems(services) { _, index ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websites[index])))
                } catch (_: ActivityNotFoundException) {
                    status.text = "Open the delivery service in your browser."
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAccount() {
        val base = validatedAddress() ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(base.resolve("/account").toString())))
        } catch (_: ActivityNotFoundException) {
            status.text = "Open the account page in a browser."
        }
    }

    private fun connect() {
        val base = validatedAddress() ?: return
        val value = base.toString()
        val secret = token.text.toString().trim()
        if (!secret.matches(Regex("[A-Za-z0-9_-]{43}"))) {
            status.text = "Get an app token from your account."
            return
        }
        endpoint = base.resolve("/v1/chat").toURL()
        pairingToken = secret
        connectionVersion++
        transcript.text = ""
        val prefs = getPreferences(MODE_PRIVATE)
        val encrypted = runCatching { ChatCredential.encrypt(value, secret) }.getOrNull()
        prefs.edit().putString("chat_address", value).putString("chat_credential", encrypted).apply()
        send.isEnabled = false
        account.text = "Gmail sign-in and plans"
        status.text = "Connecting…"
        request()
    }

    private fun request(draft: String? = null) {
        val url = endpoint ?: return
        if (busy) return
        busy = true
        send.isEnabled = false
        val version = connectionVersion
        val secret = pairingToken
        if (draft != null) {
            status.text = "Waiting for Munna…"
            send.isEnabled = false
        }
        worker.execute {
            val result = runCatching { exchange(url, secret, draft) }
            runOnUiThread {
                busy = false
                if (isDestroyed) return@runOnUiThread
                if (version != connectionVersion) {
                    request()
                    return@runOnUiThread
                }
                result.onSuccess { (code, body, expiry) ->
                    send.isEnabled = code == 200 || (code != 401 && code != 403 && transcript.text.isNotEmpty())
                    if (code == 200) {
                        val display = body
                        if (display != transcript.text.toString()) {
                            transcript.text = display
                            scroll.post { scroll.fullScroll(android.view.View.FOCUS_DOWN) }
                        }
                        if (draft != null && message.text.toString().trim() == draft) message.text.clear()
                        if (draft != null || status.text == "Connecting…" || !sendWasRejected) {
                            status.text = if (expiry.isNullOrBlank()) "Connected" else "Access until $expiry"
                            sendWasRejected = false
                        }
                        account.text = "Manage plan"
                    } else {
                        if (code == 401) {
                            transcript.text = ""
                            endpoint = null
                            pairingToken = ""
                            token.text.clear()
                            getPreferences(MODE_PRIVATE).edit().remove("chat_credential").apply()
                            account.text = "Gmail sign-in and plans"
                        }
                        if (code == 403) account.text = "Choose or renew a plan"
                        status.text = body
                        sendWasRejected = draft != null
                    }
                }.onFailure {
                    status.text = "Cannot reach chat. Check the hosted HTTPS address and internet connection."
                    send.isEnabled = false
                    sendWasRejected = false
                }
            }
        }
    }

    private var sendWasRejected = false

    private fun exchange(url: URL, secret: String, draft: String?): Triple<Int, String, String?> {
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8000
            connection.readTimeout = 35000
            connection.setRequestProperty("Authorization", "Bearer $secret")
            connection.setRequestProperty("Accept", "application/json")
            if (draft != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val body = JSONObject().put("text", draft).toString().toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code == 200) connection.inputStream else connection.errorStream
            val data = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code != 200) {
                val fallback = "Chat request failed (HTTP $code)."
                val detail = runCatching { JSONObject(data).optString("detail", fallback) }
                return Triple(code, detail.getOrDefault(fallback), null)
            }
            val payload = JSONObject(data)
            val turns = payload.getJSONArray("turns")
            val display = (0 until turns.length()).joinToString("\n\n") { index ->
                val turn = turns.getJSONObject(index)
                val speaker = if (turn.getString("role") == "user") "You" else "Munna"
                val text = turn.getJSONArray("parts").getJSONObject(0).getString("text")
                "$speaker: $text"
            }
            return Triple(code, display, payload.optString("access_until"))
        } finally {
            connection.disconnect()
        }
    }

    override fun onStart() {
        super.onStart()
        visible = true
        handler.post(poll)
    }

    override fun onStop() {
        visible = false
        handler.removeCallbacks(poll)
        super.onStop()
    }

    override fun onDestroy() {
        localSpeechReady = false
        localSpeech?.shutdown()
        worker.shutdownNow()
        super.onDestroy()
    }
}
