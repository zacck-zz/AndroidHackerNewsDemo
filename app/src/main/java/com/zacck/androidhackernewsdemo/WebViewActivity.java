package com.zacck.androidhackernewsdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        WebView mWebView = (WebView)findViewById(R.id.webViewArticle);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());

        Intent mIntent = getIntent();
        String url = mIntent.getStringExtra("articleUrl");
        String content = mIntent.getStringExtra("content");

        //mWebView.loadData(content,"text/html","UTF-8");
        mWebView.loadUrl(url);


    }

}
