package com.younggeon.whoolite.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.activity.SelectMergingEntryActivity;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.databinding.SpinnerItemAccountBinding;
import com.younggeon.whoolite.realm.Account;
import com.younggeon.whoolite.realm.Entry;
import com.younggeon.whoolite.util.Utility;
import com.younggeon.whoolite.whooing.loader.EntriesLoader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by sadless on 2016. 1. 18..
 */
public abstract class DetailActivityBaseFragment extends Fragment implements LoaderManager.LoaderCallbacks {
    protected static final int LOADER_ID_DELETE = 101;
    private static final int LOADER_ID_SEND = 102;
    private static final int LOADER_ID_EDIT_SEND = 103;

    private static final String INSTANCE_STATE_LEFT_ACCOUNT_TYPE = "left_account_type";
    private static final String INSTANCE_STATE_LEFT_ACCOUNT_ID = "left_account_id";
    private static final String INSTANCE_STATE_RIGHT_ACCOUNT_TYPE = "right_account_type";
    private static final String INSTANCE_STATE_RIGHT_ACCOUNT_ID = "right_account_id";
    private static final String INSTANCE_STATE_PROGRESSING = "progressing";
    private static final String INSTANCE_STATE_SEND_MERGE_DIALOG = "send_merge_dialog";
    private static final String INSTANCE_STATE_EDIT_SEND_MERGE_DIALOG = "edit_send_merge_dialog";
    private static final String INSTANCE_STATE_SEND_ARGUMENTS = "send_arguments";
    private static final String INSTANCE_STATE_MERGING_ITEM_SPECIFIED = "merging_item_specified";
    private static final String INSTANCE_STATE_ADDING_MONEY = "adding_money";
    private static final String INSTANCE_STATE_ENTRY_ID_FOR_MERGING = "entry_id_for_merging";

    private static final int REQUEST_CODE_MERGE_FOR_SEND = 1;
    private static final int REQUEST_CODE_MERGE_FOR_EDIT_SEND = 2;

    protected EditText mTitle;
    protected EditText mMoney;
    protected Spinner mLeft;
    protected Spinner mRight;
    protected ProgressDialog mProgress;
    protected EditText mMemo;

    protected String mSectionId;
    protected String mLeftAccountType;
    protected String mLeftAccountId;
    protected String mRightAccountType;
    protected String mRightAccountId;
    protected int mLayoutId;
    protected SimpleDateFormat mEntryDateFormat;

    private Realm mRealm;
    private RealmQuery<Account> mLeftQuery;
    private RealmQuery<Account> mRightQuery;
    private RealmResults<Account> mLeftAccounts;
    private RealmResults<Account> mRightAccounts;
    private AlertDialog mSendMergeAlertDialog;
    private AlertDialog mEditSendMergeAlertDialog;
    private Bundle mSendArguments;
    private boolean mMergingItemSpecified;
    private double mAddingMoney;
    private long mEntryIdForMerging = -1;

    abstract protected void initialize(View view);

    private AdapterView.OnItemSelectedListener mListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            onAccountSelected((AccountsAdapter) parent.getAdapter(), (Integer) parent.getTag(), position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(mLayoutId, container, false);

        mTitle = (EditText) view.findViewById(R.id.title);
        mMoney = (EditText) view.findViewById(R.id.money);
        mLeft = (Spinner) view.findViewById(R.id.left);
        mLeft.setTag(AccountsAdapter.DIRECTION_LEFT);
        mLeft.setOnItemSelectedListener(mListener);
        mRight = (Spinner) view.findViewById(R.id.right);
        mRight.setTag(AccountsAdapter.DIRECTION_RIGHT);
        mRight.setOnItemSelectedListener(mListener);
        mMemo = (EditText) view.findViewById(R.id.memo);
        mSectionId = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(PreferenceKeys.CURRENT_SECTION_ID, null);
        mEntryDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        if (savedInstanceState == null) {
            initialize(view);
        } else {
            mLeftAccountType = savedInstanceState.getString(INSTANCE_STATE_LEFT_ACCOUNT_TYPE);
            mLeftAccountId = savedInstanceState.getString(INSTANCE_STATE_LEFT_ACCOUNT_ID);
            mRightAccountType = savedInstanceState.getString(INSTANCE_STATE_RIGHT_ACCOUNT_TYPE);
            mRightAccountId = savedInstanceState.getString(INSTANCE_STATE_RIGHT_ACCOUNT_ID);
            mSendArguments = savedInstanceState.getBundle(INSTANCE_STATE_SEND_ARGUMENTS);
            mAddingMoney = savedInstanceState.getDouble(INSTANCE_STATE_ADDING_MONEY);
            mMergingItemSpecified = savedInstanceState.getBoolean(INSTANCE_STATE_MERGING_ITEM_SPECIFIED);
            mEntryIdForMerging = savedInstanceState.getLong(INSTANCE_STATE_ENTRY_ID_FOR_MERGING);
            if (savedInstanceState.getBoolean(INSTANCE_STATE_PROGRESSING, false)) {
                mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
            } else if (savedInstanceState.getBoolean(INSTANCE_STATE_SEND_MERGE_DIALOG)) {
                showSendMergeAlertDialog();
            } else if (savedInstanceState.getBoolean(INSTANCE_STATE_EDIT_SEND_MERGE_DIALOG)) {
                showEditSendMergeAlertDialog();
            }
        }

        mRealm = Realm.getDefaultInstance();
        mLeftQuery = mRealm.where(Account.class).equalTo("sectionId", mSectionId)
                .notEqualTo("accountType", WhooingKeyValues.INCOME);
        mLeftAccounts = mLeftQuery.findAllSorted("sortOrder", Sort.ASCENDING);
        mLeftAccounts.addChangeListener(new RealmChangeListener<RealmResults<Account>>() {
            @Override
            public void onChange(RealmResults<Account> element) {
                leftChanged();
            }
        });
        leftChanged();
        mRightQuery = mRealm.where(Account.class).equalTo("sectionId", mSectionId)
                .notEqualTo("accountType", WhooingKeyValues.EXPENSES);
        mRightAccounts = mRightQuery.findAllSorted("sortOrder", Sort.ASCENDING);
        mRightAccounts.addChangeListener(new RealmChangeListener<RealmResults<Account>>() {
            @Override
            public void onChange(RealmResults<Account> element) {
                rightChanged();
            }
        });
        rightChanged();
        getLoaderManager().initLoader(LOADER_ID_DELETE, null, this);
        getLoaderManager().initLoader(LOADER_ID_SEND, null, this);
        getLoaderManager().initLoader(LOADER_ID_EDIT_SEND, null, this);
        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(INSTANCE_STATE_LEFT_ACCOUNT_TYPE, mLeftAccountType);
        outState.putString(INSTANCE_STATE_LEFT_ACCOUNT_ID, mLeftAccountId);
        outState.putString(INSTANCE_STATE_RIGHT_ACCOUNT_TYPE, mRightAccountType);
        outState.putString(INSTANCE_STATE_RIGHT_ACCOUNT_ID, mRightAccountId);
        if (mProgress != null) {
            outState.putBoolean(INSTANCE_STATE_PROGRESSING, mProgress.isShowing());
        }
        if (mSendMergeAlertDialog != null) {
            outState.putBoolean(INSTANCE_STATE_SEND_MERGE_DIALOG, mSendMergeAlertDialog.isShowing());
        }
        if (mEditSendMergeAlertDialog != null) {
            outState.putBoolean(INSTANCE_STATE_EDIT_SEND_MERGE_DIALOG, mEditSendMergeAlertDialog.isShowing());
        }
        outState.putDouble(INSTANCE_STATE_ADDING_MONEY, mAddingMoney);
        outState.putBundle(INSTANCE_STATE_SEND_ARGUMENTS, mSendArguments);
        outState.putBoolean(INSTANCE_STATE_MERGING_ITEM_SPECIFIED, mMergingItemSpecified);
        outState.putLong(INSTANCE_STATE_ENTRY_ID_FOR_MERGING, mEntryIdForMerging);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLeftAccounts.removeChangeListeners();
        mRightAccounts.removeChangeListeners();
        mRealm.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_MERGE_FOR_SEND: {
                    getActivity().finish();
                    break;
                }
                case REQUEST_CODE_MERGE_FOR_EDIT_SEND: {
                    if (mEntryIdForMerging >= 0) {
                        mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
                        startDeleteEntryLoader(mEntryIdForMerging);
                    } else {
                        getActivity().finish();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
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
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
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
                                                    DetailActivityBaseFragment.this));

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
                int result = (Integer) data;

                if (result < 0) {
                    if (mProgress != null) {
                        mProgress.dismiss();
                    }

                    final EntriesLoader editSendLoader = (EntriesLoader) loader;

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.edit_failed)
                            .setMessage(R.string.edit_entry_failed)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    editSend(editSendLoader.args, false);
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else if (Utility.checkResultCodeWithAlert(getActivity(), result)) {
                    if (mEntryIdForMerging < 0) {
                        if (mProgress != null) {
                            mProgress.dismiss();
                        }
                        Toast.makeText(getActivity(), R.string.edit_entry_success, Toast.LENGTH_LONG).show();
                        getActivity().finish();
                    } else {
                        startDeleteEntryLoader(mEntryIdForMerging);
                    }
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
                                    startDeleteEntryLoader(finalLoader.args.getLong(WhooingKeyValues.ENTRY_ID));
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else if (Utility.checkResultCodeWithAlert(getActivity(), result)) {
                    getActivity().finish();
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    protected void onAccountSelected(AccountsAdapter adapter, int direction, int position) {
        if (position == 0) {
            switch (direction) {
                case AccountsAdapter.DIRECTION_LEFT: {
                    mLeftAccountType = "";
                    mLeftAccountId = "";
                    break;
                }
                case AccountsAdapter.DIRECTION_RIGHT: {
                    mRightAccountType = "";
                    mRightAccountId = "";
                    break;
                }
                default:
            }

            return;
        }
        if (position != adapter.getCount() - 1) {
            Account account = adapter.getAccount(position);

            switch (direction) {
                case AccountsAdapter.DIRECTION_LEFT: {
                    mLeftAccountType = account.getAccountType();
                    mLeftAccountId = account.getAccountId();
                    break;
                }
                case AccountsAdapter.DIRECTION_RIGHT: {
                    mRightAccountType = account.getAccountType();
                    mRightAccountId = account.getAccountId();
                    break;
                }
                default:
            }
        }
    }

    protected void leftChanged() {
        AccountsAdapter adapter = (AccountsAdapter) mLeft.getAdapter();

        if (adapter == null) {
            adapter = new AccountsAdapter(AccountsAdapter.DIRECTION_LEFT);
            mLeft.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        mLeft.setSelection(adapter.getSelection(mLeftAccountType, mLeftAccountId));
    }

    protected void rightChanged() {
        AccountsAdapter adapter = (AccountsAdapter) mRight.getAdapter();

        if (adapter == null) {
            adapter = new AccountsAdapter(AccountsAdapter.DIRECTION_RIGHT);
            mRight.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        mRight.setSelection(adapter.getSelection(mRightAccountType, mRightAccountId));
    }

    protected void send(final Bundle args) {
        if (args.getString(WhooingKeyValues.ENTRY_DATE) == null) {
            args.putString(WhooingKeyValues.ENTRY_DATE, mEntryDateFormat.format(new Date()));
        }

        RealmResults<Entry> prevEntries = Utility.getDuplicateEntries(mRealm, args);

        if (prevEntries.size() == 0) {
            mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

            EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().restartLoader(LOADER_ID_SEND, null, this));

            loader.args = args;
            loader.forceLoad();
        } else {
            if (mMergingItemSpecified = prevEntries.size() == 1) {
                Entry entry = prevEntries.first();

                mAddingMoney = entry.getMoney();
                args.putLong(WhooingKeyValues.ENTRY_ID, entry.getEntryId());
            }
            mSendArguments = args;
            showSendMergeAlertDialog();
        }
    }

    protected void editSend(Bundle args, boolean needDuplicateCheck) {
        if (needDuplicateCheck) {
            RealmResults<Entry> prevEntries = Utility.getDuplicateEntries(mRealm, args);

            if (prevEntries.size() > 0) {
                if (mMergingItemSpecified = prevEntries.size() == 1) {
                    Entry entry = prevEntries.first();

                    mAddingMoney = entry.getMoney();
                    mEntryIdForMerging = entry.getEntryId();
                }
                mSendArguments = args;
                showEditSendMergeAlertDialog();

                return;
            }
        }
        mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

        EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().restartLoader(LOADER_ID_EDIT_SEND, null, this));

        loader.args = args;
        loader.forceLoad();
    }

    protected void startDeleteEntryLoader(long entryId) {
        EntriesLoader loader = EntriesLoader.castLoader(
                getLoaderManager().restartLoader(LOADER_ID_DELETE, null, this));
        Bundle args = new Bundle();

        args.putString(WhooingKeyValues.SECTION_ID, mSectionId);
        args.putLong(WhooingKeyValues.ENTRY_ID, entryId);
        loader.args = args;
        loader.forceLoad();
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
                            editSend(mSendArguments, false);
                        } else {
                            Intent intent = new Intent(getContext(), SelectMergingEntryActivity.class);

                            intent.putExtra(SelectMergingEntryActivity.EXTRA_MERGE_ARGUMENTS, mSendArguments);
                            startActivityForResult(intent, REQUEST_CODE_MERGE_FOR_SEND);
                        }
                    }
                }).setNegativeButton(R.string.new_entry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
                        mSendArguments.remove(WhooingKeyValues.ENTRY_ID);

                        EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().restartLoader(LOADER_ID_SEND, null, DetailActivityBaseFragment.this));

                        loader.args = mSendArguments;
                        loader.forceLoad();
                    }
                }).create();
        mSendMergeAlertDialog.show();
    }

    private void showEditSendMergeAlertDialog() {
        mEditSendMergeAlertDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.merge)
                .setMessage(R.string.merge_confirm)
                .setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mMergingItemSpecified) {
                            double money = Double.parseDouble(mSendArguments.getString(WhooingKeyValues.MONEY));
                            long entryId = mSendArguments.getLong(WhooingKeyValues.ENTRY_ID);

                            mSendArguments.putString(WhooingKeyValues.MONEY, "" + (money + mAddingMoney));
                            mSendArguments.putLong(WhooingKeyValues.ENTRY_ID, mEntryIdForMerging);
                            mEntryIdForMerging = entryId;
                            editSend(mSendArguments, false);
                        } else {
                            Intent intent = new Intent(getContext(), SelectMergingEntryActivity.class);

                            mEntryIdForMerging = mSendArguments.getLong(WhooingKeyValues.ENTRY_ID);
                            intent.putExtra(SelectMergingEntryActivity.EXTRA_MERGE_ARGUMENTS, mSendArguments);
                            startActivityForResult(intent, REQUEST_CODE_MERGE_FOR_EDIT_SEND);
                        }
                    }
                }).setNegativeButton(R.string.edit_this_entry_only, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));

                        EntriesLoader loader = EntriesLoader.castLoader(getLoaderManager().restartLoader(LOADER_ID_EDIT_SEND, null, DetailActivityBaseFragment.this));

                        loader.args = mSendArguments;
                        loader.forceLoad();
                    }
                }).create();
        mEditSendMergeAlertDialog.show();
    }

    protected class AccountsAdapter extends BaseAdapter {
        static final int DIRECTION_LEFT = 1;
        static final int DIRECTION_RIGHT = 2;

        private int mDirection;
        private String[] mTypes;
        private int[] mTypeCounts;

        AccountsAdapter(int direction) {
            this.mDirection = direction;
            refresh();
        }

        @Override
        public int getCount() {
            switch (mDirection) {
                case DIRECTION_LEFT: {
                    return mLeftAccounts.size() + mTypes.length + 1;
                }
                case DIRECTION_RIGHT: {
                    return mRightAccounts.size() + mTypes.length + 1;
                }
                default:
                    return -1;
            }
        }

        @Override
        public Account getItem(int position) {
            switch (mDirection) {
                case DIRECTION_LEFT: {
                    return mLeftAccounts.get(position);
                }
                case DIRECTION_RIGHT: {
                    return mRightAccounts.get(position);
                }
                default: {
                    return null;
                }
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1,
                        parent, false);
                convertView.setTag(convertView.findViewById(android.R.id.text1));
            }

            TextView tv = (TextView) convertView.getTag();

            switch (position) {
                case 0: {
                    tv.setText(R.string.not_assigned);
                    break;
                }
                default: {
                    position--;

                    Account account = getItem(getCursorPosition(position));
                    String title = account.getTitle();

                    tv.setText(addSign(title, account.getAccountType()));
                    break;
                }
            }

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                SpinnerItemAccountBinding binding = DataBindingUtil.bind(LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item_account,
                        parent,
                        false));

                convertView = binding.getRoot();
                binding.setDefaultBackground(convertView.getBackground());
                convertView.setTag(binding);
            }

            SpinnerItemAccountBinding binding = (SpinnerItemAccountBinding) convertView.getTag();

            switch (position) {
                case 0: {
                    binding.setSelectable(true);
                    binding.setTitle(getString(R.string.not_assigned));
                    binding.setMemo(null);
                    break;
                }
                default: {
                    position--;

                    int sum = 0;
                    boolean bound = false;

                    for (int i = 0; i < mTypeCounts.length; i++) {
                        if (position == sum + i) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                binding.setColorBackground(new ColorDrawable(getResources().getColor(R.color.primary, null)));
                            } else {
                                binding.setColorBackground(new ColorDrawable(getResources().getColor(R.color.primary)));
                            }
                            binding.setSelectable(false);

                            String title = null;

                            switch (mTypes[i]) {
                                case WhooingKeyValues.ASSETS: {
                                    title = getString(R.string.assets);
                                    switch (mDirection) {
                                        case DIRECTION_LEFT: {
                                            title += "+";
                                            break;
                                        }
                                        case DIRECTION_RIGHT: {
                                            title += "-";
                                            break;
                                        }
                                        default:
                                    }
                                    break;
                                }
                                case WhooingKeyValues.LIABILITIES: {
                                    title = getString(R.string.liabilities);
                                    switch (mDirection) {
                                        case DIRECTION_LEFT: {
                                            title += "-";
                                            break;
                                        }
                                        case DIRECTION_RIGHT: {
                                            title += "+";
                                            break;
                                        }
                                        default:
                                    }
                                    break;
                                }
                                case WhooingKeyValues.CAPITAL: {
                                    title = getString(R.string.capital);
                                    switch (mDirection) {
                                        case DIRECTION_LEFT: {
                                            title += "-";
                                            break;
                                        }
                                        case DIRECTION_RIGHT: {
                                            title += "+";
                                            break;
                                        }
                                        default:
                                    }
                                    break;
                                }
                                case WhooingKeyValues.EXPENSES: {
                                    title = getString(R.string.expenses);
                                    break;
                                }
                                case WhooingKeyValues.INCOME: {
                                    title = getString(R.string.income);
                                    break;
                                }
                                default:
                            }
                            binding.setTitle(title);
                            binding.setMemo(null);
                            bound = true;
                            break;
                        }
                        sum += mTypeCounts[i];
                    }
                    if (!bound) {
                        Account account = getItem(getCursorPosition(position));
                        String title = account.getTitle();

                        if (Build.VERSION.SDK_INT >= 23) {
                            binding.setColorBackground(new ColorDrawable(getResources().getColor(R.color.primary_dark, null)));
                        } else {
                            binding.setColorBackground(new ColorDrawable(getResources().getColor(R.color.primary_dark)));
                        }
                        binding.setSelectable(!account.isGroup());
                        if (!account.isGroup()) {
                            title = addSign(title, account.getAccountType());
                        }
                        binding.setTitle(title);
                        binding.setMemo(account.getMemo());
                    }
                    break;
                }
            }

            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            switch (position) {
                case 0: {
                    return true;
                }
                default: {
                    position--;

                    int sum = 0;

                    for (int i = 0; i < mTypeCounts.length; i++) {
                        if (position == sum + i) {
                            return false;
                        }
                        sum += mTypeCounts[i];
                    }

                    return !getItem(getCursorPosition(position)).isGroup();
                }
            }
        }

        @Override
        public void notifyDataSetChanged() {
            refresh();
            super.notifyDataSetChanged();
        }

        public int getSelection(String accountType, String accountId) {
            if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountId)) {
                return 0;
            }

            RealmResults<Account> accounts;

            switch (mDirection) {
                case DIRECTION_LEFT: {
                    accounts = mLeftAccounts;
                    break;
                }
                case DIRECTION_RIGHT: {
                    accounts = mRightAccounts;
                    break;
                }
                default: {
                    accounts = null;
                    break;
                }
            }
            if (accounts != null) {
                int position = 0;

                for (Account account : accounts) {
                    if (account.getAccountType().equals(accountType) &&
                            account.getAccountId().equals(accountId)) {
                        int selection = position + 2;

                        for (String type : mTypes) {
                            if (type.equals(accountType)) {
                                return selection;
                            } else {
                                selection++;
                            }
                        }
                    }
                    position++;
                }
            }

            return getCount() - 1;
        }

        public Account getAccount(int position) {
            return getItem(getCursorPosition(position - 1));
        }

        private void refresh() {
            RealmQuery<Account> query;
            RealmResults<Account> accounts = null;

            switch (mDirection) {
                case DIRECTION_LEFT: {
                    query = mLeftQuery;
                    break;
                }
                case DIRECTION_RIGHT: {
                    query = mRightQuery;
                    break;
                }
                default: {
                    query = null;
                    break;
                }
            }
            if (query != null) {
                accounts = query.findAllSorted("sortOrder", Sort.ASCENDING);
            }

            if (accounts != null) {
                RealmResults<Account> accountTypes = accounts.distinct("accountType");

                mTypes = new String[accountTypes.size()];
                mTypeCounts = new int[accountTypes.size()];

                int i = 0;

                for (Account account : accountTypes) {
                    mTypes[i] = account.getAccountType();
                    mTypeCounts[i] = query.findAll().where().equalTo("accountType", account.getAccountType()).findAll().size();
                    i++;
                }
            }
        }

        private int getCursorPosition(int position) {
            int sum = 0;

            for (int i : mTypeCounts) {
                if (position > sum) {
                    position--;
                } else {
                    break;
                }
                sum += i;
            }

            return position;
        }

        private String addSign(String title, String accountType) {
            switch (accountType) {
                case WhooingKeyValues.ASSETS: {
                    switch (mDirection) {
                        case DIRECTION_LEFT: {
                            title += "+";
                            break;
                        }
                        case DIRECTION_RIGHT: {
                            title += "-";
                            break;
                        }
                        default:
                    }
                    break;
                }
                case WhooingKeyValues.LIABILITIES:
                case WhooingKeyValues.CAPITAL: {
                    switch (mDirection) {
                        case DIRECTION_LEFT: {
                            title += "-";
                            break;
                        }
                        case DIRECTION_RIGHT: {
                            title += "+";
                            break;
                        }
                        default:
                    }
                    break;
                }
                default:
            }

            return title;
        }
    }
}
