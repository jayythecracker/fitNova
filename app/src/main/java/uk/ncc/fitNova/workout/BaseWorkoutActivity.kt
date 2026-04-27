package uk.ncc.fitNova.workout

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import uk.ncc.fitNova.data.prefs.SessionPrefs
import uk.ncc.fitNova.data.remote.BackendConfig
import uk.ncc.fitNova.ui.applyBlackSystemBars
import java.util.Locale

/**
 * Shared workout parent centralizes timer handling and save requests so each
 * concrete workout activity can focus on its own UI, GPS, and set-tracking rules.
 */
abstract class BaseWorkoutActivity : AppCompatActivity() {
    protected val sessionPrefs by lazy { SessionPrefs(this) }
    protected var elapsedSeconds = 0
    protected var isWorkoutRunning = false
    protected var isSaving = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isWorkoutRunning) {
                return
            }

            elapsedSeconds++
            onTimerTick(elapsedSeconds)
            timerHandler.postDelayed(this, 1000)
        }
    }

    protected fun configureWorkoutScreen(savedInstanceState: Bundle?, layoutResId: Int, insetViewId: Int) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(layoutResId)
        applyBlackSystemBars(this)
        applySystemBarInsets(findViewById(insetViewId))
    }

    protected fun startTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    protected fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    protected open fun onTimerTick(totalSeconds: Int) = Unit

    protected fun requireUserId(missingUserMessageRes: Int): Int? {
        val userId = sessionPrefs.getUserId()
        if (userId <= 0) {
            Toast.makeText(this, missingUserMessageRes, Toast.LENGTH_SHORT).show()
            return null
        }
        return userId
    }

    protected fun saveWorkout(
        session: BaseWorkoutSession,
        logTag: String,
        successMessageRes: Int,
        failureMessageRes: Int,
        networkErrorMessageRes: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val request = object : StringRequest(
            Method.POST,
            BackendConfig.WORKOUT_URL,
            Response.Listener<String> { response ->
                try {
                    val payload = JSONObject(response.trim())
                    if (payload.optString("response") == "true") {
                        Toast.makeText(this, successMessageRes, Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        onFailure()
                        Toast.makeText(
                            this,
                            payload.optString("message", getString(failureMessageRes)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (exception: JSONException) {
                    Log.e(logTag, "Invalid JSON response: ${exception.message}")
                    onFailure()
                    Toast.makeText(this, failureMessageRes, Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e(logTag, "Network error: $error")
                onFailure()
                Toast.makeText(this, networkErrorMessageRes, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> = session.toJson()
        }

        Volley.newRequestQueue(this).add(request)
    }

    protected fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }

    private fun applySystemBarInsets(view: View) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )
            insets
        }

        ViewCompat.requestApplyInsets(view)
    }
}
