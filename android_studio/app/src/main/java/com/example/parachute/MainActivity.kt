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
import android.widget.EditText
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.*




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
        val port: TextInputEditText = findViewById((R.id.port))
        val message: EditText = findViewById(R.id.message)

        // handle button click
        sendButton.setOnClickListener {
            // Do something in response to button click
            Log.d(tag, "send button clicked")
            Toast.makeText(applicationContext, "Data Sending", Toast.LENGTH_LONG).show()
            val test = udpClient
            if (test.sendData(address.text, port.text, message.text)) {
                Toast.makeText(applicationContext, "Data sent", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(applicationContext, "Data send failed", Toast.LENGTH_LONG).show()
            }
        }
    }
}



object udpClient {
//    @JvmStatic
    fun sendData(address: Editable?, port: Editable?, message: Editable?):Boolean {

        var buf = ByteArray(1000)
        val dp = DatagramPacket(buf, buf.size)


        // Create the socket object
        val udpSocket = DatagramSocket()
        // Set ip to address passed in
        val ip = InetAddress.getByName(address.toString())
        println(ip)
        val port = Integer.parseInt(port.toString())
        println(port)
//        var buf: ByteArray?

        val data = message.toString()
        println(data)
        // convert the String input into the byte array.
        buf = data.toByteArray()

        // Step 2 : Create the datagramPacket for sending
        // the data.
        val udpSend = DatagramPacket(buf!!, buf!!.size, ip, port)

        // Step 3 : invoke the send call to actually send
        // the data.
        udpSocket.send(udpSend)

        return true
    }
}

//object PingExample {
//    @JvmStatic
//    fun main(addr: Editable?):Boolean {
//        var reachable = false
//        AsyncTask.execute {
//            try {
//                println(addr.toString())
//                val address = InetAddress.getByName(addr.toString())
////                val address = InetAddress.getByName("www.google.com")
//                println(address)
//                reachable = address.isReachable(10000)
//                println("Is host reachable? $reachable")
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.d("ping", "failed to ping")
//            }
//        }
//        println("Is host reachable? $reachable")
//
//        return reachable
//    }
//}
