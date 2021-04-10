package com.zjutjh.qaq;

import android.app.Application;

import java.net.Socket;

public class SocketService extends Application {
    public Socket socket = null;
    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
