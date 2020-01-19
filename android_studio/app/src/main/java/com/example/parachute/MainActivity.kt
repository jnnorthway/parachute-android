package com.example.parachute

import android.Manifest
import android.R.attr
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


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
        val gallery: Button = findViewById(R.id.gallery)
        val address: TextInputEditText = findViewById((R.id.address))
        val port: TextInputEditText = findViewById((R.id.port))
        val message: EditText = findViewById(R.id.message)

        // handle button click
        sendButton.setOnClickListener {
            // Do something in response to button click
            Log.d(tag, "send button clicked")
            Toast.makeText(applicationContext, "Data Sending", Toast.LENGTH_LONG).show()
            val test = udpClient
//            if (test.sendData(address.text, port.text, message.text.toString()).toByteArray(), ) {
//                Toast.makeText(applicationContext, "Data sent", Toast.LENGTH_LONG).show()
//            } else {
//                Toast.makeText(applicationContext, "Data send failed", Toast.LENGTH_LONG).show()
//            }
        }

        gallery.setOnClickListener {
            //check runtime permission
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED
                ) {
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    //show popup to request runtime permission
                    requestPermissions(permissions, PERMISSION_CODE);
                } else {
                    //permission already granted
                    pickImageFromGallery();
                }
            } else {
                //system OS is < Marshmallow
                pickImageFromGallery();
            }
        }
    }
    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000;
        //Permission code
        private val PERMISSION_CODE = 1001;
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.size >0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()
                }
                else{
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            Toast.makeText(this, "Image picked", Toast.LENGTH_SHORT).show()
            println(data?.data)
            println(data?.type)
            var bdata : ByteArray? = null
            var cr : ContentResolver = getBaseContext().getContentResolver()
            var inputStream : InputStream? = cr.openInputStream(data!!.data!!)
            var bitmap : Bitmap = BitmapFactory.decodeStream(inputStream)
            var baos : ByteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            bdata = baos.toByteArray()
            println(bdata.size)


            val tag = "PARACHUTE"
            // get button
            val sendButton: Button = findViewById(R.id.send_data)
            val address: TextInputEditText = findViewById((R.id.address))
            val port: TextInputEditText = findViewById((R.id.port))

            Toast.makeText(applicationContext, "Data Sending", Toast.LENGTH_LONG).show()
            val test = udpClient
            var file_name = "bird.jpg".toByteArray()
            var file_size = bdata.size.toString().toByteArray()
            var eof = "<EOF>".toByteArray()
            test.sendData(address.text, port.text, file_name, file_name.size)
            test.sendData(address.text, port.text, file_size, file_size.size)


            var i = 0
            var buffer = 1000
            while (i < bdata.size){
                var slice = i+buffer
                if (bdata.size < slice) {
                    slice = bdata.size -1
                }
                println("size = " + bdata.size)
                println("i = " + i)
                println("slice = " + slice)
                var buf = bdata.sliceArray(IntRange(i, slice))
                println("MESSAGE")
                test.sendData(address.text, port.text, buf, buf.size)
                i += buffer
            }
            test.sendData(address.text, port.text, eof, eof.size)

        }
    }

}



object udpClient {
//    @JvmStatic
    fun sendData(address: Editable?, port: Editable?, data: ByteArray?, buffer: Int):Boolean {

        var buf = ByteArray(buffer)
        val dp = DatagramPacket(buf, buf.size)


        // Create the socket object
        val udpSocket = DatagramSocket()
        // Set ip to address passed in
        val ip = InetAddress.getByName(address.toString())
        println(ip)
        val port = Integer.parseInt(port.toString())
        println(port)

        buf = data!!

        // Step 2 : Create the datagramPacket for sending
        // the data.
        val udpSend = DatagramPacket(buf!!, buf!!.size, ip, port)

        // Step 3 : invoke the send call to actually send
        // the data.
        udpSocket.send(udpSend)
        var id = udpSocket.receive(udpSend)
        println(id.toString())
        return true
    }
}
