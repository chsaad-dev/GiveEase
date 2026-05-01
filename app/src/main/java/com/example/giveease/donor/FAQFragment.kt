package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentFaqBinding

class FAQFragment : Fragment() {
    private lateinit var binding: FragmentFaqBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFaqBinding.inflate(inflater, container, false)

        setupClickListeners()
        setupFAQItems()

        return binding.root
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnContactSupport.setOnClickListener {
                navigateToContactSupport()
            }

            etSearchFAQ.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    Toast.makeText(requireContext(), "Search functionality coming soon", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupFAQItems() {
        binding.apply {
            faqItem1.setOnClickListener {
                toggleAnswer(answer1, expandIcon1)
            }

            faqItem2.setOnClickListener {
                toggleAnswer(answer2, expandIcon2)
            }

            faqItem3.setOnClickListener {
                toggleAnswer(answer3, expandIcon3)
            }

            faqItem4.setOnClickListener {
                toggleAnswer(answer4, expandIcon4)
            }

            faqItem5.setOnClickListener {
                toggleAnswer(answer5, expandIcon5)
            }
        }
    }

    private fun toggleAnswer(answerView: android.widget.TextView, iconView: android.widget.ImageView) {
        if (answerView.visibility == View.GONE) {
            answerView.visibility = View.VISIBLE
            iconView.rotation = 180f
        } else {
            answerView.visibility = View.GONE
            iconView.rotation = 0f
        }
    }

    private fun navigateToContactSupport() {
        parentFragmentManager.beginTransaction()
            .hide(this)
            .add(R.id.fragment_container_donor, ContactSupportFragment())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance() = FAQFragment()
    }
}