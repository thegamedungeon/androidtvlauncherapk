package com.gamedungeon.launcher;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        // Expose native methods to JavaScript under window.AndroidBridge
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Load installed apps right after page loads
                webView.loadUrl("javascript:initApps()");
            }
        });

        // Load local index.html from assets folder
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    // Intercept hardware Back Button to return to Home view instead of closing
    @Override
    public void onBackPressed() {
        webView.loadUrl("javascript:goHome()");
    }

    public class WebAppInterface {
        @JavascriptInterface
        public String getInstalledApps() {
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> pkgList = pm.queryIntentActivities(mainIntent, 0);

            JSONArray appsArray = new JSONArray();
            for (ResolveInfo ri : pkgList) {
                try {
                    JSONObject appObj = new JSONObject();
                    appObj.put("name", ri.loadLabel(pm).toString());
                    appObj.put("packageName", ri.activityInfo.packageName);
                    appsArray.put(appObj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return appsArray.toString();
        }

        @JavascriptInterface
        public void launchApp(String packageName) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
        }
    }
}
