package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentAdminLogsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminLogsFragment : Fragment() {

    private var _binding: FragmentAdminLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: AdminLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminLogsBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadLogs()
    }

    private fun setupRecyclerView() {
        adapter = AdminLogAdapter()
        binding.recyclerViewLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLogs.adapter = adapter
        
        // Add divider
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewLogs.context,
            (binding.recyclerViewLogs.layoutManager as LinearLayoutManager).orientation
        )
        binding.recyclerViewLogs.addItemDecoration(dividerItemDecoration)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadLogs() {
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        firestore.collection("admin_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Show last 100 logs
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                
                val logs = documents.map { doc ->
                    AdminLog(
                        id = doc.id,
                        actionType = doc.getString("actionType") ?: "",
                        actionTitle = doc.getString("actionTitle") ?: "System Event",
                        actionDetail = doc.getString("actionDetail") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        adminId = doc.getString("adminId") ?: ""
                    )
                }

                if (logs.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.recyclerViewLogs.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.recyclerViewLogs.visibility = View.VISIBLE
                    adapter.submitList(logs)
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading logs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
