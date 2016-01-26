package com.younggeon.whoolite.whooing.loader;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.R;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.provider.WhooingProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by sadless on 2016. 1. 26..
 */
public class SectionsLoader extends WhooingBaseLoader {
    private static final String URI_SECTIONS = "https://whooing.com/api/sections";

    public SectionsLoader(Context context, int method, Bundle args) {
        super(context, method, args);
    }

    @Override
    public Integer loadInBackground() {
        switch (mMethod) {
            case Request.Method.GET: {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

                if (prefs.getString(PreferenceKeys.CURRENT_SECTION_ID, null) == null) {
                    Uri.Builder builder = Uri.parse(URI_SECTIONS).buildUpon();

                    builder.appendPath("default.json");
                    WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                            builder.build().toString(),
                            mRequestFuture,
                            mRequestFuture,
                            mApiKeyFormat));

                    try {
                        JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                        if (result.optInt(WhooingKeyValues.CODE) == WhooingKeyValues.SUCCESS) {
                            JSONObject resultItem = result.optJSONObject(WhooingKeyValues.RESULT);

                            prefs.edit()
                                    .putString(PreferenceKeys.CURRENT_SECTION_ID,
                                            resultItem.optString(WhooingKeyValues.SECTION_ID)).apply();
                        } else {
                            return -1;
                        }
                    } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();

                        return -1;
                    }
                    mRequestFuture = RequestFuture.newFuture();
                }
                WhooLiteNetwork.requestQueue.add(new WhooLiteNetwork.WhooingRequest(mMethod,
                        URI_SECTIONS + ".json_array",
                        mRequestFuture,
                        mRequestFuture,
                        mApiKeyFormat));

                int resultCode;

                try {
                    JSONObject result = new JSONObject(mRequestFuture.get(10, TimeUnit.SECONDS));

                    if ((resultCode = result.optInt(WhooingKeyValues.CODE)) == WhooingKeyValues.SUCCESS) {
                        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                        JSONArray sections = result.optJSONArray(WhooingKeyValues.RESULT);
                        String ids = null;
                        Uri sectionsUri = WhooingProvider.getSectionsUri();

                        for (int i = 0; i < sections.length(); i++) {
                            JSONObject section = sections.optJSONObject(i);
                            String sectionId = section.optString(WhooingKeyValues.SECTION_ID);
                            Uri sectionItemUri = WhooingProvider.getSectionUri(sectionId);
                            Cursor c = getContext().getContentResolver().query(
                                    sectionItemUri,
                                    null,
                                    null,
                                    null,
                                    null);

                            if (c != null) {
                                ContentValues cv = new ContentValues();
                                String title = section.optString(WhooingKeyValues.TITLE);
                                String memo = section.optString(WhooingKeyValues.MEMO);
                                String currency = section.optString(WhooingKeyValues.CURRENCY);
                                String dateFormat = section.optString(WhooingKeyValues.DATE_FORMAT);

                                if (c.moveToFirst()) {
                                    if (!c.getString(Sections.COLUMN_INDEX_TITLE).equals(title)) {
                                        cv.put(Sections.COLUMN_TITLE, title);
                                    }
                                    if (!c.getString(Sections.COLUMN_INDEX_MEMO).equals(memo)) {
                                        cv.put(Sections.COLUMN_MEMO, memo);
                                    }
                                    if (!c.getString(Sections.COLUMN_INDEX_CURRENCY).equals(currency)) {
                                        cv.put(Sections.COLUMN_CURRENCY, currency);
                                    }
                                    if (!c.getString(Sections.COLUMN_INDEX_DATE_FORMAT).equals(dateFormat)) {
                                        cv.put(Sections.COLUMN_DATE_FORMAT, dateFormat);
                                    }
                                    if (c.getInt(Sections.COLUMN_INDEX_SORT_ORDER) != i) {
                                        cv.put(Sections.COLUMN_SORT_ORDER, i);
                                    }
                                    if (cv.size() > 0) {
                                        operations.add(ContentProviderOperation.newUpdate(sectionItemUri)
                                            .withValues(cv).build());
                                    }
                                } else {
                                    cv.put(Sections.COLUMN_SECTION_ID, sectionId);
                                    cv.put(Sections.COLUMN_TITLE, title);
                                    cv.put(Sections.COLUMN_MEMO, memo);
                                    cv.put(Sections.COLUMN_CURRENCY, currency);
                                    cv.put(Sections.COLUMN_DATE_FORMAT, dateFormat);
                                    cv.put(Sections.COLUMN_SORT_ORDER, i);
                                    operations.add(ContentProviderOperation.newInsert(sectionsUri)
                                        .withValues(cv).build());
                                }
                                c.close();
                            }
                            if (ids == null) {
                                ids = "('" + sectionId + "'";
                            } else {
                                ids += ",'" + sectionId + "'";
                            }
                        }
                        if (ids != null) {
                            ids += ")";
                            operations.add(ContentProviderOperation.newDelete(sectionsUri)
                                    .withSelection(Sections.COLUMN_SECTION_ID + " NOT IN " + ids, null)
                                    .build());
                        }
                        if (operations.size() > 0) {
                            getContext().getContentResolver().applyBatch(getContext().getString(R.string.whooing_authority),
                                    operations);
                        }
                    }
                } catch (JSONException | InterruptedException | ExecutionException | TimeoutException |
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
