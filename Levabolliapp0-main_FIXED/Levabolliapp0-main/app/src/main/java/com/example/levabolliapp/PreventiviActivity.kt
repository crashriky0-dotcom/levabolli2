package com.example.levabolliapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class PreventiviActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var txtEmpty: TextView

    data class QuoteItem(
        val id: Long,
        val tecnico: String,
        val data: String,
        val marca: String,
        val modello: String,
        val targa: String,
        val totaleApplicato: Int
    )

    private fun loadQuotes(): List<QuoteItem> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREVENTIVI, null) ?: return emptyList()
        val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }

        val list = mutableListOf<QuoteItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                QuoteItem(
                    id = o.optLong("id", 0L),
                    tecnico = o.optString("tecnico", ""),
                    data = o.optString("data", ""),
                    marca = o.optString("marca", ""),
                    modello = o.optString("modello", ""),
                    targa = o.optString("targa", ""),
                    totaleApplicato = o.optInt("totApp", 0)
                )
            )
        }
        return list.sortedByDescending { it.id }
    }

    private fun saveQuotes(list: List<JSONObject>) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_PREVENTIVI, arr.toString()).apply()
    }

    private fun deleteQuoteById(id: Long) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREVENTIVI, null) ?: return
        val arr = try { JSONArray(json) } catch (_: Exception) { return }
        val keep = mutableListOf<JSONObject>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optLong("id", -1L) != id) keep.add(o)
        }
        saveQuotes(keep)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivi)

        listView = findViewById(R.id.listPreventivi)
        txtEmpty = findViewById(R.id.txtPreventiviEmpty)

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val preventivi = loadQuotes()

        if (preventivi.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            listView.adapter = null
            return
        }
        txtEmpty.visibility = View.GONE

        val items = preventivi.map {
            val titolo = if (it.marca.isNotEmpty() || it.modello.isNotEmpty()) {
                "${it.marca} ${it.modello}".trim()
            } else {
                getString(R.string.quote_no_car)
            }
            val sotto = "${it.data} - ${getString(R.string.total_applied_value, it.totaleApplicato)}"
            "$titolo\n$sotto"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val p = preventivi[position]
            val data = Intent()
            data.putExtra("preventivo_id", p.id)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val p = preventivi[position]
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_quote_title)
                .setMessage(R.string.delete_quote_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    deleteQuoteById(p.id)
                    refreshList()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }
    }

    companion object {
        private const val PREFS_NAME = "levabolli_prefs"
        private const val KEY_PREVENTIVI = "preventivi"
    }
}
