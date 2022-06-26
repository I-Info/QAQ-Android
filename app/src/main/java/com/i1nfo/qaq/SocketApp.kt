package com.i1nfo.qaq

import android.annotation.SuppressLint
import android.app.Application
import android.widget.Toast
import java.net.Socket

class SocketApp : Application() {
    var socket: Socket? = null
    lateinit var toast1: Toast
        private set
    lateinit var toast2: Toast
        private set
    lateinit var toast3: Toast
        private set
    lateinit var toast4: Toast
        private set

    @SuppressLint("ShowToast")
    override fun onCreate() {
        super.onCreate()
        toast1 = Toast.makeText(applicationContext, "toast1", Toast.LENGTH_SHORT)
        toast2 = Toast.makeText(applicationContext, "toast2", Toast.LENGTH_SHORT)
        toast3 = Toast.makeText(applicationContext, "toast3", Toast.LENGTH_SHORT)
        toast4 = Toast.makeText(applicationContext, "toast4", Toast.LENGTH_SHORT)
    }
}