package com.example.giveease.ngo

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoBankAccountsBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class BankAccount(
    val bankName: String = "",
    val accountTitle: String = "",
    val accountNumber: String = ""
)

class NgoBankAccountsFragment : Fragment() {

    private var _binding: FragmentNgoBankAccountsBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val bankAccountsList = mutableListOf<BankAccount>()
    private lateinit var adapter: BankAccountsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoBankAccountsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupClickListeners()
        loadBankAccounts()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = BankAccountsAdapter(
            bankAccountsList,
            onDeleteClick = { position -> showDeleteConfirmation(position) },
            onEditClick = { position -> showEditAccountDialog(position) }
        )
        binding.recyclerViewAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAccounts.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
    }

    private fun loadBankAccounts() {
        val uid = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                bankAccountsList.clear()

                // Try new array format first
                val accounts = document.get("bankAccounts") as? List<Map<String, String>>
                if (accounts != null && accounts.isNotEmpty()) {
                    accounts.forEach { map ->
                        bankAccountsList.add(
                            BankAccount(
                                bankName = map["bankName"] ?: "",
                                accountTitle = map["accountTitle"] ?: "",
                                accountNumber = map["accountNumber"] ?: ""
                            )
                        )
                    }
                } else {
                    // Migrate legacy single bankDetails
                    val legacy = document.get("bankDetails") as? Map<String, String>
                    if (legacy != null) {
                        val name = legacy["bankName"] ?: ""
                        val title = legacy["accountTitle"] ?: ""
                        val number = legacy["accountNumber"] ?: ""
                        if (name.isNotEmpty() || title.isNotEmpty() || number.isNotEmpty()) {
                            bankAccountsList.add(BankAccount(name, title, number))
                            saveBankAccountsToFirestore()
                        }
                    }
                }

                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load accounts", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
    }

    private fun updateEmptyState() {
        if (!isAdded || _binding == null) return

        if (bankAccountsList.isEmpty()) {
            binding.recyclerViewAccounts.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewAccounts.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun showAddAccountDialog() {
        if (!isAdded) return
        showAccountDialog(title = "Add Bank Account", position = -1, existingAccount = null)
    }

    private fun showEditAccountDialog(position: Int) {
        if (!isAdded || position < 0 || position >= bankAccountsList.size) return
        showAccountDialog(
            title = "Edit Bank Account",
            position = position,
            existingAccount = bankAccountsList[position]
        )
    }

    private fun showAccountDialog(title: String, position: Int, existingAccount: BankAccount?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_bank_account, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etBankName = dialogView.findViewById<TextInputEditText>(R.id.etBankName)
        val etAccountTitle = dialogView.findViewById<TextInputEditText>(R.id.etAccountTitle)
        val etAccountNumber = dialogView.findViewById<TextInputEditText>(R.id.etAccountNumber)

        tvTitle.text = title

        // Pre-fill for edit
        if (existingAccount != null) {
            etBankName.setText(existingAccount.bankName)
            etAccountTitle.setText(existingAccount.accountTitle)
            etAccountNumber.setText(existingAccount.accountNumber)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btnCancelDialog).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnSaveAccount).setOnClickListener {
            val bankName = etBankName.text.toString().trim()
            val accountTitle = etAccountTitle.text.toString().trim()
            val accountNumber = etAccountNumber.text.toString().trim()

            when {
                bankName.isEmpty() -> {
                    etBankName.error = "Bank name is required"
                    return@setOnClickListener
                }
                accountTitle.isEmpty() -> {
                    etAccountTitle.error = "Account title is required"
                    return@setOnClickListener
                }
                accountNumber.isEmpty() -> {
                    etAccountNumber.error = "Account number / IBAN is required"
                    return@setOnClickListener
                }
            }

            val newAccount = BankAccount(bankName, accountTitle, accountNumber)

            if (position >= 0) {
                // Edit existing
                bankAccountsList[position] = newAccount
                adapter.notifyItemChanged(position)
            } else {
                // Add new
                bankAccountsList.add(newAccount)
                adapter.notifyItemInserted(bankAccountsList.size - 1)
            }

            updateEmptyState()
            saveBankAccountsToFirestore()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(position: Int) {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Remove Account")
            .setMessage("Are you sure you want to remove \"${bankAccountsList[position].bankName}\" account?")
            .setPositiveButton("Remove") { _, _ ->
                bankAccountsList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, bankAccountsList.size)
                updateEmptyState()
                saveBankAccountsToFirestore()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBankAccountsToFirestore() {
        val uid = auth.currentUser?.uid ?: return

        val accountsData = bankAccountsList.map { account ->
            hashMapOf(
                "bankName" to account.bankName,
                "accountTitle" to account.accountTitle,
                "accountNumber" to account.accountNumber
            )
        }

        firestore.collection("users").document(uid)
            .update("bankAccounts", accountsData)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Bank accounts updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to save accounts", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── Adapter ───────────────────────────────────────────────

class BankAccountsAdapter(
    private val accounts: List<BankAccount>,
    private val onDeleteClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit
) : RecyclerView.Adapter<BankAccountsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBankName: android.widget.TextView = itemView.findViewById(R.id.tvBankName)
        val tvAccountTitle: android.widget.TextView = itemView.findViewById(R.id.tvAccountTitle)
        val tvAccountNumber: android.widget.TextView = itemView.findViewById(R.id.tvAccountNumber)
        val btnDelete: android.widget.ImageView = itemView.findViewById(R.id.btnDeleteAccount)
        val btnEdit: android.widget.ImageView = itemView.findViewById(R.id.btnEditAccount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bank_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        holder.tvBankName.text = account.bankName
        holder.tvAccountTitle.text = account.accountTitle
        holder.tvAccountNumber.text = account.accountNumber
        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
        holder.btnEdit.setOnClickListener { onEditClick(position) }
    }

    override fun getItemCount() = accounts.size
}
