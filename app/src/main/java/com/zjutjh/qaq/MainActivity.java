package com.zjutjh.qaq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "com.zjutjh.qaq.";
    public static final String SERVER_IP = TAG + "SERVER_IP";
    public static final String SERVER_PORT = TAG + "SERVER_PORT";
    public static final String USERNAME = TAG + "USERNAME";
    private String serverIp;
    private int serverPort;
    private String username;

    private EditText textServerIp;
    private EditText textServerPort;
    private EditText textUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textServerIp = (EditText) findViewById(R.id.serverIp);
        textServerPort = (EditText) findViewById(R.id.serverPort);
        textUsername = (EditText) findViewById(R.id.username);

        if (savedInstanceState != null) {
            textServerIp.setText(savedInstanceState.getString(SERVER_IP));
            textServerPort.setText(savedInstanceState.getString(SERVER_PORT));
            textUsername.setText(savedInstanceState.getString(USERNAME));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serverIp != null) {
            textServerIp.setText(serverIp);
            textServerPort.setText(serverPort);
            textUsername.setText(username);
        }
    }

    public void onSubmit(View view) {


        serverIp = textServerIp.getText().toString();
        String portString = textServerPort.getText().toString();
        username = textUsername.getText().toString();

        if (serverIp.equals("")) {
            textServerIp.setError("Please input server IP address");
            return;
        }

        if (portString.equals("")) {
            textServerPort.setError("Please input server port");
            return;
        }

        if (username.equals("") || username.length() > 10) {
            textUsername.setError("Invalid username");
        }

        serverPort = Integer.parseInt(portString);
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
        intent.putExtra(SERVER_IP, serverIp);
        intent.putExtra(SERVER_PORT, serverPort);
        intent.putExtra(USERNAME, username);

        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SERVER_IP, serverIp);
        outState.putInt(SERVER_PORT, serverPort);
        outState.putString(USERNAME, username);
    }
}