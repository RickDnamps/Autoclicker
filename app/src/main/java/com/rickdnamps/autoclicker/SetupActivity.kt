package com.rickdnamps.autoclicker

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    ) {
        /** Opening the accessibility screen requires the user's informed consent first. */
        val needsConsent: Boolean get() = action == R.string.setup_open_service
    }

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
                withConsentIfNeeded(step.needsConsent) {
                    stepsTapped = maxOf(stepsTapped, index + 1)
                    step.open(this)
                }
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

    /**
     * Prominent disclosure required by Google Play before the user is sent to
     * enable an accessibility service: explains what the API is used for and
     * asks for explicit consent.
     */
    private fun withConsentIfNeeded(needed: Boolean, onAccepted: () -> Unit) {
        if (!needed || prefs.getBoolean("accessibilityConsent", false)) {
            onAccepted()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.disclosure_title)
            .setMessage(R.string.disclosure_message)
            .setCancelable(false)
            .setPositiveButton(R.string.disclosure_accept) { _, _ ->
                prefs.edit().putBoolean("accessibilityConsent", true).apply()
                onAccepted()
            }
            .setNegativeButton(R.string.disclosure_decline, null)
            .show()
    }

    /**
     * Outlines the next step to do, ticks the ones already done and locks the
     * ones after it: the Android 13+ unlock only appears once step 1 was tried.
     */
    private fun highlightCurrentStep() {
        val current = stepsTapped.coerceAtMost(steps.lastIndex)
        val density = resources.displayMetrics.density
        val primary = ContextCompat.getColor(this, R.color.brand)
        val doneColor = ContextCompat.getColor(this, R.color.status_on)
        val pendingColor = ContextCompat.getColor(this, R.color.outline)

        steps.forEachIndexed { index, step ->
            val isCurrent = index == current
            val isDone = index < current
            val isLocked = index > current
            step.view.card.strokeWidth = if (isCurrent) (2 * density).toInt() else 0
            step.view.root.alpha = when {
                isCurrent -> 1f
                isLocked -> 0.5f
                else -> 0.75f
            }
            // Done steps stay usable: step 1 may have to be redone to get the unlock.
            step.view.action.isEnabled = !isLocked
            step.view.lockHint.isVisible = isLocked
            step.view.lockHint.text = getString(R.string.setup_locked, index)
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
