package com.example.giveease.utils

import com.example.giveease.model.ChatRoom
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

object ChatHelper {

    fun openChatFromCampaign(
        campaignId: String,
        campaignName: String,
        campaignImage: String,
        ngoId: String,
        ngoName: String,
        ngoImage: String,
        currentDonorId: String,
        currentDonorName: String,
        currentDonorImage: String,
        onChatRoomCreated: (String) -> Unit,
        onError: () -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val suggestedChatId = "chat_${campaignId}_${currentDonorId}"

        firestore.collection("chats")
            .document(suggestedChatId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onChatRoomCreated(snapshot.id)
                } else {
                    val chatRoom = ChatRoom(
                        id = suggestedChatId,
                        donorId = currentDonorId,
                        donorName = currentDonorName,
                        donorImage = currentDonorImage,
                        ngoId = ngoId,
                        ngoName = ngoName,
                        ngoImage = ngoImage,
                        campaignId = campaignId,
                        campaignName = campaignName,
                        campaignImage = campaignImage,
                        lastMessage = "",
                        lastMessageSenderId = "",
                        lastMessageTime = Timestamp.now(),
                        participants = mapOf(currentDonorId to true, ngoId to true),
                        createdAt = Timestamp.now()
                    )

                    firestore.collection("chats")
                        .document(suggestedChatId)
                        .set(chatRoom)
                        .addOnSuccessListener {
                            onChatRoomCreated(suggestedChatId)
                        }
                        .addOnFailureListener {
                            onError()
                        }
                }
            }
            .addOnFailureListener {
                onError()
            }
    }
}