package com.example.giveease.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R

/**
 * Centralized animation utilities for premium UI interactions.
 * All animations are GPU-accelerated (transform-only) and optimized for 60Hz.
 */
object AnimUtils {

    private val springInterpolator = OvershootInterpolator(1.2f)

    /**
     * Apply a premium button-press scale effect.
     * Scales to 0.95 on press, springs back to 1.0 on release.
     */
    fun applyButtonPressEffect(vararg views: View) {
        views.forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val scaleDownX = ObjectAnimator.ofFloat(v, View.SCALE_X, 0.95f)
                        val scaleDownY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 0.95f)
                        val set = AnimatorSet()
                        set.playTogether(scaleDownX, scaleDownY)
                        set.duration = 100
                        set.start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val scaleUpX = ObjectAnimator.ofFloat(v, View.SCALE_X, 1.0f)
                        val scaleUpY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1.0f)
                        val set = AnimatorSet()
                        set.playTogether(scaleUpX, scaleUpY)
                        set.duration = 150
                        set.interpolator = springInterpolator
                        set.start()

                        if (event.action == MotionEvent.ACTION_UP) {
                            v.performClick()
                        }
                    }
                }
                true
            }
        }
    }

    /**
     * Apply staggered entrance animation to a RecyclerView.
     * Items fall down with a subtle overshoot bounce and 20ms stagger.
     */
    fun applyStaggeredEntrance(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(
            context, R.anim.layout_item_fall_down
        )
        recyclerView.layoutAnimation = controller
    }

    /**
     * Re-run the staggered entrance animation (e.g. after data refresh).
     */
    fun replayStaggeredEntrance(recyclerView: RecyclerView) {
        recyclerView.scheduleLayoutAnimation()
    }
}
