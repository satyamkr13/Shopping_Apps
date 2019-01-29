package com.instinotices.shoppingapps;

class ItemDetails implements Comparable {
    String title, url, imageUrl, price, brand, marketplace;
    int rank;

    @Override
    public int compareTo(Object o) {
        ItemDetails i = (ItemDetails) o;
        return this.rank - i.rank;
    }
}
