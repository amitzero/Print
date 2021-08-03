package com.zero.print;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Button printers = findViewById(R.id.connectPrinter);
        printers.setOnClickListener((view) -> startActivity(new Intent(this, ConnectActivity.class)));
        Button webSite = findViewById(R.id.webSite);
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", "https://www.kundenlogin.scan2order.de");
        webSite.setOnClickListener((view) -> startActivity(intent));
    }
}