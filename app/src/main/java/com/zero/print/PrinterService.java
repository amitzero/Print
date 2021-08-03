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
    public static final String MODEL = "com.zero.print.model";
    public BluetoothModuleControl bluetoothDriver;
    public SharedPreferences sharedPreferences;
    public String defaultPrinter;
    private boolean model_mht = false;

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
        if (sharedPreferences.contains(PRINTER) && sharedPreferences.contains(MODEL)) {
            defaultPrinter = sharedPreferences.getString(PRINTER, "");
            model_mht = sharedPreferences.getBoolean(MODEL, false);
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
                    connect(device, model_mht?0:1);
                    break;
                }
            }
        }
    }

    public void connect(BluetoothDevice device, int which) {
        sharedPreferences.edit().putString(PRINTER, device.getAddress()).apply();
        sharedPreferences.edit().putBoolean(MODEL, which==0).apply();
        if (status != null) status.connecting(device.getName());
        new Thread() {
            @Override
            public void run() {
                bluetoothDriver.connection.connect(device);
            }
        }.start();
        model_mht = which == 0;
    }

    public void printInvoice(String invoice) {
        ByteArrayOutputStream invoice_byte;
        try {
            invoice_byte = parseStringInvoice(invoice);
            if (ConnectActivity.DEBUG) {
                printMsg("Invoice length: "+invoice_byte.size());
            } else {
                if (bluetoothDriver.connection.isConnected()) {
                    bluetoothDriver.data.send(invoice_byte.toByteArray());
                } else {
                    Toast.makeText(getApplicationContext(), "Printer isn't connected!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Exception on invoice parsing!", Toast.LENGTH_SHORT).show();
        }
    }

    public void handleModule(int what, String msg) {
        switch (what) {
            case BluetoothModuleControl.HANDLE_CONNECTION_ERROR:
                sharedPreferences.edit().remove(PRINTER).apply();
                sharedPreferences.edit().remove(MODEL).apply();
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
        if (ConnectActivity.DEBUG) {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private ByteArrayOutputStream parseStringInvoice(String invoice) throws IOException {
        Toast.makeText(this, invoice, Toast.LENGTH_SHORT).show();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(Printer.INIT);

        String font = "NORMAL";
        boolean dualAlign = false;
        int bufferLength = 0;

        boolean inCommand = false;
        StringBuilder command = null;
        for (int i = 0; i < invoice.length(); i++) {
            char ch = invoice.charAt(i);
            if (ch == '<') {
                inCommand = true;
                command = new StringBuilder();
            } else if (ch == '>') {
                inCommand = false;
                assert command != null;
                String s = command.toString();
                switch (s) {
                    case "LEFT":
                        dualAlign = true;
                        byteStream.write(Printer.ALIGN_LEFT);
                        break;
                    case "CENTER":
                        byteStream.write(Printer.ALIGN_CENTER);
                        break;
                    case "RIGHT":
                        if (dualAlign) {
                            int length = 0;
                            switch (font) {
                                case "NORMAL":
                                    length = (model_mht?57:64)-bufferLength;
                                    break;
                                case "MEDIUM":
                                    length = 48-bufferLength;
                                    break;
                                case "LARGE":
                                    length = 24-bufferLength;
                            }
                            StringBuilder tmpBuffer = new StringBuilder();
                            char sch = invoice.charAt(i+1);
                            while (sch != '<' && sch != '\n') {
                                tmpBuffer.append(sch);
                                length--;
                                i++;
                                sch = invoice.charAt(i+1);
                            }
                            StringBuilder subBuffer = new StringBuilder();
                            while (length != 0) {
                                subBuffer.append(' ');
                                length--;
                            }
                            subBuffer.append(tmpBuffer);
                            byteStream.write(subBuffer.toString().getBytes());
                        }
                        byteStream.write(Printer.ALIGN_RIGHT);
                        break;
                    case "NORMAL":
                        font = "NORMAL";
                        byteStream.write(Printer.SIZE_NORMAL);
                        break;
                    case "MEDIUM":
                        font = "MEDIUM";
                        byteStream.write(Printer.SIZE_MEDIUM);
                        break;
                    case "LARGE":
                        font = "LARGE";
                        byteStream.write(Printer.SIZE_LARGE);
                        break;
                    case "BR":
                        dualAlign = false;
                        bufferLength = 0;
                        byteStream.write(Printer.PAPER_FEED);
                        break;
                    default:
                        Toast.makeText(this, "Unknown command found!", Toast.LENGTH_SHORT).show();
                }
            } else if(inCommand) {
                command.append(ch);
            } else if (ch != '\n') {
                bufferLength++;
                byteStream.write(ch);
            }
        }
        byteStream.write(Printer.PAPER_FEED_AND_CUT);
        return byteStream;
    }

    public boolean isConnected() {
        if (ConnectActivity.DEBUG) return true;
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