package com.example.parachute

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.textfield.TextInputEditText
import java.net.InetAddress
import android.os.AsyncTask.execute
import android.os.AsyncTask
import android.text.Editable


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val tag = "PARACHUTE"
        // get button
        val sendButton: Button = findViewById(R.id.send_data)
        val address: TextInputEditText = findViewById((R.id.address))

        // handle button click
        sendButton.setOnClickListener {
            // Do something in response to button click
            Log.d(tag, "send button clicked")
            Toast.makeText(applicationContext, "Data Sending", Toast.LENGTH_LONG).show()
            val test = PingExample
            if (test.main(address.text)) {
                Toast.makeText(applicationContext, "Ping worked", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(applicationContext, "Ping failed", Toast.LENGTH_LONG).show()
            }
        }
    }
}

object PingExample {
    @JvmStatic
    fun main(addr: Editable?):Boolean {
        var reachable = false
        AsyncTask.execute {
            try {
                println(addr.toString())
                val address = InetAddress.getByName(addr.toString())
//                val address = InetAddress.getByName("www.google.com")
                println(address)
                reachable = address.isReachable(10000)
                println("Is host reachable? $reachable")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("ping", "failed to ping")
            }
        }
        println("Is host reachable? $reachable")

        return reachable
    }
}
