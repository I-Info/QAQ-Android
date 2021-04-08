package com.zjutjh.qaq;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.collection.ArraySet;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class SocketThread implements Runnable {
    private Socket socket;
    private Handler uiHandler;
    private Handler revHandler;

    private BufferedReader bufferedReader = null;
    private OutputStream outputStream = null;

    private String serverIp, username;
    private int serverPort;

    public SocketThread(Handler handler, String serverIp, int serverPort, String username) {
        uiHandler = handler;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.username = username;
    }

    public void run() {
        try {
            socket = new Socket(serverIp, serverPort);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = socket.getOutputStream();

            new Thread() {
                @Override
                public void run() {
                    char[] contentChar = new char[1024];
                    String content;
                    StringBuilder message = new StringBuilder();
                    boolean startFlag = false;
                    try {
                        while (bufferedReader.read(contentChar) != -1) {
                            content = String.valueOf(contentChar);
                            for (int index = 0; index < content.length(); index++) {
                                try {
                                    if (content.charAt(index) == '{') {
                                        if (startFlag) {
                                            //异常情况，丢弃之前的message，抛出错误
                                            message = new StringBuilder();
                                            throw new Exception("message without '}' endpoint");
                                        } else {
                                            //message开始
                                            startFlag = true;
                                        }
                                    } else if (content.charAt(index) == '}' && startFlag) {
                                        //message结束
                                        startFlag = false;
                                        messageHandler(message.toString());
                                        message = new StringBuilder();

                                    } else if (startFlag) {
                                        //正常情况将字符添加到message
                                        message.append(content.charAt(index));
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
                    String[] messageArray = rawMessage.split("&;");


                }
            }.start();

        } catch (
                SocketTimeoutException exception) {
            System.out.println(exception.toString());
        } catch (
                Exception exception) {
            exception.printStackTrace();
        }
    }
}
