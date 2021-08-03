package com.zero.print;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class WebViewActivity extends AppCompatActivity {

    private PrinterService printerService;

    private ProgressDialog progress;

    private WebView webView;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @SuppressLint({"SetTextI18n", "HandlerLeak", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void printInvoice(String invoice) {
                progress = new ProgressDialog(WebViewActivity.this);
                progress.setMessage("Printing...");
                progress.setCancelable(false);
                progress.show();
                if (!ConnectActivity.DEBUG){
                    if (serviceStopped(PrinterService.class.getName())) {
                        Toast.makeText(getApplicationContext(), "Connect printer first!", Toast.LENGTH_SHORT).show();
                        progress.cancel();
                        return;
                    }
                }
                printerService.printInvoice(invoice);
                if (ConnectActivity.DEBUG) {
                    new Handler().postDelayed(() -> progress.cancel(), 2000);
                } else {
                    progress.cancel();
                }
            }
        }, "java");
        final Dialog loading = new Dialog(WebViewActivity.this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loading.setCancelable(false);
                loading.getWindow().setBackgroundDrawableResource(R.drawable.transparent);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                loading.addContentView(new ProgressBar(WebViewActivity.this), params);
                loading.show();
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (ConnectActivity.DEBUG) {
                    new Handler().postDelayed(loading::cancel, 2000);
                } else {
                    loading.cancel();
                }
                Objects.requireNonNull(getSupportActionBar()).setTitle(view.getTitle());
                super.onPageFinished(view, url);
            }
        });
        String url = getIntent().getStringExtra("url");
        if (url == null) finish();
        webView.loadUrl(url);
        loading.show();

        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                printerService = ((PrinterService.ServiceBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        bindService(new Intent(this, PrinterService.class), serviceConnection, BIND_IMPORTANT);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
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