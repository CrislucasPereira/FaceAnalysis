package com.example.faceanalysis

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvResult: TextView

    private var faceLandmarker: com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    companion object {
        private const val TAG = "FaceAnalysis"
        private const val MODEL_PATH = "face_landmarker.task" // nome exato no assets
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // só inicializa depois de confirmar que o modelo existe
                if (checkAssetExists(MODEL_PATH)) {
                    setupFaceLandmarker()
                    startCamera()
                } else {
                    tvResult.text = "Modelo não encontrado em assets: $MODEL_PATH"
                }
            } else {
                tvResult.text = "Permissão de câmera negada"
            }
        }
    private lateinit var overlayView: com.example.faceanalysis.OverlayView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvResult = findViewById(R.id.tvResult)

        // 1) CHECK: confirma que o asset está disponível no APK (logs)
        val assetOk = checkAssetExists(MODEL_PATH)
        Log.i("ASSET_CHECK", "Asset '$MODEL_PATH' presente: $assetOk")
        if (!assetOk) {
            tvResult.text = "Modelo não encontrado em assets: $MODEL_PATH"
            // ainda assim pedimos permissão (para facilitar debug), mas não inicializamos o mediapipe
        }

        // 2) Permissão de câmera e inicialização só se asset ok
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (assetOk) {
                setupFaceLandmarker()
                startCamera()
            } else {
                Log.w(TAG, "Modelo ausente: não inicializando FaceLandmarker.")
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Verifica programaticamente se o arquivo existe em assets e retorna seu tamanho (log).
     */
    private fun checkAssetExists(name: String): Boolean {
        return try {
            assets.open(name).use { stream ->
                val size = stream.available()
                Log.i("ASSET_CHECK", "Asset '$name' aberto com tamanho (bytes): $size")
            }
            true
        } catch (e: Exception) {
            Log.e("ASSET_CHECK", "Erro ao abrir asset '$name': ${e.message}", e)
            false
        }
    }

    /**
     * Cria BaseOptions de forma robusta (reflection fallback) e inicializa FaceLandmarker.
     */
    private fun createBaseOptionsSafely(modelPath: String): com.google.mediapipe.tasks.core.BaseOptions {
        // 1) Tenta API pública: BaseOptions.builder()
        try {
            val cls = com.google.mediapipe.tasks.core.BaseOptions::class.java
            val builderMethod = cls.getMethod("builder")
            val builderObj = builderMethod.invoke(null)
            val setModelMethod = builderObj.javaClass.getMethod("setModelAssetPath", String::class.java)
            setModelMethod.invoke(builderObj, modelPath)
            val buildMethod = builderObj.javaClass.getMethod("build")
            val baseOptions = buildMethod.invoke(builderObj) as com.google.mediapipe.tasks.core.BaseOptions
            return baseOptions
        } catch (e: Exception) {
            Log.w(TAG, "BaseOptions.builder() não disponível: ${e.message}. Tentando fallback por reflection...")
        }

        // 2) Fallback: tentar BaseOptions$Builder
        try {
            val builderClass = Class.forName("com.google.mediapipe.tasks.core.BaseOptions\$Builder")
            val ctor = builderClass.getDeclaredConstructor()
            ctor.isAccessible = true
            val builderInstance = ctor.newInstance()
            val setModel = builderClass.getMethod("setModelAssetPath", String::class.java)
            setModel.invoke(builderInstance, modelPath)
            val build = builderClass.getMethod("build")
            val baseOptions = build.invoke(builderInstance) as com.google.mediapipe.tasks.core.BaseOptions
            return baseOptions
        } catch (e: Exception) {
            Log.w(TAG, "Fallback por BaseOptions\$Builder falhou: ${e.message}")
        }

        throw RuntimeException(
            "Não foi possível criar BaseOptions. Verifique a versão da dependência com.google.mediapipe.tasks.core.BaseOptions."
        )
    }

    /**
     * Inicializa FaceLandmarker (tenta API direta e usa reflection como fallback).
     */
    private fun setupFaceLandmarker() {
        try {
            val baseOptions = createBaseOptionsSafely(MODEL_PATH)

            // Tenta inicialização direta (API comum)
            try {
                val directBuilder = com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions.builder()
                directBuilder.setBaseOptions(baseOptions)
                directBuilder.setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM)
                directBuilder.setNumFaces(1)
                directBuilder.setMinFaceDetectionConfidence(0.5f)

                directBuilder.setResultListener { result: com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult, _mpImage: com.google.mediapipe.framework.image.MPImage ->
                    try {
                        val faces = result.faceLandmarks()
                        if (faces.isEmpty()) {
                            runOnUiThread {
                                tvResult.text = "Nenhum rosto detectado"
                                overlayView.setPoints(emptyList(), mirror = true)
                            }
                        } else {
                            val firstFace = faces[0]
                            val normalizedPoints = mutableListOf<Pair<Float, Float>>()

                            for (lm in firstFace) {
                                // tenta acessar x/y via métodos comuns ou reflection
                                val x = try {
                                    // preferencialmente método getX()
                                    val m = lm.javaClass.getMethod("getX")
                                    (m.invoke(lm) as Number).toFloat()
                                } catch (ex1: Exception) {
                                    try {
                                        val m2 = lm.javaClass.getMethod("x")
                                        (m2.invoke(lm) as Number).toFloat()
                                    } catch (ex2: Exception) {
                                        // fallback: procura campos públicos
                                        try {
                                            val fx = lm.javaClass.getField("x").get(lm) as Number
                                            fx.toFloat()
                                        } catch (ex3: Exception) {
                                            0f
                                        }
                                    }
                                }

                                val y = try {
                                    val m = lm.javaClass.getMethod("getY")
                                    (m.invoke(lm) as Number).toFloat()
                                } catch (ex1: Exception) {
                                    try {
                                        val m2 = lm.javaClass.getMethod("y")
                                        (m2.invoke(lm) as Number).toFloat()
                                    } catch (ex2: Exception) {
                                        try {
                                            val fy = lm.javaClass.getField("y").get(lm) as Number
                                            fy.toFloat()
                                        } catch (ex3: Exception) {
                                            0f
                                        }
                                    }
                                }

                                // agora x,y podem estar normalizados (0..1) ou em pixels.
                                normalizedPoints.add(Pair(x, y))
                            }

                            // Detecta se valores parecem em pixels (maiores que 1) — então normaliza usando previewView dims
                            val viewW = previewView.width.takeIf { it > 0 } ?: 1
                            val viewH = previewView.height.takeIf { it > 0 } ?: 1

                            val normalizedOrScaled = normalizedPoints.map { (x, y) ->
                                val nx = if (x > 1.01f) (x / viewW.toFloat()) else x
                                val ny = if (y > 1.01f) (y / viewH.toFloat()) else y
                                Pair(nx.coerceIn(0f, 1f), ny.coerceIn(0f, 1f))
                            }

                            runOnUiThread {
                                tvResult.text = "Rosto detectado — pontos: ${normalizedOrScaled.size}"
                                // mirror = true para câmera frontal (espelha X)
                                overlayView.setPoints(normalizedOrScaled, mirror = true)
                            }
                        }
                    } catch (e: Exception) {
                        // se algo falhar, limpa overlay
                        runOnUiThread {
                            overlayView.setPoints(emptyList())
                            tvResult.text = "Erro processando landmarks: ${e.message}"
                        }
                    }
                }

                directBuilder.setErrorListener { ex: RuntimeException ->
                    Log.e(TAG, "MediaPipe error", ex)
                    runOnUiThread { tvResult.text = "Erro MediaPipe: ${ex.message}" }
                }

                val options = directBuilder.build()
                faceLandmarker = com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.createFromOptions(this, options)
                Log.i(TAG, "FaceLandmarker inicializado (via API direta).")
                return
            } catch (e: Throwable) {
                Log.w(TAG, "Inicialização direta FaceLandmarkerOptions falhou: ${e.message}. Tentando fallback por reflection.")
            }

            // Fallback: criar via reflection (menos funcional em relação a listeners, mas tenta)
            val faceOptionsBuilderClass =
                com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions::class.java
                    .getMethod("builder")
                    .invoke(null)

            val setBaseOptions = faceOptionsBuilderClass.javaClass.getMethod("setBaseOptions", com.google.mediapipe.tasks.core.BaseOptions::class.java)
            setBaseOptions.invoke(faceOptionsBuilderClass, baseOptions)

            val setRunningMode = faceOptionsBuilderClass.javaClass.getMethod("setRunningMode", com.google.mediapipe.tasks.vision.core.RunningMode::class.java)
            setRunningMode.invoke(faceOptionsBuilderClass, com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM)

            // build via reflection
            val buildMethod = faceOptionsBuilderClass.javaClass.getMethod("build")
            val faceOptions = buildMethod.invoke(faceOptionsBuilderClass) as com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions

            faceLandmarker = com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.createFromOptions(this, faceOptions)
            Log.i(TAG, "FaceLandmarker inicializado (via reflection).")

        } catch (e: Exception) {
            Log.e(TAG, "Erro inicializando FaceLandmarker", e)
            tvResult.text = "Erro inicializando FaceLandmarker: ${e.message}"
        }
    }

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
                    // Capture o bitmap na thread principal
                    previewView.post {
                        val bitmap: Bitmap? = previewView.bitmap
                        if (bitmap != null && faceLandmarker != null) {
                            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                            val imgProcOptions = com.google.mediapipe.tasks.vision.core.ImageProcessingOptions.builder().build()
                            val timestampMs = System.currentTimeMillis()
                            try {
                                faceLandmarker?.detectAsync(mpImage, imgProcOptions, timestampMs)
                            } catch (e: NoSuchMethodError) {
                                faceLandmarker?.detectAsync(mpImage, timestampMs)
                            }
                        }
                        imageProxy.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao analisar frame: ${e.message}", e)
                    imageProxy.close()
                }
            }

            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        faceLandmarker?.close()
        cameraExecutor.shutdown()
    }
}
