package com.example.levabolliapp

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import android.content.ActivityNotFoundException
import androidx.core.content.FileProvider

class MainActivity : AppCompatActivity() {

    private val REQ_WRITE_STORAGE = 1001
    private var pendingPdfExport = false

    private lateinit var etCliente: EditText
    private lateinit var etData: EditText
    private lateinit var etAuto: EditText
    private lateinit var etModello: EditText
    private lateinit var etTarga: EditText
    private lateinit var recyclerPanels: RecyclerView
    private lateinit var txtTotConsigliato: TextView
    private lateinit var txtTotApplicato: TextView
    private lateinit var txtTotSconto: TextView

    companion object {
        private const val PREFS_NAME = "levabolli_prefs"
        private const val KEY_CUSTOM_LISTINO = "custom_listino"
        private const val KEY_PREVENTIVI = "preventivi"
        private const val REQUEST_PREVENTIVI = 2001
    }

    // -------- LISTINO --------

    data class PrezzoBollo(
        val minBolli: Int,
        val maxBolli: Int,
        val misura: Int,
        val prezzo: Int
    )

    private val defaultListino = listOf(
        // MISURA 1 (<10 mm)
        PrezzoBollo(1, 2, 1, 44),
        PrezzoBollo(3, 5, 1, 71),
        PrezzoBollo(6, 10, 1, 99),
        PrezzoBollo(11, 20, 1, 137),
        PrezzoBollo(21, 30, 1, 181),
        PrezzoBollo(31, 40, 1, 201),
        PrezzoBollo(41, 60, 1, 217),
        PrezzoBollo(61, 80, 1, 291),
        PrezzoBollo(81, 100, 1, 346),
        PrezzoBollo(101, 120, 1, 371),
        PrezzoBollo(121, 150, 1, 392),
        PrezzoBollo(151, 180, 1, 418),
        PrezzoBollo(181, 210, 1, 478),
        PrezzoBollo(211, 240, 1, 510),
        PrezzoBollo(241, 270, 1, 555),
        PrezzoBollo(271, 300, 1, 695),
        PrezzoBollo(301, 350, 1, 737),

        // MISURA 2 (<25 mm)
        PrezzoBollo(1, 2, 2, 79),
        PrezzoBollo(3, 5, 2, 107),
        PrezzoBollo(6, 10, 2, 151),
        PrezzoBollo(11, 20, 2, 184),
        PrezzoBollo(21, 30, 2, 245),
        PrezzoBollo(31, 40, 2, 312),
        PrezzoBollo(41, 60, 2, 380),
        PrezzoBollo(61, 80, 2, 459),
        PrezzoBollo(81, 100, 2, 492),
        PrezzoBollo(101, 120, 2, 562),
        PrezzoBollo(121, 150, 2, 618),
        PrezzoBollo(151, 180, 2, 686),
        PrezzoBollo(181, 210, 2, 735),
        PrezzoBollo(211, 240, 2, 801),
        PrezzoBollo(241, 270, 2, 862),
        PrezzoBollo(271, 300, 2, 908),
        PrezzoBollo(301, 350, 2, 979),

        // MISURA 3 (<45 mm)
        PrezzoBollo(1, 2, 3, 123),
        PrezzoBollo(3, 5, 3, 158),
        PrezzoBollo(6, 10, 3, 201),
        PrezzoBollo(11, 20, 3, 241),
        PrezzoBollo(21, 30, 3, 314),
        PrezzoBollo(31, 40, 3, 382),
        PrezzoBollo(41, 60, 3, 459),
        PrezzoBollo(61, 80, 3, 566),
        PrezzoBollo(81, 100, 3, 639),
        PrezzoBollo(101, 120, 3, 717),
        PrezzoBollo(121, 150, 3, 774),
        PrezzoBollo(151, 180, 3, 827),
        PrezzoBollo(181, 210, 3, 921),
        PrezzoBollo(211, 240, 3, 992),
        PrezzoBollo(241, 270, 3, 997),
        PrezzoBollo(271, 300, 3, 1064),
        PrezzoBollo(301, 350, 3, 1138),
        PrezzoBollo(351, 500, 3, 1221)
    )

    private var currentListino: List<PrezzoBollo> = defaultListino

    // -------- PANNELLI --------

    data class PanelData(
        val nome: String,
        var numBolli: Int = 0,
        var diametroMm: Double = 0.0,
        var alluminio: Boolean = false,
        var numSportellate: Int = 0,
        var diametroSportellataMm: Double = 0.0,
        var prezzoConsigliato: Int = 0,
        var prezzoApplicato: Int = 0
    )

    private val panels = mutableListOf<PanelData>()
    private lateinit var adapter: PanelAdapter

    // -------- PREVENTIVI --------

    data class Preventivo(
        val id: Long,
        val tecnico: String,
        val data: String,
        val marca: String,
        val modello: String,
        val targa: String,
        val totaleConsigliato: Int,
        val totaleApplicato: Int,
        val pannelli: List<PanelData>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainActivityHolder.instance = this

        // Language
        val langTag = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("app_language_tag", "") ?: ""
        if (langTag.isNotBlank()) {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(langTag)
            )
        } else {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.getEmptyLocaleList()
            )
        }

        setContentView(R.layout.activity_main)

        etCliente = findViewById(R.id.etCliente)
        etData = findViewById(R.id.etData)
        etAuto = findViewById(R.id.etAuto)
        etModello = findViewById(R.id.etModello)
        etTarga = findViewById(R.id.etTarga)
        recyclerPanels = findViewById(R.id.recyclerPanels)
        txtTotConsigliato = findViewById(R.id.txtTotConsigliato)
        txtTotApplicato = findViewById(R.id.txtTotApplicato)
        txtTotSconto = findViewById(R.id.txtTotSconto)

        loadCustomListino()
        setupPanels()

        // Buttons
        findViewById<Button>(R.id.btnMisuraFoto).setOnClickListener {
            startActivity(Intent(this, MeasureActivity::class.java))
        }

        findViewById<Button>(R.id.btnListino).setOnClickListener {
            startActivity(Intent(this, ListinoActivity::class.java))
        }

        findViewById<Button>(R.id.btnInfo).setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        findViewById<Button>(R.id.btnSalvaPreventivo).setOnClickListener {
            salvaPreventivo()
        }

        findViewById<Button>(R.id.btnPreventiviSalvati).setOnClickListener {
            val intent = Intent(this, PreventiviActivity::class.java)
            startActivityForResult(intent, REQUEST_PREVENTIVI)
        }

        findViewById<Button>(R.id.btnEsportaPdf).setOnClickListener {
            exportPdf()
        }

        findViewById<Button>(R.id.btnApriCartellaPdf).setOnClickListener {
            openDownloads()
        }

        updateTotali()
    }

    private fun setupPanels() {
        panels.clear()
        panels.add(PanelData(getString(R.string.panel_hood)))
        panels.add(PanelData(getString(R.string.panel_roof)))
        panels.add(PanelData(getString(R.string.panel_trunk)))
        panels.add(PanelData(getString(R.string.panel_fender_fl)))
        panels.add(PanelData(getString(R.string.panel_door_fl)))
        panels.add(PanelData(getString(R.string.panel_door_rl)))
        panels.add(PanelData(getString(R.string.panel_fender_rl)))
        panels.add(PanelData(getString(R.string.panel_montante_sx)))
        panels.add(PanelData(getString(R.string.panel_fender_fr)))
        panels.add(PanelData(getString(R.string.panel_door_fr)))
        panels.add(PanelData(getString(R.string.panel_door_rr)))
        panels.add(PanelData(getString(R.string.panel_fender_rr)))
        panels.add(PanelData(getString(R.string.panel_montante_dx)))

        adapter = PanelAdapter(panels) { panel ->
            showPanelDialog(panel)
        }
        recyclerPanels.layoutManager = LinearLayoutManager(this)
        recyclerPanels.adapter = adapter
    }

    // --- LISTINO CUSTOM ---

    private fun loadCustomListino() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CUSTOM_LISTINO, null)
        if (json.isNullOrEmpty()) {
            currentListino = defaultListino
            return
        }
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<PrezzoBollo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    PrezzoBollo(
                        o.getInt("min"),
                        o.getInt("max"),
                        o.getInt("misura"),
                        o.getInt("prezzo")
                    )
                )
            }
            if (list.isNotEmpty()) {
                currentListino = list
            } else {
                currentListino = defaultListino
            }
        } catch (e: Exception) {
            currentListino = defaultListino
        }
    }

    fun saveCustomListinoFromActivity(json: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_LISTINO, json).apply()
        loadCustomListino()
        ricalcolaPannelli()
    }

    private fun ricalcolaPannelli() {
        panels.forEach { panel ->
            if (panel.numBolli > 0 || panel.numSportellate > 0) {
                val prezzoGrandine = calcolaPrezzoGrandine(panel.numBolli, panel.diametroMm, panel.alluminio)
                val prezzoSportellate = calcolaPrezzoSportellate(panel.numSportellate, panel.diametroSportellataMm, panel.alluminio)
                val cons = (prezzoGrandine ?: 0) + (prezzoSportellate ?: 0)
                if (cons > 0 && panel.prezzoApplicato == panel.prezzoConsigliato) {
                    panel.prezzoApplicato = cons
                }
                panel.prezzoConsigliato = cons
            }
        }
        adapter.notifyDataSetChanged()
        updateTotali()
    }

    private fun calcolaPrezzoGrandine(numBolli: Int, diametroMm: Double, isAlluminio: Boolean): Int? {
        if (numBolli <= 0 || diametroMm <= 0.0) return null

        val misura = when {
            diametroMm < 10.0 -> 1
            diametroMm < 25.0 -> 2
            diametroMm < 45.0 -> 3
            else -> 3
        }

        val voce = currentListino.firstOrNull {
            numBolli >= it.minBolli && numBolli <= it.maxBolli && it.misura == misura
        } ?: return null

        val base = voce.prezzo
        return if (isAlluminio) {
            (base * 1.3).roundToInt()
        } else {
            base
        }
    }

    private fun calcolaPrezzoSportellate(
        numSportellate: Int,
        diametroMm: Double,
        isAlluminio: Boolean
    ): Int? {
        if (numSportellate <= 0) return null

        val misura = when {
            diametroMm <= 0.0 -> 2
            diametroMm < 10.0 -> 1
            diametroMm < 25.0 -> 2
            else -> 3
        }

        val bolliEqPerSportellata = when (misura) {
            1 -> 5
            2 -> 10
            else -> 20
        }

        val numBolliEq = numSportellate * bolliEqPerSportellata

        val voce = currentListino.firstOrNull {
            numBolliEq >= it.minBolli && numBolliEq <= it.maxBolli && it.misura == misura
        } ?: return null

        val base = voce.prezzo
        return if (isAlluminio) {
            (base * 1.3).roundToInt()
        } else {
            base
        }
    }

    private fun showPanelDialog(panel: PanelData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_panel, null)

        val txtName = dialogView.findViewById<TextView>(R.id.txtPanelName)
        val edtNumBolli = dialogView.findViewById<EditText>(R.id.edtNumBolli)
        val edtDiametro = dialogView.findViewById<EditText>(R.id.edtDiametro)
        val chkAlluminio = dialogView.findViewById<CheckBox>(R.id.chkAlluminio)
        val edtNumSportellate = dialogView.findViewById<EditText>(R.id.edtNumSportellate)
        val edtDiametroSportellata = dialogView.findViewById<EditText>(R.id.edtDiametroSportellata)
        val txtPrezzoConsigliatoPanel = dialogView.findViewById<TextView>(R.id.txtPrezzoConsigliatoPanel)
        val edtPrezzoApplicatoPanel = dialogView.findViewById<EditText>(R.id.edtPrezzoApplicatoPanel)

        txtName.text = panel.nome

        if (panel.numBolli > 0) edtNumBolli.setText(panel.numBolli.toString())
        if (panel.diametroMm > 0) edtDiametro.setText(panel.diametroMm.toString())
        chkAlluminio.isChecked = panel.alluminio
        if (panel.numSportellate > 0) edtNumSportellate.setText(panel.numSportellate.toString())
        if (panel.diametroSportellataMm > 0) edtDiametroSportellata.setText(panel.diametroSportellataMm.toString())

        if (panel.prezzoConsigliato > 0) {
            txtPrezzoConsigliatoPanel.text = getString(R.string.panel_suggested_price_value, panel.prezzoConsigliato)
        }
        if (panel.prezzoApplicato > 0) {
            edtPrezzoApplicatoPanel.setText(panel.prezzoApplicato.toString())
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                val numBolli = edtNumBolli.text.toString().toIntOrNull() ?: 0
                val diametro = edtDiametro.text.toString().toDoubleOrNull() ?: 0.0
                val isAlluminio = chkAlluminio.isChecked
                val numSportellate = edtNumSportellate.text.toString().toIntOrNull() ?: 0
                val diametroSport = edtDiametroSportellata.text.toString().toDoubleOrNull() ?: 0.0

                panel.numBolli = numBolli
                panel.diametroMm = diametro
                panel.alluminio = isAlluminio
                panel.numSportellate = numSportellate
                panel.diametroSportellataMm = diametroSport

                val prezzoGrandine = calcolaPrezzoGrandine(numBolli, diametro, isAlluminio) ?: 0
                val prezzoSportellate = calcolaPrezzoSportellate(numSportellate, diametroSport, isAlluminio) ?: 0
                val cons = prezzoGrandine + prezzoSportellate

                panel.prezzoConsigliato = cons

                val prezzoAppManuale = edtPrezzoApplicatoPanel.text.toString().toIntOrNull()
                panel.prezzoApplicato = when {
                    prezzoAppManuale != null && prezzoAppManuale > 0 -> prezzoAppManuale
                    cons > 0 -> cons
                    else -> 0
                }

                adapter.notifyDataSetChanged()
                updateTotali()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateTotali() {
        val totCons = panels.sumOf { it.prezzoConsigliato }
        val totApp = panels.sumOf { it.prezzoApplicato }
        val sconto = (totCons - totApp).coerceAtLeast(0)
        val scontoPerc = if (totCons > 0) (sconto.toDouble() / totCons.toDouble() * 100.0) else 0.0

        txtTotConsigliato.text = getString(R.string.total_suggested_value, totCons)
        txtTotApplicato.text = getString(R.string.total_applied_value, totApp)
        txtTotSconto.text = getString(R.string.total_discount_value, sconto, String.format("%.1f", scontoPerc))
    }

    private fun showSettingsDialog() {
        val items = arrayOf("Sistema", "Italiano", "English")
        val tags = arrayOf("", "it", "en")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings))
            .setItems(items) { _, which ->
                val tag = tags[which]
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("app_language_tag", tag)
                    .apply()

                if (tag.isNotBlank()) {
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags(tag)
                    )
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                    )
                }
            }
            .setNeutralButton("Versione: 1.0") { _, _ -> }
            .show()
    }

    // --- PREVENTIVI ---

    private fun salvaPreventivo() {
        val totCons = panels.sumOf { it.prezzoConsigliato }
        val totApp = panels.sumOf { it.prezzoApplicato }
        if (totCons == 0 && totApp == 0) {
            Toast.makeText(this, R.string.no_data_to_save, Toast.LENGTH_SHORT).show()
            return
        }

        val id = System.currentTimeMillis()
        val tecnico = etCliente.text.toString()
        val data = etData.text.toString()
        val marca = etAuto.text.toString()
        val modello = etModello.text.toString()
        val targa = etTarga.text.toString()

        val preventivo = Preventivo(
            id,
            tecnico,
            data,
            marca,
            modello,
            targa,
            totCons,
            totApp,
            panels.toList()
        )

        val arr = loadPreventiviJson()
        arr.put(preventivoToJson(preventivo))
        savePreventiviJson(arr)

        Toast.makeText(this, R.string.quote_saved, Toast.LENGTH_SHORT).show()
    }

    private fun loadPreventiviJson(): JSONArray {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREVENTIVI, null) ?: return JSONArray()
        return try {
            JSONArray(json)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun savePreventiviJson(arr: JSONArray) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREVENTIVI, arr.toString()).apply()
    }

    private fun preventivoToJson(p: Preventivo): JSONObject {
        val o = JSONObject()
        o.put("id", p.id)
        o.put("tecnico", p.tecnico)
        o.put("data", p.data)
        o.put("marca", p.marca)
        o.put("modello", p.modello)
        o.put("targa", p.targa)
        o.put("totCons", p.totaleConsigliato)
        o.put("totApp", p.totaleApplicato)

        val pannelliArr = JSONArray()
        p.pannelli.forEach { panel ->
            val po = JSONObject()
            po.put("nome", panel.nome)
            po.put("numBolli", panel.numBolli)
            po.put("diametroMm", panel.diametroMm)
            po.put("alluminio", panel.alluminio)
            po.put("numSportellate", panel.numSportellate)
            po.put("diametroSport", panel.diametroSportellataMm)
            po.put("prezzoCons", panel.prezzoConsigliato)
            po.put("prezzoApp", panel.prezzoApplicato)
            pannelliArr.put(po)
        }
        o.put("pannelli", pannelliArr)
        return o
    }

    private fun jsonToPreventivo(o: JSONObject): Preventivo {
        val id = o.getLong("id")
        val tecnico = o.optString("tecnico", "")
        val data = o.optString("data", "")
        val marca = o.optString("marca", "")
        val modello = o.optString("modello", "")
        val targa = o.optString("targa", "")
        val totCons = o.optInt("totCons", 0)
        val totApp = o.optInt("totApp", 0)

        val pannelliArr = o.optJSONArray("pannelli") ?: JSONArray()
        val pannelliList = mutableListOf<PanelData>()
        for (i in 0 until pannelliArr.length()) {
            val po = pannelliArr.getJSONObject(i)
            pannelliList.add(
                PanelData(
                    po.getString("nome"),
                    po.optInt("numBolli", 0),
                    po.optDouble("diametroMm", 0.0),
                    po.optBoolean("alluminio", false),
                    po.optInt("numSportellate", 0),
                    po.optDouble("diametroSport", 0.0),
                    po.optInt("prezzoCons", 0),
                    po.optInt("prezzoApp", 0)
                )
            )
        }

        return Preventivo(
            id,
            tecnico,
            data,
            marca,
            modello,
            targa,
            totCons,
            totApp,
            pannelliList
        )
    }

    fun loadPreventiviForActivity(): List<Preventivo> {
        val arr = loadPreventiviJson()
        val list = mutableListOf<Preventivo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(jsonToPreventivo(o))
        }
        return list.sortedByDescending { it.id }
    }

    fun deletePreventivoById(id: Long) {
        val arr = loadPreventiviJson()
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getLong("id") != id) {
                newArr.put(o)
            }
        }
        savePreventiviJson(newArr)
    }

    private fun applyPreventivo(p: Preventivo) {
        etCliente.setText(p.tecnico)
        etData.setText(p.data)
        etAuto.setText(p.marca)
        etModello.setText(p.modello)
        etTarga.setText(p.targa)

        panels.forEach { panel ->
            panel.numBolli = 0
            panel.diametroMm = 0.0
            panel.alluminio = false
            panel.numSportellate = 0
            panel.diametroSportellataMm = 0.0
            panel.prezzoConsigliato = 0
            panel.prezzoApplicato = 0
        }

        p.pannelli.forEach { pPanel ->
            val target = panels.firstOrNull { it.nome == pPanel.nome }
            if (target != null) {
                target.numBolli = pPanel.numBolli
                target.diametroMm = pPanel.diametroMm
                target.alluminio = pPanel.alluminio
                target.numSportellate = pPanel.numSportellate
                target.diametroSportellataMm = pPanel.diametroSportellataMm
                target.prezzoConsigliato = pPanel.prezzoConsigliato
                target.prezzoApplicato = pPanel.prezzoApplicato
            }
        }

        adapter.notifyDataSetChanged()
        updateTotali()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PREVENTIVI && resultCode == RESULT_OK && data != null) {
            val id = data.getLongExtra("preventivo_id", -1L)
            if (id > 0) {
                val list = loadPreventiviForActivity()
                val p = list.firstOrNull { it.id == id }
                if (p != null) {
                    applyPreventivo(p)
                }
            }
        }
    }

    // --- PDF EXPORT ---

    private fun exportPdf() {
        val client = etCliente.text.toString().trim()
        val date = etData.text.toString().trim()
        val car = etAuto.text.toString().trim()
        val model = etModello.text.toString().trim()
        val plate = etTarga.text.toString().trim()

        val fileNameBase = listOf(
            if (client.isBlank()) "Preventivo" else client,
            if (plate.isBlank()) "" else plate.replace(" ", ""),
            if (date.isBlank()) "" else date.replace("/", "-")
        ).filter { it.isNotBlank() }.joinToString("_")

        val fileName = "${fileNameBase}.pdf"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    android.provider.MediaStore.Downloads.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS + "/LevabolliApp"
                )
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                Toast.makeText(this, "Errore: impossibile creare il file PDF", Toast.LENGTH_LONG).show()
                return
            }

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    val doc = createPdfDocument(client, date, car, model, plate)
                    doc.writeTo(out)
                    doc.close()
                }

                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Toast.makeText(this, "PDF salvato in Download/LevabolliApp", Toast.LENGTH_LONG).show()
                showPdfActions(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Errore salvataggio PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Android 9 and below
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pendingPdfExport = true
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQ_WRITE_STORAGE
            )
            Toast.makeText(this, "Concedi il permesso per salvare il PDF in Download", Toast.LENGTH_LONG).show()
            return
        }

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val folder = java.io.File(downloadsDir, "LevabolliApp")
        if (!folder.exists()) folder.mkdirs()

        val file = java.io.File(folder, fileName)
        try {
            java.io.FileOutputStream(file).use { out ->
                val doc = createPdfDocument(client, date, car, model, plate)
                doc.writeTo(out)
                doc.close()
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            Toast.makeText(this, "PDF salvato in Download/LevabolliApp", Toast.LENGTH_LONG).show()
            showPdfActions(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore salvataggio PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createPdfDocument(
        client: String,
        date: String,
        car: String,
        model: String,
        plate: String
    ): PdfDocument {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
        }

        var y = 40f

        // HEADER
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(getString(R.string.pdf_title), 30f, y, paint)
        paint.isFakeBoldText = false
        y += 30f

        paint.textSize = 12f
        canvas.drawText(getString(R.string.pdf_client, if (client.isBlank()) "-" else client), 30f, y, paint); y += 18f
        canvas.drawText(getString(R.string.pdf_date, if (date.isBlank()) "-" else date), 30f, y, paint); y += 18f
        canvas.drawText(getString(R.string.pdf_car, if (car.isBlank()) "-" else car), 30f, y, paint); y += 18f
        canvas.drawText(getString(R.string.pdf_model, if (model.isBlank()) "-" else model), 30f, y, paint); y += 18f
        canvas.drawText(getString(R.string.pdf_plate, if (plate.isBlank()) "-" else plate), 30f, y, paint); y += 24f

        val totCons = panels.sumOf { it.prezzoConsigliato }
        val totApp = panels.sumOf { it.prezzoApplicato }
        val sconto = (totCons - totApp).coerceAtLeast(0)
        val scontoPerc = if (totCons > 0) (sconto.toDouble() / totCons.toDouble() * 100.0) else 0.0

        canvas.drawText(getString(R.string.total_suggested_value, totCons), 30f, y, paint); y += 18f
        canvas.drawText(getString(R.string.total_applied_value, totApp), 30f, y, paint); y += 18f
        canvas.drawText(
            getString(R.string.total_discount_value, sconto, String.format("%.1f", scontoPerc)),
            30f,
            y,
            paint
        ); y += 30f

        // LINE
        paint.strokeWidth = 2f
        canvas.drawLine(30f, y, pageWidth - 30f, y, paint)
        y += 20f

        // CAR SCHEMA
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Sagoma auto e pannelli", 30f, y, paint)
        paint.isFakeBoldText = false
        y += 30f

        drawCarSchema(canvas, paint, y)

        doc.finishPage(page)
        return doc
    }

    private fun drawCarSchema(canvas: Canvas, paint: Paint, startY: Float) {
        val panelPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = android.graphics.Color.BLACK
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.parseColor("#E0E0E0")
        }

        val rectWidth = 100f
        val rectHeight = 40f
        val centerX = 297f // middle of A4 width
        var y = startY

        // Helper function
        fun drawPanelRect(x: Float, yPos: Float, w: Float, h: Float, nome: String, cons: Int, app: Int) {
            canvas.drawRect(x, yPos, x + w, yPos + h, fillPaint)
            canvas.drawRect(x, yPos, x + w, yPos + h, panelPaint)
            canvas.drawText(nome, x + w / 2, yPos + h / 2 - 8, textPaint)
            val appText = if (app > 0) "€ $app" else "—"
            val consText = if (cons > 0) "(€ $cons)" else ""
            canvas.drawText(appText, x + w / 2, yPos + h / 2 + 4, textPaint)
            if (consText.isNotEmpty()) {
                val smallPaint = Paint(textPaint).apply { textSize = 7f; color = android.graphics.Color.GRAY }
                canvas.drawText(consText, x + w / 2, yPos + h / 2 + 12, smallPaint)
            }
        }

        // Cofano
        val cofano = panels.firstOrNull { it.nome == getString(R.string.panel_hood) }
        if (cofano != null) {
            drawPanelRect(centerX - rectWidth / 2, y, rectWidth, rectHeight, cofano.nome, cofano.prezzoConsigliato, cofano.prezzoApplicato)
        }
        y += rectHeight + 10f

        // Parafanghi ant + Tetto + Parafanghi ant
        val parafSx1 = panels.firstOrNull { it.nome == getString(R.string.panel_fender_fl) }
        val tetto = panels.firstOrNull { it.nome == getString(R.string.panel_roof) }
        val parafDx1 = panels.firstOrNull { it.nome == getString(R.string.panel_fender_fr) }

        val sideWidth = 80f
        val gapSide = 10f
        
        if (parafSx1 != null) {
            drawPanelRect(centerX - rectWidth / 2 - sideWidth - gapSide, y, sideWidth, rectHeight, parafSx1.nome, parafSx1.prezzoConsigliato, parafSx1.prezzoApplicato)
        }
        if (tetto != null) {
            drawPanelRect(centerX - rectWidth / 2, y, rectWidth, rectHeight, tetto.nome, tetto.prezzoConsigliato, tetto.prezzoApplicato)
        }
        if (parafDx1 != null) {
            drawPanelRect(centerX + rectWidth / 2 + gapSide, y, sideWidth, rectHeight, parafDx1.nome, parafDx1.prezzoConsigliato, parafDx1.prezzoApplicato)
        }
        y += rectHeight + 10f

        // Porte ant
        val portaSx1 = panels.firstOrNull { it.nome == getString(R.string.panel_door_fl) }
        val portaDx1 = panels.firstOrNull { it.nome == getString(R.string.panel_door_fr) }

        if (portaSx1 != null) {
            drawPanelRect(centerX - rectWidth / 2 - sideWidth - gapSide, y, sideWidth, rectHeight, portaSx1.nome, portaSx1.prezzoConsigliato, portaSx1.prezzoApplicato)
        }
        if (portaDx1 != null) {
            drawPanelRect(centerX + rectWidth / 2 + gapSide, y, sideWidth, rectHeight, portaDx1.nome, portaDx1.prezzoConsigliato, portaDx1.prezzoApplicato)
        }
        y += rectHeight + 10f

        // Porte post
        val portaSx2 = panels.firstOrNull { it.nome == getString(R.string.panel_door_rl) }
        val portaDx2 = panels.firstOrNull { it.nome == getString(R.string.panel_door_rr) }

        if (portaSx2 != null) {
            drawPanelRect(centerX - rectWidth / 2 - sideWidth - gapSide, y, sideWidth, rectHeight, portaSx2.nome, portaSx2.prezzoConsigliato, portaSx2.prezzoApplicato)
        }
        if (portaDx2 != null) {
            drawPanelRect(centerX + rectWidth / 2 + gapSide, y, sideWidth, rectHeight, portaDx2.nome, portaDx2.prezzoConsigliato, portaDx2.prezzoApplicato)
        }
        y += rectHeight + 10f

        // Parafanghi post + Baule + Parafanghi post
        val parafSx2 = panels.firstOrNull { it.nome == getString(R.string.panel_fender_rl) }
        val baule = panels.firstOrNull { it.nome == getString(R.string.panel_trunk) }
        val parafDx2 = panels.firstOrNull { it.nome == getString(R.string.panel_fender_rr) }

        if (parafSx2 != null) {
            drawPanelRect(centerX - rectWidth / 2 - sideWidth - gapSide, y, sideWidth, rectHeight, parafSx2.nome, parafSx2.prezzoConsigliato, parafSx2.prezzoApplicato)
        }
        if (baule != null) {
            drawPanelRect(centerX - rectWidth / 2, y, rectWidth, rectHeight, baule.nome, baule.prezzoConsigliato, baule.prezzoApplicato)
        }
        if (parafDx2 != null) {
            drawPanelRect(centerX + rectWidth / 2 + gapSide, y, sideWidth, rectHeight, parafDx2.nome, parafDx2.prezzoConsigliato, parafDx2.prezzoApplicato)
        }
        y += rectHeight + 10f

        // Montanti
        val montanteSx = panels.firstOrNull { it.nome == getString(R.string.panel_montante_sx) }
        val montanteDx = panels.firstOrNull { it.nome == getString(R.string.panel_montante_dx) }

        if (montanteSx != null && montanteSx.prezzoApplicato > 0) {
            drawPanelRect(centerX - rectWidth / 2 - sideWidth - gapSide, y, sideWidth, 30f, montanteSx.nome, montanteSx.prezzoConsigliato, montanteSx.prezzoApplicato)
        }
        if (montanteDx != null && montanteDx.prezzoApplicato > 0) {
            drawPanelRect(centerX + rectWidth / 2 + gapSide, y, sideWidth, 30f, montanteDx.nome, montanteDx.prezzoConsigliato, montanteDx.prezzoApplicato)
        }
    }

    private fun showPdfActions(uri: android.net.Uri) {
        AlertDialog.Builder(this)
            .setTitle("PDF pronto")
            .setMessage("PDF salvato in Download/LevabolliApp.\nCosa vuoi fare?")
            .setPositiveButton("Apri") { _, _ ->
                try {
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(openIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "Nessuna app per aprire PDF trovata", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Condividi") { _, _ ->
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(share, "Condividi PDF"))
            }
            .setNeutralButton("Chiudi", null)
            .show()
    }

    private fun openDownloads() {
        try {
            val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossibile aprire Download. Cerca il PDF in: Download/LevabolliApp", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_WRITE_STORAGE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted && pendingPdfExport) {
                pendingPdfExport = false
                exportPdf()
            } else {
                pendingPdfExport = false
                Toast.makeText(this, "Permesso negato: non posso salvare il PDF in Download", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        if (MainActivityHolder.instance === this) {
            MainActivityHolder.instance = null
        }
        super.onDestroy()
    }
}
