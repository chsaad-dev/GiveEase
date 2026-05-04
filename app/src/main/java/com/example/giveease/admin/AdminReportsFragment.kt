package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.FragmentAdminReportsBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminReportsFragment : Fragment() {

    private var _binding: FragmentAdminReportsBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: ReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminReportsBinding.inflate(inflater, container, false)
        
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        setupRecyclerView()
        loadReports()
        
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ReportAdapter(
            onSuspend = { report -> suspendCampaign(report) },
            onDismiss = { report -> dismissReport(report) }
        )
        binding.recyclerViewReports.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewReports.adapter = adapter
    }

    private fun loadReports() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
        binding.recyclerViewReports.visibility = View.GONE

        firestore.collection("reports")
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                
                if (documents.isEmpty) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    val reportsList = mutableListOf<ReportData>()
                    for (doc in documents) {
                        val report = doc.toObject(ReportData::class.java).copy(id = doc.id)
                        reportsList.add(report)
                    }
                    reportsList.sortByDescending { it.timestamp }
                    adapter.submitList(reportsList)
                    binding.recyclerViewReports.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load reports", Toast.LENGTH_SHORT).show()
            }
    }

    private fun suspendCampaign(report: ReportData) {
        // Suspend the campaign and resolve the report
        val batch = firestore.batch()
        
        val campaignRef = firestore.collection("campaigns").document(report.campaignId)
        batch.update(campaignRef, "status", "Suspended")
        
        val reportRef = firestore.collection("reports").document(report.id)
        batch.update(reportRef, "status", "Resolved")
        
        batch.commit().addOnSuccessListener {
            if (!isAdded || _binding == null) return@addOnSuccessListener
            Toast.makeText(requireContext(), "Campaign suspended", Toast.LENGTH_SHORT).show()
            loadReports()
        }.addOnFailureListener {
            if (!isAdded || _binding == null) return@addOnFailureListener
            Toast.makeText(requireContext(), "Error suspending campaign", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dismissReport(report: ReportData) {
        firestore.collection("reports").document(report.id)
            .update("status", "Dismissed")
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Report dismissed", Toast.LENGTH_SHORT).show()
                loadReports()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Error dismissing report", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ReportData(
        val id: String = "",
        val campaignId: String = "",
        val campaignTitle: String = "",
        val ngoId: String = "",
        val reporterId: String = "",
        val reason: String = "",
        val status: String = "Pending",
        val timestamp: Long = 0L
    )

    class ReportAdapter(
        private val onSuspend: (ReportData) -> Unit,
        private val onDismiss: (ReportData) -> Unit
    ) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

        private var reports = listOf<ReportData>()

        fun submitList(list: List<ReportData>) {
            reports = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_report, parent, false)
            return ReportViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
            val report = reports[position]
            holder.bind(report, onSuspend, onDismiss)
        }

        override fun getItemCount() = reports.size

        class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvTitle: TextView = view.findViewById(R.id.tvCampaignTitle)
            private val tvReason: TextView = view.findViewById(R.id.tvReason)
            private val tvDate: TextView = view.findViewById(R.id.tvDate)
            private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            private val btnSuspend: Button = view.findViewById(R.id.btnSuspend)
            private val btnDismiss: Button = view.findViewById(R.id.btnDismiss)

            fun bind(report: ReportData, onSuspend: (ReportData) -> Unit, onDismiss: (ReportData) -> Unit) {
                tvTitle.text = report.campaignTitle
                tvReason.text = "Reason: ${report.reason}"
                tvStatus.text = report.status
                
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                tvDate.text = "Reported on: ${sdf.format(Date(report.timestamp))}"
                
                btnSuspend.setOnClickListener { onSuspend(report) }
                btnDismiss.setOnClickListener { onDismiss(report) }
            }
        }
    }
}
