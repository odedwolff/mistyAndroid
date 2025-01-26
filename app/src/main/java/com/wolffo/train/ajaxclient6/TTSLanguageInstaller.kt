package com.wolffo.train.ajaxclient6

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSLanguageInstaller(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private val installQueue = mutableListOf<Locale>()
    private var isInstalling = false

    var onProgressUpdate: ((Int, Int, String, String) -> Unit)? = null
    var onCompleted: ((Map<Locale, InstallResult>) -> Unit)? = null

    private val results = mutableMapOf<Locale, InstallResult>()

    enum class InstallResult {
        ALREADY_INSTALLED,
        INSTALLATION_SUCCESS,
        INSTALLATION_FAILED,
        NOT_SUPPORTED
    }

    fun initialize(onInitialized: () -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                onInitialized()
            }
        }
    }

    fun installLanguages(languages: List<Locale>) {
        installQueue.clear()
        results.clear()

        // Filter out already installed languages first
        languages.forEach { locale ->
            if (isLanguageInstalled(locale)) {
                results[locale] = InstallResult.ALREADY_INSTALLED
                onProgressUpdate?.invoke(
                    results.size,
                    languages.size,
                    locale.displayLanguage,
                    "Already installed"
                )
            } else {
                installQueue.add(locale)
            }
        }

        isInstalling = false
        installNext()
    }

    private fun isLanguageInstalled(locale: Locale): Boolean {
        return textToSpeech?.let { tts ->
            val result = tts.isLanguageAvailable(locale)
            result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        } ?: false
    }

    private fun installNext() {
        if (isInstalling || installQueue.isEmpty()) {
            if (installQueue.isEmpty()) {
                onCompleted?.invoke(results.toMap())
            }
            return
        }

        val locale = installQueue.first()
        isInstalling = true

        onProgressUpdate?.invoke(
            results.size,
            results.size + installQueue.size,
            locale.displayLanguage,
            "Installing..."
        )

        installLanguage(locale) { result ->
            results[locale] = result
            installQueue.removeFirst()
            isInstalling = false
            installNext()
        }
    }

    private fun installLanguage(locale: Locale, onResult: (InstallResult) -> Unit) {
        textToSpeech?.let { tts ->
            when (tts.isLanguageAvailable(locale)) {
                TextToSpeech.LANG_MISSING_DATA -> {
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    context.startActivity(installIntent)

                    val listener = TextToSpeech.OnInitListener { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            if (isLanguageInstalled(locale)) {
                                onResult(InstallResult.INSTALLATION_SUCCESS)
                            } else {
                                onResult(InstallResult.INSTALLATION_FAILED)
                            }
                        } else {
                            onResult(InstallResult.INSTALLATION_FAILED)
                        }
                    }

                    textToSpeech = TextToSpeech(context, listener)
                }
                TextToSpeech.LANG_NOT_SUPPORTED -> onResult(InstallResult.NOT_SUPPORTED)
                else -> {
                    if (isLanguageInstalled(locale)) {
                        onResult(InstallResult.ALREADY_INSTALLED)
                    } else {
                        onResult(InstallResult.INSTALLATION_FAILED)
                    }
                }
            }
        }
    }

    fun cleanup() {
        textToSpeech?.shutdown()
        textToSpeech = null
        installQueue.clear()
        results.clear()
    }
}