package com.instinotices.shoppingapps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class ProductListHelper {
    public final static String FLIPKART = "flipkart", AMAZON = "amazon", SHOPCLUES = "shopclues";
    public ArrayList<ItemDetails> items, newItems;
    String rawFlipkart, rawAmazon, rawShopclues;

    public ProductListHelper() {
        items = new ArrayList<>();
    }

    public void addRawData(String marketPlace, String rawData) {
        if (marketPlace.equals(FLIPKART)) {
            rawFlipkart = rawData;
            processFlipkartJSON();
        } else if (marketPlace.equals(AMAZON)) {
            rawAmazon = rawData;
            processAmazonJSON();
        } else if (marketPlace.equals(SHOPCLUES)) {
            rawShopclues = rawData;
        }
    }

    void processAmazonJSON() {
        JSONObject root = null;
        try {
            root = new JSONObject(rawAmazon);
            JSONObject itemSearchResponse = root.getJSONObject("ItemSearchResponse");
            JSONObject item1 = itemSearchResponse.getJSONObject("Items");
            JSONArray productsList = item1.getJSONArray("Item");
            resetIlists();
            for (int i = 0; i < 10; i++) {
                ItemDetails itemDetail = new ItemDetails();
                JSONObject listItem = productsList.getJSONObject(i);
                JSONObject imageLarge = listItem.getJSONObject("LargeImage");
                itemDetail.imageUrl = (imageLarge.getString("URL"));
                JSONObject itemAttributes = listItem.getJSONObject("ItemAttributes");
                itemDetail.brand = (itemAttributes.getString("Manufacturer"));
                itemDetail.title = (itemAttributes.getString("Title"));
                itemDetail.marketplace = AMAZON;
                String lowestPrice = "Out of Stock";
                try {
                    JSONObject pricess = itemAttributes.getJSONObject("ListPrice");
                    lowestPrice = pricess.getString("FormattedPrice");
                    lowestPrice = "MRP: " + lowestPrice.substring(4, lowestPrice.length() - 3) + "*";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    JSONObject offersummary = listItem.getJSONObject("OfferSummary");
                    JSONObject price = offersummary.getJSONObject("LowestNewPrice");
                    lowestPrice = price.getString("FormattedPrice");
                    lowestPrice = "₹" + lowestPrice.substring(4, lowestPrice.length() - 3);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                itemDetail.url = (listItem.getString("DetailPageURL"));
                itemDetail.price = (lowestPrice);
                itemDetail.rank = i;
                items.add(itemDetail);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        mergeData();

    }

    void processFlipkartJSON() {
        try {
            JSONObject root = new JSONObject(rawFlipkart);
            JSONArray productsList = root.getJSONArray("products");
            resetIlists();
            for (int i = 0; i < 10; i++) {
                ItemDetails itemDetail = new ItemDetails();
                JSONObject listItem = productsList.getJSONObject(i);
                JSONObject productBaseInfo = listItem.getJSONObject("productBaseInfoV1");
                itemDetail.title = (productBaseInfo.getString("title"));
                JSONObject fkrtPrice = productBaseInfo.getJSONObject("flipkartSpecialPrice");
                itemDetail.price = ("₹" + fkrtPrice.getInt("amount"));
                JSONObject imageUrls = productBaseInfo.getJSONObject("imageUrls");
                itemDetail.imageUrl = (imageUrls.getString("400x400"));
                itemDetail.url = (productBaseInfo.getString("productUrl"));
                itemDetail.brand = (productBaseInfo.getString("productBrand"));
                itemDetail.marketplace = (FLIPKART);
                itemDetail.rank = i;
                items.add(itemDetail);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mergeData();

    }

    void mergeData() {
        Collections.sort(items);
    }

    void resetIlists() {
        newItems = new ArrayList<>();
    }

}
