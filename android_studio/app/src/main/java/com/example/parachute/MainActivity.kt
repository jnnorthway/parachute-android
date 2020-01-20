package com.example.parachute

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import org.jetbrains.anko.doAsync
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class MainActivity : AppCompatActivity() {
    var data : Intent? = null
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

        // Get views
        val sendButton: Button = findViewById(R.id.send_data)
        val gallery: Button = findViewById(R.id.gallery)
        val address: TextInputEditText = findViewById((R.id.address))
        val port: TextInputEditText = findViewById((R.id.port))
        val message: EditText = findViewById(R.id.message)

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
                //permission denied
                val permissions = arrayOf(Manifest.permission.INTERNET)
                //show popup to request runtime permission
                requestPermissions(permissions, PERMISSION_CODE)
            }
        }

        gallery.setOnClickListener {
            //check runtime permission
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED
                ) {
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    //show popup to request runtime permission
                    requestPermissions(permissions, PERMISSION_CODE)
                } else {
                    //permission already granted
                    pickImageFromGallery()
                }
            } else {
                //system OS is < Marshmallow
                pickImageFromGallery()
            }
        }

        sendButton.setOnClickListener {
            // Send file to sever
            var dataSent = false
            Toast.makeText(applicationContext, "Data Sending", Toast.LENGTH_LONG).show()
            doAsync {
                dataSent = sendFile(data!!.data!!, editTextToString(address), editTextToString(port))
            }
            if (dataSent) {
                Toast.makeText(applicationContext, "Data sent", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "Data send failed", Toast.LENGTH_LONG).show()
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
        private val IMAGE_PICK_CODE = 1000
        //Permission code
        private val PERMISSION_CODE = 1001
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            val image : ImageView = findViewById(R.id.imageView)
            image.setImageURI(intent?.data)
            data = intent
        }
    }

    private fun sendFile(data : Uri, address : String, port : String) : Boolean {

        var byteData : ByteArray?
        var cr : ContentResolver = baseContext.contentResolver
        var inputStream : InputStream? = cr.openInputStream(data)
        var bitmap : Bitmap = BitmapFactory.decodeStream(inputStream)
        var byteArray = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArray)
        byteData = byteArray.toByteArray()
        println("byte data size: " + byteData.size)

        val client = UdpClient(address, port)
        println("URI = $data")

        var fileName = stringToByteArray(getFileName(data))
        var fileSize = intToByteArray(byteData.size)
        var eof = stringToByteArray("<EOF>")
        client.sendData(fileName)
        client.sendData(fileSize)
        var i = 0
        var buffer = 1000
        println("Sending data...")
        while (i < byteData.size){
            var slice = i + buffer - 1
            if (byteData.size < slice) {
                slice = byteData.size -1
            }
            var buf = byteData.sliceArray(IntRange(i, slice))
            client.sendData(buf)
            i += buffer
        }
        return client.sendData(eof)
    }

    private fun stringToByteArray(data : String) : ByteArray {
        return data.toByteArray()
    }
    private fun intToByteArray(data : Int) : ByteArray {
        return data.toString().toByteArray()
    }
    private fun editTextToString(data : EditText) : String {
        return data.text.toString()
    }
    private fun getFileName(uri : Uri) : String {
      var result : String? = null
      if (uri.scheme.equals("content")) {
        var cursor : Cursor? = contentResolver.query(uri, null, null, null, null)
        try {
          if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
          }
        } finally {
          cursor?.close()
        }
      }
      if (result == null) {
        result = uri.path!!
        var cut : Int = result.lastIndexOf('/')
        if (cut != -1) {
          result = result.substring(cut + 1)
        }
      }
      return result
    }
}


class UdpClient(address: String, port: String) {
    private val address : String = address
    private val port : Int = Integer.parseInt(port)

    fun sendData(data: ByteArray):Boolean {
        println("Sending data")
        // Create the socket object
        val udpSocket = DatagramSocket()
        // Get ip address
        val ip = InetAddress.getByName(address)
        // Create datagram packet
        val udpSend = DatagramPacket(data, data.size, ip, port)
        // Send data
        udpSocket.send(udpSend)
        // Receive data
        udpSocket.receive(udpSend)
        return true
    }
}
