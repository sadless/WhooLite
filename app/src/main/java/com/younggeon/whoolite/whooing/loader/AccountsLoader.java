package com.younggeon.whoolite.whooing.loader;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.Request;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.realm.Account;

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

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

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
                        Realm realm = Realm.getDefaultInstance();
                        ArrayList<Account> objects = new ArrayList<>();
                        RealmQuery<Account> query = realm.where(Account.class).equalTo("sectionId", sectionId);

                        while (keys.hasNext()) {
                            String accountType = keys.next();
                            JSONArray itemsInAccountType = accounts.optJSONArray(accountType);

                            for (int i = 0; i < itemsInAccountType.length(); i++, sortOrder++) {
                                JSONObject accountItem = itemsInAccountType.optJSONObject(i);
                                Account object = new Account();

                                object.setSectionId(sectionId);
                                object.setAccountType(accountType);
                                object.setAccountId(accountItem.optString(WhooingKeyValues.ACCOUNT_ID));
                                object.setTitle(accountItem.optString(WhooingKeyValues.TITLE));
                                object.setMemo(accountItem.optString(WhooingKeyValues.MEMO));
                                object.setGroup(accountItem.optString(WhooingKeyValues.TYPE).equals(GROUP));
                                object.setSortOrder(sortOrder);
                                object.composePrimaryKey();
                                query.notEqualTo("primaryKey", object.getPrimaryKey());
                                objects.add(object);
                            }
                        }

                        RealmResults<Account> willDeleteAccounts = query.findAll();

                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(objects);
                        willDeleteAccounts.deleteAllFromRealm();
                        realm.commitTransaction();
                        realm.close();
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
