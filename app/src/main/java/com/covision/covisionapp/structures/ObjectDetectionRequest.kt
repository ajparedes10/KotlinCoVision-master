package com.covision.covisionapp.structures

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.HashMap

class ObjectDetectionRequest(url: String, mode: String, image: String, private val listener: Response.Listener<List<ObjectDetectionResult>>, errorListener: Response.ErrorListener) : Request<List<ObjectDetectionResult>>(Request.Method.POST, url, errorListener) {

    private val gson: Gson
    private val params: MutableMap<String, String>

    init {
        gson = Gson()
        params = HashMap()
        params["mode"] = mode
        params["image"] = image
    }

    public override fun getParams(): Map<String, String> {
        return params
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<List<ObjectDetectionResult>> {
        try {
            val json = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers)))
            val listType = object : TypeToken<ArrayList<ObjectDetectionResult>>() {

            }.type
            val result = gson.fromJson<List<ObjectDetectionResult>>(json, listType)
            return Response.success(result, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            return Response.error(VolleyError(e.message))
        } catch (e: JsonSyntaxException) {
            return Response.error(VolleyError(e.message))
        }

    }

    override fun deliverResponse(response: List<ObjectDetectionResult>) {
        listener.onResponse(response)
    }
}
