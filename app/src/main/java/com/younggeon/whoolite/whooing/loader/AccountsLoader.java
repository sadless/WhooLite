package com.younggeon.whoolite.whooing.loader;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.volley.Request;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Accounts;
import com.younggeon.whoolite.provider.WhooingProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by sadless on 2016. 1. 27..
 */
public class AccountsLoader extends WhooingBaseLoader {
    private static final String URI_ACCOUNTS = "https://whooing.com/api/accounts";

    private static final String GROUP = "group";

    public AccountsLoader(Context context, int method, Bundle args) {
        super(context, method, args);
    }

    @Override
    public Integer loadInBackground() {
        switch (mMethod) {
            case Request.Method.GET: {
                String sectionId = args.getString(WhooingKeyValues.SECTION_ID);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                Uri.Builder builder = Uri.parse(URI_ACCOUNTS + ".json_array").buildUpon();

                builder.appendQueryParameter(WhooingKeyValues.SECTION_ID, sectionId)
                        .appendQueryParameter(WhooingKeyValues.START_DATE, dateFormat.format(new Date()));
                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                        builder.build().toString(),
                        mRequestFuture,
                        mRequestFuture,
                        mApiKeyFormat));

                int resultCode;

                try {
                    JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                    if ((resultCode = result.optInt(WhooingKeyValues.CODE)) == WhooingKeyValues.SUCCESS) {
                        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                        JSONObject accounts = result.optJSONObject(WhooingKeyValues.RESULT);
                        Iterator<String> keys = accounts.keys();
                        Uri accountsUri = WhooingProvider.getAccountsUri(sectionId);
                        int sortOrder = 0;

                        while (keys.hasNext()) {
                            String accountType = keys.next();
                            JSONArray itemsInAccountType = accounts.optJSONArray(accountType);
                            String ids = null;

                            for (int i = 0; i < itemsInAccountType.length(); i++, sortOrder++) {
                                JSONObject accountItem = itemsInAccountType.optJSONObject(i);
                                String accountId = accountItem.optString(WhooingKeyValues.ACCOUNT_ID);
                                Uri accountItemUri = WhooingProvider.getAccountItemUri(sectionId,
                                        accountType,
                                        accountId);
                                Cursor c = getContext().getContentResolver().query(
                                        accountItemUri,
                                        null,
                                        null,
                                        null,
                                        null);

                                if (c != null) {
                                    ContentValues cv = new ContentValues();
                                    String title = accountItem.optString(WhooingKeyValues.TITLE);
                                    String memo = accountItem.optString(WhooingKeyValues.MEMO);

                                    if (c.moveToFirst()) {
                                        if (!c.getString(Accounts.COLUMN_INDEX_TITLE).equals(title)) {
                                            cv.put(Accounts.COLUMN_TITLE, title);
                                        }
                                        if (!c.getString(Accounts.COLUMN_INDEX_MEMO).equals(memo)) {
                                            cv.put(Accounts.COLUMN_MEMO, memo);
                                        }
                                        if (c.getInt(Accounts.COLUMN_INDEX_SORT_ORDER) != sortOrder) {
                                            cv.put(Accounts.COLUMN_SORT_ORDER, sortOrder);
                                        }
                                        if (cv.size() > 0) {
                                            operations.add(ContentProviderOperation.newUpdate(accountItemUri)
                                                    .withValues(cv).build());
                                        }
                                    } else {
                                        cv.put(Accounts.COLUMN_ACCOUNT_TYPE, accountType);
                                        cv.put(Accounts.COLUMN_ACCOUNT_ID, accountId);
                                        cv.put(Accounts.COLUMN_TITLE, title);
                                        cv.put(Accounts.COLUMN_MEMO, memo);
                                        cv.put(Accounts.COLUMN_IS_GROUP,
                                                accountItem.optString(WhooingKeyValues.TYPE).equals(GROUP));
                                        cv.put(Accounts.COLUMN_SORT_ORDER, sortOrder);
                                        operations.add(ContentProviderOperation.newInsert(accountsUri)
                                                .withValues(cv).build());
                                    }
                                    c.close();
                                }
                                if (ids == null) {
                                    ids = "('" + accountId + "'";
                                } else {
                                    ids += ",'" + accountId + "'";
                                }
                            }
                            if (ids != null) {
                                ids += ")";
                                operations.add(ContentProviderOperation.newDelete(accountsUri)
                                        .withSelection(Accounts.COLUMN_SECTION_ID + " = ? AND " +
                                                        Accounts.COLUMN_ACCOUNT_TYPE + " = ? AND " +
                                                        Accounts.COLUMN_ACCOUNT_ID + " NOT IN " + ids,
                                                new String[]{sectionId, accountType})
                                        .build());
                            }
                        }
                        if (operations.size() > 0) {
                            getContext().getContentResolver().applyBatch(getContext().getString(R.string.whooing_authority),
                                    operations);
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException | JSONException |
                        RemoteException | OperationApplicationException e) {
                    e.printStackTrace();

                    resultCode = -1;
                }

                return resultCode;
            }
            default: {
                return null;
            }
        }
    }
}
