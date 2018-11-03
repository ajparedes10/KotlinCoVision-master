package com.covision.covisionapp.workers

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64

import com.android.volley.Response
import com.android.volley.VolleyError
import com.covision.covisionapp.fragments.ObjectDetectionFragment
import com.covision.covisionapp.structures.ObjectDetectionRequest
import com.covision.covisionapp.structures.ObjectDetectionResult

import java.io.ByteArrayOutputStream

class ObjectDetectionWorker(private val context: Context, private val image: Bitmap, private val callback: ObjectDetectionFragment.ObjectDetectionCallback) : Thread("ObjectDetection") {

    override fun run() {
        val encodedImage = encodeToBase64(image)

        val request = ObjectDetectionRequest(SERVER_URL, "navigation", encodedImage, Response.Listener { response ->
            this@ObjectDetectionWorker.callback.onDetectionResult(response) }, Response.ErrorListener { error ->
            this@ObjectDetectionWorker.callback.onError(error.message!!) })

        RestRequestQueue.getInstance(this.context)!!.addToRequestQueue(request)
    }

    companion object {
        private val SERVER_URL = "http://35.227.86.190:8080/detect"


        fun encodeToBase64(image: Bitmap): String {
            val baos = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val b = baos.toByteArray()
            return Base64.encodeToString(b, Base64.DEFAULT)
        }
    }
}
