package com.zjutjh.qaq;

import android.annotation.SuppressLint;
import android.app.Application;
import android.widget.Toast;

import java.net.Socket;

public class SocketApp extends Application {
    private Socket socket = null;
    private Toast toast1 = null;
    private Toast toast2 = null;
    private Toast toast3 = null;


    @SuppressLint("ShowToast")
    @Override
    public void onCreate() {
        super.onCreate();
        toast1 = Toast.makeText(getApplicationContext(), "toast1", Toast.LENGTH_SHORT);

        toast2 = Toast.makeText(getApplicationContext(), "toast2", Toast.LENGTH_SHORT);

        toast3 = Toast.makeText(getApplicationContext(), "toast3", Toast.LENGTH_SHORT);

    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Toast getToast1() {
        return toast1;
    }

    public Toast getToast2() {
        return toast2;
    }

    public Toast getToast3() {
        return toast3;
    }
}
