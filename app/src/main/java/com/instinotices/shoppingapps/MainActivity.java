package com.instinotices.shoppingapps;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MaterialSearchBar.OnSearchActionListener {
    public final static String FLIPKART = "flipkart", AMAZON = "amazon", SHOPCLUES = "shopclues";
    public static String SEARCH_SUGGESTIONS = "searchSuggestions";
    TextView searchHint;
    WebView webView;
    Credentials credentials;
    SharedPreferences sharedPreferences;
    private List<String> lastSearches;
    private MaterialSearchBar searchBar;
    private FirebaseAuth mAuth;

    public static void loadPageInCustomTab(Context context, String url, Integer color, boolean provideRichExperience) {
        if (url.contains("amazon.in")) {
            color = context.getResources().getColor(R.color.colorAmazon);
            provideRichExperience = true;
        } else if (url.contains("flipkart.com")) {
            provideRichExperience = true;
        }
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(color);
        builder.addDefaultShareMenuItem();
        builder.enableUrlBarHiding();
        builder.setShowTitle(true);
        builder.setInstantAppsEnabled(false);
        CustomTabsIntent customTabsIntent;
        if (provideRichExperience) {
            //Prevents the default app from opening .... and forces custom tab
            builder.setInstantAppsEnabled(false);
            Intent intent = new Intent(context.getApplicationContext(), ChromeHelperService.class);
            intent.setAction(ChromeHelperService.ACTION_ADD_TO_WATCH_LIST);
            PendingIntent pendingIntent = PendingIntent.getService(context, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setActionButton(getBitmapFromVectorDrawable(context, R.drawable.add_to_watchlist), "Add to watchlist", pendingIntent, true);
            //builder.addDefaultShareMenuItem();
            builder.addMenuItem("Add to watchlist", pendingIntent);
            customTabsIntent = builder.build();
            customTabsIntent.intent.setPackage("com.android.chrome");
        } else {
            customTabsIntent = builder.build();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER,
                    Uri.parse("com.instinotices.shoppingapps"));
        } else {
            customTabsIntent.intent.putExtra("android.intent.extra.REFERRER",
                    Uri.parse("com.instinotices.shoppingapps"));
        }
        try {
            customTabsIntent.launchUrl(context, Uri.parse(url));

        } catch (ActivityNotFoundException e) {
            //Chrome not found, try using other browsers
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            if (i.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(i);
            } else {
                //TODO No browser, use WebView
                Toast.makeText(context, "Please install Google Chrome from Play Store", Toast.LENGTH_LONG).show();
            }

        }
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        searchHint = findViewById(R.id.main_search_text);
        webView = findViewById(R.id.web_view);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        searchBar = (MaterialSearchBar) findViewById(R.id.searchBar);
        searchBar.setHint("Search Flipkart and Amazon");
        //enable searchbar callbacks
        searchBar.setOnSearchActionListener(this);
        //restore last queries from disk
        lastSearches = loadSearchSuggestionFromDisk();
        searchBar.setLastSuggestions(lastSearches);
        webView.getSettings().setJavaScriptEnabled(true);
        String htmlPre = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"></head><body style='margin:0; pading:0; background-color: black;'>";
        String htmlCode =
                "<script type=\"text/javascript\" language=\"javascript\">\n" +
                        "      var aax_size='300x250';\n" +
                        "      var aax_pubname = 'satyamcse-21';\n" +
                        "      var aax_src='302';\n" +
                        "    </script>\n" +
                        "    <script type=\"text/javascript\" language=\"javascript\" src=\"http://c.amazon-adsystem.com/aax2/assoc.js\"></script>";
        String htmlPost = "</body></html>";
        String unencodedHtml = htmlPre + htmlCode + htmlPost;
        Log.e("HTML", unencodedHtml);
        //String encodedHtml = Base64.encodeToString(unencodedHtml.getBytes(),
        //Base64.NO_PADDING);
        //webView.loadData(unencodedHtml, "text/html", "UTF-8");
        // webView.loadDataWithBaseURL("null", , "text/html", "UTF-8", null);
        webView.loadDataWithBaseURL(null, unencodedHtml, "text/html", "UTF-8", null);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            silentSignIn();
        } else {
            getCredentials();
        }
    }

    private List<String> loadSearchSuggestionFromDisk() {
        String s = sharedPreferences.getString(SEARCH_SUGGESTIONS, "");
        Gson gson = new Gson();
        List<String> a = new ArrayList<>();
        if (gson.fromJson(s, List.class) != null) {
            a = gson.fromJson(s, List.class);
        }
        return a;
    }

    private void saveSuggestions(List s) {
        sharedPreferences.edit().putString(SEARCH_SUGGESTIONS, new Gson().toJson(s)).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("Saving ", searchBar.getLastSuggestions().toString());
        saveSuggestions(searchBar.getLastSuggestions());
    }

    void silentSignIn() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("sdf", "signInAnonymously:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            getCredentials();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Please check your network connection", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void performSearch(View view) {
        String search = searchBar.getText();
        //Toast.makeText(this, search, Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, SearchActivity.class);
        i.putExtra(SearchActivity.EXTRA_SEARCH_KEYWORDS, search);
        i.putExtra("Credential", credentials);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSnapdeal(View view) {
        loadPageInCustomTab(this, "https://www.snapdeal.com", this.getResources().getColor(R.color.colorSnapdeal), false);
    }

    public void openFlipkart(View view) {
        loadPageInCustomTab(this, "https://dl.flipkart.com/?affid=satyamkum3", this.getResources().getColor(R.color.colorFlipkart), false);
    }

    public void openAmazon(View view) {
        loadPageInCustomTab(this, "https://www.amazon.in/ref=as_li_ss_tl?ie=UTF8&linkCode=ll2&tag=satyamcse-21&linkId=e0b7be4a3522e4c27a9251937421c222", this.getResources().getColor(R.color.colorAmazon), true);
    }

    public void openPaytm(View view) {
        loadPageInCustomTab(this, "https://paytm.com/", this.getResources().getColor(R.color.colorPaytm), false);
    }

    public void openShopclues(View view) {
        loadPageInCustomTab(this, "https://www.shopclues.com/", this.getResources().getColor(R.color.colorShopClues), false);
    }

    public void openMyntra(View view) {
        loadPageInCustomTab(this, "https://www.myntra.com/", this.getResources().getColor(R.color.colorWhite), false);
    }

    public void openJabong(View view) {
        loadPageInCustomTab(this, "https://www.jabong.com/", this.getResources().getColor(R.color.colorWhite), false);
    }

    public void openHomeShop18(View view) {
        loadPageInCustomTab(this, "https://www.homeshop18.com/", this.getResources().getColor(R.color.colorHomeShop18), false);
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
        String s = enabled ? "enabled" : "disabled";
        if (enabled) {
            searchHint.setVisibility(View.INVISIBLE);
        } else {
            searchHint.setVisibility(View.VISIBLE);
        }
        //Toast.makeText(MainActivity.this, "Search " + s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSearchConfirmed(CharSequence text) {
        performSearch(null);
    }

    @Override
    public void onButtonClicked(int buttonCode) {

    }

    void getCredentials() {
        //FirebaseDatabase.getInstance().setPersistenceEnabled(false);
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                credentials = dataSnapshot.getValue(Credentials.class);

                Log.e("CR", dataSnapshot.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
