package com.younggeon.whoolite.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.fragment.HistoryFragment;

public class SelectMergingEntryActivity extends FinishableActivity {
    public static final String EXTRA_MERGE_ARGUMENTS = "merge_arguments";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_merging_entry);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        if (savedInstanceState == null) {
            HistoryFragment fragment = new HistoryFragment();
            Bundle arguments = new Bundle();

            arguments.putBundle(HistoryFragment.ARG_MERGE_ARGUMENTS, getIntent().getBundleExtra(EXTRA_MERGE_ARGUMENTS));
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();

                return true;
            }
            default: {
                return false;
            }
        }
    }
}
