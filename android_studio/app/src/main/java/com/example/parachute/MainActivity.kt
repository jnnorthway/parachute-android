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
import android.os.SystemClock
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
import org.jetbrains.anko.uiThread
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.*


class MainActivity : AppCompatActivity() {
    var data : Intent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.navView)

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
            Toast.makeText(applicationContext, "Data Sending", Toast.LENGTH_SHORT).show()
            doAsync {
                val tcpClient = TcpClient(editTextToString(address), editTextToString(port))
                val dataSent = tcpClient.sendFile(data!!.data!!, baseContext.contentResolver)
                uiThread{
                    if (dataSent) {
                        Toast.makeText(applicationContext, "Data send successful", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, "Data send failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

//    private fun getDocument() {
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
//    }

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

    private fun editTextToString(data : EditText) : String {
        return data.text.toString()
    }
}


class TcpClient(serverAddress: String, serverPort: String, buffer: Int = 1024) {
    private val address : String = serverAddress
    private val port : Int = Integer.parseInt(serverPort)
    private val maxBuffer : Int = buffer
    private var tcpSocket : Socket

    init {
        val ip = InetAddress.getByName(address)
        tcpSocket = Socket(ip!!, port)
    }

    fun sendData(data: ByteArray):Boolean {
        tcpSocket.outputStream.write(data)
        return true
    }

    fun sendFile(data : Uri, contentResolver : ContentResolver) : Boolean {

        var byteData : ByteArray?
        var cr : ContentResolver = contentResolver
        var inputStream : InputStream? = cr.openInputStream(data)
        var bitmap : Bitmap = BitmapFactory.decodeStream(inputStream)
        var byteArray = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArray)
        byteData = byteArray.toByteArray()
        println("byte data size: " + byteData.size)

        var fileName = stringToByteArray(getFileName(data, contentResolver))
        var fileSize = intToByteArray(byteData.size)
        var eof = stringToByteArray("<EOF>")

        if (!sendData(fileName)){
            return false
        }
        SystemClock.sleep(500)
        if(!sendData(fileSize)){
            return false
        }
        SystemClock.sleep(500)

        var i = 0
        println("Sending data...")
        while (i < byteData.size){
            var slice = i + maxBuffer - 1
            if (byteData.size < slice) {
                slice = byteData.size -1
            }
            var buf = byteData.sliceArray(IntRange(i, slice))
            if(!sendData(buf)){
                return false
            }
            i += maxBuffer
        }
        SystemClock.sleep(500)
        return sendData(eof)
    }

    private fun getFileName(uri : Uri, cr : ContentResolver) : String {
        var result : String? = null
        if (uri.scheme.equals("content")) {
            var cursor : Cursor? = cr.query(uri, null, null, null, null)
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

    private fun stringToByteArray(data : String) : ByteArray {
        return data.toByteArray()
    }
    private fun intToByteArray(data : Int) : ByteArray {
        return data.toString().toByteArray()
    }
}
