package com.zero.print;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothModuleControl {
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket btSocket = null;
    private boolean keep_reading = true;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (deviceList != null)
                    deviceList.add(device);
                if (adapter != null)
                    adapter.notifyDataSetChanged();
            }
        }
    };
    static final public int HANDLE_DATA = 1;
    static final public int HANDLE_ERROR = 2;
    static final public int HANDLE_CONNECTION_ERROR = 3;
    static final public int HANDLE_CONNECTION_SUCCESS = 4;
    static final public int HANDLE_CONNECTION_DISCONNECTED = 5;
    public Handler handler;
    public ArrayList<BluetoothDevice> deviceList;
    public BluetoothDevices bluetoothDevices = new BluetoothDevices();
    public BluetoothDeviceViewAdapter adapter;
    public ViewSetup viewSetup;
    public Connection connection = new Connection();
    public Data data = null;


    public BluetoothModuleControl(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        adapter = new BluetoothDeviceViewAdapter();
    }

    public class BluetoothDevices {

        public void searchForNewDevice() {
            context.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            bluetoothAdapter.startDiscovery();
        }

        public void cancelSearchForNewDevice() {
            bluetoothAdapter.cancelDiscovery();
            context.unregisterReceiver(broadcastReceiver);
        }

        public void getDeviceList() {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                deviceList = new ArrayList<>();
                Set<BluetoothDevice> set = bluetoothAdapter.getBondedDevices();
                for (Object btd : set) {
                    deviceList.add((BluetoothDevice) btd);
                }
            } else {
                deviceList = null;
            }
        }
    }//BluetoothDevices class


    public class BluetoothDeviceViewAdapter extends BaseAdapter {

        public BluetoothDeviceViewAdapter(ArrayList<BluetoothDevice> list) {
            deviceList = list;
        }

        public BluetoothDeviceViewAdapter() {
            if (bluetoothDevices != null) {
                bluetoothDevices.getDeviceList();
            }
        }

        @Override
        public int getCount() {
            if (deviceList != null) {
                return deviceList.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int p1) {
            if (deviceList != null) {
                return deviceList.get(p1);
            }
            return null;
        }

        @Override
        public long getItemId(int p1) {
            return p1;
        }

        @Override
        public View getView(int p1, View p2, ViewGroup p3) {
            if (viewSetup != null)
                return viewSetup.returnView(p1, deviceList);
            else
                return null;
        }
    }//BluetoothDeviceViewAdapter class


    public interface ViewSetup {
        View returnView(int position, ArrayList<BluetoothDevice> list);
    }


    public class Connection {
        public void connect(BluetoothDevice device) {
            try {
                if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                }
                btSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
            } catch (IOException e) {
                sendToHandler(HANDLE_CONNECTION_ERROR, "Socket connection error" + e);
            }

            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        if (btSocket != null && btSocket.isConnected()) {
                            sendToHandler(HANDLE_CONNECTION_SUCCESS, "Connection success");
                            data = new Data();
                            data.start();
                            break;
                        }
                    }
                }
            }.start();
        }

        public void disconnect() {
            if (btSocket != null) {
                keep_reading = false;

                try {
                    btSocket.close();
                    btSocket = null;
                    data = null;
                    sendToHandler(HANDLE_CONNECTION_DISCONNECTED, "Disconnected");
                } catch (IOException e) {
                    sendToHandler(HANDLE_CONNECTION_ERROR, "socket close error");
                }
            }
        }

        public boolean isConnected() {
            if (btSocket == null) return false;
            return btSocket.isConnected();
        }
    }//Connection class


    public class Data extends Thread {
        public void send(byte[] data) {
            if (btSocket != null) {
                try {
                    if (btSocket.isConnected()) {
                        btSocket.getOutputStream().write(data);
                        btSocket.getOutputStream().flush();
                    } else
                        sendToHandler(HANDLE_ERROR, "Bluetooth Socket is disconnected");
                } catch (IOException e) {
                    try {
                        btSocket.close();
                    } catch (IOException ee) {
                        sendToHandler(HANDLE_ERROR, "Get OutputStream error on closing" + ee);
                    }
                    try {
                        btSocket.connect();
                        btSocket.getOutputStream().write(data);
                        btSocket.getOutputStream().flush();
                    } catch (IOException eee) {
                        sendToHandler(HANDLE_ERROR, "Get OutputStream error on reconnecting" + eee);
                    }
                }
            }
        }

        @Override
        public void run() {
            listen();
        }

        private void listen() {
            byte[] dataBuffer = new byte[1024];

            while (keep_reading) {
                if (btSocket != null) {
                    try {
                        if (btSocket.getInputStream().available() != 0) {
                            int size = btSocket.getInputStream().read(dataBuffer);
                            if (size > 0) {
                                String data = new String(dataBuffer, StandardCharsets.UTF_8);
                                sendToHandler(HANDLE_DATA, data);
                            }
                        }
                    } catch (IOException e) {
                        sendToHandler(HANDLE_ERROR, "Input stream was disconnected");
                    }
                }
            }
        }
    } //Data class


    private void sendToHandler(int what, String data) {
        if (handler != null) {
            Message msg = handler.obtainMessage(what, data);
            msg.sendToTarget();
        }
    }
}
