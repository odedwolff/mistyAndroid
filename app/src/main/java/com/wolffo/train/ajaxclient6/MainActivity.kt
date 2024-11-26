package com.wolffo.train.ajaxclient6

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.util.Log
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import java.util.Locale
import kotlinx.coroutines.*
import android.content.Intent
import android.os.Build
import android.widget.CheckBox
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class MainActivity : AppCompatActivity(){

    private lateinit var textToSpeech: TextToSpeech

    private var genText : String = "default get text"
    private var translation : String = "default translation"

    // Create a list of items for the Spinner
    //val countries = listOf("India", "USA", "UK", "Australia")
    val delayItemNames = listOf("1 sec", "2 sec", "3 sec", "4 sec", "5 sec", "6 sec", "7 sec", "8 sec")
    val delayItemVals = listOf(1000L, 2000L, 3000L, 4000L, 5000L, 6000L, 7000L, 8000L)

    val speechRateVals = listOf(2f, 1.5f, 1f, .666f, .333f)
    val speechRateNames = listOf("x2", "x1.5", "normal", "2/3", "1/3")


    var languagesNames = listOf("Italian", "Spanish", "French", "German", "Russian")
    var languagesValues= listOf("Italian", "Spanish", "French", "German", "Russian")
    var localeCodes = listOf("it", "es", "fr", "de", "ru")

    var levels = listOf("A1", "A2", "B1", "B2", "C1")


    var anotherRound : Boolean = true

    var parSpeechRate :  Float = 1f
    var parLang : String = "german"
    var parLocale : String = "de"
    var parDelayBeforeSolution : Long = 4000
    var parLevel : String = "B1"
    var parRevLangOrder : Boolean = false
    var parRepeat : Boolean = false


    private lateinit var localBroadcastManager: LocalBroadcastManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("TAG", "test foreground service")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textView = findViewById<TextView>(R.id.textView1)

        val buttonAjax = findViewById<Button>(R.id.buttonStart)
        val buttonStop = findViewById<Button>(R.id.buttonStop)
        buttonAjax.isEnabled = true
        buttonStop.isEnabled = false

        buttonAjax.setOnClickListener{
            buttonAjax.isEnabled = false
            buttonStop.isEnabled = true
            startFGService()


            //anotherRound = true
            //testSendAjax()

            //sendMessageToServiceTest()
        };

        buttonStop.setOnClickListener{
            //anotherRound = false
            buttonAjax.isEnabled = true
            buttonStop.isEnabled = false
            stopMyFGService()
        };

        //textToSpeech = TextToSpeech(this, this)

        createSpinnderDelay()
        createSpinnderLanguage()
        createSpinnerRate()
        createSpinnerLevel()
        setupCheckBoxLangOrder()
        setupCheckBoxRepeat()


        //startFGService()


        // Get instance of LocalBroadcastManager
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }


    fun testSendAjaxDel(){
        Log.d("Flow", "@testSendAjax()")
        val requestQueue = Volley.newRequestQueue(this)
        val textView = findViewById<TextView>(R.id.textView1)


        //val url = "http://10.0.2.2:3000/simpleCycle"
        val url = "https://lstream.onrender.com/simpleCycle"


        //const data = {lang:langInfo.langName, level:level, maxLen: maxLen};

        val jsonBody = JSONObject().apply {
            //put("lang", "italian")
            put("lang", "russian")
            put("level", "A2")
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                // Handle the successful response
                try {
                    Log.d("Flow", "Response: $response")
                    //val result = response.getString("result")
                    // Do something with the result
                    //val objResult = response.getJSONObject("result")
                    genText = response.getString("genText")
                    translation = response.getString("translation")
                    Log.d("flow", "getnText=$genText")
                    textView.text = genText
//                    thread {
//                        speakPart1()
//                    }
                    speakPart1Old()
                } catch (e: Exception) {
                    Log.e("api-err", e.toString())
                    Log.e("flow", e.toString())
                    e.printStackTrace()
                    //testSendAjax()
                }
            },
            { error ->
                // Handle errors
                error.printStackTrace()
                Log.e("flow", "error server: $error")

                //testSendAjax()

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

     fun onInitDel(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language
            val result = textToSpeech.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle error
                println("Language is not supported")
            } else {
                // Speak out the text
                //speakOut("Hello, this is a Text to Speech example.")
            }
        } else {
            // Initialization failed
            println("Initialization Failed!")
        }
    }




    private fun testTextToSpeech(text: String) {
        Log.d("Flow", "speeak out")
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun speakPart1Old(){
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Called when speech starts
            }

            override fun onDone(utteranceId: String?) {
                // Called when speech is completed
                Log.d("flow", "Speech completed")
//                thread {
//                    speakPart2()
//                }
                Thread.sleep(parDelayBeforeSolution)
                speakPart2()
            }

            override fun onError(utteranceId: String?) {
                // Called if an error occurs during speech
                println("Error during speech")
            }
        })

// Use the speak() function and pass a unique utterance ID
        //textToSpeech.setLanguage(Locale.US)
        textToSpeech.setLanguage(Locale("ru"))
        textToSpeech.setSpeechRate(parSpeechRate)
        textToSpeech.speak(genText, TextToSpeech.QUEUE_FLUSH, null, "utteranceID")
    }

    fun speakPart2(){
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Called when speech starts
            }

            override fun onDone(utteranceId: String?) {
                // Called when speech is completed
                Log.d("flow","Speech part 2 completed")
//                thread{
//                    testSendAjax()
//                }
                if(anotherRound){
                    //testSendAjax()
                }
            }

            override fun onError(utteranceId: String?) {
                // Called if an error occurs during speech
                println("Error during speech")
            }
    })

        textToSpeech.setLanguage(Locale.ENGLISH)
        textToSpeech.speak(translation, TextToSpeech.QUEUE_FLUSH, null, "utteranceID")
    }


    override fun onDestroy() {
        // Shutdown TTS when activity is destroyed
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        stopMyFGService()
        super.onDestroy()
    }

    fun createSpinnderDelay(){
        // Create a    Spinner instance
        val spinner: Spinner = findViewById(R.id.spinnerDelay)



        // Create an ArrayAdapter to populate the Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, delayItemNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        // Set the adapter to the Spinner
        spinner.adapter = adapter


        // Set an OnItemSelectedListener to handle item selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                var selectedDelayTime = delayItemVals[position]
                Toast.makeText(this@MainActivity, "Selected: $selectedDelayTime", Toast.LENGTH_SHORT).show()
                parDelayBeforeSolution = selectedDelayTime
                sendUpdateParmas()
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
                // Handle the case when nothing is selected
            }
        }
    }

    fun createSpinnderLanguage(){
        // Create a    Spinner instance
        val spinner: Spinner = findViewById(R.id.spinnerLang)

        // Create an ArrayAdapter to populate the Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languagesNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        // Set the adapter to the Spinner
        spinner.adapter = adapter


        // Set an OnItemSelectedListener to handle item selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                var selLang = languagesValues[position]
                var selLocaleCode = localeCodes[position]
                Toast.makeText(this@MainActivity, "Selected: $selLang", Toast.LENGTH_SHORT).show()
                //selectedLang = selLang
                //sendLangUpdate(selLang, selLocaleCode)
                parLang = selLang
                parLocale = selLocaleCode
                sendUpdateParmas()
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
                // Handle the case when nothing is selected
            }
        }
    }

    fun createSpinnerRate(){
        // Create a    Spinner instance
        val spinner: Spinner = findViewById(R.id.spinnerRate)

        // Create an ArrayAdapter to populate the Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speechRateNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        // Set the adapter to the Spinner
        spinner.adapter = adapter


        // Set an OnItemSelectedListener to handle item selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                var selRate = speechRateVals[position]
                Toast.makeText(this@MainActivity, "Selected: $selRate", Toast.LENGTH_SHORT).show()
                parSpeechRate = selRate
                sendUpdateParmas()
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
                // Handle the case when nothing is selected
            }
        }
    }

    fun createSpinnerLevel(){
        // Create a    Spinner instance
        val spinner: Spinner = findViewById(R.id.spinnerLevel)

        // Create an ArrayAdapter to populate the Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        // Set the adapter to the Spinner
        spinner.adapter = adapter


        // Set an OnItemSelectedListener to handle item selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                var selLevel = levels[position]
                Toast.makeText(this@MainActivity, "Selected: $selLevel", Toast.LENGTH_SHORT).show()
                parLevel = selLevel
                sendUpdateParmas()
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
                // Handle the case when nothing is selected
            }
        }
    }


    fun setupCheckBoxLangOrder(){
        val checkbox = findViewById<CheckBox>(R.id.checkBoxRevOrder)
        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Checkbox is checked
                Toast.makeText(this, "Checked!", Toast.LENGTH_SHORT).show()
                parRevLangOrder = true
            } else {
                // Checkbox is unchecked
                Toast.makeText(this, "Unchecked!", Toast.LENGTH_SHORT).show()
                parRevLangOrder = false
            }
            sendUpdateParmas()
        }

    }

    fun setupCheckBoxRepeat(){
        val checkbox = findViewById<CheckBox>(R.id.checkBoxRepeat)
        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Checkbox is checked
                Toast.makeText(this, "Checked!", Toast.LENGTH_SHORT).show()
                parRepeat = true
            } else {
                // Checkbox is unchecked
                Toast.makeText(this, "Unchecked!", Toast.LENGTH_SHORT).show()
                parRepeat = false
            }
            sendUpdateParmas()
        }

    }







    fun startFGService(){
        //val serviceIntent = Intent(this, FGService::class.java)
        //startService(serviceIntent)

        Intent(this, FGService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun stopMyFGService() {
        try {
            // Create an intent matching the one used to start the service
            val serviceIntent = Intent(this, FGService::class.java)

            // Stop the foreground service
            stopService(serviceIntent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping service: ${e.message}")
        }
    }

    private fun sendMessageToServiceTest() {
        // Create an Intent with a specific action
        val intent = Intent("MY_CUSTOM_ACTION")

        // Add data to the intent
        intent.putExtra("message", "Hello Service!")
        intent.putExtra("timestamp", System.currentTimeMillis())

        // Broadcast the message
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun sendLangUpdate(selLang : String, selLocaleCode : String) {
        // Create an Intent with a specific action
        val intent = Intent("MY_CUSTOM_ACTION")

        // Add data to the intent
        intent.putExtra("language", selLang)
        intent.putExtra("localeCode", selLocaleCode)
        intent.putExtra("timestamp", System.currentTimeMillis())

        // Broadcast the message
        localBroadcastManager.sendBroadcast(intent)
    }


    fun sendUpdateParmas() {
        // Create an Intent with a specific action
        val intent = Intent("MY_CUSTOM_ACTION")

        // Add data to the intent
        intent.putExtra("language", parLang)
        intent.putExtra("localeCode", parLocale)
        intent.putExtra("delay", parDelayBeforeSolution)
        intent.putExtra("level", parLevel)
        intent.putExtra("speechRate", parSpeechRate)
        intent.putExtra("reverseOrder", parRevLangOrder)
        intent.putExtra("repeat", parRepeat)





        intent.putExtra("timestamp", System.currentTimeMillis())

        // Broadcast the message
        localBroadcastManager.sendBroadcast(intent)
    }





}