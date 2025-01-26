import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log

class TTSHelper(private val context: Context, private val tts: TextToSpeech) {

    val DLG_MSG = """
    It is highly recommended to set Google TTS Engine as the Text-to-Speech (TTS) engine on your device. We have detected that it is not currently set, which is important for the app to function properly.
    Pressing "Agree" will take you to the Text-to-Speech output settings, where you should set the preferred engine to "Speech Recognition and Synthesis by Google" or a similar option.
    
    IF THE PAGE DOES NOT OPEN AUTOMATICALLY:
    1.Open your device's Settings
    2.Use the search icon to look for "Text-to-Speech" (include the hyphens)
    Once there, follow the same steps to set Google TTS as the default engine
    """.trimIndent()

    fun checkAndPromptGoogleTTS() {
        Log.d("flow", "entering checkAndPromptGoogleTTS()")
        val defaultEngine = tts.defaultEngine
        val googleTtsEngine = "com.google.android.tts"

        if (defaultEngine != googleTtsEngine) {
            // Show a dialog to prompt the user
            AlertDialog.Builder(context)
                .setTitle("Set Google TTS Engine")
                //.setMessage("It seems Google Text-to-Speech is not set as your default engine. Do you want to set it up?")
                .setMessage(DLG_MSG)
                .setPositiveButton("Agree") { _, _ ->
                    // Open TTS settings
                   // val intent = Intent(Settings.ACTION_SETTINGS)
                    val intent = Intent("com.android.settings.TTS_SETTINGS")

                    context.startActivity(intent)
                }
                .setNegativeButton("Decline", null)
                .setCancelable(false)
                .show()
        }
    }
}
