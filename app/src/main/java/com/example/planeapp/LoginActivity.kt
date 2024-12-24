package com.example.planeapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.planeapp.data.User
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            buttonAction()
        }

    }

    private fun buttonAction() {

        val phoneNumberView: EditText = findViewById(R.id.textPhone)
        val phoneNumber = phoneNumberView.text.toString()

        if (phoneNumber == "") return

        logIn(phoneNumber)

    }

    private fun toast(text: String) {

        this.runOnUiThread {

            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()

        }

    }

    private fun logIn(phoneNumber: String) {

        val url = "http://plainapp.ru:3000/api/user/byPN/$phoneNumber"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                toast(e.message.toString())

            }
            override fun onResponse(call: Call, response: Response) {

                if (response.body == null) {

                    toast("null body")

                } else {

                    when (val responseString = response.body!!.string()) {
                        "" -> {

                            toast("empty body")

                        }
                        "[]" -> {

                            toast("empty json")

                        }
                        else -> {

                            val responseArray = Json.decodeFromString<List<JsonElement>>(responseString)

                            val user = Json.decodeFromJsonElement<User>(responseArray[0])

                            toast(user.id.toString())

                        }
                    }

                }

            }
        })

        /*

        val url = "http://plainapp.ru:3000/api/user/new"

        val jsonText = "{\"phoneNumber\": \"" + phoneNumberView.text.toString() + "\"}"

        val request = Request.Builder()
            .url(url)
            .put(jsonText.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                Toast.makeText(applicationContext, e.message.toString(), Toast.LENGTH_LONG).show()

            }
            override fun onResponse(call: Call, response: Response) {

                Log.d("debug", response.message)



            }
        })

        */

        /*
        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)

         */

    }
}