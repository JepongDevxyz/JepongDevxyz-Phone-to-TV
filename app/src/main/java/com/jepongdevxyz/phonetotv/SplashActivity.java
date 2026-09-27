package com.jepongdevxyz.phonetotv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(6,16,29));
        getWindow().setNavigationBarColor(Color.rgb(6,16,29));
        ImageView splash = new ImageView(this);
        splash.setBackgroundColor(Color.rgb(6,16,29));
        splash.setImageResource(R.drawable.brand_splash);
        boolean wide = getResources().getConfiguration().screenWidthDp > getResources().getConfiguration().screenHeightDp;
        // Preserve the entire supplied artwork on phones and TVs. FIT_CENTER prevents cropping
        // or stretching; the dark background fills any letterbox area on wide TV screens.
        splash.setAdjustViewBounds(true);
        splash.setScaleType(ImageView.ScaleType.FIT_CENTER);
        splash.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1200);
    }
}
