package com.example.giveease.donor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentContactSupportBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactSupportFragment : Fragment() {

    private var _binding: FragmentContactSupportBinding? = null
    private val binding get() = _binding!!

    private val issueTypes = arrayOf(
        "Payment Issues",
        "Donation Problems",
        "Account Access",
        "Technical Issues",
        "NGO Verification",
        "App Feedback",
        "Other"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupClickListeners()
        setupIssueTypeDropdown()
    }

    private fun setupViews() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            issueTypes
        )
        binding.spinnerIssueType.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cardCallSupport.setOnClickListener {
            makePhoneCall()
        }

        binding.cardEmailSupport.setOnClickListener {
            sendEmail()
        }

        binding.btnSendMessage.setOnClickListener {
            sendSupportMessage()
        }

        binding.btnViewFAQ.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .hide(this)
                .add((requireView().parent as android.view.ViewGroup).id, FAQFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupIssueTypeDropdown() {
        binding.spinnerIssueType.setOnItemClickListener { _, _, position, _ ->
            binding.spinnerIssueType.setText(issueTypes[position], false)
        }
    }

    private fun makePhoneCall() {
        val phoneNumber = "+923111223381"
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Unable to make phone call")
        }
    }

    private fun sendEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:giveeaseapp@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "GiveEase Support Request")
            putExtra(Intent.EXTRA_TEXT, "Hi GiveEase Support Team,\n\nI need assistance with:\n\n")
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        } catch (e: Exception) {
            showToast("No email app found")
        }
    }

    private fun sendSupportMessage() {
        val issueType = binding.spinnerIssueType.text.toString().trim()
        val subject = binding.etSubject.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()

        if (!validateInputs(issueType, subject, message)) {
            return
        }

        binding.btnSendMessage.isEnabled = false
        binding.btnSendMessage.text = "Sending..."

        saveTicketToFirestore(issueType, subject, message)
    }

    private fun validateInputs(issueType: String, subject: String, message: String): Boolean {
        var isValid = true

        if (issueType.isEmpty()) {
            showToast("Please select an issue type")
            isValid = false
        }

        if (subject.isEmpty()) {
            binding.etSubject.error = "Subject is required"
            isValid = false
        } else if (subject.length < 4) {
            binding.etSubject.error = "Subject must be at least 4 characters"
            isValid = false
        } else {
            binding.etSubject.error = null
        }

        if (message.isEmpty()) {
            binding.etMessage.error = "Message is required"
            isValid = false
        } else if (message.length < 10) {
            binding.etMessage.error = "Message must be at least 10 characters"
            isValid = false
        } else {
            binding.etMessage.error = null
        }

        return isValid
    }

    private fun saveTicketToFirestore(issueType: String, subject: String, message: String) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: "anonymous"
        val userEmail = auth.currentUser?.email ?: "Unknown"
        val userName = auth.currentUser?.displayName ?: "Unknown"
        
        val ticketData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "issueType" to issueType,
            "subject" to subject,
            "message" to message,
            "status" to "Open",
            "timestamp" to System.currentTimeMillis()
        )
        
        firestore.collection("support_tickets")
            .add(ticketData)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                binding.btnSendMessage.isEnabled = true
                binding.btnSendMessage.text = "Send Message"
                showToast("Message sent successfully! We'll get back to you soon.")
                clearForm()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.btnSendMessage.isEnabled = true
                binding.btnSendMessage.text = "Send Message"
                showToast("Failed to send message. Please try again.")
            }
    }

    private fun clearForm() {
        binding.spinnerIssueType.setText("", false)
        binding.etSubject.setText("")
        binding.etMessage.setText("")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class SupportMessageRequest(
    val issueType: String,
    val subject: String,
    val message: String,
    val userEmail: String,
    val userId: String
)

data class SupportMessageResponse(
    val success: Boolean,
    val message: String,
    val ticketId: String?
)