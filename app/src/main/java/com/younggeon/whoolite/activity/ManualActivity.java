package com.younggeon.whoolite.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.fragment.ManualActivityFragment;

public class ManualActivity extends AppCompatActivity {
    private static final String INSTANCE_STATE_START_ENABLED = "start_enabled";

    private ViewPager mPager;
    private Button mNextButton;
    private MenuItem mSkipMenuItem;

    private boolean mStartEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        mPager = (ViewPager) findViewById(R.id.pager);

        ManualPagerAdapter adapter = new ManualPagerAdapter(getSupportFragmentManager());

        mPager.setAdapter(adapter);
        mPager.setOffscreenPageLimit(adapter.getCount());
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == mPager.getAdapter().getCount() - 1) {
                    enableStart();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mNextButton = (Button) findViewById(R.id.next);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPager.getCurrentItem() < mPager.getAdapter().getCount() - 1) {
                    mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                } else {
                    startActivity(new Intent(ManualActivity.this, WhooLiteActivity.class));
                    finish();
                }
            }
        });
        if (savedInstanceState != null) {
            mStartEnabled = savedInstanceState.getBoolean(INSTANCE_STATE_START_ENABLED);
            if (mStartEnabled) {
                enableStart();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(INSTANCE_STATE_START_ENABLED, mStartEnabled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_manual, menu);
        mSkipMenuItem = menu.getItem(0);
        if (mStartEnabled) {
            mSkipMenuItem.setTitle(R.string.start);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_skip: {
                startActivity(new Intent(ManualActivity.this, WhooLiteActivity.class));
                finish();

                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void enableStart() {
        mNextButton.setText(R.string.start);
        if (mSkipMenuItem != null) {
            mSkipMenuItem.setTitle(R.string.start);
        }
        mStartEnabled = true;
    }

    private class ManualPagerAdapter extends FragmentPagerAdapter {
        private final int[] DRAWABLE_IDS = new int[] {
                R.drawable.manual_1,
                R.drawable.manual_2,
                R.drawable.manual_3,
        };
        private final int[] TEXT_IDS = new int[] {
                R.string.manual_1,
                R.string.manual_2,
                R.string.manual_3,
        };

        public ManualPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            ManualActivityFragment fragment = new ManualActivityFragment();
            Bundle arguments = new Bundle();

            arguments.putInt(ManualActivityFragment.ARG_DRAWABLE_ID, DRAWABLE_IDS[position]);
            arguments.putInt(ManualActivityFragment.ARG_TEXT_ID, TEXT_IDS[position]);
            fragment.setArguments(arguments);

            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
