package com.younggeon.whoolite.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.ads.AdView;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.util.Utility;

public class HistoryDetailActivity extends FinishableActivity {
    public static final String EXTRA_SECTION_ID = "section_id";
    public static final String EXTRA_ENTRY_ID = "entry_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        Utility.setAdView(mAdView = (AdView) findViewById(R.id.adview));
    }
}
