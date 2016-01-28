package com.younggeon.whoolite.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.constant.Actions;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.fragment.FrequentlyInputFragment;
import com.younggeon.whoolite.fragment.HistoryFragment;
import com.younggeon.whoolite.provider.WhooingProvider;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.AccountsLoader;
import com.younggeon.whoolite.whooing.loader.SectionsLoader;

@SuppressWarnings("ConstantConditions")
public class WhooLiteActivity extends FinishableActivity implements LoaderManager.LoaderCallbacks {
    private static final int LOADER_ID_SECTIONS = 1;
    private static final int LOADER_ID_REFRESH_SECTIONS = 2;
    private static final int LOADER_ID_REFRESH_ACCOUNTS = 3;

    private Spinner mSectionsSpinner;

    private String mCurrentSectionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String apiKeyFormat = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PreferenceKeys.API_KEY_FORMAT, null);

        if (apiKeyFormat == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();

            return;
        }
        setContentView(R.layout.activity_whoo_lite);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mSectionsSpinner = (Spinner)findViewById(R.id.sections);
        mSectionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SectionsAdapter adapter = (SectionsAdapter)parent.getAdapter();
                Cursor c = adapter.getCursor();

                c.moveToPosition(position);

                String sectionId = c.getString(Sections.COLUMN_INDEX_SECTION_ID);

                if (!sectionId.equals(mCurrentSectionId)) {
                    mCurrentSectionId = sectionId;
                    PreferenceManager.getDefaultSharedPreferences(WhooLiteActivity.this).edit()
                            .putString(PreferenceKeys.CURRENT_SECTION_ID, sectionId).apply();

                    Intent intent = new Intent(Actions.SECTION_ID_CHANGED);

                    intent.putExtra(Actions.EXTRA_SECTION_ID, sectionId);
                    sendBroadcast(intent);
                }
                getSupportLoaderManager().restartLoader(LOADER_ID_REFRESH_ACCOUNTS, null, WhooLiteActivity.this).forceLoad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        findViewById(R.id.write).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentSectionId != null) {
                    Intent intent = new Intent(WhooLiteActivity.this,
                            HistoryDetailActivity.class);

                    intent.putExtra(HistoryDetailActivity.EXTRA_SECTION_ID, mCurrentSectionId);
                    ActivityCompat.startActivity(WhooLiteActivity.this,
                            intent,
                            ActivityOptionsCompat.makeScaleUpAnimation(v,
                                    0,
                                    0,
                                    v.getWidth(),
                                    v.getHeight()
                            ).toBundle());
                }
            }
        });
        getSupportLoaderManager().initLoader(LOADER_ID_SECTIONS, null, this);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        WhooLitePagerAdapter adapter = new WhooLitePagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(adapter.getCount());
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSupportLoaderManager().getLoader(LOADER_ID_REFRESH_SECTIONS) == null) {
            getSupportLoaderManager().initLoader(LOADER_ID_REFRESH_SECTIONS, null, this).forceLoad();
        }
        if (mCurrentSectionId != null && getSupportLoaderManager().getLoader(LOADER_ID_REFRESH_ACCOUNTS) == null) {
            getSupportLoaderManager().initLoader(LOADER_ID_REFRESH_ACCOUNTS, null, this).forceLoad();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_whoo_lite, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout: {
                Utility.logout(this);

                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_SECTIONS: {
                return new CursorLoader(this,
                        WhooingProvider.getSectionsUri(),
                        null,
                        null,
                        null,
                        Sections.COLUMN_SORT_ORDER + " ASC");
            }
            case LOADER_ID_REFRESH_SECTIONS: {
                return new SectionsLoader(this,
                        Request.Method.GET,
                        null);
            }
            case LOADER_ID_REFRESH_ACCOUNTS: {
                Bundle bundle = new Bundle();

                bundle.putString(WhooingKeyValues.SECTION_ID, mCurrentSectionId);
                return new AccountsLoader(this,
                        Request.Method.GET,
                        bundle);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_SECTIONS: {
                Cursor cursor = (Cursor) data;

                if (cursor.getCount() > 0) {
                    SectionsAdapter adapter = (SectionsAdapter)mSectionsSpinner.getAdapter();

                    if (adapter == null) {
                        mSectionsSpinner.setAdapter(new SectionsAdapter(this, cursor));
                    } else {
                        adapter.swapCursor(cursor);
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    String currentSectionId = prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null);

                    cursor.moveToFirst();

                    int position = 0;

                    do {
                        String sectionId = cursor.getString(Sections.COLUMN_INDEX_SECTION_ID);

                        if (sectionId.equals(currentSectionId)) {
                            position = cursor.getPosition();
                            break;
                        }
                    } while (cursor.moveToNext());
                    mSectionsSpinner.setSelection(position);
                }
                break;
            }
            case LOADER_ID_REFRESH_SECTIONS: {
                int resultCode = (Integer) data;

                if (resultCode < 0) {
                    Cursor c = getContentResolver().query(WhooingProvider.getSectionsUri(),
                            null,
                            null,
                            null,
                            null);

                    if (c != null) {
                        if (c.getCount() == 0) {
                            new AlertDialog.Builder(WhooLiteActivity.this)
                                    .setTitle(R.string.no_sections)
                                    .setMessage(R.string.no_sections_message)
                                    .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            getSupportLoaderManager()
                                                    .getLoader(LOADER_ID_REFRESH_SECTIONS).forceLoad();
                                        }
                                    }).setCancelable(false)
                                    .create().show();
                        }
                        c.close();
                    }
                } else {
                    Utility.checkResultCodeWithAlert(this, resultCode);
                }
                getSupportLoaderManager().destroyLoader(LOADER_ID_REFRESH_SECTIONS);
                break;
            }
            case LOADER_ID_REFRESH_ACCOUNTS: {
                int resultCode = (Integer) data;

                if (resultCode >= 0) {
                    Utility.checkResultCodeWithAlert(this, resultCode);
                }
                getSupportLoaderManager().destroyLoader(LOADER_ID_REFRESH_ACCOUNTS);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private class SectionsAdapter extends CursorAdapter {
        public SectionsAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1,
                    parent,
                    false);

            view.setTag(view.findViewById(android.R.id.text1));

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView)view.getTag();

            tv.setText(getString(R.string.section_title_format, cursor.getString(Sections.COLUMN_INDEX_TITLE),
                    cursor.getString(Sections.COLUMN_INDEX_CURRENCY)));
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item_with_memo,
                        parent,
                        false);
                convertView.setTag(new ViewHolder((TextView)convertView.findViewById(R.id.text1),
                        (TextView)convertView.findViewById(R.id.text2)));
            }

            ViewHolder vh = (ViewHolder)convertView.getTag();
            Cursor cursor = getCursor();

            cursor.moveToPosition(position);

            String memo = cursor.getString(Sections.COLUMN_INDEX_MEMO);

            vh.main.setText(getString(R.string.section_title_format, cursor.getString(Sections.COLUMN_INDEX_TITLE),
                    cursor.getString(Sections.COLUMN_INDEX_CURRENCY)));
            if (TextUtils.isEmpty(memo)) {
                vh.sub.setVisibility(View.GONE);
            } else {
                vh.sub.setText(cursor.getString(Sections.COLUMN_INDEX_MEMO));
                vh.sub.setVisibility(View.VISIBLE);
            }

            return convertView;
        }

        private class ViewHolder {
            public TextView main;
            public TextView sub;

            public ViewHolder(TextView main, TextView sub) {
                this.main = main;
                this.sub = sub;
            }
        }
    }

    private class WhooLitePagerAdapter extends FragmentPagerAdapter {
        public WhooLitePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: {
                    return new FrequentlyInputFragment();
                }
                case 1: {
                    return new HistoryFragment();
                }
                default: {
                    return null;
                }
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: {
                    return getString(R.string.frequently_input);
                }
                case 1: {
                    return getString(R.string.entries);
                }
                default: {
                    return super.getPageTitle(position);
                }
            }
        }
    }
}
