package com.younggeon.whoolite.fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.HistoryDetailActivity;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Entries;
import com.younggeon.whoolite.db.schema.FrequentItems;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.provider.WhooingProvider;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.EntriesLoader;
import com.younggeon.whoolite.whooing.loader.FrequentItemsLoader;
import com.younggeon.whoolite.whooing.loader.WhooingBaseLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by sadless on 2016. 1. 17..
 */
public class HistoryFragment extends WhooLiteActivityBaseFragment {
    private static final int LOADER_ID_BOOKMARK_SELECTED_ITEMS = 1;

    private static final String INSTANCE_STATE_SHOW_SELECT_SLOT_NUMBER = "show_select_slot_number";

    private String[] mSlotNumberItems;
    private SimpleDateFormat mEntryDateFormat;
    private SimpleDateFormat mSectionDateFormat;

    private AlertDialog mSelectSlotNumberDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReceiveFailedStringId = R.string.failed_to_receive_entries;
        mNoDataStringId = R.string.no_entries;
        mDeleteConfirmStringId = R.string.delete_entries_confirm;
        mActionMenuId = R.menu.action_menu_history;
    }

    @Override
    protected WhooLiteAdapter createAdapter(GridLayoutManager layoutManager) {
        return new HistoryAdapter(layoutManager);
    }

    @Override
    protected Uri getMainDataUri() {
        return WhooingProvider.getEntriesUri(mSectionId);
    }

    @Override
    protected void getSectionDataFromCursor(Cursor cursor) {
        super.getSectionDataFromCursor(cursor);

        if (cursor != null) {
            mSectionDateFormat = Utility.getDateFormatFromWhooingDateFormat(
                    cursor.getString(Sections.COLUMN_INDEX_DATE_FORMAT));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(INSTANCE_STATE_SHOW_SELECT_SLOT_NUMBER)) {
                showSelectSlotNumber();
            }
        }
        mEntryDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        getLoaderManager().initLoader(LOADER_ID_BOOKMARK_SELECTED_ITEMS, null, this);

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
            default: {
                return super.onCreateLoader(id, args);
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
                getLoaderManager().getLoader(LOADER_ID_MAIN_DATA).startLoading();

                int code = (Integer) data;

                if (code < 0) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.bookmark_failed)
                            .setMessage(R.string.bookmark_item_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startBookmarkLoader(finalLoader.args.getIntArray(WhooingBaseLoader.ARG_CURSOR_INDEX),
                                            finalLoader.args.getInt(FrequentItems.COLUMN_SLOT_NUMBER));
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
                        int[] index = new int[mSelectedItems.size()];

                        for (int i = 0; i < index.length; i++) {
                            index[i] = mSelectedItems.keyAt(i);
                        }
                        startBookmarkLoader(index, which + 1);
                    }
                }).create();
        mSelectSlotNumberDialog.show();
    }

    private void startBookmarkLoader(int[] index, int slotNumber) {
        mProgressTitle = getString(R.string.bookmarking);
        mProgressDialog = ProgressDialog.show(getActivity(),
                mProgressTitle,
                getString(R.string.please_wait));
        getLoaderManager().getLoader(LOADER_ID_MAIN_DATA).stopLoading();

        Bundle args = new Bundle();

        args.putIntArray(WhooingBaseLoader.ARG_CURSOR_INDEX, index);
        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
        args.putInt(FrequentItems.COLUMN_SLOT_NUMBER, slotNumber);

        FrequentItemsLoader loader = FrequentItemsLoader.castLoader(
                getLoaderManager().getLoader(LOADER_ID_BOOKMARK_SELECTED_ITEMS));

        loader.args = args;
        loader.forceLoad();
    }

    private class HistoryItemViewHolder extends ItemViewHolder {
        public TextView memo;

        public HistoryItemViewHolder(View itemView) {
            super(itemView);

            memo = (TextView) itemView.findViewById(R.id.memo);
        }
    }

    private class HistoryAdapter extends WhooLiteAdapter {
        public HistoryAdapter(GridLayoutManager gridLayoutManager) {
            super(gridLayoutManager);

            mColumnIndexTitle = Entries.COLUMN_INDEX_TITLE;
            mColumnIndexMoney = Entries.COLUMN_INDEX_MONEY;
            mColumnIndexLeftAccountType = Entries.COLUMN_INDEX_LEFT_ACCOUNT_TYPE;
            mColumnIndexRightAccountType = Entries.COLUMN_INDEX_RIGHT_ACCOUNT_TYPE;
            mColumnIndexItemId = Entries.COLUMN_INDEX_ENTRY_ID;
            mItemLayoutId = R.layout.recycler_item_entry;
        }

        @Override
        protected ItemViewHolder createItemViewHolder(View itemView) {
            return new HistoryItemViewHolder(itemView);
        }

        @Override
        protected void itemClicked(View view, Cursor cursor) {
            Intent intent = new Intent(getActivity(),
                    HistoryDetailActivity.class);

            intent.putExtra(HistoryDetailActivity.EXTRA_SECTION_ID, mSectionId);
            intent.putExtra(HistoryDetailActivity.EXTRA_ENTRY_ID,
                    cursor.getLong(Entries.COLUMN_INDEX_ENTRY_ID));
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
            return WhooingProvider.getEntriesDateCountsUri(mSectionId);
        }

        @Override
        protected String getSectionText(int sectionData) {
            try {
                return mSectionDateFormat.format(mEntryDateFormat.parse("" + sectionData));
            } catch (ParseException e) {
                e.printStackTrace();

                return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            switch (getItemViewType(position)) {
                case VIEW_TYPE_ITEM: {
                    HistoryItemViewHolder viewHolder = (HistoryItemViewHolder) holder;
                    String memo = itemCursor.getString(Entries.COLUMN_INDEX_MEMO);

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
