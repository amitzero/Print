package com.zero.print;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    BluetoothModuleControl controller;
    ListView listView;
    ProgressDialog progress;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        controller = new BluetoothModuleControl(this);
        listView = findViewById(R.id.listView);
        if(BluetoothAdapter.getDefaultAdapter() == null)
        {
            Toast.makeText(this, "This device doesn't support Bluetooth!", Toast.LENGTH_SHORT).show();
        }
        else if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
        }
        else
        {
            initializeBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            initializeBluetooth();
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("HandlerLeak")
    public void initializeBluetooth() {
        controller.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                handleModule(msg.what, (String) msg.obj);
            }
        };
        controller.viewSetup = (position, list) -> {
            @SuppressLint("InflateParams")
            View view = getLayoutInflater().inflate(R.layout.device_item, null);

            TextView title = view.findViewById(R.id.deviceName),
                    address = view.findViewById(R.id.deviceAddress);
            title.setText(list.get(position).getName());
            address.setText(list.get(position).getAddress());


            view.setOnClickListener(p1 -> connect(list.get(position)) );

            return view;
        };

        listView.setAdapter(controller.adapter);

//        refresh_tv.setOnClickListener((View.OnClickListener) p1 -> animationRefresh());
    }

    public void connect(BluetoothDevice device) {
        progress = new ProgressDialog(this);
        progress.setTitle("Connecting...");
        progress.setMessage("please wait");
        progress.setCancelable(false);
        progress.show();
        new Thread() {
            @Override
            public void run() {
                controller.connection.connect(device);
            }
        }.start();
    }

    public void handleModule(int what, String msg) {
        switch (what) {
            case BluetoothModuleControl.HANDLE_CONNECTION_ERROR: {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) progress.cancel();
                onResume();
                break;
            }
            case BluetoothModuleControl.HANDLE_CONNECTION_SUCCESS: {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) progress.cancel();
//                setConnectedUiMode();
                break;
            }
            case BluetoothModuleControl.HANDLE_CONNECTION_DISCONNECTED: {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                onCreate(new Bundle());
                break;
            }
            default:
                break;
        }
    }
}