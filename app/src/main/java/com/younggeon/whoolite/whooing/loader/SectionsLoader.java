package com.younggeon.whoolite.whooing.loader;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.younggeon.whoolite.WhooLiteNetwork;
import com.younggeon.whoolite.constant.PreferenceKeys;
import com.younggeon.whoolite.constant.WhooingKeyValues;
import com.younggeon.whoolite.db.schema.Sections;
import com.younggeon.whoolite.provider.WhooingProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                        JSONArray sections = result.optJSONArray(WhooingKeyValues.RESULT);
                        ContentValues[] values = new ContentValues[sections.length()];

                        for (int i = 0; i < sections.length(); i++) {
                            JSONObject section = sections.optJSONObject(i);

                            values[i] = new ContentValues();
                            values[i].put(Sections.COLUMN_SECTION_ID, section.optString(WhooingKeyValues.SECTION_ID));
                            values[i].put(Sections.COLUMN_TITLE, section.optString(WhooingKeyValues.TITLE));
                            values[i].put(Sections.COLUMN_MEMO, section.optString(WhooingKeyValues.MEMO));
                            values[i].put(Sections.COLUMN_CURRENCY, section.optString(WhooingKeyValues.CURRENCY));
                            values[i].put(Sections.COLUMN_DATE_FORMAT, section.optString(WhooingKeyValues.DATE_FORMAT));
                            values[i].put(Sections.COLUMN_SORT_ORDER, i);
                        }
                        getContext().getContentResolver().bulkInsert(WhooingProvider.getSectionsUri(),
                                values);
                    }
                } catch (JSONException | InterruptedException | ExecutionException | TimeoutException e) {
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
