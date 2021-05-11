package com.zjutjh.qaq;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;


public class ChatRoom extends AppCompatActivity {
    private final List<QMessage> qMessageList = new ArrayList<>();
    private String username;
    private Socket socket;
    private Thread thread;
    private BufferedReader bufferedReader = null;
    private OutputStream outputStream = null;
    private MessageAdapter messageAdapter;
    private LinearLayoutManager layoutManager;
    private DisplayMetrics displayMetrics;
    private FloatingActionButton scrollButton;
    private boolean visible = false;
    private int distance = 0;

    private RecyclerView messageBox;
    private EditText messageLine;
    private ProgressBar progressBar;
    private Toast toast;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);


        Intent intent = getIntent();
        username = intent.getStringExtra(MainActivity.USERNAME);

        messageLine = findViewById(R.id.messageLine);
        scrollButton = findViewById(R.id.scroll_button);
        progressBar = findViewById(R.id.loadingProgress);

        toast = ((SocketApp) getApplication()).getToast2();

        //设置message box (RecyclerView)
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);//软键盘弹出自动上移

        messageBox = findViewById(R.id.messageBox);
        messageBox.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(qMessageList);
        messageBox.setAdapter(messageAdapter);

        //点击屏幕上部关闭软键盘
        messageBox.setOnTouchListener((v, event) -> {
            InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            return false;
        });

        //获取屏幕宽度信息
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //初始状态自动滚动按钮隐藏
        scrollButton.animate().translationX(displayMetrics.widthPixels - scrollButton.getLeft());

        //监听RecyclerView滚动事件,根据滚动数据来控制自动滚动按钮状态
        messageBox.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!visible && distance > 200 && (qMessageList.size() - 1) - layoutManager.findLastVisibleItemPosition() > 0) {
                    visible = true;
                    scrollButton.animate().translationX(0).setInterpolator(new DecelerateInterpolator(3));
                    distance = 0;
                } else if (visible && (distance < -200 || (qMessageList.size() - 1) - layoutManager.findLastVisibleItemPosition() <= 0)) {
                    visible = false;
                    scrollButton.animate().translationX(displayMetrics.widthPixels - scrollButton.getLeft()).setInterpolator(new DecelerateInterpolator(3));
                    distance = 0;
                } else if ((qMessageList.size() - 1) - layoutManager.findLastVisibleItemPosition() <= 0) {
                    distance = 0;
                }

                if ((visible && dy < 0) || (!visible && dy > 0)) {
                    distance += dy;
                }
            }
        });

        qMessageList.add(new
                QMessage(null, null, null, QMessage.TYPE_BLANK));//占位
        messageAdapter.notifyItemInserted(0);

        //Notification Service
        notificationManager = (NotificationManager)

                getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel notificationChannel = new NotificationChannel("Default Channel", getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(notificationChannel);
        builder = new NotificationCompat.Builder(this, "Default Channel");
        Log.d("size", String.valueOf(qMessageList.size()));
    }


    @Override
    protected void onStart() {
        super.onStart();

        //如果线程已启动，则不做操作
        if (thread != null && thread.isAlive()) {
            return;
        }

        /*主socket接收线程*/
        Runnable socketThread = new Runnable() {
            @Override
            public void run() {
                try {
                    socket = ((SocketApp) getApplication()).getSocket();

                    //开始连接，连接超时时间3000ms
                    if (socket == null || !socket.isConnected())
                        throw new Exception("Connect error");
                    runOnUiThread(() -> {
                        ((SocketApp) getApplication()).getToast1().cancel();
                        toast.setText(R.string.conn_success);
                        toast.show();
                    });

                    //初始化输入输出流对象
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()), 1024 * 8);
                    outputStream = socket.getOutputStream();

                    //发送QAQ协议用户名称请求
                    outputStream.write(("{user&;named&;" + Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)) + "}").getBytes(StandardCharsets.UTF_8));
                    //发送历史记录获取请求
                    outputStream.write("{msg&;list}".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();


                    //消息读取
                    char[] content = new char[1024];

                    StringBuilder packageMessage = new StringBuilder();
                    boolean startFlag = false;
                    try {
                        //不断读取新消息，出现异常即停止。
                        while (bufferedReader.read(content) != -1) {
                            //解包循环，逐字符处理
                            for (char c : content) {
                                try {
                                    if (c == '{') {
                                        if (startFlag) {
                                            //异常情况，丢弃之前的message，抛出错误
                                            packageMessage = new StringBuilder();
                                            throw new Exception("message without '}' endpoint");
                                        } else {
                                            //message开始
                                            startFlag = true;
                                        }
                                    } else if (c == '}' && startFlag) {
                                        //message结束
                                        startFlag = false;
                                        messageHandler(packageMessage.toString());
                                        packageMessage = new StringBuilder();

                                    } else if (c == '\0') {
//                                        System.out.println(content);
                                        break;//到达C字符串末尾，退出循环
                                    } else if (startFlag) {
                                        //正常情况将字符添加到message
                                        packageMessage.append(c);
                                    }

                                } catch (Exception exception) {
                                    //数据流处理中出现异常情况
                                    runOnUiThread(() -> {
                                        toast.setText("Caught invalid message pack :(");
                                        toast.show();
                                    });
                                    exception.printStackTrace();
                                }
                            }
                            content = new char[1024];//清空
                        }
                        throw new IOException("Buffer read end");
                    } catch (
                            IOException exception) {
                        //读取失败,说明连接已断开。
                        exception.printStackTrace();
                        socket.close();
                        ((SocketApp) getApplication()).setSocket(null);
                        runOnUiThread(() -> {
                            toast.setText(R.string.error_conn_lost);
                            toast.show();
                            finish();
                        });
                    }

                } catch (Exception exception) {
                    //进入activity时socket连接已断开
                    exception.printStackTrace();
                    runOnUiThread(() -> {
                        Toast toast = ((SocketApp) getApplication()).getToast3();
                        toast.setText(R.string.error_conn_error);
                        toast.show();
                        finish();
                    });
                }
            }

            private void messageHandler(@NotNull String rawMessage) {
                //处理已经解包后的接收到的消息
                String[] messageArray = rawMessage.split("&;+");
                if (messageArray[0].equals("msg") && messageArray.length == 4) {
                    try {
                        QMessage qMessage = new QMessage(new String(Base64.getDecoder().decode(messageArray[1]), StandardCharsets.UTF_8),
                                messageArray[2],
                                new String(Base64.getDecoder().decode(messageArray[3]), StandardCharsets.UTF_8), QMessage.TYPE_LEFT);

                        runOnUiThread(() -> {
                            int size = qMessageList.size();
                            qMessageList.add(size - 1, qMessage);
                            size++;
                            messageAdapter.notifyItemInserted(size - 2);
                            messageAdapter.notifyItemRangeChanged(size - 2, 2);
                            //接收新消息的滚动条件
                            int pos = (size - 2) - layoutManager.findLastVisibleItemPosition();
                            if (pos <= 8)
                                messageBox.smoothScrollToPosition(size - 1);
                            else if (!visible) {
                                scrollButton.animate().translationX(0).setInterpolator(new DecelerateInterpolator(3));
                                visible = true;
                                distance = 0;
                            }

                            //发送通知
                            builder.setTicker(qMessage.getContent());
                            builder.setContentTitle(qMessage.getUser());
                            builder.setContentText(qMessage.getContent());
                            builder.setAutoCancel(true);
                            builder.setSmallIcon(R.mipmap.ic_launcher_round);
                            builder.setWhen(System.currentTimeMillis());
                            builder.setDefaults(NotificationCompat.DEFAULT_ALL);

                            //点击通知
                            Intent intent = new Intent(getApplicationContext(), ChatRoom.class);
                            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            builder.setContentIntent(pi);

                            notificationManager.notify(1, builder.build());

                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("raw", rawMessage);
                        runOnUiThread(() -> {
                            Toast toast = ((SocketApp) getApplication()).getToast3();
                            toast.setText(R.string.error_invalid_msg);
                            toast.show();
                        });
                    }


                } else if (messageArray[0].equals("msghistory") && messageArray.length >= 4) {
                    List<QMessage> tempList = new ArrayList<>();
                    for (int index = 1; index < messageArray.length; index += 3) {
                        QMessage qMessage;
                        try {
                            if (messageArray[index].equals(Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)))) {
                                qMessage = new QMessage(new String(Base64.getDecoder().decode(messageArray[index]), StandardCharsets.UTF_8),
                                        messageArray[index + 1],
                                        new String(Base64.getDecoder().decode(messageArray[index + 2]), StandardCharsets.UTF_8), QMessage.TYPE_RIGHT);
                            } else {
                                qMessage = new QMessage(new String(Base64.getDecoder().decode(messageArray[index]), StandardCharsets.UTF_8),
                                        messageArray[index + 1],
                                        new String(Base64.getDecoder().decode(messageArray[index + 2]), StandardCharsets.UTF_8), QMessage.TYPE_LEFT);
                            }
                            tempList.add(qMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("raw", rawMessage);
                            runOnUiThread(() -> {
                                Toast toast = ((SocketApp) getApplication()).getToast3();
                                toast.setText(R.string.error_invalid_msg);
                                toast.show();
                            });
                        }
                    }

                    runOnUiThread(() -> {
                        qMessageList.addAll(tempList);
                        qMessageList.add(new QMessage(null, null, null, QMessage.TYPE_BLANK));
                        messageAdapter.notifyDataSetChanged();
                        messageBox.scrollToPosition(qMessageList.size() - 1);
                        progressBar.setVisibility(View.GONE);
                    });

                }
            }

        };
        thread = new Thread(socketThread);
        thread.start();

    }

    //发送按钮方法
    public void sendMessage(View view) {
        if (socket != null && socket.isConnected()) {
            StringBuilder msgBil = new StringBuilder();
            String msg = messageLine.getText().toString().trim();
            if (msg.isEmpty()) {
                return;
            }
            //消息长度限制
            if (msg.length() >= 500) {
                toast.setText(R.string.error_msg_too_long);
                toast.show();
                return;
            }
            messageLine.setText("");
            msgBil.append("{msg&;send&;");
            msgBil.append(Base64.getEncoder().encodeToString(msg.getBytes(StandardCharsets.UTF_8)));
            msgBil.append('}');
            //消息推送线程
            new Thread(() -> {
                try {
                    outputStream.write(msgBil.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException exception) {
                    //发送失败，说明已断开连接
                    exception.printStackTrace();
                    runOnUiThread(() -> {
                        toast.setText(R.string.error_conn_lost);
                        toast.show();
                        finish();
                    });
                }
            }).start();
            @SuppressLint("SimpleDateFormat") QMessage qMessage = new QMessage(username, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), msg, QMessage.TYPE_RIGHT);
            int size = qMessageList.size();
            qMessageList.add(size - 1, qMessage);
            size++;
            messageAdapter.notifyItemInserted(size - 2);
            messageAdapter.notifyItemRangeChanged(size - 2, 2);
            messageBox.smoothScrollToPosition(size - 1);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //重写返回按钮
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.item_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (qMessageList.size() > 0) {
            messageBox.smoothScrollToPosition(qMessageList.size() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.default_menu, menu);
        return true;
    }

    public void scrollToBottom(View view) {
        messageBox.smoothScrollToPosition(qMessageList.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            socket.shutdownInput();//关闭输入流来关闭socket服务线程
        } catch (Exception exception) {
            exception.printStackTrace();
            Toast toast = ((SocketApp) getApplication()).getToast3();
            toast.setText("Socket already closed");
            toast.show();
        }
    }
}
