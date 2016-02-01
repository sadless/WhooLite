package com.younggeon.whoolite.whooing.loader;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.Request;
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
                        JSONObject accounts = result.optJSONObject(WhooingKeyValues.RESULT);
                        Iterator<String> keys = accounts.keys();
                        int sortOrder = 0;
                        ArrayList<ContentValues> values = new ArrayList<>();

                        while (keys.hasNext()) {
                            String accountType = keys.next();
                            JSONArray itemsInAccountType = accounts.optJSONArray(accountType);

                            for (int i = 0; i < itemsInAccountType.length(); i++, sortOrder++) {
                                JSONObject accountItem = itemsInAccountType.optJSONObject(i);
                                ContentValues cv = new ContentValues();

                                cv.put(Accounts.COLUMN_ACCOUNT_TYPE, accountType);
                                cv.put(Accounts.COLUMN_ACCOUNT_ID, accountItem.optString(WhooingKeyValues.ACCOUNT_ID));
                                cv.put(Accounts.COLUMN_TITLE, accountItem.optString(WhooingKeyValues.TITLE));
                                cv.put(Accounts.COLUMN_MEMO, accountItem.optString(WhooingKeyValues.MEMO));
                                cv.put(Accounts.COLUMN_IS_GROUP, accountItem.optString(WhooingKeyValues.TYPE).equals(GROUP));
                                cv.put(Accounts.COLUMN_SORT_ORDER, sortOrder);
                                values.add(cv);
                                sortOrder++;
                            }
                        }

                        ContentValues[] valuesArray = new ContentValues[values.size()];

                        getContext().getContentResolver().bulkInsert(WhooingProvider.getAccountsUri(sectionId),
                                values.toArray(valuesArray));
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
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
