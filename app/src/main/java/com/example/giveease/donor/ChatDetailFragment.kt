package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.giveease.R

class ChatDetailFragment : Fragment() {

    private lateinit var ngoName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ngoName = arguments?.getString("ngoName") ?: "NGO Chat"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat_detail, container, false)

        view.findViewById<TextView>(R.id.tvNgoName).text = ngoName

        view.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
}
