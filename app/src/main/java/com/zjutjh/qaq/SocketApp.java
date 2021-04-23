package com.zjutjh.qaq;

import android.app.Application;
import android.widget.Toast;

import java.net.Socket;

public class SocketApp extends Application {
    private Socket socket = null;
    private Toast toast1 = null;
    private Toast toast2 = null;
    private Toast toast3 = null;


    @Override
    public void onCreate() {
        super.onCreate();
        toast1 = new Toast(getApplicationContext());
        toast1.setDuration(Toast.LENGTH_SHORT);
        toast2 = new Toast(getApplicationContext());
        toast2.setDuration(Toast.LENGTH_SHORT);
        toast3 = new Toast(getApplicationContext());
        toast3.setDuration(Toast.LENGTH_SHORT);
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
