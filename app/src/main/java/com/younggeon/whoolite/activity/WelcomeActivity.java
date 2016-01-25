package com.younggeon.whoolite.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.fragment.WelcomeActivityFragment;

public class WelcomeActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_WHOOING_LOGIN = 1;

    private static final String INSTANCE_STATE_LOGIN_BUTTON_ENABLED = "login_button_enabled";

    private ViewPager mPager;
    private Button mNextButton;
    private boolean mLoginButtonEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        if (savedInstanceState != null) {
            mLoginButtonEnabled = savedInstanceState.getBoolean(INSTANCE_STATE_LOGIN_BUTTON_ENABLED);
        }
        mPager = (ViewPager) findViewById(R.id.pager);

        WelcomePagerAdapter adapter = new WelcomePagerAdapter(getSupportFragmentManager());

        mPager.setAdapter(adapter);
        mPager.setOffscreenPageLimit(adapter.getCount());
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 3: {
                        mLoginButtonEnabled = true;
                        enableLoginButton();
                        break;
                    }
                    default:
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
                if (mLoginButtonEnabled) {
                    startActivityForResult(new Intent(WelcomeActivity.this, WhooingLoginActivity.class),
                            REQUEST_CODE_WHOOING_LOGIN);
                } else {
                    mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                }
            }
        });
        if (mLoginButtonEnabled) {
            enableLoginButton();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(INSTANCE_STATE_LOGIN_BUTTON_ENABLED, mLoginButtonEnabled);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_WHOOING_LOGIN: {
                if (resultCode == RESULT_OK) {
                    PreferenceManager.getDefaultSharedPreferences(this).edit()
                            .putString(PreferenceKeys.API_KEY_FORMAT, data.getStringExtra(WhooingLoginActivity.EXTRA_API_KEY_FORMAT))
                            .commit();
                    startActivity(new Intent(this, WhooLiteActivity.class));
                    finish();
                }
                break;
            }
            default:
        }
    }

    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_welcome, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    private void enableLoginButton() {
        mNextButton.setText(R.string.login);
        mNextButton.getBackground().setColorFilter(0xFF1C1C1C, PorterDuff.Mode.MULTIPLY);
        mNextButton.setTextColor(0xFFFFFFFF);
    }

    private class WelcomePagerAdapter extends FragmentPagerAdapter {
        public WelcomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            Bundle arguments = new Bundle();

            switch (position) {
                case 0: {
                    fragment = new WelcomeActivityFragment();
                    arguments.putInt(WelcomeActivityFragment.ARGUMENT_LAYOUT_ID, R.layout.fragment_welcome_1);
                    break;
                }
                case 1: {
                    fragment = new WelcomeActivityFragment();
                    arguments.putInt(WelcomeActivityFragment.ARGUMENT_LAYOUT_ID, R.layout.fragment_welcome_2);
                    break;
                }
                case 2: {
                    fragment = new WelcomeActivityFragment();
                    arguments.putInt(WelcomeActivityFragment.ARGUMENT_LAYOUT_ID, R.layout.fragment_welcome_3);
                    break;
                }
                case 3: {
                    fragment = new WelcomeActivityFragment();
                    arguments.putInt(WelcomeActivityFragment.ARGUMENT_LAYOUT_ID, R.layout.fragment_welcome_4);
                    break;
                }
                default: {
                    return null;
                }
            }
            fragment.setArguments(arguments);

            return fragment;
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}
