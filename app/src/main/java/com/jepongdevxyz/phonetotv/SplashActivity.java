package com.jepongdevxyz.phonetotv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w=getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=30){
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController ctl=w.getInsetsController();
            if(ctl!=null){ctl.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());ctl.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
        }else{
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        ImageView splash = new ImageView(this);
        splash.setBackgroundColor(Color.rgb(6,16,29));
        splash.setImageResource(R.drawable.brand_splash);
        // Edge-to-edge splash: fill the entire physical display. CENTER_CROP preserves aspect ratio
        // and removes the top/bottom letterbox bars seen with FIT_CENTER.
        splash.setAdjustViewBounds(false);
        splash.setScaleType(ImageView.ScaleType.CENTER_CROP);
        setContentView(splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1200);
    }
}
