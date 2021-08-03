package com.zero.print;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class ConnectActivity extends AppCompatActivity {

    public static final boolean DEBUG = false;
    final static String[] models = {"MHT-P8001", "Printer001"};

    private ProgressDialog progress;

    private ListView listView;
    private ImageView refresh_animation;
    private TextView refresh;

    private ConstraintLayout deviceListView;
    private ConstraintLayout connectedView;

    private boolean isBounded = false;
    private PrinterService service;
    private Intent serviceIntent;
    private ServiceConnection serviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        initService();
    }

    private void init() {
        setContentView(R.layout.activity_connect);
        progress = new ProgressDialog(this);
        listView = findViewById(R.id.listView);
        refresh_animation = findViewById(R.id.refresh_image);
        refresh = findViewById(R.id.refresh_text);
        deviceListView = findViewById(R.id.device_view);
        connectedView = findViewById(R.id.connected_view);
        Button disconnect = findViewById(R.id.disconnect);
        Button printSample = findViewById(R.id.printSample);
        refresh.setOnClickListener(v -> animationRefresh());
        disconnect.setOnClickListener(v -> {
            if(service != null) {
                if (DEBUG) {
                    setView(false);
                } else {
                    new Thread(() -> service.disconnect() ).start();
                }
            }
        });
        printSample.setOnClickListener(v -> sampleWebInvoice() );

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service = ((PrinterService.ServiceBinder)binder).getService();
                isBounded = true;
                turnOnBluetooth();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBounded = false;
            }
        };

        serviceIntent = new Intent(this, PrinterService.class);
    }

    private void initService() {
        if(serviceStopped(PrinterService.class.getName())) {
            startService(serviceIntent);
            bindService(serviceIntent, serviceConnection, BIND_IMPORTANT);
        } else {
            if (!isBounded) {
                bindService(serviceIntent, serviceConnection, BIND_IMPORTANT);
            } else {
                setView(service.isConnected());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            initializeBluetooth();
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

    private void setView(boolean connected) {
        if (deviceListView != null) {
            if (connected) {
                deviceListView.setVisibility(View.GONE);
                connectedView.setVisibility(View.VISIBLE);
            } else {
                connectedView.setVisibility(View.GONE);
                deviceListView.setVisibility(View.VISIBLE);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void turnOnBluetooth() {
        if(BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(this, "This device doesn't support Bluetooth!", Toast.LENGTH_SHORT).show();
        } else if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
        } else {
            initializeBluetooth();
        }
    }

    public void initializeBluetooth() {
        service.status = new PrinterService.Status() {
            @Override
            public void connection_error(String msg) {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) progress.cancel();
                setView(DEBUG);
            }

            @Override
            public void connecting(String msg) {
                progress = new ProgressDialog(ConnectActivity.this);
                progress.setTitle("Connecting...");
                progress.setMessage("please wait");
                progress.setCancelable(false);
                progress.show();
            }

            @Override
            public void connected(String msg) {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) progress.cancel();
                setView(true);
            }

            @Override
            public void disconnected(String msg) {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                setView(false);
            }
        };
        service.initializeBluetooth();
        service.setItemViewGroup((position, list) -> {
            @SuppressLint("InflateParams")
            View view = getLayoutInflater().inflate(R.layout.device_item, null);
            TextView title = view.findViewById(R.id.deviceName);
            TextView address = view.findViewById(R.id.deviceAddress);
            title.setText(list.get(position).getName());
            address.setText(list.get(position).getAddress());
            view.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConnectActivity.this);
                builder.setTitle("Printer model")
                        .setItems(models, (dialog, which) -> service.connect(list.get(position), which))
                        .setCancelable(false).show();
            });
            return view;
        });
        listView.setAdapter(service.getDefaultAdapter());
        setView(service.isConnected());
    }

    private void sampleWebInvoice() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", "file:///android_asset/index.html");
        startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    public void animationRefresh() {
        service.discover();
        refresh.setText("Refreshing...");
        refresh_animation.setAlpha((float)1);
        ObjectAnimator oa = new ObjectAnimator();
        oa.setTarget(refresh_animation);
        oa.setPropertyName("rotation");
        oa.setFloatValues(0, -10800);
        oa.setDuration(30000);
        oa.setInterpolator(new LinearInterpolator());
        oa.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator p1) {}
            @Override
            public void onAnimationEnd(Animator p1) {
                service.cancelDiscover();
                ObjectAnimator a = new ObjectAnimator();
                a.setTarget(refresh_animation);
                a.setPropertyName("alpha");
                a.setFloatValues(1, 0);
                a.setDuration(1000);
                a.start();
                refresh.setText(R.string.refresh);
            }
            @Override
            public void onAnimationCancel(Animator p1) {}
            @Override
            public void onAnimationRepeat(Animator p1) {}
        });
        oa.start();
    }
}