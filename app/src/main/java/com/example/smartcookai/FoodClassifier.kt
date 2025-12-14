package com.example.smartcookai

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FoodClassifier(context: Context) {

    companion object {
        private const val MODEL_FILE = "1.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INGREDIENTS_FILE = "ingredients.json"

        private const val DEFAULT_IMAGE_SIZE = 224
        private const val IMAGE_CHANNELS = 3
    }

    private var interpreter: Interpreter? = null
    private val labels: List<String>
    private val ingredientsMap: Map<String, List<String>>

    private var imageSize = DEFAULT_IMAGE_SIZE
    private var isQuantized = false
    private var numClassesFromModel = 0

    init {
        interpreter = loadModel(context)
        determineModelParameters()
        labels = loadLabels(context)
        ingredientsMap = loadIngredients(context)
    }

    // ===================== MODEL =====================

    private fun loadModel(context: Context): Interpreter? {
        return try {
            val bytes = context.assets.open(MODEL_FILE).readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
            }
            Interpreter(buffer, Interpreter.Options().setNumThreads(4))
        } catch (e: Exception) {
            null
        }
    }

    private fun determineModelParameters() {
        interpreter?.let {
            val inputTensor = it.getInputTensor(0)
            val inputShape = inputTensor.shape()

            if (inputShape.size == 4) {
                imageSize = when {
                    inputShape[1] == 3 -> inputShape[2]
                    inputShape[3] == 3 -> inputShape[1]
                    else -> DEFAULT_IMAGE_SIZE
                }
            }

            isQuantized = inputTensor.dataType() == DataType.UINT8

            val outputTensor = it.getOutputTensor(0)
            numClassesFromModel = outputTensor.shape().last()
        }
    }

    // ===================== LABELS =====================

    private fun loadLabels(context: Context): List<String> {
        return try {
            context.assets.open(LABELS_FILE).bufferedReader().useLines {
                it.map { line -> line.trim() }
                    .filter { line -> line.isNotEmpty() }
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===================== INGREDIENTS =====================

    private fun loadIngredients(context: Context): Map<String, List<String>> {
        return try {
            val reader = InputStreamReader(context.assets.open(INGREDIENTS_FILE), "UTF-8")
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            Gson().fromJson(reader, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ===================== ANALYZE =====================

    fun analyzeFood(bitmap: Bitmap): FoodResult? {
        if (interpreter == null || labels.isEmpty()) return null

        val inputBuffer = prepareImageInput(bitmap)
        val results = if (isQuantized) {
            analyzeQuantized(inputBuffer)
        } else {
            analyzeFloat(inputBuffer)
        }

        if (results.isEmpty()) return null

        var bestModelIndex = 0
        var bestScore = results[0]

        for (i in 1 until results.size) {
            if (results[i] > bestScore) {
                bestScore = results[i]
                bestModelIndex = i
            }
        }

        // ⚠️ СДВИГ -1 (background)
        val labelIndex = bestModelIndex - 1
        if (labelIndex !in labels.indices) return null

        val foodName = labels[labelIndex]
        val ingredients = findIngredients(foodName)

        return FoodResult(
            foodName = foodName,
            confidence = bestScore,
            ingredients = ingredients
        )
    }

    // ===================== INGREDIENT MATCH =====================

    private fun findIngredients(foodName: String): List<String> {
        ingredientsMap[foodName]?.let { return it }

        ingredientsMap.entries.firstOrNull {
            it.key.equals(foodName, ignoreCase = true)
        }?.let { return it.value }

        ingredientsMap.entries.firstOrNull {
            it.key.lowercase().contains(foodName.lowercase()) ||
                    foodName.lowercase().contains(it.key.lowercase())
        }?.let { return it.value }

        return emptyList()
    }

    // ===================== MODEL RUN =====================

    private fun analyzeQuantized(input: ByteBuffer): FloatArray {
        val output = Array(1) { ByteArray(numClassesFromModel) }
        interpreter?.run(input, output)
        return FloatArray(numClassesFromModel) {
            (output[0][it].toInt() and 0xFF) / 255f
        }
    }

    private fun analyzeFloat(input: ByteBuffer): FloatArray {
        val output = Array(1) { FloatArray(numClassesFromModel) }
        interpreter?.run(input, output)
        return output[0]
    }

    // ===================== IMAGE =====================

    private fun prepareImageInput(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)

        return if (isQuantized) {
            ByteBuffer.allocateDirect(imageSize * imageSize * IMAGE_CHANNELS).apply {
                order(ByteOrder.nativeOrder())
                val pixels = IntArray(imageSize * imageSize)
                resized.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
                for (p in pixels) {
                    put(((p shr 16) and 0xFF).toByte())
                    put(((p shr 8) and 0xFF).toByte())
                    put((p and 0xFF).toByte())
                }
                rewind()
            }
        } else {
            ByteBuffer.allocateDirect(4 * imageSize * imageSize * IMAGE_CHANNELS).apply {
                order(ByteOrder.nativeOrder())
                val pixels = IntArray(imageSize * imageSize)
                resized.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
                for (p in pixels) {
                    putFloat(((p shr 16) and 0xFF) / 255f)
                    putFloat(((p shr 8) and 0xFF) / 255f)
                    putFloat((p and 0xFF) / 255f)
                }
                rewind()
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    data class FoodResult(
        val foodName: String,
        val confidence: Float,
        val ingredients: List<String>
    )
}
