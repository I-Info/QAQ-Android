package com.zjutjh.qaq;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView textView = findViewById(R.id.github_release);
        textView.setText(Html.fromHtml("<a href=\"https://github.com/I-Info/QAQ-Android/releases\"><img alt=\"GitHub release (latest by date including pre-releases)\" src=\"https://img.shields.io/github/v/release/I-Info/QAQ-Android?include_prereleases&style=flat-square\"></a>",Html.FROM_HTML_MODE_LEGACY));
    }
}