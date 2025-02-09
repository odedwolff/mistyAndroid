package com.wolffo.train.ajaxclient6



import SimpleLanguageInstaller
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.Locale


class FGService : Service(), TextToSpeech.OnInitListener{
    private val TAG = "MyService"

    private val SLEEP_BEFORE_SAY_AGAIN: Long = 2000

    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var broadcastReceiver: BroadcastReceiver

    private var wakeLock: PowerManager.WakeLock? = null

    private var repsToGo : Int = 1


    companion object {
        private const val CHANNEL_ID = "TtsServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var textToSpeech: TextToSpeech

    private var genText : String = "default get text"
    private var translation : String = "default translation"
    private var isRunning = false

    private var _language : String? = "spanish"
    private var _localeCode : String? = "es"
    private var _delayMS : Long = 2000
    private var _speechRate : Float = 1.0f
    private var _level : String? = "B1"
    private var _revLangOrder : Boolean = false


    private var text1 : String? = null
    private var local1 : String? = null
    private var rate1 : Float = 1f
    private var text2 : String? = null
    private var local2 : String? = null
    private var rate2 : Float = 1f

    private var languageInstaller : SimpleLanguageInstaller? = null



    var repeated : Boolean = false

    var _shouldRepeat : Boolean = false




    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")

        textToSpeech = TextToSpeech(this, this)



        languageInstaller = SimpleLanguageInstaller()

        // Get instance of LocalBroadcastManager
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Create broadcast receiver
        broadcastReceiver = object : BroadcastReceiver() {
             fun onReceiveOld(context: Context?, intent: Intent?) {
                intent?.let { receivedIntent ->
                    // Handle the received message
                    val message = receivedIntent.getStringExtra("message")
                    val timestamp = receivedIntent.getLongExtra("timestamp", 0L)

                    // Do something with the received data
                    Log.d("FGService", "Received message: $message at $timestamp")
                }
            }

            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { receivedIntent ->
                    // Handle the received message
                    _language = receivedIntent.getStringExtra("language")
                    _localeCode = receivedIntent.getStringExtra("localeCode")
                    _delayMS = receivedIntent.getLongExtra("delay", 2000)
                    _level = receivedIntent.getStringExtra("level")
                    _speechRate = receivedIntent.getFloatExtra("speechRate", 1.0f)
                    _revLangOrder =  receivedIntent.getBooleanExtra("reverseOrder", false)
                    _shouldRepeat =  receivedIntent.getBooleanExtra("repeat", false)



                    val timestamp = receivedIntent.getLongExtra("timestamp", 0L)


                    // Do something with the received data
                    Log.d("FGService", "received parameters update at at $timestamp:")
                    Log.d("FGService", "language: $_language ")
                    Log.d("FGService", "delay: $_delayMS ")
                    Log.d("FGService", "level: $_level ")
                    Log.d("FGService", "Speech Rate: $_speechRate ")
                    Log.d("FGService", "Reversed Language Order: $_revLangOrder ")
                    Log.d("FGService", "Should Repeat: $_shouldRepeat ")

                }
            }
        }

        // Register the receiver with a specific action filter
        val intentFilter = IntentFilter("MY_CUSTOM_ACTION")
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter)


    }


    fun setLangOrder(){
        if(_revLangOrder){
            text1 = translation
            local1 = "en"
            rate1 = 1f
            text2 = genText
            local2 = _localeCode
            rate2 = _speechRate
        }
        else{
            text2 = translation
            local2 = "en"
            rate2 = 1f
            text1 = genText
            local1 = _localeCode
            rate1 = _speechRate
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        // Add your service logic here
        //return START_STICKY

        if(intent != null){
            _language = intent.getStringExtra("language")
            _localeCode = intent.getStringExtra("localeCode")
            _delayMS = intent.getLongExtra("delay", 2000)
            _level = intent.getStringExtra("level")
            _speechRate = intent.getFloatExtra("speechRate", 1.0f)
            _revLangOrder =  intent.getBooleanExtra("reverseOrder", false)
            _shouldRepeat =  intent.getBooleanExtra("repeat", false)
        }





        startForeground(NOTIFICATION_ID, createNotification())
        isRunning = true

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:AudioLoopWakeLock")
        wakeLock?.acquire()


        return START_STICKY

    }

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TTS Service Running")
            .setContentText("Generating speech in background")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        localBroadcastManager.unregisterReceiver(broadcastReceiver)
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    override fun onInit(status: Int) {
        Log.d(TAG, "On init()")

        if (status == TextToSpeech.SUCCESS) {
            Log.d("flow", "TTS initialized with success")
            Log.d("flow", "available languages:${textToSpeech.availableLanguages}")

            // Set the language
            val result = textToSpeech.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle error
                println("Language is not supported")
            } else {

                sendTextRequest()
            }
        } else {
            // Initialization failed
            println("Initialization Failed!")
            Log.e("flow", "TTS failed to initialized !")
        }
    }


    public fun sendTextRequest(){
        Log.d("Flow", "@testSendAjax()")
        val requestQueue = Volley.newRequestQueue(this)
        //val textView = findViewById<TextView>(R.id.textView1)


        //val url = "http://10.0.2.2:3000/simpleCycle"
        val url = "https://lstream.onrender.com/simpleCycle"


        //const data = {lang:langInfo.langName, level:level, maxLen: maxLen};

        val jsonBody = JSONObject().apply {
            put("lang", _language)
            put("level", _level)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                // Handle the successful response
                try {
                    Log.d("Flow", "Response: $response")
                    genText = response.getString("genText")
                    translation = response.getString("translation")
                    Log.d("flow", "getnText=$genText")

                    setLangOrder()
                    speakPart1()
                } catch (e: Exception) {
                    Log.e("api-err", e.toString())
                    Log.e("flow", e.toString())
                    e.printStackTrace()
                    sendTextRequest()
                }
            },
            { error ->
                // Handle errors
                error.printStackTrace()
                Log.e("flow", "error server: $error")

                sendTextRequest()

            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            20*1000,
            //DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            5,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )


        Log.d("Flow", "sending it, i guess()")
        requestQueue.add(jsonObjectRequest)
    }

    fun speakPart1(){
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Called when speech starts
            }

            override fun onDone(utteranceId: String?) {
                // Called when speech is completed
                Log.d("flow", "Speech completed")
                Thread.sleep(_delayMS)
                speakPart2()
            }

            override fun onError(utteranceId: String?) {
                // Called if an error occurs during speech
                println("Error during speech")
            }
        })

// Use the speak() function and pass a unique utterance ID

        val result = textToSpeech.setLanguage(Locale(local1))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("flow", "language{$local1} not supported")
        }
        textToSpeech.setSpeechRate(rate1)
        textToSpeech.speak(text1, TextToSpeech.QUEUE_FLUSH, null, "utteranceID")
    }


    fun speakPart2(){
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Called when speech starts
            }

            override fun onDone(utteranceId: String?) {
                // Called when speech is completed
                Log.d("flow","Speech part 2 completed")


                //either repeat current sentene 2 stages or do a new one
                if(_shouldRepeat && !repeated){
                    Thread.sleep(SLEEP_BEFORE_SAY_AGAIN)
                    repeated = true
                    speakPart1()
                 }else{
                    repeated = false
                    sendTextRequest()
                }

            }
            override fun onError(utteranceId: String?) {
                // Called if an error occurs during speech
                println("Error during speech")
            }
        })

        val result =textToSpeech.setLanguage(Locale(local2))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("flow", "language{$local2} not supported")
        }
        textToSpeech.setSpeechRate(rate2)
        textToSpeech.speak(text2, TextToSpeech.QUEUE_FLUSH, null, "utteranceID")

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TTS Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }


    fun showConfirmationDialogBlocking(context: Context): Boolean {
        var result = false
        val dialog = AlertDialog.Builder(context)
            .setTitle("Confirmation")
            .setMessage("Do you want to proceed?")
            .setPositiveButton("Yes") { _, _ ->
                result = true
            }
            .setNegativeButton("No") { _, _ ->
                result = false
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // Block until the dialog is dismissed
        while (dialog.isShowing) {
            Thread.sleep(50)  // Short sleep to prevent high CPU usage
        }

        return result
    }



}