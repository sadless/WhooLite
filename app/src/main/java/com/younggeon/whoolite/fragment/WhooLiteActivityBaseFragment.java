package com.younggeon.whoolite.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.constant.Actions;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.provider.WhooingProvider;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.WhooingBaseLoader;

import java.text.NumberFormat;
import java.util.Currency;

/**
 * Created by sadless on 2016. 1. 5..
 */
public abstract class WhooLiteActivityBaseFragment extends Fragment implements LoaderManager.LoaderCallbacks,
    ActionMode.Callback {
    private static final int LOADER_ID_ACCOUNT = 100;

    protected static final int LOADER_ID_MAIN_DATA = 101;
    protected static final int LOADER_ID_DELETE_SELECTED_ITEMS = 102;
    protected static final int LOADER_ID_REFRESH_MAIN_DATA = 103;

    private static final String INSTANCE_STATE_SELECTED_ITEMS = "selected_items";
    private static final String INSTANCE_STATE_SHOW_DELETE_CONFIRM = "show_delete_confirm";
    private static final String INSTANCE_STATE_PROGRESS_DIALOG = "progress_dialog";
    private static final String INSTANCE_STATE_PROGRESS_TITLE = "progress_title";

    abstract protected WhooLiteAdapter createAdapter(GridLayoutManager layoutManager);
    abstract protected Uri getMainDataUri();

    protected int mReceiveFailedStringId;
    protected int mNoDataStringId;
    protected int mDeleteConfirmStringId;
    protected int mActionMenuId;
    protected String mSectionId;
    protected SparseBooleanArray mSelectedItems;
    protected String mProgressTitle;
    protected String mMainDataSortOrder;

    protected ActionMode mActionMode;
    protected RecyclerView mRecyclerView;
    protected ProgressDialog mProgressDialog;

    private Currency mCurrency;

    private Button mRetryButton;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;
    private LinearLayout mEmptyLayout;
    private AlertDialog mDeleteConfirmDialog;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setSectionId(intent.getStringExtra(Actions.EXTRA_SECTION_ID));
            getLoaderManager().restartLoader(LOADER_ID_REFRESH_MAIN_DATA,
                    null,
                    WhooLiteActivityBaseFragment.this).forceLoad();
            getLoaderManager().restartLoader(LOADER_ID_MAIN_DATA, null, WhooLiteActivityBaseFragment.this);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whoo_lite_base, container, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                getResources().getInteger(R.integer.input_item_span_count)));
        mRecyclerView.setAdapter(createAdapter((GridLayoutManager) mRecyclerView.getLayoutManager()));
        if (savedInstanceState != null) {
            int[] selectedItems = savedInstanceState.getIntArray(INSTANCE_STATE_SELECTED_ITEMS);

            if (selectedItems != null) {
                mSelectedItems = new SparseBooleanArray();
                for (int i : selectedItems) {
                    mSelectedItems.put(i, true);
                }
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
            }
            if (savedInstanceState.getBoolean(INSTANCE_STATE_SHOW_DELETE_CONFIRM)) {
                showDeleteConfirm();
            } else if (savedInstanceState.getBoolean(INSTANCE_STATE_PROGRESS_DIALOG)) {
                mProgressTitle = savedInstanceState.getString(INSTANCE_STATE_PROGRESS_TITLE);
                mProgressDialog = ProgressDialog.show(getActivity(),
                        mProgressTitle,
                        getString(R.string.please_wait));
            }
        }
        setSectionId(prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null));
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
        mEmptyLayout = (LinearLayout) view.findViewById(R.id.empty_layout);
        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        mRetryButton = (Button) view.findViewById(R.id.retry);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEmptyText.setVisibility(View.GONE);
                mRetryButton.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                getLoaderManager().initLoader(LOADER_ID_REFRESH_MAIN_DATA,
                        null,
                        WhooLiteActivityBaseFragment.this).forceLoad();
            }
        });
        getActivity().registerReceiver(mReceiver, new IntentFilter(Actions.SECTION_ID_CHANGED));
        getLoaderManager().initLoader(LOADER_ID_MAIN_DATA, null, this);
        getLoaderManager().initLoader(LOADER_ID_ACCOUNT, null, this);
        getLoaderManager().initLoader(LOADER_ID_DELETE_SELECTED_ITEMS, null, this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mActionMode == null && mSectionId != null && getLoaderManager().getLoader(LOADER_ID_REFRESH_MAIN_DATA) == null) {
            getLoaderManager().initLoader(LOADER_ID_REFRESH_MAIN_DATA, null, this).forceLoad();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSelectedItems != null) {
            int[] selectedItems = new int[mSelectedItems.size()];

            for (int i = 0; i < mSelectedItems.size(); i++) {
                selectedItems[i] = mSelectedItems.keyAt(i);
            }
            outState.putIntArray(INSTANCE_STATE_SELECTED_ITEMS, selectedItems);
        }
        if (mDeleteConfirmDialog != null) {
            outState.putBoolean(INSTANCE_STATE_SHOW_DELETE_CONFIRM, mDeleteConfirmDialog.isShowing());
        }
        if (mProgressDialog != null) {
            boolean isShowing = mProgressDialog.isShowing();

            outState.putBoolean(INSTANCE_STATE_PROGRESS_DIALOG, isShowing);
            if (isShowing) {
                outState.putString(INSTANCE_STATE_PROGRESS_TITLE, mProgressTitle);
            }
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_MAIN_DATA: {
                return new CursorLoader(getActivity(),
                        getMainDataUri(),
                        null,
                        null,
                        null,
                        mMainDataSortOrder);
            }
            case LOADER_ID_ACCOUNT: {
                return new CursorLoader(getActivity(),
                        WhooingProvider.getAccountsUri(mSectionId),
                        null,
                        null,
                        null,
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
            case LOADER_ID_MAIN_DATA: {
                Cursor cursor = (Cursor) data;

                if (cursor.getCount() > 0) {
                    WhooLiteAdapter adapter = (WhooLiteAdapter) mRecyclerView.getAdapter();

                    adapter.changeCursor(cursor);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyLayout.setVisibility(View.GONE);
                } else {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyLayout.setVisibility(View.VISIBLE);
                }
                break;
            }
            case LOADER_ID_ACCOUNT: {
                getLoaderManager().restartLoader(LOADER_ID_MAIN_DATA, null, this);
                break;
            }
            case LOADER_ID_DELETE_SELECTED_ITEMS: {
                final WhooingBaseLoader finalLoader = (WhooingBaseLoader) loader;

                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                getLoaderManager().getLoader(LOADER_ID_MAIN_DATA).startLoading();

                int code = (Integer) data;

                if (code < 0) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.delete_failed)
                            .setMessage(R.string.delete_selected_items_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startDeleteLoader(finalLoader.args.getIntArray(WhooingBaseLoader.ARG_CURSOR_INDEX));
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
                }
                getLoaderManager().restartLoader(LOADER_ID_DELETE_SELECTED_ITEMS, null, this);
                break;
            }
            case LOADER_ID_REFRESH_MAIN_DATA: {
                int code = (Integer) data;

                if (code > 0) {
                    if (Utility.checkResultCodeWithAlert(getActivity(), code)) {
                        mEmptyText.setText(mNoDataStringId);
                    }
                } else {
                    mEmptyText.setText(mReceiveFailedStringId);
                }
                mRetryButton.setVisibility(View.VISIBLE);
                mEmptyText.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                getLoaderManager().destroyLoader(LOADER_ID_REFRESH_MAIN_DATA);
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(mActionMenuId, menu);
        mode.setTitle(getString(R.string.item_selected,
                mSelectedItems.size()));
        mRecyclerView.getAdapter().notifyDataSetChanged();

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete: {
                showDeleteConfirm();

                return true;
            }
            default: {
                return false;
            }
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    protected void setSectionId(String sectionId) {
        if (sectionId != null) {
            mSectionId = sectionId;

            Cursor c = getActivity().getContentResolver().query(WhooingProvider.getSectionUri(sectionId),
                    null,
                    null,
                    null,
                    null);

            if (c != null) {
                c.moveToFirst();
                getSectionDataFromCursor(c);
                c.close();
            } else {
                getSectionDataFromCursor(null);
            }
        }
    }

    protected void getSectionDataFromCursor(Cursor cursor) {
        if (cursor != null) {
            mCurrency = Currency.getInstance(cursor.getString(Sections.COLUMN_INDEX_CURRENCY));
        } else {
            mCurrency = null;
        }
    }

    private void showDeleteConfirm() {
        mDeleteConfirmDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_confirm)
                .setMessage(getString(mDeleteConfirmStringId, mSelectedItems.size()))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int[] index = new int[mSelectedItems.size()];

                        for (int i = 0; i < index.length; i++) {
                            index[i] = mSelectedItems.keyAt(i);
                        }
                        startDeleteLoader(index);
                    }
                }).setNegativeButton(R.string.cancel, null)
                .create();
        mDeleteConfirmDialog.show();
    }

    private void startDeleteLoader(int[] index) {
        mProgressTitle = getString(R.string.deleting);
        mProgressDialog = ProgressDialog.show(getActivity(),
                mProgressTitle,
                getString(R.string.please_wait));
        getLoaderManager().getLoader(LOADER_ID_MAIN_DATA).stopLoading();

        Bundle args = new Bundle();

        args.putIntArray(WhooingBaseLoader.ARG_CURSOR_INDEX, index);
        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);

        WhooingBaseLoader loader = WhooingBaseLoader.castLoader(
                getLoaderManager().getLoader(LOADER_ID_DELETE_SELECTED_ITEMS));

        loader.args = args;
        loader.forceLoad();
    }

    protected class SectionViewHolder extends RecyclerView.ViewHolder {
        public TextView name;

        public SectionViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    protected class ItemViewHolder extends RecyclerView.ViewHolder {
        public ImageButton icon;
        public TextView title;
        public TextView money;
        public TextView left;
        public TextView right;
        public Drawable iconSelectableBackground;
        public Drawable itemViewSelectableBackground;

        public ItemViewHolder(View itemView) {
            super(itemView);

            itemView.setTag(this);
            itemViewSelectableBackground = itemView.getBackground();
            icon = (ImageButton) itemView.findViewById(R.id.icon);
            icon.setTag(this);
            iconSelectableBackground = icon.getBackground();
            title = (TextView) itemView.findViewById(R.id.title);
            money = (TextView) itemView.findViewById(R.id.money);
            left = (TextView) itemView.findViewById(R.id.left);
            right = (TextView) itemView.findViewById(R.id.right);
        }
    }

    protected abstract class WhooLiteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        protected static final int VIEW_TYPE_SECTION = 0;
        protected static final int VIEW_TYPE_ITEM = 1;

        public Cursor itemCursor;

        private TextDrawable.IBuilder mIconBuilder;
        private int[] mSectionData;
        private int[] mSectionDataCounts;

        protected int mColumnIndexTitle;
        protected int mColumnIndexMoney;
        protected int mColumnIndexLeftAccountType;
        protected int mColumnIndexRightAccountType;
        protected int mColumnIndexItemId;
        protected int mItemLayoutId;

        abstract protected ItemViewHolder createItemViewHolder(View itemView);
        abstract protected void itemClicked(View view, Cursor cursor);
        abstract protected Uri getSectionDataUri();
        abstract protected String getSectionText(int sectionData);

        public WhooLiteAdapter(final GridLayoutManager gridLayoutManager) {
            super();

            mIconBuilder = TextDrawable.builder().round();
            setHasStableIds(true);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    switch (getItemViewType(position)) {
                        case VIEW_TYPE_SECTION: {
                            return gridLayoutManager.getSpanCount();
                        }
                        case VIEW_TYPE_ITEM: {
                            return 1;
                        }
                        default: {
                            return -1;
                        }
                    }
                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            switch (viewType) {
                case VIEW_TYPE_SECTION: {
                    View view = inflater.inflate(R.layout.recycler_item_whoolite_section, parent, false);

                    return new SectionViewHolder(view);
                }
                case VIEW_TYPE_ITEM: {
                    View view = inflater.inflate(mItemLayoutId, parent, false);
                    ItemViewHolder viewHolder = createItemViewHolder(view);

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ItemViewHolder vh = (ItemViewHolder) v.getTag();

                            if (mActionMode == null) {
                                itemCursor.moveToPosition(getCursorPosition(vh.getAdapterPosition()));
                                itemClicked(v, itemCursor);
                            } else {
                                toggleSelect(vh);
                            }
                        }
                    });
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            ItemViewHolder vh = (ItemViewHolder) v.getTag();

                            if (mActionMode == null) {
                                startActionMode(vh);
                            } else {
                                toggleSelect(vh);
                            }

                            return true;
                        }
                    });
                    viewHolder.icon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ItemViewHolder vh = (ItemViewHolder) v.getTag();

                            if (mActionMode == null) {
                                startActionMode((ItemViewHolder) v.getTag());
                            } else {
                                toggleSelect(vh);
                            }
                        }
                    });

                    return viewHolder;
                }
                default: {
                    return null;
                }
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int sum = 0;

            switch (getItemViewType(position)) {
                case VIEW_TYPE_SECTION: {
                    for (int i = 0; i < mSectionDataCounts.length; i++) {
                        if (position <= sum + i) {
                            SectionViewHolder vh = (SectionViewHolder) holder;

                            vh.name.setText(getSectionText(mSectionData[i]));
                            break;
                        }
                        sum += mSectionDataCounts[i];
                    }
                    break;
                }
                case VIEW_TYPE_ITEM: {
                    ItemViewHolder vh = (ItemViewHolder) holder;

                    position = getCursorPosition(position);
                    itemCursor.moveToPosition(position);

                    String title = itemCursor.getString(mColumnIndexTitle);
                    double money = itemCursor.getDouble(mColumnIndexMoney);
                    String leftAccountType = itemCursor.getString(mColumnIndexLeftAccountType);
                    String rightAccountType = itemCursor.getString(mColumnIndexRightAccountType);

                    if (mActionMode != null && mSelectedItems.get(position)) {
                        vh.icon.setImageResource(R.drawable.ic_check);
                        vh.icon.setBackgroundResource(R.drawable.selected_icon_bg);
                        vh.itemView.setBackgroundResource(R.color.primary);
                    } else {
                        vh.icon.setImageDrawable(mIconBuilder.build(title.substring(0, 1),
                                ColorGenerator.MATERIAL.getColor(title)));
                        if (Build.VERSION.SDK_INT >= 16) {
                            vh.icon.setBackground(vh.iconSelectableBackground);
                            vh.itemView.setBackground(vh.itemViewSelectableBackground);
                        } else {
                            vh.icon.setBackgroundDrawable(vh.iconSelectableBackground);
                            vh.itemView.setBackgroundDrawable(vh.itemViewSelectableBackground);
                        }
                    }
                    vh.title.setText(title);
                    if (money < WhooingKeyValues.EPSILON) {
                        vh.money.setText(R.string.not_assigned);
                    } else {
                        vh.money.setText(getString(R.string.currency_format,
                                mCurrency.getSymbol(),
                                NumberFormat.getInstance().format(money)));
                    }
                    if (TextUtils.isEmpty(leftAccountType)) {
                        vh.left.setText(R.string.not_assigned);
                    } else {
                        String leftTitle = itemCursor.getString(itemCursor.getColumnCount() - 2);

                        if (TextUtils.isEmpty(leftTitle)) {
                            vh.left.setText(R.string.unknown);
                        } else {
                            switch (leftAccountType) {
                                case WhooingKeyValues.ASSETS: {
                                    leftTitle += "+";
                                    break;
                                }
                                case WhooingKeyValues.LIABILITIES:case WhooingKeyValues.CAPITAL: {
                                    leftTitle += "-";
                                    break;
                                }
                            }
                            vh.left.setText(leftTitle);
                        }
                    }
                    if (TextUtils.isEmpty(rightAccountType)) {
                        vh.right.setText(R.string.not_assigned);
                    } else {
                        String rightTitle = itemCursor.getString(itemCursor.getColumnCount() - 1);

                        if (TextUtils.isEmpty(rightTitle)) {
                            vh.left.setText(R.string.unknown);
                        } else {
                            switch (rightAccountType) {
                                case WhooingKeyValues.ASSETS: {
                                    rightTitle += "-";
                                    break;
                                }
                                case WhooingKeyValues.LIABILITIES:case WhooingKeyValues.CAPITAL: {
                                    rightTitle += "+";
                                    break;
                                }
                            }
                            vh.right.setText(rightTitle);
                        }
                    }
                    break;
                }
                default:
            }
        }

        @Override
        public int getItemCount() {
            if (itemCursor == null) {
                return 0;
            }
            if (mSectionData == null) {
                return itemCursor.getCount();
            } else {
                return itemCursor.getCount() + mSectionData.length;
            }
        }

        @Override
        public long getItemId(int position) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_SECTION: {
                    int sum = 0;

                    for (int i = 0; i < mSectionDataCounts.length; i++) {
                        if (position <= sum + i) {
                            return ("section:" + mSectionData[i]).hashCode();
                        }
                        sum += mSectionDataCounts[i];
                    }

                    return -1;
                }
                case VIEW_TYPE_ITEM: {
                    itemCursor.moveToPosition(getCursorPosition(position));

                    return itemCursor.getString(mColumnIndexItemId).hashCode();
                }
                default: {
                    return -1;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (mSectionData == null) {
                return VIEW_TYPE_ITEM;
            }

            int sum = 0;

            for (int i = 0; i < mSectionDataCounts.length; i++) {
                if (position == sum + i) {
                    return VIEW_TYPE_SECTION;
                } else if (position < sum + i) {
                    return VIEW_TYPE_ITEM;
                }
                sum += mSectionDataCounts[i];
            }

            return VIEW_TYPE_ITEM;
        }

        public void changeCursor(Cursor cursor) {
            itemCursor = cursor;

            Cursor c = getActivity().getContentResolver().query(getSectionDataUri(),
                    null,
                    null,
                    null,
                    null);

            if (c != null) {
                if (c.moveToFirst()) {
                    if (c.getCount() > 1) {
                        int i = 0;

                        mSectionData = new int[c.getCount()];
                        mSectionDataCounts = new int[c.getCount()];
                        do {
                            mSectionData[i] = c.getInt(0);
                            mSectionDataCounts[i] = c.getInt(1);
                            i++;
                        } while (c.moveToNext());
                    } else {
                        mSectionData = null;
                        mSectionDataCounts = null;
                    }
                }
                c.close();
            }
            notifyDataSetChanged();
        }

        protected int getCursorPosition(int position) {
            int sum = 0;

            if (mSectionDataCounts != null) {
                for (int i : mSectionDataCounts) {
                    if (position > sum) {
                        position--;
                    } else {
                        break;
                    }
                    sum += i;
                }
            }

            return position;
        }

        private void startActionMode(ItemViewHolder vh) {
            if (mSelectedItems == null) {
                mSelectedItems = new SparseBooleanArray();
            } else {
                mSelectedItems.clear();
            }
            mSelectedItems.put(getCursorPosition(vh.getAdapterPosition()), true);
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(WhooLiteActivityBaseFragment.this);
        }

        private void toggleSelect(ItemViewHolder vh) {
            int position = vh.getAdapterPosition();
            int cursorPosition = getCursorPosition(position);

            if (mSelectedItems.get(cursorPosition)) {
                mSelectedItems.delete(cursorPosition);
                if (mSelectedItems.size() == 0) {
                    mActionMode.finish();

                    return;
                }
            } else {
                mSelectedItems.put(cursorPosition, true);
            }
            mRecyclerView.getAdapter().notifyItemChanged(position);
            mActionMode.setTitle(getString(R.string.item_selected,
                    mSelectedItems.size()));
        }
    }
}
