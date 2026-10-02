package com.rickdnamps.autoclicker

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.rickdnamps.autoclicker.databinding.ActivitySetupBinding
import com.rickdnamps.autoclicker.databinding.ItemSetupStepBinding

/**
 * Step-by-step guide to enable the accessibility service, including the
 * "restricted settings" unlock that Android 13+ requires for sideloaded apps.
 * Each step has a button that opens the exact system screen needed.
 */
class SetupActivity : AppCompatActivity() {

    private class Step(
        val view: ItemSetupStepBinding,
        @StringRes val title: Int,
        @StringRes val body: Int,
        @StringRes val action: Int,
        val open: (Context) -> Unit,
    )

    private lateinit var binding: ActivitySetupBinding
    private lateinit var steps: List<Step>

    private val prefs by lazy { getSharedPreferences("setup", MODE_PRIVATE) }

    /** Number of steps whose button the user already tapped. */
    private var stepsTapped: Int
        get() = prefs.getInt("stepsTapped", 0)
        set(value) = prefs.edit().putInt("stepsTapped", value).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        steps = if (SystemSetup.hasRestrictedSettings) {
            listOf(
                Step(binding.step1, R.string.setup_step1_title, R.string.setup_step1_body,
                    R.string.setup_open_service, SystemSetup::openServiceSettings),
                Step(binding.step2, R.string.setup_step2_title, R.string.setup_step2_body,
                    R.string.setup_open_app_info, SystemSetup::openAppInfo),
                Step(binding.step3, R.string.setup_step3_title, R.string.setup_step3_body,
                    R.string.setup_open_service, SystemSetup::openServiceSettings),
            )
        } else {
            // Before Android 13 a single step is enough.
            binding.step2.root.isVisible = false
            binding.step3.root.isVisible = false
            listOf(
                Step(binding.step1, R.string.setup_simple_title, R.string.setup_simple_body,
                    R.string.setup_open_service, SystemSetup::openServiceSettings),
            )
        }

        steps.forEachIndexed { index, step ->
            step.view.number.text = (index + 1).toString()
            step.view.title.setText(step.title)
            step.view.body.setText(step.body)
            step.view.action.setText(step.action)
            step.view.action.setOnClickListener {
                stepsTapped = maxOf(stepsTapped, index + 1)
                step.open(this)
            }
        }

        binding.laterButton.setOnClickListener { finish() }
        binding.doneButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val done = SystemSetup.isServiceEnabled(this)
        binding.stepsGroup.isVisible = !done
        binding.doneGroup.isVisible = done
        binding.setupIntro.isVisible = !done
        if (done) stepsTapped = 0 else highlightCurrentStep()
    }

    /** Outlines the next step to do and ticks the ones already done. */
    private fun highlightCurrentStep() {
        val current = stepsTapped.coerceAtMost(steps.lastIndex)
        val density = resources.displayMetrics.density
        val primary = ContextCompat.getColor(this, R.color.brand)
        val doneColor = ContextCompat.getColor(this, R.color.status_on)
        val pendingColor = ContextCompat.getColor(this, R.color.outline)

        steps.forEachIndexed { index, step ->
            val isCurrent = index == current
            val isDone = index < current
            step.view.card.strokeWidth = if (isCurrent) (2 * density).toInt() else 0
            step.view.root.alpha = if (isCurrent) 1f else 0.75f
            step.view.number.text = if (isDone) "✓" else (index + 1).toString()
            step.view.number.backgroundTintList = ColorStateList.valueOf(
                when {
                    isDone -> doneColor
                    isCurrent -> primary
                    else -> pendingColor
                }
            )
        }
    }
}
