package uk.ncc.fitNova.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject
import uk.ncc.fitNova.R
import uk.ncc.fitNova.dashboard.FitnessActivity
import uk.ncc.fitNova.data.prefs.SessionPrefs
import uk.ncc.fitNova.data.prefs.SessionSnapshot
import uk.ncc.fitNova.data.remote.BackendConfig

class MainActivity : BaseAuthActivity() {

    private lateinit var userName: EditText
    private lateinit var password: EditText
    private val sessionPrefs by lazy { SessionPrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        configureAuthScreen(savedInstanceState, R.layout.activity_main, R.id.main)
        clearSavedSession()

        // Initialize UI Elements
        userName = findViewById(R.id.etUsername)
        password = findViewById(R.id.etPassword)

        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnSignIn.setOnClickListener {
            if (validate()) {
                login()
            }
        }

        btnSignUp.setOnClickListener {
            val intentSignUp = Intent(this, RegistrationActivity::class.java)
            startActivity(intentSignUp)
        }
    }

    private fun validate(): Boolean {
        if (userName.text.toString().trim().isEmpty()) {
            userName.error = "Enter Email"
            return false
        }

        if (password.text.toString().trim().isEmpty()) {
            password.error = "Enter Password"
            return false
        }

        return true
    }

    private fun login() {
        val username = userName.text.toString().trim()
        val passwordText = password.text.toString().trim()

        submitPostRequest(
            url = BackendConfig.LOGIN_URL,
            paramsProvider = {
                hashMapOf(
                    "phpFunction" to "login",
                    "username" to username,
                    "password" to passwordText
                )
            },
            onResponse = { response ->
                try {
                    val obj = JSONObject(response.trim())
                    val responseSuccess = obj.getString("response")

                    if (responseSuccess == "true") {

                        val userid = obj.getString("userid")
                        val fullName = obj.getString("FullName")
                        val weight = obj.getString("Weight")
                        val height = obj.getString("Height")
                        val email = obj.optString("Email")
                        val gender = obj.optString("Gender")
                        val age = obj.optInt("Age", 0)

                        Toast.makeText(
                            this,
                            "Thank you for logging in $fullName",
                            Toast.LENGTH_SHORT
                        ).show()

                        sessionPrefs.saveLogin(
                            SessionSnapshot(
                                userId = userid.toInt(),
                                fullName = fullName,
                                email = email,
                                gender = gender,
                                age = age,
                                weight = weight.toInt(),
                                height = height.toInt()
                            )
                        )

                        val intentFitness =
                            Intent(this@MainActivity, FitnessActivity::class.java)
                        startActivity(intentFitness)
                        finish()

                    } else {
                        Toast.makeText(this, "Account does not exist", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.e("JSONError", "Failed to parse JSON: ${e.message}")
                    Toast.makeText(this@MainActivity, "JSON Error!", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Log.e("VolleyError", "Error: ${error}")
                Toast.makeText(this@MainActivity, "Network Error!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun clearSavedSession() {
        sessionPrefs.clear()
    }
}
