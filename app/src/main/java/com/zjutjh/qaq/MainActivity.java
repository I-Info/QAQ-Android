package com.zjutjh.qaq;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "com.zjutjh.qaq";
    public static final String SERVER_IP = TAG + ".SERVER_IP";
    public static final String SERVER_PORT = TAG + ".SERVER_PORT";
    public static final String USERNAME = TAG + ".USERNAME";
    private String serverIp;
    private int serverPort;
    private String username;

    private EditText textServerIp;
    private EditText textServerPort;
    private EditText textUsername;

    private Thread thread;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //设置切换动画
        Slide slideIn = new Slide();
        slideIn.setSlideEdge(Gravity.END);
        Slide slideOut = new Slide();
        slideOut.setSlideEdge(Gravity.START);
        getWindow().setEnterTransition(slideIn);
        getWindow().setExitTransition(slideOut);

        textServerIp = findViewById(R.id.serverIp);
        textServerPort = findViewById(R.id.serverPort);
        textUsername = findViewById(R.id.username);

        preferences = getSharedPreferences(TAG, MODE_PRIVATE);


        if (savedInstanceState != null) {
            //从内存读取保存的配置信息
            textServerIp.setText(savedInstanceState.getString(SERVER_IP));
            textServerPort.setText(String.valueOf(savedInstanceState.getInt(SERVER_PORT)));
            textUsername.setText(savedInstanceState.getString(USERNAME));
        } else if (preferences.getString(SERVER_IP, null) != null) {
            //从存储中读取保存的配置信息
            textServerIp.setText(preferences.getString(SERVER_IP, null));
            textServerPort.setText(String.valueOf(preferences.getInt(SERVER_PORT, 0)));
            textUsername.setText(preferences.getString(USERNAME, null));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //回到主界面时清除socket
        ((SocketService) getApplication()).setSocket(null);
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
            return;
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

        //预先测试连接:
        if (thread != null && thread.isAlive()) {
            return;
        }

        Toast.makeText(getApplicationContext(), "Connecting..", Toast.LENGTH_SHORT).show();
        thread = new Thread(() -> {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(serverIp, serverPort), 3000);

                ((SocketService) getApplication()).setSocket(socket);
                runOnUiThread(() -> {
                    //配置信息持久化存储
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(SERVER_IP, serverIp);
                    editor.putInt(SERVER_PORT, serverPort);
                    editor.putString(USERNAME, username);
                    editor.apply();
                    Log.i(TAG, "信息保存成功");
                    Intent intent = new Intent(this, ChatRoom.class);
                    intent.putExtra(USERNAME, username);
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
                });
            } catch (IOException exception) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Connect failed.", Toast.LENGTH_SHORT).show());
                exception.printStackTrace();
            }

        });
        thread.start();

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SERVER_IP, serverIp);
        outState.putInt(SERVER_PORT, serverPort);
        outState.putString(USERNAME, username);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.default_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}