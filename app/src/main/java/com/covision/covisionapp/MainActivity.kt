package com.covision.covisionapp

import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout

import com.covision.covisionapp.fragments.MapsFragment
import com.covision.covisionapp.fragments.ObjectDetectionFragment
import com.covision.covisionapp.fragments.VoiceFragment
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import com.tomer.fadingtextview.FadingTextView

import kotlinx.android.synthetic.main.activity_main.*;


class MainActivity : AppCompatActivity(), View.OnClickListener {

    val REQUEST_ALL = 100
    val REQUEST_CAMERA = 200
    val REQUEST_LOCATION = 400

    lateinit var voice: VoiceFragment
    lateinit var maps: MapsFragment
    lateinit var objectDetection: ObjectDetectionFragment
    lateinit var fragmentManager: FragmentManager

    //private var speakButton: Button? = null
    lateinit var detectionView: FrameLayout
    lateinit var mapView: FrameLayout

    lateinit var fadingTextView: FadingTextView
    var mapsHidden = true
    var detectionHidden = true
    var savedInstanceSt: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceSt = savedInstanceState

        // Boton principal
        btnMic.setOnClickListener(this)
        // fading text on start
        fadingTextView = findViewById(R.id.fading_text_view)
        // Fragmentos
        fragmentManager = supportFragmentManager
        if (!isNetworkAvailable()) {
            displayContextualInfoOnNoInternet()
            turnOnWifiRequest()
        }
        if (savedInstanceState == null) {
            voice = VoiceFragment()
            fragmentManager.beginTransaction().add(R.id.voiceFragment, voice).commit()
            if(checkInternet()) {
                maps = MapsFragment()
                fragmentManager.beginTransaction().add(R.id.mapsFragment, maps).commit()
                objectDetection = ObjectDetectionFragment()
                fragmentManager.beginTransaction().add(R.id.objectDetectionFragment, objectDetection).commit()
            }
            else{
                voice!!.textToVoice("No tienes conexión a internet. Intenta más tarde")
            }
        }

        val PERMISSIONS = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)

        if (!hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_ALL)
        }
    }

    private fun displayContextualInfoOnNoInternet() {
        val t = arrayOf("Revisa tu conexion a internet", "Prende el Wifi", "Sal del sotano", "App no apta para ascensores")
        fadingTextView.setTexts(t)
        fadingTextView.setTextSize(38f)
        Toast.makeText(this, "Please check your internet connection state", Toast.LENGTH_LONG).show()
        if (voice != null) {
            voice.textToVoice("No tienes conexion a internet. Intenta más tarde")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    override fun onStart() {
        mapView = findViewById(R.id.mapsFragment)
        detectionView = findViewById(R.id.objectDetectionFragment)
        super.onStart()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted
            } else {
                // Not granted
            }
            Companion.REQUEST_RECORD -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted
            } else {
                // Not granted
            }
            REQUEST_LOCATION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted
            } else {
                // Not granted
            }
        }
    }

    /*
    onClick del boton principal
     */
    override fun onClick(v: View) {
        if (isNetworkAvailable()) {
            val f = arrayOf("")
            fadingTextView.setTexts(f)

            if (v.id == R.id.btnMic) {
                if (checkInternet()) {
                    if (!mapsHidden) hideMaps()
                    if (!detectionHidden) hideObjectDetection()
                    voice.recordSpeak(object : VoiceFragment.VoiceCallback {
                        override fun onSpeechResult(result: VoiceFragment.VoiceResult, vararg params: String) {
                            when (result) {
                                VoiceFragment.VoiceResult.Location -> {
                                    voice.textToVoice(maps.showCurrentPlace())
                                    showMaps()
                                }
                                VoiceFragment.VoiceResult.Route -> {
                                    voice.textToVoice("Calculando ruta hacia " + params[0])
                                    try {
                                        Thread.sleep(2400)
                                    } catch (e: InterruptedException) {
                                        // Process exception
                                    }

                                    showMaps()
                                    var res = maps.geoLocate(params[0])
                                    if (res != "error") {
                                        res = res.replace(".", "=")
                                        val d = res.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                        Log.d("QUE FOCO ", res)
                                        voice.textToVoice("estas a una distancia de " + d[0] + " metros")
                                    } else {
                                        voice.textToVoice("No se pudo calcular la distancia hasta su destino")
                                    }
                                }
                                VoiceFragment.VoiceResult.Detection -> {
                                    voice.textToVoice("Iniciando análisis de imagen")
                                    showObjectDetection()
                                    objectDetection.detect(object : ObjectDetectionFragment.DetectionMessageCallback {
                                        override fun onDetectionResult(result: String) {
                                            voice.textToVoice(result)
                                        }

                                        override fun onError(message: String) {
                                            voice.textToVoice(message)
                                        }
                                    })
                                }
                            }
                        }

                        override fun onError(message: String) {
                            voice.textToVoice(message)
                        }
                    })
                } else {
                    if (voice != null)
                        voice.textToVoice("No tienes conexion a internet. Intenta más tarde")
                }
            }
        } else {
            displayContextualInfoOnNoInternet()
            turnOnWifiRequest()
            if (isNetworkAvailable()) {
                Toast.makeText(this,
                        "Detected Internet Conection - Back Online!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showMaps() {
        val animX = ValueAnimator.ofFloat((2 * mapView.width).toFloat(), 0F)
        animX.duration = 500
        animX.addUpdateListener { animation -> mapView.translationX = animation.animatedValue as Float }
        animX.start()
        mapsHidden = false
    }

    private fun hideMaps() {
        val animX = ValueAnimator.ofFloat(0F, (2*mapView.width).toFloat())
        animX.duration = 500
        animX.addUpdateListener { animation -> mapView.translationX = animation.animatedValue as Float }
        animX.start()
        mapsHidden = true
    }

    private fun showObjectDetection() {
        val animX = ValueAnimator.ofFloat((-2 * detectionView.width).toFloat(), 0F)
        animX.duration = 500
        animX.addUpdateListener { animation -> detectionView.translationX = animation.animatedValue as Float }
        animX.start()
        detectionHidden = false
    }

    private fun hideObjectDetection() {
        val animX = ValueAnimator.ofFloat(0F, (-2 * detectionView.width).toFloat())
        animX.duration = 500
        animX.addUpdateListener { animation -> detectionView.translationX = animation.animatedValue as Float }
        animX.start()
        detectionHidden = true
    }

    fun setMargins(v: View, l: Int, t: Int, r: Int, b: Int) {
        if (v.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = v.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(l, t, r, b)
            v.requestLayout()
        }
    }


    fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    private fun checkInternet (): Boolean{
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED){
            return true
        }
        return false
    }

    private fun turnOnWifiRequest() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Do you want to turn WIFI ON?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifi.isWifiEnabled = true// true or false to activate/deactivate wifi
                }
                .setNegativeButton("No") { dialog, id ->
                    // Do Nothing or Whatever you want.
                    dialog.cancel()
                }
        val alert = builder.create()
        alert.show()
    }

    companion object {
        const val REQUEST_RECORD = 300
    }

}
