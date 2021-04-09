package com.zjutjh.qaq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class ChatRoom extends AppCompatActivity {
    String serverIp;
    int serverPort;
    String username;

    Socket socket;

    private BufferedReader bufferedReader = null;
    private OutputStream outputStream = null;

    private List<QMessage> qMessageList = new ArrayList<>();
    private MessageAdapter messageAdapter;

    RecyclerView messageBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);


        Intent intent = getIntent();
        serverIp = intent.getStringExtra(MainActivity.SERVER_IP);
        serverPort = intent.getIntExtra(MainActivity.SERVER_PORT, 8080);
        username = intent.getStringExtra(MainActivity.USERNAME);

        //设置message box
        messageBox = findViewById(R.id.messageBox);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        messageBox.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(qMessageList);
        messageBox.setAdapter(messageAdapter);



        /*主socket接收线程*/
        Runnable socketThread = new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(serverIp, serverPort), 3000);

                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outputStream = socket.getOutputStream();
                    outputStream.write(("{user&;named&;" + username + "}").getBytes(StandardCharsets.UTF_8));
                    outputStream.write("{msg&;list}".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();

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
                                    System.out.println(exception.getMessage());
                                }
                            }

                        }
                    } catch (
                            IOException exception) {
                        exception.printStackTrace();
                    }

                } catch (Exception exception) {
                    exception.printStackTrace();
                    runOnUiThread(() -> {
//                        Toast.makeText();
                        finish();
                    });
                }

            }

            private void messageHandler(@NotNull String rawMessage) {
                //处理已经解包后的接收到的消息
                String[] messageArray = rawMessage.split("&;+");
                if (messageArray[0].equals("msg") && messageArray.length == 4) {
                    QMessage qMessage = new QMessage(messageArray[1], messageArray[2], messageArray[3], QMessage.TYPE_LEFT);
                    //String msg = new String(Base64.getDecoder().decode(messageArray[3]), StandardCharsets.UTF_8);


                    qMessageList.add(qMessage);
                    messageAdapter.notify();
                    runOnUiThread(() -> {
                        messageBox.scrollToPosition(qMessageList.size() - 1);
                    });


                } else if (messageArray[0].equals("msghistory") && messageArray.length >= 4) {
                    List<QMessage> tempList = new ArrayList<>();
                    for (int index = 1; index < messageArray.length; index += 3) {
                        QMessage qMessage = new QMessage(messageArray[index], messageArray[index + 1], messageArray[index + 2], QMessage.TYPE_LEFT);
                        tempList.add(qMessage);
                    }


                    qMessageList.addAll(tempList);
                    messageAdapter.notify();
                    runOnUiThread(() -> {
                        messageBox.scrollToPosition(qMessageList.size() - 1);
                    });

                }
            }

        };
        new Thread(socketThread).start();

    }


    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    //发送按钮方法
    public void sendMessage(View view) {
        if (socket != null) {
            EditText messageLine = (EditText) findViewById(R.id.messageLine);
            StringBuilder msgBil = new StringBuilder();
            String msg = messageLine.getText().toString();
            messageLine.setText("");
            msgBil.append("{msg&;send&;");
            msgBil.append(msg);
            msgBil.append('}');
            //消息推送线程
            new Thread(() -> {
                try {
                    outputStream.write(msgBil.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
            QMessage qMessage = new QMessage(username, "2020", msg, QMessage.TYPE_RIGHT);
            qMessageList.add(qMessage);
            messageAdapter.notifyItemInserted(qMessageList.size() - 1);
            messageBox.scrollToPosition(qMessageList.size() - 1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //重写返回按钮
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }
}