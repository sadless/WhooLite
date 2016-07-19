package com.younggeon.whoolite.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.FrequentlyInputItemDetailActivity;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.realm.FrequentItem;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.EntriesLoader;
import com.younggeon.whoolite.whooing.loader.FrequentItemsLoader;

import java.text.DecimalFormat;

import io.realm.Realm;

/**
 * Created by sadless on 2016. 1. 18..
 */
public class FrequentlyInputItemDetailActivityFragment extends DetailActivityBaseFragment {
    private static final int LOADER_ID_SAVE = 1;

    private int mMode;
    private int mOldSlotNumber;

    private Spinner mSlotNumber;

    @Override
    protected void initialize() {
        Realm realm = Realm.getDefaultInstance();
        FrequentItem frequentItem = realm.where(FrequentItem.class)
                .equalTo("sectionId", mSectionId)
                .equalTo("slotNumber", mOldSlotNumber)
                .equalTo("itemId", mItemId).findFirst();

        if (frequentItem != null) {
            double money = frequentItem.getMoney();

            mTitle.setText(frequentItem.getTitle());
            mLeftAccountType = frequentItem.getLeftAccountType();
            mLeftAccountId = frequentItem.getLeftAccountId();
            mRightAccountType = frequentItem.getRightAccountType();
            mRightAccountId = frequentItem.getRightAccountId();
            if (money >= WhooingKeyValues.EPSILON) {
                mMoney.setText(new DecimalFormat().format(money).replace(",", ""));
            } else {
                switch (mMode) {
                    case FrequentlyInputItemDetailActivity.MODE_COMPLETE: {
                        if (!TextUtils.isEmpty(mLeftAccountType) || !TextUtils.isEmpty(mRightAccountType)) {
                            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                            mMoney.requestFocus();
                        }
                        break;
                    }
                    default:
                }
            }
            mSearchKeyword.setText(frequentItem.getSearchKeyword());
        }
        realm.close();
    }

    @Override
    protected void onAccountSelected(DetailActivityBaseFragment.AccountsAdapter adapter, int direction, int position) {
        super.onAccountSelected(adapter, direction, position);

        switch (mMode) {
            case FrequentlyInputItemDetailActivity.MODE_COMPLETE: {
                switch (direction) {
                    case AccountsAdapter.DIRECTION_LEFT: {
                        if (TextUtils.isEmpty(mRightAccountType)) {
                            mRight.performClick();
                        }
                        break;
                    }
                    case AccountsAdapter.DIRECTION_RIGHT: {
                        if (TextUtils.isEmpty(mLeftAccountType)) {
                            mLeft.performClick();
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
            }
            default:
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayoutId = R.layout.fragment_frequently_input_item_detail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();

        mItemId = intent.getStringExtra(FrequentlyInputItemDetailActivity.EXTRA_ITEM_ID);
        mOldSlotNumber = intent.getIntExtra(FrequentlyInputItemDetailActivity.EXTRA_SLOT_NUMBER, -1);
        mMode = intent.getIntExtra(FrequentlyInputItemDetailActivity.EXTRA_MODE,
                FrequentlyInputItemDetailActivity.MODE_EDIT);

        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view != null) {
            mSlotNumber = (Spinner) view.findViewById(R.id.slot_number);
        }
        switch (mMode) {
            case FrequentlyInputItemDetailActivity.MODE_EDIT: {
                String[] slotNumberItems = new String[3];

                for (int i = 0; i < slotNumberItems.length; i++) {
                    slotNumberItems[i] = getString(R.string.slot_name, i + 1);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_list_item_1,
                        slotNumberItems);

                adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
                mSlotNumber.setAdapter(adapter);
                mSlotNumber.setSelection(mOldSlotNumber - 1);
                mMemo.setVisibility(View.GONE);

                break;
            }
            case FrequentlyInputItemDetailActivity.MODE_COMPLETE: {
                mSlotNumber.setVisibility(View.GONE);
                mMemo.setVisibility(View.VISIBLE);
                mSearchKeyword.setVisibility(View.GONE);
                break;
            }
            default:
        }
        getLoaderManager().initLoader(LOADER_ID_SAVE, null, this);
        getLoaderManager().initLoader(LOADER_ID_SEND, null, this);
        getLoaderManager().initLoader(LOADER_ID_DELETE, null, this);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_frequently_input_item_detail, menu);
        switch (mMode) {
            case FrequentlyInputItemDetailActivity.MODE_COMPLETE: {
                menu.removeItem(R.id.action_save);
                menu.removeItem(R.id.action_delete);
                break;
            }
            default:
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save: {
                if (TextUtils.isEmpty(mTitle.getText())) {
                    mTitle.setError(getString(R.string.input_item_title));
                } else {
                    FrequentItemsLoader loader = FrequentItemsLoader.castLoader(
                            getLoaderManager().getLoader(LOADER_ID_SAVE));
                    Bundle args = new Bundle();

                    args.putInt(FrequentItemsLoader.ARG_OLD_SLOT, mOldSlotNumber);
                    args.putInt(FrequentItemsLoader.ARG_NEW_SLOT, mSlotNumber.getSelectedItemPosition() + 1);
                    args.putString(FrequentItemsLoader.ARG_SEARCH_KEYWORD, mSearchKeyword.getText().toString());
                    args.putString(WhooingKeyValues.ITEM_ID, mItemId);
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

                return true;
            }
            case R.id.action_send: {
                if (TextUtils.isEmpty(mLeftAccountId)) {
                    mLeft.performClick();
                } else if (TextUtils.isEmpty(mRightAccountId)) {
                    mRight.performClick();
                } else {
                    switch (mMode) {
                        case FrequentlyInputItemDetailActivity.MODE_EDIT: {
                            EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().getLoader(LOADER_ID_SEND));
                            Bundle args = new Bundle();

                            args.putInt(EntriesLoader.ARG_SLOT_NUMBER, mOldSlotNumber);
                            args.putString(WhooingKeyValues.ITEM_ID, mItemId);
                            args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                            args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, mLeftAccountType);
                            args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID, mLeftAccountId);
                            args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, mRightAccountType);
                            args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID, mRightAccountId);
                            args.putString(WhooingKeyValues.ITEM_TITLE, mTitle.getText().toString());
                            args.putString(WhooingKeyValues.MONEY, mMoney.getText().toString());
                            mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
                            loader.args = args;
                            loader.forceLoad();
                            break;
                        }
                        case FrequentlyInputItemDetailActivity.MODE_COMPLETE: {
                            Intent intent = new Intent();

                            intent.putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_SLOT_NUMBER, mOldSlotNumber)
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_ITEM_ID, mItemId)
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_ITEM_TITLE,
                                            mTitle.getText().toString())
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_MONEY,
                                            mMoney.getText().toString())
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_LEFT_ACCOUNT_TYPE,
                                            mLeftAccountType)
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_LEFT_ACCOUNT_ID,
                                            mLeftAccountId)
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_RIGHT_ACCOUNT_TYPE,
                                            mRightAccountType)
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_RIGHT_ACCOUNT_ID,
                                            mRightAccountId)
                                    .putExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_MEMO,
                                            mMemo.getText().toString());
                            getActivity().setResult(Activity.RESULT_OK, intent);
                            getActivity().finish();
                            break;
                        }
                        default:
                    }
                }

                return true;
            }
            case R.id.action_delete: {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_confirm)
                        .setMessage(R.string.delete_frequent_item_confirm)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FrequentItemsLoader loader = FrequentItemsLoader.castLoader(
                                        getLoaderManager().getLoader(LOADER_ID_DELETE));
                                Bundle args = new Bundle();

                                args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                                args.putInt(FrequentItems.COLUMN_SLOT_NUMBER, mOldSlotNumber);
                                args.putString(WhooingKeyValues.ITEM_ID, mItemId);
                                mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
                                loader.args = args;
                                loader.forceLoad();
                            }
                        }).setNegativeButton(R.string.cancel, null)
                        .create().show();

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
            case LOADER_ID_SAVE: {
                return new FrequentItemsLoader(getActivity(),
                        Request.Method.PUT,
                        null);
            }
            case LOADER_ID_SEND: {
                return new EntriesLoader(getActivity(),
                        Request.Method.POST,
                        null);
            }
            case LOADER_ID_DELETE: {
                return new FrequentItemsLoader(getActivity(),
                        Request.Method.DELETE,
                        null);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_SAVE: {
                if (mProgress != null) {
                    mProgress.dismiss();
                }

                int resultCode = (Integer) data;
                final FrequentItemsLoader finalLoader = (FrequentItemsLoader) loader;

                if (resultCode < 0) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.save_failed)
                            .setMessage(R.string.save_frequent_item_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

                                    FrequentItemsLoader loader = FrequentItemsLoader.castLoader(
                                            getLoaderManager().restartLoader(LOADER_ID_SAVE,
                                                    null,
                                                    FrequentlyInputItemDetailActivityFragment.this));

                                    loader.args = finalLoader.args;
                                    loader.forceLoad();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else {
                    if (Utility.checkResultCodeWithAlert(getActivity(), resultCode)) {
                        getActivity().finish();
                    }
                }
                break;
            }
            case LOADER_ID_SEND: {
                if (mProgress != null) {
                    mProgress.dismiss();
                }

                int resultCode = (Integer) data;
                final EntriesLoader finalLoader = (EntriesLoader) loader;

                if (resultCode < 0) {
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
                                                    FrequentlyInputItemDetailActivityFragment.this));

                                    loader.args = finalLoader.args;
                                    loader.forceLoad();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else {
                    if (Utility.checkResultCodeWithAlert(getActivity(), resultCode)) {
                        Toast.makeText(getActivity(), R.string.input_entry_success, Toast.LENGTH_LONG).show();
                        getActivity().finish();
                    }
                }
                break;
            }
            case LOADER_ID_DELETE: {
                if (mProgress != null) {
                    mProgress.dismiss();
                }

                int resultCode = (Integer) data;
                final FrequentItemsLoader finalLoader = (FrequentItemsLoader) loader;

                if (resultCode < 0) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.delete_failed)
                            .setMessage(R.string.delete_frequent_item_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

                                    FrequentItemsLoader loader = FrequentItemsLoader.castLoader(
                                            getLoaderManager().restartLoader(LOADER_ID_DELETE,
                                                    null,
                                                    FrequentlyInputItemDetailActivityFragment.this));

                                    loader.args = finalLoader.args;
                                    loader.forceLoad();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else {
                    if (Utility.checkResultCodeWithAlert(getActivity(), resultCode)) {
                        getActivity().finish();
                    }
                }
                break;
            }
            default:
        }
    }

    @Override
    protected void leftChanged() {
        AccountsAdapter adapter = (AccountsAdapter) mLeft.getAdapter();

        if (adapter == null) {
            switch (mMode) {
                case FrequentlyInputItemDetailActivity.MODE_COMPLETE: {
                    if (mRight.getAdapter() != null && !TextUtils.isEmpty(mMoney.getText())) {
                        openNotSelectedSpinner();
                    }
                    break;
                }
                default:
            }
        }
        super.leftChanged();
    }

    @Override
    protected void rightChanged() {
        AccountsAdapter adapter = (AccountsAdapter) mRight.getAdapter();

        if (adapter == null) {
            switch (mMode) {
                case FrequentlyInputItemDetailActivity.MODE_COMPLETE: {
                    if (mLeft.getAdapter() != null && !TextUtils.isEmpty(mMoney.getText())) {
                        openNotSelectedSpinner();
                    }
                    break;
                }
                default:
            }
        }
        super.rightChanged();
    }

    private void openNotSelectedSpinner() {
        Spinner spinner = null;

        if (TextUtils.isEmpty(mLeftAccountType)) {
            spinner = mLeft;
        } else if (TextUtils.isEmpty(mRightAccountType)) {
            spinner = mRight;
        }
        if (spinner != null) {
            final Spinner finalSpinner = spinner;

            spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= 16) {
                        finalSpinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        finalSpinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    finalSpinner.performClick();
                }
            });
        }
    }
}
