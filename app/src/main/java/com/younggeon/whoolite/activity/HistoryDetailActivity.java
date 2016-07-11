package com.younggeon.whoolite.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.ads.AdView;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.util.Utility;

public class HistoryDetailActivity extends FinishableActivity {
    public static final String EXTRA_SECTION_ID = "section_id";
    public static final String EXTRA_ENTRY_ID = "entry_id";
    public static final String EXTRA_COPY = "copy";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MONEY = "money";
    public static final String EXTRA_LEFT_ACCOUNT_TYPE = "left_account_type";
    public static final String EXTRA_LEFT_ACCOUNT_ID = "left_account_id";
    public static final String EXTRA_RIGHT_ACCOUNT_TYPE = "right_account_type";
    public static final String EXTRA_RIGHT_ACCOUNT_ID = "right_account_id";
    public static final String EXTRA_MEMO = "memo";

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
