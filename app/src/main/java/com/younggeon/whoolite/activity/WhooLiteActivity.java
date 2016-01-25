package com.younggeon.whoolite.activity;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
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
import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.Actions;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Accounts;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.fragment.FrequentlyInputFragment;
import com.younggeon.whoolite.fragment.HistoryFragment;
import com.younggeon.whoolite.provider.WhooingProvider;
import com.younggeon.whoolite.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("ConstantConditions")
public class WhooLiteActivity extends FinishableActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID_SECTIONS = 1;

    private Spinner mSectionsSpinner;

    private String mApiKeyFormat;
    private String mCurrentSectionId;

    private AdapterView.OnItemSelectedListener mSectionSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SectionsAdapter adapter = (SectionsAdapter)parent.getAdapter();
            Cursor c = adapter.getCursor();

            c.moveToPosition(position);

            String sectionId = c.getString(Sections.COLUMN_INDEX_SECTION_ID);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WhooLiteActivity.this);
            String oldSectionId = prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null);

            if (!sectionId.equals(oldSectionId)) {
                mCurrentSectionId = sectionId;
                prefs.edit().putString(PreferenceKeys.CURRENT_SECTION_ID, sectionId).apply();

                Intent intent = new Intent(Actions.SECTION_ID_CHANGED);

                intent.putExtra(Actions.EXTRA_SECTION_ID, sectionId);
                sendBroadcast(intent);
            }
            receiveAccounts(sectionId);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApiKeyFormat = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PreferenceKeys.API_KEY_FORMAT, null);
        if (mApiKeyFormat == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();

            return;
        }
        setContentView(R.layout.activity_whoo_lite);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mSectionsSpinner = (Spinner)findViewById(R.id.sections);
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

        receiveSections();
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                WhooingProvider.getSectionsUri(),
                null,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() > 0) {
            SectionsAdapter adapter = (SectionsAdapter)mSectionsSpinner.getAdapter();

            mSectionsSpinner.setOnItemSelectedListener(null);
            if (adapter == null) {
                mSectionsSpinner.setAdapter(new SectionsAdapter(this, data));
            } else {
                adapter.swapCursor(data);
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            mCurrentSectionId = prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null);
            data.moveToFirst();
            if (mCurrentSectionId == null) {
                mSectionsSpinner.setOnItemSelectedListener(mSectionSelectedListener);
                mSectionsSpinner.setSelection(0);
            } else {
                int position = 0;

                do {
                    String sectionId = data.getString(Sections.COLUMN_INDEX_SECTION_ID);

                    if (sectionId.equals(mCurrentSectionId)) {
                        position = data.getPosition();
                        break;
                    }
                } while (data.moveToNext());
                mSectionsSpinner.setOnItemSelectedListener(mSectionSelectedListener);
                mSectionsSpinner.setSelection(position);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void receiveSections() {
        new AsyncTask<String, Void, Integer>() {
            private static final String URI_SECTIONS = "https://whooing.com/api/sections.json_array";

            @Override
            protected Integer doInBackground(String... params) {
                if (mApiKeyFormat != null) {
                    RequestFuture<String> requestFuture = RequestFuture.newFuture();

                    WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(Request.Method.GET,
                            URI_SECTIONS,
                            requestFuture,
                            requestFuture,
                            mApiKeyFormat));

                    try {
                        JSONObject result = new JSONObject(requestFuture.get(10, TimeUnit.SECONDS));
                        int resultCode = result.optInt(WhooingKeyValues.CODE);

                        if (resultCode == WhooingKeyValues.SUCCESS) {
                            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                            JSONArray sections = result.optJSONArray(WhooingKeyValues.RESULT);
                            Uri sectionsUri = WhooingProvider.getSectionsUri();

                            operations.add(ContentProviderOperation.newDelete(sectionsUri).build());
                            for (int i = 0; i < sections.length(); i++) {
                                JSONObject section = sections.optJSONObject(i);

                                operations.add(ContentProviderOperation.newInsert(sectionsUri)
                                        .withValue(Sections.COLUMN_SECTION_ID,
                                                section.optString(WhooingKeyValues.SECTION_ID))
                                        .withValue(Sections.COLUMN_TITLE,
                                                section.optString(WhooingKeyValues.TITLE))
                                        .withValue(Sections.COLUMN_MEMO,
                                                section.optString(WhooingKeyValues.MEMO))
                                        .withValue(Sections.COLUMN_CURRENCY,
                                                section.optString(WhooingKeyValues.CURRENCY))
                                        .withValue(Sections.COLUMN_DATE_FORMAT,
                                                section.optString(WhooingKeyValues.DATE_FORMAT)).build());
                            }
                            try {
                                getContentResolver().applyBatch(getString(R.string.whooing_authority),
                                        operations);
                            } catch (RemoteException | OperationApplicationException e) {
                                e.printStackTrace();
                            }
                        }

                        return resultCode;
                    } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
                        e.printStackTrace();

                        return -1;
                    }
                } else {
                    return -1;
                }
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);

                boolean failed;

                failed = integer < 0 || !Utility.checkResultCodeWithAlert(WhooLiteActivity.this, integer);
                if (failed) {
                    Cursor c = getContentResolver().query(WhooingProvider.getSectionsUri(),
                            null,
                            null,
                            null,
                            null);

                    if (c.getCount() == 0) {
                        new AlertDialog.Builder(WhooLiteActivity.this)
                                .setTitle(R.string.no_sections)
                                .setMessage(R.string.no_sections_message)
                                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        receiveSections();
                                    }
                                }).setCancelable(false)
                                .create().show();
                    }
                    c.close();
                }
            }
        }.execute();
    }

    private void receiveAccounts(String sectionId) {
        new AsyncTask<String, Void, Integer>() {
            private static final String URI_ACCOUNTS = "https://whooing.com/api/accounts.json_array";

            private static final String GROUP = "group";

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);

                if (integer > 0) {
                    Utility.checkResultCodeWithAlert(WhooLiteActivity.this, integer);
                }
            }

            @Override
            protected Integer doInBackground(String... params) {
                RequestFuture<String> requestFuture = RequestFuture.newFuture();
                Uri.Builder builder = Uri.parse(URI_ACCOUNTS).buildUpon();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

                builder.appendQueryParameter(WhooingKeyValues.SECTION_ID, params[0])
                    .appendQueryParameter(WhooingKeyValues.START_DATE, dateFormat.format(new Date()));

                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(Request.Method.GET,
                        builder.build().toString(),
                        requestFuture,
                        requestFuture,
                        mApiKeyFormat));

                try {
                    JSONObject result = new JSONObject(requestFuture.get(10, TimeUnit.SECONDS));
                    int resultCode;

                    if (result == null) {
                        return -1;
                    } else {
                        resultCode = result.optInt(WhooingKeyValues.CODE);
                        if (resultCode == WhooingKeyValues.SUCCESS) {
                            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                            JSONObject accounts = result.optJSONObject(WhooingKeyValues.RESULT);
                            Iterator<String> keys = accounts.keys();
                            Uri accountsUri = WhooingProvider.getAccountsUri(params[0]);

                            operations.add(ContentProviderOperation.newDelete(accountsUri).build());
                            while (keys.hasNext()) {
                                String accountType = keys.next();
                                JSONArray itemsInAccountType = accounts.optJSONArray(accountType);

                                for (int i = 0; i < itemsInAccountType.length(); i++) {
                                    JSONObject item = itemsInAccountType.optJSONObject(i);

                                    operations.add(ContentProviderOperation.newInsert(accountsUri)
                                        .withValue(Accounts.COLUMN_ACCOUNT_TYPE, accountType)
                                        .withValue(Accounts.COLUMN_ACCOUNT_ID,
                                                item.optString(WhooingKeyValues.ACCOUNT_ID))
                                        .withValue(Accounts.COLUMN_TITLE,
                                                item.optString(WhooingKeyValues.TITLE))
                                        .withValue(Accounts.COLUMN_MEMO,
                                                item.optString(WhooingKeyValues.MEMO))
                                        .withValue(Accounts.COLUMN_OPEN_DATE,
                                                item.optString(WhooingKeyValues.OPEN_DATE))
                                        .withValue(Accounts.COLUMN_END_DATE,
                                                item.optString(WhooingKeyValues.END_DATE))
                                        .withValue(Accounts.COLUMN_IS_GROUP,
                                                item.optString(WhooingKeyValues.TYPE).equals(GROUP)).build());
                                }
                            }
                            try {
                                getContentResolver().applyBatch(getString(R.string.whooing_authority),
                                        operations);
                            } catch (RemoteException | OperationApplicationException e) {
                                e.printStackTrace();
                            }
                        }

                        return resultCode;
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
                    e.printStackTrace();

                    return -1;
                }
            }
        }.execute(sectionId);
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
