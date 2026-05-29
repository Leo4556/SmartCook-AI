package com.example.smartcookai

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionSet
import android.view.Window
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем transitions для Android 5.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)

            setupWindowAnimations()
        }
    }

    private fun setupWindowAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val fade = Fade().apply {
                duration = 200
                interpolator = DecelerateInterpolator()
            }

            window.enterTransition = fade
            window.exitTransition = fade
            window.returnTransition = fade
            window.reenterTransition = fade

            // ← ИЗМЕНЕНО: добавлен кастомный transition для скругления
            window.sharedElementEnterTransition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER

                addTransition(ChangeBounds().apply {
                    duration = 250
                    interpolator = FastOutSlowInInterpolator()
                })

                addTransition(ChangeTransform().apply {
                    duration = 250
                    interpolator = FastOutSlowInInterpolator()
                })

                addTransition(ChangeImageTransform().apply {
                    duration = 250
                    interpolator = FastOutSlowInInterpolator()
                })

                // ← ДОБАВЛЕНО: кастомный transition для скругления
                addTransition(ChangeRoundedImageTransform().apply {
                    duration = 250
                })
            }

            window.sharedElementReturnTransition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER

                addTransition(ChangeBounds().apply {
                    duration = 200
                    interpolator = FastOutSlowInInterpolator()
                })

                addTransition(ChangeTransform().apply {
                    duration = 200
                    interpolator = FastOutSlowInInterpolator()
                })

                addTransition(ChangeImageTransform().apply {
                    duration = 200
                    interpolator = FastOutSlowInInterpolator()
                })

                // ← ДОБАВЛЕНО: кастомный transition для скругления
                addTransition(ChangeRoundedImageTransform().apply {
                    duration = 200
                })
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        updateStatusBar()
    }

    private fun updateStatusBar() {
        val isDarkTheme =
            resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK ==
                    Configuration.UI_MODE_NIGHT_YES

        window.statusBarColor = getColor(R.color.colorBackground)

        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = !isDarkTheme
    }

    // Анимация при открытии нового экрана
    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(
            R.anim.elevation_fade_in,  // новый экран появляется
            R.anim.elevation_fade_out   // текущий экран исчезает
        )
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        super.startActivity(intent, options)
        // Проверяем, не используется ли shared element transition
        if (options == null || !options.containsKey("android:activity.sharedElementEnterTransition")) {
            overridePendingTransition(
                R.anim.elevation_fade_in,
                R.anim.elevation_fade_out
            )
        }
    }

    // ← ДОБАВЛЕНО: Анимация при возврате назад (кнопка "Назад" или жест)
    override fun finish() {
        super.finish()
        overridePendingTransition(
            R.anim.elevation_fade_in,  // предыдущий экран появляется
            R.anim.elevation_fade_out   // текущий экран исчезает
        )
    }

}