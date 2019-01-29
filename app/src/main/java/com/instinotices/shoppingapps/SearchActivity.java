package com.instinotices.shoppingapps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

public class SearchActivity extends AppCompatActivity implements ProductsAdapter.ProductClickListener {
    public static String EXTRA_SEARCH_KEYWORDS = "searchKeywords";
    TextView mTextView;
    ProgressBar progressBar;
    RequestQueue queue;
    String searchWhat, flipkartJSONString, amazonJSONString;
    ProductListHelper productListHelper;
    Credentials credentials;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public static byte[] hmac(String algorithm, byte[] key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        productListHelper = new ProductListHelper();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, amazonJSONString);
                startActivity(Intent.createChooser(sharingIntent, "dd"));
            }
        });
        mTextView = findViewById(R.id.text);
        progressBar = findViewById(R.id.progressBarSearch);
        Intent intent = getIntent();
        if (intent.hasExtra("Credential")) {
            credentials = intent.getParcelableExtra("Credential");
        } else {
            getCredentials();
        }
        if (intent.hasExtra(EXTRA_SEARCH_KEYWORDS)) {
            searchWhat = intent.getStringExtra(EXTRA_SEARCH_KEYWORDS);
            getSupportActionBar().setTitle(searchWhat);
            progressBar.setVisibility(View.VISIBLE);
        }
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        startSerachRequest();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (queue != null) {
            queue.cancelAll("T");
        }
    }

    void startSerachRequest() {
        if (credentials == null) {
            return;
        }
        queue = Volley.newRequestQueue(this);
        StringRequest stringFlipkartRequest = new CustomRequest(Request.Method.GET, getFlipkartSearchUrl(searchWhat),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.GONE);
                        mTextView.setText("Response is: " + response);
                        flipkartJSONString = response;
                        productListHelper.addRawData(ProductListHelper.FLIPKART, flipkartJSONString);
                        mAdapter = new ProductsAdapter(Glide.with(SearchActivity.this), SearchActivity.this, productListHelper, SearchActivity.this, ProductsAdapter.MODE_SEARCH);
                        mRecyclerView.setAdapter(mAdapter);
                        Log.e("Response", response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("FKRT ERROR", error.toString());
                progressBar.setVisibility(View.INVISIBLE);
                mTextView.setText("That didn't work!");
            }
        });
        StringRequest stringAmazonRequest = new StringRequest(Request.Method.GET, getAmazonSearchUrl(searchWhat),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        progressBar.setVisibility(View.GONE);
                        amazonJSONString = new XmlToJson.Builder(response).build().toString();
                        mTextView.setText("Response is: " + amazonJSONString);
                        productListHelper.addRawData(ProductListHelper.AMAZON, amazonJSONString);
                        mAdapter = new ProductsAdapter(Glide.with(SearchActivity.this), SearchActivity.this, productListHelper, SearchActivity.this, ProductsAdapter.MODE_SEARCH);
                        mRecyclerView.setAdapter(mAdapter);
                        Log.e("Response", response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.INVISIBLE);
                mTextView.setText("That didn't work!");
                Toast.makeText(SearchActivity.this, "Please check your internet connection and try again", Toast.LENGTH_LONG).show();
                finish();
            }
        });
        stringAmazonRequest.setTag("T");
        stringFlipkartRequest.setTag("T");
        queue.add(stringAmazonRequest);
        queue.add(stringFlipkartRequest);
    }

    public String getFlipkartSearchUrl(String searchWhat) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("affiliate-api.flipkart.net")
                .appendPath("affiliate")
                .appendPath("1.0")
                .appendPath("search.json")
                .appendQueryParameter("query", searchWhat)
                .appendQueryParameter("resultCount", "10");
        String url = builder.build().toString();
        Log.e("URL", url);
        return url;
    }

    public String getAmazonSearchUrl(String searchWhat) {
        Uri.Builder builder1 = new Uri.Builder();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -5);
        calendar.add(Calendar.MINUTE, -30);
        String Timestamp = calendar.get(Calendar.YEAR) + "-" + formatted(calendar.get(Calendar.MONTH) + 1) + "-" + formatted(calendar.get(Calendar.DAY_OF_MONTH)) + "T" + formatted(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + formatted(calendar.get(Calendar.MINUTE)) + ":" + formatted(calendar.get(Calendar.SECOND)) + "Z";
        try {
            Log.e("ATAG", credentials.AAssociateTag);
            builder1
                    .appendQueryParameter("AWSAccessKeyId", credentials.awsAccessKey)
                    .appendQueryParameter("AssociateTag", credentials.AAssociateTag)
                    .appendQueryParameter("Availability", "Available")
                    .appendQueryParameter("Keywords", searchWhat)
                    .appendQueryParameter("Operation", "ItemSearch")
                    .appendQueryParameter("ResponseGroup", "Medium, Offers, PromotionSummary")
                    .appendQueryParameter("SearchIndex", "All")
                    .appendQueryParameter("Service", "AWSECommerceService")
                    .appendQueryParameter("Timestamp", Timestamp);
            Log.e("Wata", builder1.toString().substring(1));
            builder1.appendQueryParameter("Signature", generateHashWithHmac256(credentials.secretKey, "GET\n" +
                    "webservices.amazon.in\n" +
                    "/onca/xml\n" +
                    builder1.toString().substring(1)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        builder1.scheme("https")
                .authority("webservices.amazon.in")
                .appendPath("onca")
                .appendPath("xml");
        String url1 = builder1.build().toString();
        url1 = url1.substring(0, url1.length() - 3);
        Log.e("URL", url1);
        return url1;
    }

    @Override
    public void onItemClick(int position) {
        //Toast.makeText(this, ""+position, Toast.LENGTH_SHORT).show();
        MainActivity.loadPageInCustomTab(this, productListHelper.items.get(position).url, getResources().getColor(R.color.colorFlipkart), true);
    }

    public String generateHashWithHmac256(String key, String message) {
        String messageDigest = null;
        try {
            final String hashingAlgorithm = "HmacSHA256"; //or "HmacSHA1", "HmacSHA512"

            byte[] bytes = hmac(hashingAlgorithm, key.getBytes(), message.getBytes());

            //messageDigest= bytesToHex(bytes);
            messageDigest = Base64.encodeToString(bytes, Base64.DEFAULT);
            messageDigest.trim();

            Log.i("Key", "message digest: " + messageDigest);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return messageDigest;
    }

    void getCredentials() {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                credentials = dataSnapshot.getValue(Credentials.class);
                startSerachRequest();
                //Log.e("CR",credentials.awsAccessKey);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public String formatted(int n) {
        if (n > 9) {
            return n + "";
        } else {
            return "0" + n;
        }
    }

    public class CustomRequest extends StringRequest {
        //This class is for adding Affilate Id in headers so that requests aren't denied!!!
        public CustomRequest(int method, String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> map = new HashMap<>();
            map.put("Fk-Affiliate-Id", credentials.FAId);
            map.put("Fk-Affiliate-Token", credentials.FToken);
            return map;
        }
    }

}
