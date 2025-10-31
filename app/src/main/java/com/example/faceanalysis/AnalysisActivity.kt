package com.example.faceanalysis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import ai.onnxruntime.*
import android.view.animation.AlphaAnimation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayDeque
import kotlin.math.*


class AnalysisActivity : AppCompatActivity() {

    private lateinit var alertManager: AlertManager

    private var showLandmarks: Boolean = true
    private var sensitivityLevel: String = "Média"
    private lateinit var previewView: PreviewView
    private lateinit var tvResult: TextView
    private lateinit var tvAdditionalInfo: TextView
    private lateinit var tvCounters: TextView
    private lateinit var overlayView: OverlayView
    private lateinit var analysisToolbar: Toolbar

    private var faceLandmarker: FaceLandmarker? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    private val featureBuffer = ArrayDeque<FloatArray>()
    private val SEQ_LEN = 45
    private val FEATURE_SIZE = 72
    private val earHistory = ArrayDeque<Float>()
    private val HISTORY_SIZE = 5

    private val detectedEventsHistory = ArrayDeque<HistoryEntry>()
    private val MAX_HISTORY_ENTRIES = 50
    private var lastEventSentToHistory: String = ""

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var currentEventLabel: String? = null
    private var eventStartTime: Long = 0L

    private var lastEar: Float = 0f
    private var lastMar: Float = 0f
    private var lastYaw: Float = 0f
    private var lastConf: Float = 0f

    // ⚙️ Sensibilidade
    private var detectionSensitivity: String = "Media"

    // 💤 / 👁️ controle de estados e cronômetros
    private var inMicrosleep = false
    private var inDesattention = false
    private var eyesClosedStartTime: Long = 0L
    private var yawStartTime: Long = 0L

    // 😮 Bocejo (contagem)
    private var yawnCount = 0
    private var mouthOpen = false
    private var yawnStartTime = 0L

    // ⏱️ contadores visuais (milissegundos)
    private var microsleepMillis = 0L
    private var yawMillis = 0L

    // Landmarks e índices
    private val SELECTED_LANDMARKS = listOf(
        61, 40, 37, 0, 267, 270, 291,
        78, 95, 14, 317, 308,
        33, 160, 158, 133, 153, 144, 145,
        362, 385, 387, 263, 373, 380, 381,
        70, 105, 107, 336, 334, 300,
        1, 2, 168, 152
    )
    private val leftEyeIdx = 33
    private val rightEyeIdx = 263
    private val noseIdx = 1
    private val chinIdx = 152

    private var currentFace: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>? = null

    private var attentionRecoveryStartTime: Long = 0L
    private var yawRecoveryStartTime: Long = 0L


    companion object {
        private const val TAG = "AnalysisActivity"
        private const val MODEL_PATH = "face_landmarker.task"
        private const val ONNX_MODEL_NAME = "model_lstm_3_45_euclidean.onnx"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        alertManager = AlertManager(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvResult = findViewById(R.id.tvResult)
        tvAdditionalInfo = findViewById(R.id.tvAdditionalInfo)
        tvCounters = findViewById(R.id.tvCounters)
        analysisToolbar = findViewById(R.id.analysisToolbar)

        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        setSupportActionBar(analysisToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        analysisToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            R.id.action_more -> { showBottomMenu(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- Bottom sheet ---
    private fun showBottomMenu() {
        val bottomSheet = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_menu_analysis, null)
        bottomSheet.setContentView(view)

        view.findViewById<LinearLayout>(R.id.btnHistory).setOnClickListener {
            bottomSheet.dismiss(); showHistory()
        }
        view.findViewById<LinearLayout>(R.id.btnSettings).setOnClickListener {
            bottomSheet.dismiss(); showSettingsDialog()
        }
        view.findViewById<LinearLayout>(R.id.btnExit).setOnClickListener {
            bottomSheet.dismiss(); finish()
        }

        bottomSheet.show()
    }


    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_analysis_settings, null)

        val switchShowLandmarks = dialogView.findViewById<SwitchCompat>(R.id.switchShowLandmarks)
        val spinnerSensitivity = dialogView.findViewById<Spinner>(R.id.spinnerSensitivity)
        val btnClose = dialogView.findViewById<TextView>(R.id.btnCloseDialog)

        // Estado inicial do switch
        switchShowLandmarks.isChecked = overlayView.drawLandmarks

        // Listener do switch
        switchShowLandmarks.setOnCheckedChangeListener { _, isChecked ->
            overlayView.drawLandmarks = isChecked
            overlayView.invalidate()
        }

        // Configura spinner
        val options = listOf("Alta", "Média", "Baixa")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinnerSensitivity.adapter = adapter
        spinnerSensitivity.setSelection(options.indexOf(detectionSensitivity))

        spinnerSensitivity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                detectionSensitivity = options[position]

                Toast.makeText(
                    this@AnalysisActivity,
                    "Sensibilidade ajustada para: $detectionSensitivity",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Monta o diálogo estilizado
        val dialog = android.app.AlertDialog.Builder(this, R.style.RoundedDialog)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }



    private fun showHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        intent.putExtra("history", ArrayList(detectedEventsHistory))
        startActivity(intent)
    }

    // --- Permissão de câmera ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) { setupFaceLandmarker(); startCamera() }
            else {
                tvResult.text = "Permissão de câmera negada"
                Toast.makeText(this, "Permissão de câmera é necessária.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    // --- ONNX ---
    private fun initOnnxModel() {
        try {
            val file = File(cacheDir, ONNX_MODEL_NAME)
            if (!file.exists()) {
                assets.open(ONNX_MODEL_NAME).use { input -> FileOutputStream(file).use { input.copyTo(it) } }
            }
            ortEnv = OrtEnvironment.getEnvironment()
            ortSession = ortEnv?.createSession(file.absolutePath, OrtSession.SessionOptions())
            Log.i(TAG, "ONNX model carregado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro carregando ONNX", e)
            runOnUiThread { tvResult.text = "Erro carregando modelo: ${e.message}" }
        }
    }

    // --- Sensibilidades ---
    private fun microsleepThresholdMs(): Long = when (detectionSensitivity.lowercase()) {
        "alta" -> 1200L
        "média" -> 2000L
        "baixa" -> 3000L
        else -> 2000L
    }

    private fun yawThresholdMs(): Long = when (detectionSensitivity.lowercase()) {
        "alta" -> 2000L
        "média" -> 3000L
        "baixa" -> 4000L
        else -> 3000L
    }

    private fun yawnRequiredCount(): Int = when (detectionSensitivity.lowercase()) {
        "alta" -> 2
        "média" -> 3
        "baixa" -> 4
        else -> 3
    }


    // --- MediaPipe FaceLandmarker ---
    private var lastDetectionTime: Long = 0L
    private var isProcessingFrame = false

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
                    lastDetectionTime = System.currentTimeMillis()
                    val now = System.currentTimeMillis()

                    // --- quando nenhum rosto é detectado ---
                    if (faces.isEmpty()) {
                        runOnUiThread {
                            overlayView.setPoints(emptyList())
                            tvResult.text = "Status: Nenhum rosto detectado"
                            tvAdditionalInfo.text = "Aguardando detecção facial..."
                            tvCounters.text =
                                "Microsleep: 0.0s / ${microsleepThresholdMs() / 1000.0}s\n" +
                                        "Desatenção: 0.0s / ${yawThresholdMs() / 1000.0}s\n" +
                                        "Bocejos: 0 / ${yawnRequiredCount()}"
                        }

                        // inicializa cronômetro para ausência de rosto
                        if (lastEventSentToHistory != "Sem Rosto") {
                            attentionRecoveryStartTime = now
                        }

                        // só registra evento se a ausência persistir por 2 segundos
                        if (now - attentionRecoveryStartTime >= 2000L) {
                            if (lastEventSentToHistory != "Sem Rosto") {
                                handleEventTransition("Sem Rosto")
                                addEventToHistory("Sem Rosto")
                                Log.d(TAG, "Rosto ausente por mais de 2s — evento registrado.")
                            }
                        }

                        // reset de estados de análise
                        inMicrosleep = false
                        inDesattention = false
                        eyesClosedStartTime = 0L
                        yawStartTime = 0L
                        microsleepMillis = 0L
                        yawMillis = 0L

                        // impede processamento desnecessário
                        return@setResultListener
                    }

                    // --- rosto detectado normalmente ---
                    processFace(faces[0])

                    if (currentEventLabel == null) {
                        handleEventTransition("Atento")
                        addEventToHistory("Atento")
                        eventStartTime = System.currentTimeMillis()
                        Log.d(TAG, "Primeiro rosto detectado — iniciando estado Atento")
                    }


                } catch (e: Exception) {
                    Log.e(TAG, "Erro processando landmarks: ${e.message}", e)
                    resetLandmarker()
                }
            }
            .build()

        try {
            faceLandmarker?.close()
        } catch (_: Exception) { }

        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
        Log.i(TAG, "FaceLandmarker configurado com sucesso.")
    }



    // --- Loop principal de processamento ---
    private fun processFace(face: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>) {
        currentFace = face

        // Normalização pro modelo (centro no nariz, escala distância entre olhos)
        val cx = face[noseIdx].x()
        val cy = face[noseIdx].y()
        val scale = euclidean(face[leftEyeIdx], face[rightEyeIdx]).coerceAtLeast(1e-5f)

        val features = FloatArray(FEATURE_SIZE)
        var k = 0
        for (idx in SELECTED_LANDMARKS) {
            val lm = face[idx]
            features[k++] = (lm.x() - cx) / scale
            features[k++] = (lm.y() - cy) / scale
        }
        if (featureBuffer.size >= SEQ_LEN) featureBuffer.removeFirst()
        featureBuffer.addLast(features)

        // Métricas
        lastEar = calculateEAR(face)
        lastMar = calculateMAR(face)
        lastYaw = calculateYaw(face)

        // Históricos simples (se quiser suavizar)
        if (earHistory.size >= HISTORY_SIZE) earHistory.removeFirst()
        earHistory.addLast(lastEar)

        // Overlay
        runOnUiThread {
            overlayView.setPoints(SELECTED_LANDMARKS.map { idx -> face[idx].x() to face[idx].y() })
        }

        val label = decideFinalLabelAndUpdateCounters()
        runOnUiThread {
            tvResult.text = "Status: $label"
            tvAdditionalInfo.text = when (label) {
                "Microsleep detectado" -> { tvAdditionalInfo.setTextColor(Color.RED); "⚠️ Microsleep detectado!" }
                "Desatenção detectada" -> { tvAdditionalInfo.setTextColor(Color.YELLOW); "⚠️ Desatenção detectada!" }
                "Bocejo detectado"     -> { tvAdditionalInfo.setTextColor(Color.CYAN); "😮 Bocejo detectado!" }
                "Atento"               -> { tvAdditionalInfo.setTextColor(Color.GREEN); "🟢 Atenção normal" }
                else                   -> { tvAdditionalInfo.setTextColor(Color.LTGRAY); label }
            }

            tvCounters.text =
                "Microsleep: ${"%.1f".format(microsleepMillis/1000f)}s / ${"%.1f".format(microsleepThresholdMs()/1000f)}s\n" +
                        "Desatenção: ${"%.1f".format(yawMillis/1000f)}s / ${"%.1f".format(yawThresholdMs()/1000f)}s\n" +
                        "Bocejos: $yawnCount / ${yawnRequiredCount()}"
        }
    }

    // --- Lógica dos detectores + contadores (mantém ATENTO até atingir limite) ---
    private fun decideFinalLabelAndUpdateCounters(): String {
        val now = System.currentTimeMillis()
        val msThreshold = microsleepThresholdMs()
        val yawThreshold = yawThresholdMs()
        val yawnNeeded = yawnRequiredCount()
        var status = "Atento"

        // --- MICROSLEEP ---
        if (lastEar < 0.07f) {
            // olhos fechados
            if (eyesClosedStartTime == 0L) eyesClosedStartTime = now
            microsleepMillis = now - eyesClosedStartTime

            // atingiu o limite de microsleep
            if (!inMicrosleep && microsleepMillis >= msThreshold) {
                inMicrosleep = true
                handleEventTransition("Microsleep")
                addEventToHistory("Microsleep")
            }

            // resetar recuperação
            attentionRecoveryStartTime = 0L

        } else {
            // olhos abertos
            if (inMicrosleep) {
                if (attentionRecoveryStartTime == 0L)
                    attentionRecoveryStartTime = now

                // só sai do estado se permanecer 1s de olhos abertos
                if (now - attentionRecoveryStartTime >= 1000L) {
                    inMicrosleep = false
                    handleEventTransition("Alerta")
                    eyesClosedStartTime = 0L
                    microsleepMillis = 0L
                }
            } else {
                eyesClosedStartTime = 0L
                microsleepMillis = 0L
            }
        }

        // --- DESATENÇÃO (YAW) ---
        val yawNow = currentFace?.let { calculateYaw(it) } ?: 0f

        if (yawNow > 0.35f) {
            // cabeça virada
            if (yawStartTime == 0L) yawStartTime = now
            yawMillis = now - yawStartTime

            if (!inDesattention && yawMillis >= yawThreshold) {
                inDesattention = true
                handleEventTransition("Desatenção")
                addEventToHistory("Desatenção")
            }

            // resetar tempo de recuperação da atenção frontal
            yawRecoveryStartTime = 0L

        } else {
            // cabeça centralizada
            if (inDesattention) {
                if (yawRecoveryStartTime == 0L)
                    yawRecoveryStartTime = now

                // só sai do estado após 1s de cabeça centralizada
                if (now - yawRecoveryStartTime >= 1000L) {
                    inDesattention = false
                    handleEventTransition("Alerta")
                    yawStartTime = 0L
                    yawMillis = 0L
                }
            } else {
                yawStartTime = 0L
                yawMillis = 0L
            }
        }

// --- BOCEJO ---
        if (yawnNeeded != Int.MAX_VALUE) {
            if (lastMar > 0.65f) {
                if (!mouthOpen) {
                    mouthOpen = true
                    yawnStartTime = now
                }
            } else if (mouthOpen && now - yawnStartTime >= 500L) {
                mouthOpen = false
                yawnCount++

                handleEventTransition("Bocejo")
                addEventToHistory("Bocejo")

                saveEventToFirebase(
                    status = "Bocejo",
                    start = now - 500L,
                    end = now,
                    duration = 500L,
                    ear = lastEar,
                    mar = lastMar,
                    yaw = lastYaw,
                    conf = lastConf
                )

                Log.d(TAG, "Bocejo detectado e salvo no Firebase")

                if (yawnCount >= yawnNeeded) {
                    // mostra alerta de sonolência
                    showSleepWarningDialog()

                    // registra o evento de sonolência
                    handleEventTransition("Sinais de Sono")
                    addEventToHistory("Sinais de Sono")

                    // pausa estados ativos
                    inMicrosleep = false
                    inDesattention = false
                    eyesClosedStartTime = 0L
                    yawStartTime = 0L
                }
            }
        }


        status = when {
            inMicrosleep -> "Microsleep detectado"
            inDesattention -> "Desatenção detectada"
            else -> "Atento"
        }

        if (!inMicrosleep && !inDesattention && !mouthOpen) {
            handleEventTransition("Atento")
        }

        return status
    }



    // --- EAR / MAR / Yaw ---
    private fun calculateEAR(face: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val eye = listOf(33, 160, 158, 133, 153, 144) // esquerdo
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
        return euclidean(top, bottom) / euclidean(left, right).coerceAtLeast(1e-6f)
    }

    private fun calculateYaw(face: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val left = face[leftEyeIdx]
        val right = face[rightEyeIdx]
        val nose = face[noseIdx]
        val faceWidth = euclidean(left, right)
        val distNoseLeft = euclidean(nose, left)
        val distNoseRight = euclidean(nose, right)
        return abs(distNoseLeft - distNoseRight) / faceWidth.coerceAtLeast(1e-6f)
    }

    private fun euclidean(
        a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ) = sqrt((a.x() - b.x()).pow(2) + (a.y() - b.y()).pow(2))

    // --- Histórico local (apenas UI/Recycler) ---
    private var lastEventTime: Long = 0L

    private fun addEventToHistory(eventLabel: String) {
        val now = System.currentTimeMillis()

        // só bloqueia se for o mesmo evento muito próximo (evita flood)
        val isDuplicateRecent =
            (eventLabel == lastEventSentToHistory && now - lastEventTime < 3000L)

        if (!isDuplicateRecent || detectedEventsHistory.isEmpty()) {
            if (detectedEventsHistory.size >= MAX_HISTORY_ENTRIES) detectedEventsHistory.removeFirst()
            detectedEventsHistory.addLast(HistoryEntry(eventLabel, now))
            lastEventSentToHistory = eventLabel
            lastEventTime = now
        }
    }


    // --- Transição de eventos (salva fim/início no Firestore) ---
    private fun handleEventTransition(newLabel: String) {
        val now = System.currentTimeMillis()

        // Se já há um evento ativo e o estado mudou → fecha o anterior
        if (currentEventLabel != null && currentEventLabel != newLabel) {
            val start = eventStartTime
            val end = now

            if (start > 0L && end > start) {
                val duration = end - start

                // grava evento anterior com início/fim reais
                saveEventToFirebase(
                    status = currentEventLabel!!,
                    start = start,
                    end = end,
                    duration = duration,
                    ear = lastEar,
                    mar = lastMar,
                    yaw = lastYaw,
                    conf = lastConf
                )

                Log.d(TAG, "Evento encerrado: ${currentEventLabel} (${duration / 1000.0}s)")
            }
        }

        // Se o novo estado é diferente, inicia novo bloco
        if (currentEventLabel != newLabel) {
            currentEventLabel = newLabel
            eventStartTime = now
            Log.d(TAG, "Novo evento iniciado: $newLabel às $now")
        }

        when (newLabel) {
            "Microsleep" -> alertManager.playMicrosleep()
            "Desatenção" -> alertManager.startDesattentionLoop()
            "Bocejo" -> alertManager.startBocejoLoop()
            "Sem Rosto" -> alertManager.playSemRosto()
            "Atento", "Alerta" -> {
                alertManager.stopMicrosleep()
                alertManager.stopDesattentionLoop()
                alertManager.stopBocejoLoop()
            }
        }

    }

    private fun showSleepWarningDialog() {
        runOnUiThread {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sleep_warning, null)

            val tvWarning = dialogView.findViewById<TextView>(R.id.tvWarning)
            val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

            // animação piscando
            val blink = AlphaAnimation(0.0f, 1.0f).apply {
                duration = 500
                startOffset = 50
                repeatMode = AlphaAnimation.REVERSE
                repeatCount = AlphaAnimation.INFINITE
            }
            tvWarning.startAnimation(blink)

            val dialog = AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            btnOk.setOnClickListener {
                dialog.dismiss()
                yawnCount = 0
                handleEventTransition("Alerta")
                Toast.makeText(this, "Voltando à análise...", Toast.LENGTH_SHORT).show()
            }

            dialog.show()
        }
    }


    // 🔥 Grava no Firestore
    private fun saveEventToFirebase(
        status: String,
        start: Long,
        end: Long,
        duration: Long,
        ear: Float,
        mar: Float,
        yaw: Float,
        conf: Float
    ) {
        val userId = auth.currentUser?.uid ?: return

        // Ignora gravações curtas (< 200 ms) para evitar ruído
        if (duration < 200L) return

        val startDate = Date(start)
        val endDate = Date(end)

        val data = hashMapOf(
            "status" to status,
            "startTime" to start,
            "endTime" to end,
            "duration" to duration,
            "startReadable" to android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", startDate),
            "endReadable" to android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", endDate),
            "ear" to ear,
            "mar" to mar,
            "yaw" to yaw,
            "confidence" to conf,
            "device" to android.os.Build.MODEL
        )

        db.collection("users")
            .document(userId)
            .collection("events")
            .add(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Evento salvo: $status (${duration / 1000.0}s)")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao salvar evento", e)
            }
    }

    // --- Câmera ---
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
                if (isProcessingFrame) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                isProcessingFrame = true

                try {
                    val bitmap = imageProxy.toBitmap()
                    if (bitmap != null && faceLandmarker != null) {
                        val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                        val imgProcOptions = ImageProcessingOptions.builder()
                            .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
                            .build()

                        try {
                            faceLandmarker?.detectAsync(mpImage, imgProcOptions, System.currentTimeMillis())
                            overlayView.setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro detectAsync: ${e.message}")
                            resetLandmarker()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro processando frame: ${e.message}", e)
                } finally {
                    isProcessingFrame = false
                    imageProxy.close()
                }
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis
            )
            Log.i(TAG, "Câmera iniciada com sucesso")
        }, ContextCompat.getMainExecutor(this))
    }


    // --- Conversão ImageProxy → Bitmap ---
    fun ImageProxy.toBitmap(): Bitmap? {
        return try {
            val yuvToRgbConverter = YuvToRgbConverter(this@AnalysisActivity)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            yuvToRgbConverter.yuvToRgb(this, bitmap)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Erro convertendo bitmap: ${e.message}")
            null
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        alertManager.release()
        closeOngoingEventIfAny()
        try {
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar recursos: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        closeOngoingEventIfAny()
    }

    private fun closeOngoingEventIfAny() {
        val label = currentEventLabel ?: return
        val start = eventStartTime
        if (start <= 0L) return

        val end = System.currentTimeMillis()
        val duration = end - start

        if (duration < 500L) return // ignora ruídos curtíssimos

        saveEventToFirebase(
            status = label,
            start = start,
            end = end,
            duration = duration,
            ear = lastEar,
            mar = lastMar,
            yaw = lastYaw,
            conf = lastConf
        )

        Log.d(TAG, "Evento final salvo automaticamente: $label (${duration / 1000.0}s)")
        currentEventLabel = null
        eventStartTime = 0L
    }


    private fun resetLandmarker() {
        try {
            faceLandmarker?.close()
            faceLandmarker = null
            setupFaceLandmarker()
            Log.w(TAG, "FaceLandmarker reiniciado após falha")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reiniciar Landmarker: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (faceLandmarker == null) {
                setupFaceLandmarker()
            }
            startCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao retomar câmera: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            faceLandmarker?.close()
            faceLandmarker = null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fechar faceLandmarker: ${e.message}")
        }
    }


}
