package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorChatBinding
import com.example.giveease.donor.adapter.ChatAdapter
import com.example.giveease.donor.model.Chat

class DonorChatFragment : Fragment() {
    private lateinit var binding: FragmentDonorChatBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorChatBinding.inflate(inflater, container, false)

        val chatList = listOf(
            Chat("Edhi Foundation", "Thank you for your support!", R.drawable.sample_ngo, 2),
            Chat("Saylani Welfare", "Your donation was received.", R.drawable.sample_ngo, 0)
        )

        binding.recyclerViewChats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChats.adapter = ChatAdapter(chatList)

        return binding.root
    }
}

