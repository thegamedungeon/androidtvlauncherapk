package com.gamedungeon.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from exiting launcher
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String getInstalledApps() {
            JSONArray appList = new JSONArray();
            PackageManager pm = mContext.getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> pkgAppsList = pm.queryIntentActivities(mainIntent, 0);

            for (ResolveInfo ri : pkgAppsList) {
                try {
                    JSONObject appInfo = new JSONObject();
                    String label = ri.loadLabel(pm).toString();
                    String packageName = ri.activityInfo.packageName;

                    // Exclude self from list
                    if (packageName.equals(mContext.getPackageName())) continue;

                    Drawable icon = ri.loadIcon(pm);
                    String base64Icon = drawableToBase64(icon);

                    appInfo.put("name", label);
                    appInfo.put("package", packageName);
                    appInfo.put("icon", base64Icon);

                    appList.put(appInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return appList.toString();
        }

        @JavascriptInterface
        public void launchApp(String packageName) {
            Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                mContext.startActivity(launchIntent);
            }
        }

        private String drawableToBase64(Drawable drawable) {
            try {
                Bitmap bitmap;
                if (drawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) drawable).getBitmap();
                } else {
                    bitmap = Bitmap.createBitmap(
                        Math.max(1, drawable.getIntrinsicWidth()),
                        Math.max(1, drawable.getIntrinsicHeight()),
                        Bitmap.Config.ARGB_8888
                    );
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);
            } catch (Exception e) {
                return "";
            }
        }
    }
}
