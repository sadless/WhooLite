package com.younggeon.whoolite.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.google.android.gms.ads.AdView;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.constant.Actions;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.fragment.FrequentlyInputFragment;
import com.younggeon.whoolite.fragment.HistoryFragment;
import com.younggeon.whoolite.provider.WhooingProvider;
import com.younggeon.whoolite.realm.FrequentItem;
import com.younggeon.whoolite.realm.Section;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.AccountsLoader;
import com.younggeon.whoolite.whooing.loader.SectionsLoader;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class WhooLiteActivity extends FinishableActivity implements LoaderManager.LoaderCallbacks {
    private static final int LOADER_ID_REFRESH_SECTIONS = 1;
    private static final int LOADER_ID_REFRESH_ACCOUNTS = 2;

    private Spinner mSectionsSpinner;

    private String mCurrentSectionId;
    private Realm mRealm;
    private RealmResults<Section> mSections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String apiKeyFormat = prefs.getString(PreferenceKeys.API_KEY_FORMAT, null);

        if (apiKeyFormat == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();

            return;
        }
        setContentView(R.layout.activity_whoo_lite);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ActionBar ab = getSupportActionBar();

        if (ab != null) {
            ab.setDisplayShowTitleEnabled(false);
        }
        mSectionsSpinner = (Spinner)findViewById(R.id.sections);
        mSectionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Section section = mSections.get(position);
                String sectionId = section.getSectionId();

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

        View writeButton = findViewById(R.id.write);

        if (writeButton != null) {
            writeButton.setOnClickListener(new View.OnClickListener() {
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
        }
        mRealm = Realm.getDefaultInstance();
        mSections = mRealm.where(Section.class).findAllSortedAsync("sortOrder", Sort.ASCENDING);
        mSections.addChangeListener(new RealmChangeListener<RealmResults<Section>>() {
            @Override
            public void onChange(RealmResults<Section> element) {
                sectionChanged();
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        WhooLitePagerAdapter adapter = new WhooLitePagerAdapter(getSupportFragmentManager());

        if (pager != null) {
            pager.setAdapter(adapter);
            pager.setOffscreenPageLimit(adapter.getCount());
        }
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(pager);
        }
        Utility.setAdView(mAdView = (AdView) findViewById(R.id.adview));
        if (!prefs.getBoolean(PreferenceKeys.MIGRATE_TO_REALM, false)) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Cursor c = getContentResolver().query(WhooingProvider.getSectionsUri(),
                            new String[] {Sections.COLUMN_SECTION_ID},
                            null,
                            null,
                            null);

                    if (c != null) {
                        if (c.moveToFirst()) {
                            Realm realm = Realm.getDefaultInstance();

                            realm.beginTransaction();
                            do {
                                String sectionId = c.getString(0);
                                Cursor d = getContentResolver().query(WhooingProvider.getFrequentItemsUri(sectionId),
                                        null,
                                        null,
                                        null,
                                        null);

                                if (d != null) {
                                    if (d.moveToFirst()) {
                                        FrequentItem object = new FrequentItem();

                                        object.setSectionId(sectionId);
                                        object.setSlotNumber(d.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER));
                                        object.setItemId(d.getString(FrequentItems.COLUMN_INDEX_ITEM_ID));
                                        object.setSortOrder(d.getInt(FrequentItems.COLUMN_INDEX_SORT_ORDER));
                                        object.setTitle(d.getString(FrequentItems.COLUMN_INDEX_TITLE));
                                        object.setMoney(d.getDouble(FrequentItems.COLUMN_INDEX_MONEY));
                                        object.setLeftAccountType(d.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_TYPE));
                                        object.setLeftAccountId(d.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_ID));
                                        object.setRightAccountType(d.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE));
                                        object.setRightAccountId(d.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_ID));
                                        object.composePrimaryKey();
                                        realm.copyToRealmOrUpdate(object);
                                    }
                                    d.close();
                                }
                                getContentResolver().delete(WhooingProvider.getAccountsUri(sectionId), null, null);
                                getContentResolver().delete(WhooingProvider.getFrequentItemsUri(sectionId), null, null);
                                getContentResolver().delete(WhooingProvider.getEntriesUri(sectionId), null, null);
                            } while (c.moveToNext());
                            realm.commitTransaction();
                            realm.close();
                        }
                        c.close();
                    }
                    getContentResolver().delete(WhooingProvider.getSectionsUri(), null, null);

                    return null;
                }
            }.execute();
            prefs.edit().putBoolean(PreferenceKeys.MIGRATE_TO_REALM, true).apply();
        }
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
    protected void onDestroy() {
        super.onDestroy();
        if (mSections != null) {
            mSections.removeChangeListeners();
            mRealm.close();
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
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
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
            case LOADER_ID_REFRESH_SECTIONS: {
                int resultCode = (Integer) data;

                if (resultCode < 0) {
                    Realm realm = Realm.getDefaultInstance();
                    RealmResults<Section> sections = realm.where(Section.class).findAll();

                    if (sections.size() == 0) {
                        new AlertDialog.Builder(WhooLiteActivity.this)
                                .setTitle(R.string.no_sections)
                                .setMessage(R.string.no_sections_message)
                                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        getSupportLoaderManager()
                                                .initLoader(LOADER_ID_REFRESH_SECTIONS,
                                                        null,
                                                        WhooLiteActivity.this).forceLoad();
                                    }
                                }).setCancelable(false)
                                .create().show();
                    }
                    realm.close();
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

    private void sectionChanged() {
        if (mSections.size() > 0) {
            SectionsAdapter adapter = (SectionsAdapter)mSectionsSpinner.getAdapter();

            if (adapter == null) {
                mSectionsSpinner.setAdapter(new SectionsAdapter());
            } else {
                adapter.notifyDataSetChanged();
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String currentSectionId = prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null);

            int position = 0;

            for (Section section : mSections) {
                String sectionId = section.getSectionId();

                if (sectionId.equals(currentSectionId)) {
                    break;
                }
                position++;
            }
            mSectionsSpinner.setSelection(position);
        }
    }

    private class SectionsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mSections.size();
        }

        @Override
        public Section getItem(int position) {
            return mSections.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getSectionId().hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1,
                        parent,
                        false);
                convertView.setTag(convertView.findViewById(android.R.id.text1));
            }

            TextView tv = (TextView) convertView.getTag();
            Section section = getItem(position);

            tv.setText(getString(R.string.section_title_format, section.getTitle(),
                    section.getCurrency()));

            return convertView;
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
            Section section = getItem(position);
            String memo = section.getMemo();

            vh.main.setText(getString(R.string.section_title_format, section.getTitle(),
                    section.getCurrency()));
            if (TextUtils.isEmpty(memo)) {
                vh.sub.setVisibility(View.GONE);
            } else {
                vh.sub.setText(memo);
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
