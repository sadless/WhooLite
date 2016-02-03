package com.younggeon.whoolite.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.database.DatabaseUtilsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.h6ah4i.android.compat.content.SharedPreferenceCompat;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.FrequentlyInputItemDetailActivity;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.provider.WhooingProvider;
import com.younggeon.whoolite.util.SoundSearcher;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.EntriesLoader;
import com.younggeon.whoolite.whooing.loader.FrequentItemsLoader;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by sadless on 2016. 1. 17..
 */
public class FrequentlyInputFragment extends WhooLiteActivityBaseFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int REQUEST_CODE_COMPLETE_FREQUENT_ITEM = 1;

    private static final String INSTANCE_STATE_PROGRESSING_ITEM_ID_BUNDLE = "progressing_item_id_bundle";
    private static final String INSTANCE_STATE_LAST_LOADER_ID = "last_loader_id";
    private static final String INSTANCE_STATE_PROGRESSING_LOADER_IDS = "progressing_loader_id";
    private static final String INSTANCE_STATE_MULTI_INPUT_ARGS = "multi_input_args";
    private static final String INSTANCE_STATE_QUERY_TEXT = "query_text";

    private static final int LOADER_ID_QUERY = 1;
    private static final int LOADER_ID_ENTRY_INPUT_START = 10000;

    private ArrayList<String> mCurrentProgressingItemIds;
    private ArrayList<Integer> mProgressingLoaderIds;
    private int mLastLoaderId;
    private ArrayList<Bundle> mMultiInputArgs;
    private Bundle mProgressingItemIdBundle;
    private String mQueryText;
    private String mShowSlotNumberWhere;
    private String mSearchResultWhere;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReceiveFailedStringId = R.string.failed_to_receive_frequent_item;
        mNoDataStringId = R.string.no_frequent_items;
        mDeleteConfirmStringId = R.string.delete_frequent_items_confirm;
        mActionMenuId = R.menu.action_menu_frequently_input;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (savedInstanceState != null) {
            mProgressingItemIdBundle = savedInstanceState.getBundle(INSTANCE_STATE_PROGRESSING_ITEM_ID_BUNDLE);
            mLastLoaderId = savedInstanceState.getInt(INSTANCE_STATE_LAST_LOADER_ID);
            mProgressingLoaderIds = savedInstanceState.getIntegerArrayList(INSTANCE_STATE_PROGRESSING_LOADER_IDS);
            mMultiInputArgs = savedInstanceState.getParcelableArrayList(INSTANCE_STATE_MULTI_INPUT_ARGS);
            mQueryText = savedInstanceState.getString(INSTANCE_STATE_QUERY_TEXT);
        } else {
            mProgressingItemIdBundle = new Bundle();
            mLastLoaderId = LOADER_ID_ENTRY_INPUT_START;
            mProgressingLoaderIds = new ArrayList<>();
            refreshWhere(prefs, false);
            refreshSortOrder(prefs, false);
        }

        View view = super.onCreateView(inflater, container, savedInstanceState);

        for (int id : mProgressingLoaderIds) {
            getLoaderManager().initLoader(id, null, this);
        }
        getLoaderManager().initLoader(LOADER_ID_QUERY, null, this);
        if (!TextUtils.isEmpty(mQueryText)) {
            mProgressBar.setVisibility(View.GONE);
            mEmptyText.setText(R.string.no_search_result);
            mEmptyText.setVisibility(View.VISIBLE);
        }
        prefs.registerOnSharedPreferenceChangeListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected WhooLiteAdapter createAdapter(GridLayoutManager layoutManager) {
        return new FrequentlyInputAdapter(layoutManager);
    }

    @Override
    protected Uri getMainDataUri() {
        return WhooingProvider.getFrequentItemsUri(mSectionId);
    }

    @Override
    protected void setSectionId(String sectionId) {
        super.setSectionId(sectionId);

        if (sectionId != null) {
            mCurrentProgressingItemIds = mProgressingItemIdBundle.getStringArrayList(sectionId);
            if (mCurrentProgressingItemIds == null) {
                mCurrentProgressingItemIds = new ArrayList<>();
                mProgressingItemIdBundle.putStringArrayList(sectionId, mCurrentProgressingItemIds);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBundle(INSTANCE_STATE_PROGRESSING_ITEM_ID_BUNDLE, mProgressingItemIdBundle);
        outState.putInt(INSTANCE_STATE_LAST_LOADER_ID, mLastLoaderId);
        outState.putIntegerArrayList(INSTANCE_STATE_PROGRESSING_LOADER_IDS, mProgressingLoaderIds);
        outState.putParcelableArrayList(INSTANCE_STATE_MULTI_INPUT_ARGS, mMultiInputArgs);
        outState.putString(INSTANCE_STATE_QUERY_TEXT, mQueryText);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_COMPLETE_FREQUENT_ITEM: {
                    if (mMultiInputArgs == null) {
                        inputEntry(data.getIntExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_SLOT_NUMBER, -1),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_ITEM_ID),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_ITEM_TITLE),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_MONEY),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_LEFT_ACCOUNT_TYPE),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_LEFT_ACCOUNT_ID),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_RIGHT_ACCOUNT_TYPE),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_RIGHT_ACCOUNT_ID),
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_MEMO));
                    } else {
                        Bundle args = new Bundle();

                        args.putString(WhooingKeyValues.ITEM_ID,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_ITEM_ID));
                        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_LEFT_ACCOUNT_TYPE));
                        args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_LEFT_ACCOUNT_ID));
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_RIGHT_ACCOUNT_TYPE));
                        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_RIGHT_ACCOUNT_ID));
                        args.putString(WhooingKeyValues.ITEM_TITLE,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_ITEM_TITLE));
                        args.putString(WhooingKeyValues.MONEY,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_MONEY));
                        args.putInt(EntriesLoader.ARG_SLOT_NUMBER,
                                data.getIntExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_SLOT_NUMBER, -1));
                        args.putString(WhooingKeyValues.MEMO,
                                data.getStringExtra(FrequentlyInputItemDetailActivity.RESULT_EXTRA_MEMO));
                        mMultiInputArgs.add(args);
                        popAndAddToMultiInputArgsFromSelectedItems();
                    }
                    break;
                }
                default:
            }
        } else {
            switch (requestCode) {
                case REQUEST_CODE_COMPLETE_FREQUENT_ITEM: {
                    popAndAddToMultiInputArgsFromSelectedItems();
                    break;
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_frequently_input, menu);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenu);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        if (!TextUtils.isEmpty(mQueryText)) {
            MenuItemCompat.expandActionView(searchMenu);
            searchView.setQuery(mQueryText, false);
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQueryText = newText;
                if (!TextUtils.isEmpty(newText)) {
                    QueryLoader loader = QueryLoader.castLoader(
                            getLoaderManager().restartLoader(LOADER_ID_QUERY, null, FrequentlyInputFragment.this));

                    loader.sectionId = mSectionId;
                    loader.sortOrder = mMainDataSortOrder;
                    loader.keyword = newText;
                    loader.forceLoad();
                    mEmptyText.setText(R.string.no_search_result);
                    mRetryButton.setVisibility(View.GONE);
                } else {
                    mSearchResultWhere = null;
                    mMainDataWhere = mShowSlotNumberWhere;
                    getLoaderManager().restartLoader(LOADER_ID_MAIN_DATA, null, FrequentlyInputFragment.this);
                    mEmptyText.setText(mNoDataStringId);
                    mRetryButton.setVisibility(View.VISIBLE);
                }

                return false;
            }
        });
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_DELETE_SELECTED_ITEMS: {
                return new FrequentItemsLoader(getActivity(),
                        Request.Method.DELETE,
                        null);
            }
            case LOADER_ID_REFRESH_MAIN_DATA: {
                Bundle bundle = new Bundle();

                bundle.putString(WhooingKeyValues.SECTION_ID, mSectionId);

                return new FrequentItemsLoader(getActivity(),
                        Request.Method.GET,
                        bundle);
            }
            case LOADER_ID_QUERY: {
                return new QueryLoader(getActivity());
            }
            default: {
                if (id >= LOADER_ID_ENTRY_INPUT_START) {
                    return new EntriesLoader(getActivity(), Request.Method.POST, args);
                } else {
                    return super.onCreateLoader(id, args);
                }
            }
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_QUERY: {
                if (!TextUtils.isEmpty(mQueryText)) {
                    mSearchResultWhere = FrequentItems.TABLE_NAME + "." + FrequentItems.COLUMN_TITLE + " IN " + data;
                    mMainDataWhere = DatabaseUtilsCompat.concatenateWhere(mSearchResultWhere, mShowSlotNumberWhere);
                    getLoaderManager().restartLoader(LOADER_ID_MAIN_DATA, null, this);
                }
                break;
            }
            case LOADER_ID_REFRESH_MAIN_DATA: {
                super.onLoadFinished(loader, data);
                if (!TextUtils.isEmpty(mQueryText)) {
                    mEmptyText.setText(R.string.no_search_result);
                    mEmptyText.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                    mRetryButton.setVisibility(View.GONE);
                }
                break;
            }
            default: {
                if (loader.getId() >= LOADER_ID_ENTRY_INPUT_START) {
                    int resultCode = (Integer) data;
                    final EntriesLoader finalLoader = (EntriesLoader) loader;
                    final Bundle usedArgs = ((EntriesLoader) loader).args;

                    if (resultCode < 0) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.input_entry_failed)
                                .setMessage(getString(R.string.input_entry_failed_message_with_item_title,
                                        usedArgs.getString(WhooingKeyValues.ITEM_TITLE)))
                                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ArrayList<String> progressingItemIds =
                                                mProgressingItemIdBundle.getStringArrayList(usedArgs.getString(WhooingKeyValues.SECTION_ID));

                                        if (progressingItemIds != null) {
                                            progressingItemIds.add(
                                                    usedArgs.getInt(EntriesLoader.ARG_SLOT_NUMBER) +
                                                            ":" + usedArgs.getString(WhooingKeyValues.ITEM_ID));
                                            if (progressingItemIds == mCurrentProgressingItemIds) {
                                                mRecyclerView.getAdapter().notifyDataSetChanged();
                                            }
                                        }
                                        mProgressingLoaderIds.add(finalLoader.getId());
                                        getLoaderManager().restartLoader(finalLoader.getId(),
                                                usedArgs,
                                                FrequentlyInputFragment.this).forceLoad();
                                    }
                                }).setNegativeButton(R.string.cancel_input, null)
                                .create().show();
                    } else {
                        if (Utility.checkResultCodeWithAlert(getActivity(), resultCode)) {
                            Toast.makeText(getActivity(),
                                    getString(R.string.input_entry_success_with_item_title,
                                            usedArgs.getString(WhooingKeyValues.ITEM_TITLE)),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    ArrayList<String> progressingItemIds =
                            mProgressingItemIdBundle.getStringArrayList(usedArgs.getString(WhooingKeyValues.SECTION_ID));

                    mProgressingLoaderIds.remove((Integer) loader.getId());
                    if (progressingItemIds != null) {
                        progressingItemIds.remove(usedArgs.getInt(EntriesLoader.ARG_SLOT_NUMBER) +
                                ":" + usedArgs.getString(WhooingKeyValues.ITEM_ID));
                        if (progressingItemIds == mCurrentProgressingItemIds) {
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }
                } else {
                    super.onLoadFinished(loader, data);
                }
            }
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send: {
                mMultiInputArgs = new ArrayList<>();
                popAndAddToMultiInputArgsFromSelectedItems();

                return true;
            }
            default: {
                return super.onActionItemClicked(mode, item);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_key_show_slot_numbers))) {
            refreshWhere(sharedPreferences, true);
        } else if (key.equals(getString(R.string.pref_key_frequently_input_sort_order))) {
            refreshSortOrder(sharedPreferences, true);
        }
    }

    private void inputEntry(int slotNumber, String itemId, String itemTitle, String money,
                            String leftAccountType, String leftAccountId, String rightAccountType,
                            String rightAccountId, String memo) {
        Bundle args = new Bundle();

        args.putString(WhooingKeyValues.ITEM_ID, itemId);
        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
        args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, leftAccountType);
        args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID, leftAccountId);
        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, rightAccountType);
        args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID, rightAccountId);
        args.putString(WhooingKeyValues.ITEM_TITLE, itemTitle);
        args.putString(WhooingKeyValues.MONEY, money);
        args.putInt(EntriesLoader.ARG_SLOT_NUMBER, slotNumber);
        if (!TextUtils.isEmpty(memo)) {
            args.putString(WhooingKeyValues.MEMO, memo);
        }
        inputEntry(args);
    }

    private void inputEntry(Bundle args) {
        mCurrentProgressingItemIds.add(args.getInt(EntriesLoader.ARG_SLOT_NUMBER) + ":" +
                args.getString(WhooingKeyValues.ITEM_ID));
        mRecyclerView.getAdapter().notifyDataSetChanged();
        synchronized (this) {
            mProgressingLoaderIds.add(mLastLoaderId);
            getLoaderManager().initLoader(mLastLoaderId,
                    args,
                    this).forceLoad();
            mLastLoaderId++;
        }
    }

    private void popAndAddToMultiInputArgsFromSelectedItems() {
        if (mSelectedItems != null) {
            while (mSelectedItems.size() > 0) {
                String selectionId = mSelectedItems.remove(0);
                String[] slotNumberAndId = selectionId.split(":");
                Cursor c = getActivity().getContentResolver().query(WhooingProvider.getFrequentItemUri(mSectionId,
                                Integer.parseInt(slotNumberAndId[0]),
                                slotNumberAndId[1]),
                        null,
                        null,
                        null,
                        null);

                if (c == null) {
                    continue;
                }
                c.moveToFirst();

                double money = c.getDouble(FrequentItems.COLUMN_INDEX_MONEY);
                String leftAccountType = c.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_TYPE);
                String rightAccountType = c.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE);
                int slotNumber = c.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER);
                String itemId = c.getString(FrequentItems.COLUMN_INDEX_ITEM_ID);

                if (money < WhooingKeyValues.EPSILON ||
                        TextUtils.isEmpty(leftAccountType) ||
                        TextUtils.isEmpty(rightAccountType)) {
                    Intent intent = new Intent(getActivity(),
                            FrequentlyInputItemDetailActivity.class);

                    intent.putExtra(FrequentlyInputItemDetailActivity.EXTRA_SECTION_ID,
                            mSectionId)
                            .putExtra(FrequentlyInputItemDetailActivity.EXTRA_SLOT_NUMBER,
                                    slotNumber)
                            .putExtra(FrequentlyInputItemDetailActivity.EXTRA_ITEM_ID,
                                    itemId)
                            .putExtra(FrequentlyInputItemDetailActivity.EXTRA_MODE,
                                    FrequentlyInputItemDetailActivity.MODE_COMPLETE);
                    startActivityForResult(intent, REQUEST_CODE_COMPLETE_FREQUENT_ITEM);
                    c.close();
                    return;
                } else {
                    Bundle args = new Bundle();

                    args.putString(WhooingKeyValues.ITEM_ID, itemId);
                    args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                    args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, leftAccountType);
                    args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID,
                            c.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_ID));
                    args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, rightAccountType);
                    args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID,
                            c.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_ID));
                    args.putString(WhooingKeyValues.ITEM_TITLE, c.getString(FrequentItems.COLUMN_INDEX_TITLE));
                    args.putString(WhooingKeyValues.MONEY, "" + money);
                    args.putInt(EntriesLoader.ARG_SLOT_NUMBER, slotNumber);
                    mMultiInputArgs.add(args);
                }
                c.close();
            }
            if (mSelectedItems.size() == 0) {
                mActionMode.finish();
            }
        }
    }

    private void refreshWhere(SharedPreferences prefs, boolean needRestartMainData) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }
        Set<String> showSlotNumberSet = SharedPreferenceCompat.getStringSet(prefs,
                getString(R.string.pref_key_show_slot_numbers),
                null);

        if (showSlotNumberSet != null) {
            boolean isFirst = true;

            mShowSlotNumberWhere = FrequentItems.COLUMN_SLOT_NUMBER + " IN " + "(";
            for (String slotNumber : showSlotNumberSet) {
                if (isFirst) {
                    mShowSlotNumberWhere += slotNumber;
                    isFirst = false;
                } else {
                    mShowSlotNumberWhere += "," + slotNumber;
                }
            }
            mShowSlotNumberWhere += ")";
            mMainDataWhere = DatabaseUtilsCompat.concatenateWhere(mSearchResultWhere, mShowSlotNumberWhere);
        }
        if (needRestartMainData) {
            getLoaderManager().restartLoader(LOADER_ID_MAIN_DATA, null, FrequentlyInputFragment.this);
        }
    }

    private void refreshSortOrder(SharedPreferences prefs, boolean needRestartMainData) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }
        String sortOrderFormat = prefs.getString(getString(R.string.pref_key_frequently_input_sort_order),
                getString(R.string.pref_frequently_input_sort_order_default));

        mMainDataSortOrder = String.format(FrequentItems.TABLE_NAME + "." + sortOrderFormat,
                FrequentItems.COLUMN_USE_COUNT,
                FrequentItems.COLUMN_LAST_USE_TIME,
                FrequentItems.COLUMN_SORT_ORDER);
        if (needRestartMainData) {
            getLoaderManager().restartLoader(LOADER_ID_MAIN_DATA, null, FrequentlyInputFragment.this);
        }
    }

    private class FrequentlyInputItemViewHolder extends ItemViewHolder {
        public ImageButton send;
        public ProgressBar progress;

        public FrequentlyInputItemViewHolder(View itemView) {
            super(itemView);

            send = (ImageButton) itemView.findViewById(R.id.send);
            send.setTag(this);
            progress = (ProgressBar) itemView.findViewById(R.id.progress);
        }
    }

    private class FrequentlyInputAdapter extends WhooLiteAdapter {
        public FrequentlyInputAdapter(GridLayoutManager gridLayoutManager) {
            super(gridLayoutManager);

            mColumnIndexTitle = FrequentItems.COLUMN_INDEX_TITLE;
            mColumnIndexMoney = FrequentItems.COLUMN_INDEX_MONEY;
            mColumnIndexLeftAccountType = FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_TYPE;
            mColumnIndexRightAccountType = FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE;
            mColumnIndexItemId = FrequentItems.COLUMN_INDEX_ITEM_ID;
            mItemLayoutId = R.layout.recycler_item_frequent_item;
        }

        @Override
        protected ItemViewHolder createItemViewHolder(View itemView) {
            return new FrequentlyInputItemViewHolder(itemView);
        }

        @Override
        protected void itemClicked(View view, Cursor cursor) {
            Intent intent = new Intent(getActivity(),
                    FrequentlyInputItemDetailActivity.class);

            intent.putExtra(FrequentlyInputItemDetailActivity.EXTRA_SECTION_ID, mSectionId)
                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_ITEM_ID,
                            cursor.getString(FrequentItems.COLUMN_INDEX_ITEM_ID))
                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_SLOT_NUMBER,
                            cursor.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER))
                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_MODE,
                            FrequentlyInputItemDetailActivity.MODE_EDIT);
            ActivityCompat.startActivity(getActivity(),
                    intent,
                    ActivityOptionsCompat.makeScaleUpAnimation(view,
                            0,
                            0,
                            view.getWidth(),
                            view.getHeight()
                    ).toBundle());
        }

        @Override
        protected Uri getSectionDataUri() {
            return WhooingProvider.getFrequentItemSlotCountsUri(mSectionId);
        }

        @Override
        protected String getSectionText(int sectionData) {
            return getString(R.string.slot_name, sectionData);
        }

        @Override
        protected String getSelectionId(int cursorPosition) {
            mCursor.moveToPosition(cursorPosition);

            return mCursor.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER) + ":" +
                    mCursor.getString(FrequentItems.COLUMN_INDEX_ITEM_ID);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            switch (getItemViewType(position)) {
                case VIEW_TYPE_ITEM: {
                    FrequentlyInputItemViewHolder viewHolder = (FrequentlyInputItemViewHolder) holder;

                    viewHolder.send.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FrequentlyInputItemViewHolder vh = (FrequentlyInputItemViewHolder) v.getTag();
                            int position = vh.getAdapterPosition();

                            mCursor.moveToPosition(getCursorPosition(position));

                            double money = mCursor.getDouble(FrequentItems.COLUMN_INDEX_MONEY);
                            String leftAccountType = mCursor.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_TYPE);
                            String rightAccountType = mCursor.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE);
                            int slotNumber = mCursor.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER);
                            String itemId = mCursor.getString(FrequentItems.COLUMN_INDEX_ITEM_ID);

                            if (money < WhooingKeyValues.EPSILON ||
                                    TextUtils.isEmpty(leftAccountType) ||
                                    TextUtils.isEmpty(rightAccountType)) {
                                Intent intent = new Intent(getActivity(),
                                        FrequentlyInputItemDetailActivity.class);

                                intent.putExtra(FrequentlyInputItemDetailActivity.EXTRA_SECTION_ID,
                                        mSectionId)
                                        .putExtra(FrequentlyInputItemDetailActivity.EXTRA_SLOT_NUMBER,
                                                slotNumber)
                                        .putExtra(FrequentlyInputItemDetailActivity.EXTRA_ITEM_ID,
                                                itemId)
                                        .putExtra(FrequentlyInputItemDetailActivity.EXTRA_MODE,
                                                FrequentlyInputItemDetailActivity.MODE_COMPLETE);
                                startActivityForResult(intent, REQUEST_CODE_COMPLETE_FREQUENT_ITEM);
                            } else {
                                inputEntry(slotNumber,
                                        itemId,
                                        mCursor.getString(FrequentItems.COLUMN_INDEX_TITLE),
                                        "" + money,
                                        leftAccountType,
                                        mCursor.getString(FrequentItems.COLUMN_INDEX_LEFT_ACCOUNT_ID),
                                        rightAccountType,
                                        mCursor.getString(FrequentItems.COLUMN_INDEX_RIGHT_ACCOUNT_ID),
                                        null);
                            }
                        }
                    });
                    viewHolder.send.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Intent intent = new Intent(getActivity(),
                                    FrequentlyInputItemDetailActivity.class);

                            intent.putExtra(FrequentlyInputItemDetailActivity.EXTRA_SECTION_ID,
                                    mSectionId)
                                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_SLOT_NUMBER,
                                            mCursor.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER))
                                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_ITEM_ID,
                                            mCursor.getString(FrequentItems.COLUMN_INDEX_ITEM_ID))
                                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_MODE,
                                            FrequentlyInputItemDetailActivity.MODE_COMPLETE);
                            startActivityForResult(intent, REQUEST_CODE_COMPLETE_FREQUENT_ITEM);

                            return true;
                        }
                    });
                    if (mActionMode == null) {
                        if (mCurrentProgressingItemIds.contains(
                                mCursor.getInt(FrequentItems.COLUMN_INDEX_SLOT_NUMBER) + ":" +
                                        mCursor.getString(FrequentItems.COLUMN_INDEX_ITEM_ID))) {
                            viewHolder.send.setVisibility(View.GONE);
                            viewHolder.progress.setVisibility(View.VISIBLE);
                        } else {
                            viewHolder.send.setVisibility(View.VISIBLE);
                            viewHolder.progress.setVisibility(View.GONE);
                        }
                    } else {
                        viewHolder.send.setVisibility(View.GONE);
                        viewHolder.progress.setVisibility(View.GONE);
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    private static class QueryLoader extends AsyncTaskLoader<String> {
        public String sectionId;
        public String sortOrder;
        public String keyword;

        public QueryLoader(Context context) {
            super(context);
        }

        @Override
        public String loadInBackground() {
            Cursor c = getContext().getContentResolver().query(WhooingProvider.getFrequentItemsUri(sectionId),
                    null,
                    null,
                    null,
                    sortOrder);
            String retVal = "(";
            boolean isFirst = true;

            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String originalTitle = c.getString(FrequentItems.COLUMN_INDEX_TITLE);
                        String trimmedTitle = originalTitle.replaceAll("\\s", "");

                        if (SoundSearcher.matchString(trimmedTitle, keyword)) {
                            if (isFirst) {
                                retVal += "'" + originalTitle + "'";
                                isFirst = false;
                            } else {
                                retVal += ",'" + originalTitle + "'";
                            }
                        }
                    } while (c.moveToNext());
                }
                c.close();
            }
            retVal += ")";

            return retVal;
        }

        public static QueryLoader castLoader(Loader loader) {
            return (QueryLoader) loader;
        }
    }
}
