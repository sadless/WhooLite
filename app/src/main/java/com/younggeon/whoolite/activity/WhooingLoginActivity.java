package com.younggeon.whoolite.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.younggeon.whoolite.R;

public class WhooingLoginActivity extends AppCompatActivity {
    public static final String EXTRA_API_KEY_FORMAT = "api_key_format";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whooing_login);
    }

}
