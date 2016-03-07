package com.younggeon.whoolite.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.ads.AdView;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.util.Utility;

public class FrequentlyInputItemDetailActivity extends FinishableActivity {
    public static final String EXTRA_SECTION_ID = "section_id";
    public static final String EXTRA_ITEM_ID = "item_id";
    public static final String EXTRA_SLOT_NUMBER = "slot_number";
    public static final String EXTRA_MODE = "mode";

    public static final int MODE_EDIT = 1;
    public static final int MODE_COMPLETE = 2;

    public static final String RESULT_EXTRA_SLOT_NUMBER = "slot_number";
    public static final String RESULT_EXTRA_ITEM_ID = "item_id";
    public static final String RESULT_EXTRA_ITEM_TITLE = "item_title";
    public static final String RESULT_EXTRA_MONEY = "money";
    public static final String RESULT_EXTRA_LEFT_ACCOUNT_TYPE = "left_account_type";
    public static final String RESULT_EXTRA_LEFT_ACCOUNT_ID = "left_account_id";
    public static final String RESULT_EXTRA_RIGHT_ACCOUNT_TYPE = "right_account_type";
    public static final String RESULT_EXTRA_RIGHT_ACCOUNT_ID = "right_account_id";
    public static final String RESULT_EXTRA_MEMO = "memo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequently_input_item_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        Utility.setAdView(mAdView = (AdView) findViewById(R.id.adview));
    }
}
