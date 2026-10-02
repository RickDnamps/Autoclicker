package com.rickdnamps.autoclicker

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.slider.Slider
import com.rickdnamps.autoclicker.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private companion object {
        var setupShownThisLaunch = false
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarsPadding()

        bindConfig(ClickConfig.load(this))

        binding.setupButton.setOnClickListener { openSetup() }
        binding.overlayButton.setOnClickListener { toggleOverlay() }

        binding.intervalInput.doAfterTextChanged { updateSpeedEstimate() }
        binding.pressInput.doAfterTextChanged { updateSpeedEstimate() }
        binding.randomIntervalSwitch.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        binding.randomOffsetSwitch.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        binding.stopGroup.setOnCheckedChangeListener { _, _ -> updateStopField() }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        // Show the setup guide by itself the first time the app opens without the service.
        if (!setupShownThisLaunch && !SystemSetup.isServiceEnabled(this)) openSetup()
    }

    override fun onPause() {
        super.onPause()
        saveConfig()
    }

    private fun bindConfig(config: ClickConfig): Unit = with(binding) {
        intervalInput.setText(config.intervalMs.toString())
        pressInput.setText(config.pressMs.toString())
        randomIntervalSwitch.isChecked = config.randomInterval
        randomIntervalInput.setText(config.randomIntervalMs.toString())
        randomOffsetSwitch.isChecked = config.randomOffset
        randomOffsetInput.setText(config.randomOffsetPx.toString())
        multiGroup.check(if (config.simultaneous) R.id.multiSimultaneous else R.id.multiSequential)
        stopGroup.check(
            when (config.stopMode) {
                StopMode.NEVER -> R.id.stopNever
                StopMode.COUNT -> R.id.stopCount
                StopMode.TIME -> R.id.stopTime
            }
        )
        targetSizeSlider.setStepValue(config.targetSizeDp)
        panelSizeSlider.setStepValue(config.panelScalePercent)
        updateVisibility()
        updateStopField()
        updateSpeedEstimate()
    }

    private fun readConfig(): ClickConfig = with(binding) {
        val previous = ClickConfig.load(this@MainActivity)
        val stopMode = when (stopGroup.checkedRadioButtonId) {
            R.id.stopCount -> StopMode.COUNT
            R.id.stopTime -> StopMode.TIME
            else -> StopMode.NEVER
        }
        val stopValue = stopValueInput.longOrNull()
        ClickConfig(
            intervalMs = intervalInput.longOrNull() ?: previous.intervalMs,
            pressMs = pressInput.longOrNull() ?: previous.pressMs,
            randomInterval = randomIntervalSwitch.isChecked,
            randomIntervalMs = randomIntervalInput.longOrNull() ?: previous.randomIntervalMs,
            randomOffset = randomOffsetSwitch.isChecked,
            randomOffsetPx = randomOffsetInput.longOrNull()?.toInt() ?: previous.randomOffsetPx,
            simultaneous = multiGroup.checkedRadioButtonId == R.id.multiSimultaneous,
            stopMode = stopMode,
            stopCount = if (stopMode == StopMode.COUNT && stopValue != null) stopValue else previous.stopCount,
            stopSeconds = if (stopMode == StopMode.TIME && stopValue != null) stopValue else previous.stopSeconds,
            targetSizeDp = targetSizeSlider.value.toInt(),
            panelScalePercent = panelSizeSlider.value.toInt(),
        ).sanitized()
    }

    private fun saveConfig() {
        val config = readConfig()
        if (config == ClickConfig.load(this)) return
        config.save(this)
        AutoClickService.instance?.overlay?.applyConfig()
    }

    private fun updateVisibility() {
        binding.randomIntervalLayout.isVisible = binding.randomIntervalSwitch.isChecked
        binding.randomOffsetLayout.isVisible = binding.randomOffsetSwitch.isChecked
    }

    /** Shows the field matching the selected stop condition, filled with its saved value. */
    private fun updateStopField() {
        val config = ClickConfig.load(this)
        val value = when (binding.stopGroup.checkedRadioButtonId) {
            R.id.stopCount -> config.stopCount to R.string.stop_count_value
            R.id.stopTime -> config.stopSeconds to R.string.stop_time_value
            else -> null
        }
        binding.stopValueLayout.isVisible = value != null
        if (value != null) {
            binding.stopValueLayout.hint = getString(value.second)
            binding.stopValueInput.setText(value.first.toString())
        }
    }

    private fun updateSpeedEstimate() {
        val interval = binding.intervalInput.longOrNull() ?: return
        val press = binding.pressInput.longOrNull() ?: ClickConfig.MIN_PRESS_MS
        val period = maxOf(interval, press, 1L)
        val cps = String.format(Locale.getDefault(), "%.1f", 1000.0 / period)
        binding.speedEstimate.text = getString(R.string.speed_estimate, cps)
    }

    private fun updateStatus() {
        val service = AutoClickService.instance
        val enabled = SystemSetup.isServiceEnabled(this)
        binding.statusText.setText(if (enabled) R.string.status_on else R.string.status_off)
        binding.statusDot.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (enabled) R.color.status_on else R.color.status_off)
        )
        binding.setupButton.isVisible = !enabled

        val showing = service?.overlay?.isShowing == true
        binding.overlayButton.setText(if (showing) R.string.hide_panel else R.string.show_panel)
        binding.overlayButton.setIconResource(if (showing) R.drawable.ic_close else R.drawable.ic_play)
    }

    private fun toggleOverlay() {
        val overlay = AutoClickService.instance?.overlay
        if (overlay == null) {
            Toast.makeText(this, R.string.toast_enable_first, Toast.LENGTH_SHORT).show()
            openSetup()
            return
        }
        saveConfig()
        if (overlay.isShowing) {
            overlay.hide()
            updateStatus()
        } else {
            overlay.show()
            // Go back to the home screen so the user can open the game.
            moveTaskToBack(true)
        }
    }

    private fun openSetup() {
        setupShownThisLaunch = true
        startActivity(Intent(this, SetupActivity::class.java))
    }

    private fun EditText.longOrNull(): Long? = text?.toString()?.trim()?.toLongOrNull()

    private fun Slider.setStepValue(raw: Int) {
        val steps = ((raw - valueFrom) / stepSize).toInt()
        value = (valueFrom + steps * stepSize).coerceIn(valueFrom, valueTo)
    }
}
