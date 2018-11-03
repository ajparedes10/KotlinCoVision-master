package com.covision.covisionapp.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.covision.covisionapp.MainActivity
import com.covision.covisionapp.R
import java.text.Normalizer
import java.util.Locale

class VoiceFragment : Fragment() {
    private var progressBar: ProgressBar? = null

    private var toSpeech: TextToSpeech? = null
    private var res: Int = 0
    private var sr: SpeechRecognizer? = null
    private val LOG_TAG = "VoiceFragment"
    private val options = arrayOf("llevame a", "donde estoy", "frente", "adelante")

    private var callback: VoiceCallback? = null

    enum class VoiceResult {
        Location,
        Route,
        Detection
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // infla el layout del fragmento
        val myView = inflater.inflate(R.layout.fragment_voice, container, false)

        progressBar = myView.findViewById(R.id.progressBar)
        progressBar!!.visibility = View.INVISIBLE

        //crea el SpeechRecognizer y su listener
        sr = SpeechRecognizer.createSpeechRecognizer(activity)
        sr!!.setRecognitionListener(listener())

        toSpeech = TextToSpeech(this.context, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                res = toSpeech!!.setLanguage(Locale("es", "ES"))
            }
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(activity, "Tu dispositivo no soporta la función de text to speech", Toast.LENGTH_SHORT).show()
            }
        })

        return myView
    }

    interface VoiceCallback {
        fun onSpeechResult(result: VoiceResult, vararg params: String)
        fun onError(message: String)
    }

    override fun onDestroy() {
        sr!!.destroy()
        sr = null
        super.onDestroy()
    }

    /*
     * listener del SpeechRecognizer.
     */
    internal inner class listener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle) {
            Log.d(LOG_TAG, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {

            Log.d(LOG_TAG, "onBeginningOfSpeech")
            progressBar!!.isIndeterminate = false
            progressBar!!.max = 10
        }

        override fun onRmsChanged(rmsdB: Float) {
            Log.d(LOG_TAG, "onRmsChanged")
            progressBar!!.progress = rmsdB.toInt()
        }

        override fun onBufferReceived(buffer: ByteArray) {
            Log.d(LOG_TAG, "onBufferReceived")
        }

        override fun onEndOfSpeech() {
            Log.d(LOG_TAG, "onEndofSpeech")
            progressBar!!.isIndeterminate = true
            progressBar!!.visibility = View.INVISIBLE
        }

        override fun onError(error: Int) {
            Log.d(LOG_TAG, "error $error")
        }

        override fun onResults(results: Bundle) {

            Log.d(LOG_TAG, "onResults $results")
            // Lista de resultados obtenidos por el SpeechRecognizer
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            //guarda el primer resultado
            if (matches != null && !matches.isEmpty())
                logthis(matches[0])

        }

        override fun onPartialResults(partialResults: Bundle) {
            Log.d(LOG_TAG, "onPartialResults")
        }

        override fun onEvent(eventType: Int, params: Bundle) {
            Log.d(LOG_TAG, "onEvent $eventType")
        }
    }

    fun recordSpeak(callback: VoiceCallback) {
        this.callback = callback
        //si la app no tiene permiso para usar microfono, lo pide
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG, "asking for permissions")
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.RECORD_AUDIO), MainActivity.REQUEST_RECORD)
        } else {
            progressBar!!.visibility = View.VISIBLE
            progressBar!!.isIndeterminate = true

            //se crea el intent para escuchar al usuario
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            sr!!.startListening(intent)
            Log.i(LOG_TAG, "Intent sent")
        }
    }

    /*
     * método para añadir la información resivida al TextView, analizarla y reproducirla
     */
    fun logthis(newinfo: String) {
        if (newinfo.compareTo("") != 0) {
            analizeSpeech(newinfo)
        }
    }

    /*
     * método que debe revisar el comando recibido por el usuario
     */
    fun analizeSpeech(sp : String) {
        var speech = sp
        speech = Normalizer.normalize(speech, Normalizer.Form.NFD)
        speech = Regex("\\p{InCombiningDiacriticalMarks}+").replace(speech, "")
        speech = speech.toLowerCase()
        var opt = -1
        Log.i(LOG_TAG, "analize: normalizó a $speech")
        var i = 0
        while (i < options.size && opt == -1) {
            if (speech.contains(options[i])) opt = i
            i++
        }
        Log.i(LOG_TAG, "analize: opcion $opt")
        when (opt) {
            0 -> if (speech.contains(" a ")) {
                //puede ser a, al, a la
                val div = speech.split(" a ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                this.callback?.onSpeechResult(VoiceResult.Route, div[1])
            }
            1 -> this.callback?.onSpeechResult(VoiceResult.Location)
            2 -> this.callback?.onSpeechResult(VoiceResult.Detection)
            3 -> this.callback?.onSpeechResult(VoiceResult.Detection)

            else -> this.callback?.onError("Lo siento, esa no es una opción disponible. Intenta de nuevo porfavor")
        }

    }

    /*
     * método para pasar de texto a voz
     */
    fun textToVoice(message: String?) {
        if (message != null) {
            Log.i(LOG_TAG, "entra else textToSpeach")
            toSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}
