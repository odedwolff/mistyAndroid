import android.app.Activity
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SimpleLanguageInstaller : AppCompatActivity() {
    companion object {
        private const val TTS_DATA_CHECK = 100
    }

    fun installLanguage(languageCode: String) {
        Log.d("flow","entering installLanguage({$languageCode})")
        val installIntent = Intent().apply {
            action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            `package` = "com.google.android.tts"  // For Google TTS Engine
            putExtra("language", languageCode)    // e.g., "fra" for French
        }
        startActivityForResult(installIntent, TTS_DATA_CHECK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TTS_DATA_CHECK) {
            // Installation completed (successfully or not)
            // You can add your post-installation logic here
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Installation successful
                    Log.d("flow","installation results OK")
                }
                Activity.RESULT_CANCELED -> {
                    // Installation failed or was cancelled
                    Log.e("flow","installaion failed!")

                }
            }
        }
    }
}