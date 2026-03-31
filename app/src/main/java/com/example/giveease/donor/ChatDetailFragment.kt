package com.example.giveease.donor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.adapter.MessageAdapter
import com.example.giveease.databinding.FragmentChatDetailBinding
import com.example.giveease.model.Message
import com.example.giveease.model.MessageStatus
import com.example.giveease.model.MessageType
import com.google.firebase.Timestamp
import com.example.giveease.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ChatDetailFragment : Fragment() {
    private lateinit var binding: FragmentChatDetailBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var messageAdapter: MessageAdapter
    private var messageListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null
    private var typingHandler: Handler? = null
    private var typingRunnable: Runnable? = null

    private lateinit var chatRoomId: String
    private lateinit var otherUserId: String
    private lateinit var otherUserName: String
    private var otherUserImage: String = ""
    private var campaignName: String = ""
    private var isDonor: Boolean = true

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { showImagePreview(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatRoomId = it.getString("chatRoomId") ?: ""
            otherUserId = it.getString("otherUserId") ?: ""
            otherUserName = it.getString("otherUserName") ?: ""
            otherUserImage = it.getString("otherUserImage") ?: ""
            campaignName = it.getString("campaignName") ?: ""
            isDonor = it.getBoolean("isDonor", true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        typingHandler = Handler(Looper.getMainLooper())

        setupUI()
        setupRecyclerView()
        setupListeners()

        // Handle navigation bar overlap
        ViewCompat.setOnApplyWindowInsetsListener(binding.messageInputLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom + 8) // +8 for visual comfort
            insets
        }

        loadMessages()
        updateOnlineStatus(true)
        listenForTypingStatus()
        listenForOnlineStatus()
    }

    private fun setupUI() {
        binding.tvName.text = otherUserName

        if (otherUserImage.isNotEmpty()) {
            Glide.with(this)
                .load(otherUserImage)
                .placeholder(R.drawable.sample_profile)
                .into(binding.imgProfile)
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(UserManager.getUserId(requireContext())) { imageUrl ->
            showFullImage(imageUrl)
        }

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupListeners() {
        binding.fabSend.setOnClickListener {
            sendMessage()
        }

        binding.btnAttach.setOnClickListener {
            pickImage()
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                handleTyping(s.toString().isNotEmpty())
            }
        })

        binding.btnCancelPreview.setOnClickListener {
            hideImagePreview()
        }

        binding.fabSendImage.setOnClickListener {
            sendImageMessage()
        }
    }
    private fun loadMessages() {
        messageListener?.remove()
        messageListener = firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val msg = doc.toObject(Message::class.java)?.copy(id = doc.id)
                    msg?.let {
                        it.status = when {
                            it.isRead -> MessageStatus.READ
                            it.isDelivered -> MessageStatus.DELIVERED
                            else -> MessageStatus.SENT
                        }
                    }
                    msg
                } ?: emptyList()

                messageAdapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                    }
                }

                markMessagesAsRead()
            }
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val message = Message(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId,
            senderId = UserManager.getUserId(requireContext()),
            receiverId = otherUserId,
            senderName = UserManager.getUserName(requireContext()),
            message = messageText,
            type = MessageType.TEXT,
            status = MessageStatus.SENDING,
            timestamp = Timestamp.now()
        )

        sendMessageToFirestore(message)
        binding.etMessage.text?.clear()
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun showImagePreview(uri: Uri) {
        binding.imagePreviewLayout.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(binding.imgPreview)
    }

    private fun hideImagePreview() {
        binding.imagePreviewLayout.visibility = View.GONE
        binding.etImageCaption.text?.clear()
        selectedImageUri = null
    }

    private fun sendImageMessage() {
        val uri = selectedImageUri ?: return
        val caption = binding.etImageCaption.text.toString().trim()

        binding.progressBar.visibility = View.VISIBLE

        val storageRef = storage.reference
            .child("chat_images/${chatRoomId}/${UUID.randomUUID()}.jpg")

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    chatRoomId = chatRoomId,
                    senderId = UserManager.getUserId(requireContext()),
                    receiverId = otherUserId,
                    senderName = UserManager.getUserName(requireContext()),
                    message = caption,
                    imageUrl = downloadUri.toString(),
                    type = MessageType.IMAGE,
                    status = MessageStatus.SENDING,
                    timestamp = Timestamp.now()
                )

                sendMessageToFirestore(message)
                binding.progressBar.visibility = View.GONE
                hideImagePreview()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendMessageToFirestore(message: Message) {
        firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .document(message.id)
            .set(message)
            .addOnSuccessListener {
                updateChatRoomLastMessage(message)
            }
    }

    private fun updateChatRoomLastMessage(message: Message) {
        val updates = hashMapOf<String, Any>(
            "lastMessage" to (if (message.type == MessageType.IMAGE) "📷 Photo" else message.message),
            "lastMessageSenderId" to message.senderId,
            "lastMessageTime" to Timestamp.now(),
            (if (isDonor) "ngoUnread" else "donorUnread") to
                    com.google.firebase.firestore.FieldValue.increment(1)
        )

        firestore.collection("chats")
            .document(chatRoomId)
            .update(updates)
    }

    private fun markMessagesAsRead() {
        val currentUserId = UserManager.getUserId(requireContext())
        
        firestore.collection("chats")
            .document(chatRoomId)
            .update(
                if (isDonor) "donorUnread" else "ngoUnread",
                0
            )

        firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    firestore.runBatch { batch ->
                        for (doc in querySnapshot.documents) {
                            batch.update(doc.reference, "isRead", true, "isDelivered", true)
                        }
                    }
                }
            }
    }

    private fun handleTyping(isTyping: Boolean) {
        typingRunnable?.let { typingHandler?.removeCallbacks(it) }

        if (isTyping) {
            updateTypingStatus(true)

            typingRunnable = Runnable {
                updateTypingStatus(false)
            }
            typingHandler?.postDelayed(typingRunnable!!, 3000)
        } else {
            updateTypingStatus(false)
        }
    }

    private fun updateTypingStatus(isTyping: Boolean) {
        firestore.collection("chats")
            .document(chatRoomId)
            .update(
                if (isDonor) "donorTyping" else "ngoTyping",
                isTyping
            )
    }

    private fun listenForTypingStatus() {
        statusListener = firestore.collection("chats")
            .document(chatRoomId)
            .addSnapshotListener { snapshot, _ ->
                val isTyping = if (isDonor) {
                    snapshot?.getBoolean("ngoTyping") ?: false
                } else {
                    snapshot?.getBoolean("donorTyping") ?: false
                }

                binding.typingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
            }
    }

    private fun listenForOnlineStatus() {
        firestore.collection("chats")
            .document(chatRoomId)
            .addSnapshotListener { snapshot, _ ->
                val isOnline = if (isDonor) {
                    snapshot?.getBoolean("ngoOnline") ?: false
                } else {
                    snapshot?.getBoolean("donorOnline") ?: false
                }

                val lastSeen = if (isDonor) {
                    snapshot?.getTimestamp("ngoLastSeen")
                } else {
                    snapshot?.getTimestamp("donorLastSeen")
                }

                updateStatusText(isOnline, lastSeen)
            }
    }

    private fun updateStatusText(isOnline: Boolean, lastSeen: Timestamp?) {
        binding.tvStatus.text = when {
            isOnline -> "online"
            lastSeen != null -> {
                val now = System.currentTimeMillis()
                val diff = now - lastSeen.toDate().time
                val minutes = diff / 60000

                when {
                    minutes < 1 -> "last seen just now"
                    minutes < 60 -> "last seen ${minutes}m ago"
                    minutes < 1440 -> "last seen ${minutes / 60}h ago"
                    else -> "last seen ${minutes / 1440}d ago"
                }
            }
            else -> "offline"
        }
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        firestore.collection("chats")
            .document(chatRoomId)
            .update(
                if (isDonor) "donorOnline" else "ngoOnline",
                isOnline,
                if (isDonor) "donorLastSeen" else "ngoLastSeen",
                Timestamp.now()
            )
    }

    private fun showFullImage(imageUrl: String) {
        // TODO: Implement full screen image viewer
        Toast.makeText(requireContext(), "Image viewer coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        updateOnlineStatus(false)
        updateTypingStatus(false)
    }

    override fun onResume() {
        super.onResume()
        updateOnlineStatus(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageListener?.remove()
        statusListener?.remove()
        typingRunnable?.let { typingHandler?.removeCallbacks(it) }
        updateOnlineStatus(false)
        updateTypingStatus(false)
    }
}