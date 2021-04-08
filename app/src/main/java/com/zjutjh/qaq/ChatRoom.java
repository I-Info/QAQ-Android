package com.zjutjh.qaq;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ChatRoom extends AppCompatActivity {
    String serverIp;
    int serverPort;
    String username;

    Socket socket;

    private BufferedReader bufferedReader = null;
    private OutputStream outputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);


        Intent intent = getIntent();
        serverIp = intent.getStringExtra(MainActivity.EXTRA_SERVER_IP);
        serverPort = intent.getIntExtra(MainActivity.EXTRA_SERVER_PORT, 8080);
        username = intent.getStringExtra(MainActivity.EXTRA_USERNAME);

        //Socket连接线程
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, serverPort);
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = socket.getOutputStream();
                outputStream.write(("{user&;named&;" + username + "}").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.write("{msg&;list}".getBytes(StandardCharsets.UTF_8));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }).start();

        /*主socket接收线程*/
        Runnable socketThread = new Runnable() {
            @Override
            public void run() {
                char[] contentChar = new char[1024];
                String content;
                StringBuilder packageMessage = new StringBuilder();
                boolean startFlag = false;
                try {
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
            }

            private void messageHandler(@NotNull String rawMessage) {
                //处理已经解包后的接收到的消息
                String[] messageArray = rawMessage.split("&;+");
                if (messageArray[0].equals("msg") && messageArray.length == 4) {
                    String user = messageArray[1];
                    String date = messageArray[2];
                    String msg = messageArray[3];
                    //String msg = new String(Base64.getDecoder().decode(messageArray[3]), StandardCharsets.UTF_8);

                    //发送获取的新消息到UI线程..
                    runOnUiThread(() -> {


                    });

                } else if (messageArray[0].equals("msghistory") && messageArray.length >= 4) {
                    ArrayList<String> users = new ArrayList<String>(), dates = new ArrayList<String>(), msgs = new ArrayList<String>();
                    for (int index = 1; index < messageArray.length; index += 3) {
                        users.add(messageArray[index]);
                        dates.add(messageArray[index + 1]);
                        msgs.add(messageArray[index + 2]);
                    }

                    //发送历史消息到UI线程...
                    runOnUiThread(() -> {

                    });
                }
            }

        };
        /*End of Runnable*/
        new Thread(socketThread).start();


    }


    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    //发送按钮方法
    public void sendMessage(View view) {
        if (socket.isConnected()) {
            StringBuilder msgBil = new StringBuilder();
            String msg = ((EditText) findViewById(R.id.messageLine)).getText().toString();
            msgBil.append("{msg&;send&;");
            msgBil.append(msg);
            msgBil.append('}');
            new Thread(() -> {
                try {
                    outputStream.write(msgBil.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }).start();
        }
    }
}