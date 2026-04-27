package uk.ncc.fitNova.auth

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import uk.ncc.fitNova.ui.applyBlackSystemBars

/**
 * Shared auth parent so login and registration reuse the same screen setup
 * and POST request plumbing instead of duplicating it in each activity.
 */
abstract class BaseAuthActivity : AppCompatActivity() {

    protected fun configureAuthScreen(savedInstanceState: Bundle?, layoutResId: Int, insetViewId: Int) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(layoutResId)
        applyBlackSystemBars(this)
        applySystemBarInsets(findViewById(insetViewId))
    }

    protected fun submitPostRequest(
        url: String,
        paramsProvider: () -> Map<String, String>,
        onResponse: (String) -> Unit,
        onError: (VolleyError) -> Unit
    ) {
        val request = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener(onResponse),
            Response.ErrorListener(onError)
        ) {
            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> = paramsProvider()
        }

        Volley.newRequestQueue(this).add(request)
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
