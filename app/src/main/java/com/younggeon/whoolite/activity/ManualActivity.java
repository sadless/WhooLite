package com.younggeon.whoolite.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.databinding.ActivityManualBinding;
import com.younggeon.whoolite.fragment.ManualActivityFragment;

public class ManualActivity extends AppCompatActivity {
    private static final String INSTANCE_STATE_START_ENABLED = "start_enabled";

    private MenuItem mSkipMenuItem;
    private ActivityManualBinding mBinding;

    private ObservableBoolean mStartEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mStartEnabled = new ObservableBoolean(false);
        } else {
            mStartEnabled = new ObservableBoolean(savedInstanceState.getBoolean(INSTANCE_STATE_START_ENABLED));
        }
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_manual);
        mBinding.setActivity(this);
        mBinding.setStartEnabled(mStartEnabled);
        setSupportActionBar(mBinding.toolbar);

        ManualPagerAdapter adapter = new ManualPagerAdapter(getSupportFragmentManager());

        mBinding.viewPager.setAdapter(adapter);
        mBinding.viewPager.setOffscreenPageLimit(adapter.getCount());
        mBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == mBinding.viewPager.getAdapter().getCount() - 1) {
                    mStartEnabled.set(true);
                    mSkipMenuItem.setTitle(R.string.start);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(INSTANCE_STATE_START_ENABLED, mStartEnabled.get());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_manual, menu);
        mSkipMenuItem = menu.getItem(0);
        if (mStartEnabled.get()) {
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

    public void nextClicked() {
        if (mBinding.viewPager.getCurrentItem() < mBinding.viewPager.getAdapter().getCount() - 1) {
            mBinding.viewPager.setCurrentItem(mBinding.viewPager.getCurrentItem() + 1);
        } else {
            startActivity(new Intent(ManualActivity.this, WhooLiteActivity.class));
            finish();
        }
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
