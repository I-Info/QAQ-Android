package com.zjutjh.qaq;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SERVER_IP = "com.zjutjh.qaq.SERVER_IP";
    public static final String EXTRA_SERVER_PORT = "com.zjutjh.qaq.SERVER_PORT";
    public static final String EXTRA_USERNAME = "com.zjutjh.qaq.USERNAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSubmit(View view) {
        EditText textServerIp = (EditText) findViewById(R.id.serverIp);
        EditText textServerPort = (EditText) findViewById(R.id.serverPort);
        EditText textUsername = (EditText) findViewById(R.id.username);

        String serverIp = textServerIp.getText().toString();
        String portString = textServerPort.getText().toString();
        String userName = textUsername.getText().toString();

        if (serverIp.equals("")) {
            textServerIp.setError("Please input server IP address");
            return;
        }

        if (portString.equals("")) {
            textServerPort.setError("Please input server port");
            return;
        }

        if (userName.equals("") || userName.length() > 10) {
            textUsername.setError("Invalid username");
        }

        int serverPort = Integer.parseInt(portString);
        if (serverPort < 0 || serverPort > 65535) {
            textServerPort.setError("Invalid server port");
            return;
        }

        String regExp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(regExp);
        if (!pattern.matcher(serverIp).matches()) {
            textServerIp.setError("Invalid IP address");
            return;
        }


        Intent intent = new Intent(this, ChatRoom.class);


    }


}