package com.i1nfo.qaq

import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {
    private var serverIp: String? = null
    private var serverPort = 0
    private var username: String? = null
    private lateinit var hostnameLayout: TextInputLayout
    private lateinit var portLayout: TextInputLayout
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var hostnameInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var usernameInput: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private var thread: Thread? = null
    private lateinit var toast: Toast
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //设置切换动画
        val slideIn = Slide()
        slideIn.slideEdge = Gravity.END
        val slideOut = Slide()
        slideOut.slideEdge = Gravity.START
        window.enterTransition = slideIn
        window.exitTransition = slideOut

        hostnameInput = findViewById(R.id.hostname)
        portInput = findViewById(R.id.port)
        usernameInput = findViewById(R.id.username)

        hostnameLayout = findViewById(R.id.hostnameInputLayout)
        portLayout = findViewById(R.id.portInputLayout)
        usernameLayout = findViewById(R.id.usernameInputLayout)

        hostnameInput.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) checkHostname() }
        portInput.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) checkPort() }
        usernameInput.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) checkUsername() }

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
        toast = (application as SocketApp).toast1
        preferences = getSharedPreferences(TAG, MODE_PRIVATE)
        if (savedInstanceState != null) {
            //从内存读取保存的配置信息
            hostnameInput.setText(savedInstanceState.getString(SERVER_IP))
            portInput.setText(savedInstanceState.getInt(SERVER_PORT).toString())
            usernameInput.setText(savedInstanceState.getString(USERNAME))
        } else if (preferences.getString(SERVER_IP, null) != null) {
            //从存储中读取保存的配置信息
            hostnameInput.setText(preferences.getString(SERVER_IP, null))
            portInput.setText(preferences.getInt(SERVER_PORT, 0).toString())
            usernameInput.setText(preferences.getString(USERNAME, null))
        }
    }

    private fun checkHostname(): Boolean {
        val text = hostnameInput.text
        if (text == null || text.isBlank()) {
            hostnameLayout.error = getString(R.string.require_ip)
            return false
        }
        hostnameLayout.error = null
        return true
    }

    private fun checkPort(): Boolean {
        val text = portInput.text
        if (text == null || text.isBlank()) {
            portLayout.error = getString(R.string.require_port)
            return false
        }
        val port = text.toString().toInt()
        if (port < 0 || port > 65535) {
            return false
        }
        portLayout.error = null
        return true
    }

    private fun checkUsername(): Boolean {
        val text = usernameInput.text
        if (text == null || text.isBlank() || text.length > 10) {
            usernameLayout.error = getString(R.string.invalid_user)
            return false
        }
        usernameLayout.error = null
        return true
    }

    override fun onResume() {
        super.onResume()
        //回到主界面时清除socket
        (application as SocketApp).socket = null
    }

    fun onSubmit(view: View) {
        val imm = applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)

        if (!(checkHostname() && checkPort() && checkUsername())) {
            return
        }

        serverIp = hostnameInput.text.toString()
        val portString = portInput.text.toString()
        username = usernameInput.text.toString()
        serverPort = portString.toInt()

        //预先测试连接:
        if (thread != null && thread!!.isAlive) {
            return
        }
        if ((application as SocketApp).socket != null) {
            return
        }
        progressBar.visibility = View.VISIBLE
        toast.setText(R.string.conn)
        toast.show()
        thread = Thread {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(serverIp, serverPort), 3000)
                (application as SocketApp).socket = socket
                runOnUiThread {

                    //配置信息持久化存储
                    val editor = preferences.edit()
                    editor.putString(SERVER_IP, serverIp)
                    editor.putInt(SERVER_PORT, serverPort)
                    editor.putString(USERNAME, username)
                    editor.apply()
                    progressBar.visibility = View.GONE
                    Log.i(TAG, "Config saved")
                    val intent = Intent(this, ChatRoom::class.java)
                    intent.putExtra(USERNAME, username)
                    startActivity(
                        intent,
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                    )
                }
            } catch (exception: UnknownHostException) {
                exception.printStackTrace()
                runOnUiThread {
                    toast.cancel()
                    hostnameInput.error = getString(R.string.invalid_server_name)
                    progressBar.visibility = View.GONE
                }
            } catch (exception: IOException) {
                //连接失败
                runOnUiThread {
                    toast.cancel()
                    val toast = (application as SocketApp).toast2
                    toast.setText(R.string.error_conn_fail)
                    toast.show()
                    progressBar.visibility = View.GONE
                }
                exception.printStackTrace()
            }
        }
        thread!!.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SERVER_IP, serverIp)
        outState.putInt(SERVER_PORT, serverPort)
        outState.putString(USERNAME, username)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.default_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_about) {
            val intent = Intent(this, About::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val TAG = "com.zjutjh.qaq"
        const val SERVER_IP = "$TAG.SERVER_IP"
        const val SERVER_PORT = "$TAG.SERVER_PORT"
        const val USERNAME = "$TAG.USERNAME"
    }
}