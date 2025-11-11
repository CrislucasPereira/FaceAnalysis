package com.example.faceanalysis

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class ReportActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: HorizontalBarChart
    private lateinit var radarChart: RadarChart
    private lateinit var tvChartTitle: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var spinnerChartType: Spinner

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val calendar = Calendar.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private var selectedChart = "Pizza"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val toolbar = findViewById<MaterialToolbar>(R.id.reportToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        radarChart = findViewById(R.id.radarChart)
        tvChartTitle = findViewById(R.id.tvChartTitle)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        spinnerChartType = findViewById(R.id.spinnerChartType)

        // Mensagem quando não houver dados para exibir nos gráficos
        val noDataMsg = getString(R.string.no_chart_data)
        val noDataColor = Color.parseColor("#FFB300")
        pieChart.setNoDataText(noDataMsg); pieChart.setNoDataTextColor(noDataColor)
        barChart.setNoDataText(noDataMsg); barChart.setNoDataTextColor(noDataColor)
        radarChart.setNoDataText(noDataMsg); radarChart.setNoDataTextColor(noDataColor)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
        } catch (_: Exception) { }

        //Spinner com texto escuro e visual consistente
        val chartTypes = listOf("Pizza", "Barras", "Radar")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, chartTypes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerChartType.adapter = adapter
        spinnerChartType.setSelection(0)
        spinnerChartType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedChart = chartTypes[pos]
                updateChartVisibility()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        //Efeito de clique suave
        btnSelectDate.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    showDatePicker()
                }
            }
            true
        }

        btnSelectDate.setOnClickListener { showDatePicker() }

        loadEventsForDate(calendar.time)
    }

    private fun showDatePicker() {
        val picker = DatePickerDialog(
            this,
            { _, y, m, d ->
                calendar.set(y, m, d)
                loadEventsForDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        picker.show()
    }

    private fun loadEventsForDate(date: Date) {
        val userId = auth.currentUser?.uid ?: return
        listenerRegistration?.remove()

        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000

        listenerRegistration = db.collection("users")
            .document(userId)
            .collection("events")
            .whereGreaterThanOrEqualTo("startTime", startOfDay)
            .whereLessThan("startTime", endOfDay)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("Report", "Erro carregando eventos: ${error?.message}", error)
                    Toast.makeText(this, "Erro ao carregar eventos", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot.isEmpty) {
                    findViewById<LinearLayout>(R.id.containerSummary).removeAllViews()
                    pieChart.clear(); barChart.clear(); radarChart.clear()
                    // força redesenho para exibir a mensagem "Sem dados disponíveis"
                    pieChart.invalidate(); barChart.invalidate(); radarChart.invalidate()
                    return@addSnapshotListener
                }

                val events = snapshot.documents.mapNotNull { it.data }
                displayCharts(events, date)
            }
    }

    private fun displayCharts(events: List<Map<String, Any>>, date: Date) {
        //Unifica "Alerta" e "Atento"
        val normalizedEvents = events.map { event ->
            val status = event["status"].toString()
            val unifiedStatus = when (status.lowercase()) {
                "alerta" -> "Atento"
                else -> status
            }
            event.toMutableMap().apply { this["status"] = unifiedStatus }
        }

        // Remove eventos de Desatenção dos relatórios
        val filtered = normalizedEvents.filter { !it["status"].toString().contains("Desatenção", true) }
        val grouped = filtered.groupBy { it["status"].toString() }
        // Lida com números salvos como Long, Int ou Double no Firestore
        val summary = grouped.mapValues { group ->
            group.value.sumOf { e ->
                val n = e["duration"]
                when (n) {
                    is Number -> n.toLong()
                    is String -> n.toLongOrNull() ?: 0L
                    else -> 0L
                }
            }
        }

        val pieEntries = ArrayList<PieEntry>()
        val barEntries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()
        val statuses = summary.keys.toList()

        var i = 0
        summary.forEach { (status, durationMs) ->
            val min = durationMs / 60000f
            if (min > 0) {
                pieEntries.add(PieEntry(min, status))
                barEntries.add(BarEntry(i.toFloat(), min))
                colors.add(
                    when {
                        status.contains("Microsleep", true) -> Color.parseColor("#E53935")
                        status.contains("Bocejo", true) -> Color.parseColor("#FFB300")
                        status.contains("Atento", true) -> Color.parseColor("#43A047")
                        // Sem mapeamento de cor para estados descontinuados
                        status.contains("Sem Rosto", true) -> Color.parseColor("#9E9E9E")
                        status.contains("Sinais", true) -> Color.parseColor("#FB8C00")
                        else -> ColorTemplate.MATERIAL_COLORS[i % ColorTemplate.MATERIAL_COLORS.size]
                    }
                )
                i++
            }
        }

        setupPieChart(pieEntries, colors)
        setupBarChart(barEntries, statuses, colors)
        setupRadarChart(summary)
        setupInteractiveSummary(filtered, summary)
        updateChartVisibility()
    }

    // ---------- Gráficos ----------
    private fun setupPieChart(entries: List<PieEntry>, colors: List<Int>) {
        val ds = PieDataSet(entries, "")
        ds.colors = colors
        ds.valueTextColor = Color.BLACK
        ds.valueTextSize = 13f

        //Rótulos externos com linhas conectando as fatias
        ds.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        ds.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        ds.valueLinePart1OffsetPercentage = 85f
        ds.valueLinePart1Length = 0.4f
        ds.valueLinePart2Length = 0.5f
        ds.valueLineColor = Color.DKGRAY

        //Dados de exibição
        val data = PieData(ds)
        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(false) // oculta rótulos internos
        pieChart.isDrawHoleEnabled = true

        // Estilo doughnut premium
        pieChart.holeRadius = 60f
        pieChart.transparentCircleRadius = 65f
        pieChart.setHoleColor(Color.parseColor("#FAFAFA"))

        // Legenda: evitar sobreposição e permitir quebra de linha
        val legend = pieChart.legend
        legend.textColor = Color.DKGRAY
        legend.textSize = 12f
        legend.formSize = 12f
        legend.form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        legend.isWordWrapEnabled = true
        legend.xEntrySpace = 12f
        legend.yEntrySpace = 8f
        legend.formToTextSpace = 6f
        legend.setDrawInside(false)
        pieChart.extraBottomOffset = 14f

        // Texto central dinâmico
        val totalMinutes = entries.sumOf { it.value.toDouble() }
        pieChart.centerText = "Total Analisado\n${"%.1f".format(totalMinutes)} min"
        pieChart.setCenterTextSize(15f)
        pieChart.setCenterTextColor(Color.parseColor("#1C1C1E"))
        pieChart.setCenterTextTypeface(resources.getFont(R.font.poppins_semibold))

        // Legenda e aparência geral
        pieChart.legend.textColor = Color.DKGRAY
        pieChart.legend.textSize = 13f
        pieChart.legend.isWordWrapEnabled = true
        pieChart.description.isEnabled = false

        // Animação suave
        pieChart.animateXY(1000, 1000)

        // Atualiza o gráfico
        pieChart.invalidate()
    }


    private fun setupBarChart(entries: List<BarEntry>, labels: List<String>, colors: List<Int>) {
        val ds = BarDataSet(entries, "Tempo (minutos)")
        ds.colors = colors
        ds.valueTextColor = Color.BLACK
        ds.valueTextSize = 13f
        ds.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${"%.1f".format(value)} min"
            }
        }


        val data = BarData(ds)
        data.barWidth = 0.7f
        barChart.data = data

        // Configuração dos eixos
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.DKGRAY
        xAxis.textSize = 13f
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        val leftAxis = barChart.axisLeft
        leftAxis.textColor = Color.DKGRAY
        leftAxis.textSize = 12f
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#DDDDDD")

        barChart.axisRight.isEnabled = false

        // Aparência geral
        barChart.description.isEnabled = false
        barChart.legend.apply {
            textColor = Color.DKGRAY
            textSize = 13f
            isWordWrapEnabled = true
            horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            xEntrySpace = 12f; yEntrySpace = 8f; formToTextSpace = 6f
            setDrawInside(false)
        }
        barChart.setFitBars(true)
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)
        barChart.setDrawGridBackground(false)
        barChart.extraBottomOffset = 10f
        barChart.extraTopOffset = 10f

        // Animação suave
        barChart.animateY(1000)

        // Atualiza o gráfico
        barChart.invalidate()
    }


    private fun setupRadarChart(summary: Map<String, Long>) {
        val entries = summary.map { RadarEntry(it.value / 60000f) }
        val ds = RadarDataSet(entries, "Distribuição de Tempo")
        ds.color = Color.parseColor("#1E88E5")
        ds.fillColor = Color.parseColor("#42A5F5")
        ds.setDrawFilled(true)
        ds.lineWidth = 2f
        val data = RadarData(ds)
        data.setValueTextColor(Color.DKGRAY)
        radarChart.data = data
        radarChart.xAxis.valueFormatter =
            com.github.mikephil.charting.formatter.IndexAxisValueFormatter(summary.keys.toList())
        radarChart.yAxis.setDrawLabels(false)
        radarChart.legend.apply {
            textColor = Color.DKGRAY
            textSize = 12f
            isWordWrapEnabled = true
            horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            xEntrySpace = 12f; yEntrySpace = 8f; formToTextSpace = 6f
            setDrawInside(false)
        }
        radarChart.description.text = ""
        radarChart.animateXY(900, 900)
        radarChart.invalidate()
    }

    private fun updateChartVisibility() {
        pieChart.visibility = if (selectedChart == "Pizza") View.VISIBLE else View.GONE
        barChart.visibility = if (selectedChart == "Barras") View.VISIBLE else View.GONE
        radarChart.visibility = if (selectedChart == "Radar") View.VISIBLE else View.GONE
        tvChartTitle.text = when (selectedChart) {
            "Pizza" -> "Distribuição de Estados"
            "Barras" -> "Tempo por Categoria"
            "Radar" -> "Comparativo Geral de Estados"
            else -> ""
        }
    }

    // ---------- Cards Interativos ----------
    @SuppressLint("InflateParams")
    private fun setupInteractiveSummary(events: List<Map<String, Any>>, summary: Map<String, Long>) {
        val container = findViewById<LinearLayout>(R.id.containerSummary)
        container.removeAllViews()
        val inflater = layoutInflater
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        summary.forEach { (status, durationMs) ->
            // Inflate com o parent para que as LayoutParams (margens) sejam respeitadas
            val view = inflater.inflate(R.layout.item_status_card, container, false)
            val tvName = view.findViewById<TextView>(R.id.tvStatusName)
            val tvTime = view.findViewById<TextView>(R.id.tvTotalTime)
            val tvOcc = view.findViewById<TextView>(R.id.tvOccurrences)
            val tvAvg = view.findViewById<TextView>(R.id.tvAvgDuration)
            val tvRange = view.findViewById<TextView>(R.id.tvTimeRange)
            val details = view.findViewById<LinearLayout>(R.id.detailsLayout)
            val header = view.findViewById<LinearLayout>(R.id.headerLayout)
            val icon = view.findViewById<ImageView>(R.id.iconExpand)

            val matches = events.filter { it["status"] == status }
            val occ = matches.size
            val avgMs = if (occ > 0) durationMs / occ else 0L
            val first = matches.minOfOrNull { it["startTime"] as? Long ?: 0L }
            val last = matches.maxOfOrNull { it["endTime"] as? Long ?: 0L }

            tvName.text = status
            tvTime.text = String.format("%.1f min", durationMs / 60000f)
            tvOcc.text = "Ocorrências: $occ"
            tvAvg.text = "Duração média: ${(avgMs / 1000)}s"
            tvRange.text =
                if (first != null && last != null)
                    "Primeiro: ${format.format(Date(first))} / Último: ${format.format(Date(last))}"
                else "Sem intervalo registrado"

            header.setOnClickListener {
                val expanded = details.visibility == View.VISIBLE
                details.visibility = if (expanded) View.GONE else View.VISIBLE
                icon.rotation = if (expanded) 0f else 180f
            }

            container.addView(view)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}

