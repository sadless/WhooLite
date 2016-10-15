package com.younggeon.whoolite.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
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
import com.younggeon.whoolite.activity.SelectMergingEntryActivity;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.realm.Entry;
import com.younggeon.whoolite.realm.FrequentItem;
import com.younggeon.whoolite.util.SoundSearcher;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.EntriesLoader;
import com.younggeon.whoolite.whooing.loader.FrequentItemsLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by sadless on 2016. 1. 17..
 */
public class FrequentlyInputFragment extends WhooLiteActivityBaseFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int REQUEST_CODE_COMPLETE_FREQUENT_ITEM = 1;
    private static final int REQUEST_CODE_MERGE_FOR_SEND = 2;

    private static final String INSTANCE_STATE_PROGRESSING_ITEM_ID_BUNDLE = "progressing_item_id_bundle";
    private static final String INSTANCE_STATE_LAST_LOADER_ID = "last_loader_id";
    private static final String INSTANCE_STATE_PROGRESSING_LOADER_IDS = "progressing_loader_id";
    private static final String INSTANCE_STATE_MULTI_INPUT_ARGS = "multi_input_args";
    private static final String INSTANCE_STATE_QUERY_TEXT = "query_text";
    private static final String INSTANCE_STATE_SEARCHED_TITLES = "searched_titles";
    private static final String INSTANCE_STATE_SEND_MERGE_DIALOG = "send_merge_dialog";
    private static final String INSTANCE_STATE_ADDING_MONEY = "adding_money";
    private static final String INSTANCE_STATE_MERGING_ITEM_SPECIFIED = "merging_item_specified";
    private static final String INSTANCE_STATE_SEND_ARGUMENTS = "send_arguments";

    private static final int LOADER_ID_QUERY = 1;
    private static final int LOADER_ID_ENTRY_INPUT_START = 10000;

    private ArrayList<String> mCurrentProgressingItemIds;
    private ArrayList<Integer> mProgressingLoaderIds;
    private int mLastLoaderId;
    private ArrayList<Bundle> mMultiInputArgs;
    private Bundle mProgressingItemIdBundle;
    private ArrayList<String> mSearchedTitles;
    private RealmQuery<FrequentItem> mQuery;
    private RealmResults<FrequentItem> mFrequentItems;
    private String[] mSortOrderFields;
    private Sort[] mSortOrderValues;
    private double mAddingMoney;
    private boolean mMergingItemSpecified;
    private Bundle mSendArguments;
    private AlertDialog mSendMergeAlertDialog;
    private SimpleDateFormat mEntryDateFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeleteConfirmStringId = R.string.delete_frequent_items_confirm;
        mActionMenuId = R.menu.action_menu_frequently_input;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String queryText = null;

        if (savedInstanceState != null) {
            mProgressingItemIdBundle = savedInstanceState.getBundle(INSTANCE_STATE_PROGRESSING_ITEM_ID_BUNDLE);
            mLastLoaderId = savedInstanceState.getInt(INSTANCE_STATE_LAST_LOADER_ID);
            mProgressingLoaderIds = savedInstanceState.getIntegerArrayList(INSTANCE_STATE_PROGRESSING_LOADER_IDS);
            mMultiInputArgs = savedInstanceState.getParcelableArrayList(INSTANCE_STATE_MULTI_INPUT_ARGS);
            queryText = savedInstanceState.getString(INSTANCE_STATE_QUERY_TEXT);
            mSearchedTitles = savedInstanceState.getStringArrayList(INSTANCE_STATE_SEARCHED_TITLES);
            mAddingMoney = savedInstanceState.getDouble(INSTANCE_STATE_ADDING_MONEY);
            mMergingItemSpecified = savedInstanceState.getBoolean(INSTANCE_STATE_MERGING_ITEM_SPECIFIED);
            mSendArguments = savedInstanceState.getBundle(INSTANCE_STATE_SEND_ARGUMENTS);
            if (savedInstanceState.getBoolean(INSTANCE_STATE_SEND_MERGE_DIALOG)) {
                showSendMergeAlertDialog();
            }
        } else {
            mProgressingItemIdBundle = new Bundle();
            mLastLoaderId = LOADER_ID_ENTRY_INPUT_START;
            mProgressingLoaderIds = new ArrayList<>();
        }

        View view = super.onCreateView(inflater, container, savedInstanceState);

        mEntryDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        mQueryText.set(queryText);
        mBinding.setReceiveFailedText(getString(R.string.failed_to_receive_frequent_item));
        mBinding.setNoDataText(getString(R.string.no_frequent_items));
        for (int id : mProgressingLoaderIds) {
            getLoaderManager().initLoader(id, null, this);
        }
        getLoaderManager().initLoader(LOADER_ID_QUERY, null, this);
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        setSortOrder();
        sectionChanged();

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
        outState.putString(INSTANCE_STATE_QUERY_TEXT, mQueryText.get());
        outState.putStringArrayList(INSTANCE_STATE_SEARCHED_TITLES, mSearchedTitles);
        outState.putDouble(INSTANCE_STATE_ADDING_MONEY, mAddingMoney);
        outState.putBoolean(INSTANCE_STATE_MERGING_ITEM_SPECIFIED, mMergingItemSpecified);
        outState.putBundle(INSTANCE_STATE_SEND_ARGUMENTS, mSendArguments);
        if (mSendMergeAlertDialog != null) {
            outState.putBoolean(INSTANCE_STATE_SEND_MERGE_DIALOG, mSendMergeAlertDialog.isShowing());
        }
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
                case REQUEST_CODE_MERGE_FOR_SEND: {
                    popAndAddToMultiInputArgsFromSelectedItems();
                    break;
                }
                default:
            }
        } else {
            switch (requestCode) {
                case REQUEST_CODE_COMPLETE_FREQUENT_ITEM:case REQUEST_CODE_MERGE_FOR_SEND: {
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
        if (!TextUtils.isEmpty(mQueryText.get())) {
            MenuItemCompat.expandActionView(searchMenu);
            searchView.setQuery(mQueryText.get(), false);
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQueryText.set(newText);
                if (!TextUtils.isEmpty(newText)) {
                    QueryLoader loader = QueryLoader.castLoader(
                            getLoaderManager().restartLoader(LOADER_ID_QUERY, null, FrequentlyInputFragment.this));

                    loader.sectionId = mSectionId;
                    loader.keyword = newText;
                    loader.forceLoad();
                } else {
                    mSearchedTitles = null;
                    sectionChanged();
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
                }

                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_QUERY: {
                if (!TextUtils.isEmpty(mQueryText.get())) {
                    mSearchedTitles = (ArrayList<String>) data;
                    sectionChanged();
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
                                                mBinding.recyclerView.getAdapter().notifyDataSetChanged();
                                            }
                                        }
                                        mProgressingLoaderIds.add(finalLoader.getId());

                                        EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().restartLoader(finalLoader.getId(),
                                                usedArgs,
                                                FrequentlyInputFragment.this));

                                        loader.method = finalLoader.method;
                                        loader.forceLoad();
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
                            mBinding.recyclerView.getAdapter().notifyDataSetChanged();
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
        if (key.equals(getString(R.string.pref_key_frequently_input_sort_order))) {
            setSortOrder();
        }
        sectionChanged();
    }

    @Override
    protected int getDataCount() {
        return mFrequentItems.size();
    }

    @Override
    protected void sectionChanged() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Set<String> showSlotNumberSet = SharedPreferenceCompat.getStringSet(prefs,
                getString(R.string.pref_key_show_slot_numbers), null);

        mQuery = mRealm.where(FrequentItem.class).equalTo("sectionId", mSectionId);
        if (showSlotNumberSet != null && showSlotNumberSet.size() < 3) {
            for (String slotNumber : showSlotNumberSet) {
                mQuery = mQuery.equalTo("slotNumber", Integer.parseInt(slotNumber));
            }
        }
        if (mSearchedTitles != null) {
            if (mSearchedTitles.size() == 0) {
                mQuery = mQuery.equalTo("title", "");
            } else {
                boolean first = true;

                for (String title : mSearchedTitles) {
                    if (!first) {
                        mQuery = mQuery.or();
                    } else {
                        first = false;
                    }
                    mQuery = mQuery.equalTo("title", title);
                }
            }
        }
        if (mFrequentItems != null) {
            mFrequentItems.removeChangeListeners();
        }
        mFrequentItems = mQuery.findAllSorted(mSortOrderFields, mSortOrderValues);
        mFrequentItems.addChangeListener(new RealmChangeListener<RealmResults<FrequentItem>>() {
            @Override
            public void onChange(RealmResults<FrequentItem> element) {
                mainDataChanged();
            }
        });
        mainDataChanged();
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
        args.putString(WhooingKeyValues.ENTRY_DATE, mEntryDateFormat.format(new Date()));

        RealmResults<Entry> prevEntries = Utility.getDuplicateEntries(mRealm, args);

        if (prevEntries.size() > 0) {
            if (mMergingItemSpecified = prevEntries.size() == 1) {
                Entry entry = prevEntries.first();

                mAddingMoney = entry.getMoney();
                args.putLong(WhooingKeyValues.ENTRY_ID, entry.getEntryId());
            }
            mSendArguments = args;
            showSendMergeAlertDialog();
        } else {
            mCurrentProgressingItemIds.add(args.getInt(EntriesLoader.ARG_SLOT_NUMBER) + ":" +
                    args.getString(WhooingKeyValues.ITEM_ID));
            mBinding.recyclerView.getAdapter().notifyDataSetChanged();
            synchronized (this) {
                mProgressingLoaderIds.add(mLastLoaderId);
                getLoaderManager().initLoader(mLastLoaderId,
                        args,
                        this).forceLoad();
                mLastLoaderId++;
            }
        }
    }

    private void popAndAddToMultiInputArgsFromSelectedItems() {
        if (mSelectedItems != null) {
            while (mSelectedItems.size() > 0) {
                Realm realm = Realm.getDefaultInstance();
                String selectionId = mSelectedItems.remove(0);
                String[] slotNumberAndId = selectionId.split(":");
                int slotNumber = Integer.parseInt(slotNumberAndId[0]);
                String itemId = slotNumberAndId[1];
                FrequentItem frequentItem = realm.where(FrequentItem.class)
                        .equalTo("sectionId", mSectionId)
                        .equalTo("slotNumber", slotNumber)
                        .equalTo("itemId", itemId).findFirst();

                realm.close();
                if (frequentItem == null) {
                    continue;
                }

                double money = frequentItem.getMoney();
                String leftAccountType = frequentItem.getLeftAccountType();
                String rightAccountType = frequentItem.getRightAccountType();

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

                    return;
                } else {
                    Bundle args = new Bundle();

                    args.putString(WhooingKeyValues.ITEM_ID, itemId);
                    args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
                    args.putString(WhooingKeyValues.LEFT_ACCOUNT_TYPE, leftAccountType);
                    args.putString(WhooingKeyValues.LEFT_ACCOUNT_ID, frequentItem.getLeftAccountId());
                    args.putString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE, rightAccountType);
                    args.putString(WhooingKeyValues.RIGHT_ACCOUNT_ID, frequentItem.getRightAccountId());
                    args.putString(WhooingKeyValues.ITEM_TITLE, frequentItem.getTitle());
                    args.putString(WhooingKeyValues.MONEY, "" + money);
                    args.putInt(EntriesLoader.ARG_SLOT_NUMBER, slotNumber);
                    mMultiInputArgs.add(args);
                }
            }
            if (mSelectedItems.size() == 0) {
                for (Bundle arg : mMultiInputArgs) {
                    inputEntry(arg);
                }
                mActionMode.finish();
            }
        }
    }

    private void setSortOrder() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrderPref = prefs.getString(getString(R.string.pref_key_frequently_input_sort_order),
                getString(R.string.pref_frequently_input_sort_order_default));
        String[] prefComponents = sortOrderPref.split(" ");

        mSortOrderFields = new String[2];
        mSortOrderValues = new Sort[2];
        mSortOrderFields[0] = "slotNumber";
        mSortOrderValues[0] = Sort.ASCENDING;
        switch (prefComponents[0].charAt(1)) {
            case '1': {
                mSortOrderFields[1] = "useCount";
                break;
            }
            case '2': {
                mSortOrderFields[1] = "lastUseTime";
                break;
            }
            case '3': {
                mSortOrderFields[1] = "sortOrder";
                break;
            }
        }
        switch (prefComponents[1].charAt(0)) {
            case 'A': {
                mSortOrderValues[1] = Sort.ASCENDING;
                break;
            }
            case 'D': {
                mSortOrderValues[1] = Sort.DESCENDING;
                break;
            }
        }
    }

    private void showSendMergeAlertDialog() {
        mSendMergeAlertDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.merge)
                .setMessage(R.string.merge_confirm)
                .setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mMergingItemSpecified) {
                            double money = Double.parseDouble(mSendArguments.getString(WhooingKeyValues.MONEY));

                            mSendArguments.putString(WhooingKeyValues.MONEY, "" + (money + mAddingMoney));
                            mCurrentProgressingItemIds.add(mSendArguments.getInt(EntriesLoader.ARG_SLOT_NUMBER) + ":" +
                                    mSendArguments.getString(WhooingKeyValues.ITEM_ID));
                            mBinding.recyclerView.getAdapter().notifyDataSetChanged();
                            synchronized (this) {
                                mProgressingLoaderIds.add(mLastLoaderId);

                                EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().initLoader(mLastLoaderId,
                                        mSendArguments,
                                        FrequentlyInputFragment.this));

                                loader.method = Request.Method.PUT;
                                loader.forceLoad();
                                mLastLoaderId++;
                            }
                        } else {
                            Intent intent = new Intent(getContext(), SelectMergingEntryActivity.class);

                            intent.putExtra(SelectMergingEntryActivity.EXTRA_MERGE_ARGUMENTS, mSendArguments);
                            startActivityForResult(intent, REQUEST_CODE_MERGE_FOR_SEND);
                        }
                    }
                }).setNegativeButton(R.string.new_entry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCurrentProgressingItemIds.add(mSendArguments.getInt(EntriesLoader.ARG_SLOT_NUMBER) + ":" +
                                mSendArguments.getString(WhooingKeyValues.ITEM_ID));
                        mSendArguments.remove(WhooingKeyValues.ENTRY_ID);
                        mBinding.recyclerView.getAdapter().notifyDataSetChanged();
                        synchronized (this) {
                            mProgressingLoaderIds.add(mLastLoaderId);
                            getLoaderManager().initLoader(mLastLoaderId,
                                    mSendArguments,
                                    FrequentlyInputFragment.this).forceLoad();
                            mLastLoaderId++;
                        }
                    }
                }).create();
        mSendMergeAlertDialog.show();
    }

    private class FrequentlyInputItemViewHolder extends ItemViewHolder {
        ImageButton send;
        ProgressBar progress;

        FrequentlyInputItemViewHolder(View itemView) {
            super(itemView);

            send = (ImageButton) itemView.findViewById(R.id.send);
            send.setTag(this);
            progress = (ProgressBar) itemView.findViewById(R.id.progress);
        }
    }

    private class FrequentlyInputAdapter extends WhooLiteAdapter {
        FrequentlyInputAdapter(GridLayoutManager gridLayoutManager) {
            super(gridLayoutManager);

            mItemLayoutId = R.layout.recycler_item_frequent_item;
        }

        @Override
        protected ItemViewHolder createItemViewHolder(View itemView) {
            return new FrequentlyInputItemViewHolder(itemView);
        }

        @Override
        protected void itemClicked(View view, int dataPosition) {
            Intent intent = new Intent(getActivity(),
                    FrequentlyInputItemDetailActivity.class);
            FrequentItem item = mFrequentItems.get(dataPosition);

            intent.putExtra(FrequentlyInputItemDetailActivity.EXTRA_SECTION_ID, mSectionId)
                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_ITEM_ID,
                            item.getItemId())
                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_SLOT_NUMBER,
                            item.getSlotNumber())
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
        protected String getSelectionId(int dataPosition) {
            FrequentItem item = mFrequentItems.get(dataPosition);

            return item.getSlotNumber() + ":" + item.getItemId();
        }

        @Override
        protected String getTitle(int dataPosition) {
            return mFrequentItems.get(dataPosition).getTitle();
        }

        @Override
        protected double getMoney(int dataPosition) {
            return mFrequentItems.get(dataPosition).getMoney();
        }

        @Override
        protected String getLeftAccountType(int dataPosition) {
            return mFrequentItems.get(dataPosition).getLeftAccountType();
        }

        @Override
        protected String getLeftAccountId(int dataPosition) {
            return mFrequentItems.get(dataPosition).getLeftAccountId();
        }

        @Override
        protected String getRightAccountType(int dataPosition) {
            return mFrequentItems.get(dataPosition).getRightAccountType();
        }

        @Override
        protected String getRightAccountId(int dataPosition) {
            return mFrequentItems.get(dataPosition).getRightAccountId();
        }

        @Override
        protected long getValueForItemId(int dataPosition) {
            return mFrequentItems.get(dataPosition).getItemId().hashCode();
        }

        @Override
        protected void refreshSections() {
            RealmResults<FrequentItem> items = mQuery.findAllSorted("sortOrder", Sort.ASCENDING);
            RealmResults<FrequentItem> itemSlots = items.distinct("slotNumber");

            if (itemSlots.size() > 1) {
                mSectionTitles = new String[itemSlots.size()];
                mSectionDataCounts = new int[itemSlots.size()];

                int i = 0;

                for (FrequentItem item : items) {
                    mSectionTitles[i] = getString(R.string.slot_name, item.getSlotNumber());
                    mSectionDataCounts[i] = mQuery.findAll().where().equalTo("slotNumber", item.getSlotNumber()).findAll().size();
                    i++;
                }
            } else {
                mSectionTitles = null;
                mSectionDataCounts = null;
            }
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
                            FrequentItem item = mFrequentItems.get(getDataPosition(position));

                            double money = item.getMoney();
                            String leftAccountType = item.getLeftAccountType();
                            String rightAccountType = item.getRightAccountType();
                            int slotNumber = item.getSlotNumber();
                            String itemId = item.getItemId();

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
                                        item.getTitle(),
                                        "" + money,
                                        leftAccountType,
                                        item.getLeftAccountId(),
                                        rightAccountType,
                                        item.getRightAccountId(),
                                        null);
                            }
                        }
                    });
                    viewHolder.send.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            FrequentlyInputItemViewHolder vh = (FrequentlyInputItemViewHolder) v.getTag();
                            int position = vh.getAdapterPosition();
                            FrequentItem item = mFrequentItems.get(getDataPosition(position));
                            Intent intent = new Intent(getActivity(),
                                    FrequentlyInputItemDetailActivity.class);

                            intent.putExtra(FrequentlyInputItemDetailActivity.EXTRA_SECTION_ID,
                                    mSectionId)
                                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_SLOT_NUMBER,
                                            item.getSlotNumber())
                                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_ITEM_ID,
                                            item.getItemId())
                                    .putExtra(FrequentlyInputItemDetailActivity.EXTRA_MODE,
                                            FrequentlyInputItemDetailActivity.MODE_COMPLETE);
                            startActivityForResult(intent, REQUEST_CODE_COMPLETE_FREQUENT_ITEM);

                            return true;
                        }
                    });
                    if (mActionMode == null) {
                        FrequentItem item = mFrequentItems.get(getDataPosition(position));

                        if (mCurrentProgressingItemIds.contains(
                                item.getSlotNumber() + ":" + item.getItemId())) {
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

    private static class QueryLoader extends AsyncTaskLoader<ArrayList<String>> {
        public String sectionId;
        String keyword;

        QueryLoader(Context context) {
            super(context);
        }

        @Override
        public ArrayList<String> loadInBackground() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            Set<String> showSlotNumberSet = SharedPreferenceCompat.getStringSet(prefs,
                    getContext().getString(R.string.pref_key_show_slot_numbers), null);
            Realm realm = Realm.getDefaultInstance();
            RealmQuery<FrequentItem> query = realm.where(FrequentItem.class).equalTo("sectionId", sectionId);

            if (showSlotNumberSet != null && showSlotNumberSet.size() < 3) {
                for (String slotNumber : showSlotNumberSet) {
                    query = query.equalTo("slotNumber", Integer.parseInt(slotNumber));
                }
            }

            RealmResults<FrequentItem> items = query.findAll();
            ArrayList<String> retVal = new ArrayList<>();

            for (FrequentItem item : items) {
                String originalTitle = item.getTitle();
                String trimmedTitle = originalTitle.replaceAll("\\s", "");

                if (SoundSearcher.matchString(trimmedTitle, keyword)) {
                    retVal.add(originalTitle);
                } else if (!TextUtils.isEmpty(item.getSearchKeyword())){
                    String searchKeyword = item.getSearchKeyword().replaceAll("\\s", "");

                    if (SoundSearcher.matchString(searchKeyword, keyword)) {
                        retVal.add(originalTitle);
                    }
                }
            }
            realm.close();

            return retVal;
        }

        public static QueryLoader castLoader(Loader loader) {
            return (QueryLoader) loader;
        }
    }
}
