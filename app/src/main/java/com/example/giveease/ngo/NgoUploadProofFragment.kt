package com.example.giveease.ngo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentNgoUploadProofBinding
import com.example.giveease.models.DonationProof
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class NgoUploadProofFragment : Fragment() {

    private var _binding: FragmentNgoUploadProofBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var donationId: String? = null
    private var campaignTitle: String? = null

    private var handoverImageBitmap: Bitmap? = null
    private var addressProofImageBitmap: Bitmap? = null

    private var isSelectingHandover = true

    // ActivityResultLauncher for picking images from camera/gallery
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                if (isSelectingHandover) {
                    handoverImageBitmap = imageBitmap
                    binding.ivHandoverImage.setImageBitmap(imageBitmap)
                    binding.ivHandoverImage.visibility = View.VISIBLE
                    binding.llHandoverPlaceholder.visibility = View.GONE
                } else {
                    addressProofImageBitmap = imageBitmap
                    binding.ivAddressProofImage.setImageBitmap(imageBitmap)
                    binding.ivAddressProofImage.visibility = View.VISIBLE
                    binding.llAddressPlaceholder.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            donationId = it.getString(ARG_DONATION_ID)
            campaignTitle = it.getString(ARG_CAMPAIGN_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoUploadProofBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCampaignContext.text = "Donation for Campaign: $campaignTitle"

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cardHandoverImage.setOnClickListener {
            isSelectingHandover = true
            openCamera()
        }

        binding.cardAddressProofImage.setOnClickListener {
            isSelectingHandover = false
            openCamera()
        }

        binding.btnSubmitProof.setOnClickListener {
            submitProof()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        // Heavily compress image to save Firebase Storage space
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos)
        return baos.toByteArray()
    }

    private fun submitProof() {
        val name = binding.etBeneficiaryName.text.toString().trim()
        val contact = binding.etContactNumber.text.toString().trim()
        val additionalInfo = binding.etAdditionalInfo.text.toString().trim()

        if (name.isEmpty() || contact.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (handoverImageBitmap == null || addressProofImageBitmap == null) {
            Toast.makeText(requireContext(), "Please capture both required photos", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loadingOverlay.visibility = View.VISIBLE
        binding.btnSubmitProof.isEnabled = false

        // Upload Handover Image
        val handoverRef = storage.reference.child("donation_proofs/$donationId/handover_${UUID.randomUUID()}.jpg")
        val addressRef = storage.reference.child("donation_proofs/$donationId/address_${UUID.randomUUID()}.jpg")

        val handoverData = compressBitmap(handoverImageBitmap!!)
        val addressData = compressBitmap(addressProofImageBitmap!!)

        handoverRef.putBytes(handoverData).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            handoverRef.downloadUrl
        }.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            val handoverUrl = task.result.toString()
            
            // Start address upload
            addressRef.putBytes(addressData).continueWithTask { innerTask ->
                if (!innerTask.isSuccessful) {
                    innerTask.exception?.let { throw it }
                }
                addressRef.downloadUrl
            }.addOnSuccessListener { uri ->
                val addressUrl = uri.toString()
                saveToFirestore(name, contact, additionalInfo, handoverUrl, addressUrl)
            }
        }.addOnFailureListener { e ->
            binding.loadingOverlay.visibility = View.GONE
            binding.btnSubmitProof.isEnabled = true
            Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveToFirestore(name: String, contact: String, additionalInfo: String, handoverUrl: String, addressUrl: String) {
        val proof = DonationProof(
            beneficiaryName = name,
            contactNumber = contact,
            cnicOrAdditionalInfo = additionalInfo,
            addressProofImageUrl = addressUrl,
            handoverImageUrl = handoverUrl,
            uploadedAt = System.currentTimeMillis()
        )

        firestore.collection("donations").document(donationId!!)
            .update(
                mapOf(
                    "status" to "Delivered",
                    "proof" to proof
                )
            )
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                binding.loadingOverlay.visibility = View.GONE
                Toast.makeText(requireContext(), "Proof submitted successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.loadingOverlay.visibility = View.GONE
                binding.btnSubmitProof.isEnabled = true
                Toast.makeText(requireContext(), "Database update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DONATION_ID = "donation_id"
        private const val ARG_CAMPAIGN_TITLE = "campaign_title"

        @JvmStatic
        fun newInstance(donationId: String, campaignTitle: String) =
            NgoUploadProofFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DONATION_ID, donationId)
                    putString(ARG_CAMPAIGN_TITLE, campaignTitle)
                }
            }
    }
}
