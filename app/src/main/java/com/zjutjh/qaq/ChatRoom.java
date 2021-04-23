package com.zjutjh.qaq;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private String username;

    private Socket socket;
    private Thread thread;

    private BufferedReader bufferedReader = null;
    private OutputStream outputStream = null;

    private final List<QMessage> qMessageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private LinearLayoutManager layoutManager;

    private RecyclerView messageBox;
    private EditText messageLine;
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

        //回车发送
//        messageLine.setOnEditorActionListener((v, actionId, event) -> {
//            sendMessage(v);
//            return false;
//        });

        //Notification Service
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel("Default Channel", getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(notificationChannel);
        builder = new NotificationCompat.Builder(this, "Default Channel");


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
                    socket = ((SocketService) getApplication()).getSocket();

                    //开始连接，连接超时时间3000ms
                    if (socket == null || !socket.isConnected())
                        throw new Exception("Connect error");
                    runOnUiThread(() -> {
                        if (toast != null) {
                            toast.cancel();
                        }
                        toast = Toast.makeText(getApplicationContext(), R.string.conn_success, Toast.LENGTH_SHORT);
                        toast.show();
                    });

                    //初始化输入输出流对象
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outputStream = socket.getOutputStream();

                    //发送QAQ协议用户名称请求
                    outputStream.write(("{user&;named&;" + Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)) + "}").getBytes(StandardCharsets.UTF_8));
                    //发送历史记录获取请求
                    outputStream.write("{msg&;list}".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();


                    //消息读取
                    char[] contentChar = new char[1024];
                    String content;
                    StringBuilder packageMessage = new StringBuilder();
                    boolean startFlag = false;
                    try {
                        //不断读取新消息，出现异常即停止。
                        while (bufferedReader.read(contentChar) != -1) {
                            content = String.valueOf(contentChar);
                            //解包循环，逐字符处理
                            for (int index = 0; index < content.length(); index++) {
                                try {
                                    if (content.charAt(index) == '{') {
                                        if (startFlag) {
                                            //异常情况，丢弃之前的message，抛出错误
                                            packageMessage = new StringBuilder();
                                            throw new Exception("message without '}' endpoint");
                                        } else {
                                            //message开始
                                            startFlag = true;
                                        }
                                    } else if (content.charAt(index) == '}' && startFlag) {
                                        //message结束
                                        startFlag = false;
                                        messageHandler(packageMessage.toString());
                                        packageMessage = new StringBuilder();


                                    } else if (startFlag) {
                                        //正常情况将字符添加到message
                                        packageMessage.append(content.charAt(index));
                                    }

                                } catch (Exception exception) {
                                    //数据流处理中出现异常情况
                                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Caught invalid message pack :(", Toast.LENGTH_SHORT).show());
                                    exception.printStackTrace();
                                }
                            }

                        }
                        throw new IOException("Buffer read end");
                    } catch (
                            IOException exception) {
                        //读取失败,说明连接已断开。
                        exception.printStackTrace();
                        runOnUiThread(() -> {
                            if (toast != null) {
                                toast.cancel();
                            }
                            toast = Toast.makeText(getApplicationContext(), R.string.error_conn_lost, Toast.LENGTH_SHORT);
                            toast.show();
                            finish();
                        });
                    }

                } catch (Exception exception) {
                    //进入activity时socket连接已断开
                    exception.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), R.string.error_conn_error, Toast.LENGTH_SHORT).show();
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
                            qMessageList.add(qMessage);
                            messageAdapter.notifyItemInserted(qMessageList.size() - 1);

                            //发送通知
                            builder.setTicker(qMessage.getContent());
                            builder.setContentTitle(qMessage.getUser());
                            builder.setContentText(qMessage.getContent());
                            builder.setAutoCancel(true);
                            builder.setSmallIcon(R.mipmap.ic_launcher_round);
                            builder.setWhen(System.currentTimeMillis());
                            builder.setDefaults(NotificationCompat.DEFAULT_ALL);

                            Intent intent = new Intent(getApplicationContext(), ChatRoom.class);
                            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            builder.setContentIntent(pi);

                            notificationManager.notify(1, builder.build());


                            //接收新消息的滚动条件
                            if ((qMessageList.size() - 1) - layoutManager.findLastVisibleItemPosition() <= 8)
                                messageBox.smoothScrollToPosition(qMessageList.size() - 1);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            if (toast != null) {
                                toast.cancel();
                                toast = Toast.makeText(getApplicationContext(), R.string.error_invalid_msg, Toast.LENGTH_SHORT);
                                toast.show();
                            }
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
                            System.out.println(rawMessage);
                            runOnUiThread(() -> {
                                if (toast != null) {
                                    toast.cancel();
                                    toast = Toast.makeText(getApplicationContext(), R.string.error_invalid_msg, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            });
                        }
                    }

                    runOnUiThread(() -> {
                        qMessageList.addAll(tempList);
                        messageAdapter.notifyDataSetChanged();
                        messageBox.smoothScrollToPosition(qMessageList.size() - 1);
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
                        if (toast != null) {
                            toast.cancel();
                        }
                        toast = Toast.makeText(getApplicationContext(), R.string.error_conn_lost, Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                    });
                }
            }).start();
            @SuppressLint("SimpleDateFormat") QMessage qMessage = new QMessage(username, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), msg, QMessage.TYPE_RIGHT);
            qMessageList.add(qMessage);
            messageAdapter.notifyItemInserted(qMessageList.size() - 1);
            messageBox.smoothScrollToPosition(qMessageList.size() - 1);
            InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            socket.shutdownInput();//关闭输入流来关闭socket服务线程
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
