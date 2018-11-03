package com.covision.covisionapp.structures

import android.util.Log

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.HashMap


class DataParser {


    private fun getDuration(googleDirectionsJson: JSONArray?): HashMap<String, String> {
        val googleDirectionsMap = HashMap<String, String>()
        var duration = ""
        var distance = ""

        if (googleDirectionsJson != null) {
            Log.d("Json RESPONSE ", googleDirectionsJson.toString())
            try {
                duration = googleDirectionsJson.getJSONObject(0).getJSONObject("duration").getString("text")
                distance = googleDirectionsJson.getJSONObject(0).getJSONObject("distance").getString("text")
                googleDirectionsMap["duration"] = duration
                googleDirectionsMap["distance"] = distance
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        } else {
            googleDirectionsMap["duration"] = " No hay duracion disponible "
            googleDirectionsMap["distance"] = " No hay ruta exacta disponible "
        }
        return googleDirectionsMap
    }

    private fun getPlace(googlePlaceJson: JSONObject): HashMap<String, String> {
        val googlePlaceMap = HashMap<String, String>()
        var placeName = "--NA--"
        var vicinity = "--NA--"
        var latitude = ""
        var longitude = ""
        var reference = ""

        Log.d("DataParser", "jsonobject =" + googlePlaceJson.toString())


        try {
            if (!googlePlaceJson.isNull("name")) {
                placeName = googlePlaceJson.getString("name")
            }
            if (!googlePlaceJson.isNull("vicinity")) {
                vicinity = googlePlaceJson.getString("vicinity")
            }

            latitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lat")
            longitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lng")

            reference = googlePlaceJson.getString("reference")

            googlePlaceMap["place_name"] = placeName
            googlePlaceMap["vicinity"] = vicinity
            googlePlaceMap["lat"] = latitude
            googlePlaceMap["lng"] = longitude
            googlePlaceMap["reference"] = reference


        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return googlePlaceMap

    }

    private fun getPlaces(jsonArray: JSONArray): List<HashMap<String, String>> {
        val count = jsonArray.length()
        val placelist = ArrayList<HashMap<String, String>>()
        var placeMap: HashMap<String, String>? = null

        for (i in 0 until count) {
            try {
                placeMap = getPlace(jsonArray.get(i) as JSONObject)
                placelist.add(placeMap)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return placelist
    }

    fun parseDirections(jsonData: String): HashMap<String, String> {
        var jsonArray: JSONArray? = null
        val jsonObject: JSONObject

        try {
            jsonObject = JSONObject(jsonData)
            jsonArray = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return getDuration(jsonArray)

    }

    fun parseDirectionsPaint(jsonData: String): Array<String?>? {
        var jsonArray: JSONArray? = null
        val jsonObject: JSONObject

        try {
            jsonObject = JSONObject(jsonData)
            jsonArray = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return getPaths(jsonArray)

    }

    fun getPaths(googleStepsJson: JSONArray?): Array<String?>? {
        var polylines: Array<String?>
        if (googleStepsJson != null) {
            val count = googleStepsJson.length()
            polylines = arrayOfNulls<String>(count)
            for (i in 0 until count) {
                try {
                    polylines[i] = getPath(googleStepsJson.getJSONObject(i))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        } else {
            return null
        }

        return polylines
    }

    fun getPath(googlePathJson: JSONObject): String {

        var polyline = ""
        try {
            polyline = googlePathJson.getJSONObject("polyline").getString("points")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return polyline
    }


    fun parse(jsonData: String): List<HashMap<String, String>> {
        var jsonArray: JSONArray? = null
        val jsonObject: JSONObject

        Log.d("json data", jsonData)

        try {
            jsonObject = JSONObject(jsonData)
            jsonArray = jsonObject.getJSONArray("results")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return getPlaces(jsonArray!!)
    }
}