package com.zjutjh.qaq;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class ChatRoom extends AppCompatActivity {
    String serverIp;
    int serverPort;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);


        Intent intent = getIntent();
        serverIp = intent.getStringExtra(MainActivity.EXTRA_SERVER_IP);
        serverPort = intent.getIntExtra(MainActivity.EXTRA_SERVER_PORT, 8080);
        username = intent.getStringExtra(MainActivity.EXTRA_USERNAME);

    }


    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}