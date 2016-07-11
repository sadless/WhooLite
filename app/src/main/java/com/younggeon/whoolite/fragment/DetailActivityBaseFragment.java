package com.younggeon.whoolite.fragment;

import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.younggeon.whoolite.R;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.databinding.SpinnerItemAccountBinding;
import com.younggeon.whoolite.realm.Account;

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
    protected static final int LOADER_ID_SEND = 102;

    private static final String INSTANCE_STATE_LEFT_ACCOUNT_TYPE = "left_account_type";
    private static final String INSTANCE_STATE_LEFT_ACCOUNT_ID = "left_account_id";
    private static final String INSTANCE_STATE_RIGHT_ACCOUNT_TYPE = "right_account_type";
    private static final String INSTANCE_STATE_RIGHT_ACCOUNT_ID = "right_account_id";
    private static final String INSTANCE_STATE_PROGRESSING = "progressing";

    protected EditText mTitle;
    protected EditText mMoney;
    protected Spinner mLeft;
    protected Spinner mRight;
    protected ProgressDialog mProgress;
    protected EditText mMemo;
    protected EditText mSearchKeyword;

    protected String mSectionId;
    protected String mLeftAccountType;
    protected String mLeftAccountId;
    protected String mRightAccountType;
    protected String mRightAccountId;
    protected String mItemId;
    protected int mLayoutId;

    private Realm mRealm;
    private RealmQuery<Account> mLeftQuery;
    private RealmQuery<Account> mRightQuery;
    private RealmResults<Account> mLeftAccounts;
    private RealmResults<Account> mRightAccounts;

    abstract protected void initialize();

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
        mSearchKeyword = (EditText) view.findViewById(R.id.search_keyword);
        mSectionId = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(PreferenceKeys.CURRENT_SECTION_ID, null);
        if (savedInstanceState == null) {
            initialize();
        } else {
            mLeftAccountType = savedInstanceState.getString(INSTANCE_STATE_LEFT_ACCOUNT_TYPE);
            mLeftAccountId = savedInstanceState.getString(INSTANCE_STATE_LEFT_ACCOUNT_ID);
            mRightAccountType = savedInstanceState.getString(INSTANCE_STATE_RIGHT_ACCOUNT_TYPE);
            mRightAccountId = savedInstanceState.getString(INSTANCE_STATE_RIGHT_ACCOUNT_ID);
            if (savedInstanceState.getBoolean(INSTANCE_STATE_PROGRESSING, false)) {
                mProgress = ProgressDialog.show(getActivity(), null, getString(R.string.please_wait));
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLeftAccounts.removeChangeListeners();
        mRightAccounts.removeChangeListeners();
        mRealm.close();
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

    protected class AccountsAdapter extends BaseAdapter {
        public static final int DIRECTION_LEFT = 1;
        public static final int DIRECTION_RIGHT = 2;

        private int mDirection;
        private String[] mTypes;
        private int[] mTypeCounts;

        public AccountsAdapter(int direction) {
            this.mDirection = direction;
            refresh();
        }

        @Override
        public int getCount() {
            switch (mDirection) {
                case DIRECTION_LEFT: {
                    return mLeftAccounts.size() + mTypes.length + 2;
                }
                case DIRECTION_RIGHT: {
                    return mRightAccounts.size() + mTypes.length + 2;
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
                    if (position == getCount() - 1) {
                        tv.setText(R.string.unknown);
                        break;
                    }
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
                    if (position == getCount() - 1) {
                        binding.setSelectable(true);
                        binding.setTitle(getString(R.string.unknown));
                        binding.setMemo(null);
                        break;
                    }
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
                    if (position == getCount() - 1) {
                        return true;
                    }
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
