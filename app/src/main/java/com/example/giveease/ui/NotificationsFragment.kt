package com.example.giveease.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentNotificationsBinding
import com.example.giveease.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            notifications = emptyList(),
            onNotificationClick = { notification, position ->
                handleNotificationClick(notification, position)
            },
            onSelectionChanged = { count ->
                handleSelectionMode(count)
            }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            if (adapter.isSelectionMode) {
                adapter.clearSelections()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        binding.btnDeleteSelected.setOnClickListener {
            deleteSelectedItems()
        }
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
        binding.rvNotifications.visibility = View.GONE

        firestore.collection("users").document(uid).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                val notificationList = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.apply { id = doc.id }
                } ?: emptyList()

                if (notificationList.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE
                    adapter.updateData(notificationList)
                }
            }
    }

    private fun handleNotificationClick(notification: Notification, position: Int) {
        if (!notification.isRead) {
            val uid = auth.currentUser?.uid ?: return
            
            // Mark as read in Firestore
            firestore.collection("users").document(uid)
                .collection("notifications").document(notification.id)
                .update("isRead", true)
        }
        
        // Show details in dialog
        AlertDialog.Builder(requireContext())
            .setTitle(notification.title)
            .setMessage(notification.message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun handleSelectionMode(count: Int) {
        if (count > 0) {
            binding.tvToolbarTitle.text = "$count Selected"
            binding.btnDeleteSelected.visibility = View.VISIBLE
            binding.btnBack.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            binding.tvToolbarTitle.text = "Notifications"
            binding.btnDeleteSelected.visibility = View.GONE
            binding.btnBack.setImageResource(com.example.giveease.R.drawable.ic_arrow_back)
        }
    }

    private fun deleteSelectedItems() {
        val uid = auth.currentUser?.uid ?: return
        val selectedIds = adapter.selectedItems.toList()

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notifications")
            .setMessage("Are you sure you want to delete ${selectedIds.size} item(s)?")
            .setPositiveButton("Delete") { _, _ ->
                firestore.runBatch { batch ->
                    selectedIds.forEach { id ->
                        val ref = firestore.collection("users").document(uid)
                            .collection("notifications").document(id)
                        batch.delete(ref)
                    }
                }.addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                        adapter.clearSelections()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
