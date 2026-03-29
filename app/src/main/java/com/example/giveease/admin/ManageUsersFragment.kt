package com.example.giveease.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentManageUsersBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore

class ManageUsersFragment : Fragment() {

    private var _binding: FragmentManageUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: ManageUsersAdapter
    private var allUsers = mutableListOf<AdminUser>()
    private var currentFilter = "All"
    private var searchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageUsersBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChipFilters()
        setupSearch()
        setupClickListeners()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = ManageUsersAdapter { user ->
            showUserActionDialog(user)
        }
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUsers.adapter = adapter
    }

    private fun setupChipFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(binding.chipAll.id) -> "All"
                checkedIds.contains(binding.chipDonors.id) -> "Donors"
                checkedIds.contains(binding.chipNgos.id) -> "NGOs"
                checkedIds.contains(binding.chipVerified.id) -> "Verified"
                checkedIds.contains(binding.chipPending.id) -> "Pending"
                checkedIds.contains(binding.chipRejected.id) -> "Rejected"
                else -> "All"
            }
            applyFilter()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim().lowercase()
                applyFilter()
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadUsers() {
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        firestore.collection("users")
            .whereIn("role", listOf("donor", "ngo"))
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                allUsers.clear()

                for (doc in documents) {
                    val user = AdminUser(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "",
                        verificationStatus = doc.getString("verificationStatus") ?: "pending",
                        phone = doc.getString("phone") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                    allUsers.add(user)
                }

                allUsers.sortByDescending { it.createdAt }
                applyFilter()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilter() {
        var filtered = allUsers.toList()

        // Apply role/status filter
        filtered = when (currentFilter) {
            "Donors" -> filtered.filter { it.role == "donor" }
            "NGOs" -> filtered.filter { it.role == "ngo" }
            "Verified" -> filtered.filter { it.verificationStatus == "verified" }
            "Pending" -> filtered.filter { it.verificationStatus == "pending" }
            "Rejected" -> filtered.filter { it.verificationStatus == "rejected" }
            else -> filtered
        }

        // Apply search
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.lowercase().contains(searchQuery) ||
                it.email.lowercase().contains(searchQuery)
            }
        }

        binding.tvUserCount.text = "${filtered.size} users"

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewUsers.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewUsers.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    private fun showUserActionDialog(user: AdminUser) {
        if (!isAdded) return

        val roleDisplay = if (user.role == "ngo") "NGO" else "Donor"
        val statusDisplay = user.verificationStatus.replaceFirstChar { it.uppercase() }

        val details = """
            Name: ${user.name.ifEmpty { "Not set" }}
            Email: ${user.email}
            Role: $roleDisplay
            Phone: ${user.phone.ifEmpty { "Not set" }}
            Status: $statusDisplay
        """.trimIndent()

        val options = mutableListOf<String>()

        when (user.verificationStatus) {
            "pending" -> {
                options.add("Approve User")
                options.add("Reject User")
            }
            "verified" -> {
                options.add("Revoke Verification")
            }
            "rejected" -> {
                options.add("Approve User")
            }
        }
        options.add("Close")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("User Details")
            .setMessage(details)
            .setItems(options.toTypedArray()) { _, which ->
                val action = options[which]
                when (action) {
                    "Approve User" -> approveUser(user)
                    "Reject User" -> rejectUser(user)
                    "Revoke Verification" -> revokeUser(user)
                }
            }
            .show()
    }

    private fun approveUser(user: AdminUser) {
        val updates = hashMapOf<String, Any>(
            "verificationStatus" to "verified",
            "verifiedAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(user.userId)
            .update(updates)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "${user.name} approved", Toast.LENGTH_SHORT).show()
                AdminLogger.logAction("approve_user", "Approve User", "Admin approved ${user.name} (${user.role})")
                loadUsers()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectUser(user: AdminUser) {
        val updates = hashMapOf<String, Any>(
            "verificationStatus" to "rejected",
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(user.userId)
            .update(updates)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "${user.name} rejected", Toast.LENGTH_SHORT).show()
                AdminLogger.logAction("reject_user", "Reject User", "Admin rejected ${user.name} (${user.role})")
                loadUsers()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun revokeUser(user: AdminUser) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Revoke Verification?")
            .setMessage("This will revoke verification for ${user.name}. They will need to be re-verified.")
            .setPositiveButton("Revoke") { _, _ ->
                val updates = hashMapOf<String, Any>(
                    "verificationStatus" to "pending",
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("users").document(user.userId)
                    .update(updates)
                    .addOnSuccessListener {
                        if (!isAdded || _binding == null) return@addOnSuccessListener
                        Toast.makeText(requireContext(), "Verification revoked", Toast.LENGTH_SHORT).show()
                        AdminLogger.logAction("revoke_user", "Revoke Verification", "Admin revoked verification for ${user.name} (${user.role})")
                        loadUsers()
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
