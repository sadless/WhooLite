package com.younggeon.whoolite.fragment;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.HistoryDetailActivity;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.realm.Entry;
import com.younggeon.whoolite.realm.Section;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.EntriesLoader;
import com.younggeon.whoolite.whooing.loader.FrequentItemsLoader;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import io.realm.Realm;

/**
 * Created by sadless on 2016. 1. 23..
 */
public class HistoryDetailActivityFragment extends DetailActivityBaseFragment {
    private static final String INSTANCE_STATE_ENTRY_DATE = "entry_date";
    private static final String INSTANCE_STATE_SELECT_SLOT_NUMBER_DIALOG = "select_slot_number_dialog";

    private static final int LOADER_ID_BOOKMARK = 1;
    private static final int LOADER_ID_EDIT_SEND = 2;

    private Button mDate;
    private AlertDialog mSelectSlotNumberDialog;

    private int mEntryDate;
    private long mEntryId;
    private SimpleDateFormat mSectionDateFormat;
    private String[] mSlotNumberItems;
    private SimpleDateFormat mEntryDateFormat;

    @Override
    protected void initialize() {
        if (mEntryId >= 0) {
            Realm realm = Realm.getDefaultInstance();
            Entry entry = realm.where(Entry.class).equalTo("sectionId", mSectionId)
                    .equalTo("entryId", mEntryId).findFirst();

            if (entry != null) {
                mEntryDate = entry.getEntryDate();
                double money = entry.getMoney();
                mTitle.setText(entry.getTitle());
                if (money >= WhooingKeyValues.EPSILON) {
                    mMoney.setText(new DecimalFormat().format(money).replace(",", ""));
                }
                mMemo.setText(entry.getMemo());
                mLeftAccountType = entry.getLeftAccountType();
                mLeftAccountId = entry.getLeftAccountId();
                mRightAccountType = entry.getRightAccountType();
                mRightAccountId = entry.getRightAccountId();
            }
            realm.close();
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else {
            Intent intent = getActivity().getIntent();

            mEntryDate = Integer.parseInt(mEntryDateFormat.format(new Date()));
            if (intent.getBooleanExtra(HistoryDetailActivity.EXTRA_COPY, false)) {
                mTitle.setText(intent.getStringExtra(HistoryDetailActivity.EXTRA_TITLE));
                mMoney.setText(intent.getStringExtra(HistoryDetailActivity.EXTRA_MONEY));
                mLeftAccountType = intent.getStringExtra(HistoryDetailActivity.EXTRA_LEFT_ACCOUNT_TYPE);
                mLeftAccountId = intent.getStringExtra(HistoryDetailActivity.EXTRA_LEFT_ACCOUNT_ID);
                mRightAccountType = intent.getStringExtra(HistoryDetailActivity.EXTRA_RIGHT_ACCOUNT_TYPE);
                mRightAccountId = intent.getStringExtra(HistoryDetailActivity.EXTRA_RIGHT_ACCOUNT_ID);
                mMemo.setText(intent.getStringExtra(HistoryDetailActivity.EXTRA_MEMO));
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayoutId = R.layout.fragment_history_detail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();

        mEntryId = intent.getLongExtra(HistoryDetailActivity.EXTRA_ENTRY_ID, -1);
        mEntryDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view != null) {
            mDate = (Button) view.findViewById(R.id.date);
            mDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int dateValue = mEntryDate;
                    int year = dateValue / 10000;
                    int month, date;

                    dateValue %= 10000;
                    month = dateValue / 100;
                    date = dateValue % 100;
                    new DatePickerDialog(getActivity(),
                            new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                    mEntryDate = year * 10000 + (monthOfYear + 1) * 100 + dayOfMonth;

                                    GregorianCalendar date = new GregorianCalendar(year,
                                            monthOfYear,
                                            dayOfMonth);

                                    mDate.setText(mSectionDateFormat.format(date.getTime()));
                                }
                            }, year,
                            month - 1,
                            date).show();
                    }
            });
        }

        Section section = Realm.getDefaultInstance().where(Section.class).equalTo("sectionId", mSectionId).findFirst();

        if (section != null) {
            mSectionDateFormat = Utility.getDateFormatFromWhooingDateFormat(
                    section.getDateFormat());
        }
        if (savedInstanceState != null) {
            mEntryDate = savedInstanceState.getInt(INSTANCE_STATE_ENTRY_DATE);
            if (savedInstanceState.getBoolean(INSTANCE_STATE_SELECT_SLOT_NUMBER_DIALOG)) {
                showSelectSlotNumberDialog();
            }
        }
        try {
            mDate.setText(mSectionDateFormat.format(mEntryDateFormat.parse("" + mEntryDate)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        getLoaderManager().initLoader(LOADER_ID_BOOKMARK, null, this);
        if (mEntryId >= 0) {
            getLoaderManager().initLoader(LOADER_ID_EDIT_SEND, null, this);
            getLoaderManager().initLoader(LOADER_ID_DELETE, null, this);
        } else {
            getLoaderManager().initLoader(LOADER_ID_SEND, null, this);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(INSTANCE_STATE_ENTRY_DATE, mEntryDate);
        if (mSelectSlotNumberDialog != null && mSelectSlotNumberDialog.isShowing()) {
            outState.putBoolean(INSTANCE_STATE_SELECT_SLOT_NUMBER_DIALOG, true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (mEntryId >= 0) {
            inflater.inflate(R.menu.menu_history_detail, menu);
        } else {
            inflater.inflate(R.menu.menu_history_detail_write, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bookmark: {
                if (TextUtils.isEmpty(mTitle.getText())) {
                    mTitle.setError(getString(R.string.input_item_title));
                } else {
                    showSelectSlotNumberDialog();
                }

                return true;
            }
            case R.id.action_send: {
                if (TextUtils.isEmpty(mTitle.getText())) {
                    mTitle.setError(getString(R.string.input_item_title));
                } else if (TextUtils.isEmpty(mLeftAccountType)) {
                    mLeft.performClick();
                } else if (TextUtils.isEmpty(mRightAccountType)) {
                    mRight.performClick();
                } else {
                    mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

                    if (mEntryId >= 0) {
                        EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().getLoader(LOADER_ID_EDIT_SEND));
                        Bundle args = new Bundle();

                        args.putLong(WhooingKeyValues.ENTRY_ID, mEntryId);
                        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                        args.putString(WhooingKeyValues.ENTRY_DATE, "" + mEntryDate);
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, mLeftAccountType);
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID, mLeftAccountId);
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, mRightAccountType);
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID, mRightAccountId);
                        args.putString(WhooingKeyValues.ITEM_TITLE, mTitle.getText().toString());
                        args.putString(WhooingKeyValues.MONEY, mMoney.getText().toString());
                        args.putString(WhooingKeyValues.MEMO, mMemo.getText().toString());
                        loader.args = args;
                        loader.forceLoad();
                    } else {
                        EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().getLoader(LOADER_ID_SEND));
                        Bundle args = new Bundle();

                        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, mLeftAccountType);
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID, mLeftAccountId);
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, mRightAccountType);
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID, mRightAccountId);
                        args.putString(WhooingKeyValues.ITEM_TITLE, mTitle.getText().toString());
                        args.putString(WhooingKeyValues.MONEY, mMoney.getText().toString());
                        args.putString(WhooingKeyValues.MEMO, mMemo.getText().toString());
                        loader.args = args;
                        loader.forceLoad();
                    }
                }

                return true;
            }
            case R.id.action_delete: {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_confirm)
                        .setMessage(R.string.delete_entry_confirm)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EntriesLoader loader = EntriesLoader.castLoader(
                                        getLoaderManager().getLoader(LOADER_ID_DELETE));
                                Bundle args = new Bundle();

                                mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
                                args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                                args.putLong(WhooingKeyValues.ENTRY_ID, mEntryId);
                                loader.args = args;
                                loader.forceLoad();
                            }
                        }).setNegativeButton(R.string.cancel, null)
                        .create().show();

                return true;
            }
            case R.id.action_copy: {
                Intent intent = new Intent(getActivity(), HistoryDetailActivity.class);

                intent.putExtra(HistoryDetailActivity.EXTRA_SECTION_ID, mSectionId);
                intent.putExtra(HistoryDetailActivity.EXTRA_COPY, true);
                intent.putExtra(HistoryDetailActivity.EXTRA_TITLE, mTitle.getText().toString());
                intent.putExtra(HistoryDetailActivity.EXTRA_MONEY, mMoney.getText().toString());
                intent.putExtra(HistoryDetailActivity.EXTRA_LEFT_ACCOUNT_TYPE, mLeftAccountType);
                intent.putExtra(HistoryDetailActivity.EXTRA_LEFT_ACCOUNT_ID, mLeftAccountId);
                intent.putExtra(HistoryDetailActivity.EXTRA_RIGHT_ACCOUNT_TYPE, mRightAccountType);
                intent.putExtra(HistoryDetailActivity.EXTRA_RIGHT_ACCOUNT_ID, mRightAccountId);
                intent.putExtra(HistoryDetailActivity.EXTRA_MEMO, mMemo.getText().toString());
                startActivity(intent);
                getActivity().finish();

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
            case LOADER_ID_BOOKMARK: {
                return new FrequentItemsLoader(getActivity(),
                        Request.Method.POST,
                        null);
            }
            case LOADER_ID_SEND: {
                return new EntriesLoader(getActivity(),
                        Request.Method.POST,
                        null);
            }
            case LOADER_ID_EDIT_SEND: {
                return new EntriesLoader(getActivity(),
                        Request.Method.PUT,
                        null);
            }
            case LOADER_ID_DELETE: {
                return new EntriesLoader(getActivity(),
                        Request.Method.DELETE,
                        null);
            }
            default:{
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_BOOKMARK: {
                if (mProgress != null) {
                    mProgress.dismiss();
                }

                int code = (Integer) data;

                if (code < 0) {
                    final FrequentItemsLoader finalLoader = (FrequentItemsLoader) loader;

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.bookmark_failed)
                            .setMessage(R.string.bookmark_item_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FrequentItemsLoader loader = FrequentItemsLoader.castLoader(getLoaderManager()
                                            .restartLoader(LOADER_ID_BOOKMARK,
                                                    null,
                                                    HistoryDetailActivityFragment.this));

                                    loader.args = finalLoader.args;
                                    mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
                                    loader.forceLoad();
                                }
                            }).setNegativeButton(R.string.cancel_input, null)
                            .create().show();
                } else if (Utility.checkResultCodeWithAlert(getActivity(), code)) {
                    Toast.makeText(getActivity(), R.string.bookmark_selected_item_success, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            }
            case LOADER_ID_SEND: {
                if (mProgress != null) {
                    mProgress.dismiss();
                }

                int code = (Integer) data;

                if (code < 0) {
                    final EntriesLoader finalLoader = (EntriesLoader) loader;

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.input_entry_failed)
                            .setMessage(R.string.input_entry_failed_message)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

                                    EntriesLoader loader = EntriesLoader.castLoader(
                                            getLoaderManager().restartLoader(LOADER_ID_SEND,
                                                    null,
                                                    HistoryDetailActivityFragment.this));

                                    loader.args = finalLoader.args;
                                    loader.forceLoad();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else if (Utility.checkResultCodeWithAlert(getActivity(), code)) {
                    Toast.makeText(getActivity(), R.string.input_entry_success, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
                break;
            }
            case LOADER_ID_EDIT_SEND: {
                if (mProgress != null) {
                    mProgress.dismiss();
                }

                int result = (Integer) data;

                if (result < 0) {
                    final EntriesLoader editSendLoader = (EntriesLoader) loader;

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.edit_failed)
                            .setMessage(R.string.edit_entry_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

                                    EntriesLoader loader = EntriesLoader.castLoader(
                                            getLoaderManager().restartLoader(LOADER_ID_EDIT_SEND,
                                                    null,
                                                    HistoryDetailActivityFragment.this));

                                    loader.args = editSendLoader.args;
                                    loader.forceLoad();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else if (Utility.checkResultCodeWithAlert(getActivity(), result)) {
                    getActivity().finish();
                }
                break;
            }
            case LOADER_ID_DELETE: {
                if (mProgress != null) {
                    mProgress.dismiss();
                }

                int result = (Integer) data;

                if (result < 0) {
                    final EntriesLoader finalLoader = (EntriesLoader) loader;

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.delete_failed)
                            .setMessage(R.string.delete_entry_faield)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

                                    EntriesLoader loader = EntriesLoader.castLoader(
                                            getLoaderManager().restartLoader(LOADER_ID_DELETE,
                                                    null,
                                                    HistoryDetailActivityFragment.this));

                                    loader.args = finalLoader.args;
                                    loader.forceLoad();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else if (Utility.checkResultCodeWithAlert(getActivity(), result)) {
                    getActivity().finish();
                }
                break;
            }
            default:
        }
    }

    private void showSelectSlotNumberDialog() {
        if (mSlotNumberItems == null) {
            mSlotNumberItems = new String[3];

            for (int i = 0; i < 3; i++) {
                mSlotNumberItems[i] = getString(R.string.slot_name, i + 1);
            }
        }
        mSelectSlotNumberDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_slot_number)
                .setItems(mSlotNumberItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FrequentItemsLoader loader = FrequentItemsLoader.castLoader(
                                getLoaderManager().getLoader(LOADER_ID_BOOKMARK));
                        Bundle args = new Bundle();

                        args.putInt(FrequentItems.COLUMN_SLOT_NUMBER, which + 1);
                        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                        args.putString(WhooingKeyValues.ITEM_TITLE, mTitle.getText().toString());
                        args.putString(WhooingKeyValues.MONEY, mMoney.getText().toString());
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, mLeftAccountType);
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID, mLeftAccountId);
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, mRightAccountType);
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID, mRightAccountId);
                        mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
                        loader.args = args;
                        loader.forceLoad();
                    }
                }).create();
        mSelectSlotNumberDialog.show();
    }
}
