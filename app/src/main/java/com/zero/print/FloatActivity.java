package com.zero.print;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class FloatActivity extends Activity {

    public ProgressDialog progress;
    public Handler handler;
    public ServiceConnection serviceConnection;

    @SuppressWarnings("deprecation")
    @SuppressLint({"SetTextI18n", "HandlerLeak"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_screen);

        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    new Handler().postDelayed(() -> {
                        unbindService(serviceConnection);
                        progress.cancel();
                        finish();
                    }, 2000);
                } else if (msg.what == 2) {
                    unbindService(serviceConnection);
                    progress.cancel();
                    finish();
                }
            }
        };

        Intent intent = getIntent();
        if(intent != null && intent.getAction().equals("PRINT_INVOICE")) {
            progress = new ProgressDialog(FloatActivity.this);
            progress.setMessage("Printing...");
            progress.setCancelable(false);
            progress.show();
            if (!MainActivity.DEBUG){
                if (serviceStopped(PrinterService.class.getName())) {
                    Toast.makeText(this, "Connect printer first!", Toast.LENGTH_SHORT).show();
                    progress.cancel();
                    finish();
                }
            }
            String invoice = intent.getStringExtra("invoice");

            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    ((PrinterService.ServiceBinder)service).getService().printInvoice(invoice);
                    if (MainActivity.DEBUG) {
                        handler.sendMessage(handler.obtainMessage(1));
                    } else {
                        handler.sendMessage(handler.obtainMessage(2));
                    }
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
            bindService(new Intent(this, PrinterService.class), serviceConnection, BIND_AUTO_CREATE);
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