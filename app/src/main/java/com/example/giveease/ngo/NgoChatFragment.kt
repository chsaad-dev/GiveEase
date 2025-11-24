package com.example.giveease.ngo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentNgoChatBinding
import com.example.giveease.ngo.model.ChatItem

class NgoChatFragment : Fragment() {

    private var _binding: FragmentNgoChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: NgoChatAdapter
    private var chatList = mutableListOf<ChatItem>()
    private var filteredChatList = mutableListOf<ChatItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupClickListeners()
        loadChats()
    }

    private fun setupRecyclerView() {
        chatAdapter = NgoChatAdapter { chatItem ->
            onChatItemClick(chatItem)
        }

        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterChats(s.toString())
            }
        })
    }

    private fun setupClickListeners() {
        binding.fabNewChat.setOnClickListener {
            // Navigate to donor selection or new chat screen
            // findNavController().navigate(R.id.action_ngoChat_to_selectDonor)
        }
    }

    private fun loadChats() {
        // Simulate loading chats (replace with API call)
        chatList = getSampleChats().toMutableList()
        filteredChatList = chatList.toMutableList()

        updateUI()
    }

    private fun filterChats(query: String) {
        filteredChatList = if (query.isEmpty()) {
            chatList.toMutableList()
        } else {
            chatList.filter {
                it.donorName.contains(query, ignoreCase = true) ||
                        it.lastMessage.contains(query, ignoreCase = true) ||
                        it.campaignName.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        updateUI()
    }

    private fun updateUI() {
        if (filteredChatList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewChats.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewChats.visibility = View.VISIBLE
            chatAdapter.submitList(filteredChatList)
        }
    }

    private fun onChatItemClick(chatItem: ChatItem) {
        // Navigate to chat detail
        val bundle = Bundle().apply {
            putString("donorId", chatItem.donorId)
            putString("donorName", chatItem.donorName)
            putString("campaignName", chatItem.campaignName)
            putString("donorProfileUrl", chatItem.donorProfileUrl)
        }

        // findNavController().navigate(R.id.action_ngoChat_to_chatDetail, bundle)
    }

    private fun getSampleChats(): List<ChatItem> {
        return listOf(
            ChatItem(
                id = "1",
                donorId = "d1",
                donorName = "Ahmed Khan",
                donorProfileUrl = null,
                lastMessage = "Thank you for your donation! When can we schedule pickup?",
                timestamp = "2:30 PM",
                campaignName = "Flood Relief Campaign",
                unreadCount = 3,
                isOnline = true
            ),
            ChatItem(
                id = "2",
                donorId = "d2",
                donorName = "Sara Ali",
                donorProfileUrl = null,
                lastMessage = "I'd like to donate food items",
                timestamp = "1:15 PM",
                campaignName = "Food Drive",
                unreadCount = 0,
                isOnline = false
            ),
            ChatItem(
                id = "3",
                donorId = "d3",
                donorName = "Ali Hassan",
                donorProfileUrl = null,
                lastMessage = "Can you provide more details about the project?",
                timestamp = "Yesterday",
                campaignName = "Education Support",
                unreadCount = 1,
                isOnline = true
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
