package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentChatDetailBinding
import com.example.giveease.ngo.model.Message
import com.example.giveease.ngo.model.MessageType
import java.util.*

class ChatDetailFragment : Fragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private var donorId: String? = null
    private var donorName: String? = null
    private var campaignName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getArgumentsData()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
    }

    private fun getArgumentsData() {
        arguments?.let {
            donorId = it.getString("donorId")
            donorName = it.getString("donorName")
            campaignName = it.getString("campaignName")
        }
    }

    private fun setupToolbar() {
        // binding.tvNgoName.text = donorName ?: "Donor"
        // binding.tvNgoStatus.text = "Online"

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnMore.setOnClickListener {
            showOptionsMenu()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabSend.setOnClickListener {
            sendMessage()
        }

        binding.btnAttach.setOnClickListener {
            // Open image picker
            // attachImage()
        }

        binding.etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollToBottom()
            }
        }
    }

    private fun loadMessages() {
        messageList.addAll(getSampleMessages())
        messageAdapter.submitList(messageList.toList())
        scrollToBottom()
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()

        if (messageText.isEmpty()) return

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "ngo_1",
            receiverId = donorId ?: "",
            content = messageText,
            timestamp = System.currentTimeMillis(),
            isSent = true,
            isDelivered = false,
            isRead = false,
            messageType = MessageType.TEXT
        )

        messageList.add(message)
        messageAdapter.submitList(messageList.toList())

        binding.etMessage.text?.clear()
        scrollToBottom()
    }

    private fun scrollToBottom() {
        binding.recyclerViewMessages.postDelayed({
            if (messageList.isNotEmpty()) {
                binding.recyclerViewMessages.smoothScrollToPosition(messageList.size - 1)
            }
        }, 100)
    }

    private fun showOptionsMenu() {
        // Show popup menu
    }

    private fun getSampleMessages(): List<Message> {
        val currentTime = System.currentTimeMillis()

        return listOf(
            Message(
                id = "1",
                senderId = donorId ?: "d1",
                receiverId = "ngo_1",
                content = "Hi, I'm interested in donating to your flood relief campaign",
                timestamp = currentTime - 3600000,
                isSent = true,
                isDelivered = true,
                isRead = true,
                messageType = MessageType.TEXT
            ),
            Message(
                id = "2",
                senderId = "ngo_1",
                receiverId = donorId ?: "d1",
                content = "Thank you so much! We really appreciate your support. What would you like to donate?",
                timestamp = currentTime - 3000000,
                isSent = true,
                isDelivered = true,
                isRead = true,
                messageType = MessageType.TEXT
            ),
            Message(
                id = "3",
                senderId = donorId ?: "d1",
                receiverId = "ngo_1",
                content = "I have blankets and food supplies. When can we arrange pickup?",
                timestamp = currentTime - 1800000,
                isSent = true,
                isDelivered = true,
                isRead = true,
                messageType = MessageType.TEXT
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}