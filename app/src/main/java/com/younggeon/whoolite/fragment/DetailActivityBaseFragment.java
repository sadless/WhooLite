package com.younggeon.whoolite.fragment;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
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
import com.younggeon.whoolite.db.schema.Accounts;
import com.younggeon.whoolite.provider.WhooingProvider;

/**
 * Created by sadless on 2016. 1. 18..
 */
public abstract class DetailActivityBaseFragment extends Fragment implements LoaderManager.LoaderCallbacks {
    protected static final int LOADER_ID_LEFT = 101;
    protected static final int LOADER_ID_RIGHT = 102;
    protected static final int LOADER_ID_DELETE = 103;
    protected static final int LOADER_ID_SEND = 104;

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

    protected String mSectionId;
    protected String mLeftAccountType;
    protected String mLeftAccountId;
    protected String mRightAccountType;
    protected String mRightAccountId;
    protected String mItemId;
    protected int mLayoutId;

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
        getLoaderManager().initLoader(LOADER_ID_LEFT, null, this);
        getLoaderManager().initLoader(LOADER_ID_RIGHT, null, this);
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
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_LEFT: {
                return new CursorLoader(getActivity(),
                        WhooingProvider.getAccountsUri(mSectionId),
                        null,
                        Accounts.COLUMN_ACCOUNT_TYPE + " != ?",
                        new String[] {WhooingKeyValues.INCOME},
                        Accounts._ID + " ASC");
            }
            case LOADER_ID_RIGHT: {
                return new CursorLoader(getActivity(),
                        WhooingProvider.getAccountsUri(mSectionId),
                        null,
                        Accounts.COLUMN_ACCOUNT_TYPE + " != ?",
                        new String[] {WhooingKeyValues.EXPENSES},
                        Accounts._ID + " ASC");
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_LEFT: {
                AccountsAdapter adapter = (AccountsAdapter) mLeft.getAdapter();

                if (adapter == null) {
                    adapter = new AccountsAdapter((Cursor) data, AccountsAdapter.DIRECTION_LEFT);
                    mLeft.setAdapter(adapter);
                } else {
                    adapter.changeCursor((Cursor) data);
                }
                mLeft.setSelection(adapter.getSelection(mLeftAccountType, mLeftAccountId));
                break;
            }
            case LOADER_ID_RIGHT: {
                AccountsAdapter adapter = (AccountsAdapter) mRight.getAdapter();

                if (adapter == null) {
                    adapter = new AccountsAdapter((Cursor) data, AccountsAdapter.DIRECTION_RIGHT);
                    mRight.setAdapter(adapter);
                } else {
                    adapter.changeCursor((Cursor) data);
                }
                mRight.setSelection(adapter.getSelection(mRightAccountType, mRightAccountId));
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
            Cursor c = adapter.getCursor(position);

            switch (direction) {
                case AccountsAdapter.DIRECTION_LEFT: {
                    mLeftAccountType = c.getString(Accounts.COLUMN_INDEX_ACCOUNT_TYPE);
                    mLeftAccountId = c.getString(Accounts.COLUMN_INDEX_ACCOUNT_ID);
                    break;
                }
                case AccountsAdapter.DIRECTION_RIGHT: {
                    mRightAccountType = c.getString(Accounts.COLUMN_INDEX_ACCOUNT_TYPE);
                    mRightAccountId = c.getString(Accounts.COLUMN_INDEX_ACCOUNT_ID);
                    break;
                }
                default:
            }
        }
    }

    protected class AccountsAdapter extends BaseAdapter {
        public static final int DIRECTION_LEFT = 1;
        public static final int DIRECTION_RIGHT = 2;

        private Cursor mCursor;
        private int mDirection;
        private int mItemPadding;
        private int mTypePadding;
        private String[] mTypes;
        private int[] mTypeCounts;

        public AccountsAdapter(Cursor cursor, int direction) {
            this.mDirection = direction;
            setCursor(cursor);
            mItemPadding = getResources().getDimensionPixelSize(R.dimen.account_item_padding);
            mTypePadding = getResources().getDimensionPixelSize(R.dimen.account_type_padding);
        }

        @Override
        public int getCount() {
            if (mCursor != null) {
                return mCursor.getCount() + mTypes.length + 2;
            } else {
                return 2;
            }
        }

        @Override
        public Cursor getItem(int position) {
            mCursor.moveToPosition(position);

            return mCursor;
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

                    Cursor c = getItem(getCursorPosition(position));
                    String title = c.getString(Accounts.COLUMN_INDEX_TITLE);

                    tv.setText(addSign(title, c.getString(Accounts.COLUMN_INDEX_ACCOUNT_TYPE)));
                    break;
                }
            }

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item_with_memo,
                        parent,
                        false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.text1),
                        (TextView) convertView.findViewById(R.id.text2),
                        convertView.getBackground()));
            }

            ViewHolder vh = (ViewHolder) convertView.getTag();

            switch (position) {
                case 0: {
                    convertView.setPadding(mItemPadding, mItemPadding, mItemPadding, mItemPadding);
                    if (Build.VERSION.SDK_INT >= 16) {
                        convertView.setBackground(vh.background);
                    } else {
                        convertView.setBackgroundDrawable(vh.background);
                    }
                    vh.main.setText(R.string.not_assigned);
                    vh.sub.setVisibility(View.GONE);
                    break;
                }
                default: {
                    if (position == getCount() - 1) {
                        convertView.setPadding(mItemPadding, mItemPadding, mItemPadding, mItemPadding);
                        if (Build.VERSION.SDK_INT >= 16) {
                            convertView.setBackground(vh.background);
                        } else {
                            convertView.setBackgroundDrawable(vh.background);
                        }
                        vh.main.setText(R.string.unknown);
                        vh.sub.setVisibility(View.GONE);
                        break;
                    }
                    position--;

                    int sum = 0;
                    boolean bound = false;

                    for (int i = 0; i < mTypeCounts.length; i++) {
                        if (position == sum + i) {
                            convertView.setPadding(mTypePadding, mTypePadding, mTypePadding, mTypePadding);
                            convertView.setBackgroundResource(R.color.primary);

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
                            vh.main.setText(title);
                            vh.sub.setVisibility(View.GONE);
                            bound = true;
                            break;
                        }
                        sum += mTypeCounts[i];
                    }
                    if (!bound) {
                        Cursor c = getItem(getCursorPosition(position));
                        String title = c.getString(Accounts.COLUMN_INDEX_TITLE);

                        if (c.getInt(Accounts.COLUMN_INDEX_IS_GROUP) == 1) {
                            convertView.setPadding(mTypePadding, mTypePadding, mTypePadding, mTypePadding);
                            convertView.setBackgroundResource(R.color.primary_dark);
                        } else {
                            convertView.setPadding(mItemPadding, mItemPadding, mItemPadding, mItemPadding);
                            if (Build.VERSION.SDK_INT >= 16) {
                                convertView.setBackground(vh.background);
                            } else {
                                convertView.setBackgroundDrawable(vh.background);
                            }
                            title = addSign(title, c.getString(Accounts.COLUMN_INDEX_ACCOUNT_TYPE));
                        }

                        String memo = c.getString(Accounts.COLUMN_INDEX_MEMO);

                        vh.main.setText(title);
                        if (TextUtils.isEmpty(memo)) {
                            vh.sub.setVisibility(View.GONE);
                        } else {
                            vh.sub.setText(c.getString(Accounts.COLUMN_INDEX_MEMO));
                            vh.sub.setVisibility(View.VISIBLE);
                        }
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

                    return getItem(getCursorPosition(position)).getInt(Accounts.COLUMN_INDEX_IS_GROUP) != 1;
                }
            }
        }

        public void changeCursor(Cursor cursor) {
            setCursor(cursor);
            notifyDataSetChanged();
        }

        public int getSelection(String accountType, String accountId) {
            if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountId)) {
                return 0;
            }

            Cursor c = getItem(0);

            do {
                if (c.getString(Accounts.COLUMN_INDEX_ACCOUNT_TYPE).equals(accountType) &&
                        c.getString(Accounts.COLUMN_INDEX_ACCOUNT_ID).equals(accountId)) {
                    int selection = c.getPosition() + 2;

                    for (String type : mTypes) {
                        if (type.equals(accountType)) {
                            return selection;
                        } else {
                            selection++;
                        }
                    }
                }
            } while (c.moveToNext());

            return getCount() - 1;
        }

        public Cursor getCursor(int position) {
            return getItem(getCursorPosition(position - 1));
        }

        private void setCursor(Cursor cursor) {
            this.mCursor = cursor;

            Cursor c;

            switch (mDirection) {
                case DIRECTION_LEFT: {
                    c = getActivity().getContentResolver().query(WhooingProvider.getAccountsTypeCountsUri(mSectionId),
                            null,
                            Accounts.COLUMN_ACCOUNT_TYPE + " != ?",
                            new String[] {WhooingKeyValues.INCOME},
                            null);
                    break;
                }
                case DIRECTION_RIGHT: {
                    c = getActivity().getContentResolver().query(WhooingProvider.getAccountsTypeCountsUri(mSectionId),
                            null,
                            Accounts.COLUMN_ACCOUNT_TYPE + " != ?",
                            new String[] {WhooingKeyValues.EXPENSES},
                            null);
                    break;
                }
                default: {
                    c = null;
                    break;
                }
            }

            if (c != null) {
                if (c.moveToFirst()) {
                    int i = 0;

                    mTypes = new String[c.getCount()];
                    mTypeCounts = new int[c.getCount()];
                    do {
                        mTypes[i] = c.getString(0);
                        mTypeCounts[i] = c.getInt(1);
                        i++;
                    } while (c.moveToNext());
                }
                c.close();
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

        private class ViewHolder {
            public TextView main;
            public TextView sub;
            public Drawable background;

            public ViewHolder(TextView main, TextView sub, Drawable background) {
                this.main = main;
                this.sub = sub;
                this.background = background;
            }
        }
    }
}
