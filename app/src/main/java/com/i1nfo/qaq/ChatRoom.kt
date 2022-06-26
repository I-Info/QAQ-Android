package com.i1nfo.qaq

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var toast: Toast
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
        messageLayout.addOnEditTextAttachedListener { textInputLayout: TextInputLayout ->
            textInputLayout.isEndIconVisible = false
            textInputLayout.setEndIconOnClickListener { sendMessage() }
        }
        messageLine.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                messageLayout.isEndIconVisible =
                    !(messageLine.text == null || messageLine.text!!.isEmpty())
            }

            override fun afterTextChanged(s: Editable) {}
        })
        scrollButton = findViewById(R.id.scroll_button)
        progressBar = findViewById(R.id.loadingProgress)
        toast = (application as SocketApp).toast2

        //设置message box (RecyclerView)
        layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true //软键盘弹出自动上移
        messageBox = findViewById(R.id.messageBox)
        messageBox.layoutManager = layoutManager
        messageAdapter = MessageAdapter(qMessageList)
        messageBox.adapter = messageAdapter

        //点击屏幕上部关闭软键盘

        messageBox.setOnTouchAction {
            val imm =
                applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            messageLine.clearFocus()
            false
        }

        //获取屏幕宽度信息
        displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT < 30) {
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        } else {
            displayMetrics.widthPixels = windowManager.currentWindowMetrics.bounds.width()
        }
        //初始状态自动滚动按钮隐藏
        scrollButton.animate()
            .translationX((displayMetrics.widthPixels - scrollButton.left).toFloat())

        //监听RecyclerView滚动事件,根据滚动数据来控制自动滚动按钮状态
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
        qMessageList.add(QMessage(null, null, null, QMessage.TYPE_BLANK)) //占位
        messageAdapter.notifyItemInserted(0)

        //Notification Service
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            "Default Channel", getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(notificationChannel)
        builder = NotificationCompat.Builder(this, "Default Channel")
        Log.d("size", qMessageList.size.toString())
    }

    override fun onStart() {
        super.onStart()

        //如果线程已启动，则不做操作
        if (thread != null && thread!!.isAlive) {
            return
        }

        /*主socket接收线程*/
        val socketThread: Runnable = object : Runnable {
            override fun run() {
                try {
                    socket = (application as SocketApp).socket

                    //开始连接，连接超时时间3000ms
                    if (socket == null || !socket!!.isConnected) throw Exception("Connect error")
                    runOnUiThread {
                        (application as SocketApp).toast1.cancel()
                        toast.setText(R.string.conn_success)
                        toast.show()
                    }

                    //初始化输入输出流对象
                    bufferedReader =
                        BufferedReader(InputStreamReader(socket!!.getInputStream()), 1024 * 8)
                    outputStream = socket!!.getOutputStream()

                    //发送QAQ协议用户名称请求
                    outputStream.write(
                        ("{user&;named&;" + Base64.getEncoder().encodeToString(
                            username.toByteArray(StandardCharsets.UTF_8)
                        ) + "}").toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    //发送历史记录获取请求
                    outputStream.write("{msg&;list}".toByteArray(StandardCharsets.UTF_8))
                    outputStream.flush()
                    runOnUiThread { progressBar.visibility = View.VISIBLE }

                    //消息读取
                    var content = CharArray(1024)
                    var packageMessage = StringBuilder()
                    var startFlag = false
                    try {
                        //不断读取新消息，出现异常即停止。
                        while (bufferedReader.read(content) != -1) {
                            //解包循环，逐字符处理
                            for (c in content) {
                                try {
                                    if (c == '{') {
                                        if (startFlag) {
                                            //异常情况，丢弃之前的message，抛出错误
                                            packageMessage = StringBuilder()
                                            throw Exception("message without '}' endpoint")
                                        } else {
                                            //message开始
                                            startFlag = true
                                        }
                                    } else if (c == '}' && startFlag) {
                                        //message结束
                                        startFlag = false
                                        messageHandler(packageMessage.toString())
                                        packageMessage = StringBuilder()
                                    } else if (c == '\u0000') {
//                                        System.out.println(content);
                                        break //到达C字符串末尾，退出循环
                                    } else if (startFlag) {
                                        //正常情况将字符添加到message
                                        packageMessage.append(c)
                                    }
                                } catch (exception: Exception) {
                                    //数据流处理中出现异常情况
                                    runOnUiThread {
                                        toast.setText("Caught invalid message pack :(")
                                        toast.show()
                                    }
                                    exception.printStackTrace()
                                }
                            }
                            content = CharArray(1024) //清空
                        }
                        throw IOException("Buffer read end")
                    } catch (exception: IOException) {
                        //读取失败,说明连接已断开。
                        socket!!.close()
                        (application as SocketApp).socket = null
                        runOnUiThread {
                            val toast = (application as SocketApp).toast3
                            toast.setText(R.string.error_conn_lost)
                            toast.show()
                            finish()
                        }
                    }
                } catch (exception: Exception) {
                    //进入activity时socket连接已断开
                    exception.printStackTrace()
                    runOnUiThread {
                        val toast = (application as SocketApp).toast3
                        toast.setText(R.string.error_conn_error)
                        toast.show()
                        finish()
                    }
                }
            }

            private fun messageHandler(rawMessage: String) {
                //处理已经解包后的接收到的消息
                val messageArray = rawMessage.split("&;")
                Log.d("Message", messageArray.toString())
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
                            //接收新消息的滚动条件
                            val pos = size - 2 - layoutManager.findLastVisibleItemPosition()
                            if (pos <= 8) messageBox.smoothScrollToPosition(size - 1) else if (!fabVisibility) {
                                scrollButton.animate().translationX(0f).interpolator =
                                    DecelerateInterpolator(3F)
                                fabVisibility = true
                                distance = 0
                            }

                            //发送通知
                            builder.setTicker(qMessage.content)
                            builder.setContentTitle(qMessage.user)
                            builder.setContentText(qMessage.content)
                            builder.setAutoCancel(true)
                            builder.setSmallIcon(R.mipmap.ic_launcher_round)
                            builder.setWhen(System.currentTimeMillis())
                            builder.setDefaults(NotificationCompat.DEFAULT_ALL)

                            //点击通知
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
                        e.printStackTrace()
                        Log.d("raw", rawMessage)
                        runOnUiThread {
                            val toast = (application as SocketApp).toast3
                            toast.setText(R.string.error_invalid_msg)
                            toast.show()
                        }
                    }
                } else if (messageArray[0] == "msghistory" && messageArray.size >= 4) {
                    Log.d("Protocol", messageArray.toString())
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
                            e.printStackTrace()
                            Log.d("raw", rawMessage)
                            runOnUiThread {
                                val toast = (application as SocketApp).toast3
                                toast.setText(R.string.error_invalid_msg)
                                toast.show()
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

                        //获取历史记录成功，也是连接成功的标志
                        val toast = (application as SocketApp).toast4
                        toast.setText(R.string.get_success)
                        toast.show()
                        progressBar.visibility = View.INVISIBLE
                    }
                }
            }
        }
        thread = Thread(socketThread).apply { start() }
    }

    //发送按钮方法
    private fun sendMessage() {
        if (socket != null && socket!!.isConnected) {
            val msgBil = StringBuilder()
            val msg = messageLine.text
            if (msg == null || msg.isBlank()) {
                return
            } else if (msg.length >= 500) {
                toast.setText(R.string.error_msg_too_long)
                toast.show()
                return
            }

            msg.clear()

            Log.d("InputBox", String.format("Set height: %s", messageLine.height))

            msgBil.append("{msg&;send&;")
            msgBil.append(
                Base64.getEncoder()
                    .encodeToString(msg.toString().toByteArray(StandardCharsets.UTF_8))
            )
            msgBil.append('}')
            //消息推送线程
            Thread {
                try {
                    outputStream.write(msgBil.toString().toByteArray(StandardCharsets.UTF_8))
                    outputStream.flush()
                } catch (exception: IOException) {
                    //发送失败，说明已断开连接
                    exception.printStackTrace()
                    runOnUiThread {
                        toast.setText(R.string.error_conn_lost)
                        toast.show()
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
        //重写返回按钮
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
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
            socket!!.shutdownInput() //关闭输入流来关闭socket服务线程
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
}