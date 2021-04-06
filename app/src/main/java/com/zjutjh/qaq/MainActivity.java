package com.zjutjh.qaq;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSubmit(View view) {
        String serverIp = ((EditText) findViewById(R.id.serverIp)).getText().toString();
        String portString = ((EditText) findViewById(R.id.serverPort)).getText().toString();
        String userName = ((EditText) findViewById(R.id.userName)).getText().toString();

        if (serverIp.equals("")) {
            ((EditText) findViewById(R.id.serverIp)).setError("Please input server IP address");
            return;
        }

        if (portString.equals("")) {
            ((EditText) findViewById(R.id.serverPort)).setError("Please input server port");
            return;
        }

        if (userName.equals("") || userName.length() > 10) {
            ((EditText) findViewById(R.id.userName)).setError("Invalid username");
        }

        int serverPort = Integer.parseInt(portString);
        if (serverPort < 0 || serverPort > 65535) {
            ((EditText) findViewById(R.id.serverPort)).setError("Invalid server port");
            return;
        }

        String regExp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(regExp);
        if (!pattern.matcher(serverIp).matches()) {
            ((EditText) findViewById(R.id.serverIp)).setError("Invalid IP address");
            return;
        }


    }


}