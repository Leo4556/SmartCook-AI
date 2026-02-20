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


class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding

    private var latestDetectedText: String = ""
    private val eNumberRegex = Regex("""\b[Ee][\s-]?\d{3}\b""")

    private var imageWidth = 0
    private var imageHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val found = matches.map { it.value }.toList()

            if (found.isNotEmpty()) {

                val resultText = found.joinToString("\n")

                val sheet = ResultBottomSheetFragment(resultText) {}
                sheet.show(supportFragmentManager, "result")

            } else {
                val sheet = ResultBottomSheetFragment("E-добавки не найдены") {}
                sheet.show(supportFragmentManager, "result")
            }
        }

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

            // ImageAnalysis с повышенным разрешением
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720)) // лучше для OCR
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

            // 🔥 Автофокус по центру экрана
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

                        if (box != null && frameRect.contains(box)) {
                            filteredText.append(block.text).append("\n")
                        }
                    }

                    latestDetectedText = filteredText.toString()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
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
