package com.zero.print;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PrinterService extends Service {

    public static final String PRINTER = "com.zero.print";
    public BluetoothModuleControl bluetoothDriver;
    public SharedPreferences sharedPreferences;
    public String defaultPrinter;

    public Status status;

    @Override
    public IBinder onBind(Intent intent) {
        printMsg("Bind");
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        printMsg("Rebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        printMsg("Unbind");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        printMsg("StartCommand");
        sharedPreferences = getSharedPreferences(PRINTER, MODE_MULTI_PROCESS);
        if (sharedPreferences.contains(PRINTER)) {
            defaultPrinter = sharedPreferences.getString(PRINTER, "");
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        printMsg("Destroy");
        if (bluetoothDriver != null && bluetoothDriver.connection.isConnected()) {
            bluetoothDriver.connection.disconnect();
        }
        super.onDestroy();
    }

    public void setItemViewGroup(BluetoothModuleControl.ViewSetup viewGroup) {
        bluetoothDriver.viewSetup = viewGroup;
    }

    BluetoothModuleControl.BluetoothDeviceViewAdapter getDefaultAdapter() {
        return bluetoothDriver.adapter;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("HandlerLeak")
    public void initializeBluetooth() {
        bluetoothDriver = new BluetoothModuleControl(getApplicationContext());
        bluetoothDriver.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                handleModule(msg.what, (String) msg.obj);
            }
        };

        if (defaultPrinter != null && !defaultPrinter.isEmpty()) {
            for (BluetoothDevice device : bluetoothDriver.deviceList) {
                if (device.getAddress().equals(defaultPrinter)) {
                    connect(device);
                    break;
                }
            }
        }
    }

    public void connect(BluetoothDevice device) {
        sharedPreferences.edit().putString(PRINTER, device.getAddress()).apply();
        if (status != null) status.connecting(device.getName());
        new Thread() {
            @Override
            public void run() {
                bluetoothDriver.connection.connect(device);
            }
        }.start();
    }

    public boolean printInvoice(String invoice) {
        ByteArrayOutputStream invoice_byte;
        try {
            invoice_byte = parseStringInvoice(invoice);
            if (MainActivity.DEBUG) {
                printMsg("Invoice length: "+invoice_byte.size());
                return true;
            } else {
                if (bluetoothDriver.connection.isConnected()) {
                    bluetoothDriver.data.send(invoice_byte.toByteArray());
                    return true;
                } else {
                    printMsg("Printer isn't connected!");
                    return false;
                }
            }
        } catch (IOException e) {
            printMsg("Exception on invoice parsing!");
            return false;
        }
    }

    public void handleModule(int what, String msg) {
        switch (what) {
            case BluetoothModuleControl.HANDLE_CONNECTION_ERROR:
                sharedPreferences.edit().remove(PRINTER).apply();
                if (status != null) status.connection_error(msg);
                break;
            case BluetoothModuleControl.HANDLE_CONNECTION_SUCCESS:
                if (status != null) status.connected(msg);
                break;
            case BluetoothModuleControl.HANDLE_CONNECTION_DISCONNECTED:
                if (status != null) status.disconnected(msg);
                break;
            default:
                break;
        }
    }

    private void printMsg(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    private ByteArrayOutputStream parseStringInvoice(String invoice) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(Printer.INIT);
        boolean inCommand = false;
        StringBuilder command = null;
        char[] chArray = invoice.toCharArray();
        for (char ch : chArray) {
            if (ch == '<') {
                inCommand = true;
                command = new StringBuilder();
            } else if (ch == '>') {
                inCommand = false;
                assert command != null;
                String s = command.toString();
                switch (s) {
                    case "LEFT":
                        byteStream.write(Printer.ALIGN_LEFT);
                        break;
                    case "CENTER":
                        byteStream.write(Printer.ALIGN_CENTER);
                        break;
                    case "RIGHT":
                        byteStream.write(Printer.ALIGN_RIGHT);
                        break;
                    case "NORMAL":
                        byteStream.write(Printer.SIZE_NORMAL);
                        break;
                    case "MEDIUM":
                        byteStream.write(Printer.SIZE_MEDIUM);
                        break;
                    case "LARGE":
                        byteStream.write(Printer.SIZE_LARGE);
                        break;
                    case "BR":
                        byteStream.write(Printer.PAPER_FEED);
                        break;
                    default:
                        Toast.makeText(this, "Unknown command!", Toast.LENGTH_SHORT).show();
                }
            } else if(inCommand) {
                command.append(ch);
            } else if (ch != '\n') {
                byteStream.write(ch);
            }
        }
        byteStream.write(Printer.PAPER_FEED_AND_CUT);
        return byteStream;
    }

    public boolean isConnected() {
        return bluetoothDriver.connection.isConnected();
    }

    public void disconnect() {
        bluetoothDriver.connection.disconnect();
    }

    public void discover() {
        bluetoothDriver.bluetoothDevices.searchForNewDevice();
    }

    public void cancelDiscover() {
        bluetoothDriver.bluetoothDevices.cancelSearchForNewDevice();
    }

    interface Status {
        void connection_error(String msg);
        void connecting(String msg);
        void connected(String msg);
        void disconnected(String msg);
    }

    ServiceBinder binder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        PrinterService getService() {
            return PrinterService.this;
        }
    }
}