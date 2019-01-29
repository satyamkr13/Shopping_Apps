package com.instinotices.shoppingapps;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ViewHolder> {
    public final static int MODE_SEARCH = 345, MODE_WATCHLIST = 346, MODE_OFFERS = 347;
    final private ProductClickListener productClickListener;
    public ProductListHelper productListHelper;
    public int mode;
    Context context;
    private RequestManager glide;

    public ProductsAdapter(RequestManager glide, Context context, ProductListHelper productListHelper, ProductClickListener productClickListener, int mode) {
        this.context = context;
        this.productListHelper = productListHelper;
        this.productClickListener = productClickListener;
        this.glide = glide;
        this.mode = mode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(layoutInflater.inflate(R.layout.search_item, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mode == MODE_SEARCH) {
            holder.bindForSearch(position);
        }
    }

    @Override
    public int getItemCount() {
        return productListHelper.items.size();
    }

    public interface ProductClickListener {
        void onItemClick(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView title, price, brand;
        CardView cardView;
        ImageView imageView, marketPlaceImage;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.search_title);
            price = itemView.findViewById(R.id.search_price);
            imageView = itemView.findViewById(R.id.search_image);
            marketPlaceImage = itemView.findViewById(R.id.search_marketplace);
            cardView = itemView.findViewById(R.id.search_card);
            brand = itemView.findViewById(R.id.search_brand);
            cardView.setOnClickListener(this);
        }

        void bindForSearch(int position) {
            title.setText(productListHelper.items.get(position).title);
            price.setText(productListHelper.items.get(position).price);
            brand.setText("Manufacturer: " + productListHelper.items.get(position).brand);
            glide.load(productListHelper.items.get(position).imageUrl).into(imageView);
            if (productListHelper.items.get(position).marketplace.equals(ProductListHelper.AMAZON)) {
                glide.load(R.drawable.amazon_search).into(marketPlaceImage);
            } else {
                marketPlaceImage.setImageDrawable(context.getResources().getDrawable(R.drawable.flipkart_search));
            }
        }

        @Override
        public void onClick(View view) {
            productClickListener.onItemClick(getAdapterPosition());
        }
    }
}
