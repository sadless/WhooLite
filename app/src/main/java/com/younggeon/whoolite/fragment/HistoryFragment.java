package com.younggeon.whoolite.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.younggeon.whoolite.whooing.loader.WhooingBaseLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by sadless on 2016. 1. 17..
 */
public class HistoryFragment extends WhooLiteActivityBaseFragment {
    private static final int LOADER_ID_BOOKMARK_SELECTED_ITEMS = 1;
    private static final int LOADER_ID_EDIT = 2;

    private static final String INSTANCE_STATE_SHOW_SELECT_SLOT_NUMBER = "show_select_slot_number";

    public static final String ARG_MERGE_ARGUMENTS = "merge_arguments";

    private String[] mSlotNumberItems;
    private SimpleDateFormat mSectionDateFormat;
    private RealmQuery<Entry> mQuery;
    private RealmResults<Entry> mEntries;
    private Bundle mMergeArguments;

    private AlertDialog mSelectSlotNumberDialog;

    private RealmChangeListener<RealmResults<Entry>> mDataChangeListener = new RealmChangeListener<RealmResults<Entry>>() {
        @Override
        public void onChange(RealmResults<Entry> element) {
            mainDataChanged();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeleteConfirmStringId = R.string.delete_entries_confirm;
        if (getArguments() == null) {
            mActionMenuId = R.menu.action_menu_history;
        } else {
            mActionMenuId = R.menu.action_menu_select_merging_entry;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.action_menu_select_merging_entry, menu);
    }

    @Override
    protected void getDataFromSection(Section section) {
        super.getDataFromSection(section);

        if (section != null) {
            mSectionDateFormat = Utility.getDateFormatFromWhooingDateFormat(
                    section.getDateFormat());
        }
    }

    @Override
    protected WhooLiteAdapter createAdapter(GridLayoutManager layoutManager) {
        return new HistoryAdapter(layoutManager);
    }

    @Override
    protected int getDataCount() {
        return mEntries.size();
    }

    @Override
    protected void sectionChanged() {
        mQuery = mRealm.where(Entry.class).equalTo("sectionId", mSectionId);
        if (mMergeArguments != null) {
            String memo = mMergeArguments.getString(WhooingKeyValues.MEMO);
            String entryDate = mMergeArguments.getString(WhooingKeyValues.ENTRY_DATE);

            if (memo == null) {
                memo = "";
            }
            if (entryDate == null) {
                entryDate = mEntryDateFormat.format(new Date());
            }
            mQuery = mQuery.equalTo("entryDate", Integer.parseInt(entryDate))
                    .equalTo("title", mMergeArguments.getString(WhooingKeyValues.ITEM_TITLE))
                    .equalTo("leftAccountType", mMergeArguments.getString(WhooingKeyValues.LEFT_ACCOUNT_TYPE))
                    .equalTo("leftAccountId", mMergeArguments.getString(WhooingKeyValues.LEFT_ACCOUNT_ID))
                    .equalTo("rightAccountType", mMergeArguments.getString(WhooingKeyValues.RIGHT_ACCOUNT_TYPE))
                    .equalTo("rightAccountId", mMergeArguments.getString(WhooingKeyValues.RIGHT_ACCOUNT_ID))
                    .equalTo("memo", memo);
        }
        if (mEntries != null) {
            mEntries.removeChangeListeners();
        }
        mEntries = mQuery.findAllSorted("entryDateRaw", Sort.DESCENDING);
        mEntries.addChangeListener(mDataChangeListener);
        mainDataChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mBinding.setReceiveFailedText(getString(R.string.failed_to_receive_entries));
        mBinding.setNoDataText(getString(R.string.no_entries));
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(INSTANCE_STATE_SHOW_SELECT_SLOT_NUMBER)) {
                showSelectSlotNumber();
            }
        }
        if (getArguments() != null) {
            mMergeArguments = getArguments().getBundle(ARG_MERGE_ARGUMENTS);
            mActionMenuId = R.menu.action_menu_select_merging_entry;
            getLoaderManager().initLoader(LOADER_ID_EDIT, null, this);
        } else {
            mActionMenuId = R.menu.action_menu_history;
        }
        getLoaderManager().initLoader(LOADER_ID_BOOKMARK_SELECTED_ITEMS, null, this);
        sectionChanged();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSelectSlotNumberDialog != null) {
            outState.putBoolean(INSTANCE_STATE_SHOW_SELECT_SLOT_NUMBER, mSelectSlotNumberDialog.isShowing());
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_DELETE_SELECTED_ITEMS: {
                return new EntriesLoader(getActivity(),
                        Request.Method.DELETE,
                        null);
            }
            case LOADER_ID_BOOKMARK_SELECTED_ITEMS: {
                return new FrequentItemsLoader(getActivity(),
                        Request.Method.POST,
                        null);
            }
            case LOADER_ID_REFRESH_MAIN_DATA: {
                Bundle bundle = new Bundle();

                bundle.putString(WhooingKeyValues.SECTION_ID, mSectionId);

                return new EntriesLoader(getActivity(),
                        Request.Method.GET,
                        bundle);
            }
            case LOADER_ID_EDIT: {
                return new EntriesLoader(getActivity(),
                        Request.Method.PUT,
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
            case LOADER_ID_BOOKMARK_SELECTED_ITEMS: {
                final FrequentItemsLoader finalLoader = (FrequentItemsLoader) loader;

                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                mEntries.addChangeListener(mDataChangeListener);

                int code = (Integer) data;

                if (code < 0) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.bookmark_failed)
                            .setMessage(R.string.bookmark_item_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startBookmarkLoader(finalLoader.args.getInt(FrequentItems.COLUMN_SLOT_NUMBER));
                                }
                            }).setNegativeButton(R.string.cancel_input, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mActionMode.finish();
                        }
                    }).create().show();
                } else {
                    Utility.checkResultCodeWithAlert(getActivity(), code);
                    mActionMode.finish();
                    Toast.makeText(getActivity(), R.string.bookmark_selected_item_success, Toast.LENGTH_SHORT).show();
                }
                getLoaderManager().restartLoader(LOADER_ID_BOOKMARK_SELECTED_ITEMS, null, this);
                break;
            }
            case LOADER_ID_EDIT: {
                int code = (Integer) data;

                if (code < 0) {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }

                    final EntriesLoader finalLoader = (EntriesLoader) loader;

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.edit_failed)
                            .setMessage(R.string.edit_entry_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startSendLoader(finalLoader.args);
                                }
                            }).setNegativeButton(R.string.cancel_input, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    })
                            .create().show();
                } else if (Utility.checkResultCodeWithAlert(getActivity(), code)){
                    if (mSelectedItems.size() > 0) {
                        startDeleteLoader();
                    } else {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                        Toast.makeText(getActivity(), R.string.edit_entry_success, Toast.LENGTH_LONG).show();
                    }
                }
                getLoaderManager().restartLoader(LOADER_ID_EDIT, null, this);
                break;
            }
            case LOADER_ID_DELETE_SELECTED_ITEMS: {
                if (mMergeArguments == null) {
                    super.onLoadFinished(loader, data);
                } else {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }

                    int code = (Integer) data;

                    if (code < 0) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.delete_failed)
                                .setMessage(R.string.delete_selected_items_failed)
                                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startDeleteLoader();
                                    }
                                }).setNegativeButton(R.string.cancel_input, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getActivity().finish();
                            }
                        }).create().show();
                    } else if (Utility.checkResultCodeWithAlert(getActivity(), code)) {
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                        Toast.makeText(getActivity(), R.string.edit_entry_success, Toast.LENGTH_LONG).show();
                    }
                    getLoaderManager().restartLoader(LOADER_ID_DELETE_SELECTED_ITEMS, null, this);
                }
                break;
            }
            default: {
                super.onLoadFinished(loader, data);

                break;
            }
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bookmark: {
                showSelectSlotNumber();

                return true;
            }
            case R.id.action_send: {
                Bundle args = (Bundle) mMergeArguments.clone();
                RealmQuery<Entry> query = mRealm.where(Entry.class).equalTo("sectionId", mSectionId);

                query.beginGroup().equalTo("entryId", Long.parseLong(mSelectedItems.get(0)));
                for (int i = 1; i < mSelectedItems.size(); i++) {
                    query.or().equalTo("entryId", Long.parseLong(mSelectedItems.get(i)));
                }
                query.endGroup();

                RealmResults<Entry> selectedEntries = query.findAll();
                double money = Double.parseDouble(mMergeArguments.getString(WhooingKeyValues.MONEY));

                for (Entry entry : selectedEntries) {
                    money += entry.getMoney();
                }

                long entryId = selectedEntries.get(0).getEntryId();

                mSelectedItems.remove("" + entryId);
                args.putLong(WhooingKeyValues.ENTRY_ID, entryId);
                args.putString(WhooingKeyValues.MONEY, "" + money);
                startSendLoader(args);

                return true;
            }
            default: {
                return super.onActionItemClicked(mode, item);
            }
        }
    }

    private void showSelectSlotNumber() {
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
                        startBookmarkLoader(which + 1);
                    }
                }).create();
        mSelectSlotNumberDialog.show();
    }

    private void startBookmarkLoader(int slotNumber) {
        mProgressTitle = getString(R.string.bookmarking);
        mProgressDialog = ProgressDialog.show(getActivity(),
                mProgressTitle,
                getString(R.string.please_wait));
        mEntries.removeChangeListeners();

        Bundle args = new Bundle();

        args.putStringArrayList(WhooingBaseLoader.ARG_SELECTED_ITEMS, mSelectedItems);
        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
        args.putInt(FrequentItems.COLUMN_SLOT_NUMBER, slotNumber);

        FrequentItemsLoader loader = FrequentItemsLoader.castLoader(
                getLoaderManager().getLoader(LOADER_ID_BOOKMARK_SELECTED_ITEMS));

        loader.args = args;
        loader.forceLoad();
    }

    private void startSendLoader(Bundle args) {
        mProgressDialog = ProgressDialog.show(getActivity(),
                null,
                getString(R.string.please_wait));
        mEntries.removeChangeListeners();

        EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().getLoader(LOADER_ID_EDIT));

        loader.args = args;
        loader.forceLoad();
    }

    private class HistoryItemViewHolder extends ItemViewHolder {
        public TextView memo;

        HistoryItemViewHolder(View itemView) {
            super(itemView);

            memo = (TextView) itemView.findViewById(R.id.memo);
        }
    }

    private class HistoryAdapter extends WhooLiteAdapter {
        HistoryAdapter(GridLayoutManager gridLayoutManager) {
            super(gridLayoutManager);

            mItemLayoutId = R.layout.recycler_item_entry;
        }

        @Override
        protected ItemViewHolder createItemViewHolder(View itemView) {
            return new HistoryItemViewHolder(itemView);
        }

        @Override
        protected String getSelectionId(int dataPosition) {
            return "" + mEntries.get(dataPosition).getEntryId();
        }

        @Override
        protected void itemClicked(View view, int dataPosition) {
            if (mMergeArguments == null) {
                Intent intent = new Intent(getActivity(),
                        HistoryDetailActivity.class);

                intent.putExtra(HistoryDetailActivity.EXTRA_SECTION_ID, mSectionId);
                intent.putExtra(HistoryDetailActivity.EXTRA_ENTRY_ID,
                        mEntries.get(dataPosition).getEntryId());
                ActivityCompat.startActivity(getActivity(),
                        intent,
                        ActivityOptionsCompat.makeScaleUpAnimation(view,
                                0,
                                0,
                                view.getWidth(),
                                view.getHeight()
                        ).toBundle());
            } else {
                if (mSelectedItems == null) {
                    startActionMode((ItemViewHolder) view.getTag());
                } else {
                    toggleSelect((ItemViewHolder) view.getTag());
                }
            }
        }

        @Override
        protected String getTitle(int dataPosition) {
            return mEntries.get(dataPosition).getTitle();
        }

        @Override
        protected double getMoney(int dataPosition) {
            return mEntries.get(dataPosition).getMoney();
        }

        @Override
        protected String getLeftAccountType(int dataPosition) {
            return mEntries.get(dataPosition).getLeftAccountType();
        }

        @Override
        protected String getLeftAccountId(int dataPosition) {
            return mEntries.get(dataPosition).getLeftAccountId();
        }

        @Override
        protected String getRightAccountType(int dataPosition) {
            return mEntries.get(dataPosition).getRightAccountType();
        }

        @Override
        protected String getRightAccountId(int dataPosition) {
            return mEntries.get(dataPosition).getRightAccountId();
        }

        @Override
        protected long getValueForItemId(int dataPosition) {
            return mEntries.get(dataPosition).getEntryId();
        }

        @Override
        protected void refreshSections() {
            RealmResults<Entry> items = mQuery.findAllSorted("entryDateRaw", Sort.DESCENDING);
            RealmResults<Entry> dates = items.distinct("entryDate");

            mSectionTitles = new String[dates.size()];
            mSectionDataCounts = new int[dates.size()];

            int i = 0;

            for (Entry item : items) {
                int entryDate = item.getEntryDate();
                if (mSectionDateFormat != null) {
                    try {
                        mSectionTitles[i] = mSectionDateFormat.format(mEntryDateFormat.parse("" + entryDate));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        mSectionTitles[i] = "";
                    }
                } else {
                    mSectionTitles[i] = "";
                }
                mSectionDataCounts[i] = mQuery.findAll().where().equalTo("entryDate", entryDate).findAll().size();
                i++;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            switch (getItemViewType(position)) {
                case VIEW_TYPE_ITEM: {
                    HistoryItemViewHolder viewHolder = (HistoryItemViewHolder) holder;
                    String memo = mEntries.get(getDataPosition(position)).getMemo();

                    if (TextUtils.isEmpty(memo)) {
                        viewHolder.memo.setVisibility(View.GONE);
                    } else {
                        viewHolder.memo.setText(memo);
                        viewHolder.memo.setVisibility(View.VISIBLE);
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }
}
