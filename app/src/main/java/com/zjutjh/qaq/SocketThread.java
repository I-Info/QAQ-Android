package com.zjutjh.qaq;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;

public class SocketThread implements Runnable {
    private Socket socket;
    private Handler uiHandler;
    private static Handler revHandler;

    private BufferedReader bufferedReader = null;
    private OutputStream outputStream = null;

    private final String serverIp, username;
    private final int serverPort;

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
                        Bundle bundle = new Bundle();
                        bundle.putString("user",messageArray[1]);
                        bundle.putString("date", messageArray[2]);
                        bundle.putString("message", messageArray[3]);
                        //String msg = new String(Base64.getDecoder().decode(messageArray[3]), StandardCharsets.UTF_8);
                        //发送获取的新消息到UI线程
                        Message message = Message.obtain();
                        message.what = 0x1;
                        message.setData(bundle);
                        uiHandler.sendMessage(message);
                    } else if (messageArray[0].equals("msghistory") && messageArray.length >= 4) {
                        ArrayList<String> users = new ArrayList<String>(), dates = new ArrayList<String>(), msgs = new ArrayList<String>();
                        for (int index = 1; index < messageArray.length; index += 3) {
                            users.add(messageArray[index]);
                            dates.add(messageArray[index + 1]);
                            msgs.add(messageArray[index + 2]);
                        }
                        //发送历史消息到UI线程
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("users",users);
                        bundle.putStringArrayList("dates",dates);
                        bundle.putStringArrayList("messages",msgs);
                        Message message = Message.obtain();
                        message.setData(bundle);
                        uiHandler.sendMessage(message);
                    }

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
