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

public class SocketThread implements Runnable{
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
            socket = new Socket(serverIp,serverPort);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = socket.getOutputStream();


        } catch (SocketTimeoutException exception) {
            System.out.println(exception.toString());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
