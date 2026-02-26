package com.example.smartcookai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartcookai.databinding.ActivityScanBinding
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import java.util.concurrent.Executors
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.util.Size
import androidx.camera.core.*
import com.example.smartcookai.utils.EAdditiveRepository


class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding

    private var latestDetectedText: String = ""
    private val eNumberRegex = Regex("""\b[Ee][\s-]?\d{3,4}\b""")

    private val textBuffer = ArrayDeque<String>()
    private val BUFFER_SIZE = 5

    private var imageWidth = 0
    private var imageHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EAdditiveRepository.load(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }

        binding.btnAnalyze.setOnClickListener {

            val matches = eNumberRegex.findAll(latestDetectedText)

            val found = matches
                .map { it.value.uppercase()
                    .replace(" ", "")
                    .replace("-", "")
                    .replace("I", "1")
                    .replace("L", "1")
                    .replace("O", "0")
                    .replace("S", "5")
                    .replace("-", "")}
                .toSet()

            if (found.isEmpty()) {
                ResultBottomSheetFragment("E-добавки не найдены") {}
                    .show(supportFragmentManager, "result")
                return@setOnClickListener
            }

            val resultBuilder = android.text.SpannableStringBuilder()
            var totalRisk = 0
            var counted = 0

            for (code in found) {

                val additive = EAdditiveRepository.getAdditive(code)

                if (additive != null) {

                    // Заголовок
                    appendColored(
                        resultBuilder,
                        "${additive.code} — ${additive.name}\n",
                        getThemeTextColor()
                    )

                    // Цвет по уровню риска
                    val riskColor = when (additive.safetyLevel) {
                        1 -> android.graphics.Color.parseColor("#4CAF50") // зелёный
                        2 -> android.graphics.Color.parseColor("#FFC107") // жёлтый
                        3 -> android.graphics.Color.parseColor("#F44336") // красный
                        else -> android.graphics.Color.GRAY
                    }

                    appendColored(
                        resultBuilder,
                        "${additive.safetyLabel}\n",
                        riskColor,
                        bold = true
                    )

                    appendColored(
                        resultBuilder,
                        "${additive.description}\n\n",
                        getThemeSecondaryTextColor()
                    )

                    totalRisk += additive.safetyLevel
                    counted++

                } else {

                    appendColored(
                        resultBuilder,
                        "$code — Нет данных\n\n",
                        android.graphics.Color.GRAY,
                        bold = true
                    )
                }
            }

            if (counted > 0) {
                val averageRisk = totalRisk / counted
                resultBuilder.append("Общая оценка риска: $averageRisk / 3")
            }

            ResultBottomSheetFragment(resultBuilder) {}
                .show(supportFragmentManager, "result")
        }

    }

    private fun appendColored(
        builder: android.text.SpannableStringBuilder,
        text: String,
        color: Int,
        bold: Boolean = false
    ) {
        val start = builder.length
        builder.append(text)
        val end = builder.length

        builder.setSpan(
            android.text.style.ForegroundColorSpan(color),
            start,
            end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (bold) {
            builder.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun getThemeTextColor(): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        return ContextCompat.getColor(this, typedValue.resourceId)
    }

    private fun getThemeSecondaryTextColor(): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        return ContextCompat.getColor(this, typedValue.resourceId)
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1920, 1080)) // лучше для OCR
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            imageAnalyzer.setAnalyzer(
                ContextCompat.getMainExecutor(this)
            ) { imageProxy ->
                processImageProxy(recognizer, imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // Автофокус по центру экрана
            binding.previewView.post {

                val factory = binding.previewView.meteringPointFactory
                val centerPoint = factory.createPoint(
                    binding.previewView.width / 2f,
                    binding.previewView.height / 2f
                )

                val action = FocusMeteringAction.Builder(centerPoint)
                    .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                camera.cameraControl.startFocusAndMetering(action)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        imageProxy: ImageProxy
    ) {

        val mediaImage = imageProxy.image
        if (mediaImage != null) {

            imageWidth = imageProxy.width
            imageHeight = imageProxy.height

            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(image)
                .addOnSuccessListener { visionText ->

                    val frameRect = getFrameRectInImage()

                    val filteredText = StringBuilder()

                    for (block in visionText.textBlocks) {

                        val box = block.boundingBox

                        if (box != null && android.graphics.Rect.intersects(frameRect, box)) {
                            filteredText.append(block.text).append("\n")
                        }
                    }

                    val currentText = filteredText.toString()

                    if (currentText.isNotBlank()) {

                        if (textBuffer.size >= BUFFER_SIZE) {
                            textBuffer.removeFirst()
                        }

                        textBuffer.addLast(currentText)

                        latestDetectedText = getStableText()
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun getStableText(): String {

        if (textBuffer.isEmpty()) return ""

        return textBuffer
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: ""
    }

    private fun getFrameRectInImage(): android.graphics.Rect {

        val previewWidth = binding.previewView.width.toFloat()
        val previewHeight = binding.previewView.height.toFloat()

        val scaleX = imageWidth / previewWidth
        val scaleY = imageHeight / previewHeight

        val frameLeft = binding.scanFrame.x
        val frameTop = binding.scanFrame.y

        val frameRight = frameLeft + binding.scanFrame.width
        val frameBottom = frameTop + binding.scanFrame.height

        return android.graphics.Rect(
            (frameLeft * scaleX).toInt(),
            (frameTop * scaleY).toInt(),
            (frameRight * scaleX).toInt(),
            (frameBottom * scaleY).toInt()
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }


}
