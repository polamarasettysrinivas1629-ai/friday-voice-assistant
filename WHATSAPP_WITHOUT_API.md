# Friday local WhatsApp

Use the installed WhatsApp or WhatsApp Business app on your own device. These local features require no WhatsApp API key, OpenWA session, QR code or cloud AI. The package also contains the original Friday assistant, whose hosted features can be configured separately.

## Android

- **New messages:** Open Friday → **WhatsApp replies → Notification access** and enable Friday in Android settings. Return to Friday after receiving a notification. Pick a conversation to show its message, tap **Read message aloud** if desired, then enter a reply or edit one of the Telugu or English acknowledgements. Tap **Reply in WhatsApp** to submit. The latest notification must still contain the WhatsApp Reply action. Submission hands the text to WhatsApp. Check WhatsApp for delivery.
- **Old messages:** Open **Read an open WhatsApp chat** in Friday and enable its service and on-screen accessibility shortcut. Switch to the target WhatsApp chat and tap the Android accessibility button. Scroll up and tap the button again for earlier text. This requires Android 8+ with an available accessibility button. Friday reads visible text that WhatsApp exposes to Android accessibility, which can include UI labels. An offline Telugu or English text-to-speech voice must be installed. The service reads on request and does not export or retain chat history.
- **Business replies:** Select a compatible individual WhatsApp Business notification, choose **Business auto-reply**, then approve the chat. This initial rule supports one selected Business conversation. A milk request gets a question about quantity and date. A bill question is acknowledged for follow-up. The rule uses Telugu or English, has a one-hour minimum interval, and ignores unrelated text. The current build has no live dairy ledger or order integration. Turn it off in **WhatsApp replies**. Personal notifications never enter the automatic reply rule.
- Standard Android notification access sees only content made available in notifications. If no text or Reply action is exposed, open the chat manually.

### Build Android

Open the `android-companion` folder in Android Studio with JDK 17, Gradle 8.10.2 and Android SDK 35, or run `gradle assembleDebug` from that folder with those tools installed. Alternatively, place the contents of the Friday folder at the root of a GitHub repo and run **Build FRIDAY Android App APK** under Actions. The workflow uploads the debug APK.

## Windows

Extract the ZIP and double-click `START_LOCAL_WHATSAPP_WINDOWS.bat`. Open the chat in installed WhatsApp first. In Friday click **Read open WhatsApp window**, then switch to WhatsApp within four seconds. Friday attempts to bring in text exposed by the current WhatsApp window through Windows UI Automation. This depends on the installed WhatsApp build. Scroll with WhatsApp focused before taking another reading. If the installed build exposes no text, copy the selected message and paste it into Friday.

Use **Speak text in Friday** with a matching installed Windows voice. Review or edit the reply, then choose **Copy reply and open WhatsApp**. Select the recipient, paste the reply and send in WhatsApp. The Windows tool uses Windows PowerShell and local speech. It does not monitor new notifications or send automatically.

## Verification

After building, test a new personal notification, a Business milk request for the selected chat, and an older chat on an unlocked phone. On Windows, verify the visible text capture against the installed WhatsApp build and test the copy-and-send fallback. This source archive does not include an APK or Windows installer.
