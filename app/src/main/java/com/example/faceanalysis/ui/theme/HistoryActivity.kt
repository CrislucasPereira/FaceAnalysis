package com.example.faceanalysis

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val historyToolbar: Toolbar = findViewById(R.id.historyToolbar)
        setSupportActionBar(historyToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Habilita o botão de voltar na Toolbar

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Recebe o histórico da Intent
        val historyData = intent.getSerializableExtra("history") as? ArrayList<HistoryEntry>
            ?: arrayListOf()

        val adapter = HistoryAdapter(historyData)
        recyclerView.adapter = adapter

        // Configura o comportamento do botão "Voltar"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // Apenas finaliza esta Activity e volta para a anterior
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { // ID do botão de voltar da Toolbar
                onBackPressedDispatcher.onBackPressed() // Usa o novo dispatcher
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
