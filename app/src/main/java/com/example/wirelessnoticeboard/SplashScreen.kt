package com.example.wirelessnoticeboard

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.wirelessnoticeboard.databinding.SplashScreenBinding

class SplashScreen : AppCompatActivity() {

    private lateinit var binding : SplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = SplashScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        fadeInAnim(binding.screenText)
    }

    private fun fadeInAnim(splash_text : TextView){
        var fadeIn : Animation = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = AccelerateInterpolator();
        fadeIn.duration = 1000

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                splash_text.visibility = View.VISIBLE
                Handler().postDelayed({
                    startActivity(Intent(applicationContext, LoginActivity::class.java))
                    finish()
                }, SLASH_DURATION.toLong())
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        splash_text.startAnimation(fadeIn)
    }

    companion object {
        const val SLASH_DURATION = 500
    }
}

