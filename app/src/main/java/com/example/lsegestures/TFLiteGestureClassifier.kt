package com.example.lsegestures

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteGestureClassifier(context: Context) {

    companion object {
        private const val TAG = "TFLiteGestureClassifier"
        private const val MODEL_PATH = "lse_model.tflite"

        // Tu modelo: input (1, 60, 126), output (1, 4)
        private const val SEQ_LENGTH = 60
        private const val FEATURE_SIZE = 126
        private const val NUM_CLASSES = 4
    }

    private var interpreter: Interpreter? = null

    init {
        try {
            val modelBuffer = loadModelFile(context)
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "Intérprete TFLite inicializado correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando TFLite: ${e.message}", e)
            interpreter = null
        }
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val assetManager = context.assets
        val inputStream = assetManager.open(MODEL_PATH)
        val bytes = inputStream.readBytes()
        inputStream.close()

        // Creamos un ByteBuffer directo para el modelo
        return ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            rewind()
        }
    }

    /**
     * Ejecuta una inferencia de prueba con datos dummy.
     * Útil solo para comprobar que el modelo corre en Android.
     */
    fun runDummyInference(): FloatArray? {
        val interp = interpreter ?: return null

        // Input: shape (1, 60, 126)
        val input = Array(1) {
            Array(SEQ_LENGTH) {
                FloatArray(FEATURE_SIZE) { 0f }  // todo ceros
            }
        }

        // Output: shape (1, 4)
        val output = Array(1) {
            FloatArray(NUM_CLASSES)
        }

        return try {
            interp.run(input, output)
            val probs = output[0]
            Log.d(TAG, "Dummy inference OK. Output = ${probs.joinToString(", ")}")
            probs
        } catch (e: Exception) {
            Log.e(TAG, "Error en dummy inference: ${e.message}", e)
            null
        }
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando intérprete TFLite: ${e.message}", e)
        }
    }

    fun predict(sequence: Array<FloatArray>): FloatArray? {
        val interp = interpreter ?: return null

        // Input: (1,60,126)
        val input = Array(1) { sequence }

        val output = Array(1) { FloatArray(NUM_CLASSES) }

        return try {
            interp.run(input, output)
            output[0]
        } catch (e: Exception) {
            Log.e(TAG, "Error en predict(): ${e.message}", e)
            null
        }
    }
}
