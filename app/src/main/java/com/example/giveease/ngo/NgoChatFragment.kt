package com.example.giveease.ngo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.adapter.ChatAdapter
import com.example.giveease.databinding.FragmentNgoChatBinding
import com.example.giveease.donor.ChatDetailFragment
import com.example.giveease.model.ChatRoom
import com.example.giveease.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NgoChatFragment : Fragment() {
    private lateinit var binding: FragmentNgoChatBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var chatAdapter: ChatAdapter
    private var chatListener: ListenerRegistration? = null
    private val allChats = mutableListOf<ChatRoom>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNgoChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        loadChats()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(UserManager.getUserId(requireContext())) { chatRoom ->
            openChatDetail(chatRoom)
        }

        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterChats(s.toString())
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadChats()
        }
    }

    private fun loadChats() {
        val userId = UserManager.getUserId(requireContext())

        binding.progressBar.visibility = View.VISIBLE

        chatListener?.remove()
        chatListener = firestore.collection("chats")
            .whereEqualTo("ngoId", userId)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    android.util.Log.e("NgoChatFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                allChats.clear()

                snapshot?.documents?.forEach { doc ->
                    val chat = doc.toObject(ChatRoom::class.java)
                    if (chat == null) {
                            android.util.Log.w("NgoChatFragment", "Warning: ChatRoom parsed as NULL!")
                    } else {
                        allChats.add(chat.copy(id = doc.id))
                    }
                }

                allChats.sortByDescending { it.lastMessageTime }
                updateUI()
            }
    }

    private fun filterChats(query: String) {
        val filtered = if (query.isEmpty()) {
            allChats
        } else {
            allChats.filter {
                it.donorName.contains(query, ignoreCase = true) ||
                        it.campaignName.contains(query, ignoreCase = true) ||
                        it.lastMessage.contains(query, ignoreCase = true)
            }
        }

        chatAdapter.submitList(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateUI() {
        chatAdapter.submitList(allChats.toList())
        binding.emptyState.visibility = if (allChats.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openChatDetail(chatRoom: ChatRoom) {
        val fragment = ChatDetailFragment().apply {
            arguments = Bundle().apply {
                putString("chatRoomId", chatRoom.id)
                putString("otherUserId", chatRoom.donorId)
                putString("otherUserName", chatRoom.donorName)
                putString("otherUserImage", chatRoom.donorImage)
                putString("campaignName", chatRoom.campaignName)
                putBoolean("isDonor", false)
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatListener?.remove()
    }
}