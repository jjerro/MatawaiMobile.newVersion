package com.dalehi.matawaimobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Dapoer_Kreatif on 15/04/2018.
 */

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start home activity
        startActivity(new Intent (SplashActivity.this, MainActivity.class));
        // close splash activity
        finish();
    }
}
