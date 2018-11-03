package com.covision.covisionapp.fragments

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.covision.covisionapp.R
import com.covision.covisionapp.structures.ObjectDetectionResult
import com.covision.covisionapp.workers.ObjectDetectionWorker

import java.util.Collections

class ObjectDetectionFragment : Fragment() {
    private var cameraManager: CameraManager? = null
    private var device: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraFacing: Int = 0
    private var cameraId: String? = null
    private var imageView: ImageView? = null
    private var textureView: TextureView? = null
    private var surfaceTextureListener: TextureView.SurfaceTextureListener? = null
    private var previewSize: Size? = null

    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var stateCallback: CameraDevice.StateCallback? = null
    private var detectCallback: DetectionMessageCallback? = null

    private var cameraOpened = false

    private var workingImage: Bitmap? = null

    interface ObjectDetectionCallback {
        fun onDetectionResult(result: List<ObjectDetectionResult>)
        fun onError(message: String)
    }

    interface DetectionMessageCallback {
        fun onDetectionResult(result: String)
        fun onError(message: String)
    }

    fun detect(callback: DetectionMessageCallback) {
        detectCallback = callback
        if (textureView!!.isAvailable && cameraId !=
                null) {
            if (!cameraOpened) openCamera()

            val image = textureView!!.bitmap
            workingImage = image.copy(image.config, true)
            val canvas = Canvas(workingImage!!)
            imageView!!.setImageBitmap(workingImage)
            ObjectDetectionWorker(context!!, image, object : ObjectDetectionCallback {
                override fun onDetectionResult(result: List<ObjectDetectionResult>) {
                    val paint = Paint()
                    paint.setARGB(1, 100, 100, 100)
                    var text = ""
                    for (box in result) {
                        if (box.getIsFinal() === 1) {
                            text = box.getClassName()
                            if (text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size == 1) text = "No encontre ningun objeto"
                        } else {
                            val rect = box.getBox()
                            canvas.drawRect(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat(), paint)
                        }
                    }
                    detectCallback!!.onDetectionResult(text)
                }

                override fun onError(message: String) {
                    detectCallback!!.onError("Ocurrio un problema al conectarse con el servidor, vuelve a intentarlo")
                }
            }).start()
        } else {
            detectCallback!!.onError("Ocurrio un problema al abrir la camara")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // infla el layout del fragmento
        val myView = inflater.inflate(R.layout.fragment_object_detection, container, false)

        cameraManager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK
        textureView = myView.findViewById(R.id.texture_view)
        imageView = myView.findViewById(R.id.image_view)

        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                setUpCamera()
            }

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {

            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {

            }
        }

        stateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice) {
                this@ObjectDetectionFragment.device = cameraDevice
                createPreviewSession()
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                cameraDevice.close()
                this@ObjectDetectionFragment.device = null
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                cameraDevice.close()
                this@ObjectDetectionFragment.device = null
            }
        }

        return myView
    }

    override fun onResume() {
        openBackgroundThread()
        if (textureView!!.isAvailable) {
            setUpCamera()
        } else {
            textureView!!.surfaceTextureListener = surfaceTextureListener
        }
        super.onResume()
    }

    private fun setUpCamera() {
        try {
            for (cameraId in cameraManager!!.cameraIdList) {
                val cameraCharacteristics = cameraManager!!.getCameraCharacteristics(cameraId)
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                    val streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    previewSize = streamConfigurationMap!!.getOutputSizes(SurfaceTexture::class.java)[0]
                    this.cameraId = cameraId
                    if (!cameraOpened) openCamera()
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(context!!, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager!!.openCamera(cameraId!!, stateCallback!!, backgroundHandler)
                cameraOpened = true
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun openBackgroundThread() {
        backgroundThread = HandlerThread("camera_background_thread")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun createPreviewSession() {
        try {
            val surfaceTexture = textureView!!.surfaceTexture
            surfaceTexture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            val previewSurface = Surface(surfaceTexture)
            captureRequestBuilder = device!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(previewSurface)

            device!!.createCaptureSession(listOf(previewSurface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            if (device == null) {
                                return
                            }

                            try {
                                val captureRequest = captureRequestBuilder!!.build()
                                this@ObjectDetectionFragment.cameraCaptureSession = cameraCaptureSession
                                this@ObjectDetectionFragment.cameraCaptureSession!!.setRepeatingRequest(captureRequest, null, backgroundHandler)
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }

                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {

                        }
                    }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    override fun onStop() {
        super.onStop()
        closeCamera()
        closeBackgroundThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCamera()
        closeBackgroundThread()
    }

    private fun closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession!!.close()
            cameraCaptureSession = null
        }

        if (device != null) {
            device!!.close()
            device = null
        }
    }

    private fun closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread!!.quitSafely()
            backgroundThread = null
            backgroundHandler = null
        }
    }
}// Required empty public constructor
