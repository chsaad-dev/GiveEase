package com.example.giveease.donor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.donor.model.Campaign

class CampaignAdapter(private val campaigns: List<Campaign>) :
    RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder>() {

    inner class CampaignViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvCampaignTitle)
        val desc: TextView = view.findViewById(R.id.tvCampaignDesc)
        val image: ImageView = view.findViewById(R.id.imgCampaign)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val donateButton: Button = view.findViewById(R.id.btnDonate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_compaign, parent, false)
        return CampaignViewHolder(view)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        val campaign = campaigns[position]
        holder.title.text = campaign.title
        holder.desc.text = campaign.description
        holder.image.setImageResource(campaign.imageRes)
        holder.progressBar.progress = campaign.progress
        holder.donateButton.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Donate clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = campaigns.size
}
