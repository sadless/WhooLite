package com.younggeon.whoolite.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.constant.Actions;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.databinding.FragmentWhooLiteBaseBinding;
import com.younggeon.whoolite.databinding.RecyclerItemWhooliteSectionBinding;
import com.younggeon.whoolite.realm.Account;
import com.younggeon.whoolite.realm.Section;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.WhooingBaseLoader;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by sadless on 2016. 1. 5..
 */
public abstract class WhooLiteActivityBaseFragment extends Fragment implements LoaderManager.LoaderCallbacks,
    ActionMode.Callback {
    protected static final int LOADER_ID_DELETE_SELECTED_ITEMS = 101;
    protected static final int LOADER_ID_REFRESH_MAIN_DATA = 102;

    private static final String INSTANCE_STATE_SELECTED_ITEMS = "selected_items";
    private static final String INSTANCE_STATE_SHOW_DELETE_CONFIRM = "show_delete_confirm";
    private static final String INSTANCE_STATE_PROGRESS_DIALOG = "progress_dialog";
    private static final String INSTANCE_STATE_PROGRESS_TITLE = "progress_title";

    abstract protected WhooLiteAdapter createAdapter(GridLayoutManager layoutManager);
    abstract protected int getDataCount();
    abstract protected void sectionChanged();

    protected int mDeleteConfirmStringId;
    protected int mActionMenuId;
    protected String mSectionId;
    protected ArrayList<String> mSelectedItems;
    protected String mProgressTitle;
    protected Realm mRealm;
    protected ObservableInt mDataCount;
    protected ObservableField<String> mQueryText;

    protected ActionMode mActionMode;
    protected FragmentWhooLiteBaseBinding mBinding;
    protected ProgressDialog mProgressDialog;

    private AlertDialog mDeleteConfirmDialog;

    private Currency mCurrency;
    private RealmQuery<Account> mAccountQuery;
    private RealmResults<Account> mAccounts;
    private Section mSection;
    private boolean mSectionReady = false;
    private boolean mAccountsReady = false;
    private ObservableBoolean mReceived;
    private ObservableBoolean mFailed;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String sectionId = intent.getStringExtra(Actions.EXTRA_SECTION_ID);

            if (!sectionId.equals(mSectionId)) {
                setSectionId(intent.getStringExtra(Actions.EXTRA_SECTION_ID));
                getLoaderManager().restartLoader(LOADER_ID_REFRESH_MAIN_DATA,
                        null,
                        WhooLiteActivityBaseFragment.this).forceLoad();
                sectionChanged();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = DataBindingUtil.bind(inflater.inflate(R.layout.fragment_whoo_lite_base, container, false));
        mBinding.setFragment(this);
        mBinding.setDataCount(mDataCount = new ObservableInt());
        mBinding.setReceived(mReceived = new ObservableBoolean(false));
        mBinding.setFailed(mFailed = new ObservableBoolean(false));
        mBinding.setQueryText(mQueryText = new ObservableField<>());
        mBinding.setNoSearchResultText(getString(R.string.no_search_result));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (savedInstanceState != null) {
            mSelectedItems = savedInstanceState.getStringArrayList(INSTANCE_STATE_SELECTED_ITEMS);

            if (mSelectedItems != null && mSelectedItems.size() > 0) {
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
        mRealm = Realm.getDefaultInstance();
        setSectionId(prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null));
        getActivity().registerReceiver(mReceiver, new IntentFilter(Actions.SECTION_ID_CHANGED));
        mAccountQuery = mRealm.where(Account.class).equalTo("sectionId", mSectionId);
        mAccounts = mAccountQuery.findAll();
        mAccounts.addChangeListener(new RealmChangeListener<RealmResults<Account>>() {
            @Override
            public void onChange(RealmResults<Account> element) {
                if (WhooLiteActivityBaseFragment.this.isAdded()) {
                    synchronized (WhooLiteActivityBaseFragment.this) {
                        mAccountsReady = true;
                        if (mSectionReady) {
                            refreshMainData();
                        }
                    }
                }
            }
        });
        getLoaderManager().initLoader(LOADER_ID_DELETE_SELECTED_ITEMS, null, this);

        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getActivity().unregisterReceiver(mReceiver);
        mAccounts.removeChangeListeners();
        if (mSection != null) {
            mSection.removeChangeListeners();
        }
        mRealm.close();
    }

    @Override
    public void onPause() {
        super.onPause();

        mSectionReady = mAccountsReady = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSelectedItems != null && mSelectedItems.size() > 0) {
            outState.putStringArrayList(INSTANCE_STATE_SELECTED_ITEMS, mSelectedItems);
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
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_DELETE_SELECTED_ITEMS: {
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
                        mReceived.set(true);
                        mFailed.set(false);
                    }
                } else {
                    mFailed.set(true);
                }
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
        mBinding.recyclerView.getAdapter().notifyDataSetChanged();

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
        mSelectedItems = null;
        mBinding.recyclerView.getAdapter().notifyDataSetChanged();
    }

    protected void setSectionId(String sectionId) {
        if (sectionId != null) {
            mSectionId = sectionId;
            mAccountQuery = mRealm.where(Account.class).equalTo("sectionId", mSectionId);

            Section section = Realm.getDefaultInstance().where(Section.class).equalTo("sectionId", sectionId).findFirst();

            if (section != null) {
                section.removeChangeListeners();
                section.addChangeListener(new RealmChangeListener<RealmModel>() {
                    @Override
                    public void onChange(RealmModel element) {
                        synchronized (WhooLiteActivityBaseFragment.this) {
                            sectionReady();
                        }
                    }
                });
                mSection = section;
                sectionReady();
            }
            getDataFromSection(section);
        }
    }

    protected void getDataFromSection(Section section) {
        if (section != null) {
            mCurrency = Currency.getInstance(section.getCurrency());
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
                        startDeleteLoader();
                    }
                }).setNegativeButton(R.string.cancel, null)
                .create();
        mDeleteConfirmDialog.show();
    }

    private void startDeleteLoader() {
        mProgressTitle = getString(R.string.deleting);
        mProgressDialog = ProgressDialog.show(getActivity(),
                mProgressTitle,
                getString(R.string.please_wait));

        Bundle args = new Bundle();

        args.putStringArrayList(WhooingBaseLoader.ARG_SELECTED_ITEMS, mSelectedItems);
        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);

        WhooingBaseLoader loader = WhooingBaseLoader.castLoader(
                getLoaderManager().getLoader(LOADER_ID_DELETE_SELECTED_ITEMS));

        loader.args = args;
        loader.forceLoad();
    }

    private void refreshMainData() {
        if (mActionMode == null && mSectionId != null && getLoaderManager().getLoader(LOADER_ID_REFRESH_MAIN_DATA) == null) {
            getLoaderManager().initLoader(LOADER_ID_REFRESH_MAIN_DATA, null, this).forceLoad();
        }
    }

    private void sectionReady() {
        synchronized (this) {
            mSectionReady = true;
            if (mAccountsReady) {
                mainDataChanged();
            }
        }
    }

    public void retryClicked() {
        mFailed.set(false);
        getLoaderManager().initLoader(LOADER_ID_REFRESH_MAIN_DATA,
                null,
                this).forceLoad();
    }

    protected void mainDataChanged() {
        WhooLiteAdapter adapter = (WhooLiteAdapter) mBinding.recyclerView.getAdapter();

        if (adapter == null) {
            mBinding.recyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                    getResources().getInteger(R.integer.input_item_span_count)));
            mBinding.recyclerView.setAdapter(createAdapter((GridLayoutManager) mBinding.recyclerView.getLayoutManager()));
        } else {
            adapter.refresh();
        }
        mDataCount.set(getDataCount());
    }

    protected class SectionViewHolder extends RecyclerView.ViewHolder {
        public RecyclerItemWhooliteSectionBinding binding;

        public SectionViewHolder(View itemView) {
            super(itemView);

            binding = DataBindingUtil.bind(itemView);
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

        private TextDrawable.IBuilder mIconBuilder;

        protected String[] mSectionTitles;
        protected int[] mSectionDataCounts;
        protected int mItemLayoutId;

        abstract protected ItemViewHolder createItemViewHolder(View itemView);
        abstract protected void itemClicked(View view, int dataPosition);
        abstract protected String getSelectionId(int dataPosition);
        abstract protected String getTitle(int dataPosition);
        abstract protected double getMoney(int dataPosition);
        abstract protected String getLeftAccountType(int dataPosition);
        abstract protected String getLeftAccountId(int dataPosition);
        abstract protected String getRightAccountType(int dataPosition);
        abstract protected String getRightAccountId(int dataPosition);
        abstract protected long getValueForItemId(int dataPosition);
        abstract protected void refreshSections();

        public WhooLiteAdapter(final GridLayoutManager gridLayoutManager) {
            super();

            refreshSections();
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
                                itemClicked(v, getDataPosition(vh.getAdapterPosition()));
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

                            vh.binding.setText(mSectionTitles[i]);
                            break;
                        }
                        sum += mSectionDataCounts[i];
                    }
                    break;
                }
                case VIEW_TYPE_ITEM: {
                    ItemViewHolder vh = (ItemViewHolder) holder;

                    position = getDataPosition(position);

                    String title = getTitle(position);
                    double money = getMoney(position);
                    String leftAccountType = getLeftAccountType(position);
                    String rightAccountType = getRightAccountType(position);
                    Account leftAccount = mAccountQuery.findAll().where()
                            .equalTo("accountType", leftAccountType)
                            .equalTo("accountId", getLeftAccountId(position))
                            .findFirst();
                    Account rightAccount = mAccountQuery.findAll().where()
                            .equalTo("accountType", rightAccountType)
                            .equalTo("accountId", getRightAccountId(position))
                            .findFirst();
                    String leftAccountTitle = leftAccount == null ? null : leftAccount.getTitle();
                    String rightAccountTitle = rightAccount == null ? null : rightAccount.getTitle();

                    if (mActionMode != null && mSelectedItems.contains(getSelectionId(position))) {
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
                        if (mCurrency == null) {
                            vh.money.setText(R.string.unknown);
                        } else {
                            vh.money.setText(getString(R.string.currency_format,
                                    mCurrency.getSymbol(),
                                    NumberFormat.getInstance().format(money)));
                        }
                    }
                    if (TextUtils.isEmpty(leftAccountType)) {
                        vh.left.setText(R.string.not_assigned);
                    } else {
                        if (TextUtils.isEmpty(leftAccountTitle)) {
                            vh.left.setText(R.string.unknown);
                        } else {
                            switch (leftAccountType) {
                                case WhooingKeyValues.ASSETS: {
                                    leftAccountTitle += "+";
                                    break;
                                }
                                case WhooingKeyValues.LIABILITIES:case WhooingKeyValues.CAPITAL: {
                                    leftAccountTitle += "-";
                                    break;
                                }
                            }
                            vh.left.setText(leftAccountTitle);
                        }
                    }
                    if (TextUtils.isEmpty(rightAccountType)) {
                        vh.right.setText(R.string.not_assigned);
                    } else {
                        if (TextUtils.isEmpty(rightAccountTitle)) {
                            vh.left.setText(R.string.unknown);
                        } else {
                            switch (rightAccountType) {
                                case WhooingKeyValues.ASSETS: {
                                    rightAccountTitle += "-";
                                    break;
                                }
                                case WhooingKeyValues.LIABILITIES:case WhooingKeyValues.CAPITAL: {
                                    rightAccountTitle += "+";
                                    break;
                                }
                            }
                            vh.right.setText(rightAccountTitle);
                        }
                    }
                    break;
                }
                default:
            }
        }

        @Override
        public int getItemCount() {
            if (mSectionTitles != null) {
                return getDataCount() + mSectionTitles.length;
            } else {
                return getDataCount();
            }
        }

        @Override
        public long getItemId(int position) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_SECTION: {
                    int sum = 0;

                    for (int i = 0; i < mSectionDataCounts.length; i++) {
                        if (position <= sum + i) {
                            return ("section:" + mSectionTitles[i]).hashCode();
                        }
                        sum += mSectionDataCounts[i];
                    }

                    return -1;
                }
                case VIEW_TYPE_ITEM: {
                    return getValueForItemId(getDataPosition(position));
                }
                default: {
                    return -1;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (mSectionTitles == null) {
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

        public void refresh() {
            refreshSections();
            notifyDataSetChanged();
        }

        protected int getDataPosition(int position) {
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
                mSelectedItems = new ArrayList<>();
            } else {
                mSelectedItems.clear();
            }
            mSelectedItems.add(getSelectionId(getDataPosition(vh.getAdapterPosition())));
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(WhooLiteActivityBaseFragment.this);
        }

        private void toggleSelect(ItemViewHolder vh) {
            int position = vh.getAdapterPosition();
            int cursorPosition = getDataPosition(position);
            String selectionId = getSelectionId(cursorPosition);

            if (mSelectedItems.contains(selectionId)) {
                mSelectedItems.remove(selectionId);
                if (mSelectedItems.size() == 0) {
                    mActionMode.finish();

                    return;
                }
            } else {
                mSelectedItems.add(selectionId);
            }
            mBinding.recyclerView.getAdapter().notifyItemChanged(position);
            mActionMode.setTitle(getString(R.string.item_selected,
                    mSelectedItems.size()));
        }
    }
}
