package com.instinotices.shoppingapps;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChromeHelperService extends Service {
    public static final String ACTION_ADD_TO_WATCH_LIST = "com.instinotices.shoppingapps.action.FOO";
    SharedPreferences sharedPreferences;

    public ChromeHelperService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_ADD_TO_WATCH_LIST.equals(action)) {
                String url = intent.getDataString();
                ProductDetails productDetail = new ProductDetails();
                Log.e("i", url);
                String product_id = "";
                if (url.contains("https://www.flipkart.com")) {
                    int ind = url.indexOf("pid=");
                    //Log.e("INDEX",ind+"");
                    if (ind == -1) {
                        Log.e("Index not found", "jhg");
                        Toast.makeText(getApplicationContext(), "Please open a product to get price drop alerts", Toast.LENGTH_LONG).show();
                        return super.onStartCommand(intent, flags, startId);
                    }
                    for (char c : url.substring(ind + 4).toCharArray()) {
                        if (c == '&') {
                            break;
                        }
                        product_id += c;
                    }
                    productDetail.productId = product_id;
                    productDetail.marketplace = MainActivity.FLIPKART;
                    Log.e("PID", product_id);
                    Toast.makeText(this, "Product added to your watchlist", Toast.LENGTH_SHORT).show();
                } else if (url.contains("https://www.amazon.in")) {
                    Pattern asinPattern = Pattern.compile("/([A-Z0-9]{10})");
                    Matcher matcher = asinPattern.matcher(url);
                    if (matcher.find()) {
                        product_id = url.substring(matcher.start() + 1, matcher.end());
                    } else {
                        Toast.makeText(getApplicationContext(), "Please open a product to get price drop alerts", Toast.LENGTH_LONG).show();
                        return super.onStartCommand(intent, flags, startId);
                    }
                    Log.e("PID", product_id);
                    productDetail.productId = product_id;
                    productDetail.marketplace = MainActivity.AMAZON;
                } else {
                    Log.e("Unknown URL", "");
                }

            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
