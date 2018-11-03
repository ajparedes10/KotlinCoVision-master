package com.covision.covisionapp.structures

import android.graphics.Color
import android.os.AsyncTask
import android.util.Log

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil

import java.io.IOException

class GetDirectionsDataRoutes : AsyncTask<Any, String, String>() {

    internal lateinit var mMap: GoogleMap
    internal lateinit var url: String
    internal lateinit var googleDirectionsData: String
    internal var duration: String? = null
    internal var distance: String? = null
    internal lateinit var latLng: LatLng

    override fun doInBackground(vararg objects: Any): String {
        var ob = objects.get(0) as Array <Any>
        mMap = ob.get(0) as GoogleMap
        url = ob.get(1) as String
        val dwnu = DownloadUrl()
        latLng = ob.get(2) as LatLng
        try {
            Log.i("getDirectionsdata Route", "la url es "+ url!!.toString())
            googleDirectionsData = dwnu.readUrl(url)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return googleDirectionsData
    }

    override fun onPostExecute(s: String) {

        val directionList: Array<String?>?
        val parser = DataParser()
        directionList = parser.parseDirectionsPaint(s)
        if(directionList != null) displayDirection(directionList)

    }

    fun displayDirection(directionsList: Array<String?>) {
        val count = directionsList.size
        for (i in 0 until count) {
            val options = PolylineOptions()
            options.color(Color.RED)
            options.width(10f)
            options.addAll(PolyUtil.decode(directionsList[i]))
            mMap.addPolyline(options)


        }
    }
}
