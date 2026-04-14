package com.example.smartcookai

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Outline
import android.os.Build
import android.transition.Transition
import android.transition.TransitionValues
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ChangeRoundedImageTransform : Transition() {

    companion object {
        private const val PROPNAME_RADIUS = "smartcook:changeRoundedTransform:radius"
        private const val CORNER_RADIUS = 16f // dp
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        if (view.width <= 0 || view.height <= 0) return

        transitionValues.values[PROPNAME_RADIUS] = CORNER_RADIUS * view.context.resources.displayMetrics.density
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) return null

        val view = endValues.view
        val radius = CORNER_RADIUS * view.context.resources.displayMetrics.density

        // Устанавливаем постоянное скругление
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        view.clipToOutline = true

        // Возвращаем пустую анимацию, чтобы transition не игнорировался
        return ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 1f).setDuration(1)
    }
}