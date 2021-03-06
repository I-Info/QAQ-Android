package com.i1nfo.qaq

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class ChatRoom : AppCompatActivity() {
    private val qMessageList: MutableList<QMessage> = ArrayList()
    private lateinit var username: String
    private var socket: Socket? = null
    private var thread: Thread? = null
    private lateinit var bufferedReader: BufferedReader
    private lateinit var outputStream: OutputStream
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var scrollButton: FloatingActionButton
    private var fabVisibility = false
    private var distance = 0
    private lateinit var messageBox: MessageRecycleView
    private lateinit var messageLine: TextInputEditText
    private lateinit var messageLayout: TextInputLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom)
        val intent = intent
        username = intent.getStringExtra(MainActivity.USERNAME)!!
        messageLine = findViewById(R.id.messageInput)
        messageLayout = findViewById(R.id.messageInputLayout)


        // Init message layout behavior
        messageLayout.setEndIconOnClickListener { sendMessage() }

        scrollButton = findViewById(R.id.scroll_button)
        progressBar = findViewById(R.id.loadingProgress)

        //??????message box (RecyclerView)
        layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true //???????????????????????????
        messageBox = findViewById(R.id.messageBox)
        messageBox.layoutManager = layoutManager
        messageAdapter = MessageAdapter(qMessageList)
        messageBox.adapter = messageAdapter

        //?????????????????????????????????

        messageBox.setOnTouchAction {
            val imm =
                applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            messageLine.clearFocus()
            false
        }

        //????????????????????????
        displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT < 30) {
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        } else {
            displayMetrics.widthPixels = windowManager.currentWindowMetrics.bounds.width()
        }
        //????????????????????????????????????
        scrollButton.animate()
            .translationX((displayMetrics.widthPixels - scrollButton.left).toFloat())

        //??????RecyclerView????????????,???????????????????????????????????????????????????
        messageBox.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!fabVisibility && distance > 200 && qMessageList.size - 1 - layoutManager.findLastVisibleItemPosition() > 0) {
                    fabVisibility = true
                    scrollButton.animate().translationX(0f).interpolator =
                        DecelerateInterpolator(3F)
                    distance = 0
                } else if (fabVisibility && (distance < -200 || qMessageList.size - 1 - layoutManager.findLastVisibleItemPosition() <= 0)) {
                    fabVisibility = false
                    scrollButton.animate()
                        .translationX((displayMetrics.widthPixels - scrollButton.left).toFloat()).interpolator =
                        DecelerateInterpolator(3F)
                    distance = 0
                } else if (qMessageList.size - 1 - layoutManager.findLastVisibleItemPosition() <= 0) {
                    distance = 0
                }
                if (fabVisibility && dy < 0 || !fabVisibility && dy > 0) {
                    distance += dy
                }
            }
        })
        qMessageList.add(QMessage(null, null, null, QMessage.TYPE_BLANK)) //??????
        messageAdapter.notifyItemInserted(0)

        //Notification Service
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            "Default Channel", getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(notificationChannel)
        builder = NotificationCompat.Builder(this, "Default Channel")
        Log.d("MessageSize", qMessageList.size.toString())
    }

    override fun onStart() {
        super.onStart()

        //???????????????????????????????????????
        if (thread != null && thread!!.isAlive) {
            return
        }

        /*???socket????????????*/
        val socketThread: Runnable = object : Runnable {
            override fun run() {
                try {
                    socket = (application as SocketApp).socket

                    //?????????????????????????????????3000ms
                    if (socket == null || !socket!!.isConnected) throw Exception("Connect error")

                    //??????????????????????????????
                    bufferedReader =
                        BufferedReader(InputStreamReader(socket!!.getInputStream()), 1024 * 8)
                    outputStream = socket!!.getOutputStream()

                    //??????QAQ????????????????????????
                    outputStream.write(
                        ("{user&;named&;" + Base64.getEncoder().encodeToString(
                            username.toByteArray(StandardCharsets.UTF_8)
                        ) + "}").toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    //??????????????????????????????
                    outputStream.write("{msg&;list}".toByteArray(StandardCharsets.UTF_8))
                    outputStream.flush()
                    runOnUiThread { progressBar.visibility = View.VISIBLE }

                    //????????????
                    var content = CharArray(1024)
                    var packageMessage = StringBuilder()
                    var startFlag = false
                    try {
                        //????????????????????????????????????????????????
                        while (bufferedReader.read(content) != -1) {
                            //??????????????????????????????
                            for (c in content) {
                                try {
                                    if (c == '{') {
                                        if (startFlag) {
                                            //??????????????????????????????message???????????????
                                            packageMessage = StringBuilder()
                                            throw Exception("message without '}' endpoint")
                                        } else {
                                            //message??????
                                            startFlag = true
                                        }
                                    } else if (c == '}' && startFlag) {
                                        //message??????
                                        startFlag = false
                                        messageHandler(packageMessage.toString())
                                        packageMessage = StringBuilder()
                                    } else if (c == '\u0000') {
//                                        System.out.println(content);
                                        break //??????C??????????????????????????????
                                    } else if (startFlag) {
                                        //??????????????????????????????message
                                        packageMessage.append(c)
                                    }
                                } catch (exception: Exception) {
                                    //????????????????????????????????????
                                    Log.e("Message", exception.toString())
                                }
                            }
                            content = CharArray(1024) //??????
                        }
                        throw IOException("Buffer read end")
                    } catch (exception: IOException) {
                        //????????????,????????????????????????
                        socket!!.close()
                        (application as SocketApp).socket = null
                        runOnUiThread {
                            Toast.makeText(
                                baseContext,
                                R.string.error_conn_lost,
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                } catch (exception: Exception) {
                    //??????activity???socket???????????????
                    Log.e("QAQMessenger", exception.toString())
                    runOnUiThread {
                        Toast.makeText(baseContext, R.string.error_conn_error, Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                }
            }

            private fun messageHandler(rawMessage: String) {
                //??????????????????????????????????????????
                val messageArray = rawMessage.split("&;")
                Log.d("GetMessage", messageArray.toString())
                if (messageArray[0] == "msg" && messageArray.size == 4) {
                    try {
                        val qMessage = QMessage(
                            String(
                                Base64.getDecoder().decode(
                                    messageArray[1]
                                ), StandardCharsets.UTF_8
                            ),
                            messageArray[2],
                            String(
                                Base64.getDecoder().decode(messageArray[3]),
                                StandardCharsets.UTF_8
                            ), QMessage.TYPE_LEFT
                        )
                        runOnUiThread {
                            var size = qMessageList.size
                            qMessageList.add(size - 1, qMessage)
                            size++
                            messageAdapter.notifyItemInserted(size - 2)
                            messageAdapter.notifyItemRangeChanged(size - 2, 2)
                            //??????????????????????????????
                            val pos = size - 2 - layoutManager.findLastVisibleItemPosition()
                            if (pos <= 8) messageBox.smoothScrollToPosition(size - 1) else if (!fabVisibility) {
                                scrollButton.animate().translationX(0f).interpolator =
                                    DecelerateInterpolator(3F)
                                fabVisibility = true
                                distance = 0
                            }

                            //????????????
                            builder.setTicker(qMessage.content)
                            builder.setContentTitle(qMessage.user)
                            builder.setContentText(qMessage.content)
                            builder.setAutoCancel(true)
                            builder.setSmallIcon(R.mipmap.ic_launcher_round)
                            builder.setWhen(System.currentTimeMillis())
                            builder.setDefaults(NotificationCompat.DEFAULT_ALL)

                            //????????????
                            val intent = Intent(applicationContext, ChatRoom::class.java)
                            val pi = PendingIntent.getActivity(
                                applicationContext,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE
                            )
                            builder.setContentIntent(pi)
                            notificationManager.notify(1, builder.build())
                        }
                    } catch (e: Exception) {
                        Log.e("QAQMessenger", e.toString())
                        runOnUiThread {
                            Toast.makeText(
                                baseContext,
                                R.string.error_invalid_msg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else if (messageArray[0] == "msghistory" && messageArray.size >= 4) {
                    val tempList: MutableList<QMessage> = ArrayList()
                    var index = 1
                    while (index < messageArray.size) {
                        var qMessage: QMessage
                        try {
                            qMessage =
                                if (messageArray[index] == Base64.getEncoder().encodeToString(
                                        username.toByteArray(StandardCharsets.UTF_8)
                                    )
                                ) {
                                    QMessage(
                                        String(
                                            Base64.getDecoder().decode(messageArray[index]),
                                            StandardCharsets.UTF_8
                                        ),
                                        messageArray[index + 1],
                                        String(
                                            Base64.getDecoder().decode(messageArray[index + 2]),
                                            StandardCharsets.UTF_8
                                        ), QMessage.TYPE_RIGHT
                                    )
                                } else {
                                    QMessage(
                                        String(
                                            Base64.getDecoder().decode(messageArray[index]),
                                            StandardCharsets.UTF_8
                                        ),
                                        messageArray[index + 1],
                                        String(
                                            Base64.getDecoder().decode(messageArray[index + 2]),
                                            StandardCharsets.UTF_8
                                        ), QMessage.TYPE_LEFT
                                    )
                                }
                            tempList.add(qMessage)
                        } catch (e: Exception) {
                            Log.e("QAQMessenger", e.toString())
                            runOnUiThread {
                                Toast.makeText(
                                    baseContext,
                                    R.string.error_invalid_msg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }
                        index += 3
                    }
                    runOnUiThread {
                        qMessageList.addAll(tempList)
                        qMessageList.add(QMessage(null, null, null, QMessage.TYPE_BLANK))
                        messageAdapter.notifyItemRangeInserted(
                            qMessageList.size - (tempList.size + 1),
                            tempList.size + 1
                        )
                        messageBox.scrollToPosition(qMessageList.size - 1)

                        progressBar.visibility = View.INVISIBLE
                    }
                }
            }
        }
        thread = Thread(socketThread).apply { start() }
    }

    //??????????????????
    private fun sendMessage() {
        if (socket != null && socket!!.isConnected) {
            val msgBil = StringBuilder()
            val msg = messageLine.text
            if (msg == null || msg.isBlank()) {
                return
            } else if (msg.length >= 500) {
                Toast.makeText(baseContext, R.string.error_msg_too_long, Toast.LENGTH_SHORT).show()
                return
            }

            messageLine.text = null

            msgBil.append("{msg&;send&;")
            msgBil.append(
                Base64.getEncoder()
                    .encodeToString(msg.toString().toByteArray(StandardCharsets.UTF_8))
            )
            msgBil.append('}')
            //??????????????????
            Thread {
                try {
                    outputStream.write(msgBil.toString().toByteArray(StandardCharsets.UTF_8))
                    outputStream.flush()
                } catch (exception: IOException) {
                    //????????????????????????????????????
                    exception.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(baseContext, R.string.error_conn_lost, Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                }
            }.start()
            val qMessage = QMessage(
                username, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(
                    Date()
                ), msg.toString(), QMessage.TYPE_RIGHT
            )
            var size = qMessageList.size
            qMessageList.add(size - 1, qMessage)
            ++size
            messageAdapter.notifyItemInserted(size - 2)
            messageAdapter.notifyItemRangeChanged(size - 2, 2)
            messageBox.smoothScrollToPosition(size - 1)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        //??????????????????
//        if (item.itemId == android.R.id.home) {
//            finish()
//            return true
//        }
        if (item.itemId == R.id.item_about) {
            val intent = Intent(this, About::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (qMessageList.size > 0) {
            messageBox.smoothScrollToPosition(qMessageList.size - 1)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.default_menu, menu)
        return true
    }

    fun scrollToBottom(view: View?) {
        messageBox.smoothScrollToPosition(qMessageList.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            socket!!.shutdownInput() //????????????????????????socket????????????
        } catch (exception: Exception) {
            Log.e("OnDestroy", exception.toString())
        }
    }
}