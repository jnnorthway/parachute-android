package com.example.parachute

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private var data : Uri? = null
    private val addressBase = "Device IP Address: "
    private val fileBase = "File: "
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val intentData : Intent = intent
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
        val sendButton: Button = findViewById(R.id.sendData)
        val receiveButton: Button = findViewById(R.id.receiveData)
        val selectButton: Button = findViewById(R.id.selectFile)
        val address: TextInputEditText = findViewById((R.id.address))
        val port: TextInputEditText = findViewById(R.id.port)
        val progressBar : ProgressBar = findViewById(R.id.progressBar)
        var tcpServer : TcpServer? = null
        var t : Future<Unit>? = null
        getIpAddress()

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
                //permission denied
                val permissions = arrayOf(Manifest.permission.INTERNET)
                //show popup to request runtime permission
                requestPermissions(permissions, PERMISSION_CODE)
            }
        }

        if (intentData?.action == Intent.ACTION_SEND) {
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                var items = intentData.clipData!!.itemCount
                var item : ClipData.Item = intentData.clipData!!.getItemAt(items-1)
                data = item.uri
                filePath.text = fileBase + getFileName(data!!, baseContext.contentResolver)
            }
            else {
                Toast.makeText(applicationContext, "This device is not capable of this feature.", Toast.LENGTH_LONG).show()
            }
        }

        selectButton.setOnClickListener {
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
                    getFile()
                }
            } else {
                //system OS is < Marshmallow
                getFile()
            }
        }

        receiveButton.setOnClickListener {
            // Receive file from client
            if (t != null) {
                if (!t!!.isDone) {
                    // Not working
                    t!!.cancel(true)
                }
            }
            t = doAsync {
                if (tcpServer != null){
                    tcpServer!!.close()
                }
                tcpServer = TcpServer(applicationContext, progressBar)
                tcpServer!!.receiveFile()
                uiThread{
                    Toast.makeText(applicationContext, "File received.", Toast.LENGTH_LONG).show()
                }
            }
            Toast.makeText(applicationContext, "Device ready to receive data.", Toast.LENGTH_LONG).show()
        }

        sendButton.setOnClickListener {
            // Send file to sever
            Toast.makeText(applicationContext, "Data Sending", Toast.LENGTH_SHORT).show()
            doAsync {
                val tcpClient = TcpClient(editTextToString(address), editTextToString(port), progressBar)
                val dataSent = tcpClient.sendFile(data!!, baseContext.contentResolver)
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

    @SuppressWarnings("deprecation")
    private fun getIpAddress() {
        val wifiMgr : WifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo : WifiInfo = wifiMgr.connectionInfo
        val ip : Int = wifiInfo.ipAddress
        val ipAddress = Formatter.formatIpAddress(ip)
        deviceAddress.text = addressBase + ipAddress
    }

    private fun getFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        startActivityForResult(intent, REQUEST_CODE)
    }

    companion object {
        //generic code
        private const val REQUEST_CODE = 43
        //Permission code
        private const val PERMISSION_CODE = 1001
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    getFile()
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
         if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            if (intent != null){
                val uri : Uri = intent!!.data!!
                filePath.text = fileBase + getFileName(uri, baseContext.contentResolver)
                data = intent.data
            }
        }
    }

    private fun editTextToString(data : EditText) : String {
        return data.text.toString()
    }
}


class TcpClient(serverAddress: String, serverPort: String, progressBar: ProgressBar, buffer: Int = 1024) {
    private val address : String = serverAddress
    private val port : Int = Integer.parseInt(serverPort)
    private var maxBuffer : Int = buffer
    private var tcpSocket : Socket
    private var fileSize = 0
    private var bytesSent = 0
    private val progressBar = progressBar

    init {
        val ip = InetAddress.getByName(address)
        tcpSocket = Socket(ip!!, port)
    }

    private fun sendData(data: ByteArray):Boolean {
        tcpSocket.outputStream.write(data)
        return true
    }

    private fun sendData(data: String):Boolean {
        tcpSocket.outputStream.write(stringToByteArray(data))
        return true
    }

    private fun sendData(data: Int):Boolean {
        tcpSocket.outputStream.write(intToByteArray(data))
        return true
    }

    private fun receiveData() : ByteArray? {
        val numOfBytes : Int = min(tcpSocket.inputStream.available(), tcpSocket.receiveBufferSize)
        if(numOfBytes > 0) {
            var byteData = ByteArray(numOfBytes)
            tcpSocket.inputStream.read(byteData, 0, numOfBytes)
            return byteData
        }
        return null
    }

    fun sendFile(data : Uri, contentResolver : ContentResolver) : Boolean {
        var cr : ContentResolver = contentResolver
        var inputStream : InputStream? = cr.openInputStream(data)
        val byteData = inputStream!!.readBytes()
        println("byte data size: " + byteData.size)

        progressBar.progress = bytesSent
        var fileName = getFileName(data, contentResolver)
        fileSize = byteData.size

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
            maxBuffer += tcpSocket.sendBufferSize
            var slice = i + maxBuffer - 1
            if (byteData.size < slice) {
                slice = byteData.size -1
            }
            var buf = byteData.sliceArray(IntRange(i, slice))
            if(!sendData(buf)){
                return false
            }
            bytesSent += buf.size
            i += maxBuffer
            updateProgress()
        }
        while (receiveData() == null)
            ;
        tcpSocket.close()
        return true
    }

    private fun updateProgress() {
        var progress  = 0.0f
        if (fileSize > 0) {
            progress = (bytesSent.toFloat()/fileSize.toFloat()) * 100.0f
        }
        progressBar.progress = progress.toInt()
    }

    private fun stringToByteArray(data : String) : ByteArray {
        return data.toByteArray()
    }
    private fun intToByteArray(data : Int) : ByteArray {
        return data.toString().toByteArray()
    }
}

class TcpServer(applicationContext : Context, progressBar: ProgressBar) {
    private val applicationContext = applicationContext
    private val port : Int = 20001
    private var tcpSocket : ServerSocket? = null
    private var socket : Socket? = null
    private val maxBufferSize = 81920
    private var bytesRead = 0
    private var fileSize = 0
    private var fileName = ""
    private val ACKMSG = stringToByteArray("<ACK>")
    private val progressBar = progressBar

    init {
        tcpSocket = ServerSocket(port)
    }

    private fun sendData(data: ByteArray) : Boolean {
        println("Sending Data")
        socket!!.outputStream.write(data)
        return true
    }

    private fun receiveData() : ByteArray? {
        val numOfBytes : Int = min(socket!!.inputStream.available(), min(maxBufferSize, socket!!.receiveBufferSize))
        if(numOfBytes > 0) {
            var byteData = ByteArray(numOfBytes)
            socket!!.inputStream.read(byteData, 0, numOfBytes)
            return byteData
        }
        return null
    }

    fun receiveFile() {
        var fileData : ByteArray? = null
        try {
            println("Waiting for connection")
            progressBar.progress = bytesRead
            socket = tcpSocket!!.accept()
            println("Connected to ${socket!!.inetAddress}:${socket!!.port}")
            while (true) {
                var message : ByteArray = receiveData() ?: continue
                if (fileName == ""){
                    fileName = byteArrayToString(message)
                    println("File Name: $fileName")
                    sendData(stringToByteArray("ACK"))
                }
                else if (fileSize == 0){
                    fileSize = byteArrayToInt(message)
                    println("File Size: $fileSize")
                    sendData(stringToByteArray("ACK"))
                }
                else{
                    if (fileData != null) fileData += message
                    else fileData = message
                    bytesRead += message.size
                    updateProgress()
                    if (bytesRead >= fileSize) {
                        println("File received.")
                        saveFile(fileData, fileName)
                        break
                    }
                }
            }
            sendData(ACKMSG)
            socket!!.close()
        }
        catch (e : UnknownHostException) {
            println("ERROR")
            e.printStackTrace()
        } catch (e : IOException) {
            println("ERROR")
            e.printStackTrace()
        }
    }

    private fun saveFile(fileData : ByteArray, name : String, attempt : Int = 0) : Boolean {
        var path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file : File
        file = if (attempt > 0) {
            val nameTokens = name.split(".").toTypedArray()
            val fileType = ".${nameTokens[nameTokens.size-1]}"
            val newName = name.split(fileType)[0] + "($attempt)" + fileType
            println("name = $newName")
            File(path, newName)
        } else {
            File(path, name)
        }
        return if (!file.exists()) {
            file.createNewFile()
            val fileStream = FileOutputStream(file)
            fileStream.write(fileData)
            val downloadManager = applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.addCompletedDownload(file.name, file.name, true, "*/*", file.absolutePath,file.length(),true)
            println("File written!")
            true
        } else {
            println("File already exists, modify name...")
            saveFile(fileData, "$name", attempt + 1)
        }
    }

    private fun updateProgress() {
        var progress  = 0.0f
        if (fileSize > 0) {
            progress = (bytesRead.toFloat()/fileSize.toFloat()) * 100.0f
        }
        progressBar.progress = progress.toInt()
    }

    fun close() {
        socket!!.close()
    }

    private fun stringToByteArray(data : String) : ByteArray {
        return data.toByteArray()
    }
    private fun byteArrayToString(data : ByteArray) : String {
       return String(data)
    }
    private fun intToByteArray(data : Int) : ByteArray {
        return stringToByteArray(data.toString())
    }
    private fun byteArrayToInt(data : ByteArray) : Int {
        return Integer.parseInt(byteArrayToString(data))
    }
}

fun getFileName(uri : Uri, cr : ContentResolver) : String {
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
