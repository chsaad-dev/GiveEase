package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.github.chrisbanes.photoview.PhotoView

class ImageViewerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_viewer, container, false)
        
        val photoView = view.findViewById<PhotoView>(R.id.photoView)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        
        val imageUrl = arguments?.getString("imageUrl")
        
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(photoView)
        }
        
        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        view.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        photoView.setOnOutsidePhotoTapListener {
            parentFragmentManager.popBackStack()
        }
        
        return view
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.bottom_nav_donor)?.visibility = View.GONE
        requireActivity().findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<View>(R.id.bottom_nav_donor)?.visibility = View.VISIBLE
        requireActivity().findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    companion object {
        fun newInstance(imageUrl: String): ImageViewerFragment {
            val fragment = ImageViewerFragment()
            val args = Bundle()
            args.putString("imageUrl", imageUrl)
            fragment.arguments = args
            return fragment
        }
    }
}
