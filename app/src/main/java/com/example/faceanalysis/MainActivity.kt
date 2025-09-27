package com.example.faceanalysis

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import ai.onnxruntime.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.ArrayDeque
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvResult: TextView
    private lateinit var overlayView: OverlayView

    private var faceLandmarker: FaceLandmarker? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // ONNX Runtime
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // Buffer (45 frames × 72 features)
    private val featureBuffer = ArrayDeque<FloatArray>()
    private val SEQ_LEN = 45
    private val FEATURE_SIZE = 72   // 36 landmarks × 2 coords

    // rotação do frame mais recente
    private var lastRotationDegrees: Int = 0

    companion object {
        private const val TAG = "FaceAnalysis"
        private const val MODEL_PATH = "face_landmarker.task"
        private const val ONNX_MODEL_NAME = "model_lstm_3_45_euclidean.onnx"
    }

    // Índices dos 36 landmarks que escolhemos
    private val SELECTED_LANDMARKS = listOf(
        61, 40, 37, 0, 267, 270, 291,      // boca externa
        78, 95, 14, 317, 308,              // boca interna
        33, 160, 158, 133, 153, 144, 145,  // olho esquerdo
        362, 385, 387, 263, 373, 380, 381, // olho direito
        70, 105, 107,                      // sobrancelha esq
        336, 334, 300,                     // sobrancelha dir
        1, 2, 168,                         // nariz
        152                                // queixo
    )

    private val leftEyeIdx = 33
    private val rightEyeIdx = 263
    private val noseIdx = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvResult = findViewById(R.id.tvResult)

        initOnnxModel()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setupFaceLandmarker()
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setupFaceLandmarker()
                startCamera()
            } else {
                tvResult.text = "Permissão de câmera negada"
            }
        }

    /** Copia modelo ONNX de assets p/ cache */
    private fun initOnnxModel() {
        try {
            val assetManager = assets
            val file = File(cacheDir, ONNX_MODEL_NAME)

            if (!file.exists()) {
                val inputStream: InputStream = assetManager.open(ONNX_MODEL_NAME)
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            }

            ortEnv = OrtEnvironment.getEnvironment()
            ortSession = ortEnv?.createSession(file.absolutePath, OrtSession.SessionOptions())

            Log.i(TAG, "ONNX model carregado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro carregando modelo ONNX", e)
        }
    }

    /** Configuração do FaceLandmarker */
    private fun setupFaceLandmarker() {
        val baseOptions = com.google.mediapipe.tasks.core.BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setResultListener { result, _ ->
                try {
                    val faces = result.faceLandmarks()
                    if (faces.isEmpty()) {
                        runOnUiThread {
                            overlayView.setPoints(emptyList())
                            tvResult.text = "Nenhum rosto detectado"
                        }
                    } else {
                        val face = faces[0]

                        val cx = face[noseIdx].x()
                        val cy = face[noseIdx].y()
                        val scale = euclidean(face[leftEyeIdx], face[rightEyeIdx]).coerceAtLeast(1e-5f)

                        // extrair 72 features (36 × 2)
                        val features = FloatArray(FEATURE_SIZE)
                        var k = 0
                        for (idx in SELECTED_LANDMARKS) {
                            val lm = face[idx]
                            val nx = (lm.x() - cx) / scale
                            val ny = (lm.y() - cy) / scale
                            features[k++] = nx
                            features[k++] = ny
                        }

                        if (featureBuffer.size >= SEQ_LEN) featureBuffer.removeFirst()
                        featureBuffer.addLast(features)

                        // EAR e MAR
                        val ear = calculateEAR(face)
                        val mar = calculateMAR(face)

                        Log.d(TAG, "👁️ EAR: %.4f".format(ear))
                        Log.d(TAG, "👄 MAR: %.4f".format(mar))

                        if (featureBuffer.size == SEQ_LEN) {
                            val prediction = runOnnxInference(featureBuffer.toList(), ear, mar)
                            runOnUiThread {
                                tvResult.text = prediction
                            }
                        } else {
                            runOnUiThread {
                                tvResult.text = "Coletando dados... (${featureBuffer.size}/$SEQ_LEN)"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro processando landmarks: ${e.message}", e)
                    runOnUiThread {
                        tvResult.text = "Erro processando: ${e.message}"
                        overlayView.setPoints(emptyList())
                    }
                }
            }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
    }

    private fun calculateEAR(face: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val eye = listOf(33, 160, 158, 133, 153, 144)
        val A = euclidean(face[eye[1]], face[eye[5]])
        val B = euclidean(face[eye[2]], face[eye[4]])
        val C = euclidean(face[eye[0]], face[eye[3]])
        return (A + B) / (2.0f * C.coerceAtLeast(1e-6f))
    }

    private fun calculateMAR(face: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val top = face[13]
        val bottom = face[14]
        val left = face[61]
        val right = face[291]
        val vertical = euclidean(top, bottom)
        val horizontal = euclidean(left, right)
        return vertical / horizontal.coerceAtLeast(1e-6f)
    }

    /** Distância euclidiana */
    private fun euclidean(
        a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): Float {
        return sqrt((a.x() - b.x()).pow(2) + (a.y() - b.y()).pow(2))
    }

    /** Rodar ONNX + hibrid EAR/MAR */
    private fun runOnnxInference(sequence: List<FloatArray>, ear: Float, mar: Float): String {
        if (ortSession == null) return "Modelo não carregado"
        val inputName = ortSession!!.inputNames.iterator().next()

        val inputData = Array(1) { Array(SEQ_LEN) { FloatArray(FEATURE_SIZE) } }
        for (i in 0 until SEQ_LEN) {
            inputData[0][i] = sequence[i]
        }

        val inputTensor = OnnxTensor.createTensor(ortEnv, inputData)
        val startTime = System.currentTimeMillis()
        val results = ortSession!!.run(mapOf(inputName to inputTensor))
        val elapsed = System.currentTimeMillis() - startTime

        val output = results[0].value as Array<FloatArray>
        val probs = output[0]

        Log.d(TAG, "⚡ Tempo de inferência: ${elapsed}ms")
        Log.d(TAG, "📊 Probabilidades: alerta=%.5f, bocejo=%.5f, microsleep=%.5f"
            .format(probs[0], probs[1], probs[2]))
        Log.d(TAG, "👁️ EAR usado: %.4f".format(ear))
        Log.d(TAG, "👄 MAR usado: %.4f".format(mar))

        // lógica híbrida: se EAR < 0.2 por tempo -> microsleep, se MAR > 0.35 -> bocejo
        val classes = listOf("Alerta", "Bocejo", "Microsleep")
        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
        var finalIdx = maxIdx

        if (ear < 0.18f && probs[2] > 0.15f) {
            finalIdx = 2
        } else if (mar > 0.35f && probs[1] > 0.20f) {
            finalIdx = 1
        }

        return "${classes[finalIdx]} (conf: %.2f)".format(probs[finalIdx])
    }

    /** Inicia câmera em tempo real */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val bitmap = imageProxy.toBitmap()
                    if (bitmap != null && faceLandmarker != null) {
                        lastRotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                        val imgProcOptions = ImageProcessingOptions.builder()
                            .setRotationDegrees(lastRotationDegrees)
                            .build()
                        faceLandmarker?.detectAsync(mpImage, imgProcOptions, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro processando frame: ${e.message}", e)
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }
}

/** Utilitários: conversão correta ImageProxy → Bitmap */
fun ImageProxy.toBitmap(): Bitmap? {
    try {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        Log.e("ImageProxy", "Erro convertendo para bitmap: ${e.message}")
        return null
    }
}
