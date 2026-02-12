package com.example.levabolliapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PanelAdapter(
    private val panels: List<MainActivity.PanelData>,
    private val onPanelEdit: (MainActivity.PanelData) -> Unit
) : RecyclerView.Adapter<PanelAdapter.PanelViewHolder>() {

    class PanelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chkSelected: CheckBox = view.findViewById(R.id.chkPanelSelected)
        val txtName: TextView = view.findViewById(R.id.txtPanelName)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditPanel)
        val layoutPrezzi: View = view.findViewById(R.id.layoutPrezzi)
        val txtConsigliato: TextView = view.findViewById(R.id.txtPrezzoConsigliato)
        val txtApplicato: TextView = view.findViewById(R.id.txtPrezzoApplicato)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_panel_card, parent, false)
        return PanelViewHolder(view)
    }

    override fun onBindViewHolder(holder: PanelViewHolder, position: Int) {
        val panel = panels[position]
        
        holder.txtName.text = panel.nome
        
        val hasData = panel.prezzoApplicato > 0 || panel.prezzoConsigliato > 0
        holder.chkSelected.isChecked = hasData
        
        if (hasData) {
            holder.layoutPrezzi.visibility = View.VISIBLE
            holder.txtConsigliato.text = "Consigliato: € ${panel.prezzoConsigliato}"
            holder.txtApplicato.text = "Applicato: € ${panel.prezzoApplicato}"
        } else {
            holder.layoutPrezzi.visibility = View.GONE
        }
        
        holder.chkSelected.setOnClickListener {
            if (!holder.chkSelected.isChecked && hasData) {
                // Reset data if unchecked
                panel.numBolli = 0
                panel.diametroMm = 0.0
                panel.alluminio = false
                panel.numSportellate = 0
                panel.diametroSportellataMm = 0.0
                panel.prezzoConsigliato = 0
                panel.prezzoApplicato = 0
                notifyItemChanged(position)
                onPanelEdit(panel) // trigger update
            } else if (holder.chkSelected.isChecked && !hasData) {
                // Open dialog to enter data
                onPanelEdit(panel)
            }
        }
        
        holder.btnEdit.setOnClickListener {
            onPanelEdit(panel)
        }
        
        holder.itemView.setOnClickListener {
            onPanelEdit(panel)
        }
    }

    override fun getItemCount() = panels.size
}
