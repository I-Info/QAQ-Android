package com.i1nfo.qaq

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class About : AppCompatActivity() {

    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        titleText = findViewById(R.id.text_title)
        val code = getVersionCode(applicationContext)
        val name = getVersionName(applicationContext)
        titleText.text = getString(R.string.about_message_title, name, code)
    }

    private fun getVersionCode(ctx: Context): Long {
        val manager = ctx.packageManager
        var code = 0L
        try {
            val info = manager.getPackageInfo(ctx.packageName, 0)
            code = info.longVersionCode
        } catch (e: Exception) {
            Log.e("GetVersionCode", e.toString())
        }
        return code
    }

    private fun getVersionName(ctx: Context): String {
        val manager = ctx.packageManager
        var name = ""
        try {
            val info = manager.getPackageInfo(ctx.packageName, 0)
            name = info.versionName
        } catch (e: Exception) {
            Log.e("GetVersionName", e.toString())
        }
        return name
    }

}