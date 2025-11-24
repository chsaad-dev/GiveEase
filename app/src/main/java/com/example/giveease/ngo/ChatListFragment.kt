package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentNgoChatBinding

class ChatListFragment : Fragment() {

    private var _binding: FragmentNgoChatBinding? = null
    private val binding get() = _binding!!

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

        binding.fabNewChat.setOnClickListener {
            // Navigate to new chat or show donor list
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}