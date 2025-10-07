package com.example.peterfood;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    private static final String LOGO_URL = "https://drive.google.com/uc?export=download&id=1A0wNVSle3_c-iKoYuyCgrxLxG0Wox5AJ"; // Link logo Google Drive

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void loadLogo() {
        ImageView ivLogo = findViewById(R.id.ivLogo);
        if (ivLogo != null) {
            Log.d(TAG, "Loading logo from: " + LOGO_URL);
            Glide.with(this)
                    .load(LOGO_URL)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontTransform()
                    .override(600, 600)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Glide load failed (logo): " + (e != null ? e.getMessage() : "Unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Glide loaded logo from: " + dataSource);
                            return false;
                        }
                    })
                    .into(ivLogo);
        } else {
            Log.w(TAG, "ImageView ivLogo not found in layout");
        }
    }
}