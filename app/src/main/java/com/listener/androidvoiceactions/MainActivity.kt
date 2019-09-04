package com.listener.androidvoiceactions

import android.Manifest
import android.app.Activity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity(), RecognitionListener {
    private val response_code_basic_voice = 10
    private val response_code_audio_permission = 15

    private lateinit var textMessage: TextView
    private lateinit var talkButton: Button
    private lateinit var talkToggleButton: ToggleButton

    private var speech: SpeechRecognizer? = null

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_simple -> {
                talkButton.visibility = View.VISIBLE
                talkToggleButton.visibility = View.GONE
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_advanced -> {
                talkButton.visibility = View.GONE
                talkToggleButton.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private val onBasicTalkButtonPressedListener = View.OnClickListener { button ->
        startActivityForResult(requestBasicSpeechRecognizer(), response_code_basic_voice)

    }

    private val onTalkToggleButtonPressedListener = CompoundButton.OnCheckedChangeListener { button, isChecked ->
        if (isChecked) {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    response_code_audio_permission
                )
            } else {
                Toast.makeText(
                    this@MainActivity, "Device does not support Voice Recognition", Toast.LENGTH_SHORT).show()
            }
        } else {

            speech?.stopListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        textMessage = findViewById(R.id.message)
        talkButton = findViewById(R.id.button_action)
        talkToggleButton = findViewById(R.id.toggle_button_action)

        talkButton.setOnClickListener(onBasicTalkButtonPressedListener)
        talkToggleButton.setOnCheckedChangeListener(onTalkToggleButtonPressedListener)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        speech = SpeechRecognizer.createSpeechRecognizer(this)
        speech?.setRecognitionListener(this)
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onStop() {
        super.onStop()
        speech?.apply {
            speech?.destroy()
        }
    }

    private fun requestBasicSpeechRecognizer(): Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What is your command")
        return intent
    }

    private fun requestComplexSpeechRecognizer() : Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, 3)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)


        return intent
    }

    // This callback is invoked when the Speech Recognizer returns.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == response_code_basic_voice && resultCode == Activity.RESULT_OK) {
            val results = data!!.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            val spokenText = results!![0]
            textMessage.text = spokenText
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            response_code_audio_permission -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this@MainActivity, "App Does not have permission", Toast.LENGTH_SHORT).show()
                } else {
                    speech?.startListening(requestComplexSpeechRecognizer())
                }
            }
        }
    }

    override fun onReadyForSpeech(argsIn: Bundle?) {
        //speech engine ready
    }

    override fun onRmsChanged(p0: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        //unused
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        //unused
    }

    override fun onBeginningOfSpeech() {

    }

    override fun onEndOfSpeech() {
        talkToggleButton.isChecked = false
    }

    override fun onError(errorCode: Int) {
        var message = "There was an error"
        when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> message = "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> message = "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> message = "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> message = "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> message = "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> message = "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> message = "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> message = "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> message = "No speech input"
            else -> message = "Didn't understand, please try again."
        }

        textMessage.text = message
        talkToggleButton.isChecked = false
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.apply {
            var text = ""
            for (result in matches)
                text += result + "\n"

            textMessage.text = text
        }

    }

    override fun onPartialResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.apply {
            var text = "partial: "
            for (result in matches)
                text += result + "\n"

            textMessage.text = text
        }
    }
}