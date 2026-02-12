package com.example.levabolliapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class ListinoActivity : AppCompatActivity() {

    private lateinit var edtListino: EditText
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listino)

        edtListino = findViewById(R.id.edtListino)
        txtStatus = findViewById(R.id.txtListinoStatus)
        val btnSalva: Button = findViewById(R.id.btnSalvaListino)
        val btnRipristina: Button = findViewById(R.id.btnRipristinaListino)

        // Non carichiamo qui il listino corrente in formato testo per semplicità,
        // l'utente può incollare/riscrivere il proprio.

        btnSalva.setOnClickListener {
            val text = edtListino.text.toString().trim()
            if (text.isEmpty()) {
                txtStatus.text = getString(R.string.custom_pricing_empty_error)
                return@setOnClickListener
            }

            val arr = JSONArray()
            val lines = text.split("\n".toRegex())
            try {
                for (line in lines) {
                    if (line.isBlank()) continue
                    val parts = line.split(";")
                    if (parts.size < 4) continue
                    val misura = parts[0].trim().toInt()
                    val min = parts[1].trim().toInt()
                    val max = parts[2].trim().toInt()
                    val prezzo = parts[3].trim().toInt()
                    val o = JSONObject()
                    o.put("misura", misura)
                    o.put("min", min)
                    o.put("max", max)
                    o.put("prezzo", prezzo)
                    arr.put(o)
                }
            } catch (e: Exception) {
                txtStatus.text = getString(R.string.custom_pricing_parse_error)
                return@setOnClickListener
            }

            if (arr.length() == 0) {
                txtStatus.text = getString(R.string.custom_pricing_empty_error)
                return@setOnClickListener
            }

            // Passiamo il JSON alla MainActivity (semplice accesso via cast)
            val main = MainActivityHolder.instance
            main?.saveCustomListinoFromActivity(arr.toString())
            txtStatus.text = getString(R.string.custom_pricing_saved)
        }

        btnRipristina.setOnClickListener {
            val main = MainActivityHolder.instance
            main?.saveCustomListinoFromActivity("") // cancella custom e torna al default
            txtStatus.text = getString(R.string.custom_pricing_reset)
        }
    }
}

// Semplice holder per avere un riferimento alla MainActivity
object MainActivityHolder {
    var instance: MainActivity? = null
}
