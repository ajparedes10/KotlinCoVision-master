package com.covision.covisionapp.structures

import com.google.gson.annotations.SerializedName

class ObjectDetectionResult {

    @SerializedName("class")
    private lateinit var className: String

    @SerializedName("box")
    private lateinit var box: DoubleArray

    @SerializedName("score")
    private var score: Double = 0.0

    @SerializedName("final")
    private var isFinal: Int = 0

    fun getIsFinal(): Int {
        return isFinal
    }

    fun getBox(): DoubleArray {
        return box
    }

    fun getClassName(): String {
        return className
    }
}
