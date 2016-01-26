package com.younggeon.whoolite.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.fragment.WhooingLoginActivityFragment;

public class WhooingLoginActivity extends AppCompatActivity {
    public static final String EXTRA_API_KEY_FORMAT = "api_key_format";

    private WhooingLoginActivityFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whooing_login);

        mFragment = (WhooingLoginActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
    }

    @Override
    public void onBackPressed() {
        if (!mFragment.webViewGoBack()) {
            super.onBackPressed();
        }
    }
}
