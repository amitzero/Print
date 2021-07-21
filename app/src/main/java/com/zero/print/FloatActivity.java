package com.zero.print;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;

public class FloatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView status = new TextView(this);
        Intent intent = getIntent();
        if(intent != null && intent.getAction().equals("PRINT_INVOICE")) {
            if (serviceStopped(PrinterService.class.getName())) {
                Toast.makeText(this, "Printer isn't connected!", Toast.LENGTH_SHORT).show();
                finish();
            }
            String invoice = intent.getStringExtra("invoice");
            bindService(new Intent(this, PrinterService.class), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    ((PrinterService.ServiceBinder)service).getService().printInvoice(invoice);
                    finish();
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            }, BIND_IMPORTANT);
        }
    }

    public boolean serviceStopped(String serviceName) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(info.service.getClassName())) {
                return false;
            }
        }
        return true;
    }
}