package com.example.lsegestures

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    private val context: Context,
    private val maxNumHands: Int = DEFAULT_NUM_HANDS,
    private val minHandDetectionConfidence: Float = DEFAULT_HAND_DETECTION_CONFIDENCE,
    private val minHandTrackingConfidence: Float = DEFAULT_HAND_TRACKING_CONFIDENCE,
    private val minHandPresenceConfidence: Float = DEFAULT_HAND_PRESENCE_CONFIDENCE,
    private val listener: LandmarkerListener? = null
) {

    companion object {
        const val TAG = "HandLandmarkerHelper"

        // Nombre del modelo en assets
        private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"

        const val DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_HANDS = 2

        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    private var handLandmarker: HandLandmarker? = null

    init {
        setupHandLandmarker()
    }

    fun clear() {
        try {
            handLandmarker?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar HandLandmarker: ${e.message}", e)
        }
        handLandmarker = null
    }

    private fun setupHandLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
            .setModelAssetPath(MP_HAND_LANDMARKER_TASK)
            .setDelegate(Delegate.CPU) // 👈 SOLO CPU

        // Vamos a usar modo LIVE_STREAM (cámara en tiempo real)
        val runningMode = RunningMode.LIVE_STREAM

        if (runningMode == RunningMode.LIVE_STREAM && listener == null) {
            throw IllegalStateException(
                "listener no puede ser null en RunningMode.LIVE_STREAM"
            )
        }

        try {
            val baseOptions = baseOptionBuilder.build()

            val optionsBuilder =
                HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinHandDetectionConfidence(minHandDetectionConfidence)
                    .setMinTrackingConfidence(minHandTrackingConfidence)
                    .setMinHandPresenceConfidence(minHandPresenceConfidence)
                    .setNumHands(maxNumHands)
                    .setRunningMode(runningMode)

            optionsBuilder
                .setResultListener(this::returnLiveStreamResult)
                .setErrorListener(this::returnLiveStreamError)

            val options = optionsBuilder.build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.d(TAG, "HandLandmarker inicializado correctamente.")

        } catch (e: IllegalStateException) {
            listener?.onError(
                "Hand Landmarker no se pudo inicializar: ${e.message}",
                OTHER_ERROR
            )
            Log.e(TAG, "Error al cargar MediaPipe task: ${e.message}", e)
        } catch (e: RuntimeException) {
            listener?.onError(
                "Hand Landmarker falló (posible error GPU/modelo): ${e.message}",
                GPU_ERROR
            )
            Log.e(TAG, "Error en HandLandmarker: ${e.message}", e)
        }
    }

    /**
     * Recibe un ImageProxy de CameraX y lo procesa en modo LIVE_STREAM.
     * Cierra el ImageProxy internamente (NO lo cierres fuera si llamas a esta función).
     */
    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        val landmarker = handLandmarker
        if (landmarker == null) {
            Log.w(TAG, "HandLandmarker es null, descartando frame.")
            imageProxy.close()
            return
        }

        val frameTime = SystemClock.uptimeMillis()

        // Crear un bitmap del frame
        val bitmapBuffer =
            Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

        imageProxy.use { proxy ->
            bitmapBuffer.copyPixelsFromBuffer(proxy.planes[0].buffer)
        }
        // imageProxy.use también lo cierra

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            matrix,
            true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        try {
            landmarker.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error en detectAsync: ${e.message}", e)
        }
    }

    /** Callback de resultados en modo LIVE_STREAM. */
    private fun returnLiveStreamResult(
        result: HandLandmarkerResult,
        inputImage: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        val bundle = ResultBundle(
            results = listOf(result),
            inferenceTime = inferenceTime,
            inputImageHeight = inputImage.height,
            inputImageWidth = inputImage.width
        )
        listener?.onResults(bundle)
    }

    /** Callback de errores en modo LIVE_STREAM. */
    private fun returnLiveStreamError(error: RuntimeException) {
        Log.e(TAG, "Error en livestream: ${error.message}", error)
        listener?.onError(
            error.message ?: "Error desconocido en HandLandmarker",
            OTHER_ERROR
        )
    }

    data class ResultBundle(
        val results: List<HandLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
}
