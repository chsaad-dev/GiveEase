package com.example.giveease.donor

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.giveease.databinding.FragmentDonorViewProofBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DonorViewProofFragment : Fragment() {

    private var _binding: FragmentDonorViewProofBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorViewProofBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        arguments?.let { bundle ->
            binding.tvBeneficiaryName.text = bundle.getString(ARG_NAME, "")
            binding.tvContactNumber.text = bundle.getString(ARG_CONTACT, "")
            
            val info = bundle.getString(ARG_INFO, "")
            if (info.isEmpty()) {
                binding.llAdditionalInfo.visibility = View.GONE
            } else {
                binding.tvAdditionalInfo.text = info
            }

            val uploadedAt = bundle.getLong(ARG_DATE, 0L)
            if (uploadedAt > 0L) {
                val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                binding.tvUploadedAt.text = "Uploaded on: ${sdf.format(Date(uploadedAt))}"
            } else {
                binding.tvUploadedAt.visibility = View.GONE
            }

            val handoverUrl = bundle.getString(ARG_HANDOVER_URL, "")
            val addressUrl = bundle.getString(ARG_ADDRESS_URL, "")

            if (handoverUrl.isNotEmpty()) {
                Glide.with(this).load(handoverUrl).into(binding.ivHandoverImage)
                binding.ivHandoverImage.setOnClickListener {
                    showFullScreenImage(handoverUrl)
                }
            }

            if (addressUrl.isNotEmpty()) {
                Glide.with(this).load(addressUrl).into(binding.ivAddressProofImage)
                binding.ivAddressProofImage.setOnClickListener {
                    showFullScreenImage(addressUrl)
                }
            }
        }
    }

    private fun showFullScreenImage(url: String) {
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        Glide.with(this).load(url).into(imageView)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(imageView)
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NAME = "arg_name"
        private const val ARG_CONTACT = "arg_contact"
        private const val ARG_INFO = "arg_info"
        private const val ARG_HANDOVER_URL = "arg_handover"
        private const val ARG_ADDRESS_URL = "arg_address"
        private const val ARG_DATE = "arg_date"

        @JvmStatic
        fun newInstance(
            name: String,
            contact: String,
            info: String,
            handoverUrl: String,
            addressUrl: String,
            uploadedAt: Long
        ) = DonorViewProofFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_NAME, name)
                putString(ARG_CONTACT, contact)
                putString(ARG_INFO, info)
                putString(ARG_HANDOVER_URL, handoverUrl)
                putString(ARG_ADDRESS_URL, addressUrl)
                putLong(ARG_DATE, uploadedAt)
            }
        }
    }
}
