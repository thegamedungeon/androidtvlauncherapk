package com.gamedungeon.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MainActivity extends Activity {

    private WebView dungeonWebView;
    private WebView appsWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.HORIZONTAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setBackgroundColor(0xFF0D0E15);

        dungeonWebView = new WebView(this);
        LinearLayout.LayoutParams dungeonParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                2.2f // Takes 68% of horizontal screen width
        );
        dungeonWebView.setLayoutParams(dungeonParams);

        // Configure persistent Web Storage & DOM Storage for Game Dungeon
        WebSettings dungeonSettings = dungeonWebView.getSettings();
        dungeonSettings.setJavaScriptEnabled(true);
        dungeonSettings.setDomStorageEnabled(true);
        dungeonSettings.setDatabaseEnabled(true);
        dungeonSettings.setAllowFileAccess(true);
        dungeonSettings.setAllowContentAccess(true);
        dungeonSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Enable cookies and session persistence
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(dungeonWebView, true);

        dungeonWebView.setWebViewClient(new WebViewClient());
        dungeonWebView.loadUrl("https://thegamedungeon.qzz.io/");

        appsWebView = new WebView(this);
        LinearLayout.LayoutParams appsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1.0f // Takes 32% of horizontal screen width
        );
        appsWebView.setLayoutParams(appsParams);

        WebSettings appsSettings = appsWebView.getSettings();
        appsSettings.setJavaScriptEnabled(true);
        appsSettings.setDomStorageEnabled(true);
        appsSettings.setAllowFileAccess(true);
        appsSettings.setAllowContentAccess(true);

        appsWebView.setWebViewClient(new WebViewClient());
        appsWebView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");
        appsWebView.loadUrl("file:///android_asset/index.html");

        rootLayout.addView(dungeonWebView);
        rootLayout.addView(appsWebView);

        setContentView(rootLayout);
    }

    @Override
    public void onBackPressed() {
        if (dungeonWebView.canGoBack()) {
            dungeonWebView.goBack();
        } else if (appsWebView.canGoBack()) {
            appsWebView.goBack();
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

                    // Exclude self from launcher list
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
