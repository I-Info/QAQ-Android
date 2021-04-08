package com.zjutjh.qaq;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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
                    char[] contentRaw = new char[1024];
                    String content;
                    String temp = "";
                    int startIndex, endIndex;
                    try {
                        while (bufferedReader.read(contentRaw) != -1) {
                            content = String.valueOf(contentRaw);
                            while (!content.isEmpty()) {
                                //对获取到的字符串进行循环处理
                                try {
                                    if (temp.isEmpty()) {
                                        startIndex = content.indexOf('{');
                                        if ((endIndex = content.indexOf('}')) != -1) {
                                            String messageString = content.substring(startIndex + 1, endIndex);
                                            content = content.substring(endIndex + 1);
                                            //对获取的信息进行处理
                                            messageHandler(messageString);

                                        } else {
                                            //不存在结束标志符，content内容存入temp
                                            temp = content.substring(startIndex + 1);
                                            break;
                                        }
                                    } else {
                                        //存在之前未结束的数据流
                                        startIndex = content.indexOf('{');
                                        endIndex = content.indexOf('}');
                                        if (startIndex == -1 && endIndex == -1) {
                                            //头尾标志符均未出现，content仍然为之前封包的一部分
                                            temp = temp.concat(content);
                                            break;
                                        } else if (endIndex != -1 && endIndex < startIndex) {
                                            //出现了结束标志符，且结束标识符在开始标志符之前(或开始标识符不存在)（正常情况）
                                            String messageString = temp.concat(content.substring(0, endIndex));
                                            temp = "";//temp清空
                                            content = content.substring(endIndex + 1);
                                            //对获取的信息进行处理
                                            messageHandler(messageString);


                                        } else if (endIndex == -1) {
                                            //不存在结束标志符，但存在开始标志符（异常情况），丢弃temp，抛出异常。
                                            temp = "";
                                            throw new Exception("message without '}' endpoint");
                                        }
                                    }
                                } catch (Exception exception) {
                                    //处理字符串处理过程中的异常
                                    System.out.println(exception.getMessage());
                                }

                            }
                        }
                    } catch (
                            IOException exception) {
                        exception.printStackTrace();
                    }
                }

                private void messageHandler(String rawMessage) {
                    //处理已经解包后的接收到的消息

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
